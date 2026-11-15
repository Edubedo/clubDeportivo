// Siembra datos de demostración "como si el club ya estuviera funcionando" SOLO en los emuladores locales.
// Uso (con los emuladores corriendo):  node tools/datos-demo/sembrar-demo.mjs
// Se niega a correr si no detecta FIRESTORE_EMULATOR_HOST / FIREBASE_AUTH_EMULATOR_HOST: nunca toca Firebase real.
import { createRequire } from 'node:module';
const require = createRequire(new URL('../../functions/package.json', import.meta.url));
process.env.FIRESTORE_EMULATOR_HOST ??= '127.0.0.1:8080';
process.env.FIREBASE_AUTH_EMULATOR_HOST ??= '127.0.0.1:9099';
if (!/^(127\.0\.0\.1|localhost)/.test(process.env.FIRESTORE_EMULATOR_HOST)) {
  throw new Error('Este script solo corre contra emuladores locales.');
}
const { initializeApp } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getFirestore, Timestamp } = require('firebase-admin/firestore');
import { writeFileSync } from 'node:fs';

const PROYECTO = process.env.PROYECTO ?? 'clubdeportivoudc';
initializeApp({ projectId: PROYECTO });
const auth = getAuth();
const db = getFirestore();

// ---------- Fechas (hora local de la máquina, igual que Fechas.hoy() en el teléfono) ----------
const pad = (n) => String(n).padStart(2, '0');
const dia = (offset = 0) => {
  const d = new Date();
  d.setDate(d.getDate() + offset);
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
};
const ahora = Timestamp.now();
const ts = (offsetDias) => Timestamp.fromDate(new Date(Date.now() + offsetDias * 86400000));
const hh = (h) => `${pad(h)}:00`;

// ---------- Cuentas ----------
const cuentas = [
  { uid: 'adm-demo', rol: 'ADMIN', nombre: 'Laura Mendoza Ortega', email: 'admin@athleticclub.test', password: 'Admin#2026Club', tel: '6671000001', turno: 'Matutino', area: '' },
  { uid: 'sup-demo', rol: 'SUPERVISOR', nombre: 'Carlos Rivera Beltrán', email: 'supervisor@athleticclub.test', password: 'Super#2026Club', tel: '6671000002', turno: 'Vespertino', area: '' },
  { uid: 'enc-futbol', rol: 'AYUDANTE_AREA', nombre: 'Jorge Salazar Peña', email: 'encargado.futbol@athleticclub.test', password: 'Futbol#2026Club', tel: '6671000003', turno: 'Matutino', area: 'Fútbol' },
  { uid: 'enc-basquet', rol: 'AYUDANTE_AREA', nombre: 'Ana Lucía Duarte', email: 'encargado.basquetbol@athleticclub.test', password: 'Basquet#2026Club', tel: '6671000004', turno: 'Vespertino', area: 'Básquetbol' },
  { uid: 'enc-tenis', rol: 'AYUDANTE_AREA', nombre: 'Miguel Ángel Cota', email: 'encargado.tenis@athleticclub.test', password: 'Tenis#2026Club', tel: '6671000005', turno: 'Matutino', area: 'Tenis' },
  { uid: 'enc-natacion', rol: 'AYUDANTE_AREA', nombre: 'Daniela Robles Urías', email: 'encargado.natacion@athleticclub.test', password: 'Natacion#2026Club', tel: '6671000006', turno: 'Vespertino', area: 'Natación' }
];

