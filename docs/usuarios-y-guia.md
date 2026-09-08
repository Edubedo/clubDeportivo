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

La app está conectada a **Firebase** (Authentication + Firestore) — ver
[`arquitectura.md`](arquitectura.md#conectado-a-firebase) para el detalle. El login es real: hace
falta una cuenta de verdad, no cualquier correo/contraseña como en una versión anterior de este
proyecto.

## Usuarios de prueba

Ya existen 6 cuentas reales, una por rol (creadas el 2026-09-08 desde la propia pantalla
"Registrate", igual que haría cualquiera). Todas usan la contraseña **`123456`**:

| Correo | Rol |
|---|---|
| `superadmin@clubdeportivo.com` | Superadministrador |
| `admin@clubdeportivo.com` | Administrador |
| `areadmin@clubdeportivo.com` | Administrador de área |
| `ayudante@clubdeportivo.com` | Ayudante de área |
| `socio@prueba.com` | Socio |
| `externo@clubdeportivo.com` | Visitante externo |

La cuenta **Socio** (`socio@prueba.com`) es la más completa para probar: tiene una membresía
familiar activa (con 2 integrantes), 3 reservas en distintos estados (confirmada, pendiente de
aprobación y finalizada con check-in), y una inscripción a un torneo — ver
[`arquitectura.md`](arquitectura.md#datos-de-ejemplo-ya-cargados) para el detalle completo de qué
se cargó y cómo.

Si necesitás una cuenta nueva (para otro nombre, u otro rol de prueba), se crea igual: pantalla de
login → **"¿No tenés cuenta? Registrate"** → nombre, correo, contraseña de 6+ caracteres, y el rol
de la lista. Queda guardada de verdad en Firebase (Authentication tiene el correo/contraseña;
Firestore, en la colección `usuarios`, tiene el nombre y el rol elegido) — pero arranca sin
reservas ni membresía, esas hay que crearlas usando la app o a mano en la consola.

## Cómo trabajar en este proyecto

### Requisitos
- Android Studio (versión reciente, compatible con Kotlin DSL de Gradle).
- JDK indicado por `gradle/gradle-daemon-jvm.properties`.

### Poner el proyecto en marcha
1. Cloná el repo y abrilo con Android Studio (`File > Open`, seleccioná la carpeta raíz).
2. Conseguí el archivo `app/google-services.json` de tu proyecto de Firebase (no está en el
   repo — cada quien usa el suyo) y ponelo en esa ruta exacta.
3. Dejá que Gradle sincronice (`./gradlew` ya trae el wrapper, no hace falta instalar Gradle aparte).
4. Corré la app en un emulador o dispositivo con el botón ▶️, o desde terminal:
   ```
   ./gradlew installDebug
   ```
5. En la pantalla de login, tocá "Registrate" y creá una cuenta (ver "Usuarios de prueba" arriba).

### Estructura del código
- `data/model/` — modelos de datos (`Usuario`, `Reserva`, `Membresia`, `Torneo`, `Pago`, etc).
- `data/repository/` — interfaces de repositorio + implementaciones Firebase (`Firebase...Repository`).
- `data/Catalogos.kt` — precios y reglas de negocio centralizados (planes, paquetes familiares,
  reglas de reservas e inasistencias) para no repetir números "mágicos" en el código.
- `ui/<pantalla>/` — cada pantalla tiene su `Screen` (la UI, en Jetpack Compose) + `ViewModel`
  (patrón MVVM), navegadas desde `ui/ClubDeportivoApp.kt` + `ui/navigation/Destinations.kt`.
- `util/` — helpers (`Fechas`, `Resultado` para éxito/error).

Ver [`arquitectura.md`](arquitectura.md) para una explicación más completa, pensada para alguien
sin experiencia previa en Android/Kotlin.
