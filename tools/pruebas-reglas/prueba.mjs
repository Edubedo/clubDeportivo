import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { readFileSync } from 'fs';
import { doc, getDoc, setDoc, updateDoc, getDocs, collection, writeBatch, addDoc, query, where, deleteField } from 'firebase/firestore';

const env = await initializeTestEnvironment({
  projectId: 'demo-athletic',
  firestore: { rules: readFileSync('../../firestore.rules', 'utf8'), host: '127.0.0.1', port: 8089 }
});

let fallos = 0;
async function prueba(nombre, fn) {
  try { await fn(); console.log('OK   ', nombre); } catch (e) { fallos++; console.log('FALLA', nombre, '\n      ', String(e.message).split('\n')[0]); }
}

await env.withSecurityRulesDisabled(async ctx => {
  const db = ctx.firestore();
  await setDoc(doc(db, 'usuarios/admin1'), { nombre: 'Admin', rol: 'ADMIN', estado: 'ACTIVO' });
  await setDoc(doc(db, 'usuarios/ayudante1'), { nombre: 'Ayu', rol: 'AYUDANTE_AREA', estado: 'ACTIVO', areaTrabajo: 'Fútbol' });
  await setDoc(doc(db, 'usuarios/ayudante2'), { nombre: 'Ayu2', rol: 'AYUDANTE_AREA', estado: 'ACTIVO', areaTrabajo: 'Tenis' });
  await setDoc(doc(db, 'usuarios/ayudante3'), { nombre: 'Ayu3', rol: 'AYUDANTE_AREA', estado: 'ACTIVO', areaTrabajo: '' });
  await setDoc(doc(db, 'usuarios/interno1'), { nombre: 'Ana', rol: 'SOCIO', estado: 'ACTIVO', codigoMiembro: 'CLB-AAAAAA' });
  await setDoc(doc(db, 'usuarios/interno2'), { nombre: 'Visita', rol: 'VISITANTE_EXTERNO', estado: 'ACTIVO', codigoMiembro: 'CLB-BBBBBB' });
  await setDoc(doc(db, 'miembros/CLB-AAAAAA'), { nombre: 'Ana', usuarioId: 'interno1', membresiaId: 'm1', parentesco: 'Titular' });
  await setDoc(doc(db, 'miembros/CLB-BBBBBB'), { nombre: 'Visita', usuarioId: 'interno2', membresiaId: 'm2', parentesco: 'Titular' });
  await setDoc(doc(db, 'registroCodigos/CLB-AAAAAA'), { usuarioId: 'interno1', cuentaCreada: false });
  await setDoc(doc(db, 'registroCodigos/CLB-BBBBBB'), { usuarioId: 'interno2', cuentaCreada: false });
  await setDoc(doc(db, 'membresias/m1'), { usuarioId: 'interno1', estado: 'ACTIVA', tipo: 'INDIVIDUAL' });
  await setDoc(doc(db, 'areas/a1'), { nombre: 'Cancha' });
});

const anonimo = env.unauthenticatedContext().firestore();
const interno1 = env.authenticatedContext('interno1').firestore();
const interno2 = env.authenticatedContext('interno2').firestore();
const nuevo1 = env.authenticatedContext('nuevo1').firestore();
const intruso = env.authenticatedContext('intruso').firestore();
const admin = env.authenticatedContext('admin1').firestore();
const ayudante = env.authenticatedContext('ayudante1').firestore();
const ayudanteTenis = env.authenticatedContext('ayudante2').firestore();
const ayudanteSinArea = env.authenticatedContext('ayudante3').firestore();

// ---- consulta del código antes de iniciar sesión
await prueba('sin sesión se puede consultar UN código', () => assertSucceeds(getDoc(doc(anonimo, 'registroCodigos/CLB-AAAAAA'))));
await prueba('sin sesión NO se pueden listar los códigos', () => assertFails(getDocs(collection(anonimo, 'registroCodigos'))));
await prueba('sin sesión NO se leen los miembros', () => assertFails(getDoc(doc(anonimo, 'miembros/CLB-AAAAAA'))));
await prueba('sin sesión NO se puede crear un perfil', () => assertFails(setDoc(doc(anonimo, 'usuarios/x'), { nombre: 'x', rol: 'SOCIO', estado: 'ACTIVO' })));

