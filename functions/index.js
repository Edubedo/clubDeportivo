const { onCall, HttpsError } = require('firebase-functions/v2/https');
const { onDocumentCreated, onDocumentUpdated } = require('firebase-functions/v2/firestore');
const { onSchedule } = require('firebase-functions/v2/scheduler');
const { initializeApp } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
const { getMessaging } = require('firebase-admin/messaging');
const { defineString, defineSecret } = require('firebase-functions/params');
const { primerError } = require('./contrasena');
const { crearTransporte, enviarBienvenida } = require('./bienvenida');
const Correos = require('./correos');

initializeApp();

const ROLES_ADMINISTRADOR = ['SUPERADMIN', 'ADMIN'];
// Quienes revisan cambios de plan; las renovaciones también las pueden resolver encargados de área.
const ROLES_REVISORES = ['SUPERADMIN', 'ADMIN', 'SUPERVISOR'];
const ROLES_DE_PERSONAL = ['SUPERADMIN', 'ADMIN', 'SUPERVISOR', 'ADMIN_AREA', 'AYUDANTE_AREA'];

/**
 * Un administrador cambia la contraseña de una persona del personal sin enviarle correo.
 * data: { uid: string, contrasena: string }
 *
 * Solo la puede usar un administrador activo, y solo sobre cuentas del personal (nunca sobre miembros).
 * Cierra las sesiones abiertas de esa persona para que entre con la contraseña nueva.
 */
exports.cambiarContrasenaPersonal = onCall({ region: 'us-central1', maxInstances: 5 }, async (request) => {
  if (!request.auth) throw new HttpsError('unauthenticated', 'Inicia sesión para continuar.');

  const db = getFirestore();
  const yo = await db.doc(`usuarios/${request.auth.uid}`).get();
  const miRol = yo.exists ? yo.get('rol') : null;
  const activo = yo.exists && (yo.get('estado') || 'ACTIVO') !== 'INACTIVO';
  if (!activo || !ROLES_ADMINISTRADOR.includes(miRol)) {
    throw new HttpsError('permission-denied', 'Solo un administrador puede cambiar contraseñas del personal.');
  }

  const uid = typeof request.data?.uid === 'string' ? request.data.uid.trim() : '';
  const contrasena = request.data?.contrasena;
  if (!uid) throw new HttpsError('invalid-argument', 'Falta la persona a la que se le cambia la contraseña.');
  const error = primerError(contrasena);
  if (error) throw new HttpsError('invalid-argument', error);

  const objetivo = await db.doc(`usuarios/${uid}`).get();
  if (!objetivo.exists) throw new HttpsError('not-found', 'Esa persona ya no existe.');
  if (!ROLES_DE_PERSONAL.includes(objetivo.get('rol'))) {
    throw new HttpsError('permission-denied', 'Solo se puede cambiar la contraseña del personal, no la de los socios.');
  }

  // Solo un superadministrador puede cambiar la contraseña de otro superadministrador (un ADMIN tomaría su cuenta).
  if (objetivo.get('rol') === 'SUPERADMIN' && miRol !== 'SUPERADMIN') {
    throw new HttpsError('permission-denied', 'Solo un superadministrador puede cambiar la contraseña de otro superadministrador.');
  }

  try {
    await getAuth().updateUser(uid, { password: contrasena });
    await getAuth().revokeRefreshTokens(uid);
  } catch (e) {
    if (e.code === 'auth/user-not-found') throw new HttpsError('not-found', 'Esa persona no tiene cuenta de acceso.');
    console.error('No se pudo cambiar la contraseña', uid, e.code);
    throw new HttpsError('internal', 'No se pudo cambiar la contraseña. Intenta de nuevo.');
  }
  await db.doc(`usuarios/${uid}`).set(
    { contrasenaCambiadaEn: FieldValue.serverTimestamp(), contrasenaCambiadaPor: request.auth.uid },
    { merge: true }
  );
  return { ok: true };
});

