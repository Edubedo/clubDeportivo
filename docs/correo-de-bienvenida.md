# Correo de bienvenida a miembros nuevos

Cuando el personal registra a un miembro (*Membresías → Registrar miembro*) o un titular agrega a un familiar, la persona
recibe un correo con su **código de acceso** (`CLB-XXXXXX`) y un botón **Crear mi cuenta** que abre la app con el código ya escrito.

## Cómo funciona

1. El alta crea el documento `miembros/{codigo}` con el correo de la persona.
2. La Cloud Function `enviarBienvenidaMiembro` (`functions/index.js`) se dispara y manda el correo por **SMTP**.
   Queda `bienvenidaEnviadaEn` en el documento (no se manda dos veces). Si falla queda `bienvenidaError`
   (`smtp:…`, `correo-invalido`); si la persona no tiene correo no pasa nada: el personal le da el código en persona.
3. El botón del correo apunta a `https://<proyecto>.web.app/unirse?codigo=CLB-XXXXXX` (página de Firebase Hosting en
   `hosting/unirse/`). Esa página muestra el código y abre `clubdeportivo://registro?codigo=…`, el enlace que la app ya sabe recibir.
   Los correos no permiten enlaces `clubdeportivo://` directos, por eso el paso intermedio por https.

## Servicio de correo gratuito (elige uno)

| Servicio | Gratis | Qué necesitas |
|---|---|---|
| **Gmail** (por defecto) | ~500 correos/día | Cuenta de Google con verificación en 2 pasos y una [contraseña de aplicación](https://myaccount.google.com/apppasswords). `SMTP_HOST=smtp.gmail.com`, puerto `465`. |
| **Resend** | 3,000 correos/mes (100/día) | Cuenta gratis y una API key (`re_…`). `SMTP_HOST=smtp.resend.com`, puerto `465`, `SMTP_USER=resend`, `SMTP_PASS=<la API key>`, `SMTP_REMITENTE=Athletic Club <onboarding@resend.dev>`. **Sin verificar un dominio propio solo entrega al correo con el que te registraste en Resend**; para escribirle a cualquier miembro hay que verificar un dominio en resend.com/domains. |
| **Brevo** | 300 correos/día | Cuenta gratis, verificar un correo remitente, y crear una clave SMTP. `SMTP_HOST=smtp-relay.brevo.com`, puerto `587`. |

Con Gmail el remitente siempre es esa cuenta; con Brevo o Resend (dominio verificado) puede ser el correo del club.

## 1. Probar tus credenciales (sin desplegar nada)

```
cd functions && npm install
SMTP_USER=tucorreo@gmail.com SMTP_PASS='contraseña-de-aplicación' node probar-correo.js ezescobedo27@gmail.com
```

Manda un correo de ejemplo con el código `CLB-7K3M9Q`. Si llega, las credenciales sirven.

## 2. Publicar

Requiere el plan **Blaze** (igual que `cambiarContrasenaPersonal`; este volumen no genera costo) y el proyecto `clubdeportivoudc`.

```
firebase login
firebase functions:secrets:set SMTP_PASS      # pega la contraseña de aplicación / clave SMTP
cd functions && npm install && cd ..
firebase deploy --only functions,hosting
```

Al desplegar, Firebase pregunta los parámetros (`SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_REMITENTE`, `ENLACE_BASE`) y los guarda en
`functions/.env.clubdeportivoudc` (no se sube al repositorio). `ENLACE_BASE` déjalo vacío: usa `https://clubdeportivoudc.web.app/unirse`.

## Descargar la app desde el enlace

La página `/unirse` tiene el botón **Descargar la app (Android)**, apunta al APK de GitHub Releases (`URL_APK`)
(`hosting/unirse/index.html`). Firebase Hosting gratis **no admite `.apk`**: súbelo a GitHub Releases, Drive o Google Play y pega su enlace.

- **App ya instalada:** el enlace https del correo abre la app directo con el código escrito (App Link: filtro con `autoVerify` en
  `AndroidManifest.xml` + `hosting/.well-known/assetlinks.json`). Si Android no lo verifica, la página tiene el botón *Abrir la app*.
- **App sin instalar:** descarga el APK, lo instala y **vuelve a tocar el enlace del correo**; entonces se abre en su registro con el código.
  Sin Google Play no existe el "enlace diferido" que recuerde el código después de instalar, por eso se pide volver a tocar el enlace.
- Si cambias de llave de firma (release, Google Play), agrega su huella SHA-256 a `assetlinks.json`
  (`keytool -list -v -keystore tu.jks`); hoy solo trae la de la llave de depuración de esta computadora.
- Para publicar en Play Store, cambia el botón por el enlace de la ficha (`https://play.google.com/store/apps/details?id=com.example.clubdeportivo`).

## Pruebas

- Unitarias del correo: `cd functions && npm run test:bienvenida`.
- De extremo a extremo se hicieron con los emuladores de Firestore/Functions y un SMTP local: el alta de un miembro con correo mandó el
  correo con el código y el enlace; sin correo no mandó nada; con un correo mal escrito dejó `bienvenidaError: correo-invalido`.

## Límites

- No hay botón de **reenviar**: si el correo falló (queda `bienvenidaError`), el personal le da el código en persona. Reenviar con un botón
  en la app sería una segunda función; no está hecha.
- El enlace abre la app solo en Android con la app instalada; en otro caso la página muestra el código para escribirlo a mano.