// ---- intentos de abuso antes de reclamar
await prueba('una cuenta ajena NO puede marcar un código como registrado', () =>
  assertFails(updateDoc(doc(intruso, 'registroCodigos/CLB-AAAAAA'), { cuentaCreada: true, usuarioNuevo: 'intruso' })));
await prueba('una cuenta con sesión pero sin código NO puede crearse perfil de socio', () =>
  assertFails(setDoc(doc(intruso, 'usuarios/intruso'), { nombre: 'I', rol: 'SOCIO', estado: 'ACTIVO' })));
await prueba('NO se puede crear perfil con un código sin haberlo reclamado', () =>
  assertFails(setDoc(doc(nuevo1, 'usuarios/nuevo1'), { nombre: 'Ana', rol: 'SOCIO', estado: 'ACTIVO', codigoMiembro: 'CLB-AAAAAA' })));

// ---- registro completo de Ana (sesión del código = interno1)
await prueba('la cuenta interna lee su miembro', () => assertSucceeds(getDoc(doc(interno1, 'miembros/CLB-AAAAAA'))));
await prueba('la cuenta interna NO puede reclamar el código de otra persona', () =>
  assertFails(updateDoc(doc(interno1, 'registroCodigos/CLB-BBBBBB'), { cuentaCreada: true, usuarioNuevo: 'nuevo1' })));
await prueba('reclamo atómico del código y del miembro por su cuenta interna', async () => {
  const lote = writeBatch(interno1);
  lote.update(doc(interno1, 'registroCodigos/CLB-AAAAAA'), { cuentaCreada: true, usuarioNuevo: 'nuevo1' });
  lote.update(doc(interno1, 'miembros/CLB-AAAAAA'), { usuarioId: 'nuevo1', cuentaCreada: true, correoCuenta: 'ana@correo.com' });
  await assertSucceeds(lote.commit());
});
await prueba('la cuenta interna NO puede cambiar otros campos del miembro', () =>
  assertFails(updateDoc(doc(interno1, 'miembros/CLB-AAAAAA'), { nombre: 'Otra' })));
await prueba('NO se crea perfil con rol distinto al que dio el personal', () =>
  assertFails(setDoc(doc(nuevo1, 'usuarios/nuevo1'), { nombre: 'Ana', rol: 'VISITANTE_EXTERNO', estado: 'ACTIVO', codigoMiembro: 'CLB-AAAAAA' })));
await prueba('NO se crea perfil de ADMIN con un código', () =>
  assertFails(setDoc(doc(nuevo1, 'usuarios/nuevo1'), { nombre: 'Ana', rol: 'ADMIN', estado: 'ACTIVO', codigoMiembro: 'CLB-AAAAAA' })));
await prueba('otro usuario NO puede crear perfil con el código ya reclamado por nuevo1', () =>
  assertFails(setDoc(doc(intruso, 'usuarios/intruso'), { nombre: 'I', rol: 'SOCIO', estado: 'ACTIVO', codigoMiembro: 'CLB-AAAAAA' })));
await prueba('la cuenta nueva SÍ crea su perfil con el código reclamado', () =>
  assertSucceeds(setDoc(doc(nuevo1, 'usuarios/nuevo1'), { nombre: 'Ana', rol: 'SOCIO', estado: 'ACTIVO', codigoMiembro: 'CLB-AAAAAA', email: 'ana@correo.com' })));
await prueba('la cuenta nueva lee su miembro (usuarioId ya es el suyo)', () => assertSucceeds(getDoc(doc(nuevo1, 'miembros/CLB-AAAAAA'))));
await prueba('la cuenta nueva lee su membresía por el código', () => assertSucceeds(getDoc(doc(nuevo1, 'membresias/m1'))));
await prueba('la cuenta nueva NO puede cambiarse el rol', () => assertFails(updateDoc(doc(nuevo1, 'usuarios/nuevo1'), { rol: 'ADMIN' })));
await prueba('la cuenta nueva lee las áreas y puede reservar a su nombre', async () => {
  await assertSucceeds(getDoc(doc(nuevo1, 'areas/a1')));
  await assertSucceeds(addDoc(collection(nuevo1, 'reservas'), { usuarioId: 'nuevo1', areaId: 'a1', estado: 'PENDIENTE_APROBACION' }));
});
await prueba('un miembro NO puede crear una reserva ya confirmada (siempre queda en revisión)', () =>
  assertFails(addDoc(collection(nuevo1, 'reservas'), { usuarioId: 'nuevo1', areaId: 'a1', estado: 'CONFIRMADA' })));
