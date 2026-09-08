# ClubDeportivo

App Android (Kotlin + Jetpack Compose) para gestionar un club deportivo: reservar turnos en
áreas, ver membresías y torneos, y administrar el perfil de cada usuario según su rol.

El frontend es esta app; el backend es **Firebase** (Authentication + Firestore) — ver
["¿Esta app tiene backend y frontend?"](docs/arquitectura.md#esta-app-tiene-backend-y-frontend).

## Empezar

1. Abrí la carpeta raíz del repo con Android Studio y dejá que Gradle sincronice.
2. Conseguí `app/google-services.json` (no está en el repo, te lo pasa quien tenga acceso al
   proyecto de Firebase — ver [`docs/arquitectura.md`](docs/arquitectura.md#repo-público-y-google-servicesjson))
   y ponelo en esa ruta exacta.
3. Corré la app (▶️ en Android Studio, o `./gradlew installDebug` desde terminal).
4. En el login, tocá "Registrate" y creá una cuenta (ver
   [`docs/usuarios-y-guia.md`](docs/usuarios-y-guia.md)).

## Documentación

Toda la documentación del proyecto vive en [`docs/`](docs/README.md):

| Documento | Contenido |
|---|---|
| [`docs/arquitectura.md`](docs/arquitectura.md) | **Empezá acá si nunca usaste Android/Kotlin.** Cómo está organizado el código y por qué. |
| [`docs/modelo-de-datos.md`](docs/modelo-de-datos.md) | Todas las entidades del dominio y las reglas de negocio del club. |
| [`docs/usuarios-y-guia.md`](docs/usuarios-y-guia.md) | Roles, usuarios de prueba, y guía paso a paso para poner el proyecto a andar. |
