// Batería funcional de: cambio de horario de una reserva, mantenimiento por periodo (cancelación + aviso + correo) y
// contraseñas (cambiar desde el perfil, restablecer por correo). Usa las reglas de seguridad y las funciones REALES en los
// emuladores locales: nunca toca Firebase real.
// Uso (emuladores con auth, firestore y functions corriendo):
//   node tools/datos-demo/sembrar-demo.mjs && node tools/datos-demo/probar-cambios-mantenimiento.mjs
import { createRequire } from 'node:module';
const req = createRequire(new URL('../pruebas-reglas/package.json', import.meta.url));
const { initializeApp } = req('firebase/app');
const { getAuth, connectAuthEmulator, signInWithEmailAndPassword, EmailAuthProvider, reauthenticateWithCredential, updatePassword, sendPasswordResetEmail } = req('firebase/auth');
const { getFirestore, connectFirestoreEmulator, doc, getDoc, setDoc, updateDoc, addDoc, collection, getDocs, query, where, writeBatch, deleteField, serverTimestamp } = req('firebase/firestore');

const reqFn = createRequire(new URL('../../functions/package.json', import.meta.url));
process.env.FIRESTORE_EMULATOR_HOST ??= '127.0.0.1:8080';
const { initializeApp: initAdmin, getApp } = reqFn('firebase-admin/app');
const { getFirestore: dbAdmin } = reqFn('firebase-admin/firestore');
const PROYECTO = 'clubdeportivoudc';
initAdmin({ projectId: PROYECTO }, 'admin-sdk');
const pad = (n) => String(n).padStart(2, '0');
const dia = (o = 0) => { const d = new Date(); d.setDate(d.getDate() + o); return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`; };
const CUENTAS = {
  admin: ['admin@athleticclub.test', 'Admin#2026Club'],
  encTenis: ['encargado.tenis@athleticclub.test', 'Tenis#2026Club'],
  mariana: ['mariana.lopez@athleticclub.test', 'Socio#2026Mariana'],
  diego: ['diego.hernandez@athleticclub.test', 'Socio#2026Diego']
};
let ok = 0, fallos = 0;
const prueba = (n, c, d = '') => { if (c) { ok++; console.log('OK    ' + n); } else { fallos++; console.log('FALLA ' + n + (d ? '  → ' + d : '')); } };
const permitido = async (fn) => { try { await fn(); return true; } catch (e) { if (e.code === 'permission-denied') return false; throw e; } };
const espera = (ms) => new Promise((r) => setTimeout(r, ms));
/** Espera (hasta ~12 s) a que una función del servidor deje algo en Firestore. */
async function esperar(fn) { for (let i = 0; i < 24; i++) { const v = await fn(); if (v) return v; await espera(500); } return null; }

const S = {};
async function entrar(k) {
  const app = initializeApp({ projectId: PROYECTO, apiKey: 'emulador', appId: k }, k + Math.random());
  const auth = getAuth(app); connectAuthEmulator(auth, 'http://127.0.0.1:9099', { disableWarnings: true });
  const db = getFirestore(app); connectFirestoreEmulator(db, '127.0.0.1', 8080);
  const c = await signInWithEmailAndPassword(auth, ...CUENTAS[k]);
  return (S[k] = { app, auth, db, user: c.user, uid: c.user.uid });
}
for (const k of Object.keys(CUENTAS)) await entrar(k);
const { admin, encTenis, mariana, diego } = S;

const notis = async (s, usuarioId, titulo) => {
  const r = await getDocs(query(collection(s.db, 'notificaciones'), where('usuarioId', '==', usuarioId)));
  return r.docs.map((d) => d.data()).filter((n) => n.titulo === titulo);
};
const hora = (h) => `${pad(h)}:00`;
const nueva = (socio, nombre, area, areaNombre, fecha, h, estado) => ({
  usuarioId: socio.uid, usuarioNombre: nombre, areaId: area, areaNombre, deporte: 'Tenis', fecha, horaInicio: hora(h), horaFin: hora(h + 1),
  duracionHoras: 1, personas: 1, carril: null, estado, esExterno: false, creadoEn: new Date(), actualizadoEn: new Date()
});

console.log('\n=== A. CAMBIO DE HORARIO DE UNA RESERVA CONFIRMADA ===');
const f1 = dia(3), f2 = dia(4);
const rA = await addDoc(collection(mariana.db, 'reservas'), nueva(mariana, 'Mariana López Ruiz', '6', 'Cancha de tenis 2', f1, 15, 'PENDIENTE_APROBACION'));
prueba('Mariana NO puede pedir cambio mientras su reserva sigue en revisión', !(await permitido(() => updateDoc(doc(mariana.db, 'reservas', rA.id), { modificacion: { fecha: f2, horaInicio: '16:00', horaFin: '17:00', personas: 1 } }))));
await updateDoc(doc(encTenis.db, 'reservas', rA.id), { estado: 'CONFIRMADA', aprobadaEn: serverTimestamp(), actualizadoEn: serverTimestamp() });
const pedido = { fecha: f2, horaInicio: '16:00', horaFin: '17:00', personas: 1, carril: null, solicitadaEn: serverTimestamp() };
prueba('Mariana solicita el cambio (la reserva no cambia)', await permitido(() => updateDoc(doc(mariana.db, 'reservas', rA.id), { modificacion: pedido, actualizadoEn: serverTimestamp() })));
let d = (await getDoc(doc(mariana.db, 'reservas', rA.id))).data();
prueba('la reserva sigue igual y la solicitud queda anotada', d.fecha === f1 && d.horaInicio === '15:00' && d.modificacion?.fecha === f2);
prueba('Mariana NO puede aplicarse el cambio ella misma', !(await permitido(() => updateDoc(doc(mariana.db, 'reservas', rA.id), { fecha: f2, modificacion: deleteField() }))));
const avisoEnc = await esperar(async () => (await notis(encTenis, encTenis.uid, 'Solicitud de cambio de reserva'))[0]);
prueba('el servidor avisa al encargado de tenis de la solicitud', !!avisoEnc, 'sin notificación');
prueba('el aviso dice cuándo es hoy y a cuándo quiere cambiarla', !!avisoEnc && /\d{1,2} de \w+ de \d{4}/.test(avisoEnc.mensaje) && !avisoEnc.mensaje.includes(f1), avisoEnc?.mensaje);
prueba('Diego NO puede tocar la solicitud de Mariana', !(await permitido(() => updateDoc(doc(diego.db, 'reservas', rA.id), { modificacion: deleteField() }))));
prueba('el encargado aprueba: aplica el cambio y deja constancia', await permitido(() => updateDoc(doc(encTenis.db, 'reservas', rA.id), {
  fecha: f2, horaInicio: '16:00', horaFin: '17:00', modificacion: deleteField(),
  resolucionModificacion: { resultado: 'APROBADA', resueltaPor: encTenis.uid, resueltaEn: serverTimestamp() }, actualizadoEn: serverTimestamp() })));
d = (await getDoc(doc(mariana.db, 'reservas', rA.id))).data();
prueba('la reserva quedó en su nuevo día y hora, sin solicitud pendiente', d.fecha === f2 && d.horaInicio === '16:00' && !d.modificacion);
const avisoOk = await esperar(async () => (await notis(mariana, mariana.uid, 'Cambio de reserva aprobado'))[0]);
prueba('el servidor avisa a Mariana que se aprobó', !!avisoOk && /de \d{4}/.test(avisoOk.mensaje), avisoOk?.mensaje);
// rechazo
await updateDoc(doc(mariana.db, 'reservas', rA.id), { modificacion: { ...pedido, fecha: dia(5), solicitadaEn: serverTimestamp() }, actualizadoEn: serverTimestamp() });
await updateDoc(doc(encTenis.db, 'reservas', rA.id), { modificacion: deleteField(), resolucionModificacion: { resultado: 'RECHAZADA', motivo: '', resueltaEn: serverTimestamp() }, actualizadoEn: serverTimestamp() });
const avisoNo = await esperar(async () => (await notis(mariana, mariana.uid, 'Cambio de reserva rechazado'))[0]);
prueba('si se rechaza, la reserva sigue y Mariana recibe el aviso', !!avisoNo && (await getDoc(doc(mariana.db, 'reservas', rA.id))).data().fecha === f2);

console.log('\n=== B. MANTENIMIENTO CON PERIODO ===');
const desde = dia(6), hasta = dia(8);
const dentro = await addDoc(collection(mariana.db, 'reservas'), nueva(mariana, 'Mariana López Ruiz', '5', 'Cancha de tenis 1', dia(7), 10, 'PENDIENTE_APROBACION'));
await updateDoc(doc(encTenis.db, 'reservas', dentro.id), { estado: 'CONFIRMADA', actualizadoEn: serverTimestamp() });
const dentroDiego = await addDoc(collection(diego.db, 'reservas'), nueva(diego, 'Diego Hernández Soto', '5', 'Cancha de tenis 1', dia(8), 12, 'PENDIENTE_APROBACION'));
const fuera = await addDoc(collection(mariana.db, 'reservas'), nueva(mariana, 'Mariana López Ruiz', '5', 'Cancha de tenis 1', dia(10), 10, 'PENDIENTE_APROBACION'));
prueba('el encargado pone el área en mantenimiento con periodo', await permitido(() => updateDoc(doc(encTenis.db, 'areas', '5'), {
  disponibilidad: 'MANTENIMIENTO', mantenimientoDesde: desde, mantenimientoHasta: hasta, mantenimientoMotivo: 'Cambio de superficie', actualizadoEn: serverTimestamp() })));
const lote = writeBatch(encTenis.db);
for (const r of [dentro, dentroDiego]) {
  lote.update(doc(encTenis.db, 'reservas', r.id), {
    estado: 'CANCELADA', canceladaEn: serverTimestamp(), sinPenalizacion: true, canceladaPorMantenimiento: true,
    mantenimientoDesde: desde, mantenimientoHasta: hasta, mantenimientoMotivo: 'Cambio de superficie', modificacion: deleteField(), actualizadoEn: serverTimestamp() });
}
prueba('el encargado cancela en lote SOLO las reservas del periodo', await permitido(() => lote.commit()));
prueba('la reserva fuera del periodo sigue vigente', (await getDoc(doc(mariana.db, 'reservas', fuera.id))).data().estado === 'PENDIENTE_APROBACION');
const area = (await getDoc(doc(mariana.db, 'areas', '5'))).data();
prueba('el área guarda su periodo de mantenimiento', area.disponibilidad === 'MANTENIMIENTO' && area.mantenimientoDesde === desde && area.mantenimientoHasta === hasta);
const avisoM = await esperar(async () => (await notis(mariana, mariana.uid, 'Reserva cancelada por mantenimiento'))[0]);
prueba('Mariana recibe el aviso en la app', !!avisoM && avisoM.mensaje.includes('mantenimiento'), avisoM?.mensaje);
prueba('Diego recibe también su aviso', !!(await esperar(async () => (await notis(diego, diego.uid, 'Reserva cancelada por mantenimiento'))[0])));
const correo = await esperar(async () => {
  // `correos_enviados` solo lo lee el servidor: se consulta con el SDK de administrador.
  const r = await dbAdmin(getApp('admin-sdk')).collection('correos_enviados').where('tipo', '==', 'reservaCanceladaMantenimiento').get();
  return r.docs.map((x) => x.data()).find((c) => c.para === 'mariana.lopez@athleticclub.test');
});
prueba('se registra el correo a Mariana (simulado en el emulador)', !!correo && correo.estado === 'SIMULADO', JSON.stringify(correo));
prueba('el correo trae el periodo, el motivo y cómo reagendar', !!correo && /Cambio de superficie/.test(correo.texto) && /reagendarla/.test(correo.texto), correo?.texto);
prueba('Mariana marca la reserva como reagendada', await permitido(() => updateDoc(doc(mariana.db, 'reservas', dentro.id), { reagendada: true, actualizadoEn: serverTimestamp() })));
prueba('Mariana NO puede reabrir la reserva cancelada', !(await permitido(() => updateDoc(doc(mariana.db, 'reservas', dentro.id), { estado: 'CONFIRMADA' }))));
await updateDoc(doc(encTenis.db, 'areas', '5'), { disponibilidad: 'DISPONIBLE', mantenimientoDesde: '', mantenimientoHasta: '', mantenimientoMotivo: '', actualizadoEn: serverTimestamp() });

console.log('\n=== C. CONTRASEÑAS ===');
const cred = EmailAuthProvider.credential(CUENTAS.mariana[0], 'Equivocada#1');
let malaRechazada = false;
try { await reauthenticateWithCredential(mariana.user, cred); } catch (e) { malaRechazada = /invalid-credential|wrong-password/.test(e.code); }
prueba('con la contraseña actual equivocada NO deja cambiarla', malaRechazada);
await reauthenticateWithCredential(mariana.user, EmailAuthProvider.credential(CUENTAS.mariana[0], CUENTAS.mariana[1]));
await updatePassword(mariana.user, 'Nueva#2026Mariana');
const app2 = initializeApp({ projectId: PROYECTO, apiKey: 'emulador', appId: 'x' }, 'login-nueva');
const auth2 = getAuth(app2); connectAuthEmulator(auth2, 'http://127.0.0.1:9099', { disableWarnings: true });
let nuevaOk = false, vieja = true;
try { await signInWithEmailAndPassword(auth2, CUENTAS.mariana[0], 'Nueva#2026Mariana'); nuevaOk = true; } catch {}
try { await signInWithEmailAndPassword(auth2, CUENTAS.mariana[0], CUENTAS.mariana[1]); } catch { vieja = false; }
prueba('tras cambiarla entra con la nueva y ya no con la anterior', nuevaOk && !vieja);
// restablecer por correo
await sendPasswordResetEmail(auth2, CUENTAS.mariana[0]);
const oob = await (await fetch(`http://127.0.0.1:9099/emulator/v1/projects/${PROYECTO}/oobCodes`)).json();
const codigo = (oob.oobCodes || []).filter((c) => c.email === CUENTAS.mariana[0] && c.requestType === 'PASSWORD_RESET').pop();
prueba('"Olvidaste tu contraseña" genera el enlace de restablecimiento para ese correo', !!codigo);
const res = await fetch(`http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:resetPassword?key=emulador`, {
  method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ oobCode: codigo?.oobCode, newPassword: 'Restablecida#2026' }) });
prueba('con ese enlace se elige una contraseña nueva', res.ok);
let restOk = false; try { await signInWithEmailAndPassword(auth2, CUENTAS.mariana[0], 'Restablecida#2026'); restOk = true; } catch {}
prueba('y entra con la contraseña restablecida', restOk);
let sinCuenta = null; try { await sendPasswordResetEmail(auth2, 'no-existe@athleticclub.test'); sinCuenta = 'sin error'; } catch (e) { sinCuenta = e.code; }
console.log('INFO  correo inexistente →', sinCuenta);

console.log(`\n${fallos === 0 ? 'TODAS LAS PRUEBAS PASARON' : 'HAY FALLAS'} (${ok} ok, ${fallos} fallas)`);
process.exit(fallos === 0 ? 0 : 1);