async function guardarNotificacion(usuarioId, id, titulo, mensaje, tipo, ruta = 'membresia') {
  const db = getFirestore();
  const referencia = db.collection('notificaciones').doc(id);
  const creada = await db.runTransaction(async (tx) => {
    const existente = await tx.get(referencia);
    if (existente.exists) return false;
    tx.create(referencia, {
      id,
      usuarioId,
      titulo,
      mensaje,
      tipo,
      leida: false,
      createdAt: Date.now(),
      ruta
    });
    return true;
  });
  if (creada) await enviarPush(usuarioId, { id, titulo, mensaje, tipo, ruta });
}

async function enviarPush(usuarioId, notificacion) {
  const db = getFirestore();
  const [perfil, tokens] = await Promise.all([
    db.doc(`usuarios/${usuarioId}`).get(),
    db.collection('fcmTokens').where('usuarioId', '==', usuarioId).get()
  ]);
  if (!perfil.exists || (perfil.get('estado') || 'ACTIVO') === 'INACTIVO') return;
  if (tokens.empty) {
    console.warn('No hay token FCM registrado para el destinatario', usuarioId, notificacion.id);
    return;
  }

  const rol = perfil.get('rol');
  const categoria = notificacion.tipo === 'RESERVA'
    ? 'SOLICITUDES'
    : notificacion.tipo === 'RESERVA_RESPUESTA'
      ? 'RESERVA_RESPUESTA'
      : notificacion.tipo === 'VENCIMIENTOS'
        ? 'VENCIMIENTOS'
        : notificacion.tipo === 'MEMBRESIA'
          ? 'MEMBRESIA'
      : ['RENOVACION', 'CAMBIO_PLAN'].includes(notificacion.tipo)
        ? (['SOCIO', 'VISITANTE_EXTERNO'].includes(rol) ? 'MEMBRESIA' : 'SOLICITUDES')
        : 'AVISOS';
  await Promise.all(tokens.docs.map(async (tokenDoc) => {
    try {
      await getMessaging().send({
        token: tokenDoc.get('token'),
        data: {
          notificacionId: notificacion.id,
          usuarioId,
          titulo: notificacion.titulo,
          mensaje: notificacion.mensaje,
          tipo: notificacion.tipo,
          categoria,
          ruta: notificacion.ruta
        },
        android: { priority: 'high' }
      });
    } catch (e) {
      console.error('No se pudo enviar push', usuarioId, notificacion.id, e.code || e.message);
      if (['messaging/invalid-registration-token', 'messaging/registration-token-not-registered'].includes(e.code)) {
        await tokenDoc.ref.delete();
      }
    }
  }));
}

// ---------- Parámetros del correo (SMTP) ----------
// Servicio de correo: cualquier SMTP gratuito (Gmail con contraseña de aplicación, Brevo...). Ver docs/correo-de-bienvenida.md.
const SMTP_HOST = defineString('SMTP_HOST', { default: 'smtp.gmail.com' });
const SMTP_PORT = defineString('SMTP_PORT', { default: '465' });
const SMTP_USER = defineString('SMTP_USER', { default: '' });
const SMTP_REMITENTE = defineString('SMTP_REMITENTE', { default: '' }); // "Athletic Club <correo@dominio>"; vacío = SMTP_USER
const ENLACE_BASE = defineString('ENLACE_BASE', { default: '' }); // vacío = https://<proyecto>.web.app/unirse
const SMTP_PASS = defineSecret('SMTP_PASS');

/**
 * Transporte de correo, o null para NO enviar de verdad ("simulado": solo queda constancia en `correos_enviados`).
 * Es null en el emulador y mientras no haya un SMTP configurado, así probar nunca manda correos reales.
 */
function transporteDeCorreo() {
  const usuario = SMTP_USER.value();
  if (process.env.FUNCTIONS_EMULATOR === 'true' || !usuario) return null;
  return crearTransporte({ host: SMTP_HOST.value(), port: SMTP_PORT.value(), user: usuario, pass: SMTP_PASS.value() });
}

