const test = require('node:test');
const assert = require('node:assert');
const nodemailer = require('nodemailer');
const { esCodigo, enlaceDeBienvenida, componerBienvenida, enviarBienvenida } = require('./bienvenida');

const base = 'https://clubdeportivoudc.web.app/unirse';
const transporteDePrueba = () => nodemailer.createTransport({ jsonTransport: true });

test('códigos válidos e inválidos (mismas reglas que ReglasMembresia.kt)', () => {
  assert.ok(esCodigo('CLB-7K3M9Q'));
  for (const mal of ['clb-7k3m9q', 'CLB-123', 'CLB-0OIL11', 'CLB-7K3M9QX', '', null, undefined]) assert.ok(!esCodigo(mal), String(mal));
});

test('el enlace lleva el código como parámetro', () => {
  assert.strictEqual(enlaceDeBienvenida(base, 'CLB-7K3M9Q'), `${base}?codigo=CLB-7K3M9Q`);
});

test('el correo trae el código y el enlace, y escapa el nombre en el html', () => {
  const enlace = enlaceDeBienvenida(base, 'CLB-7K3M9Q');
  const { asunto, texto, html } = componerBienvenida({ nombre: '<b>Ana</b> Ruiz', codigo: 'CLB-7K3M9Q', enlace });
  assert.ok(asunto.includes('CLB-7K3M9Q'));
  assert.ok(texto.includes('CLB-7K3M9Q') && texto.includes(enlace));
  assert.ok(html.includes('CLB-7K3M9Q') && html.includes(`href="${enlace}"`));
  assert.ok(!html.includes('<b>Ana</b>'));
});

test('se manda al correo del miembro', async () => {
  const t = transporteDePrueba();
  let enviado;
  const original = t.sendMail.bind(t);
  t.sendMail = async (m) => { enviado = await original(m); return enviado; };
  const r = await enviarBienvenida({
    transporte: t, de: 'Athletic Club <club@ejemplo.com>', baseEnlace: base,
    miembro: { nombre: 'Eduardo Escobedo', correo: ' ezescobedo27@gmail.com ' }, codigo: 'CLB-7K3M9Q'
  });
  assert.deepStrictEqual(r, { enviado: true });
  const m = JSON.parse(enviado.message);
  assert.deepStrictEqual(m.to, [{ address: 'ezescobedo27@gmail.com', name: '' }]);
  assert.ok(m.text.includes(`${base}?codigo=CLB-7K3M9Q`));
});

test('sin correo, con correo malo o con código malo no se manda nada', async () => {
  const t = transporteDePrueba();
  t.sendMail = async () => assert.fail('no debía enviar');
  const args = { transporte: t, de: 'x@y.com', baseEnlace: base };
  assert.deepStrictEqual(await enviarBienvenida({ ...args, miembro: { correo: '' }, codigo: 'CLB-7K3M9Q' }), { enviado: false, motivo: 'sin-correo' });
  assert.deepStrictEqual(await enviarBienvenida({ ...args, miembro: { correo: 'no-es-correo' }, codigo: 'CLB-7K3M9Q' }), { enviado: false, motivo: 'correo-invalido' });
  assert.deepStrictEqual(await enviarBienvenida({ ...args, miembro: { correo: 'a@b.com' }, codigo: 'malo' }), { enviado: false, motivo: 'codigo-invalido' });
});
