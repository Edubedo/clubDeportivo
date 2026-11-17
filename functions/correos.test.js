const test = require('node:test');
const assert = require('node:assert');
const C = require('./correos');

test('elige el correo real de la cuenta; si es interno, el de contacto; si no hay, null', () => {
  assert.strictEqual(C.elegirCorreo({ correoCuenta: 'ana@correo.com', correoContacto: 'otro@x.com' }), 'ana@correo.com');
  assert.strictEqual(C.elegirCorreo({ correoCuenta: 'clb-abc234@miembros.clubdeportivo.app', correoContacto: 'contacto@x.com' }), 'contacto@x.com');
  assert.strictEqual(C.elegirCorreo({ correoCuenta: 'clb-abc234@miembros.clubdeportivo.app', correoContacto: '' }), null);
  assert.strictEqual(C.elegirCorreo({ correoCuenta: 'no-es-correo', correoContacto: undefined }), null);
});

test('fechas y dinero legibles', () => {
  assert.strictEqual(C.fechaLegible('2026-12-05'), '5 de diciembre de 2026');
  assert.strictEqual(C.dinero(3500), '$3,500');
  assert.strictEqual(C.nombrePlan('DELUXE'), 'Individual deluxe');
});

test('cada compositor trae asunto, texto y html con el nombre y los datos', () => {
  const casos = {
    cambioPlanRecibido: { nombre: 'Mariana López', planActual: 'NORMAL', planNuevo: 'DELUXE', precioNuevo: 3500 },
    cambioPlanAprobado: { nombre: 'Mariana López', planAnterior: 'NORMAL', planNuevo: 'DELUXE', precioNuevo: 3500 },
    cambioPlanRechazado: { nombre: 'Mariana López', planNuevo: 'DELUXE', motivo: 'Adeudo pendiente' },
    planCambiadoPorElClub: { nombre: 'Mariana López', planAnterior: 'NORMAL', planNuevo: 'NINO', precioNuevo: 1500 },
    renovacionRecibida: { nombre: 'Mariana López', monto: 1800 },
    renovacionAprobada: { nombre: 'Mariana López', hasta: '2026-12-17' },
    renovacionRechazada: { nombre: 'Mariana López', motivo: 'Comprobante ilegible' },
    membresiaSuspendida: { nombre: 'Mariana López', motivo: 'Falta de pago' },
    membresiaReactivada: { nombre: 'Mariana López', hasta: '2026-12-17' }
  };
  for (const [tipo, datos] of Object.entries(casos)) {
    const c = C.compositores[tipo](datos);
    assert.ok(c.asunto && c.texto && c.html, tipo);
    assert.match(c.texto, /Hola Mariana,/, tipo);
    assert.match(c.html, /<html/, tipo);
  }
  assert.match(C.compositores.cambioPlanRechazado(casos.cambioPlanRechazado).texto, /Adeudo pendiente/);
  assert.match(C.compositores.renovacionAprobada(casos.renovacionAprobada).texto, /17 de diciembre de 2026/);
  assert.match(C.compositores.cambioPlanAprobado(casos.cambioPlanAprobado).texto, /\$3,500/);
});

test('el html escapa lo que escribe el personal (motivos)', () => {
  const c = C.compositores.cambioPlanRechazado({ nombre: 'A', planNuevo: 'DELUXE', motivo: '<script>alert(1)</script>' });
  assert.ok(!c.html.includes('<script>'));
  assert.ok(c.html.includes('&lt;script&gt;'));
});