/**
 * Manda (o simula) un correo de membresía a una persona y deja constancia en `correos_enviados`.
 * Si el correo de su cuenta es interno (cuentas por código) se usa el de contacto de `miembros/{codigo}`.
 */
async function correoAlMiembro({ event, tipo, uid, codigo, datos }) {
  const db = getFirestore();
  const perfil = uid ? await db.doc(`usuarios/${uid}`).get() : null;
  const cod = codigo || (perfil && perfil.get('codigoMiembro')) || '';
  const miembro = cod ? await db.doc(`miembros/${cod}`).get() : null;
  const para = Correos.elegirCorreo({
    correoCuenta: perfil && perfil.get('email'),
    correoContacto: miembro && miembro.get('correo')
  });
  const nombre = datos.nombre || (perfil && perfil.get('nombre')) || '';
  const correo = Correos.compositores[tipo]({ ...datos, nombre });
  return Correos.despachar({
    db, id: `${event.id}_${tipo}`, transporte: transporteDeCorreo(),
    de: SMTP_REMITENTE.value() || SMTP_USER.value(), para, correo, tipo, meta: { usuarioId: uid || '' }
  });
}

const OPCIONES = { region: 'us-central1', maxInstances: 5, secrets: [SMTP_PASS] };
const NIVELES = ['NINO', 'NORMAL', 'DELUXE'];

async function notificarARevisores(
  prefijoId, solicitudId, titulo, mensaje, tipo, roles = ROLES_REVISORES
) {
  const db = getFirestore();
  const revisores = await db.collection('usuarios').where('rol', 'in', roles).get();
  await Promise.all(revisores.docs
    .filter((u) => (u.get('estado') || 'ACTIVO') !== 'INACTIVO')
    .map((u) => guardarNotificacion(u.id, `${prefijoId}_${solicitudId}_${u.id}`, titulo, mensaje, tipo)));
}

/** Cuentas que atienden las reservas de un deporte: sus encargados activos; si no hay, los administradores. */
async function destinatariosDeReserva(deporte) {
  const db = getFirestore();
  const encargados = await db.collection('usuarios')
    .where('rol', 'in', ['ADMIN_AREA', 'AYUDANTE_AREA'])
    .get();
  const area = String(deporte || '').trim().toLowerCase();
  const asignados = encargados.docs.filter((usuario) =>
    (usuario.get('estado') || 'ACTIVO') !== 'INACTIVO' &&
    String(usuario.get('areaTrabajo') || '').trim().toLowerCase() === area
  );
  return asignados.length > 0
    ? asignados
    : (await db.collection('usuarios').where('rol', 'in', ROLES_ADMINISTRADOR).get())
      .docs.filter((usuario) => (usuario.get('estado') || 'ACTIVO') !== 'INACTIVO');
}

// Las cuentas del área reciben solicitudes; si no hay ninguna activa, el administrador es el respaldo.
exports.notificarNuevaReserva = onDocumentCreated(
  { ...OPCIONES, document: 'reservas/{reservaId}' },
  async (event) => {
    const reserva = event.data?.data();
    if (!reserva || reserva.estado !== 'PENDIENTE_APROBACION') return;

    const destinatarios = await destinatariosDeReserva(reserva.deporte);

    await Promise.all(destinatarios.map((usuario) =>
      guardarNotificacion(
        usuario.id,
        `reserva_${event.params.reservaId}_nueva_${usuario.id}`,
        'Nueva solicitud de cancha',
        `${reserva.usuarioNombre || 'Un socio'} solicita ${reserva.areaNombre || reserva.deporte || 'una cancha'} para el ${reserva.fecha || ''} a las ${reserva.horaInicio || ''}.`,
        'RESERVA',
        'reservas'
      )
    ));
  }
);