// Miembros: los dos primeros son las cuentas "oficiales" de prueba; el resto pueblan el club (dashboard, listas, reportes).
// plan: NINO|NORMAL|DELUXE (INDIVIDUAL) · estado: ACTIVA|SUSPENDIDA · vence: días desde hoy (negativo = vencida)
const PRECIO = { NINO: 1500, NORMAL: 1800, DELUXE: 3500 };
const miembros = [
  { uid: 'soc-mariana', codigo: 'CLB-7K3M9Q', nombre: 'Mariana López Ruiz', email: 'mariana.lopez@athleticclub.test', password: 'Socio#2026Mariana', plan: 'NORMAL', estado: 'ACTIVA', vence: 2, oficial: true },
  { uid: 'soc-diego', codigo: 'CLB-X4RT82', nombre: 'Diego Hernández Soto', email: 'diego.hernandez@athleticclub.test', password: 'Socio#2026Diego', plan: 'DELUXE', estado: 'ACTIVA', vence: 20, oficial: true },
  { uid: 'soc-01', codigo: 'CLB-B2C3D4', nombre: 'Valeria Acosta Pérez', plan: 'NORMAL', estado: 'ACTIVA', vence: 12 },
  { uid: 'soc-02', codigo: 'CLB-F5G6H7', nombre: 'Roberto Medina Cruz', plan: 'DELUXE', estado: 'ACTIVA', vence: 5 },
  { uid: 'soc-03', codigo: 'CLB-J8K9M2', nombre: 'Sofía Ibarra Lara', plan: 'NINO', estado: 'ACTIVA', vence: 26 },
  { uid: 'soc-04', codigo: 'CLB-N3P4Q5', nombre: 'Fernando Quiroz Vega', plan: 'NORMAL', estado: 'ACTIVA', vence: 1 },
  { uid: 'soc-05', codigo: 'CLB-R6S7T8', nombre: 'Camila Beltrán Ríos', plan: 'NORMAL', estado: 'ACTIVA', vence: 18 },
  { uid: 'soc-06', codigo: 'CLB-V9W2X3', nombre: 'Andrés Camacho Luna', plan: 'DELUXE', estado: 'SUSPENDIDA', vence: 9, motivo: 'FALTA_DE_PAGO' },
  { uid: 'soc-07', codigo: 'CLB-Y4Z5A6', nombre: 'Paola Escalante Gil', plan: 'NORMAL', estado: 'ACTIVA', vence: -6 },
  { uid: 'soc-08', codigo: 'CLB-C7D8E9', nombre: 'Héctor Valenzuela Mora', plan: 'NORMAL', estado: 'ACTIVA', vence: 7 },
  { uid: 'soc-09', codigo: 'CLB-G2H3J4', nombre: 'Isabela Fierro Nava', plan: 'NINO', estado: 'ACTIVA', vence: 22 },
  { uid: 'soc-10', codigo: 'CLB-K5M6N7', nombre: 'Ricardo Sandoval Ayala', plan: 'DELUXE', estado: 'ACTIVA', vence: 15 }
];
const PASS_RELLENO = 'Relleno#2026Club';
const correoRelleno = (m) => `${m.uid}@athleticclub.test`;

// ---------- Áreas, horarios e inventario (ids "1".."8" y herramientas 10/11/12/15: el código los usa así) ----------
const areas = [
  ['1', 'Cancha de fútbol 1', 'Fútbol', 22, '⚽'], ['2', 'Cancha de fútbol 2', 'Fútbol', 22, '⚽'],
  ['3', 'Cancha de básquetbol 1', 'Básquetbol', 10, '🏀'], ['4', 'Cancha de básquetbol 2', 'Básquetbol', 10, '🏀'],
  ['5', 'Cancha de tenis 1', 'Tenis', 4, '🎾'], ['6', 'Cancha de tenis 2', 'Tenis', 4, '🎾'],
  ['7', 'Alberca olímpica', 'Natación', 30, '🏊', 8], ['8', 'Alberca de práctica', 'Natación', 20, '🏊', 4]
];
const herramientas = [
  ['10', 'Balones de fútbol', 'Fútbol', ['1', '2'], '⚽', 14, 14, 6],
  ['12', 'Balones de básquetbol', 'Básquetbol', ['3', '4'], '🏀', 10, 10, 4],
  ['11', 'Raquetas de tenis', 'Tenis', ['5', '6'], '🎾', 8, 8, 4],
  ['15', 'Tablas de natación', 'Natación', ['7', '8'], '🏊', 12, 12, 5],
  ['h-redes', 'Redes de repuesto', 'Fútbol', ['1', '2'], '🥅', 2, 2, 3],           // stock BAJO (2 < mínimo 3)
  ['h-pelotas', 'Pelotas de tenis (tubo)', 'Tenis', ['5', '6'], '🎾', 3, 3, 6],     // stock BAJO
  ['h-gorras', 'Gorras de natación', 'Natación', ['7', '8'], '🧢', 25, 25, 10],
  ['h-conos', 'Conos de entrenamiento', 'Básquetbol', ['3', '4'], '🔶', 30, 30, 10]
];

// ---------- Escritura ----------
async function crearUsuarioAuth({ uid, email, password, nombre }) {
  try { await auth.deleteUser(uid); } catch { /* no existía */ }
  await auth.createUser({ uid, email, password, displayName: nombre, emailVerified: true });
}
const set = (ruta, datos) => db.doc(ruta).set(datos);

