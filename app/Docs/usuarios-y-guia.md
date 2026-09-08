# ClubDeportivo — Usuarios de prueba y guía de trabajo

> Ver también: [`README.md`](README.md) (índice), [`arquitectura.md`](arquitectura.md) y
> [`modelo-de-datos.md`](modelo-de-datos.md).

## ¿Qué es esta app?

**ClubDeportivo** es una app Android (Kotlin, arquitectura MVVM con `ViewModel` + `LiveData`) para
gestionar un club deportivo. Permite a los usuarios:

- Iniciar sesión y ver un inicio con accesos según su rol.
- Ver las **áreas** del club y **reservar** turnos en ellas.
- Consultar **"Mis reservas"**.
- Ver planes de **membresía** (individuales y paquetes familiares) y sus precios.
- Ver **torneos**.
- Administrar su **perfil**.

La app maneja distintos **roles** (`Rol.kt`), de mayor a menor alcance:

| Rol | Alcance |
|---|---|
| `SUPERADMIN` | Nivel sistema, controla todo. |
| `ADMIN` | Nivel empresa, controla todas las áreas. |
| `ADMIN_AREA` | Solo administra su área. |
| `AYUDANTE_AREA` | Ayuda dentro de un área. |
| `SOCIO` | Elige área y reserva turnos. |
| `VISITANTE_EXTERNO` | Acceso limitado, requiere aprobación para reservar. |

Actualmente **no hay backend conectado**: el login usa `FakeAuthRepository`
(`app/src/main/java/com/example/clubdeportivo/data/repository/AuthRepository.kt`), una
implementación de prueba que asigna un rol según el correo ingresado, pensada para poder
probar las pantallas de cada rol mientras el servidor real no existe. Cuando el backend esté
listo, `login()` se reemplaza por una llamada real vía `RetrofitClient.apiService.login(...)`.

## Usuarios de prueba (demo)

El login **no valida una contraseña real contra una base de datos**: cualquier contraseña de
**al menos 6 caracteres** es aceptada. Lo que determina el rol es el **correo** que se usa para
entrar:

| Correo | Rol asignado | ID interno |
|---|---|---|
| `superadmin@clubdeportivo.com` | Superadministrador | 1 |
| `admin@clubdeportivo.com` | Administrador | 2 |
| `areadmin@clubdeportivo.com` | Administrador de área | 3 |
| `ayudante@clubdeportivo.com` | Ayudante de área | 4 |
| `externo@clubdeportivo.com` | Visitante externo | 6 |
|  `socio@clubdeportivo.com` | Socio | 5 |

Ejemplo para entrar como Socio: `socio@prueba.com` / `123456`.

Estos usuarios están definidos en:
`app/src/main/java/com/example/clubdeportivo/data/repository/AuthRepository.kt`

## Cómo trabajar en este proyecto

### Requisitos
- Android Studio (versión reciente, compatible con Kotlin DSL de Gradle).
- JDK indicado por `gradle/gradle-daemon-jvm.properties`.

### Poner el proyecto en marcha
1. Cloná el repo y abrilo con Android Studio (`File > Open`, seleccioná la carpeta raíz).
2. Dejá que Gradle sincronice (`./gradlew` ya trae el wrapper, no hace falta instalar Gradle aparte).
3. Corré la app en un emulador o dispositivo con el botón ▶️, o desde terminal:
   ```
   ./gradlew installDebug
   ```
4. En la pantalla de login, usá cualquiera de los correos de la tabla de arriba con una
   contraseña de 6+ caracteres para probar cada rol.

### Estructura del código
- `data/model/` — modelos de datos (`Usuario`, `Reserva`, `Membresia`, `Torneo`, `Pago`, etc).
- `data/repository/` — repositorios (hoy son implementaciones "fake" en memoria, listas para
  reemplazar por llamadas reales vía `data/remote/ApiService.kt` + Retrofit).
- `data/Catalogos.kt` — precios y reglas de negocio centralizados (planes, paquetes familiares,
  reglas de reservas e inasistencias) para no repetir números "mágicos" en el código.
- `ui/<pantalla>/` — cada pantalla tiene su `Fragment` + `ViewModel` (patrón MVVM), navegadas
  desde `res/navigation/nav_graph.xml`.
- `util/` — helpers (`Fechas`, `Resultado` para éxito/error).

### Cuando llegue el backend real
Reemplazar las implementaciones "Fake" en `data/repository/` por llamadas a
`RetrofitClient.apiService` (definido en `data/remote/ApiService.kt` y
`data/remote/RetrofitClient.kt`), manteniendo las mismas interfaces de repositorio para no tocar
los `ViewModel`.
