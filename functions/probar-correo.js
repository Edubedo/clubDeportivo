// Manda UN correo de bienvenida real, sin Firebase, para comprobar tus credenciales SMTP.
//   SMTP_USER=tucorreo@gmail.com SMTP_PASS='contraseña de aplicación' node probar-correo.js ezescobedo27@gmail.com
// Opcionales: SMTP_HOST (smtp.gmail.com), SMTP_PORT (465), SMTP_REMITENTE, ENLACE_BASE.
const { crearTransporte, enviarBienvenida } = require('./bienvenida');
const { SMTP_USER, SMTP_PASS } = process.env;
const destino = process.argv[2];
if (!SMTP_USER || !SMTP_PASS || !destino) {
  console.error('Uso: SMTP_USER=... SMTP_PASS=... node probar-correo.js correo@destino.com');
  process.exit(1);
}
const transporte = crearTransporte({ host: process.env.SMTP_HOST || 'smtp.gmail.com', port: process.env.SMTP_PORT || 465, user: SMTP_USER, pass: SMTP_PASS });
enviarBienvenida({
  transporte,
  de: process.env.SMTP_REMITENTE || `Athletic Club <${SMTP_USER}>`,
  baseEnlace: process.env.ENLACE_BASE || 'https://clubdeportivoudc.web.app/unirse',
  miembro: { nombre: 'Eduardo Escobedo', correo: destino },
  codigo: 'CLB-7K3M9Q' // código de ejemplo: no existe en la base
}).then((r) => console.log(r.enviado ? `Enviado a ${destino}. Revisa la bandeja (y spam).` : `No se envió: ${r.motivo}`))
  .catch((e) => { console.error('Falló el envío:', e.code || '', e.message); process.exit(1); });