async function limpiarColecciones() {
  const nombres = ['usuarios', 'miembros', 'registroCodigos', 'membresias', 'pagos', 'areas', 'herramientas', 'reservas',
    'materialAsignado', 'checkins', 'torneos', 'inscripcionesTorneo', 'avisos', 'publicaciones', 'notificaciones',
    'solicitudes_renovacion', 'solicitudes_cambio_plan', 'correos_enviados', 'listaEspera', 'precios', 'config'];
  for (const n of nombres) await db.recursiveDelete(db.collection(n));
}

console.log('Limpiando emuladores…');
await limpiarColecciones();

console.log('Creando cuentas de personal…');
for (const c of cuentas) {
  await crearUsuarioAuth(c);
  await set(`usuarios/${c.uid}`, {
    nombre: c.nombre, email: c.email, telefono: c.tel, rol: c.rol, estado: 'ACTIVO',
    tipoPersonal: 'Instructor', turno: c.turno, areaTrabajo: c.area,
    fechaRegistro: dia(-120), fechaIngreso: dia(-120), creadoPor: 'adm-demo', creadoEn: ts(-120), actualizadoEn: ts(-120)
  });
}

console.log('Creando áreas e inventario…');
for (const [id, nombre, tipo, capacidad, emoji, carriles = 0] of areas) {
  await set(`areas/${id}`, { nombre, tipo, capacidad, disponibilidad: 'DISPONIBLE', permiteExternos: tipo !== 'Tenis', emoji, carriles, imagenUrl: '', creadoEn: ts(-200), actualizadoEn: ts(-200) });
}
for (const [id, nombre, deporte, areaIds, icono, total, disp, minimo] of herramientas) {
  await set(`herramientas/${id}`, { nombre, deporte, areaIds, icono, cantidadTotal: total, cantidadDisponible: disp, stockMinimo: minimo, estado: 'BUENO', creadoEn: ts(-90), actualizadoEn: ts(-1) });
}

console.log('Creando miembros, membresías y pagos…');
const miembrosEnClub = [];
let nPago = 0;
for (const m of miembros) {
  const email = m.email ?? correoRelleno(m);
  const password = m.password ?? PASS_RELLENO;
  await crearUsuarioAuth({ uid: m.uid, email, password, nombre: m.nombre });
  const membresiaId = `mem-${m.uid}`;
  const vencimiento = dia(m.vence);
  const inicio = dia(m.vence - 30);
  await set(`usuarios/${m.uid}`, {
    nombre: m.nombre, email, telefono: '66720' + String(10000 + miembrosEnClub.length * 37).slice(0, 5), rol: 'SOCIO', estado: m.estado === 'ACTIVA' ? 'ACTIVO' : 'ACTIVO',
    codigoMiembro: m.codigo, terminosVersion: '1', terminosAceptadosEn: ts(-60), fechaRegistro: dia(-60), creadoEn: ts(-60), actualizadoEn: ts(-1)
  });
  await set(`miembros/${m.codigo}`, {
    nombre: m.nombre, telefono: '', correo: email, membresiaId, parentesco: 'Titular', esTitular: true,
    usuarioId: m.uid, cuentaCreada: true, correoCuenta: email, fechaRegistro: dia(-60), creadoEn: ts(-60)
  });
  await set(`registroCodigos/${m.codigo}`, { usuarioId: m.uid, cuentaCreada: true, usuarioNuevo: m.uid, creadoEn: ts(-60), activadaEn: ts(-59) });
  const mem = {
    usuarioId: m.uid, titularCodigo: m.codigo, titularNombre: m.nombre, totalPersonas: 1, tipo: 'INDIVIDUAL', plan: m.plan,
    precio: PRECIO[m.plan], estado: m.estado, fechaInicio: inicio, fechaVencimiento: vencimiento, renovaciones: 1,
    ultimaRenovacion: inicio, comprobanteUrl: '', creadoPor: 'sup-demo', creadoEn: ts(-60), actualizadoEn: ts(-1)
  };
  if (m.estado === 'SUSPENDIDA') Object.assign(mem, { motivoSuspension: m.motivo, suspendidaEn: dia(-3), suspendidaPor: 'adm-demo' });
  await set(`membresias/${membresiaId}`, mem);
  // Historial de cobros: el alta hace 2 meses y dos mensualidades (así el dashboard tiene meses anteriores).
  const metodos = ['EFECTIVO', 'TARJETA', 'TRANSFERENCIA'];
  for (const [i, concepto] of ['ALTA', 'MENSUALIDAD', 'MENSUALIDAD'].entries()) {
    await set(`pagos/pago-${String(++nPago).padStart(3, '0')}`, {
      usuarioId: m.uid, membresiaId, titularNombre: m.nombre, concepto, monto: PRECIO[m.plan], moneda: 'MXN',
      fechaPago: dia(-60 + i * 30 + (m.vence % 5)), metodoPago: metodos[(nPago + i) % 3], estado: 'PAGADO', registradoPor: 'sup-demo', creadoEn: ts(-60 + i * 30)
    });
  }
  miembrosEnClub.push(m);
}
// Una visita de un día y un cobro suelto de hoy
await set('pagos/pago-visita-1', { usuarioId: 'sup-demo', concepto: 'VISITA', titularNombre: 'Visitante de día', monto: 200, moneda: 'MXN', fechaPago: dia(-1), metodoPago: 'EFECTIVO', estado: 'PAGADO', registradoPor: 'sup-demo', creadoEn: ts(-1) });

