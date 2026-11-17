// Pruebas de las notificaciones del servidor: cada aviso debe quedar en la bandeja (colección `notificaciones`,
// lo que muestra la campanita) Y salir como push. Se simula firebase-admin/firebase-functions, sin emulador.
const test = require('node:test');
const assert = require('node:assert/strict');
const Module = require('node:module');

const datos = new Map();          // ruta -> documento
const pushes = [];                // mensajes enviados
let fallaPush = false;

const snapDoc = (ruta) => ({
  id: ruta.split('/').pop(), exists: datos.has(ruta), ref: { path: ruta, delete: async () => datos.delete(ruta) },
  data: () => datos.get(ruta), get: (k) => (datos.get(ruta) || {})[k]
});
const coleccion = (nombre) => {
  const consulta = (filtros) => ({
    where: (campo, op, valor) => consulta([...filtros, [campo, op, valor]]),
    get: async () => {
      const docs = [...datos.keys()].filter((r) => r.startsWith(nombre + '/') && r.split('/').length === 2)
        .filter((r) => filtros.every(([c, op, v]) => (op === 'in' ? v.includes(datos.get(r)[c]) : datos.get(r)[c] === v)))
        .map(snapDoc);
      return { docs, empty: docs.length === 0 };
    }
  });
  return { ...consulta([]), doc: (id) => ({ path: `${nombre}/${id}`, create: async (d) => { if (datos.has(`${nombre}/${id}`)) throw Object.assign(new Error('ya existe'), { code: 6 }); datos.set(`${nombre}/${id}`, d); }, set: async (d) => datos.set(`${nombre}/${id}`, d), update: async (d) => datos.set(`${nombre}/${id}`, { ...datos.get(`${nombre}/${id}`), ...d }), get: async () => snapDoc(`${nombre}/${id}`) }) };
};
const db = {
  collection: coleccion,
  doc: (ruta) => ({ path: ruta, get: async () => snapDoc(ruta), set: async (d) => datos.set(ruta, d) }),
  runTransaction: async (fn) => fn({
    get: async (ref) => snapDoc(ref.path),
    create: (ref, d) => { if (datos.has(ref.path)) throw new Error('ya existe'); datos.set(ref.path, d); }
  })
};

const falsos = {
  'firebase-functions/v2/https': { onCall: (_o, f) => f, HttpsError: class extends Error {} },
  'firebase-functions/v2/firestore': { onDocumentCreated: (_o, f) => f, onDocumentUpdated: (_o, f) => f },
  'firebase-functions/v2/scheduler': { onSchedule: (_o, f) => f },
  'firebase-functions/params': { defineString: () => ({ value: () => '' }), defineSecret: () => ({ value: () => '' }) },
  'firebase-admin/app': { initializeApp: () => {} },
  'firebase-admin/auth': { getAuth: () => ({}) },
  'firebase-admin/firestore': { getFirestore: () => db, FieldValue: {} },
  'firebase-admin/messaging': {
    getMessaging: () => ({
      send: async (m) => { if (fallaPush) throw Object.assign(new Error('x'), { code: 'messaging/internal-error' }); pushes.push(m); }
    })
  }
};
const original = Module._load;
Module._load = function (peticion, ...resto) { return falsos[peticion] || original.call(this, peticion, ...resto); };
const funciones = require('./index.js');
Module._load = original;

const reiniciar = () => {
  datos.clear(); pushes.length = 0; fallaPush = false;
  datos.set('usuarios/admin1', { rol: 'ADMIN', estado: 'ACTIVO' });
  datos.set('usuarios/sup1', { rol: 'SUPERVISOR', estado: 'ACTIVO' });
  datos.set('usuarios/viejo', { rol: 'ADMIN', estado: 'INACTIVO' });
  datos.set('usuarios/socio1', { rol: 'SOCIO', estado: 'ACTIVO' });
  for (const u of ['admin1', 'sup1', 'socio1']) datos.set(`fcmTokens/t_${u}`, { usuarioId: u, token: `tok_${u}` });
};
const bandeja = (uid) => [...datos.entries()].filter(([r, d]) => r.startsWith('notificaciones/') && d.usuarioId === uid).map(([, d]) => d);
const evento = (antes, despues, params = { solicitudId: 's1' }) => ({
  id: 'ev1', params, data: { data: () => despues, before: { data: () => antes }, after: { data: () => despues } }
});

