import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { readFileSync } from 'fs';
import { doc, getDoc, setDoc, updateDoc, deleteDoc, getDocs, collection, writeBatch, addDoc, query, where, deleteField } from 'firebase/firestore';

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

// ---- publicaciones del dashboard (independientes de avisos)
await prueba('sin sesión NO se leen publicaciones', () => assertFails(getDocs(collection(anonimo, 'publicaciones'))));
await env.withSecurityRulesDisabled(async ctx => {
  const db = ctx.firestore();
  await setDoc(doc(db, 'publicaciones/pub-socio'), { titulo: 'Socios', autorId: 'admin1', audiencia: 'SOCIOS', fechaMillis: 3 });
  await setDoc(doc(db, 'publicaciones/pub-personal'), { titulo: 'Personal', autorId: 'admin1', audiencia: 'PERSONAL', fechaMillis: 2 });
  await setDoc(doc(db, 'publicaciones/pub-todos'), { titulo: 'Todos', autorId: 'admin1', audiencia: 'TODOS', fechaMillis: 1 });
});
await prueba('un socio consulta solo publicaciones para socios y todos', async () => {
  const r = await assertSucceeds(getDocs(query(
    collection(nuevo1, 'publicaciones'),
    where('audiencia', 'in', ['SOCIOS', 'TODOS'])
  )));
  if (r.size !== 2) throw new Error('esperaba 2, hubo ' + r.size);
});
await prueba('un socio NO lee una publicación exclusiva para personal', () =>
  assertFails(getDoc(doc(nuevo1, 'publicaciones/pub-personal'))));
await prueba('el personal consulta solo publicaciones para personal y todos', async () => {
  const r = await assertSucceeds(getDocs(query(
    collection(ayudante, 'publicaciones'),
    where('audiencia', 'in', ['PERSONAL', 'TODOS'])
  )));
  if (r.size !== 2) throw new Error('esperaba 2, hubo ' + r.size);
});
await prueba('el personal NO lee una publicación exclusiva para socios', () =>
  assertFails(getDoc(doc(ayudante, 'publicaciones/pub-socio'))));
await prueba('el administrador consulta todas las publicaciones', () =>
  assertSucceeds(getDocs(collection(admin, 'publicaciones'))));