exports.notificarResolucionReserva = onDocumentUpdated(
  { ...OPCIONES, document: 'reservas/{reservaId}' },
  async (event) => {
    const anterior = event.data?.before.data();
    const reserva = event.data?.after.data();
    if (!anterior || !reserva || anterior.estado !== 'PENDIENTE_APROBACION' ||
        !['CONFIRMADA', 'RECHAZADA'].includes(reserva.estado) || !reserva.usuarioId) return;

    const aprobada = reserva.estado === 'CONFIRMADA';
    await guardarNotificacion(
      reserva.usuarioId,
      `reserva_${event.params.reservaId}_resuelta_${reserva.estado.toLowerCase()}_${reserva.usuarioId}`,
      aprobada ? 'Reserva confirmada' : 'Reserva rechazada',
      aprobada
        ? `Tu reserva de ${reserva.areaNombre || reserva.deporte || 'cancha'} para el ${reserva.fecha || ''} a las ${reserva.horaInicio || ''} fue confirmada.`
        : `Tu solicitud de reserva de ${reserva.areaNombre || reserva.deporte || 'cancha'} para el ${reserva.fecha || ''} a las ${reserva.horaInicio || ''} fue rechazada.`,
      'RESERVA_RESPUESTA',
      'reservas'
    );
  }
);

// El miembro pidió cambiar el día u horario de una reserva confirmada: le toca al encargado del área validarlo.
exports.notificarModificacionReserva = onDocumentUpdated(
  { ...OPCIONES, document: 'reservas/{reservaId}' },
  async (event) => {
    const antes = event.data?.before.data();
    const reserva = event.data?.after.data();
    const mod = reserva?.modificacion;
    if (!antes || !mod || reserva.estado !== 'CONFIRMADA') return;
    if (JSON.stringify(antes.modificacion || null) === JSON.stringify(mod)) return;

    const destinatarios = await destinatariosDeReserva(reserva.deporte);
    await Promise.all(destinatarios.map((usuario) =>
      guardarNotificacion(
        usuario.id,
        `reserva_${event.params.reservaId}_cambio_${event.id}_${usuario.id}`,
        'Solicitud de cambio de reserva',
        `${reserva.usuarioNombre || 'Un socio'} quiere cambiar su reserva de ${reserva.areaNombre || reserva.deporte || 'cancha'} ` +
          `del ${Correos.fechaLegible(reserva.fecha)} a las ${reserva.horaInicio || ''} al ${Correos.fechaLegible(mod.fecha)} a las ${mod.horaInicio || ''}.`,
        'RESERVA',
        'reservas'
      )
    ));
  }
);

// El encargado aprobó o rechazó el cambio: se le avisa al miembro (la app deja `resolucionModificacion` al resolver).
exports.notificarResolucionModificacionReserva = onDocumentUpdated(
  { ...OPCIONES, document: 'reservas/{reservaId}' },
  async (event) => {
    const antes = event.data?.before.data();
    const reserva = event.data?.after.data();
    const res = reserva?.resolucionModificacion;
    if (!antes || !res || !reserva.usuarioId) return;
    if (JSON.stringify(antes.resolucionModificacion || null) === JSON.stringify(res)) return;

    const aprobada = res.resultado === 'APROBADA';
    const lugar = reserva.areaNombre || reserva.deporte || 'cancha';
    await guardarNotificacion(
      reserva.usuarioId,
      `reserva_${event.params.reservaId}_cambio_resuelto_${event.id}`,
      aprobada ? 'Cambio de reserva aprobado' : 'Cambio de reserva rechazado',
      aprobada
        ? `Aprobamos el cambio de tu reserva de ${lugar}: ahora es el ${Correos.fechaLegible(reserva.fecha)} de ${reserva.horaInicio || ''} a ${reserva.horaFin || ''}.`
        : `No pudimos aprobar el cambio de tu reserva de ${lugar}. Se mantiene el ${Correos.fechaLegible(reserva.fecha)} a las ${reserva.horaInicio || ''}.`,
      'RESERVA_RESPUESTA',
      'reservas'
    );
  }
);

