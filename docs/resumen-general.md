# ClubDeportivo: resumen general

> Resumen de toda la aplicación (problema que resuelve, arquitectura, base de datos, flujos, roles y
> limitaciones). Está sacado del código actual; cuando otros documentos de `docs/` difieren, manda el código.
> Detalle complementario: [`arquitectura.md`](arquitectura.md), [`modelo-de-datos.md`](modelo-de-datos.md),
> [`diccionario-de-datos.txt`](diccionario-de-datos.txt), [`usuarios-y-guia.md`](usuarios-y-guia.md).

## Qué problema resuelve

App Android para administrar un club deportivo (fútbol, básquetbol, tenis, natación y otros deportes que se
pueden dar de alta). Reemplaza el control manual de cuatro cosas:

- **Reservas de canchas y áreas**, con control de cupo por hora y sin choques con torneos.
- **Membresías y miembros**: planes, precios, vigencia y códigos de acceso.
- **Personal del club**: altas, roles, turnos y área de trabajo.
- **Torneos e inventario** de material deportivo.

Tiene dos lados: los socios y visitantes reservan y consultan su membresía; el personal administra todo lo demás.

## Arquitectura

- **Lenguaje y UI:** Kotlin con Jetpack Compose y Material 3. No quedan pantallas en XML.
- **Patrón:** MVVM. Cada pantalla tiene un `Screen` (el dibujo) y un `ViewModel` con `LiveData`.
- **Navegación:** una sola `Activity` y un `NavHost` en `ui/ClubDeportivoApp.kt`. Las rutas están en
  `ui/navigation/Destinations.kt`.
- **Backend:** Firebase como BaaS (sin servidor propio): Authentication (correo y contraseña) y Cloud Firestore.
- **Sin inyección de dependencias:** el objeto `AppContainer` crea los repositorios una sola vez y los comparten
  todas las pantallas.
- **Configuración:** `minSdk` 24, `targetSdk` 37.

```
ui/  (Screen + ViewModel)  →  data/repository/ (interfaz + Firebase*Repository)  →  Firestore / Auth
                                      ↑
                       data/model + util/Reglas* (lógica de negocio pura, con tests)
```

### Piezas clave

- **Repositorios:** una interfaz por tema y una implementación `Firebase*` que mapea los documentos a mano (sin
  `toObject()`). Devuelven `Resultado<T>` (éxito o error).
- **Reglas de negocio:** viven en `data/Catalogos.kt` (precios y límites), `util/ReglasReserva.kt` y
  `util/ReglasMembresia.kt`. No dependen de Firebase, por eso tienen tests unitarios.
- **Sesión:** `SesionManager` guarda en memoria el usuario logueado. Cerrar la app lo pierde y siempre vuelve al login.
- **Migración:** `MigracionEsquema.kt` actualiza los documentos de Firestore al esquema actual (versión 5). La corre
  un administrador al entrar al dashboard. Solo agrega campos y es idempotente.
- **Seguridad:** `firestore.rules` se pega a mano en la consola de Firebase. Las reglas usan el rol guardado en
  `usuarios/{uid}`.

## Tipos de usuario

| Rol | Qué ve y qué puede hacer |
|---|---|
| **SUPERADMIN / ADMIN** | Dashboard, Personal, Áreas (con Inventario), Reservas (con Torneos) y Membresías. Son los únicos que crean cuentas de personal. |
| **AYUDANTE_AREA** (y ADMIN_AREA heredado) | Se muestra como "Empleado de apoyo". Ve Reservas (de su área, con aprobaciones), Áreas, Membresías y Perfil; sin dashboard, Personal ni cambio de precios. |
| **SOCIO** | Reservas (solo las suyas), su Membresía y su Perfil. |
| **VISITANTE_EXTERNO** | Igual que el socio, pero sus reservas nacen `PENDIENTE_APROBACION`. Solo puede reservar en áreas que permiten externos. |

El menú inferior cambia según el rol, y `RutaProtegida` evita abrir pantallas sin permiso.

## Base de datos (Firestore)

NoSQL, orientada a documentos. Cada documento copia el nombre de lo que referencia para no hacer lecturas extra.