console.log('Creando reservas…');
// [id, areaId, usuarioUid, dia, horaInicio, horas, personas, estado]
const nombreDe = Object.fromEntries([...miembros.map((m) => [m.uid, m.nombre])]);
const areaPorId = Object.fromEntries(areas.map((a) => [a[0], a]));
const reservas = [
  // Historial reciente (finalizadas)
  ['r01', '1', 'soc-01', -9, 18, 2, 10, 'FINALIZADA'], ['r02', '5', 'soc-mariana', -8, 9, 1, 2, 'FINALIZADA'],
  ['r03', '3', 'soc-02', -7, 19, 1, 6, 'FINALIZADA'], ['r04', '7', 'soc-diego', -6, 7, 1, 1, 'FINALIZADA'],
  ['r05', '2', 'soc-05', -5, 17, 2, 12, 'FINALIZADA'], ['r06', '6', 'soc-08', -5, 10, 1, 2, 'FINALIZADA'],
  ['r07', '4', 'soc-10', -4, 20, 1, 8, 'FINALIZADA'], ['r08', '8', 'soc-mariana', -3, 8, 1, 1, 'FINALIZADA'],
  ['r09', '1', 'soc-diego', -2, 18, 2, 14, 'FINALIZADA'], ['r10', '5', 'soc-04', -2, 16, 1, 2, 'CANCELADA'],
  ['r11', '3', 'soc-05', -1, 19, 1, 6, 'FINALIZADA'], ['r12', '7', 'soc-01', -1, 7, 1, 1, 'FINALIZADA'],
  // Hoy (confirmadas más tarde; el encargado debe pasar asistencia de las ya empezadas)
  ['r13', '1', 'soc-02', 0, 8, 1, 8, 'CONFIRMADA'], ['r14', '5', 'soc-mariana', 0, 20, 1, 2, 'CONFIRMADA'],
  ['r15', '3', 'soc-10', 0, 19, 1, 8, 'CONFIRMADA'], ['r16', '7', 'soc-diego', 0, 18, 1, 1, 'CONFIRMADA'],
  // Próximos días
  ['r17', '6', 'soc-diego', 1, 9, 1, 2, 'CONFIRMADA'], ['r18', '2', 'soc-01', 1, 18, 2, 10, 'CONFIRMADA'],
  ['r19', '4', 'soc-05', 2, 19, 1, 6, 'CONFIRMADA'], ['r20', '8', 'soc-mariana', 3, 8, 1, 1, 'CONFIRMADA'],
  // Por aprobar (en revisión): una por deporte, para que cada encargado tenga algo pendiente
  ['r21', '1', 'soc-08', 1, 20, 1, 6, 'PENDIENTE_APROBACION'], ['r22', '3', 'soc-02', 1, 17, 1, 5, 'PENDIENTE_APROBACION'],
  ['r23', '5', 'soc-diego', 2, 11, 1, 2, 'PENDIENTE_APROBACION'], ['r24', '7', 'soc-05', 2, 9, 1, 1, 'PENDIENTE_APROBACION']
];
for (const [id, areaId, uid, d, h, horas, personas, estado] of reservas) {
  const a = areaPorId[areaId];
  const base = {
    usuarioId: uid, usuarioNombre: nombreDe[uid], areaId, areaNombre: a[1], deporte: a[2], fecha: dia(d),
    horaInicio: hh(h), horaFin: hh(h + horas), duracionHoras: horas, personas, carril: null, estado, esExterno: false,
    creadoEn: ts(d - 3), actualizadoEn: ts(d - 2)
  };
  if (estado === 'CANCELADA') Object.assign(base, { canceladaEn: ts(d - 1), sinPenalizacion: true });
  if (estado === 'CONFIRMADA' || estado === 'FINALIZADA') base.aprobadaEn = ts(d - 2);
  await set(`reservas/${id}`, base);
}
// Material apartado para algunas confirmadas + asistencia de reservas ya finalizadas
for (const [rid, hid, areaId, d] of [['r13', '10', '1', 0], ['r17', '11', '6', 1], ['r18', '10', '2', 1]]) {
  await set(`materialAsignado/ma-${rid}`, { reservaId: rid, herramientaId: hid, herramientaNombre: herramientas.find((h) => h[0] === hid)[1], areaId, fecha: dia(d), cantidad: 1, estado: 'ASIGNADO', creadoEn: ts(d - 1) });
}
for (const rid of ['r01', 'r02', 'r03', 'r05', 'r09']) {
  const r = reservas.find((x) => x[0] === rid);
  await set(`checkins/${rid}`, { reservaId: rid, usuarioId: r[2], asistencia: 'ASISTIO', registradoEn: ts(r[3]) });
}
await set('checkins/r06', { reservaId: 'r06', usuarioId: 'soc-08', asistencia: 'NO_ASISTIO', registradoEn: ts(-5) });