// El área entró en mantenimiento y la app canceló las reservas de ese periodo: aviso en la app y por correo al miembro.
exports.notificarReservaCanceladaPorMantenimiento = onDocumentUpdated(
  { ...OPCIONES, document: 'reservas/{reservaId}' },
  async (event) => {
    const antes = event.data?.before.data();
    const reserva = event.data?.after.data();
    if (!antes || !reserva || !reserva.canceladaPorMantenimiento || antes.canceladaPorMantenimiento ||
        reserva.estado !== 'CANCELADA' || !reserva.usuarioId) return;

    const lugar = reserva.areaNombre || reserva.deporte || 'La cancha';
    const periodo = reserva.mantenimientoDesde && reserva.mantenimientoHasta
      ? ` del ${Correos.fechaLegible(reserva.mantenimientoDesde)} al ${Correos.fechaLegible(reserva.mantenimientoHasta)}`
      : '';
    await guardarNotificacion(
      reserva.usuarioId,
      `reserva_${event.params.reservaId}_mantenimiento_${reserva.usuarioId}`,
      'Reserva cancelada por mantenimiento',
      `${lugar} estará en mantenimiento${periodo}. Cancelamos tu reserva del ${Correos.fechaLegible(reserva.fecha)} a las ${reserva.horaInicio || ''}; ` +
        'puedes reagendarla desde Mis reservas.',
      'RESERVA_RESPUESTA',
      'reservas'
    );
    try {
      await correoAlMiembro({
        event, tipo: 'reservaCanceladaMantenimiento', uid: reserva.usuarioId,
        datos: {
          nombre: reserva.usuarioNombre, area: reserva.areaNombre, fecha: reserva.fecha,
          horaInicio: reserva.horaInicio, horaFin: reserva.horaFin,
          desde: reserva.mantenimientoDesde, hasta: reserva.mantenimientoHasta, motivo: reserva.mantenimientoMotivo
        }
      });
    } catch (e) {
      console.error('No se pudo mandar el correo de mantenimiento', event.params.reservaId, e.message);
    }
  }
);

exports.notificarMembresiasPorVencer = onSchedule(
  { schedule: '0 8 * * *', timeZone: 'America/Mexico_City', region: 'us-central1', maxInstances: 1 },
  async () => {
    const db = getFirestore();
    const partesFecha = new Intl.DateTimeFormat('en-CA', {
      timeZone: 'America/Mexico_City', year: 'numeric', month: '2-digit', day: '2-digit'
    }).formatToParts(new Date()).reduce((partes, parte) => {
      partes[parte.type] = parte.value;
      return partes;
    }, {});
    const hoy = `${partesFecha.year}-${partesFecha.month}-${partesFecha.day}`;
    const fechaLimite = new Date(Date.parse(`${hoy}T00:00:00Z`) + 3 * 24 * 60 * 60 * 1000)
      .toISOString().slice(0, 10);
    const [membresias, personal] = await Promise.all([
      db.collection('membresias').where('estado', '==', 'ACTIVA').get(),
      db.collection('usuarios').where('rol', 'in', ROLES_DE_PERSONAL).get()
    ]);

    for (const membresia of membresias.docs) {
      const vence = membresia.get('fechaVencimiento');
      if (typeof vence !== 'string' || vence < hoy || vence > fechaLimite) continue;

      const dias = Math.round((Date.parse(`${vence}T12:00:00Z`) - Date.parse(`${hoy}T12:00:00Z`)) / 86400000);
      const idsSocios = new Set([membresia.get('usuarioId')].filter(Boolean));
      const miembros = await db.collection('miembros').where('membresiaId', '==', membresia.id).get();
      miembros.docs.forEach((miembro) => {
        const uid = miembro.get('usuarioId');
        if (typeof uid === 'string' && uid) idsSocios.add(uid);
      });

      const venceMensaje = dias === 0 ? 'vence hoy' : `vence en ${dias} día${dias === 1 ? '' : 's'}`;
      const nombre = membresia.get('titularNombre') || 'Tu membresía';
      await Promise.all([
        ...[...idsSocios].map((uid) => guardarNotificacion(
          uid,
          `vencimiento_${membresia.id}_${vence}_${uid}`,
          dias === 0 ? 'Tu membresía vence hoy' : 'Tu membresía está por vencer',
          `${nombre}: tu paquete ${venceMensaje} (${vence}).`,
          'MEMBRESIA',
          'membresia'
        )),
        ...personal.docs
          .filter((usuario) => (usuario.get('estado') || 'ACTIVO') !== 'INACTIVO')
          .map((usuario) => guardarNotificacion(
            usuario.id,
            `vencimiento_${membresia.id}_${vence}_${usuario.id}`,
            'Membresía próxima a vencer',
            `El paquete de ${nombre} ${venceMensaje} (${vence}).`,
            'VENCIMIENTOS',
            'membresia'
          ))
      ]);
    }
  }
);