| Grupo | Colecciones |
|---|---|
| Personas | `usuarios/{uid}` (también guarda al personal con `tipoPersonal`, `turno` y `areaTrabajo`), `miembros/{CLB-XXXXXX}` |
| Membresías y dinero | `membresias`, `pagos`, `precios`, `integrantesFamiliares` (obsoleta) |
| Instalaciones | `areas`, `herramientas`, `restriccionesHorario` |
| Reservas | `reservas`, `materialAsignado`, `checkins`, `notificaciones` |
| Torneos | `torneos`, `inscripcionesTorneo/{torneoId}_{usuarioId}` |
| Sistema | `config/esquema` |

Decisiones de diseño que importan:

- Los ids de documento son `String`.
- Las fechas y horas de negocio se guardan como texto (`yyyy-MM-dd`, `HH:mm`).
- Los paquetes familiares son 3 opciones fijas en `Catalogos.kt` y no están en la base.
- El horario por deporte también sale del código.
- `torneos.inscritos` es un contador que se actualiza en transacción y nunca pasa de `cupoMaximo`.
- Se eligió Firestore pese a que el modelo es muy relacional. Es una decisión ya tomada.

### Tablas (colecciones) y sus campos

Convenciones: **PK** es el id del documento y **FK** un campo que guarda el id de otro documento (Firestore no
valida las referencias; lo hace la app). **Req** indica si el campo siempre existe (S) o es opcional (N). Las fechas
de negocio son texto `yyyy-MM-dd`, las horas `HH:mm`, y los `Timestamp` los pone el servidor. El detalle completo
(índices, reglas, valores permitidos) está en [`diccionario-de-datos.txt`](diccionario-de-datos.txt).

#### `usuarios`
Perfil de cada persona con acceso (personal, socios y visitantes). **PK:** uid de Firebase Authentication.

| Campo | Tipo | Req | Descripción |
|---|---|---|---|
| `nombre` | String | S | Nombre completo |
| `email` | String | S | Correo de la cuenta (en miembros con código, un correo interno derivado del código) |
| `telefono` | String | S | Teléfono; `""` si no se capturó |
| `rol` | String | S | `SUPERADMIN` · `ADMIN` · `ADMIN_AREA` · `AYUDANTE_AREA` · `SOCIO` · `VISITANTE_EXTERNO` (si falta, se asume `SOCIO`) |
| `estado` | String | S | `ACTIVO` · `INACTIVO` (inactivo no puede iniciar sesión) |
| `fotoUrl` | String | N | Foto de perfil |
| `fechaRegistro` | String | N | Fecha de alta de la cuenta |
| `creadoEn` / `actualizadoEn` | Timestamp | N | Auditoría |
| `tipoPersonal` | String | N | Solo personal: tipo de trabajo (p. ej. "Instructor") |
| `turno` | String | N | Solo personal: `Matutino` · `Vespertino` |
| `areaTrabajo` | String | N | Solo personal: deporte donde trabaja; `""` = sin área |
| `fechaIngreso` | String | N | Solo personal: fecha en que entró |
| `creadoPor` | String | N | uid del administrador que lo dio de alta |
| `codigoMiembro` | String | N | Solo socios y visitantes con código (`CLB-XXXXXX`) |

Obsoletos: `correo` (ahora `email`), `codigo` (ahora `codigoMiembro`). La colección `empleados` ya no existe: sus
datos se fusionaron aquí en la migración a la versión 4.

#### `miembros`
Cada persona cubierta por una membresía (titular e integrantes). **PK:** el código (`CLB-` + 6 caracteres), así no
puede repetirse. **FK:** `membresiaId` → `membresias`, `usuarioId` → `usuarios`.

| Campo | Tipo | Req | Descripción |
|---|---|---|---|
| `nombre` | String | S | Nombre completo |
| `telefono` | String | S | Teléfono de contacto |
| `correo` | String | S | Correo de contacto (no es el de acceso) |
| `membresiaId` | String | S | FK a `membresias` |
| `parentesco` | String | S | "Titular" u otro (Cónyuge, Hijo, Integrante…) |
| `esTitular` | Boolean | S | `true` solo para el titular |
| `usuarioId` | String | S | FK a `usuarios` (su cuenta de acceso) |
| `fechaRegistro` | String | S | Fecha de alta |
| `creadoEn` / `actualizadoEn` | Timestamp | N | Auditoría |

#### `membresias`
Contrato de una persona o familia con el club. **PK:** id automático. **FK:** `usuarioId` → `usuarios` (titular),
`titularCodigo` → `miembros`.

