import { initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { readFileSync } from 'node:fs';
import { doc, setDoc } from 'firebase/firestore';

const projectId = process.env.FIREBASE_PROJECT_ID
  ?? JSON.parse(readFileSync(new URL('../../app/google-services.json', import.meta.url), 'utf8')).project_info.project_id;
const authUrl = 'http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1';
const demoAccounts = [
  { email: 'richie.demo@club.test', password: 'RichieDemo2026!', name: 'Richie de prueba', role: 'SOCIO' },
  { email: 'admin.demo@club.test', password: 'AdminDemo2026!', name: 'Admin de prueba', role: 'ADMIN' }
];

async function ensureDemoUser({ email, password }) {
  const create = await fetch(`${authUrl}/accounts:signUp?key=emulator-key`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password, returnSecureToken: true })
  });
  if (create.ok) return (await create.json()).localId;

  const createError = await create.json();
  if (createError.error?.message !== 'EMAIL_EXISTS') {
    throw new Error(`No se pudo crear ${email} en el emulador: ${createError.error?.message ?? create.status}`);
  }

  const signIn = await fetch(`${authUrl}/accounts:signInWithPassword?key=emulator-key`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password, returnSecureToken: true })
  });
  if (!signIn.ok) throw new Error(`No se pudo iniciar sesión para ${email} en el emulador.`);
  return (await signIn.json()).localId;
}

function fechaEnMexico(date) {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'America/Mexico_City',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).formatToParts(date);
  const values = Object.fromEntries(parts.map(({ type, value }) => [type, value]));
  return `${values.year}-${values.month}-${values.day}`;
}

const today = new Date(`${fechaEnMexico(new Date())}T12:00:00-06:00`);
const yesterday = new Date(today);
yesterday.setDate(yesterday.getDate() - 1);
const expiredOn = fechaEnMexico(yesterday);
const startDate = new Date(today);
startDate.setDate(startDate.getDate() - 30);
const startedOn = fechaEnMexico(startDate);

const [memberUid, adminUid] = await Promise.all(demoAccounts.map(ensureDemoUser));
const env = await initializeTestEnvironment({
  projectId,
  firestore: {
    rules: readFileSync(new URL('../../firestore.rules', import.meta.url), 'utf8'),
    host: '127.0.0.1',
    port: 8080
  }
});

try {
  await env.withSecurityRulesDisabled(async context => {
    const db = context.firestore();
    await Promise.all([
      setDoc(doc(db, `usuarios/${memberUid}`), {
        nombre: 'Richie de prueba',
        email: demoAccounts[0].email,
        rol: 'SOCIO',
        estado: 'ACTIVO',
        codigoMiembro: 'CLB-DEMO01'
      }),
      setDoc(doc(db, `usuarios/${adminUid}`), {
        nombre: 'Admin de prueba',
        email: demoAccounts[1].email,
        rol: 'ADMIN',
        estado: 'ACTIVO'
      }),
      setDoc(doc(db, 'miembros/CLB-DEMO01'), {
        nombre: 'Richie de prueba',
        usuarioId: memberUid,
        membresiaId: 'demo-richie-membership',
        parentesco: 'Titular',
        cuentaCreada: true
      }),
      setDoc(doc(db, 'membresias/demo-richie-membership'), {
        usuarioId: memberUid,
        titularNombre: 'Richie de prueba',
        tipo: 'INDIVIDUAL',
        plan: 'NORMAL',
        precio: 1800,
        estado: 'ACTIVA',
        fechaInicio: startedOn,
        fechaVencimiento: expiredOn,
        renovaciones: 0
      }),
      setDoc(doc(db, 'solicitudes_renovacion/demo-richie-renewal'), {
        id: 'demo-richie-renewal',
        socioId: memberUid,
        socioNombre: 'Richie de prueba',
        membresiaId: 'demo-richie-membership',
        planId: 'INDIVIDUAL_NORMAL',
        monto: 1800,
        concepto: 'REN-DEMO',
        comprobanteUrl: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/lckAAAAASUVORK5CYII=',
        status: 'PENDIENTE',
        createdAt: Date.now()
      })
    ]);
  });
} finally {
  await env.cleanup();
}

console.log('Caso de prueba creado solo en los emuladores locales.');
console.log(`Socio: ${demoAccounts[0].email} / ${demoAccounts[0].password}`);
console.log(`Admin: ${demoAccounts[1].email} / ${demoAccounts[1].password}`);
console.log(`Membresía vencida el ${expiredOn}; solicitud de renovación pendiente.`);