exports.notificarNuevaRenovacion = onDocumentCreated(
  { ...OPCIONES, document: 'solicitudes_renovacion/{solicitudId}' },
  async (event) => {
    const solicitud = event.data?.data();
    if (!solicitud || solicitud.status !== 'PENDIENTE') return;
    await notificarARevisores(
      'renovacion', event.params.solicitudId, 'Nueva solicitud de renovación',
      `${solicitud.socioNombre || 'Un socio'} envió una renovación por $${Number(solicitud.monto || 0).toFixed(2)}. Revisa la pestaña Renovaciones.`,
      'RENOVACION', ROLES_DE_PERSONAL
    );
    // Acuse de recibo por correo al miembro.
    if (solicitud.socioId) {
      await correoAlMiembro({
        event, tipo: 'renovacionRecibida', uid: solicitud.socioId,
        datos: { nombre: solicitud.socioNombre, monto: solicitud.monto }
      });
    }
  }
);

exports.notificarResolucionRenovacion = onDocumentUpdated(
  { ...OPCIONES, document: 'solicitudes_renovacion/{solicitudId}' },
  async (event) => {
    const anterior = event.data?.before.data();
    const actual = event.data?.after.data();
    if (!anterior || !actual || anterior.status !== 'PENDIENTE' ||
        !['APROBADO', 'RECHAZADO'].includes(actual.status) || !actual.socioId) return;

    const aprobado = actual.status === 'APROBADO';
    const hasta = actual.renewedUntil ? ` Vigente hasta el ${Correos.fechaLegible(actual.renewedUntil)}.` : ' Tu vigencia ya está actualizada.';
    const motivo = actual.motivoRechazo ? ` Motivo: ${actual.motivoRechazo}` : ' Comunícate con el personal del club para más información.';
    await guardarNotificacion(
      actual.socioId,
      `renovacion_${event.params.solicitudId}_${actual.status.toLowerCase()}`,
      aprobado ? 'Renovación aprobada' : 'Renovación rechazada',
      aprobado ? `Tu renovación de membresía fue aprobada.${hasta}` : `Tu solicitud de renovación fue rechazada.${motivo}`,
      'RENOVACION'
    );
    // El correo de "aprobada" sale con el cambio de la membresía (cubre también las renovaciones hechas en recepción).
    if (!aprobado) {
      await correoAlMiembro({
        event, tipo: 'renovacionRechazada', uid: actual.socioId,
        datos: { nombre: actual.socioNombre, motivo: actual.motivoRechazo }
      });
    }
  }
);

// ---------- Cambio de plan ----------
exports.notificarNuevoCambioPlan = onDocumentCreated(
  { ...OPCIONES, document: 'solicitudes_cambio_plan/{solicitudId}' },
  async (event) => {
    const s = event.data?.data();
    if (!s || s.status !== 'PENDIENTE') return;
    const sube = NIVELES.indexOf(s.planNuevo) > NIVELES.indexOf(s.planActual);
    await notificarARevisores(
      'cambioplan', event.params.solicitudId, 'Solicitud de cambio de plan',
      `${s.socioNombre || 'Un socio'} pide ${sube ? 'subir' : 'bajar'} de ${Correos.nombrePlan(s.planActual)} a ${Correos.nombrePlan(s.planNuevo)}. Revisa la pestaña Cambios de plan.`,
      'CAMBIO_PLAN'
    );
    await correoAlMiembro({
      event, tipo: 'cambioPlanRecibido', uid: s.socioId,
      datos: { nombre: s.socioNombre, planActual: s.planActual, planNuevo: s.planNuevo, precioNuevo: s.precioNuevo }
    });
  }
);