console.log('Creando torneos, avisos y publicaciones…');
await set('torneos/t-tenis', { nombre: 'Copa Athletic de Tenis', disciplina: 'Tenis', areaId: '5', areaNombre: 'Cancha de tenis 1', fechaInicio: dia(1), fechaFin: dia(2), horaInicio: '14:00', horaFin: '18:00', cupoMaximo: 8, inscritos: 3, creadoPor: 'adm-demo', creadoEn: ts(-10), actualizadoEn: ts(-2) });
await set('torneos/t-futbol', { nombre: 'Liga Relámpago de Fútbol 7', disciplina: 'Fútbol', areaId: '1', areaNombre: 'Cancha de fútbol 1', fechaInicio: dia(9), fechaFin: dia(10), horaInicio: '09:00', horaFin: '14:00', cupoMaximo: 16, inscritos: 2, creadoPor: 'adm-demo', creadoEn: ts(-8), actualizadoEn: ts(-2) });
await set('torneos/t-natacion', { nombre: 'Maratón de Natación de Primavera', disciplina: 'Natación', areaId: '7', areaNombre: 'Alberca olímpica', fechaInicio: dia(-14), fechaFin: dia(-14), horaInicio: '08:00', horaFin: '12:00', cupoMaximo: 20, inscritos: 2, creadoPor: 'adm-demo', creadoEn: ts(-30), actualizadoEn: ts(-14) });
for (const [t, u] of [['t-tenis', 'soc-mariana'], ['t-tenis', 'soc-diego'], ['t-tenis', 'soc-08'], ['t-futbol', 'soc-02'], ['t-futbol', 'soc-10'], ['t-natacion', 'soc-diego'], ['t-natacion', 'soc-01']]) {
  const torneoNombre = { 't-tenis': 'Copa Athletic de Tenis', 't-futbol': 'Liga Relámpago de Fútbol 7', 't-natacion': 'Maratón de Natación de Primavera' }[t];
  await set(`inscripcionesTorneo/${t}_${u}`, { torneoId: t, torneoNombre, usuarioId: u, usuarioNombre: nombreDe[u], fechaInscripcion: dia(-5), estado: 'CONFIRMADA', creadoEn: ts(-5) });
}
// Avisos viejos (no deben disparar notificación: la primera revisión "se pone al día")
const hace = (dias) => Date.now() - dias * 86400000;
await set('avisos/av-1', { titulo: 'Mantenimiento de la alberca olímpica', mensaje: 'La alberca olímpica estará cerrada el sábado de 6:00 a 10:00 por limpieza profunda.', destinatario: 'TODOS', autorId: 'adm-demo', autorNombre: 'Laura Mendoza Ortega', fechaMillis: hace(4), imagenUrl: '', vigencia: dia(10), creadoEn: ts(-4) });
await set('avisos/av-2', { titulo: 'Junta de personal', mensaje: 'Junta general de encargados el lunes a las 8:30 en la oficina.', destinatario: 'EMPLEADOS', autorId: 'adm-demo', autorNombre: 'Laura Mendoza Ortega', fechaMillis: hace(3), imagenUrl: '', vigencia: dia(7), creadoEn: ts(-3) });
await set('avisos/av-3', { titulo: 'Nuevo horario de tenis', mensaje: 'Las canchas de tenis abren desde las 7:00 de lunes a viernes.', destinatario: 'SOCIOS', autorId: 'sup-demo', autorNombre: 'Carlos Rivera Beltrán', fechaMillis: hace(2), imagenUrl: '', vigencia: dia(20), creadoEn: ts(-2) });
const PNG = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/lckAAAAASUVORK5CYII=';
await set('publicaciones/pub-1', { titulo: 'Inscripciones abiertas: Copa de Tenis', descripcion: 'Inscríbete en la Copa Athletic de Tenis. Cupo limitado a 8 jugadores.', imagenUrl: PNG, audiencia: 'TODOS', vigencia: dia(15), autorId: 'adm-demo', fechaMillis: hace(2), creadoEn: ts(-2) });
await set('publicaciones/pub-2', { titulo: 'Clases de natación para niños', descripcion: 'Nuevo grupo vespertino de 5 a 8 años con la instructora Daniela.', imagenUrl: PNG, audiencia: 'SOCIOS', vigencia: dia(30), autorId: 'adm-demo', fechaMillis: hace(1), creadoEn: ts(-1) });