test('el socio sube un pago: el personal recibe push Y notificación en su campanita', async () => {
  reiniciar();
  await funciones.notificarNuevaRenovacion({ ...evento(null, { status: 'PENDIENTE', socioId: 'socio1', socioNombre: 'Mario', monto: 500 }), data: { data: () => ({ status: 'PENDIENTE', socioId: 'socio1', socioNombre: 'Mario', monto: 500 }) } });
  for (const uid of ['admin1', 'sup1']) {
    assert.equal(bandeja(uid).length, 1, `bandeja de ${uid}`);
    assert.equal(bandeja(uid)[0].tipo, 'RENOVACION');
    assert.equal(bandeja(uid)[0].leida, false);
    assert.ok(pushes.some((p) => p.token === `tok_${uid}`), `push a ${uid}`);
  }
  assert.equal(bandeja('viejo').length, 0, 'un usuario INACTIVO no recibe nada');
  assert.equal(bandeja('socio1').length, 0, 'el socio no recibe el aviso del personal');
});

test('pago rechazado: el socio recibe push Y notificación en su campanita', async () => {
  reiniciar();
  await funciones.notificarResolucionRenovacion(evento(
    { status: 'PENDIENTE', socioId: 'socio1' },
    { status: 'RECHAZADO', socioId: 'socio1', motivoRechazo: 'Comprobante ilegible' }
  ));
  const [n] = bandeja('socio1');
  assert.equal(n.titulo, 'Renovación rechazada');
  assert.match(n.mensaje, /Comprobante ilegible/);
  assert.equal(n.leida, false);
  assert.equal(pushes.length, 1);
  assert.equal(pushes[0].data.notificacionId, n.id, 'el push apunta al mismo documento de la bandeja');
});

test('pago aprobado: queda en la bandeja del socio', async () => {
  reiniciar();
  await funciones.notificarResolucionRenovacion(evento({ status: 'PENDIENTE', socioId: 'socio1' }, { status: 'APROBADO', socioId: 'socio1' }));
  assert.equal(bandeja('socio1')[0].titulo, 'Renovación aprobada');
});

test('si el push falla o no hay token, la notificación de la campanita igual se guarda', async () => {
  reiniciar(); fallaPush = true;
  await funciones.notificarResolucionRenovacion(evento({ status: 'PENDIENTE', socioId: 'socio1' }, { status: 'RECHAZADO', socioId: 'socio1' }));
  assert.equal(bandeja('socio1').length, 1);
  reiniciar(); datos.delete('fcmTokens/t_socio1');
  await funciones.notificarResolucionRenovacion(evento({ status: 'PENDIENTE', socioId: 'socio1' }, { status: 'RECHAZADO', socioId: 'socio1' }));
  assert.equal(bandeja('socio1').length, 1);
});

test('un evento repetido no duplica ni la notificación ni el push', async () => {
  reiniciar();
  const ev = evento({ status: 'PENDIENTE', socioId: 'socio1' }, { status: 'RECHAZADO', socioId: 'socio1' });
  await funciones.notificarResolucionRenovacion(ev);
  await funciones.notificarResolucionRenovacion(ev);
  assert.equal(bandeja('socio1').length, 1);
  assert.equal(pushes.length, 1);
});

test('cambio de plan rechazado llega a la campanita del socio', async () => {
  reiniciar();
  await funciones.notificarResolucionCambioPlan(evento(
    { status: 'PENDIENTE' }, { status: 'RECHAZADO', socioId: 'socio1', planNuevo: 'DELUXE', motivoRechazo: 'x' }));
  assert.equal(bandeja('socio1').length, 1);
  assert.equal(bandeja('socio1')[0].tipo, 'CAMBIO_PLAN');
});
