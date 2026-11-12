const { onCall, HttpsError } = require('firebase-functions/v2/https');
const { initializeApp } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
const { onDocumentCreated } = require('firebase-functions/v2/firestore');
const { defineString, defineSecret } = require('firebase-functions/params');
const { primerError } = require('./contrasena');
const { crearTransporte, enviarBienvenida } = require('./bienvenida');

initializeApp();

const ROLES_ADMINISTRADOR = ['SUPERADMIN', 'ADMIN'];
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

// ---------- Correo de bienvenida ----------
// Servicio de correo: cualquier SMTP gratuito (Gmail con contraseña de aplicación, Brevo...). Ver docs/correo-de-bienvenida.md.
const SMTP_HOST = defineString('SMTP_HOST', { default: 'smtp.gmail.com' });
const SMTP_PORT = defineString('SMTP_PORT', { default: '465' });
const SMTP_USER = defineString('SMTP_USER', { default: '' });
const SMTP_REMITENTE = defineString('SMTP_REMITENTE', { default: '' }); // "Athletic Club <correo@dominio>"; vacío = SMTP_USER
const ENLACE_BASE = defineString('ENLACE_BASE', { default: '' }); // vacío = https://<proyecto>.web.app/unirse
const SMTP_PASS = defineSecret('SMTP_PASS');

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