exports.notificarResolucionCambioPlan = onDocumentUpdated(
  { ...OPCIONES, document: 'solicitudes_cambio_plan/{solicitudId}' },
  async (event) => {
    const antes = event.data?.before.data();
    const s = event.data?.after.data();
    if (!antes || !s || antes.status !== 'PENDIENTE' || !['APROBADO', 'RECHAZADO'].includes(s.status) || !s.socioId) return;
    const aprobado = s.status === 'APROBADO';
    const motivo = s.motivoRechazo ? ` Motivo: ${s.motivoRechazo}` : '';
    await guardarNotificacion(
      s.socioId, `cambioplan_${event.params.solicitudId}_${s.status.toLowerCase()}`,
      aprobado ? 'Cambio de plan aprobado' : 'Cambio de plan rechazado',
      aprobado
        ? `Tu plan ahora es ${Correos.nombrePlan(s.planNuevo)}. El nuevo precio se cobra desde tu próxima renovación.`
        : `No se aprobó tu cambio al plan ${Correos.nombrePlan(s.planNuevo)}.${motivo}`,
      'CAMBIO_PLAN'
    );
    await correoAlMiembro({
      event, tipo: aprobado ? 'cambioPlanAprobado' : 'cambioPlanRechazado', uid: s.socioId,
      datos: aprobado
        ? { nombre: s.socioNombre, planAnterior: s.planActual, planNuevo: s.planNuevo, precioNuevo: s.precioNuevo }
        : { nombre: s.socioNombre, planNuevo: s.planNuevo, motivo: s.motivoRechazo }
    });
  }
);

// ---------- Cambios en la membresía (renovada, suspendida, reactivada, plan cambiado por el club) ----------
exports.correosDeMembresia = onDocumentUpdated(
  { ...OPCIONES, document: 'membresias/{membresiaId}' },
  async (event) => {
    const antes = event.data?.before.data();
    const despues = event.data?.after.data();
    const correos = Correos.correosPorCambioDeMembresia(antes, despues);
    for (const { tipo, datos } of correos) {
      await correoAlMiembro({
        event, tipo, uid: despues.usuarioId, codigo: despues.titularCodigo,
        datos: { ...datos, nombre: despues.titularNombre }
      });
    }
  }
);

// ---------- Correo de bienvenida ----------

/**
* Cuando el personal registra a un miembro (o el titular agrega a un familiar) nace su documento `miembros/{codigo}`:
* se le manda por correo su código de acceso y un enlace que abre la app para crear su cuenta.
* Se manda una sola vez (queda `bienvenidaEnviadaEn`); si falla, queda `bienvenidaError` y no se reintenta solo.
*/
exports.enviarBienvenidaMiembro = onDocumentCreated(
 { document: 'miembros/{codigo}', region: 'us-central1', maxInstances: 5, secrets: [SMTP_PASS] },
 async (event) => {
   const snap = event.data;
   if (!snap) return;
   const codigo = event.params.codigo;
   const miembro = snap.data();
   if (miembro.bienvenidaEnviadaEn) return;

   const proyecto = process.env.GCLOUD_PROJECT || '';
   const baseEnlace = ENLACE_BASE.value() || `https://${proyecto}.web.app/unirse`;
   const usuario = SMTP_USER.value();
   const transporte = crearTransporte({ host: SMTP_HOST.value(), port: SMTP_PORT.value(), user: usuario, pass: SMTP_PASS.value() });

   try {
     const r = await enviarBienvenida({
       transporte, de: SMTP_REMITENTE.value() || usuario, baseEnlace, miembro, codigo
     });
     if (r.enviado) {
       await snap.ref.set({ bienvenidaEnviadaEn: FieldValue.serverTimestamp(), bienvenidaError: FieldValue.delete() }, { merge: true });
     } else if (r.motivo !== 'sin-correo') {
       // Sin correo no es un error: el personal le da el código en persona. Cualquier otro motivo sí se deja anotado.
       await snap.ref.set({ bienvenidaError: r.motivo }, { merge: true });
     }
   } catch (e) {
     console.error('No se pudo enviar la bienvenida', codigo, e.code || e.message);
     await snap.ref.set({ bienvenidaError: `smtp:${e.code || 'error'}` }, { merge: true });
   }
 }
);

