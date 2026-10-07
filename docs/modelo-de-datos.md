# Modelo de datos

> **Diccionario de datos completo de Firestore: [`diccionario-de-datos.txt`](diccionario-de-datos.txt)**
> (campos, tipos, relaciones, valores permitidos, índices y reglas de cada colección). Este archivo solo
> describe las clases de Kotlin; si algo difiere, manda el diccionario.


Todas las entidades viven como `data class` / `enum class` en
`app/src/main/java/com/example/clubdeportivo/data/model/`. Son los objetos que circulan entre
los repositorios (ver [`arquitectura.md`](arquitectura.md#conectado-a-firebase)) y la UI, con la
misma forma que tienen los documentos guardados en Firestore. El `id` de cada entidad es un
`String` (así son los IDs de documento en Firestore) — la excepción es `PaqueteFamiliar`, que no
vive en la base de datos: son 3 opciones fijas definidas en `Catalogos.kt`.

## Usuario y roles (`Usuario.kt`)

```kotlin
data class Usuario(
    val id: String, // el uid que genera Firebase Authentication al crear la cuenta
    val nombre: String,
    val correo: String,
    val rol: Rol,
    val fotoUrl: String? = null
)

enum class Rol { SUPERADMIN, ADMIN, ADMIN_AREA, AYUDANTE_AREA, SOCIO, VISITANTE_EXTERNO }

// El personal (administradores y encargados de área) es un usuario más: sus datos laborales
// (tipoPersonal, turno, areaTrabajo) viven en usuarios/{uid}; ya no hay colección empleados.
```

Ver el detalle de cada rol y cómo crear una cuenta de prueba para cada uno en
[`usuarios-y-guia.md`](usuarios-y-guia.md).

## Áreas e instalaciones (`Instalaciones.kt`)

```kotlin
enum class DisponibilidadArea { DISPONIBLE, OCUPADA, MANTENIMIENTO }

data class Area(
    val id: String, val nombre: String, val tipo: String, val capacidad: Int,
    val disponibilidad: DisponibilidadArea, val permiteExternos: Boolean = false,
    val emoji: String = "" // solo para deportes dados de alta desde la app; vacío = el de Deportes.kt
)

data class Herramienta(val id: String, val nombre: String, val areaId: String, val cantidadTotal: Int, val cantidadDisponible: Int)

data class RestriccionHorario(val id: String, val areaId: String, val diaSemana: String, val horaInicio: String, val horaFin: String)
```

## Miembros, códigos y precios (`Miembro.kt`, `Precios.kt`)

Cada persona del club (titular o integrante de un paquete familiar) tiene un **código único** del tipo
`CLB-7K3M9Q`. El código **no es una contraseña**: sirve una sola vez, para que la persona cree su cuenta (correo y
contraseña propios) con *Regístrate con tu código*. Se dan de alta desde Membresías → Registrar miembro.

- Colección `miembros/{codigo}`: el código es el id del documento, así que no puede repetirse. Guarda
  nombre, teléfono, correo, `membresiaId`, parentesco y `usuarioId`.
- Cada código tiene su cuenta en Firebase Authentication (correo interno `clb-xxxxxx@miembros.clubdeportivo.app`,
  contraseña = el propio código) y su perfil en `usuarios/{uid}`, con rol `SOCIO` (o `VISITANTE_EXTERNO` para visitas).
- Solo entra quien tiene una membresía `ACTIVA` y dentro de su vigencia (`ReglasMembresia.estadoEfectivo`). Suspender
  o dejar vencer una membresía bloquea los códigos de todas sus personas; quitar a un integrante borra su documento.
- Las membresías de un mes vencen a los 30 días aprox. (un mes calendario); renovar suma un mes desde el vencimiento
  (si sigue vigente) o desde hoy (si ya venció). La visita vence el mismo día.
- Colección `precios/{clave}`: precios que el administrador editó (claves `INDIVIDUAL_NORMAL`, `FAMILIAR_1`, `VISITA`...).
  Lo que no está ahí usa el precio base de `Catalogos`. Cada membresía guarda el precio con que se registró o renovó.
- Las membresías creadas antes de los códigos se muestran "sin código (registro anterior)": se pueden renovar,
  suspender y cambiar de plan, pero sus personas no se editan.

**Importante (seguridad):** el código funciona como contraseña, así que conviene que los códigos se entreguen en persona
y que las reglas de Firestore restrinjan la escritura de `miembros`, `membresias` y `precios` al personal del club.

## Reservas (`Reserva.kt`)

```kotlin
enum class EstadoReserva { CONFIRMADA, PENDIENTE_APROBACION, CANCELADA, FINALIZADA }

data class Reserva(
    val id: String, val usuarioId: String, val areaId: String, val fecha: String,
    val horaInicio: String, val horaFin: String, val estado: EstadoReserva, val esExterno: Boolean = false,
    val personas: Int = 1 // lugares del cupo del área que ocupa la reserva
)

data class MaterialAsignado(val id: String, val reservaId: String, val herramientaId: String, val cantidad: Int)
data class Checkin(val id: String, val reservaId: String, val horaLlegada: String)
```

Las reservas de `VISITANTE_EXTERNO` nacen en `PENDIENTE_APROBACION` (`esExterno = true`) en vez de
`CONFIRMADA` directamente.

**Control de cupo y choques** (todo en `util/ReglasReserva.kt`, con pruebas en `ReglasReservaTest`):

- Cada reserva vigente (`CONFIRMADA` o `PENDIENTE_APROBACION`) ocupa `personas` lugares de la
  `capacidad` del área en cada hora que abarca. Cuando una hora alcanza la capacidad queda
  **llena** y ya no se puede reservar; además no se puede reservar para más personas que los
  lugares que quedan libres en todas las horas elegidas.
- Una reserva dura de 1 a 4 horas seguidas (`Catalogos.MAX_HORAS_POR_RESERVA`).
- Un torneo ocupa el área completa en su horario (`horaInicio`–`horaFin`, cada día entre
  `fechaInicio` y `fechaFin`; sin horas = día completo): en esas horas no entra ninguna reserva.
- A la inversa, un torneo no se puede crear ni editar encima de reservas vigentes ni de otro
  torneo en la misma área y horario.
- Las áreas en `MANTENIMIENTO` no se pueden reservar. Los roles de personal (superadmin, admin,
  admin de área) no están sujetos al límite de reservas activas ni al bloqueo por inasistencias.
- Las validaciones se hacen con datos recién leídos de Firestore justo antes de guardar. Como
  Firestore no permite transacciones sobre consultas, dos personas que confirmen exactamente al
  mismo tiempo la última plaza podrían pasar ambas; el riesgo es bajo a la escala del club.

## Membresías (`Membresia.kt`)

```kotlin
enum class TipoMembresia { INDIVIDUAL, FAMILIAR, VISITA }
enum class EstadoMembresia { ACTIVA, VENCIDA, SUSPENDIDA }
enum class PlanIndividual { NINO, NORMAL, DELUXE }

/** No vive en Firestore: son 3 opciones fijas definidas en Catalogos.kt. */
data class PaqueteFamiliar(val id: Int, val nombre: String, val descripcion: String, val maxIntegrantes: Int, val precioMensual: Double)

data class Membresia(
    val id: String, val usuarioId: String, val tipo: TipoMembresia, val plan: PlanIndividual? = null,
    val paqueteFamiliarId: Int? = null, val precio: Double, val estado: EstadoMembresia,
    val fechaInicio: String, val fechaVencimiento: String
)

data class IntegranteFamiliar(val id: String, val membresiaId: String, val nombre: String, val parentesco: String)
```

## Torneos (`Torneo.kt`)

```kotlin
data class Torneo(
    val id: String, val nombre: String, val disciplina: String,
    val areaId: String, val fechaInicio: String, val fechaFin: String,
    val cupoMaximo: Int, val inscritos: Int // "inscritos" no se guarda: se cuenta en el momento
)

data class InscripcionTorneo(val id: String, val torneoId: String, val usuarioId: String, val fechaInscripcion: String)
```

Mientras dura un torneo, el área que ocupa (`areaId`) queda bloqueada para reservas individuales
(solo en las horas del torneo; ver "Control de cupo y choques" en Reservas).

## Pagos y notificaciones (`Pago.kt`)

Estos dos todavía no los usa ninguna pantalla ni repositorio — quedan definidos para cuando se
agregue esa funcionalidad:

```kotlin
enum class EstadoPago { PAGADO, PENDIENTE, RECHAZADO }

data class Pago(val id: String, val usuarioId: String, val concepto: String, val monto: Double, val estado: EstadoPago, val fecha: String)
data class Notificacion(val id: String, val usuarioId: String, val titulo: String, val mensaje: String, val leida: Boolean, val fecha: String)
```

## Reglas de negocio y precios (`Catalogos.kt`)

Todos los números "mágicos" del negocio están centralizados en un único `object` para no
repetirlos en el código:

| Concepto | Valor |
|---|---|
| Precio visita puntual | $200 |
| Plan individual — Niños | $1500/mes |
| Plan individual — Normal | $1800/mes |
| Plan individual — Deluxe | $3500/mes |
| Paquete Familiar (2 adultos + 3 niños) | $7000/mes |
| Paquete Pareja (2 adultos) | $2999/mes |
| Paquete Niños (hermanos) | $2800/mes |
| Máx. reservas activas por socio | 3 |
| Anticipación mínima para reservar | 2 horas |
| Anticipación máxima para reservar | 7 días |
| Duración de una reserva | 1 hora |
| Cancelación sin penalización | hasta 4 horas antes |
| Inasistencias para bloqueo | 3 en 30 días → 7 días de bloqueo |
| Horario para visitantes externos aprobados | 08:00–20:00 |

El plan Deluxe además incluye prioridad en reservas, entrenamiento personalizado, agua y snacks.

Estos valores son configuración fija de la app (precios, reglas), no datos de un usuario en
particular — por eso siguen viviendo en código y no en Firestore.
