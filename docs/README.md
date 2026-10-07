# Documentación — ClubDeportivo

Índice de la documentación del proyecto. Empezá por acá.

## ¿Qué es esta app?

**Athletic Club** es una app Android nativa (Kotlin, Jetpack Compose, MVVM) para gestionar un
club deportivo: reservar turnos en áreas, ver membresías y torneos, y administrar el perfil de
cada usuario según su rol dentro del club.

Tiene frontend (esta app) y backend (**Firebase**: Authentication + Firestore) — ver
["¿Esta app tiene backend y frontend?"](arquitectura.md#esta-app-tiene-backend-y-frontend) en
`arquitectura.md` para la explicación completa. Todos los datos (usuarios, reservas, membresías,
torneos) viven de verdad en Firestore, no en memoria.

## Documentos

| Documento | Contenido |
|---|---|
| [`arquitectura.md`](arquitectura.md) | **Empezá acá si nunca usaste Android/Kotlin.** Qué es backend/frontend en este proyecto, stack técnico, estructura de carpetas, patrón MVVM, capa de datos y navegación, cómo está conectado Firebase. |
| [`modelo-de-datos.md`](modelo-de-datos.md) | Todas las entidades del dominio (usuario, reserva, membresía, torneo, etc.) y las reglas de negocio centralizadas en `Catalogos.kt`. |
| [`usuarios-y-guia.md`](usuarios-y-guia.md) | Los roles, cómo se crea cada tipo de cuenta (empleados y miembros con código) y la guía para poner el proyecto a andar. |
| [`produccion.md`](produccion.md) | **Lista de pasos antes de publicar:** reglas de Firebase, cuentas de prueba, firma, prueba de humo y límites conocidos. |

## Inicio rápido

1. Abrí la carpeta raíz del repo con Android Studio.
2. Conseguí `app/google-services.json` (no está en el repo — ver
   [`arquitectura.md`](arquitectura.md#repo-público-y-google-servicesjson)) y ponelo en esa ruta
   exacta.
3. Dejá que Gradle sincronice (el wrapper ya está incluido, no hace falta instalar Gradle aparte).
4. Corré la app (▶️ en Android Studio, o `./gradlew installDebug` desde terminal).
5. Para entrar necesitas una cuenta: los empleados las crea un administrador y los miembros se registran con su código (ver [`usuarios-y-guia.md`](usuarios-y-guia.md)).

Para el detalle completo (requisitos, estructura del código, cómo está armado Firebase), ver
[`usuarios-y-guia.md`](usuarios-y-guia.md) y [`arquitectura.md`](arquitectura.md).
