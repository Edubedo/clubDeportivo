# Athletic Club — Roles, acceso y guía de trabajo

> Ver también: [`README.md`](README.md) (índice), [`arquitectura.md`](arquitectura.md),
> [`modelo-de-datos.md`](modelo-de-datos.md) y [`produccion.md`](produccion.md) (pasos antes de publicar).

## ¿Qué es esta app?

**Athletic Club** es una app Android (Kotlin, Jetpack Compose, MVVM) para gestionar un club deportivo:
reservas de áreas, membresías y cobros, torneos, inventario, personal y avisos. El backend es Firebase
(Authentication + Firestore).

## Roles

| Rol | Qué ve y qué hace |
|---|---|
| `SUPERADMIN` / `ADMIN` | **Administrador.** Dashboard (ingresos y membresías), Personal, Áreas e inventario, Reservas (con torneos y aprobaciones) y Membresías (incluye cambiar precios). Es el único que crea cuentas de personal y avisa a empleados. |
| `AYUDANTE_AREA` (y `ADMIN_AREA`, heredado) | **Encargado.** Solo ve lo de su área de trabajo (obligatoria al darlo de alta): Reservas (aprueba o rechaza las solicitudes, ve la disponibilidad por hora y toma asistencia), Mi área (la puede poner en mantenimiento), Inventario de su deporte y Perfil. No ve Membresías, precios ni Personal. |
| `SOCIO` | Reserva espacios, ve sus reservas, su membresía y los avisos del club. |
| `VISITANTE_EXTERNO` | Igual que el miembro, y solo puede reservar en áreas que admiten visitantes. |

## Cómo se crea cada tipo de cuenta

- **Empleados (administrador o apoyo):** el administrador los da de alta en *Personal → Agregar personal*. La
  contraseña debe tener mínimo 8 caracteres, mayúscula, minúscula, número y un carácter especial.
- **Miembros del club (socios y visitantes):** el personal los da de alta en *Membresías → Registrar miembro*. El
  sistema genera un **código único por persona** (ej. `CLB-7K3M9Q`) y registra el cobro con su método de pago.
  - La persona abre la app → **Regístrate con tu código** → escribe el código → si es un código real que aún no se
    usó, la app le pide nombre, correo y contraseña. Si el código ya tiene cuenta, le avisa que ya tiene una cuenta
    creada y la manda a iniciar sesión.
  - Si alguien se equivocó de correo o perdió el acceso, el personal puede usar **Restablecer acceso** (en la edición
    de la membresía) para que vuelva a registrarse con el mismo código.
  - Entrar a la app siempre es con **correo y contraseña**; el código solo sirve una vez, para registrarse.

## Membresías

- Suspender una membresía **siempre pide un motivo** (falta de pago, incumplimiento del reglamento, conducta, a
  solicitud del miembro, documentación, motivo médico u otro con texto). El motivo se guarda y el miembro lo ve en su
  membresía y al intentar reservar.
- Renovar suma un mes, **cobra** (con método de pago) y registra el pago.
- Una membresía suspendida, vencida o inexistente **no puede reservar** (la app avisa por qué).

## Reservas: de la solicitud a la confirmación

1. El **miembro** reserva un espacio y la solicitud queda **"En revisión"** (ya ocupa cupo, para que nadie más tome esas horas).
2. El **encargado del área** (o un administrador) la ve en *Reservas → Por aprobar* y la **aprueba** (pasa a *Confirmada* y se aparta el material) o la **rechaza** (el miembro la ve como *Rechazada* y el lugar se libera).
3. El miembro puede cancelar su solicitud o reserva cuando quiera (con menos de 4 h de anticipación cuenta como tardía).
4. El encargado ve la ocupación hora por hora de su área en *Reservas → Horarios*. Un miembro puede tener máximo 3 reservas activas.

> `firestore.rules` obliga a que las reservas de miembros nazcan "en revisión" y a que solo el encargado de ese deporte (o un administrador) las apruebe: hay que **publicar** el archivo en Firebase tras cada cambio.

## Cambiar la contraseña del personal

*Personal → Editar → Cambiar contraseña*: el administrador escribe la contraseña nueva (con los mismos requisitos del alta) y se
cambia al instante, **sin correo**. Avísale a la persona su contraseña nueva; sus sesiones abiertas se cierran. Funciona con una
Cloud Function que hay que publicar una vez: ver [`../functions/README.md`](../functions/README.md).

## Dashboard del administrador

Ingresos del mes (contra el mes anterior), gráfica de seis meses, membresías activas,
altas del mes, vencimientos de la semana, vencidas y suspendidas, cobros por método y la operación del día (reservas,
reservas por aprobar, áreas, stock bajo, personal, torneos). **Los ingresos salen de los cobros que registra la app
desde que se estrenó esta versión**; no se inventan cobros anteriores.

## Cómo trabajar en este proyecto

### Requisitos
- Android Studio reciente. El JDK que trae Android Studio sirve (`Contents/jbr`).
- `app/google-services.json` de tu proyecto de Firebase.

### Poner el proyecto en marcha
1. Abre la carpeta raíz con Android Studio y deja que Gradle sincronice.
2. Corre la app (▶️) o `./gradlew installDebug`.
3. Pruebas unitarias: `./gradlew testDebugUnitTest`.
4. Pruebas de las reglas de seguridad: ver [`../tools/pruebas-reglas/README.md`](../tools/pruebas-reglas/README.md).

### Estructura del código
- `data/model/` — modelos (`Usuario`, `Reserva`, `Membresia`, `PagoClub`, `ArticuloInventario`...).
- `data/repository/` — interfaces y su implementación con Firebase (`Firebase...Repository`).
- `data/Catalogos.kt` — precios y reglas del club.
- `ui/theme/` — **sistema de diseño**: un solo naranja de marca, grises cálidos, blanco y tres colores de estado
  (`Color.kt`); tipografía Plus Jakarta Sans (`Type.kt`). Usa los componentes de `ui/components/` (tarjetas, botones,
  insignias, diálogos) en lugar de crear estilos nuevos.
- `ui/<pantalla>/` — cada pantalla con su `Screen` y su `ViewModel`.
- `util/` — reglas de negocio puras con pruebas (`ReglasMembresia`, `ReglasReserva`, `ReglasContrasena`, `ResumenAdmin`...).
