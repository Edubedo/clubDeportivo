# Pruebas de las reglas de seguridad

Comprueban `firestore.rules` contra el emulador de Firestore (no tocan tu proyecto real). Cubren sobre todo el
registro de miembros con código, el acceso por rol, los avisos, las publicaciones y los precios.

Requisitos: Node 18+ y JDK 21+ (el emulador de Firestore requiere Java 21 o superior).

```
cd tools/pruebas-reglas
npm install
npm test
```

Corre estas pruebas **cada vez que cambies `firestore.rules`** y antes de publicarlas en la consola de Firebase.

## Probar visualmente una renovación

Este caso crea un socio y un admin ficticios en los emuladores locales; no modifica cuentas ni datos de producción.
Necesita tener `app/google-services.json` configurado para que la app inicie Firebase.

1. En una terminal, desde `tools/pruebas-reglas`, ejecuta `npx firebase emulators:start --only auth,firestore --project <project-id-de-app>` usando el `project_id` de `app/google-services.json`.
2. En otra terminal, desde la raíz del proyecto, ejecuta `.\gradlew.bat installDebug -PfirebaseEmulador=10.0.2.2`.
3. En una tercera terminal, ejecuta `node tools/pruebas-reglas/seed-renovacion-demo.mjs`.
4. Inicia sesión en la app con la cuenta de socio que informa el script. La membresía aparece vencida y la solicitud pendiente.
   Para probar la aprobación, cierra sesión, entra con la cuenta admin, aprueba la solicitud y vuelve a entrar como socio.

La semilla vuelve a dejar la solicitud pendiente al ejecutarse otra vez. Las dos cuentas y contraseñas solo existen en el
emulador local; no las uses con Firebase real.