test('cambios en la membresía -> qué correos', () => {
  const base = { tipo: 'INDIVIDUAL', plan: 'NORMAL', precio: 1800, estado: 'ACTIVA', renovaciones: 1, fechaVencimiento: '2026-11-17' };
  const tipos = (antes, despues) => C.correosPorCambioDeMembresia(antes, despues).map((x) => x.tipo);
  assert.deepStrictEqual(tipos(base, { ...base, renovaciones: 2, fechaVencimiento: '2026-12-17' }), ['renovacionAprobada']);
  assert.deepStrictEqual(tipos(base, { ...base, estado: 'SUSPENDIDA', motivoSuspension: 'Falta de pago' }), ['membresiaSuspendida']);
  assert.deepStrictEqual(tipos({ ...base, estado: 'SUSPENDIDA' }, base), ['membresiaReactivada']);
  assert.deepStrictEqual(tipos(base, { ...base, plan: 'DELUXE', precio: 3500 }), ['planCambiadoPorElClub']);
  // por solicitud: el correo sale con la solicitud, no aquí
  assert.deepStrictEqual(tipos(base, { ...base, plan: 'DELUXE', precio: 3500, cambioPlanSolicitudId: 's1' }), []);
  // sin cambios relevantes (p. ej. solo actualizadoEn o el comprobante)
  assert.deepStrictEqual(tipos(base, { ...base, actualizadoEn: 'x' }), []);
  // un paquete familiar no dispara "cambio de plan individual"
  assert.deepStrictEqual(tipos({ ...base, tipo: 'FAMILIAR', plan: undefined }, { ...base, tipo: 'FAMILIAR', plan: undefined, precio: 7500 }), []);
  // renovar y cambiar de plan a la vez: ambos
  assert.deepStrictEqual(tipos(base, { ...base, renovaciones: 2, plan: 'NINO', precio: 1500 }), ['renovacionAprobada', 'planCambiadoPorElClub']);
});

test('despachar: simula sin transporte, envía con transporte, es idempotente y anota errores', async () => {
  const guardado = {};
  const db = { collection: () => ({ doc: (id) => ({
    create: async (d) => { if (guardado[id]) { const e = new Error('ALREADY_EXISTS: already exists'); e.code = 6; throw e; } guardado[id] = { ...d }; },
    update: async (d) => { Object.assign(guardado[id], d); }
  }) }) };
  const correo = C.compositores.renovacionAprobada({ nombre: 'Ana', hasta: '2026-12-17' });
  assert.strictEqual((await C.despachar({ db, id: 'e1', transporte: null, de: 'x', para: 'a@b.com', correo, tipo: 't' })).estado, 'SIMULADO');
  assert.strictEqual((await C.despachar({ db, id: 'e1', transporte: null, de: 'x', para: 'a@b.com', correo, tipo: 't' })).estado, 'DUPLICADO');
  assert.strictEqual((await C.despachar({ db, id: 'e2', transporte: null, de: 'x', para: null, correo, tipo: 't' })).estado, 'SIN_CORREO');
  const enviados = [];
  const ok = { sendMail: async (m) => enviados.push(m) };
  assert.strictEqual((await C.despachar({ db, id: 'e3', transporte: ok, de: 'club@x.com', para: 'a@b.com', correo, tipo: 't' })).estado, 'ENVIADO');
  assert.strictEqual(enviados[0].to, 'a@b.com');
  assert.strictEqual(enviados[0].subject, 'Tu membresía fue renovada');
  const mal = { sendMail: async () => { const e = new Error('x'); e.code = 'EAUTH'; throw e; } };
  assert.strictEqual((await C.despachar({ db, id: 'e4', transporte: mal, de: 'x', para: 'a@b.com', correo, tipo: 't' })).estado, 'ERROR');
  assert.strictEqual(guardado.e4.error, 'EAUTH');
});

test('correos de un familiar quitado: al titular (con su lugar libre) y a quien se quitó', () => {
  const t = C.compositores.familiarQuitadoTitular({ nombre: 'Mariana López', familiar: 'Pedro López', lugaresLibres: 1 });
  assert.match(t.texto, /Hola Mariana,/);
  assert.match(t.texto, /Pedro López/);
  assert.match(t.texto, /lugar libre/);
  assert.match(t.html, /Lugares libres en tu paquete: 1/);
  const f = C.compositores.familiarQuitadoFamiliar({ nombre: 'Pedro López', titular: 'Mariana López' });
  assert.match(f.texto, /Hola Pedro,/);
  assert.match(f.texto, /Mariana López te quitó/);
  assert.match(f.asunto, /suspendido/);
});
