const { onCall, HttpsError } = require('firebase-functions/v2/https');
const { onDocumentCreated, onDocumentUpdated } = require('firebase-functions/v2/firestore');
const { initializeApp } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
const { defineString, defineSecret } = require('firebase-functions/params');
const { primerError } = require('./contrasena');
const { crearTransporte, enviarBienvenida } = require('./bienvenida');
const Correos = require('./correos');

initializeApp();

const ROLES_ADMINISTRADOR = ['SUPERADMIN', 'ADMIN'];
// Quienes revisan renovaciones y cambios de plan: los administradores y los supervisores.
const ROLES_REVISORES = ['SUPERADMIN', 'ADMIN', 'SUPERVISOR'];
const ROLES_DE_PERSONAL = ['SUPERADMIN', 'ADMIN', 'ADMIN_AREA', 'AYUDANTE_AREA'];

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
    throw new HttpsError('permission-denied', 'Solo se puede cambiar la contraseña del personal, no la de los miembros.');
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

async function guardarNotificacion(usuarioId, id, titulo, mensaje, tipo) {
  const db = getFirestore();
  const referencia = db.collection('notificaciones').doc(id);
  await db.runTransaction(async (tx) => {
    const existente = await tx.get(referencia);
    if (existente.exists) return;
    tx.create(referencia, {
      id,
      usuarioId,
      titulo,
      mensaje,
      tipo,
      leida: false,
      createdAt: Date.now(),
      ruta: 'membresia'
    });
  });
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

async function notificarARevisores(prefijoId, solicitudId, titulo, mensaje, tipo) {
  const db = getFirestore();
  const revisores = await db.collection('usuarios').where('rol', 'in', ROLES_REVISORES).get();
  await Promise.all(revisores.docs
    .filter((u) => (u.get('estado') || 'ACTIVO') !== 'INACTIVO')
    .map((u) => guardarNotificacion(u.id, `${prefijoId}_${solicitudId}_${u.id}`, titulo, mensaje, tipo)));
}

exports.notificarNuevaRenovacion = onDocumentCreated(
  { ...OPCIONES, document: 'solicitudes_renovacion/{solicitudId}' },
  async (event) => {
    const solicitud = event.data?.data();
    if (!solicitud || solicitud.status !== 'PENDIENTE') return;
    await notificarARevisores(
      'renovacion', event.params.solicitudId, 'Nueva solicitud de renovación',
      `${solicitud.socioNombre || 'Un socio'} envió una renovación por $${Number(solicitud.monto || 0).toFixed(2)}. Revisa la pestaña Renovaciones.`,
      'RENOVACION'
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
