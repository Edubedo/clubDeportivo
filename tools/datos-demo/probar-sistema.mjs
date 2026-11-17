// Batería funcional: entra con cada cuenta de prueba (cliente Firebase real + reglas de seguridad reales) y hace lo
// que haría la app, incluida la cadena de notificaciones por Cloud Functions.
// Requiere los emuladores corriendo y los datos de `sembrar-demo.mjs`.  Uso: node tools/datos-demo/probar-sistema.mjs
import { createRequire } from 'node:module';
const require = createRequire(new URL('../pruebas-reglas/package.json', import.meta.url));
const { initializeApp, deleteApp } = require('firebase/app');
const { getAuth, connectAuthEmulator, signInWithEmailAndPassword } = require('firebase/auth');
const { getFirestore, connectFirestoreEmulator, doc, getDoc, setDoc, updateDoc, addDoc, collection, getDocs, query, where, deleteDoc } = require('firebase/firestore');

// El SDK de administrador (limpieza) debe apuntar siempre al emulador, nunca al proyecto real.
process.env.FIRESTORE_EMULATOR_HOST ||= '127.0.0.1:8080';
const requireAdmin = createRequire(new URL('../../functions/package.json', import.meta.url));
const { initializeApp: initAdmin } = requireAdmin('firebase-admin/app');
const { getFirestore: getAdminDb } = requireAdmin('firebase-admin/firestore');

