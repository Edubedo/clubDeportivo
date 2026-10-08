# Antes de publicar: lista de pasos

Marca cada punto. Los marcados con ⚠️ son obligatorios: sin ellos la app **no funciona** o **no es segura**.

## 1. Firebase

- [ ] ⚠️ **Publica las reglas nuevas.** Consola de Firebase → Firestore → Reglas → pega el contenido de
      [`firestore.rules`](../firestore.rules) → Publicar. La app nueva usa las colecciones `registroCodigos`, `avisos` y
      `pagos`; con las reglas anteriores el registro por código y los avisos fallan.
      Antes de publicar corre `tools/pruebas-reglas` (36 pruebas; ver su README).
- [ ] ⚠️ **Entra una vez como administrador** y abre el Dashboard: ahí corre la migración a la versión 6 que crea el
      registro de códigos de los miembros que ya existían. Hasta que lo hagas, esos miembros verán "No encontramos ese
      código" al registrarse.
- [ ] ⚠️ **Borra o cambia la contraseña de las cuentas de prueba** (`admin@clubdeportivo.com`,
      `superadmin@clubdeportivo.com`, `socio@prueba.com`, etc.). Tenían contraseña `123456`: cualquiera que conozca el
      correo entra como administrador. Authentication → Usuarios.
- [ ] Borra los datos de ejemplo que ya estén en Firestore (reservas, membresía y pagos de prueba, torneos de ejemplo,
      áreas de ejemplo) para que el dashboard y los listados muestren solo datos reales. La app ya no siembra datos
      de ejemplo sola.
- [ ] Authentication → Configuración → **Política de contraseñas**: activa mayúscula, minúscula, número y carácter
      especial (la app ya lo exige, pero así también se exige fuera de la app).
- [ ] Authentication → Plantillas: pon en español y con el nombre "Athletic Club" el correo de restablecer contraseña.
- [ ] Activa **App Check** (Play Integrity) para Firestore y Authentication. Protege el código de miembro contra
      intentos de adivinarlo desde fuera de la app.
- [ ] Activa copias de seguridad programadas de Firestore y alertas de presupuesto.

## 2. Compilar y firmar

- [ ] Crea tu llave de firma (una sola vez, guárdala y respáldala: sin ella no podrás actualizar la app):
      `keytool -genkeypair -v -keystore athletic-club.jks -alias athletic -keyalg RSA -keysize 2048 -validity 10000`
- [ ] Android Studio → Build → Generate Signed App Bundle (AAB) → release. El release ya compila con minificación (R8).
- [ ] Sube el `versionCode` en `app/build.gradle.kts` en cada publicación.
- [ ] Prueba el AAB/APK firmado en un teléfono real **antes** de subirlo.

## 3. Prueba de humo (30 min, con un miembro de prueba)

1. Admin: crea un encargado (Personal, con su área) y un miembro (Membresías → Registrar miembro, cobra en efectivo).
2. Miembro: **Regístrate con tu código**. Repite con el mismo código: debe decir que ya tiene cuenta.
3. Miembro: inicia sesión, reserva un espacio, cancela la reserva (con confirmación).
4. Admin: suspende esa membresía con un motivo. El miembro ve el motivo y no puede reservar. Reactívala.
5. Admin: renueva la membresía (método de pago) y mira que el Dashboard suba los ingresos del mes.
6. Visitante: reserva y el personal la aprueba o rechaza (Reservas → Por aprobar).
7. Admin: envía un aviso a socios; el miembro lo ve en la campana.
8. Inventario: agrega un artículo, cierra y abre la app: debe seguir ahí.

## 4. Límites conocidos (decídelos antes de publicar)

- **Fotos de perfil**: se guardan solo en el teléfono de quien las sube (no hay Firebase Storage). El personal no ve las
  fotos de otros.
- **Sesión**: la app recuerda la sesión en el teléfono hasta que la persona cierra sesión.
- **Reservas y membresía**: la app impide reservar con membresía suspendida o vencida, pero la regla del servidor no
  lo comprueba; alguien que manipule la app podría saltársela. Para blindarlo habría que agregar Cloud Functions.
- **Código de miembro**: tiene 6 caracteres (≈ 887 millones de combinaciones). Es suficiente para un club con App Check
  activo; sin App Check, un atacante podría intentar adivinar códigos.
- **Avisos**: son avisos dentro de la app (campana); no hay notificaciones push.
