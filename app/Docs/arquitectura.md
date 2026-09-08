# Arquitectura

## Stack técnico

- **Lenguaje:** Kotlin
- **UI:** Vistas XML tradicionales (`Fragment` + `ViewBinding`), sin Jetpack Compose.
- **Patrón:** MVVM (`ViewModel` + `LiveData`) por pantalla.
- **Navegación:** Jetpack Navigation Component (`res/navigation/nav_graph.xml`) + `BottomNavigationView`.
- **Red (preparada, aún sin usar):** Retrofit + Gson + OkHttp logging interceptor.
- **Concurrencia:** Coroutines (`kotlinx.coroutines`).
- **minSdk 24 / targetSdk 37 / compileSdk 37.**

No hay inyección de dependencias (Hilt/Koin, etc.): el objeto `AppContainer` cumple ese rol a mano
(ver más abajo).

## Estructura de carpetas

```
app/src/main/java/com/example/clubdeportivo/
├── data/
│   ├── model/          → data classes y enums del dominio (Usuario, Reserva, Membresia, Torneo, Pago...)
│   ├── repository/     → interfaces de repositorio + implementaciones "Fake" en memoria
│   ├── remote/         → ApiService (endpoints Retrofit) y RetrofitClient (config HTTP)
│   ├── Catalogos.kt    → precios y reglas de negocio centralizadas
│   ├── SesionManager.kt→ usuario logueado actual, en memoria
│   └── AppContainer.kt → punto único de acceso a los repositorios
├── ui/
│   └── <pantalla>/     → un Fragment + un ViewModel por pantalla (login, home, areas, reservar,
│                          reservas, membresia, torneos, perfil)
└── util/                → helpers (Fechas, Resultado<T> para éxito/error)
```

## Capa de datos: repositorios "Fake"

Cada repositorio se define como interfaz (`AuthRepository`, `AreaRepository`,
`ReservaRepository`, `MembresiaRepository`, `TorneoRepository`,
`RestriccionHorarioRepository`) con una única implementación de prueba (`Fake...Repository`) que
guarda datos en memoria y simula latencia de red con `delay(...)`.

`AppContainer` (`data/AppContainer.kt`) instancia cada repositorio **una sola vez** como
`object`, y cada `ViewModel` lo consume desde ahí en vez de crear su propia instancia. Esto es lo
que hace que, por ejemplo, una reserva creada en la pantalla "Reservar" aparezca después en "Mis
reservas": ambas pantallas leen del mismo `FakeReservaRepository`.

```kotlin
object AppContainer {
    val authRepository: AuthRepository = FakeAuthRepository()
    val areaRepository: AreaRepository = FakeAreaRepository()
    val restriccionHorarioRepository: RestriccionHorarioRepository = FakeRestriccionHorarioRepository()
    val reservaRepository: ReservaRepository = FakeReservaRepository()
    val membresiaRepository: MembresiaRepository = FakeMembresiaRepository()
    val torneoRepository: TorneoRepository = FakeTorneoRepository()
}
```

Todos los métodos devuelven `Resultado<T>` (`util/Resultado.kt`), un wrapper simple de
éxito/error para que los `ViewModel` no manejen excepciones sueltas.

## Sesión

`SesionManager` (`data/SesionManager.kt`) guarda en memoria el `Usuario` que inició sesión, para
que cualquier pantalla pueda preguntar "¿quién soy?" sin volver a pedirlo al backend. Se pierde al
cerrar la app — es intencional mientras no hay backend; en producción esto se reemplazaría por un
token de sesión persistido en `EncryptedSharedPreferences` o `DataStore`.

## Navegación y pantallas

El grafo de navegación (`res/navigation/nav_graph.xml`) arranca en `loginFragment` y, tras un
login exitoso, hace un `popUpTo` que saca el login del back stack para llegar a `homeFragment`.
Desde ahí, la barra inferior (`bottom_nav_menu.xml`) da acceso a: Inicio, Áreas, Reservas,
Torneos y Perfil. Desde "Áreas" se navega a "Reservar" (pasando `areaId` y `areaNombre` como
argumentos de navegación), y desde "Perfil" se puede ir a "Mi membresía" o cerrar sesión (lo que
vacía el back stack completo y vuelve al login).

## Preparado para un backend real

`data/remote/ApiService.kt` ya define, con anotaciones de Retrofit, los endpoints que se espera
que exponga un backend Node.js + Express (`auth/login`, `areas`, `reservas/...`,
`membresias/usuario/{id}`, `torneos`, etc.), y `data/remote/RetrofitClient.kt` configura el
cliente HTTP (`BASE_URL = http://10.0.2.2:3000/api/`, la dirección que usa el emulador de Android
para apuntar al `localhost` de la computadora).

Cuando el backend exista, el trabajo es: reemplazar el cuerpo de cada `Fake...Repository` por una
llamada a `RetrofitClient.apiService`, manteniendo la misma interfaz de repositorio. Como los
`ViewModel` solo conocen la interfaz (a través de `AppContainer`), no hace falta tocarlos.
