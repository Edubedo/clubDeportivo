# Arquitectura

> Si nunca trabajaste con Android o Kotlin, empezá por
> [¿Esta app tiene backend y frontend?](#esta-app-tiene-backend-y-frontend) y
> [Para quien nunca vio un proyecto Android](#para-quien-nunca-vio-un-proyecto-android) antes de
> leer el resto — te dan el mapa mental que hace que todo lo demás tenga sentido.

## ¿Esta app tiene backend y frontend?

Sí, tiene los dos — solo que el backend no lo escribimos nosotros, lo provee Firebase.

- **Frontend:** es esta app de Android (todo lo que hay en `app/`, escrito en Kotlin +
  Jetpack Compose). Es el "cliente": lo que corre en el teléfono de cada usuario, dibuja las
  pantallas y reacciona a lo que toca.
- **Backend:** es **Firebase** — un conjunto de servidores de Google que dan, ya hechos,
  los dos servicios que normalmente tendrías que programar y alojar vos mismo:
  - **Authentication**: recibe correo/contraseña, valida, y decide si el login es válido.
  - **Firestore**: la base de datos remota (guarda áreas, reservas, torneos, etc.) y responde
    cuando la app pide o guarda datos.

La diferencia con un backend "tradicional" (por ejemplo uno en Node.js + Express, que es lo que
este proyecto tenía planeado originalmente — ver `data/repository/AuthRepository.kt`, que todavía
tiene comentarios viejos de esa idea) es que ahí el equipo tendría que escribir el código del
servidor, elegir dónde alojarlo (Render, Railway, un VPS...) y mantenerlo corriendo. Con Firebase,
Google ya tiene ese servidor corriendo 24/7 — la app simplemente le habla por internet usando el
SDK oficial (`FirebaseAuth`, `FirebaseFirestore`, ver `data/repository/Firebase*Repository.kt`).
Es lo que se llama un **BaaS** (*Backend as a Service*): "backend en la nube ya armado", en vez de
"backend que programás vos".

```
┌────────────────────┐        internet        ┌──────────────────────────┐
│   FRONTEND          │ ───────────────────▶  │   BACKEND (Firebase)     │
│   App de Android    │                        │   • Authentication       │
│   (este repositorio)│ ◀───────────────────  │   • Firestore (datos)    │
└────────────────────┘      respuestas         └──────────────────────────┘
```

Ningún dato "vive" dentro del teléfono de forma permanente (salvo mientras la app está abierta,
en `SesionManager`) — todo lo importante (usuarios, reservas, torneos...) vive del lado del
backend, en los servidores de Firebase, y cualquier dispositivo con la app instalada y una cuenta
puede acceder a esos mismos datos.

## Para quien nunca vio un proyecto Android

Pensá la app en **tres capas**, cada una en su propia carpeta, que solo se hablan entre ellas de
una manera (nunca "de atrás para adelante"):

```
┌─────────────────────────────────────────────────────────┐
│  ui/          "Lo que el usuario ve y toca"              │
│  cada pantalla = una función que dibuja la UI (Screen)   │
│  + una clase que guarda su estado y su lógica (ViewModel)│
└───────────────────────────┬─────────────────────────────┘
                             │ el ViewModel le PIDE datos al repositorio
                             ▼
┌─────────────────────────────────────────────────────────┐
│  data/repository/   "De dónde salen los datos"           │
│  una interfaz por tema (reservas, áreas, torneos...)     │
│  implementación real: le pregunta a Firebase (Firestore) │
│  (queda también una versión "Fake" vieja, de referencia) │
└───────────────────────────┬─────────────────────────────┘
                             │ ambas versiones hablan el mismo idioma:
                             ▼
┌─────────────────────────────────────────────────────────┐
│  data/model/   "La forma de los datos"                   │
│  qué campos tiene un Usuario, una Reserva, un Torneo...  │
└─────────────────────────────────────────────────────────┘
```

La regla de oro: **una pantalla nunca inventa datos ni habla directo con el backend** — siempre le
pregunta a su `ViewModel`, y el `ViewModel` siempre le pregunta al repositorio. Esto es lo que
permite que, más adelante, cambiemos "de dónde salen los datos" (de datos inventados a un backend
real) sin tocar ni una sola pantalla.

Un ejemplo concreto, la pantalla de Torneos:

- `ui/torneos/TorneosScreen.kt` — el dibujo: una lista de tarjetas, un botón "Inscribirme". No
  sabe nada de dónde salen los torneos, solo le pregunta al ViewModel.
- `ui/torneos/TorneosViewModel.kt` — el cerebro de esa pantalla: guarda la lista de torneos
  actual, y cuando el usuario toca "Inscribirme" le avisa al repositorio.
- `data/repository/TorneoRepository.kt` — la interfaz (`obtenerTorneos()`, `inscribirse(...)`).
  `data/repository/FirebaseTorneoRepository.kt` la implementa hablando con Firestore de verdad.
- `data/model/Torneo.kt` — la forma de un torneo: qué campos tiene (nombre, fechas, cupo...).

## Stack técnico

- **Lenguaje:** Kotlin.
- **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) + Material 3 — la UI se
  describe con funciones (`@Composable`), no con archivos XML. No hay layouts XML para las
  pantallas de la app.
- **Patrón:** MVVM (`ViewModel` + `LiveData`) — una clase `ViewModel` por pantalla, separada de la
  función que dibuja la pantalla.
- **Navegación:** [Navigation Compose](https://developer.android.com/develop/ui/compose/navigation),
  todo definido en código Kotlin (`ui/ClubDeportivoApp.kt` + `ui/navigation/Destinations.kt`), sin
  XML.
- **Base de datos remota:** [Firebase](https://firebase.google.com/) — Cloud Firestore (base de
  datos de documentos) + Firebase Authentication (login con correo/contraseña real). Es la única
  base de datos remota de la app; no hay backend propio (ver "Conectado a Firebase" más abajo).
- **Concurrencia:** Coroutines (`kotlinx.coroutines`) — así una pantalla puede "pedir datos y
  esperar" sin congelarse mientras espera.
- **minSdk 24 / targetSdk 37 / compileSdk 37.**

No hay inyección de dependencias (Hilt/Koin, etc.): el objeto `AppContainer` cumple ese rol a mano
(ver más abajo).

## Estructura de carpetas

```
app/src/main/java/com/example/clubdeportivo/
├── MainActivity.kt      → el punto de arranque de la app. Solo arma la ventana y le dice
│                          "dibujá esto" a ClubDeportivoApp(); no tiene lógica propia.
├── data/
│   ├── model/           → data classes y enums del dominio (Usuario, Reserva, Membresia, Torneo, Pago...)
│   ├── repository/      → interfaces de repositorio + implementaciones Firebase (y las viejas
│   │                       "Fake" en memoria, que ya no se usan pero quedan de referencia)
│   ├── Catalogos.kt     → precios y reglas de negocio centralizadas
│   ├── SesionManager.kt → usuario logueado actual, en memoria
│   ├── FirebaseSeeder.kt→ carga los datos de ejemplo (áreas, torneos) en Firestore la
│   │                       primera vez que la app los necesita y la colección está vacía
│   └── AppContainer.kt  → punto único de acceso a los repositorios
├── ui/
│   ├── ClubDeportivoApp.kt → arma la pantalla completa: la barra de arriba, el menú de abajo,
│   │                         y qué pantalla mostrar según por dónde navegó el usuario
│   ├── navigation/      → Destinations.kt: la lista de "nombres de pantalla" (rutas) que existen
│   ├── theme/           → colores y estilo visual (Material 3), en un solo lugar
│   ├── components/      → piezas de UI reutilizables entre pantallas (ej. una rueda de "cargando")
│   └── <pantalla>/      → un par Screen + ViewModel por pantalla (login, registro, home, areas,
│                          reservar, reservas, membresia, torneos, perfil)
└── util/                 → helpers (Fechas, Resultado<T> para éxito/error)
```

Dentro de cada `ui/<pantalla>/` hay siempre el mismo patrón, por ejemplo `ui/torneos/`:

- `TorneosScreen.kt` — **la UI**: una función `@Composable` que recibe el estado actual y dibuja
  la pantalla. No decide nada, solo muestra lo que el ViewModel le da y le avisa cuando el usuario
  toca algo.
- `TorneosViewModel.kt` — **el estado y la lógica**: guarda la lista de torneos, el "está
  cargando", etc., y le pide datos al repositorio cuando hace falta.

## Capa de datos: repositorios

Cada repositorio se define como interfaz (`AuthRepository`, `AreaRepository`,
`ReservaRepository`, `MembresiaRepository`, `TorneoRepository`,
`RestriccionHorarioRepository`). `AppContainer` (`data/AppContainer.kt`) instancia cada
repositorio **una sola vez** como `object`, y cada `ViewModel` lo consume desde ahí en vez de
crear su propia instancia. Esto es lo que hace que, por ejemplo, una reserva creada en la
pantalla "Reservar" aparezca después en "Mis reservas": ambas pantallas leen del mismo
`FirebaseReservaRepository`.

```kotlin
object AppContainer {
    val authRepository: AuthRepository = FirebaseAuthRepository()
    val areaRepository: AreaRepository = FirebaseAreaRepository()
    val restriccionHorarioRepository: RestriccionHorarioRepository = FakeRestriccionHorarioRepository()
    val reservaRepository: ReservaRepository = FirebaseReservaRepository()
    val membresiaRepository: MembresiaRepository = FirebaseMembresiaRepository()
    val torneoRepository: TorneoRepository = FirebaseTorneoRepository()
}
```

`restriccionHorarioRepository` es la única que sigue siendo la versión "Fake" (en memoria): el
horario por tipo de área es configuración fija de la app (no cambia con el uso), así que no
aporta nada guardarlo en la base de datos remota. Las viejas implementaciones `Fake...Repository`
de las demás (que inventaban datos en memoria) siguen en el proyecto sin usarse, como referencia
de cómo se ve el mismo patrón sin una base de datos real detrás.

Todos los métodos de `login`/`registrar` devuelven `Resultado<T>` (`util/Resultado.kt`), un
wrapper simple de éxito/error para que los `ViewModel` no manejen excepciones sueltas.

## Sesión

`SesionManager` (`data/SesionManager.kt`) guarda en memoria el `Usuario` que inició sesión, para
que cualquier pantalla pueda preguntar "¿quién soy?" sin volver a pedirlo a Firebase. Se pierde al
cerrar la app — Firebase Authentication sí recuerda la sesión entre reinicios (es su
comportamiento por defecto), pero esta app todavía no lo aprovecha: hoy, cerrar y volver a abrir
la app siempre vuelve a mostrar el login. Quedaría como una mejora a futuro.

## Conectado a Firebase

La base de datos remota es **Firebase**: Cloud Firestore guarda los datos (áreas, reservas,
torneos, etc. — una colección por tabla del modelo de datos, ver
[`modelo-de-datos.md`](modelo-de-datos.md)) y Firebase Authentication maneja el login real
(correo + contraseña). La configuración de conexión vive en `app/google-services.json`
(no se sube al control de versiones — cada quien usa el suyo, descargado desde la consola de
Firebase del proyecto).

**Reglas de seguridad de Firestore.** Por defecto Firestore no deja pasar nada; hay que definir
reglas en la consola de Firebase (pestaña **Firestore Database → Reglas**). Como mínimo, esto
exige haber iniciado sesión para leer o escribir cualquier dato:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

**Datos de catálogo (áreas, torneos).** Firestore empieza vacío. `data/FirebaseSeeder.kt` carga
los mismos datos de ejemplo que antes vivían hardcodeados en `FakeAreaRepository`/
`FakeTorneoRepository`, la primera vez que la app los necesita y encuentra la colección vacía —
no hace falta cargar nada a mano.

**Lo que todavía no se puede hacer desde la app** (falta pantalla para eso, no es un límite de
Firebase): contratar una membresía. Para probar la pantalla "Mi membresía" con datos hay que
crear a mano, desde la consola de Firebase (Firestore Database → Iniciar colección), un
documento en la colección `membresias` con al menos los campos `usuarioId` (el uid de una cuenta
ya registrada), `tipo`, `precio`, `estado`, `fechaInicio` y `fechaVencimiento`.

### Repo público y `google-services.json`

Este repositorio no tiene ninguna clave ni contraseña adentro — se revisó todo el código y el
historial de git. Lo único sensible es `app/google-services.json`, que **ya está en
`.gitignore`** y nunca se subió: es seguro hacer este repositorio público tal cual está.

Ese archivo identifica tu proyecto de Firebase (no es una "contraseña" en el sentido tradicional
— viaja igual dentro de cualquier app instalada — pero no hay razón para publicarlo). Cada
integrante del equipo lo necesita para poder compilar y correr la app. Dos formas de dárselo:

1. **Recomendada:** en la consola de Firebase → ⚙️ Configuración del proyecto →
   **Usuarios y permisos** → agregar el correo de Google de cada compañero (rol Editor o Viewer
   alcanza). Así cada uno entra a la consola con su propia cuenta y descarga el mismo
   `google-services.json` él mismo, sin que nadie tenga que reenviarlo.
2. **Más simple pero menos prolijo:** mandarles el archivo directo (Drive, WhatsApp, lo que
   usen) por un canal privado — nunca por un issue o PR público del repo.

En cualquier caso, cada compañero hace lo mismo que hiciste vos: guardar el archivo en
`app/google-services.json` dentro de su copia local del proyecto (no se commitea).

## Navegación y pantallas

Todas las pantallas viven dentro de una sola `Activity` (`MainActivity.kt`), que lo único que hace
es decirle a Compose "dibujá `ClubDeportivoApp()`". Adentro de `ClubDeportivoApp.kt` vive un
`NavHost`: una lista de "si la ruta es X, mostrá la pantalla Y", con las rutas definidas en
`ui/navigation/Destinations.kt` (`"home"`, `"areas"`, `"reservar/{areaId}/{areaNombre}"`, etc — el
equivalente en Compose a lo que antes era `nav_graph.xml`).

Arranca en `"login"` y, tras un login exitoso, saca el login (y "registro", si el usuario pasó por
ahí) de la pila de navegación para llegar a `"home"` (así el botón "atrás" del teléfono no vuelve
al login). Desde "login" también se puede ir a `"registro"` (crear cuenta) y volver. Desde
"home", la barra inferior da acceso a: Inicio, Áreas, Reservas, Torneos y Perfil. Desde "Áreas" se
navega a "Reservar" (pasando `areaId` y `areaNombre` como parte de la ruta), y desde "Perfil" se
puede ir a "Mi membresía" o cerrar sesión (lo que vacía toda la pila de navegación y vuelve al
login).
