// Correos de cambios en la membresía (renovaciones, cambio de plan, suspensión...). Sin dependencias de Firebase
// para poder probarlos solos (ver correos.test.js); el envío real lo hace un transporte de nodemailer que se inyecta.

const REGEX_CORREO = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;
const DOMINIO_INTERNO = '@miembros.clubdeportivo.app'; // cuentas internas por código: no son correos reales

const NOMBRES_PLAN = { NINO: 'Individual niños', NORMAL: 'Individual normal', DELUXE: 'Individual deluxe' };
const NOMBRES_PAQUETE = { 1: 'Familiar', 2: 'Pareja', 3: 'Niños' };

const escapar = (t) =>
  String(t ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);

const dinero = (n) => '$' + Number(n || 0).toLocaleString('en-US', { maximumFractionDigits: 0 });

const nombrePlan = (plan) => NOMBRES_PLAN[plan] || String(plan || '');

/** "2026-12-15" -> "15 de diciembre de 2026". */
function fechaLegible(iso) {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(iso || ''));
  if (!m) return String(iso || '');
  const meses = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];
  return `${Number(m[3])} de ${meses[Number(m[2]) - 1]} de ${m[1]}`;
}

const esCorreo = (c) => typeof c === 'string' && REGEX_CORREO.test(c.trim());
const esCorreoReal = (c) => esCorreo(c) && !c.trim().toLowerCase().endsWith(DOMINIO_INTERNO);

/**
 * A qué correo se le avisa: el de su cuenta si es real; si no (cuentas internas por código), el correo de contacto
 * que dejó el personal al registrarlo. null si no hay ninguno válido.
 */
function elegirCorreo({ correoCuenta, correoContacto }) {
  if (esCorreoReal(correoCuenta)) return correoCuenta.trim();
  if (esCorreoReal(correoContacto)) return correoContacto.trim();
  return null;
}

function plantilla({ club = 'Athletic Club', nombre, titulo, parrafos, destacado, nota }) {
  const primer = String(nombre || '').trim().split(/\s+/)[0] || '';
  const saludo = primer ? `Hola ${primer}` : 'Hola';
  const texto = [`${saludo},`, '', ...parrafos, ...(destacado ? ['', destacado] : []), ...(nota ? ['', nota] : []), '', `— ${club}`].join('\n');
  const html = `<!doctype html>
<html lang="es"><body style="margin:0;padding:24px;background:#f8fafc;font-family:Arial,Helvetica,sans-serif;color:#0b1220">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0"><tr><td align="center">
<table role="presentation" width="480" cellpadding="0" cellspacing="0" style="max-width:480px;background:#ffffff;border-radius:12px;padding:28px">
<tr><td>
<h1 style="margin:0 0 12px;font-size:22px;color:#052659">${escapar(club)}</h1>
<h2 style="margin:0 0 16px;font-size:18px">${escapar(titulo)}</h2>
<p style="margin:0 0 14px;font-size:16px">${escapar(saludo)},</p>
${parrafos.map((p) => `<p style="margin:0 0 14px;font-size:15px;line-height:1.45">${escapar(p)}</p>`).join('\n')}
${destacado ? `<p style="margin:0 0 14px;padding:14px;background:#dbeafe;border-radius:8px;font-size:17px;font-weight:bold">${escapar(destacado)}</p>` : ''}
${nota ? `<p style="margin:16px 0 0;font-size:12px;color:#64748b">${escapar(nota)}</p>` : ''}
</td></tr></table></td></tr></table></body></html>`;
  return { texto, html };
}

const PIE = 'Si no esperabas este correo o tienes dudas, acércate a recepción.';