// ---------- Un titular quita a un familiar de su paquete ----------
/**
 * La app deja `bajasFamiliares/{codigo}` en el mismo lote que borra al miembro y suspende su cuenta. Aquí se avisa por
 * correo (y en la app) al titular, con un recordatorio de que ya tiene un lugar libre, y a quien se quitó.
 */
exports.avisarFamiliarQuitado = onDocumentCreated(
  { ...OPCIONES, document: 'bajasFamiliares/{codigo}' },
  async (event) => {
    const baja = event.data?.data();
    if (!baja) return;
    const db = getFirestore();
    const membresia = baja.membresiaId ? await db.doc(`membresias/${baja.membresiaId}`).get() : null;
    const total = Number(membresia?.get('totalPersonas') || 1);
    const tope = { 1: 4, 2: 2, 3: 4 }[membresia?.get('paqueteFamiliarId')] || total;
    const lugaresLibres = Math.max(tope - total, 0);

    if (baja.titularId) {
      await guardarNotificacion(
        baja.titularId, `baja_familiar_${event.params.codigo}`,
        'Familiar quitado de tu paquete',
        `${baja.nombre} salió de tu paquete familiar.` + (lugaresLibres > 0 ? ' Ya tienes un lugar libre para registrar a otra persona.' : ''),
        'MEMBRESIA'
      );
      await correoAlMiembro({
        event, tipo: 'familiarQuitadoTitular', uid: baja.titularId,
        datos: { nombre: baja.titularNombre, familiar: baja.nombre, lugaresLibres }
      });
    }

    // A quien se quitó: el correo de su cuenta si es real; si no (cuenta interna por código), el de contacto del alta.
    const perfil = baja.usuarioId ? await db.doc(`usuarios/${baja.usuarioId}`).get() : null;
    const para = Correos.elegirCorreo({ correoCuenta: perfil && perfil.get('email'), correoContacto: baja.correo });
    await Correos.despachar({
      db, id: `${event.id}_familiarQuitadoFamiliar`, transporte: transporteDeCorreo(),
      de: SMTP_REMITENTE.value() || SMTP_USER.value(), para, tipo: 'familiarQuitadoFamiliar',
      correo: Correos.compositores.familiarQuitadoFamiliar({ nombre: baja.nombre, titular: baja.titularNombre }),
      meta: { usuarioId: baja.usuarioId || '' }
    });
  }
);

// ---------- Acceso revocado ----------
/**
 * Cuando el personal restablece el acceso de un miembro (restablecerAcceso), su cuenta real anterior queda marcada con
 * `accesoRevocado`: aquí se deshabilita en Firebase Authentication y se cierran sus sesiones, para que no siga siendo
 * una credencial válida (la app solo la bloquea por el estado INACTIVO del perfil).
 */
exports.revocarAccesoDeCuenta = onDocumentUpdated(
  { ...OPCIONES, document: 'usuarios/{uid}' },
  async (event) => {
    const antes = event.data?.before.data();
    const despues = event.data?.after.data();
    if (!despues || despues.accesoRevocado !== true || antes?.accesoRevocado === true) return;
    try {
      await getAuth().updateUser(event.params.uid, { disabled: true });
      await getAuth().revokeRefreshTokens(event.params.uid);
    } catch (e) {
      if (e.code !== 'auth/user-not-found') console.error('No se pudo revocar el acceso', event.params.uid, e.code);
    }
  }
);
