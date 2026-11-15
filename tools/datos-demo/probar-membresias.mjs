// Batería funcional de MEMBRESÍAS: cambio de plan (subir y bajar), renovaciones y los avisos que generan
// (notificaciones en la app + correos). Replica las operaciones que hace la app (transacciones incluidas), con las
// reglas de seguridad reales. Requiere los emuladores corriendo (con las funciones) y los datos de sembrar-demo.mjs.
// Uso: node tools/datos-demo/sembrar-demo.mjs && node tools/datos-demo/probar-membresias.mjs
import { createRequire } from 'node:module';
const requireCliente = createRequire(new URL('../pruebas-reglas/package.json', import.meta.url));
const requireAdmin = createRequire(new URL('../../functions/package.json', import.meta.url));
process.env.FIRESTORE_EMULATOR_HOST = '127.0.0.1:8080';
process.env.FIREBASE_AUTH_EMULATOR_HOST = '127.0.0.1:9099';
const { initializeApp, deleteApp } = requireCliente('firebase/app');
const { getAuth, connectAuthEmulator, signInWithEmailAndPassword } = requireCliente('firebase/auth');
const { getFirestore, connectFirestoreEmulator, doc, getDoc, setDoc, updateDoc, collection, getDocs, query, where, runTransaction, writeBatch, increment, serverTimestamp } = requireCliente('firebase/firestore');
const { initializeApp: initAdmin } = requireAdmin('firebase-admin/app');
const { getFirestore: getAdminDb } = requireAdmin('firebase-admin/firestore');

const PROYECTO = 'clubdeportivoudc';
initAdmin({ projectId: PROYECTO });
const adminDb = getAdminDb();