console.log('Marcando el esquema como actualizado…');
await set('config/esquema', { version: 6, migradoEn: ahora, camposAgregados: {} });

const oficiales = miembros.filter((m) => m.oficial);
const fila = (rol, nombre, usuario, pass, extra = '') => `| ${rol} | ${nombre} | \`${usuario}\` | \`${pass}\` | ${extra} |`;
const md = [
  '# Cuentas de prueba (SOLO emuladores locales)',
  '',
  'Estas cuentas existen únicamente en el emulador de Firebase Authentication/Firestore local; **no existen en el Firebase real**.',
  'Se recrean con `node tools/datos-demo/sembrar-demo.mjs`. Inicio de sesión: correo + contraseña.',
  '',
  '| Rol | Nombre | Usuario (correo) | Contraseña | Notas |',
  '|---|---|---|---|---|',
  ...cuentas.map((c) => fila(c.rol === 'ADMIN' ? 'Administrador' : c.rol === 'SUPERVISOR' ? 'Supervisor' : 'Encargado de área', c.nombre, c.email, c.password, c.area ? `Área: ${c.area}` : '')),
  ...oficiales.map((m) => fila('Miembro (socio)', m.nombre, m.email, m.password, `Código ${m.codigo} · plan ${m.plan} · vence ${dia(m.vence)}`)),
  '',
  `Miembros de relleno (también pueden entrar): correo \`<id>@athleticclub.test\` con contraseña \`${PASS_RELLENO}\`:`,
  '',
  ...miembros.filter((m) => !m.oficial).map((m) => `- \`${correoRelleno(m)}\` — ${m.nombre} (${m.plan}, ${m.estado}, vence ${dia(m.vence)})`),
  '',
  '## Scripts (con los emuladores corriendo)',
  '',
  '- `node tools/datos-demo/sembrar-demo.mjs` — borra y vuelve a crear todos los datos y estas cuentas.',
  '- `node tools/datos-demo/probar-sistema.mjs` — permisos por rol y notificaciones de renovación.',
  '- `node tools/datos-demo/probar-membresias.mjs` — cambio de plan (subir/bajar), renovaciones, correos y notificaciones.',
  '- `node tools/datos-demo/probar-reservas-torneos.mjs` — reservas, aprobaciones, torneos, cupo y concurrencia.',
  '',
  'Los correos en el emulador quedan como `SIMULADO` en la colección `correos_enviados` (no se manda nada real).',
  ''
].join('\n');
writeFileSync(new URL('./CUENTAS.md', import.meta.url), md);
console.log('Listo. Credenciales en tools/datos-demo/CUENTAS.md');