await prueba('solo el administrador crea publicaciones para audiencias válidas', async () => {
  await assertFails(addDoc(collection(ayudante, 'publicaciones'), {
    titulo: 'Novedad', autorId: 'ayudante1', audiencia: 'PERSONAL'
  }));
  await assertFails(addDoc(collection(admin, 'publicaciones'), {
    titulo: 'Novedad', autorId: 'admin1', audiencia: 'TODOS_INVALIDO'
  }));
  await assertSucceeds(addDoc(collection(admin, 'publicaciones'), {
    titulo: 'Novedad', descripcion: 'Descripción', imagenUrl: 'data:image/jpeg;base64,abc',
    autorId: 'admin1', audiencia: 'TODOS', vigencia: '2026-10-23', fechaMillis: Date.now()
  }));
});
await prueba('solo el administrador autor modifica o borra sus publicaciones', async () => {
  const id = (await addDoc(collection(admin, 'publicaciones'), {
    titulo: 'Otra', autorId: 'admin1', audiencia: 'SOCIOS'
  })).id;
  await assertFails(updateDoc(doc(ayudante, 'publicaciones/' + id), { titulo: 'Editada' }));
  await assertSucceeds(updateDoc(doc(admin, 'publicaciones/' + id), { titulo: 'Editada' }));
  await assertSucceeds(deleteDoc(doc(admin, 'publicaciones/' + id)));
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

// ---- precios: solo el administrador (ni el encargado ni un socio ni el titular de un plan)
await prueba('un encargado NO cambia precios aunque sea personal', () =>
  assertFails(setDoc(doc(ayudante, 'precios/FAMILIAR_1'), { precio: 1, moneda: 'MXN' })));
await prueba('un socio NO cambia precios', () => assertFails(setDoc(doc(miembroX, 'precios/FAMILIAR_1'), { precio: 1, moneda: 'MXN' })));
await prueba('el administrador SÍ cambia el precio de un paquete familiar', () =>
  assertSucceeds(setDoc(doc(admin, 'precios/FAMILIAR_1'), { precio: 7500, moneda: 'MXN' })));

// ---- plan familiar: el titular da de alta a su familia
await env.withSecurityRulesDisabled(async ctx => {
  const db = ctx.firestore();
  // Paquete "Pareja" (id 2, tope 2 personas) y paquete "Familiar" (id 1, tope 5); titulares ya registrados con su cuenta real.
  await setDoc(doc(db, 'membresias/fam1'), { usuarioId: 'viejoT1', tipo: 'FAMILIAR', paqueteFamiliarId: 1, estado: 'ACTIVA', totalPersonas: 1 });
  await setDoc(doc(db, 'membresias/par1'), { usuarioId: 'viejoT2', tipo: 'FAMILIAR', paqueteFamiliarId: 2, estado: 'ACTIVA', totalPersonas: 1 });
  await setDoc(doc(db, 'membresias/ind1'), { usuarioId: 'viejoT3', tipo: 'INDIVIDUAL', plan: 'NORMAL', estado: 'ACTIVA', totalPersonas: 1 });
  await setDoc(doc(db, 'membresias/fam2'), { usuarioId: 'viejoT4', tipo: 'FAMILIAR', paqueteFamiliarId: 1, estado: 'ACTIVA', totalPersonas: 1 });
  await setDoc(doc(db, 'membresias/famVencida'), { usuarioId: 'viejoT5', tipo: 'FAMILIAR', paqueteFamiliarId: 1, estado: 'SUSPENDIDA', totalPersonas: 1 });
  for (const [uid, cod, mem] of [['titular1', 'CLB-FAM001', 'fam1'], ['titular2', 'CLB-PAR001', 'par1'], ['titular3', 'CLB-IND001', 'ind1'],
                                  ['titular4', 'CLB-FAM004', 'fam2'], ['titular5', 'CLB-FAM005', 'famVencida']]) {
    await setDoc(doc(db, 'usuarios/' + uid), { nombre: 'T ' + uid, rol: 'SOCIO', estado: 'ACTIVO', codigoMiembro: cod });
    await setDoc(doc(db, 'miembros/' + cod), { nombre: 'T ' + uid, usuarioId: uid, membresiaId: mem, parentesco: 'Titular', esTitular: true, cuentaCreada: true });
  }
  await setDoc(doc(db, 'usuarios/hijoYa'), { nombre: 'Hijo', rol: 'SOCIO', estado: 'ACTIVO', codigoMiembro: 'CLB-HIJ001' });
  await setDoc(doc(db, 'miembros/CLB-HIJ001'), { nombre: 'Hijo', usuarioId: 'hijoYa', membresiaId: 'fam1', parentesco: 'Hijo', esTitular: false, cuentaCreada: true });
});
const titular1 = env.authenticatedContext('titular1').firestore();
const titular2 = env.authenticatedContext('titular2').firestore();
const titular3 = env.authenticatedContext('titular3').firestore();
const titular5 = env.authenticatedContext('titular5').firestore();
const hijoYa = env.authenticatedContext('hijoYa').firestore();

// Lo que hace la app al dar de alta a un familiar: miembro + registro del código + perfil interno + contador, en un lote.
function altaFamiliar(db, { membresia, total, codigo, uid, parentesco = 'Hijo', rol = 'SOCIO' }) {
  const lote = writeBatch(db);
  lote.set(doc(db, 'miembros/' + codigo), { nombre: 'Familiar', membresiaId: membresia, parentesco, esTitular: false, usuarioId: uid });
  lote.set(doc(db, 'usuarios/' + uid), { nombre: 'Familiar', rol, estado: 'ACTIVO', codigoMiembro: codigo });
  lote.set(doc(db, 'registroCodigos/' + codigo), { usuarioId: uid, cuentaCreada: false });
  lote.update(doc(db, 'membresias/' + membresia), { totalPersonas: total });
  return lote.commit();
}
await prueba('el titular del paquete Familiar da de alta a un familiar', () =>
  assertSucceeds(altaFamiliar(titular1, { membresia: 'fam1', total: 2, codigo: 'CLB-NEW001', uid: 'cuentaNew1' })));
await prueba('el titular ve a su familia (consulta por membresía)', async () => {
  const r = await assertSucceeds(getDocs(query(collection(titular1, 'miembros'), where('membresiaId', '==', 'fam1'))));
  if (r.size !== 3) throw new Error('esperaba 3 (titular, hijo y el nuevo), hubo ' + r.size);
});
await prueba('un familiar NO ve a los demás integrantes (solo lo suyo)', () =>
  assertFails(getDocs(query(collection(hijoYa, 'miembros'), where('membresiaId', '==', 'fam1')))));
await prueba('un familiar NO puede dar de alta a más gente', () =>
  assertFails(altaFamiliar(hijoYa, { membresia: 'fam1', total: 4, codigo: 'CLB-NEW002', uid: 'cuentaNew2' })));
await prueba('el titular NO puede dar de alta en la membresía de OTRA familia', () =>
  assertFails(altaFamiliar(titular1, { membresia: 'fam2', total: 2, codigo: 'CLB-NEW003', uid: 'cuentaNew3' })));
await prueba('el titular de un plan INDIVIDUAL NO puede agregar gente', () =>
  assertFails(altaFamiliar(titular3, { membresia: 'ind1', total: 2, codigo: 'CLB-NEW004', uid: 'cuentaNew4' })));
await prueba('una membresía suspendida NO permite agregar familiares', () =>
  assertFails(altaFamiliar(titular5, { membresia: 'famVencida', total: 2, codigo: 'CLB-NEW005', uid: 'cuentaNew5' })));
await prueba('el titular NO se salta el tope: la pareja (2) no admite un tercero', async () => {
  await assertSucceeds(altaFamiliar(titular2, { membresia: 'par1', total: 2, codigo: 'CLB-NEW006', uid: 'cuentaNew6', parentesco: 'Cónyuge' }));
  await assertFails(altaFamiliar(titular2, { membresia: 'par1', total: 3, codigo: 'CLB-NEW007', uid: 'cuentaNew7' }));
});
await prueba('el titular NO puede agregar sin subir el contador (saltarse el tope)', () => {
  const lote = writeBatch(titular1);
  lote.set(doc(titular1, 'miembros/CLB-NEW008'), { nombre: 'F', membresiaId: 'fam1', parentesco: 'Hijo', esTitular: false, usuarioId: 'cuentaNew8' });
  lote.set(doc(titular1, 'usuarios/cuentaNew8'), { nombre: 'F', rol: 'SOCIO', estado: 'ACTIVO', codigoMiembro: 'CLB-NEW008' });
  lote.set(doc(titular1, 'registroCodigos/CLB-NEW008'), { usuarioId: 'cuentaNew8', cuentaCreada: false });
  return assertFails(lote.commit());
});
await prueba('el titular NO puede darle a un familiar rol de administrador ni de visitante', async () => {
  await assertFails(altaFamiliar(titular1, { membresia: 'fam1', total: 3, codigo: 'CLB-NEW009', uid: 'cuentaNew9', rol: 'ADMIN' }));
  await assertFails(altaFamiliar(titular1, { membresia: 'fam1', total: 3, codigo: 'CLB-NEW010', uid: 'cuentaNew10', rol: 'VISITANTE_EXTERNO' }));
});
await prueba('el titular NO puede crear a otro "Titular"', () => {
  const lote = writeBatch(titular1);
  lote.set(doc(titular1, 'miembros/CLB-NEW011'), { nombre: 'F', membresiaId: 'fam1', parentesco: 'Titular', esTitular: true, usuarioId: 'cuentaNew11' });
  lote.set(doc(titular1, 'usuarios/cuentaNew11'), { nombre: 'F', rol: 'SOCIO', estado: 'ACTIVO', codigoMiembro: 'CLB-NEW011' });
  lote.set(doc(titular1, 'registroCodigos/CLB-NEW011'), { usuarioId: 'cuentaNew11', cuentaCreada: false });
  lote.update(doc(titular1, 'membresias/fam1'), { totalPersonas: 3 });
  return assertFails(lote.commit());
});
await prueba('el titular NO puede cambiar plan, precio ni estado de su membresía', async () => {
  await assertFails(updateDoc(doc(titular1, 'membresias/fam1'), { estado: 'ACTIVA', precio: 1 }));
  await assertFails(updateDoc(doc(titular1, 'membresias/fam1'), { paqueteFamiliarId: 1, totalPersonas: 2 }));
});
await prueba('el familiar invitado puede registrarse con su código (flujo normal de registro)', async () => {
  const interno = env.authenticatedContext('cuentaNew1').firestore();
  const lote = writeBatch(interno);
  lote.update(doc(interno, 'registroCodigos/CLB-NEW001'), { cuentaCreada: true, usuarioNuevo: 'familiarReal' });
  lote.update(doc(interno, 'miembros/CLB-NEW001'), { usuarioId: 'familiarReal', cuentaCreada: true, correoCuenta: 'f@correo.com' });
  await assertSucceeds(lote.commit());
  const real = env.authenticatedContext('familiarReal').firestore();
  await assertSucceeds(setDoc(doc(real, 'usuarios/familiarReal'), { nombre: 'Familiar', rol: 'SOCIO', estado: 'ACTIVO', codigoMiembro: 'CLB-NEW001', email: 'f@correo.com' }));
  await assertSucceeds(getDoc(doc(real, 'membresias/fam1')));
});

await env.cleanup();
console.log(fallos === 0 ? '\nTODAS LAS PRUEBAS DE REGLAS PASARON' : `\n${fallos} PRUEBA(S) FALLARON`);
process.exit(fallos === 0 ? 0 : 1);