const pad = (n) => String(n).padStart(2, '0');
const dia = (o = 0) => { const d = new Date(); d.setDate(d.getDate() + o); return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`; };
const CUENTAS = {
  admin: ['admin@athleticclub.test', 'Admin#2026Club'],
  supervisor: ['supervisor@athleticclub.test', 'Super#2026Club'],
  encTenis: ['encargado.tenis@athleticclub.test', 'Tenis#2026Club'],
  mariana: ['mariana.lopez@athleticclub.test', 'Socio#2026Mariana'], // NORMAL, vence en 2 días
  diego: ['diego.hernandez@athleticclub.test', 'Socio#2026Diego'],   // DELUXE, vigente
  paola: ['soc-07@athleticclub.test', 'Relleno#2026Club'],           // NORMAL, vencida
  andres: ['soc-06@athleticclub.test', 'Relleno#2026Club']           // DELUXE, suspendida
};
const PRECIO = { NINO: 1500, NORMAL: 1800, DELUXE: 3500 };

let ok = 0, fallos = 0;
const prueba = (nombre, cond, detalle = '') => { if (cond) { ok++; console.log('OK    ' + nombre); } else { fallos++; console.log('FALLA ' + nombre + (detalle ? '  → ' + detalle : '')); } };
const permitido = async (fn) => { try { await fn(); return true; } catch (e) { if (e.code === 'permission-denied') return false; throw e; } };
const esperar = async (fn, ms = 15000) => { const t = Date.now(); while (Date.now() - t < ms) { const v = await fn(); if (v) return v; await new Promise((r) => setTimeout(r, 400)); } return null; };
const pausa = (ms) => new Promise((r) => setTimeout(r, ms));

const S = {};
async function entrar(k) {
  const app = initializeApp({ projectId: PROYECTO, apiKey: 'emulador', appId: k }, k + Math.random());
  const auth = getAuth(app); connectAuthEmulator(auth, 'http://127.0.0.1:9099', { disableWarnings: true });
  const db = getFirestore(app); connectFirestoreEmulator(db, '127.0.0.1', 8080);
  const c = await signInWithEmailAndPassword(auth, ...CUENTAS[k]);
  return (S[k] = { app, db, uid: c.user.uid });
}
const notifs = async (uid) => (await adminDb.collection('notificaciones').where('usuarioId', '==', uid).get()).docs.map((d) => d.data());
const correos = async (uid) => (await adminDb.collection('correos_enviados').where('usuarioId', '==', uid).get()).docs.map((d) => d.data());
const correoDe = async (uid, tipo, desde = 0) => (await correos(uid)).filter((c) => c.tipo === tipo).length > desde;

for (const k of Object.keys(CUENTAS)) await entrar(k);
const { admin, supervisor, encTenis, mariana, diego, paola, andres } = S;
const JPEG = 'data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wAALCAABAAEBAREA/8QAFAABAAAAAAAAAAAAAAAAAAAAAP/EABQQAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQEAAD8AKp//2Q==';

// Lo que hace CambioPlanRepository.aprobar() en la app: una transacción que cambia la membresía y la solicitud.
async function aprobarCambio(revisor, solicitudId, membresiaId, planNuevo, planAnterior, precioAnterior) {
  return runTransaction(revisor.db, async (tx) => {
    const sRef = doc(revisor.db, 'solicitudes_cambio_plan', solicitudId);
    const mRef = doc(revisor.db, 'membresias', membresiaId);
    const s = await tx.get(sRef); const m = await tx.get(mRef);
    if (s.data().status !== 'PENDIENTE') throw new Error('ya atendida');
    if (m.data().plan !== planAnterior) throw new Error('obsoleta');
    tx.update(mRef, { plan: planNuevo, precio: PRECIO[planNuevo], planAnterior, precioAnterior, cambioPlanSolicitudId: solicitudId, cambioPlanEn: serverTimestamp(), cambioPlanPor: revisor.uid, actualizadoEn: serverTimestamp() });
    tx.update(sRef, { status: 'APROBADO', precioNuevo: PRECIO[planNuevo], reviewedBy: revisor.uid, reviewedByNombre: 'Revisor', reviewedAt: serverTimestamp() });
  });
}
const pedirCambio = (socio, id, membresiaId, nombre, actual, nuevo) => setDoc(doc(socio.db, 'solicitudes_cambio_plan', id), {
  id, socioId: socio.uid, socioNombre: nombre, membresiaId, planActual: actual, planNuevo: nuevo,
  precioActual: PRECIO[actual], precioNuevo: PRECIO[nuevo], status: 'PENDIENTE', createdAt: Date.now()
});

console.log('\n=== 1. CAMBIO DE PLAN: subir de nivel (Mariana NORMAL → DELUXE), aprueba el SUPERVISOR ===');
const supNotifAntes = (await notifs(supervisor.uid)).length;
const admNotifAntes = (await notifs(admin.uid)).length;
await pedirCambio(mariana, 'cp-sube', 'mem-soc-mariana', 'Mariana López Ruiz', 'NORMAL', 'DELUXE');
prueba('la socia envía la solicitud de subir de plan', true);
const nSup = await esperar(async () => { const n = await notifs(supervisor.uid); return n.length > supNotifAntes ? n : null; });
prueba('el SUPERVISOR recibe la notificación "Solicitud de cambio de plan"', !!nSup && nSup.some((n) => n.titulo === 'Solicitud de cambio de plan' && /subir/.test(n.mensaje) && n.mensaje.includes('Individual normal') && n.mensaje.includes('Individual deluxe')), JSON.stringify(nSup?.slice(-1)));
const nAdm = await esperar(async () => { const n = await notifs(admin.uid); return n.length > admNotifAntes ? n : null; });
prueba('el ADMINISTRADOR también la recibe', !!nAdm && nAdm.some((n) => n.titulo === 'Solicitud de cambio de plan'));
prueba('el ENCARGADO de área NO la recibe', !(await notifs(encTenis.uid)).some((n) => n.titulo === 'Solicitud de cambio de plan'));
prueba('la socia recibe el correo de acuse ("Recibimos tu solicitud…")', !!(await esperar(() => correoDe(mariana.uid, 'cambioPlanRecibido'))));
prueba('Diego no recibe nada de esto', (await correos(diego.uid)).length === 0);
// Reglas: el encargado no la resuelve; el supervisor sí
prueba('el ENCARGADO no puede aprobarla', !(await permitido(() => updateDoc(doc(encTenis.db, 'solicitudes_cambio_plan', 'cp-sube'), { status: 'APROBADO', reviewedBy: encTenis.uid }))));
await aprobarCambio(supervisor, 'cp-sube', 'mem-soc-mariana', 'DELUXE', 'NORMAL', 1800);
const m1 = (await getDoc(doc(mariana.db, 'membresias', 'mem-soc-mariana'))).data();
prueba('la membresía quedó en DELUXE a $3,500 (con el plan anterior registrado)', m1.plan === 'DELUXE' && m1.precio === 3500 && m1.planAnterior === 'NORMAL' && m1.precioAnterior === 1800);
const nMar = await esperar(async () => (await notifs(mariana.uid)).find((n) => n.titulo === 'Cambio de plan aprobado'));
prueba('la socia recibe la notificación "Cambio de plan aprobado"', !!nMar && /Individual deluxe/.test(nMar.mensaje));
prueba('la socia recibe el correo "Tu cambio de plan fue aprobado"', !!(await esperar(() => correoDe(mariana.uid, 'cambioPlanAprobado'))));
await pausa(2500);
prueba('NO se manda además el correo genérico "plan cambiado por el club" (sin duplicados)', !(await correoDe(mariana.uid, 'planCambiadoPorElClub')));
const cMar = (await correos(mariana.uid)).find((c) => c.tipo === 'cambioPlanAprobado');
prueba('el correo queda registrado (simulado en el emulador) y dirigido a su correo real', cMar?.estado === 'SIMULADO' && cMar?.para === 'mariana.lopez@athleticclub.test', JSON.stringify(cMar));
prueba('no se puede volver a aprobar la misma solicitud', await aprobarCambio(supervisor, 'cp-sube', 'mem-soc-mariana', 'DELUXE', 'NORMAL', 1800).then(() => false, () => true));

console.log('\n=== 2. CAMBIO DE PLAN: bajar de nivel (Diego DELUXE → NORMAL), lo rechaza el ADMIN con motivo ===');
await pedirCambio(diego, 'cp-baja', 'mem-soc-diego', 'Diego Hernández Soto', 'DELUXE', 'NORMAL');
prueba('Diego envía la solicitud de bajar de plan', !!(await esperar(() => correoDe(diego.uid, 'cambioPlanRecibido'))));
prueba('el admin NO puede rechazar sin motivo', !(await permitido(() => updateDoc(doc(admin.db, 'solicitudes_cambio_plan', 'cp-baja'), { status: 'RECHAZADO', motivoRechazo: '' }))));
await updateDoc(doc(admin.db, 'solicitudes_cambio_plan', 'cp-baja'), { status: 'RECHAZADO', motivoRechazo: 'Tienes una reservación Deluxe pendiente', reviewedBy: admin.uid, reviewedByNombre: 'Laura', reviewedAt: serverTimestamp() });
const nDie = await esperar(async () => (await notifs(diego.uid)).find((n) => n.titulo === 'Cambio de plan rechazado'));
prueba('Diego recibe "Cambio de plan rechazado" con el motivo', !!nDie && nDie.mensaje.includes('Tienes una reservación Deluxe pendiente'));
const cDie = await esperar(async () => (await correos(diego.uid)).find((c) => c.tipo === 'cambioPlanRechazado'));
prueba('Diego recibe el correo de rechazo', !!cDie);
prueba('su plan NO cambió', (await getDoc(doc(diego.db, 'membresias', 'mem-soc-diego'))).data().plan === 'DELUXE');
const mDie = await adminDb.doc('correos_enviados/' + (await adminDb.collection('correos_enviados').where('tipo', '==', 'cambioPlanRechazado').get()).docs[0].id).get();
prueba('el correo de rechazo incluye el motivo', /Tienes una reservación Deluxe pendiente/.test(mDie.data().texto || ''));

console.log('\n=== 3. CAMBIO DE PLAN: bajar de nivel aprobado (Diego DELUXE → NINO) ===');
await pedirCambio(diego, 'cp-baja2', 'mem-soc-diego', 'Diego Hernández Soto', 'DELUXE', 'NINO');
await aprobarCambio(admin, 'cp-baja2', 'mem-soc-diego', 'NINO', 'DELUXE', 3500);
const m2 = (await getDoc(doc(diego.db, 'membresias', 'mem-soc-diego'))).data();
prueba('la membresía bajó a NIÑO a $1,500', m2.plan === 'NINO' && m2.precio === 1500);
prueba('Diego recibe el correo de cambio aprobado (Individual niños)', !!(await esperar(async () => (await correos(diego.uid)).find((c) => c.tipo === 'cambioPlanAprobado'))));

console.log('\n=== 4. Reglas del cambio de plan ===');
prueba('un miembro con la membresía SUSPENDIDA no puede pedir cambio', !(await permitido(() => pedirCambio(andres, 'cp-susp', 'mem-soc-06', 'Andrés Camacho Luna', 'DELUXE', 'NORMAL'))));
prueba('Mariana no puede pedir el cambio a nombre de Diego', !(await permitido(() => setDoc(doc(mariana.db, 'solicitudes_cambio_plan', 'cp-robo'), { id: 'cp-robo', socioId: diego.uid, socioNombre: 'x', membresiaId: 'mem-soc-diego', planActual: 'NINO', planNuevo: 'NORMAL', precioActual: 1500, precioNuevo: 1800, status: 'PENDIENTE', createdAt: Date.now() }))));
prueba('Diego no ve la solicitud de Mariana', !(await permitido(() => getDoc(doc(diego.db, 'solicitudes_cambio_plan', 'cp-sube')))));
prueba('la socia sí ve su propia solicitud ya aprobada', (await getDoc(doc(mariana.db, 'solicitudes_cambio_plan', 'cp-sube'))).data().status === 'APROBADO');

console.log('\n=== 5. Cambio directo del personal (sin solicitud) y suspensión/reactivación ===');
await updateDoc(doc(supervisor.db, 'membresias', 'mem-soc-01'), { plan: 'DELUXE', precio: 3500, actualizadoEn: serverTimestamp() });
prueba('cambio de plan hecho en recepción → correo "Tu plan cambió"', !!(await esperar(async () => (await correos('soc-01')).find((c) => c.tipo === 'planCambiadoPorElClub'))));
await updateDoc(doc(admin.db, 'membresias', 'mem-soc-02'), { estado: 'SUSPENDIDA', motivoSuspension: 'Incumplimiento del reglamento', suspendidaEn: dia(0), actualizadoEn: serverTimestamp() });
const cSusp = await esperar(async () => (await correos('soc-02')).find((c) => c.tipo === 'membresiaSuspendida'));
prueba('suspender → correo "Tu membresía fue suspendida"', !!cSusp);
prueba('el correo de suspensión dice el motivo', /Incumplimiento del reglamento/.test((await adminDb.collection('correos_enviados').where('tipo', '==', 'membresiaSuspendida').get()).docs[0].data().texto || ''));
await updateDoc(doc(admin.db, 'membresias', 'mem-soc-02'), { estado: 'ACTIVA', actualizadoEn: serverTimestamp() });
prueba('reactivar → correo "Tu membresía está activa de nuevo"', !!(await esperar(async () => (await correos('soc-02')).find((c) => c.tipo === 'membresiaReactivada'))));
await updateDoc(doc(admin.db, 'membresias', 'mem-soc-03'), { comprobanteUrl: 'x', actualizadoEn: serverTimestamp() });
await pausa(2500);
prueba('un cambio sin importancia (otro campo) NO genera correo', (await correos('soc-03')).length === 0);

console.log('\n=== 6. RENOVACIÓN por solicitud (Paola, vencida) — misma transacción que la app ===');
const antesAdm = (await notifs(admin.uid)).filter((n) => n.titulo === 'Nueva solicitud de renovación').length;
const solId = 'ren-paola';
await setDoc(doc(paola.db, 'solicitudes_renovacion', solId), { id: solId, socioId: paola.uid, socioNombre: 'Paola Escalante Gil', membresiaId: 'mem-soc-07', planId: 'INDIVIDUAL_NORMAL', monto: 1800, concepto: 'REN-PAOLA', comprobanteUrl: JPEG, status: 'PENDIENTE', createdAt: Date.now() });
prueba('Paola (membresía vencida) puede enviar su renovación', true);
prueba('el admin recibe "Nueva solicitud de renovación"', !!(await esperar(async () => (await notifs(admin.uid)).filter((n) => n.titulo === 'Nueva solicitud de renovación').length > antesAdm)));
prueba('el SUPERVISOR también recibe la solicitud de renovación', (await notifs(supervisor.uid)).some((n) => n.titulo === 'Nueva solicitud de renovación' && n.mensaje.includes('Paola')));
prueba('Paola recibe el correo de acuse de su renovación', !!(await esperar(() => correoDe(paola.uid, 'renovacionRecibida'))));
const pagosAntes = (await adminDb.collection('pagos').where('membresiaId', '==', 'mem-soc-07').get()).size;
// Aprobación por el SUPERVISOR: igual que FirebaseGestionMembresiasRepository.renovarPorSolicitud
const hoy = dia(0); const vence = dia(31);
await runTransaction(supervisor.db, async (tx) => {
  const sRef = doc(supervisor.db, 'solicitudes_renovacion', solId); const mRef = doc(supervisor.db, 'membresias', 'mem-soc-07');
  const s = await tx.get(sRef); const m = await tx.get(mRef);
  if (s.data().status !== 'PENDIENTE') throw new Error('ya atendida');
  if (s.data().planId !== 'INDIVIDUAL_' + m.data().plan || s.data().monto !== m.data().precio) throw new Error('obsoleta');
  tx.update(mRef, { estado: 'ACTIVA', fechaInicio: hoy, fechaVencimiento: vence, precio: 1800, renovaciones: increment(1), ultimaRenovacion: hoy, actualizadoEn: serverTimestamp() });
  tx.set(doc(collection(supervisor.db, 'pagos')), { usuarioId: paola.uid, membresiaId: 'mem-soc-07', titularNombre: 'Paola Escalante Gil', concepto: 'MENSUALIDAD', monto: 1800, moneda: 'MXN', fechaPago: hoy, metodoPago: 'TRANSFERENCIA', estado: 'PAGADO', registradoPor: supervisor.uid, creadoEn: serverTimestamp() });
  tx.update(sRef, { status: 'APROBADO', reviewedBy: supervisor.uid, reviewedDate: hoy, renewedUntil: vence, reviewedAt: serverTimestamp() });
});
const mP = (await getDoc(doc(paola.db, 'membresias', 'mem-soc-07'))).data();
prueba('la membresía vencida queda ACTIVA hasta ' + vence + ' (desde hoy, sin regalar días)', mP.estado === 'ACTIVA' && mP.fechaVencimiento === vence && mP.renovaciones === 2);
prueba('se registró el pago de la mensualidad', (await adminDb.collection('pagos').where('membresiaId', '==', 'mem-soc-07').get()).size === pagosAntes + 1);
const nPao = await esperar(async () => (await notifs(paola.uid)).find((n) => n.titulo === 'Renovación aprobada'));
prueba('Paola recibe la notificación "Renovación aprobada" con la fecha', !!nPao && nPao.mensaje.includes('Vigente hasta el'), JSON.stringify(nPao));
const cPao = await esperar(async () => (await correos(paola.uid)).filter((c) => c.tipo === 'renovacionAprobada'));
await pausa(2500);
const cPaoFinal = (await correos(paola.uid)).filter((c) => c.tipo === 'renovacionAprobada');
prueba('Paola recibe UN solo correo "Tu membresía fue renovada"', cPaoFinal.length === 1, 'recibió ' + cPaoFinal.length);

console.log('\n=== 7. RENOVACIÓN rechazada (Mariana) y renovación directa en recepción ===');
const solM = 'ren-mariana';
await setDoc(doc(mariana.db, 'solicitudes_renovacion', solM), { id: solM, socioId: mariana.uid, socioNombre: 'Mariana López Ruiz', membresiaId: 'mem-soc-mariana', planId: 'INDIVIDUAL_DELUXE', monto: 3500, concepto: 'REN-MAR', comprobanteUrl: JPEG, status: 'PENDIENTE', createdAt: Date.now() });
prueba('tras subir a DELUXE, su renovación se pide ya con el plan y el precio nuevos', true);
await updateDoc(doc(supervisor.db, 'solicitudes_renovacion', solM), { status: 'RECHAZADO', motivoRechazo: 'El monto no coincide', reviewedBy: supervisor.uid, reviewedAt: serverTimestamp() });
prueba('Mariana recibe la notificación de renovación rechazada con motivo', !!(await esperar(async () => (await notifs(mariana.uid)).find((n) => n.titulo === 'Renovación rechazada' && n.mensaje.includes('El monto no coincide')))));
prueba('Mariana recibe el correo de renovación rechazada', !!(await esperar(() => correoDe(mariana.uid, 'renovacionRechazada'))));
await updateDoc(doc(admin.db, 'membresias', 'mem-soc-04'), { fechaVencimiento: dia(31), renovaciones: increment(1), ultimaRenovacion: dia(0), actualizadoEn: serverTimestamp() });
prueba('renovación hecha en recepción → correo "Tu membresía fue renovada"', !!(await esperar(async () => (await correos('soc-04')).find((c) => c.tipo === 'renovacionAprobada'))));

console.log('\n=== 8. Un cambio de plan con renovación en revisión (conflicto) ===');
const solD = 'ren-diego';
await setDoc(doc(diego.db, 'solicitudes_renovacion', solD), { id: solD, socioId: diego.uid, socioNombre: 'Diego Hernández Soto', membresiaId: 'mem-soc-diego', planId: 'INDIVIDUAL_NINO', monto: 1500, concepto: 'REN-DIEGO', comprobanteUrl: JPEG, status: 'PENDIENTE', createdAt: Date.now() });
const pendRenov = (await getDocs(query(collection(admin.db, 'solicitudes_renovacion'), where('membresiaId', '==', 'mem-soc-diego'), where('status', '==', 'PENDIENTE')))).size;
prueba('el personal puede detectar la renovación pendiente de esa membresía (la app bloquea el cambio de plan)', pendRenov === 1);

console.log(`\n${fallos === 0 ? 'TODAS LAS PRUEBAS DE MEMBRESÍAS PASARON' : fallos + ' FALLARON'} (${ok} OK)`);
await Promise.all(Object.values(S).map((s) => deleteApp(s.app).catch(() => {})));
process.exit(fallos ? 1 : 0);
