const { onCall, HttpsError } = require('firebase-functions/v2/https');
const { onDocumentCreated, onDocumentUpdated } = require('firebase-functions/v2/firestore');
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

exports.notificarNuevaRenovacion = onDocumentCreated(
  { document: 'solicitudes_renovacion/{solicitudId}', region: 'us-central1' },
  async (event) => {
    const solicitud = event.data?.data();
    if (!solicitud || solicitud.status !== 'PENDIENTE') return;

    const db = getFirestore();
    const administradores = await db.collection('usuarios')
      .where('rol', 'in', ROLES_ADMINISTRADOR)
      .get();
    await Promise.all(administradores.docs
      .filter((admin) => (admin.get('estado') || 'ACTIVO') !== 'INACTIVO')
      .map((admin) => guardarNotificacion(
        admin.id,
        `renovacion_${event.params.solicitudId}_${admin.id}`,
        'Nueva solicitud de renovación',
        `${solicitud.socioNombre || 'Un socio'} envió una renovación por $${Number(solicitud.monto || 0).toFixed(2)}. Revisa la pestaña Renovaciones.`,
        'RENOVACION'
      )));
  }
);

exports.notificarResolucionRenovacion = onDocumentUpdated(
  { document: 'solicitudes_renovacion/{solicitudId}', region: 'us-central1' },
  async (event) => {
    const anterior = event.data?.before.data();
    const actual = event.data?.after.data();
    if (!anterior || !actual || anterior.status !== 'PENDIENTE' ||
        !['APROBADO', 'RECHAZADO'].includes(actual.status) || !actual.socioId) return;

    const aprobado = actual.status === 'APROBADO';
    await guardarNotificacion(
      actual.socioId,
      `renovacion_${event.params.solicitudId}_${actual.status.toLowerCase()}`,
      aprobado ? 'Renovación aprobada' : 'Renovación rechazada',
      aprobado
        ? 'Tu renovación de membresía fue aprobada. Tu vigencia ya está actualizada.'
        : 'Tu solicitud de renovación fue rechazada. Comunícate con el personal del club para más información.',
      'RENOVACION'
    );
  }
);