await prueba('ya reclamado: se consulta como "ya registrado"', async () => {
  const s = await assertSucceeds(getDoc(doc(anonimo, 'registroCodigos/CLB-AAAAAA')));
  if (s.data().cuentaCreada !== true) throw new Error('debería estar reclamado');
});

// ---- deshacer un registro a medias (lo hace la cuenta interna)
await prueba('si el registro falla, la cuenta interna puede liberar el código de Visita', async () => {
  const lote = writeBatch(interno2);
  lote.update(doc(interno2, 'registroCodigos/CLB-BBBBBB'), { cuentaCreada: true, usuarioNuevo: 'nuevo2' });
  lote.update(doc(interno2, 'miembros/CLB-BBBBBB'), { usuarioId: 'nuevo2', cuentaCreada: true });
  await assertSucceeds(lote.commit());
  // el reclamo se deshace con la sesión interna? ya no es dueña (usuarioId cambió en miembros) pero sí en registroCodigos
  await assertSucceeds(updateDoc(doc(interno2, 'registroCodigos/CLB-BBBBBB'), { cuentaCreada: false, usuarioNuevo: deleteField() }));
});

// ---- avisos
await env.withSecurityRulesDisabled(async ctx => {
  const db = ctx.firestore();
  await setDoc(doc(db, 'avisos/av1'), { titulo: 'Socios', destinatario: 'SOCIOS', autorId: 'admin1' });
  await setDoc(doc(db, 'avisos/av2'), { titulo: 'Empleados', destinatario: 'EMPLEADOS', autorId: 'admin1' });
  await setDoc(doc(db, 'avisos/av3'), { titulo: 'Todos', destinatario: 'TODOS', autorId: 'admin1' });
});
await prueba('un socio lee avisos para socios y para todos con la consulta de la app', async () => {
  const r = await assertSucceeds(getDocs(query(collection(nuevo1, 'avisos'), where('destinatario', 'in', ['SOCIOS', 'TODOS']))));
  if (r.size !== 2) throw new Error('esperaba 2, hubo ' + r.size);
});
await prueba('un socio NO lee un aviso para empleados', () => assertFails(getDoc(doc(nuevo1, 'avisos/av2'))));
await prueba('un socio NO puede crear avisos', () => assertFails(addDoc(collection(nuevo1, 'avisos'), { titulo: 'x', destinatario: 'TODOS', autorId: 'nuevo1' })));
await prueba('el personal lee todos los avisos', () => assertSucceeds(getDocs(collection(ayudante, 'avisos'))));
await prueba('el personal crea un aviso a su nombre', () => assertSucceeds(addDoc(collection(ayudante, 'avisos'), { titulo: 'x', destinatario: 'SOCIOS', autorId: 'ayudante1' })));
await prueba('el personal NO crea un aviso a nombre de otro', () => assertFails(addDoc(collection(ayudante, 'avisos'), { titulo: 'x', destinatario: 'SOCIOS', autorId: 'admin1' })));
await prueba('el personal NO edita avisos ajenos; el admin sí', async () => {
  await assertFails(updateDoc(doc(ayudante, 'avisos/av1'), { titulo: 'hack' }));
  await assertSucceeds(updateDoc(doc(admin, 'avisos/av1'), { titulo: 'ok' }));
});

// ---- pagos, miembros, personal
await prueba('el personal registra un cobro y un socio NO lo lee', async () => {
  await assertSucceeds(addDoc(collection(ayudante, 'pagos'), { monto: 100, fechaPago: '2026-10-07' }));
  await assertFails(getDocs(collection(nuevo1, 'pagos')));
});
await prueba('el personal da de alta un miembro y su código', async () => {
  await assertSucceeds(setDoc(doc(ayudante, 'registroCodigos/CLB-CCCCCC'), { usuarioId: 'x', cuentaCreada: false }));
  await assertSucceeds(setDoc(doc(ayudante, 'miembros/CLB-CCCCCC'), { nombre: 'N', usuarioId: 'x', membresiaId: 'm3' }));
  await assertSucceeds(setDoc(doc(ayudante, 'usuarios/x'), { nombre: 'N', rol: 'SOCIO', estado: 'ACTIVO', codigoMiembro: 'CLB-CCCCCC' }));
});
await prueba('el personal desactiva la cuenta anterior de un miembro (restablecer acceso)', () =>
  assertSucceeds(updateDoc(doc(ayudante, 'usuarios/nuevo1'), { estado: 'INACTIVO' })));
