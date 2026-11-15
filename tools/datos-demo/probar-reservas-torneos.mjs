// Batería funcional de RESERVAS y TORNEOS: flujo del miembro al encargado, permisos, inscripciones (cupo, duplicados,
// concurrencia). Replica las operaciones de la app (transacciones incluidas) con las reglas de seguridad reales.
// Requiere los emuladores corriendo y los datos de sembrar-demo.mjs.
// Uso: node tools/datos-demo/sembrar-demo.mjs && node tools/datos-demo/probar-reservas-torneos.mjs
import { createRequire } from 'node:module';
const req = createRequire(new URL('../pruebas-reglas/package.json', import.meta.url));
const { initializeApp, deleteApp } = req('firebase/app');
const { getAuth, connectAuthEmulator, signInWithEmailAndPassword } = req('firebase/auth');
const { getFirestore, connectFirestoreEmulator, doc, getDoc, setDoc, updateDoc, addDoc, deleteDoc, collection, getDocs, query, where, runTransaction, serverTimestamp } = req('firebase/firestore');

const PROYECTO = 'clubdeportivoudc';
const pad = (n) => String(n).padStart(2, '0');
const dia = (o = 0) => { const d = new Date(); d.setDate(d.getDate() + o); return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`; };
const CUENTAS = {
  admin: ['admin@athleticclub.test', 'Admin#2026Club'],
  supervisor: ['supervisor@athleticclub.test', 'Super#2026Club'],
  encFutbol: ['encargado.futbol@athleticclub.test', 'Futbol#2026Club'],
  encTenis: ['encargado.tenis@athleticclub.test', 'Tenis#2026Club'],
  mariana: ['mariana.lopez@athleticclub.test', 'Socio#2026Mariana'],
  diego: ['diego.hernandez@athleticclub.test', 'Socio#2026Diego'],
  valeria: ['soc-01@athleticclub.test', 'Relleno#2026Club'],
  roberto: ['soc-02@athleticclub.test', 'Relleno#2026Club']
};
let ok = 0, fallos = 0;
const prueba = (n, c, d = '') => { if (c) { ok++; console.log('OK    ' + n); } else { fallos++; console.log('FALLA ' + n + (d ? '  → ' + d : '')); } };
const info = (t) => console.log('INFO  ' + t);
const permitido = async (fn) => { try { await fn(); return true; } catch (e) { if (e.code === 'permission-denied') return false; throw e; } };
const S = {};
async function entrar(k) {
  const app = initializeApp({ projectId: PROYECTO, apiKey: 'emulador', appId: k }, k + Math.random());
  const auth = getAuth(app); connectAuthEmulator(auth, 'http://127.0.0.1:9099', { disableWarnings: true });
  const db = getFirestore(app); connectFirestoreEmulator(db, '127.0.0.1', 8080);
  const c = await signInWithEmailAndPassword(auth, ...CUENTAS[k]);
  return (S[k] = { app, db, uid: c.user.uid });
}
for (const k of Object.keys(CUENTAS)) await entrar(k);
const { admin, supervisor, encFutbol, encTenis, mariana, diego, valeria, roberto } = S;

const nuevaReserva = (socio, nombre, areaId, areaNombre, deporte, fecha, h, horas, personas, estado = 'PENDIENTE_APROBACION') => ({
  usuarioId: socio.uid, usuarioNombre: nombre, areaId, areaNombre, deporte, fecha, horaInicio: `${pad(h)}:00`, horaFin: `${pad(h + horas)}:00`,
  duracionHoras: horas, personas, carril: null, estado, esExterno: false, creadoEn: new Date(), actualizadoEn: new Date()
});

console.log('\n=== 1. RESERVAS DE ÁREAS: del miembro al encargado ===');
const mañana = dia(1), pasado = dia(2);
const r1 = await addDoc(collection(mariana.db, 'reservas'), nuevaReserva(mariana, 'Mariana López Ruiz', '6', 'Cancha de tenis 2', 'Tenis', mañana, 15, 2, 2));
prueba('Mariana reserva tenis (2 h, 2 personas) y queda EN REVISIÓN', (await getDoc(r1)).data().estado === 'PENDIENTE_APROBACION');
const r2 = await addDoc(collection(diego.db, 'reservas'), nuevaReserva(diego, 'Diego Hernández Soto', '2', 'Cancha de fútbol 2', 'Fútbol', pasado, 19, 1, 10));
prueba('Diego reserva fútbol (10 personas) y queda EN REVISIÓN', (await getDoc(r2)).data().estado === 'PENDIENTE_APROBACION');
const mias = await getDocs(query(collection(mariana.db, 'reservas'), where('usuarioId', '==', mariana.uid)));
prueba('Mariana consulta SUS reservas (la consulta que hace la app)', mias.size >= 1 && mias.docs.every((d) => d.data().usuarioId === mariana.uid));
const deTenis = await getDocs(query(collection(encTenis.db, 'reservas'), where('deporte', '==', 'Tenis')));
prueba('el encargado de tenis lista las reservas de su deporte (consulta de la app)', deTenis.docs.some((d) => d.id === r1.id));
prueba('el encargado de FÚTBOL no puede aprobar la de TENIS', !(await permitido(() => updateDoc(doc(encFutbol.db, 'reservas', r1.id), { estado: 'CONFIRMADA' }))));
prueba('el encargado de TENIS aprueba la de su área', await permitido(() => updateDoc(doc(encTenis.db, 'reservas', r1.id), { estado: 'CONFIRMADA', aprobadaEn: serverTimestamp(), actualizadoEn: serverTimestamp() })));
prueba('al aprobarse se aparta el material (el encargado crea materialAsignado)', await permitido(() => setDoc(doc(encTenis.db, 'materialAsignado', 'ma-r1'), { reservaId: r1.id, herramientaId: '11', herramientaNombre: 'Raquetas de tenis', areaId: '6', fecha: mañana, cantidad: 1, estado: 'ASIGNADO', creadoEn: new Date() })));
prueba('el supervisor rechaza la de fútbol con motivo (cualquier área)', await permitido(() => updateDoc(doc(supervisor.db, 'reservas', r2.id), { estado: 'RECHAZADA', motivoRechazo: 'Cancha en mantenimiento', rechazadaEn: serverTimestamp(), actualizadoEn: serverTimestamp() })));
const verR2 = (await getDoc(doc(diego.db, 'reservas', r2.id))).data();
prueba('Diego ve su reserva RECHAZADA con el motivo', verR2.estado === 'RECHAZADA' && verR2.motivoRechazo === 'Cancha en mantenimiento');
prueba('Roberto no puede cancelar la reserva de Mariana', !(await permitido(() => updateDoc(doc(roberto.db, 'reservas', r1.id), { estado: 'CANCELADA' }))));
prueba('Mariana cancela su reserva confirmada (sin penalización: con más de 4 h)', await permitido(() => updateDoc(doc(mariana.db, 'reservas', r1.id), { estado: 'CANCELADA', canceladaEn: serverTimestamp(), sinPenalizacion: true, actualizadoEn: serverTimestamp() })));
prueba('Mariana no puede reactivar su reserva cancelada', !(await permitido(() => updateDoc(doc(mariana.db, 'reservas', r1.id), { estado: 'CONFIRMADA' }))));
const rs = await addDoc(collection(supervisor.db, 'reservas'), nuevaReserva(supervisor, 'Carlos Rivera', '3', 'Cancha de básquetbol 1', 'Básquetbol', mañana, 12, 1, 4, 'CONFIRMADA'));
prueba('el personal reserva sin pedir permiso (queda CONFIRMADA)', (await getDoc(rs)).data().estado === 'CONFIRMADA');
prueba('un miembro NO puede borrar reservas', !(await permitido(() => deleteDoc(doc(diego.db, 'reservas', r2.id)))));
prueba('Mariana no reserva a nombre de Diego', !(await permitido(() => addDoc(collection(mariana.db, 'reservas'), nuevaReserva(diego, 'Diego', '1', 'x', 'Fútbol', mañana, 9, 1, 1)))));

console.log('\n=== 2. Concurrencia en el cupo (límite conocido) ===');
// Cancha de tenis 1 (id 5) capacidad 4. Dos miembros piden 3 lugares a la misma hora, a la vez.
const [a, b] = await Promise.all([
  addDoc(collection(valeria.db, 'reservas'), nuevaReserva(valeria, 'Valeria', '5', 'Cancha de tenis 1', 'Tenis', pasado, 8, 1, 3)),
  addDoc(collection(roberto.db, 'reservas'), nuevaReserva(roberto, 'Roberto', '5', 'Cancha de tenis 1', 'Tenis', pasado, 8, 1, 3))
]);
const ocupadas = (await getDocs(query(collection(admin.db, 'reservas'), where('areaId', '==', '5'), where('fecha', '==', pasado)))).docs
  .filter((d) => ['PENDIENTE_APROBACION', 'CONFIRMADA'].includes(d.data().estado) && d.data().horaInicio === '08:00').reduce((t, d) => t + d.data().personas, 0);
if (ocupadas > 4) info(`SOBRECUPO posible: dos solicitudes simultáneas sumaron ${ocupadas} personas en una cancha de 4. El cupo se valida solo en la app (Firestore no puede sumar reservas en las reglas); lo detecta el encargado al aprobar.`);
prueba('(informativo) ambas solicitudes simultáneas se registran; el encargado decide cuál aprueba', !!a.id && !!b.id);

console.log('\n=== 3. TORNEOS: alta, inscripción, cupo, duplicados ===');
const torneo = { nombre: 'Torneo de prueba Relámpago', disciplina: 'Tenis', areaId: '6', areaNombre: 'Cancha de tenis 2', fechaInicio: dia(5), fechaFin: dia(5), horaInicio: '09:00', horaFin: '13:00', cupoMaximo: 2, inscritos: 0, creadoPor: admin.uid, creadoEn: new Date(), actualizadoEn: new Date() };
prueba('un miembro NO puede crear torneos', !(await permitido(() => addDoc(collection(mariana.db, 'torneos'), torneo))));
const tRef = await addDoc(collection(admin.db, 'torneos'), torneo);
prueba('el administrador crea un torneo (cupo 2)', !!tRef.id);
const tId = tRef.id;

// Misma transacción que FirebaseTorneoRepository.inscribirse
async function inscribirse(socio, nombre) {
  const torneoRef = doc(socio.db, 'torneos', tId);
  const insRef = doc(socio.db, 'inscripcionesTorneo', `${tId}_${socio.uid}`);
  await runTransaction(socio.db, async (tx) => {
    const t = await tx.get(torneoRef);
    if (!t.exists()) throw new Error('ya no existe');
    if ((await tx.get(insRef)).exists()) throw new Error('ya inscrito');
    const cupo = t.data().cupoMaximo, actuales = t.data().inscritos || 0;
    if (actuales >= cupo) throw new Error('lleno');
    tx.set(insRef, { torneoId: tId, torneoNombre: t.data().nombre, usuarioId: socio.uid, usuarioNombre: nombre, fechaInscripcion: dia(0), estado: 'CONFIRMADA', creadoEn: serverTimestamp() });
    tx.update(torneoRef, { inscritos: actuales + 1, actualizadoEn: serverTimestamp() });
  });
}
await inscribirse(mariana, 'Mariana López Ruiz');
prueba('Mariana se inscribe (inscritos = 1)', (await getDoc(doc(admin.db, 'torneos', tId))).data().inscritos === 1);
prueba('Mariana no puede inscribirse dos veces', await inscribirse(mariana, 'Mariana').then(() => false, (e) => e.message === 'ya inscrito'));
prueba('no se puede inscribir a otra persona', !(await permitido(() => setDoc(doc(mariana.db, 'inscripcionesTorneo', `${tId}_${diego.uid}`), { torneoId: tId, usuarioId: diego.uid, usuarioNombre: 'Diego', estado: 'CONFIRMADA' }))));
// Carrera por el último lugar: dos personas a la vez, queda 1 lugar.
const resultados = await Promise.allSettled([inscribirse(diego, 'Diego Hernández Soto'), inscribirse(valeria, 'Valeria Acosta Pérez')]);
const ganaron = resultados.filter((r) => r.status === 'fulfilled').length;
const t2 = (await getDoc(doc(admin.db, 'torneos', tId))).data();
prueba('carrera por el ÚLTIMO lugar: exactamente una persona entra', ganaron === 1, `entraron ${ganaron}`);
prueba('el contador queda en 2 de 2 (nunca pasa del cupo)', t2.inscritos === 2);
const insc = await getDocs(query(collection(admin.db, 'inscripcionesTorneo'), where('torneoId', '==', tId)));
prueba('hay exactamente 2 inscripciones registradas (coinciden con el contador)', insc.size === 2);
prueba('con el torneo lleno, un tercero no entra', await inscribirse(roberto, 'Roberto').then(() => false, (e) => e.message === 'lleno'));
prueba('nadie puede subir el contador por encima del cupo (reglas)', !(await permitido(() => updateDoc(doc(roberto.db, 'torneos', tId), { inscritos: 3 }))));
prueba('un miembro no puede cambiar el nombre ni el cupo del torneo', !(await permitido(() => updateDoc(doc(roberto.db, 'torneos', tId), { cupoMaximo: 99 }))) && !(await permitido(() => updateDoc(doc(roberto.db, 'torneos', tId), { nombre: 'Hackeado', inscritos: 2 }))));
const bajar = await permitido(() => updateDoc(doc(roberto.db, 'torneos', tId), { inscritos: 0, actualizadoEn: new Date() }));
if (bajar) info('HALLAZGO de seguridad: cualquier miembro puede BAJAR torneos.inscritos (las reglas solo exigen que no pase del cupo), liberando cupo falso. Se recomienda exigir inscritos == anterior + 1 y que exista su inscripción.');
await updateDoc(doc(admin.db, 'torneos', tId), { inscritos: 2 });
prueba('el encargado de un deporte puede editar torneos (personal)', await permitido(() => updateDoc(doc(encTenis.db, 'torneos', tId), { horaFin: '14:00', actualizadoEn: new Date() })));
prueba('Mariana ve sus inscripciones (consulta de la app)', (await getDocs(query(collection(mariana.db, 'inscripcionesTorneo'), where('usuarioId', '==', mariana.uid)))).docs.some((d) => d.data().torneoId === tId));
prueba('un miembro no puede cancelar/borrar la inscripción de otro', !(await permitido(() => deleteDoc(doc(roberto.db, 'inscripcionesTorneo', `${tId}_${mariana.uid}`)))));
info('La app no tiene "cancelar inscripción" para el miembro (solo el personal borra inscripciones).');
await deleteDoc(doc(admin.db, 'torneos', tId));

console.log(`\n${fallos === 0 ? 'TODAS LAS PRUEBAS DE RESERVAS Y TORNEOS PASARON' : fallos + ' FALLARON'} (${ok} OK)`);
await Promise.all(Object.values(S).map((s) => deleteApp(s.app).catch(() => {})));
process.exit(fallos ? 1 : 0);