/** Cada compositor devuelve { asunto, texto, html }. */
const compositores = {
  cambioPlanRecibido: ({ nombre, planActual, planNuevo, precioNuevo }) => ({
    asunto: 'Recibimos tu solicitud de cambio de plan',
    ...plantilla({
      nombre, titulo: 'Solicitud de cambio de plan recibida',
      parrafos: [
        `Recibimos tu solicitud para pasar de ${nombrePlan(planActual)} a ${nombrePlan(planNuevo)}.`,
        'Un administrador o supervisor la revisará y te avisaremos por la app y por correo en cuanto tenga respuesta. Mientras tanto, tu plan actual sigue igual.'
      ],
      destacado: `${nombrePlan(planNuevo)} · ${dinero(precioNuevo)} al mes`, nota: PIE
    })
  }),
  cambioPlanAprobado: ({ nombre, planAnterior, planNuevo, precioNuevo }) => ({
    asunto: `Tu cambio de plan fue aprobado: ${nombrePlan(planNuevo)}`,
    ...plantilla({
      nombre, titulo: 'Cambio de plan aprobado',
      parrafos: [
        `Aprobamos tu cambio de ${nombrePlan(planAnterior)} a ${nombrePlan(planNuevo)}. Ya está activo en tu membresía.`,
        `El nuevo precio (${dinero(precioNuevo)} al mes) se cobra desde tu próxima renovación.`
      ],
      destacado: `Tu plan ahora: ${nombrePlan(planNuevo)}`, nota: PIE
    })
  }),
  cambioPlanRechazado: ({ nombre, planNuevo, motivo }) => ({
    asunto: 'Tu solicitud de cambio de plan no fue aprobada',
    ...plantilla({
      nombre, titulo: 'Cambio de plan no aprobado',
      parrafos: [
        `Por ahora no pudimos aprobar tu cambio al plan ${nombrePlan(planNuevo)}.`,
        `Motivo: ${motivo || 'Sin motivo indicado.'}`,
        'Tu plan actual sigue igual. Puedes volver a solicitarlo desde la app cuando quieras.'
      ], nota: PIE
    })
  }),
  planCambiadoPorElClub: ({ nombre, planAnterior, planNuevo, precioNuevo }) => ({
    asunto: `Tu plan cambió: ${nombrePlan(planNuevo)}`,
    ...plantilla({
      nombre, titulo: 'Tu plan fue actualizado',
      parrafos: [
        `El personal del club cambió tu plan de ${nombrePlan(planAnterior)} a ${nombrePlan(planNuevo)}.`,
        `Tu precio mensual ahora es ${dinero(precioNuevo)}.`
      ],
      destacado: `Tu plan ahora: ${nombrePlan(planNuevo)}`, nota: PIE
    })
  }),
  renovacionRecibida: ({ nombre, monto }) => ({
    asunto: 'Recibimos tu solicitud de renovación',
    ...plantilla({
      nombre, titulo: 'Renovación recibida',
      parrafos: [
        `Recibimos tu comprobante por ${dinero(monto)} para renovar tu membresía.`,
        'Un administrador lo revisará y te avisaremos por la app y por correo en cuanto esté validado.'
      ], nota: PIE
    })
  }),
  renovacionAprobada: ({ nombre, hasta }) => ({
    asunto: 'Tu membresía fue renovada',
    ...plantilla({
      nombre, titulo: 'Renovación aprobada',
      parrafos: ['Validamos tu pago y renovamos tu membresía. ¡Gracias!'],
      destacado: `Vigente hasta el ${fechaLegible(hasta)}`, nota: PIE
    })
  }),
  renovacionRechazada: ({ nombre, motivo }) => ({
    asunto: 'Tu solicitud de renovación no fue aprobada',
    ...plantilla({
      nombre, titulo: 'Renovación no aprobada',
      parrafos: [
        'No pudimos aprobar tu solicitud de renovación.',
        `Motivo: ${motivo || 'Sin motivo indicado.'}`,
        'Puedes enviar un comprobante nuevo desde la sección Membresía de la app.'
      ], nota: PIE
    })
  }),
  familiarQuitadoTitular: ({ nombre, familiar, lugaresLibres }) => ({
    asunto: `${familiar} salió de tu paquete familiar`,
    ...plantilla({
      nombre, titulo: 'Quitaste a un familiar de tu paquete',
      parrafos: [
        `Quitaste a ${familiar} de tu paquete familiar. Su cuenta quedó suspendida y ya no puede entrar al club ni registrarse con su código.`,
        lugaresLibres > 0
          ? 'Ya tienes un lugar libre: desde la app, en Membresía > Tu familia, puedes registrar a otra persona. Le llegará su código de acceso por correo.'
          : 'Puedes registrar a otra persona desde la app, en Membresía > Tu familia.'
      ],
      destacado: lugaresLibres > 0 ? `Lugares libres en tu paquete: ${lugaresLibres}` : undefined, nota: PIE
    })
  }),
  familiarQuitadoFamiliar: ({ nombre, titular }) => ({
    asunto: 'Tu acceso a Athletic Club fue suspendido',
    ...plantilla({
      nombre, titulo: 'Saliste de un paquete familiar',
      parrafos: [
        `${titular || 'El titular de tu paquete familiar'} te quitó de su paquete, por lo que tu cuenta quedó suspendida y ya no tienes acceso al club.`,
        'Si crees que fue un error, habla con el titular o acércate a recepción.'
      ],
      nota: PIE
    })
  }),
  membresiaSuspendida: ({ nombre, motivo }) => ({
    asunto: 'Tu membresía fue suspendida',
    ...plantilla({
      nombre, titulo: 'Membresía suspendida',
      parrafos: [
        'Tu membresía fue suspendida y no podrás reservar mientras siga así.',
        `Motivo: ${motivo || 'Sin motivo indicado.'}`,
        'Si crees que es un error, comunícate con el personal del club.'
      ], nota: PIE
    })
  }),
  reservaCanceladaMantenimiento: ({ nombre, area, fecha, horaInicio, horaFin, desde, hasta, motivo }) => ({
    asunto: `Tu reserva del ${fechaLegible(fecha)} se canceló por mantenimiento`,
    ...plantilla({
      nombre, titulo: 'Reserva cancelada por mantenimiento',
      parrafos: [
        `${area || 'El área'} estará en mantenimiento ${desde && hasta ? `del ${fechaLegible(desde)} al ${fechaLegible(hasta)}` : 'por tiempo indefinido'}` +
          `${motivo ? ` (${motivo})` : ''}, por lo que tuvimos que cancelar tu reserva. No se te penaliza.`,
        'Puedes reagendarla desde la app, en Reservas > Mis reservas, tocando "Reagendar": elige otro día fuera del mantenimiento u otra cancha del mismo deporte.'
      ],
      destacado: `Reserva cancelada: ${fechaLegible(fecha)}, ${horaInicio || ''}–${horaFin || ''}`, nota: PIE
    })
  }),
  membresiaReactivada: ({ nombre, hasta }) => ({
    asunto: 'Tu membresía está activa de nuevo',
    ...plantilla({
      nombre, titulo: 'Membresía reactivada',
      parrafos: ['Reactivamos tu membresía. Ya puedes volver a reservar.'],
      destacado: hasta ? `Vigente hasta el ${fechaLegible(hasta)}` : undefined, nota: PIE
    })
  })
};

