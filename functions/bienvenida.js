// Correo de bienvenida de los miembros nuevos: su código de acceso y un enlace que abre la app.
// Sin dependencias de Firebase para poder probarlo solo (ver bienvenida.test.js).

const nodemailer = require('nodemailer');

const ALFABETO = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789'; // el mismo que ReglasMembresia.kt (sin 0/O ni 1/I/L)
const REGEX_CODIGO = new RegExp(`^CLB-[${ALFABETO}]{6}$`);
const REGEX_CORREO = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

const esCodigo = (c) => typeof c === 'string' && REGEX_CODIGO.test(c);
const esCorreo = (c) => typeof c === 'string' && REGEX_CORREO.test(c.trim());

const escapar = (texto) =>
  String(texto).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);

/** Enlace https que va en el correo: la página de hosting lo redirige a clubdeportivo://registro?codigo=... */
function enlaceDeBienvenida(base, codigo) {
  const url = new URL(base);
  url.searchParams.set('codigo', codigo);
  return url.toString();
}

/** Arma el correo (asunto, texto y html). No lo envía. */
function componerBienvenida({ nombre, codigo, enlace, club = 'Athletic Club' }) {
  const primerNombre = String(nombre || '').trim().split(/\s+/)[0] || '';
  const saludo = primerNombre ? `Hola ${primerNombre}` : 'Hola';
  const saludoHtml = escapar(saludo);
  const e = escapar(enlace);
  const asunto = `Tu código de acceso a ${club}: ${codigo}`;

  const texto = [
    `${saludo},`,
    '',
    `Te damos la bienvenida a ${club}. Ya eres miembro.`,
    '',
    `Tu código de acceso: ${codigo}`,
    '',
    'Para crear tu cuenta abre este enlace desde tu teléfono Android (con la app instalada):',
    enlace,
    '',
    'Si el enlace no abre la app, entra a "Regístrate con tu código" y escribe el código a mano.',
    'El código sirve una sola vez, para crear tu cuenta. Después entras con tu correo y contraseña.',
    '',
    'Si no esperabas este correo, ignóralo.'
  ].join('\n');

  const html = `<!doctype html>
<html lang="es"><body style="margin:0;padding:24px;background:#f5f3f0;font-family:Arial,Helvetica,sans-serif;color:#2b2622">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0"><tr><td align="center">
<table role="presentation" width="480" cellpadding="0" cellspacing="0" style="max-width:480px;background:#ffffff;border-radius:12px;padding:28px">
<tr><td>
<h1 style="margin:0 0 12px;font-size:22px;color:#e8590c">${escapar(club)}</h1>
<p style="margin:0 0 16px;font-size:16px">${saludoHtml}, te damos la bienvenida. Ya eres miembro.</p>
<p style="margin:0 0 6px;font-size:14px;color:#6b625a">Tu código de acceso</p>
<p style="margin:0 0 20px;font-size:28px;font-weight:bold;letter-spacing:3px;font-family:Consolas,Menlo,monospace">${escapar(codigo)}</p>
<p style="margin:0 0 20px"><a href="${e}" style="display:inline-block;background:#e8590c;color:#ffffff;text-decoration:none;font-weight:bold;padding:12px 22px;border-radius:8px">Crear mi cuenta</a></p>
<p style="margin:0 0 8px;font-size:13px;color:#6b625a">Ábrelo desde tu teléfono Android con la app instalada. Si no abre la app, entra a <b>Regístrate con tu código</b> y escribe el código a mano.</p>
<p style="margin:0 0 8px;font-size:13px;color:#6b625a">El código sirve una sola vez, para crear tu cuenta. Después entras con tu correo y contraseña.</p>
<p style="margin:16px 0 0;font-size:12px;color:#9a9188">Si el botón no funciona, copia este enlace: ${e}<br>Si no esperabas este correo, ignóralo.</p>
</td></tr></table></td></tr></table></body></html>`;

  return { asunto, texto, html };
}

function crearTransporte({ host, port, user, pass }) {
  const puerto = Number(port) || 465;
  return nodemailer.createTransport({ host, port: puerto, secure: puerto === 465, auth: { user, pass } });
}

/**
 * Manda la bienvenida a un miembro. `transporte` es un transporte de nodemailer (se inyecta para poder probar).
 * @returns {{enviado: boolean, motivo?: string}}
 */
async function enviarBienvenida({ transporte, de, baseEnlace, miembro, codigo, club }) {
  if (!esCodigo(codigo)) return { enviado: false, motivo: 'codigo-invalido' };
  const correo = String(miembro?.correo || '').trim();
  if (!correo) return { enviado: false, motivo: 'sin-correo' };
  if (!esCorreo(correo)) return { enviado: false, motivo: 'correo-invalido' };

  const enlace = enlaceDeBienvenida(baseEnlace, codigo);
  const { asunto, texto, html } = componerBienvenida({ nombre: miembro.nombre, codigo, enlace, club });
  await transporte.sendMail({ from: de, to: correo, subject: asunto, text: texto, html });
  return { enviado: true };
}

module.exports = { esCodigo, esCorreo, enlaceDeBienvenida, componerBienvenida, crearTransporte, enviarBienvenida };
