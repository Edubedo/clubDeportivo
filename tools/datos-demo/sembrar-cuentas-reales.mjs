// Crea SOLO las 8 cuentas de prueba en el Firebase REAL (Auth + usuarios/miembros/membresias). NO borra nada.
// Uso: GOOGLE_APPLICATION_CREDENTIALS=<adc.json> node tools/datos-demo/sembrar-cuentas-reales.mjs
// Si un correo ya existe en Auth, no lo toca (no cambia contraseña ni datos existentes).
import { createRequire } from 'node:module';
const require = createRequire(new URL('../../functions/package.json', import.meta.url));
if (process.env.FIRESTORE_EMULATOR_HOST || process.env.FIREBASE_AUTH_EMULATOR_HOST) throw new Error('Quita las variables de emulador.');
const { initializeApp } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getFirestore, Timestamp } = require('firebase-admin/firestore');
initializeApp({ projectId: 'clubdeportivoudc' });
const auth = getAuth(); const db = getFirestore();
const pad = (n) => String(n).padStart(2, '0');
const dia = (o = 0) => { const d = new Date(); d.setDate(d.getDate() + o); return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`; };
const ts = (o) => Timestamp.fromDate(new Date(Date.now() + o * 86400000));
const staff = [
  ['adm-demo', 'ADMIN', 'Laura Mendoza Ortega', 'admin@athleticclub.test', 'Admin#2026Club', '6671000001', 'Matutino', ''],
  ['sup-demo', 'SUPERVISOR', 'Carlos Rivera Beltrán', 'supervisor@athleticclub.test', 'Super#2026Club', '6671000002', 'Vespertino', ''],
  ['enc-futbol', 'AYUDANTE_AREA', 'Jorge Salazar Peña', 'encargado.futbol@athleticclub.test', 'Futbol#2026Club', '6671000003', 'Matutino', 'Fútbol'],
  ['enc-basquet', 'AYUDANTE_AREA', 'Ana Lucía Duarte', 'encargado.basquetbol@athleticclub.test', 'Basquet#2026Club', '6671000004', 'Vespertino', 'Básquetbol'],
  ['enc-tenis', 'AYUDANTE_AREA', 'Miguel Ángel Cota', 'encargado.tenis@athleticclub.test', 'Tenis#2026Club', '6671000005', 'Matutino', 'Tenis'],
  ['enc-natacion', 'AYUDANTE_AREA', 'Daniela Robles Urías', 'encargado.natacion@athleticclub.test', 'Natacion#2026Club', '6671000006', 'Vespertino', 'Natación']
];
const socios = [
  ['soc-mariana', 'CLB-7K3M9Q', 'Mariana López Ruiz', 'mariana.lopez@athleticclub.test', 'Socio#2026Mariana', 'NORMAL', 1800, 2],
  ['soc-diego', 'CLB-X4RT82', 'Diego Hernández Soto', 'diego.hernandez@athleticclub.test', 'Socio#2026Diego', 'DELUXE', 3500, 20]
];
async function crear(uid, email, password, nombre) {
  try { const u = await auth.getUserByEmail(email); console.log('YA EXISTE (omitido):', email, u.uid); return false; } catch (e) { if (e.code !== 'auth/user-not-found') throw e; }
  await auth.createUser({ uid, email, password, displayName: nombre, emailVerified: true });
  console.log('creado:', email); return true;
}
for (const [uid, rol, nombre, email, password, tel, turno, area] of staff) {
  if (!(await crear(uid, email, password, nombre))) continue;
  await db.doc(`usuarios/${uid}`).set({ nombre, email, telefono: tel, rol, estado: 'ACTIVO', tipoPersonal: 'Instructor', turno, areaTrabajo: area,
    fechaRegistro: dia(), fechaIngreso: dia(), creadoPor: 'adm-demo', creadoEn: ts(0), actualizadoEn: ts(0) });
}
for (const [uid, codigo, nombre, email, password, plan, precio, vence] of socios) {
  if (!(await crear(uid, email, password, nombre))) continue;
  const membresiaId = `mem-${uid}`;
  await db.doc(`usuarios/${uid}`).set({ nombre, email, telefono: '', rol: 'SOCIO', estado: 'ACTIVO', codigoMiembro: codigo, terminosVersion: '1', terminosAceptadosEn: ts(0), fechaRegistro: dia(), creadoEn: ts(0), actualizadoEn: ts(0) });
  await db.doc(`miembros/${codigo}`).set({ nombre, telefono: '', correo: email, membresiaId, parentesco: 'Titular', esTitular: true, usuarioId: uid, cuentaCreada: true, correoCuenta: email, fechaRegistro: dia(), creadoEn: ts(0) });
  await db.doc(`registroCodigos/${codigo}`).set({ usuarioId: uid, cuentaCreada: true, usuarioNuevo: uid, creadoEn: ts(0), activadaEn: ts(0) });
  await db.doc(`membresias/${membresiaId}`).set({ usuarioId: uid, titularCodigo: codigo, titularNombre: nombre, totalPersonas: 1, tipo: 'INDIVIDUAL', plan, precio, estado: 'ACTIVA',
    fechaInicio: dia(vence - 30), fechaVencimiento: dia(vence), renovaciones: 1, ultimaRenovacion: dia(vence - 30), comprobanteUrl: '', creadoPor: 'sup-demo', creadoEn: ts(0), actualizadoEn: ts(0) });
}
console.log('Listo.');
