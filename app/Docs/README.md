# Documentación — ClubDeportivo

Índice de la documentación del proyecto. Empezá por acá.

## ¿Qué es esta app?

**ClubDeportivo** es una app Android nativa (Kotlin, MVVM) para gestionar un club deportivo:
reservar turnos en áreas, ver membresías y torneos, y administrar el perfil de cada usuario según
su rol dentro del club.

Hoy el proyecto es **frontend-only**: no hay backend real conectado todavía. Todos los datos
(usuarios, reservas, membresías, torneos) viven en repositorios "Fake" en memoria, pensados para
reemplazarse por llamadas HTTP reales cuando el backend (Node.js + Express) esté listo.

## Documentos

| Documento | Contenido |
|---|---|
| [`arquitectura.md`](arquitectura.md) | Stack técnico, estructura de carpetas, patrón MVVM, capa de datos y navegación. |
| [`modelo-de-datos.md`](modelo-de-datos.md) | Todas las entidades del dominio (usuario, reserva, membresía, torneo, etc.) y las reglas de negocio centralizadas en `Catalogos.kt`. |
| [`usuarios-y-guia.md`](usuarios-y-guia.md) | Los roles y usuarios de prueba para probar cada pantalla, y la guía paso a paso para poner el proyecto a andar. |

## Inicio rápido

1. Abrí la carpeta raíz del repo con Android Studio.
2. Dejá que Gradle sincronice (el wrapper ya está incluido, no hace falta instalar Gradle aparte).
3. Corré la app (▶️ en Android Studio, o `./gradlew installDebug` desde terminal).
4. En el login, usá uno de los correos de prueba listados en
   [`usuarios-y-guia.md`](usuarios-y-guia.md) con cualquier contraseña de 6+ caracteres.

Para el detalle completo (requisitos, estructura del código, qué hacer cuando llegue el backend
real), ver [`usuarios-y-guia.md`](usuarios-y-guia.md) y [`arquitectura.md`](arquitectura.md).
