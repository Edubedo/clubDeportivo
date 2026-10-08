const { onCall, HttpsError } = require('firebase-functions/v2/https');
const { initializeApp } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
const { primerError } = require('./contrasena');

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