| Campo | Tipo | Req | Descripción |
|---|---|---|---|
| `usuarioId` | String | S | Usuario del titular |
| `titularCodigo` | String | N | Código del titular (falta en membresías anteriores a los códigos) |
| `titularNombre` | String | S | Nombre del titular (copia) |
| `totalPersonas` | Number | S | Personas que cubre, titular incluido |
| `tipo` | String | S | `INDIVIDUAL` · `FAMILIAR` · `VISITA` |
| `plan` | String | N | `NINO` · `NORMAL` · `DELUXE` (solo si `INDIVIDUAL`) |
| `paqueteFamiliarId` | Number | N | 1 Familiar · 2 Pareja · 3 Niños (solo si `FAMILIAR`) |
| `precio` | Number | S | Precio (MXN) con el que se registró o renovó; no cambia si luego se edita el plan |
| `estado` | String | S | `ACTIVA` · `SUSPENDIDA` (`VENCIDA` se calcula al leer, no se guarda) |
| `fechaInicio` | String | S | Inicio de la vigencia |
| `fechaVencimiento` | String | S | Fin de la vigencia (un mes; la visita, el mismo día) |
| `renovaciones` | Number | S | Veces que se ha renovado |
| `ultimaRenovacion` | String | N | Fecha de la última renovación |
| `creadoPor` | String | N | uid de quien la registró |
| `creadoEn` / `actualizadoEn` | Timestamp | N | Auditoría |

#### `integrantesFamiliares` (obsoleta)
Antes guardaba a los integrantes de un paquete familiar; la reemplazó `miembros`. Ya no se escribe, solo se lee para
mostrar membresías anteriores a los códigos. **FK:** `membresiaId` → `membresias`.

| Campo | Tipo | Req | Descripción |
|---|---|---|---|
| `membresiaId` | String | S | FK a `membresias` |
| `nombre` | String | S | Nombre del integrante |
| `parentesco` | String | S | Cónyuge, Hijo… |

#### `pagos`
Dinero recibido por el club. **PK:** id automático. **FK:** `usuarioId` → `usuarios`, `membresiaId` → `membresias`.
Hoy ninguna pantalla la escribe ni la lee.

| Campo | Tipo | Req | Descripción |
|---|---|---|---|
| `usuarioId` | String | S | Quién pagó |
| `membresiaId` | String | N | Membresía pagada; ausente si es un cobro suelto |
| `concepto` | String | S | `MENSUALIDAD` · `VISITA` · `OTRO` |
| `monto` | Number | S | Importe |
| `moneda` | String | S | `MXN` |
| `metodoPago` | String | S | `EFECTIVO` · `TARJETA` · `TRANSFERENCIA` |
| `estado` | String | S | `PAGADO` · `PENDIENTE` · `RECHAZADO` |
| `fechaPago` | String | S | Fecha del pago |

#### `precios`
Precios que el administrador editó; lo que no esté aquí usa el precio base de `Catalogos.kt`. **PK:** clave del
precio (`INDIVIDUAL_NINO`, `INDIVIDUAL_NORMAL`, `INDIVIDUAL_DELUXE`, `FAMILIAR_1`, `FAMILIAR_2`, `FAMILIAR_3`,
`VISITA`).

| Campo | Tipo | Req | Descripción |
|---|---|---|---|
| `precio` | Number | S | Nuevo precio (MXN), mayor a 0 |
| `moneda` | String | S | `MXN` |
| `actualizadoEn` | Timestamp | S | Cuándo se cambió |
| `actualizadoPor` | String | S | uid de quien lo cambió |

#### `areas`
Espacios reservables (canchas y albercas). **PK:** id automático.

| Campo | Tipo | Req | Descripción |
|---|---|---|---|
| `nombre` | String | S | Nombre visible; no se repite dentro del deporte |
| `tipo` | String | S | Deporte: Fútbol, Básquetbol, Tenis, Natación u otro dado de alta |
| `capacidad` | Number | S | Cupo máximo de personas por hora |
| `disponibilidad` | String | S | `DISPONIBLE` · `OCUPADA` · `MANTENIMIENTO` (solo esta última bloquea reservas) |
| `permiteExternos` | Boolean | S | Si los visitantes externos pueden reservarla |
| `emoji` | String | N | Emoji propio de un deporte nuevo |
| `creadoEn` / `actualizadoEn` | Timestamp | N | Auditoría |

No se puede eliminar un área con reservas futuras o torneos pendientes. Al eliminarla se borra su horario.