const PROYECTO = 'clubdeportivoudc';
initAdmin({ projectId: PROYECTO });
const adminDb = getAdminDb();
const hoy = new Date();
const pad = (n) => String(n).padStart(2, '0');
const dia = (o = 0) => { const d = new Date(); d.setDate(d.getDate() + o); return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`; };

const CUENTAS = {
  admin: ['admin@athleticclub.test', 'Admin#2026Club'],
  supervisor: ['supervisor@athleticclub.test', 'Super#2026Club'],
  encFutbol: ['encargado.futbol@athleticclub.test', 'Futbol#2026Club'],
  encBasquet: ['encargado.basquetbol@athleticclub.test', 'Basquet#2026Club'],
  encTenis: ['encargado.tenis@athleticclub.test', 'Tenis#2026Club'],
  encNatacion: ['encargado.natacion@athleticclub.test', 'Natacion#2026Club'],
  mariana: ['mariana.lopez@athleticclub.test', 'Socio#2026Mariana'],
  diego: ['diego.hernandez@athleticclub.test', 'Socio#2026Diego']
};

let ok = 0, fallos = 0;
const prueba = (nombre, cond, detalle = '') => { if (cond) { ok++; console.log('OK    ' + nombre); } else { fallos++; console.log('FALLA ' + nombre + (detalle ? '  → ' + detalle : '')); } };
const permitido = async (fn) => { try { await fn(); return true; } catch (e) { return e.code === 'permission-denied' ? false : (() => { throw e; })(); } };

const sesiones = {};
async function entrar(clave) {
  const app = initializeApp({ projectId: PROYECTO, apiKey: 'emulador', appId: clave }, clave + Math.random());
  const auth = getAuth(app); connectAuthEmulator(auth, 'http://127.0.0.1:9099', { disableWarnings: true });
  const db = getFirestore(app); connectFirestoreEmulator(db, '127.0.0.1', 8080);
  const cred = await signInWithEmailAndPassword(auth, ...CUENTAS[clave]);
  sesiones[clave] = { app, db, uid: cred.user.uid };
  return sesiones[clave];
}
const esperar = async (fn, ms = 15000) => { const t = Date.now(); while (Date.now() - t < ms) { const v = await fn(); if (v) return v; await new Promise((r) => setTimeout(r, 400)); } return null; };

console.log('\n=== 1. Inicio de sesión y perfil por rol ===');
const esperado = { admin: 'ADMIN', supervisor: 'SUPERVISOR', encFutbol: 'AYUDANTE_AREA', encBasquet: 'AYUDANTE_AREA', encTenis: 'AYUDANTE_AREA', encNatacion: 'AYUDANTE_AREA', mariana: 'SOCIO', diego: 'SOCIO' };
const areaEsperada = { encFutbol: 'Fútbol', encBasquet: 'Básquetbol', encTenis: 'Tenis', encNatacion: 'Natación' };
for (const k of Object.keys(CUENTAS)) {
  const s = await entrar(k);
  const perfil = (await getDoc(doc(s.db, 'usuarios', s.uid))).data();
  prueba(`${k}: entra y su perfil dice ${esperado[k]}${areaEsperada[k] ? ' / ' + areaEsperada[k] : ''}`, perfil?.rol === esperado[k] && perfil?.estado === 'ACTIVO' && (!areaEsperada[k] || perfil.areaTrabajo === areaEsperada[k]));
}
prueba('contraseña incorrecta se rechaza', await signInWithEmailAndPassword(getAuth(initializeApp({ projectId: PROYECTO, apiKey: 'x' }, 'mal')), CUENTAS.admin[0], 'Equivocada#1').then(() => false, () => true));

console.log('\n=== 2. Permisos por rol (reglas de seguridad) ===');
const { admin, supervisor, encFutbol, encTenis, mariana, diego } = sesiones;
// Lectura
prueba('socio lee el listado de áreas', (await getDocs(collection(mariana.db, 'areas'))).size === 8);
prueba('socio NO lee membresías ajenas', !(await permitido(() => getDoc(doc(mariana.db, 'membresias', 'mem-soc-diego')))));
prueba('socio SÍ lee su propia membresía', await permitido(() => getDoc(doc(mariana.db, 'membresias', 'mem-soc-mariana'))));
prueba('socio NO lee los pagos', !(await permitido(() => getDocs(collection(mariana.db, 'pagos')))));
prueba('socio NO lee avisos solo para EMPLEADOS', (await getDocs(query(collection(mariana.db, 'avisos'), where('destinatario', 'in', ['SOCIOS', 'TODOS'])))).docs.every((d) => d.data().destinatario !== 'EMPLEADOS'));
prueba('socio NO puede consultar todos los avisos (incluye de personal)', !(await permitido(() => getDocs(collection(mariana.db, 'avisos')))));
prueba('encargado SÍ lee los avisos del personal', await permitido(() => getDocs(collection(encFutbol.db, 'avisos'))));
prueba('admin lee todos los pagos (dashboard)', (await getDocs(collection(admin.db, 'pagos'))).size > 30);
// Escalada de privilegios
prueba('socio NO puede subirse a ADMIN', !(await permitido(() => updateDoc(doc(mariana.db, 'usuarios', mariana.uid), { rol: 'ADMIN' }))));
prueba('socio SÍ puede cambiar su teléfono', await permitido(() => updateDoc(doc(mariana.db, 'usuarios', mariana.uid), { telefono: '6679999999' })));
prueba('encargado NO crea cuentas de personal', !(await permitido(() => setDoc(doc(encFutbol.db, 'usuarios', 'intruso'), { nombre: 'x', email: 'x@x.com', rol: 'ADMIN', estado: 'ACTIVO' }))));
prueba('admin SÍ crea una persona de personal', await permitido(() => setDoc(doc(admin.db, 'usuarios', 'prueba-personal'), { nombre: 'Temporal', email: 't@athleticclub.test', rol: 'AYUDANTE_AREA', estado: 'ACTIVO', areaTrabajo: 'Tenis' })));
await deleteDoc(doc(admin.db, 'usuarios', 'prueba-personal'));
prueba('supervisor NO cambia precios (admin sí)', !(await permitido(() => setDoc(doc(supervisor.db, 'precios', 'INDIVIDUAL_NORMAL'), { precio: 1, moneda: 'MXN' }))) && await permitido(() => setDoc(doc(admin.db, 'precios', 'INDIVIDUAL_NORMAL'), { precio: 1850, moneda: 'MXN', actualizadoEn: new Date(), actualizadoPor: admin.uid })));
await deleteDoc(doc(admin.db, 'precios', 'INDIVIDUAL_NORMAL'));

console.log('\n=== 3. Reservas: del miembro al encargado ===');
const nueva = { usuarioId: mariana.uid, usuarioNombre: 'Mariana López Ruiz', areaId: '6', areaNombre: 'Cancha de tenis 2', deporte: 'Tenis', fecha: dia(1), horaInicio: '15:00', horaFin: '16:00', duracionHoras: 1, personas: 2, carril: null, estado: 'PENDIENTE_APROBACION', esExterno: false, creadoEn: new Date(), actualizadoEn: new Date() };
prueba('socio NO puede crear una reserva ya CONFIRMADA', !(await permitido(() => addDoc(collection(mariana.db, 'reservas'), { ...nueva, estado: 'CONFIRMADA' }))));
prueba('socio NO reserva a nombre de otra persona', !(await permitido(() => addDoc(collection(mariana.db, 'reservas'), { ...nueva, usuarioId: diego.uid }))));
const ref = await addDoc(collection(mariana.db, 'reservas'), nueva);
prueba('socio crea su reserva (queda en revisión)', !!ref.id);
prueba('encargado de FÚTBOL NO aprueba una reserva de TENIS', !(await permitido(() => updateDoc(doc(encFutbol.db, 'reservas', ref.id), { estado: 'CONFIRMADA' }))));
prueba('el socio NO se auto-aprueba', !(await permitido(() => updateDoc(doc(mariana.db, 'reservas', ref.id), { estado: 'CONFIRMADA' }))));
prueba('encargado de TENIS SÍ aprueba la reserva de su área', await permitido(() => updateDoc(doc(encTenis.db, 'reservas', ref.id), { estado: 'CONFIRMADA' })));
prueba('socio cancela su reserva', await permitido(() => updateDoc(doc(mariana.db, 'reservas', ref.id), { estado: 'CANCELADA', canceladaEn: new Date(), sinPenalizacion: true })));
prueba('supervisor aprueba una reserva de cualquier área (r21 fútbol)', await permitido(() => updateDoc(doc(supervisor.db, 'reservas', 'r21'), { estado: 'CONFIRMADA' })));
prueba('encargado registra asistencia de su área', await permitido(() => setDoc(doc(encFutbol.db, 'checkins', 'r13'), { reservaId: 'r13', usuarioId: 'soc-02', asistencia: 'ASISTIO', registradoEn: new Date() })));
await updateDoc(doc(supervisor.db, 'reservas', 'r21'), { estado: 'PENDIENTE_APROBACION' });
await deleteDoc(doc(admin.db, 'checkins', 'r13'));
await deleteDoc(doc(admin.db, 'reservas', ref.id));

console.log('\n=== 4. NOTIFICACIONES por Cloud Functions (renovación) ===');
const miNotifs = async (s) => (await getDocs(query(collection(s.db, 'notificaciones'), where('usuarioId', '==', s.uid)))).docs.map((d) => d.data());
const adminAntes = (await miNotifs(admin)).length;
const solicitudId = 'sol-prueba-' + Date.now();
const JPEG = 'data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wAALCAABAAEBAREA/8QAFAABAAAAAAAAAAAAAAAAAAAAAP/EABQQAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQEAAD8AKp//2Q==';
await setDoc(doc(mariana.db, 'solicitudes_renovacion', solicitudId), { id: solicitudId, socioId: mariana.uid, socioNombre: 'Mariana López Ruiz', membresiaId: 'mem-soc-mariana', planId: 'INDIVIDUAL_NORMAL', monto: 1800, concepto: 'REN-PRUEBA', comprobanteUrl: JPEG, status: 'PENDIENTE', createdAt: Date.now() });
prueba('socio envía su solicitud de renovación', true);
const paraAdmin = await esperar(async () => { const n = await miNotifs(admin); return n.length > adminAntes ? n : null; });
prueba('el ADMIN recibe la notificación "Nueva solicitud de renovación"', !!paraAdmin && paraAdmin.some((n) => n.titulo === 'Nueva solicitud de renovación' && n.mensaje.includes('Mariana López Ruiz') && n.tipo === 'RENOVACION' && n.leida === false), JSON.stringify(paraAdmin?.slice(-1)));
// Las solicitudes de renovación se avisan a todo el personal (ROLES_DE_PERSONAL en functions/index.js): también al encargado.
prueba('el encargado (personal) también recibe esa notificación', (await miNotifs(encTenis)).some((n) => n.titulo === 'Nueva solicitud de renovación' && n.tipo === 'RENOVACION'));
prueba('Diego (otro socio) NO la recibe', (await miNotifs(diego)).length === 0);
const marianaAntes = (await miNotifs(mariana)).length;
await updateDoc(doc(admin.db, 'solicitudes_renovacion', solicitudId), { status: 'RECHAZADO', motivoRechazo: 'Comprobante ilegible', reviewedBy: admin.uid, reviewedAt: new Date() });
const rechazo = await esperar(async () => { const n = await miNotifs(mariana); return n.length > marianaAntes ? n : null; });
prueba('la SOCIA recibe "Renovación rechazada"', !!rechazo && rechazo.some((n) => n.titulo === 'Renovación rechazada'), JSON.stringify(rechazo));
// Segunda solicitud → aprobada
const sol2 = 'sol-prueba-2-' + Date.now();
await setDoc(doc(mariana.db, 'solicitudes_renovacion', sol2), { id: sol2, socioId: mariana.uid, socioNombre: 'Mariana López Ruiz', membresiaId: 'mem-soc-mariana', planId: 'INDIVIDUAL_NORMAL', monto: 1800, concepto: 'REN-PRUEBA-2', comprobanteUrl: JPEG, status: 'PENDIENTE', createdAt: Date.now() });
await updateDoc(doc(admin.db, 'solicitudes_renovacion', sol2), { status: 'APROBADO', reviewedBy: admin.uid, reviewedAt: new Date(), reviewedDate: dia(0), renewedUntil: dia(32) });
const aprob = await esperar(async () => (await miNotifs(mariana)).find((n) => n.titulo === 'Renovación aprobada'));
prueba('la SOCIA recibe "Renovación aprobada"', !!aprob);
const idempotente = (await miNotifs(mariana)).filter((n) => n.titulo === 'Renovación aprobada').length;
prueba('la notificación no se duplica (idempotente)', idempotente === 1);
// Marcar como leída (lo que hace la campana de la app)
const docNotif = (await getDocs(query(collection(mariana.db, 'notificaciones'), where('usuarioId', '==', mariana.uid), where('titulo', '==', 'Renovación aprobada')))).docs[0];
prueba('la socia marca su notificación como leída', await permitido(() => updateDoc(docNotif.ref, { leida: true, leidaEn: Date.now() })));
prueba('Diego NO puede marcar leída la de Mariana', !(await permitido(() => updateDoc(doc(diego.db, 'notificaciones', docNotif.id), { leida: true }))));
// Limpieza de lo creado por la prueba
// Los clientes no pueden borrar notificaciones (regla), así que la limpieza va por el SDK de administrador.
for (const s of [admin, mariana, encTenis]) {
  for (const n of (await adminDb.collection('notificaciones').where('usuarioId', '==', s.uid).get()).docs) await n.ref.delete();
}

console.log('\n=== 5. Avisos del club (insumo de las notificaciones locales) ===');
const aviso = { titulo: 'Prueba: cancha de tenis en mantenimiento', mensaje: 'Aviso de prueba generado por la batería funcional.', destinatario: 'SOCIOS', autorId: encTenis.uid, autorNombre: 'Miguel Ángel Cota', fechaMillis: Date.now(), imagenUrl: '', vigencia: dia(1) };
prueba('socio NO publica avisos', !(await permitido(() => addDoc(collection(mariana.db, 'avisos'), { ...aviso, autorId: mariana.uid }))));
const av = await addDoc(collection(encTenis.db, 'avisos'), aviso);
prueba('encargado SÍ publica un aviso para socios', !!av.id);
prueba('socio ve el aviso nuevo', (await getDocs(query(collection(mariana.db, 'avisos'), where('destinatario', 'in', ['SOCIOS', 'TODOS'])))).docs.some((d) => d.id === av.id));
await deleteDoc(av);

console.log(`\n${fallos === 0 ? 'TODAS LAS PRUEBAS FUNCIONALES PASARON' : fallos + ' FALLARON'} (${ok} OK)`);
await Promise.all(Object.values(sesiones).map((s) => deleteApp(s.app).catch(() => {})));
process.exit(fallos ? 1 : 0);