await prueba('el encargado NO crea personal; el admin sí', async () => {
  await assertFails(setDoc(doc(ayudante, 'usuarios/emp1'), { nombre: 'E', rol: 'AYUDANTE_AREA', estado: 'ACTIVO' }));
  await assertSucceeds(setDoc(doc(admin, 'usuarios/emp1'), { nombre: 'E', rol: 'AYUDANTE_AREA', estado: 'ACTIVO' }));
});

await prueba('los precios los cambia solo un administrador', async () => {
  await assertSucceeds(setDoc(doc(admin, 'precios/individual-normal'), { monto: 1900 }));
  await assertFails(setDoc(doc(ayudante, 'precios/individual-normal'), { monto: 1 }));
  await assertSucceeds(getDoc(doc(nuevo1, 'precios/individual-normal')));
});
await prueba('nadie puede crearse un perfil de socio por su cuenta sin código (cierra el registro abierto)', () =>
  assertFails(setDoc(doc(intruso, 'usuarios/intruso'), { nombre: 'I', rol: 'SOCIO', estado: 'ACTIVO' })));
await prueba('una cuenta desactivada no puede reservar', async () => {
  await assertSucceeds(updateDoc(doc(admin, 'usuarios/nuevo1'), { estado: 'INACTIVO' }));
  await assertFails(addDoc(collection(nuevo1, 'reservas'), { usuarioId: 'nuevo1', areaId: 'a1', estado: 'PENDIENTE_APROBACION' }));
});

// ---- aprobación de reservas: solo el encargado del área (o un administrador)
await env.withSecurityRulesDisabled(async ctx => {
  const db = ctx.firestore();
  await setDoc(doc(db, 'usuarios/miembroX'), { nombre: 'Mario', rol: 'SOCIO', estado: 'ACTIVO' });
  for (const id of ['rA', 'rB', 'rC', 'rD']) {
    await setDoc(doc(db, 'reservas/' + id), { usuarioId: 'miembroX', areaId: 'a1', deporte: 'Fútbol', estado: 'PENDIENTE_APROBACION' });
  }
});
const miembroX = env.authenticatedContext('miembroX').firestore();
await prueba('el miembro NO puede aprobar su propia reserva', () => assertFails(updateDoc(doc(miembroX, 'reservas/rA'), { estado: 'CONFIRMADA' })));
await prueba('el miembro SÍ puede cancelar su solicitud en revisión', () => assertSucceeds(updateDoc(doc(miembroX, 'reservas/rD'), { estado: 'CANCELADA' })));
await prueba('el encargado de OTRA área (Tenis) NO aprueba una reserva de Fútbol', () =>
  assertFails(updateDoc(doc(ayudanteTenis, 'reservas/rA'), { estado: 'CONFIRMADA' })));
await prueba('un encargado sin área NO aprueba nada', () => assertFails(updateDoc(doc(ayudanteSinArea, 'reservas/rA'), { estado: 'CONFIRMADA' })));
await prueba('el encargado del área (Fútbol) aprueba', () => assertSucceeds(updateDoc(doc(ayudante, 'reservas/rA'), { estado: 'CONFIRMADA' })));
await prueba('el encargado del área (Fútbol) rechaza', () => assertSucceeds(updateDoc(doc(ayudante, 'reservas/rB'), { estado: 'RECHAZADA' })));
await prueba('el administrador aprueba cualquier reserva', () => assertSucceeds(updateDoc(doc(admin, 'reservas/rC'), { estado: 'CONFIRMADA' })));

await env.cleanup();
console.log(fallos === 0 ? '\nTODAS LAS PRUEBAS DE REGLAS PASARON' : `\n${fallos} PRUEBA(S) FALLARON`);
process.exit(fallos === 0 ? 0 : 1);