#### `herramientas`
Material deportivo del club. **PK:** id automático. **FK:** `areaIds[]` → `areas`. La pantalla Inventario lee y escribe
aquí (`cantidadTotal`, `stockMinimo`, `icono`, `deporte`).

| Campo | Tipo | Req | Descripción |
|---|---|---|---|
| `nombre` | String | S | Nombre del artículo |
| `deporte` | String | S | Deporte al que pertenece (igual que `areas.tipo`) |
| `areaIds` | Array&lt;String&gt; | S | Áreas donde se usa |
| `icono` | String | S | Emoji |
| `cantidadTotal` | Number | S | Existencias totales |
| `cantidadDisponible` | Number | S | Existencias libres (≤ `cantidadTotal`) |
| `stockMinimo` | Number | S | Umbral de alerta; 0 = sin alerta |
| `estado` | String | S | `BUENO` · `REGULAR` · `DANADO` · `BAJA` |

#### `restriccionesHorario`
Horario de apertura de cada área. **PK:** `rh-{areaId}`. **FK:** `areaId` → `areas`. Es informativa: la app calcula
el horario por deporte desde el código y no lee esta colección.

| Campo | Tipo | Req | Descripción |
|---|---|---|---|
| `areaId` | String | S | FK a `areas` |
| `areaNombre` | String | S | Nombre del área (copia) |
| `tipo` | String | S | `HORARIO_GENERAL` (reservado: `CIERRE_ESPECIAL`) |
| `dias` | Array&lt;String&gt; | S | `LUN` `MAR` `MIE` `JUE` `VIE` `SAB` `DOM` |
| `horaApertura` | String | S | `HH:mm` |
| `horaCierre` | String | S | `HH:mm` |
| `activa` | Boolean | S | `false` = se ignora |
| `motivo` | String | N | Descripción libre |

#### `reservas`
Uso de un área por una persona en una fecha y rango de horas. **PK:** id automático. **FK:** `usuarioId` →
`usuarios`, `areaId` → `areas`.

| Campo | Tipo | Req | Descripción |
|---|---|---|---|
| `usuarioId` | String | S | Quien reserva |
| `usuarioNombre` | String | S | Nombre de quien reserva (copia histórica) |
| `areaId` | String | S | FK a `areas` |
| `areaNombre` | String | S | Nombre del área al reservar (copia histórica) |
| `deporte` | String | S | Deporte del área (copia histórica) |
| `fecha` | String | S | Día de la reserva |
| `horaInicio` | String | S | `HH:mm`, siempre en punto |
| `horaFin` | String | S | `HH:mm`, posterior a `horaInicio`; máximo 4 horas |
| `duracionHoras` | Number | S | `horaFin` − `horaInicio` |
| `personas` | Number | S | Lugares del cupo que ocupa (mínimo 1) |
| `estado` | String | S | `CONFIRMADA` · `PENDIENTE_APROBACION` · `CANCELADA` · `FINALIZADA` |
| `esExterno` | Boolean | S | `true` si la hizo un visitante externo |
| `canceladaEn` | Timestamp | N | Solo si `CANCELADA` |
| `sinPenalizacion` | Boolean | N | `true` si se canceló con 4 horas o más de anticipación (solo si `CANCELADA`) |
| `creadoEn` / `actualizadoEn` | Timestamp | N | Auditoría |

#### `materialAsignado`
Material del inventario que se aparta para una reserva. **PK:** id automático. **FK:** `reservaId` → `reservas`,
`herramientaId` → `herramientas`, `areaId` → `areas`.

| Campo | Tipo | Req | Descripción |
|---|---|---|---|
| `reservaId` | String | S | FK a `reservas` |
| `herramientaId` | String | S | FK a `herramientas` |
| `herramientaNombre` | String | S | Nombre de la herramienta (copia histórica) |
| `areaId` | String | S | Área de la reserva |
| `fecha` | String | S | Día de la reserva |
| `cantidad` | Number | S | Unidades apartadas |
| `estado` | String | S | `ASIGNADO` · `ENTREGADO` · `DEVUELTO` · `CANCELADO` (hoy la app usa `ASIGNADO` y `CANCELADO`) |
| `creadoEn` | Timestamp | N | Alta |

#### `checkins`
Llegada o salida de una persona a su reserva. **PK:** id automático. **FK:** `reservaId` → `reservas`, `usuarioId` →
`usuarios`, `areaId` → `areas`. Todavía sin pantalla ni escritura.