/**
 * Decide qué correos genera un cambio en una membresía (antes -> después). Pura: no toca la base.
 * @returns {Array<{tipo: string, datos: object}>} en el orden en que deben enviarse
 */
function correosPorCambioDeMembresia(antes, despues) {
  const salida = [];
  if (!antes || !despues) return salida;

  const renovada = Number(despues.renovaciones || 0) > Number(antes.renovaciones || 0);
  if (renovada) {
    salida.push({ tipo: 'renovacionAprobada', datos: { hasta: despues.fechaVencimiento } });
  } else if (antes.estado !== despues.estado) {
    if (despues.estado === 'SUSPENDIDA') {
      salida.push({ tipo: 'membresiaSuspendida', datos: { motivo: despues.motivoSuspension } });
    } else if (antes.estado === 'SUSPENDIDA' && despues.estado === 'ACTIVA') {
      salida.push({ tipo: 'membresiaReactivada', datos: { hasta: despues.fechaVencimiento } });
    }
  }

  const cambioPlan = despues.tipo === 'INDIVIDUAL' && antes.plan && despues.plan && antes.plan !== despues.plan;
  const porSolicitud = despues.cambioPlanSolicitudId && despues.cambioPlanSolicitudId !== antes.cambioPlanSolicitudId;
  // Si el cambio vino de una solicitud, el correo de "aprobado" sale con la solicitud (trae el contexto completo).
  if (cambioPlan && !porSolicitud) {
    salida.push({ tipo: 'planCambiadoPorElClub', datos: { planAnterior: antes.plan, planNuevo: despues.plan, precioNuevo: despues.precio } });
  }
  return salida;
}

/**
 * Manda un correo y deja constancia en `correos_enviados/{id}`. `id` (el id del evento) hace idempotente el envío:
 * si el evento se reintenta, no se vuelve a mandar. Sin transporte (sin SMTP configurado, o emulador) queda "SIMULADO".
 * @param {object} p {db(Firestore admin), id, transporte|null, de, para, correo:{asunto,texto,html}, tipo, meta}
 */
async function despachar({ db, id, transporte, de, para, correo, tipo, meta = {} }) {
  const ref = db.collection('correos_enviados').doc(id);
  try {
    await ref.create({ estado: 'PROCESANDO', tipo, para: para || '', asunto: correo.asunto, ...meta, creadoEn: new Date() });
  } catch (e) {
    if (e.code === 6 || /already exists/i.test(e.message || '')) return { estado: 'DUPLICADO' };
    throw e;
  }
  if (!para) { await ref.update({ estado: 'SIN_CORREO' }); return { estado: 'SIN_CORREO' }; }
  if (!transporte) { await ref.update({ estado: 'SIMULADO', texto: correo.texto }); return { estado: 'SIMULADO' }; }
  try {
    await transporte.sendMail({ from: de, to: para, subject: correo.asunto, text: correo.texto, html: correo.html });
    await ref.update({ estado: 'ENVIADO', enviadoEn: new Date() });
    return { estado: 'ENVIADO' };
  } catch (e) {
    await ref.update({ estado: 'ERROR', error: String(e.code || e.message || 'error').slice(0, 120) });
    return { estado: 'ERROR' };
  }
}

module.exports = {
  NOMBRES_PLAN, NOMBRES_PAQUETE, nombrePlan, dinero, fechaLegible, esCorreo, esCorreoReal, elegirCorreo,
  compositores, correosPorCambioDeMembresia, despachar
};