| Campo | Tipo | Req | Descripción |
|---|---|---|---|
| `reservaId` | String | S | FK a `reservas` |
| `usuarioId` | String | S | Quien llegó |
| `areaId` | String | S | Área donde se registró |
| `fecha` | String | S | Día |
| `fechaHora` | String | S | Momento exacto, ISO `yyyy-MM-ddTHH:mm:ss` |
| `tipo` | String | S | `ENTRADA` · `SALIDA` |
| `metodo` | String | S | `MANUAL` · `CODIGO` |

#### `notificaciones`
Avisos para una persona. **PK:** id automático. **FK:** `usuarioId` → `usuarios`. Todavía sin pantalla ni escritura.

| Campo | Tipo | Req | Descripción |
|---|---|---|---|
| `usuarioId` | String | S | Destinatario |
| `tipo` | String | S | `RESERVA` · `MEMBRESIA` · `TORNEO` · `SISTEMA` |
| `titulo` | String | S | Encabezado corto |
| `mensaje` | String | S | Texto del aviso |
| `leida` | Boolean | S | `false` hasta que se abre |
| `creadoEn` | Timestamp | S | Cuándo se generó |

#### `torneos`
Competencia que ocupa un área completa en un rango de fechas y horas. **PK:** id automático. **FK:** `areaId` →
`areas`.

| Campo | Tipo | Req | Descripción |
|---|---|---|---|
| `nombre` | String | S | Nombre del torneo |
| `disciplina` | String | S | Deporte; coincide con `areas.tipo` |
| `areaId` | String | S | FK a `areas` |
| `areaNombre` | String | S | Nombre del área (copia) |
| `fechaInicio` | String | S | Primer día |
| `fechaFin` | String | S | Último día; no anterior a `fechaInicio` |
| `horaInicio` | String | N | Con `horaFin`, horario de cada día del rango |
| `horaFin` | String | N | Si faltan las dos horas, ocupa el día completo |
| `cupoMaximo` | Number | S | Participantes permitidos |
| `inscritos` | Number | S | Contador de inscripciones confirmadas (0 ≤ `inscritos` ≤ `cupoMaximo`) |
| `creadoPor` | String | N | uid de quien lo creó |
| `creadoEn` / `actualizadoEn` | Timestamp | N | Auditoría |

#### `inscripcionesTorneo`
Participación de una persona en un torneo. **PK:** `{torneoId}_{usuarioId}` (impide inscribirse dos veces).
**FK:** `torneoId` → `torneos`, `usuarioId` → `usuarios`.

| Campo | Tipo | Req | Descripción |
|---|---|---|---|
| `torneoId` | String | S | FK a `torneos` |
| `torneoNombre` | String | S | Nombre del torneo (copia histórica) |
| `usuarioId` | String | S | FK a `usuarios` |
| `usuarioNombre` | String | S | Nombre de la persona (copia histórica) |
| `fechaInscripcion` | String | S | Día de inscripción |
| `estado` | String | S | `CONFIRMADA` · `CANCELADA` |
| `creadoEn` | Timestamp | N | Alta |

#### `config`
Ajustes internos. Documento actual: `config/esquema`.

| Campo | Tipo | Req | Descripción |
|---|---|---|---|
| `version` | Number | S | Versión del esquema a la que ya se migró la base (la migración del código está en la 5) |
| `migradoEn` | Timestamp | S | Cuándo se hizo la última migración |
| `camposAgregados` | Map | S | Cuántos campos se agregaron por colección en esa migración |

#### Relaciones

```
usuarios ──< miembros (usuarioId) >── membresias ──< pagos
   │                                       └──< integrantesFamiliares (obsoleta)
   ├──< reservas >── areas ──< restriccionesHorario
   │       ├──< materialAsignado >── herramientas
   │       └──< checkins
   ├──< inscripcionesTorneo >── torneos >── areas
   └──< notificaciones
precios, config: independientes
```

## Flujos principales

### 1. Acceso (dos formas)

- **Correo y contraseña:** login de Firebase Authentication. Se carga el perfil de `usuarios/{uid}`; si el estado es
  `INACTIVO`, se bloquea el acceso.
- **Código de miembro (`CLB-7K3M9Q`):** ya no sirve para iniciar sesión; sirve para **registrarse una vez**
  (*Regístrate con tu código*). Al dar de alta a un miembro, el personal deja una cuenta interna en Firebase (correo
  `clb-xxxxxx@miembros.clubdeportivo.app`, contraseña = el código) y un documento `registroCodigos/{código}`. Al
  registrarse, la persona demuestra que conoce el código, se crea su cuenta real (correo y contraseña propios), el
  código queda marcado como usado y la cuenta interna se borra. Si el código ya tiene cuenta, la app lo avisa.
  - Quien tenga una membresía suspendida o vencida puede entrar, pero no reservar (ve el motivo).
  - Si la membresía se suspende o vence, se bloquea a todas las personas de esa membresía.
- **Registro:** `RegistroScreen` crea la cuenta y su perfil en `usuarios`.

### 2. Reservar (socio o visitante)

1. Elige área, fecha, hora de inicio y fin (de 1 a 4 horas) y cuántas personas.
2. La app valida con datos recién leídos de Firestore:
   - Anticipación de 2 horas como mínimo y 7 días como máximo.
   - Máximo 3 reservas activas.
   - Bloqueo de 7 días tras 3 inasistencias en 30 días.
   - Área que no esté en mantenimiento.
   - Cupo por hora: la suma de `personas` no puede pasar de la `capacidad` del área.
   - Sin choque con un torneo, que ocupa el área completa en su horario.
3. Se crea la reserva: `CONFIRMADA`, o `PENDIENTE_APROBACION` si es visitante externo.
4. Cancelar es gratis con 4 horas o más de anticipación. Con menos se permite, pero se avisa de la penalización.

El personal no tiene límite de reservas ni bloqueo por inasistencias, y ve las reservas de todos.

### 3. Membresías (personal)

- La pantalla tiene las pestañas **Miembros** y **Planes y precios**.
- Se registra al titular y, en los paquetes familiares, a sus integrantes. Cada persona recibe su código.
- Se puede renovar, suspender, cambiar de plan y editar precios.
- Una membresía vence al mes. Renovar suma un mes desde el vencimiento si sigue vigente, o desde hoy si ya venció.
- El socio solo ve el catálogo de planes y su propia membresía.

| Plan | Precio |
|---|---|
| Individual niños | $1,500 / mes |
| Individual normal | $1,800 / mes |
| Individual deluxe | $3,500 / mes (prioridad en reservas, entrenamiento personalizado, agua y snacks) |
| Paquete Familiar (2 adultos + 3 niños) | $7,000 / mes |
| Paquete Pareja (2 adultos) | $2,999 / mes |
| Paquete Niños (hermanos) | $2,800 / mes |
| Visita | $200 (vence el mismo día) |

### 4. Personal (solo admin)

Alta de personal con rol Administrador o Empleado de apoyo. Crea su cuenta de Firebase y captura tipo de trabajo,
turno y área.

### 5. Áreas e inventario (personal)

- **Áreas:** alta, edición y borrado, con su capacidad, su disponibilidad (disponible, ocupada o mantenimiento) y si
  permiten externos.
- **Inventario:** control de artículos con stock mínimo e historial de movimientos.

### 6. Torneos

Están dentro de la pestaña "Torneo" de Reservas. Se crean, se editan y se inscriben personas con cupo máximo. No
pueden crearse encima de reservas vigentes ni de otro torneo en la misma área y horario.

### 7. Dashboard (admin)

Muestra totales de áreas, personal, miembros con membresía activa y reservas de hoy.

## Pendientes y limitaciones conocidas

- **Pagos:** desde esta versión cada alta y renovación crea un documento en `pagos` (monto, método y fecha). No se
  inventan cobros de antes: el dashboard solo cuenta lo registrado desde entonces.
- **Herramientas:** al asignar material a una reserva no baja `cantidadDisponible`.
- **Avisos:** viven en la colección `avisos` (bandeja con campana para todos los roles). La colección antigua
  `notificaciones` ya no se usa.
- **Sesión:** la app recuerda la sesión de Firebase en el teléfono hasta que la persona cierra sesión.
- **Concurrencia de reservas:** Firestore no hace transacciones sobre consultas; dos personas que confirmen la última
  plaza al mismo tiempo podrían pasar las dos.
- **Seguridad:** el código de miembro funciona como contraseña, así que debe entregarse en persona.
- **Aprobación de reservas de visitantes externos:** no se verificó si existe una pantalla para aprobarlas.
- **Documentación desactualizada:** partes de `docs/` (por ejemplo, Home, Torneos y Perfil como pestañas, usuarios
  demo, "no hay panel de administración") ya no coinciden con el código.
