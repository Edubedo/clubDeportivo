# ClubDeportivo (Athletic Club): arquitectura y conexión a Firebase

Documento de apoyo para la exposición. Está basado en el código del repositorio, no solo en la documentación existente.

---

## 1. Visión general

**ClubDeportivo** es una app Android nativa para gestionar un club deportivo: áreas, reservas, membresías, torneos, inventario y personal.

| Pieza | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| UI | Jetpack Compose + Material 3 (sin XML) |
| Patrón | MVVM (ViewModel + LiveData) |
| Navegación | Navigation Compose |
| Asincronía | Coroutines (`suspend`, `viewModelScope`) |
| Backend | Firebase (Authentication + Firestore + 1 Cloud Function) |
| Imágenes | Coil |
| SDK | minSdk 24 / targetSdk 37 |

### Frontend y backend

- **Frontend:** la app Android (carpeta `app/`).
- **Backend:** Firebase. No escribimos ni alojamos un servidor propio. Esto se llama **BaaS** (*Backend as a Service*).

```
┌──────────────────┐     internet (SDK de Firebase)     ┌────────────────────────────┐
│  App Android     │ ─────────────────────────────────▶ │ Firebase                   │
│  (Compose+MVVM)  │ ◀───────────────────────────────── │  • Authentication          │
└──────────────────┘                                    │  • Firestore + Reglas      │
                                                        │  • Cloud Function (Node)   │
                                                        └────────────────────────────┘
```

Ningún dato importante vive de forma permanente en el teléfono. Todo está en Firebase, así que cualquier dispositivo con una cuenta accede a los mismos datos.

### Roles

`SUPERADMIN`, `ADMIN`, `ADMIN_AREA`, `AYUDANTE_AREA`, `SOCIO`, `VISITANTE_EXTERNO`.

| Rol | Qué ve |
|---|---|
| Administrador (`SUPERADMIN`, `ADMIN`) | Dashboard, Personal, Áreas, Reservas, Membresías |
| Encargado (`AYUDANTE_AREA`, `ADMIN_AREA`) | Reservas, su área e inventario de su deporte |
| Cliente (`SOCIO`, `VISITANTE_EXTERNO`) | Reservar, su membresía y su perfil |

---

## 2. Arquitectura en 3 capas

```
 ui/          Screen (@Composable)  +  ViewModel
   │             dibuja                 estado y lógica
   ▼
 data/repository/   Interfaz  ──implementada por──▶  Firebase*Repository
   │
   ▼
 data/model/        data classes: Usuario, Reserva, Area, Membresia, Torneo...
```

**Regla de oro:** una pantalla nunca habla con Firebase. La cadena siempre es:

```
Screen → ViewModel → Repositorio → Firebase
```

### 2.1 Estructura de carpetas

```
app/src/main/java/com/example/clubdeportivo/
├── MainActivity.kt          Única Activity; solo llama a ClubDeportivoApp()
├── data/
│   ├── model/               data classes y enums del dominio
│   ├── repository/          interfaces + implementaciones Firebase*Repository
│   ├── AppContainer.kt      punto único de acceso a los repositorios
│   ├── SesionManager.kt     usuario logueado, en memoria
│   └── Catalogos.kt         precios y reglas de negocio centralizadas
├── ui/
│   ├── ClubDeportivoApp.kt  Scaffold + NavHost
│   ├── navigation/          rutas y menú por rol
│   ├── theme/               colores y tipografía (Material 3)
│   ├── components/          piezas reutilizables
│   └── <pantalla>/          un par Screen + ViewModel por pantalla
└── util/                    Resultado<T>, Fechas, ReglasReserva, ReglasMembresia...
```

### 2.2 Capa UI

Cada pantalla es un par de archivos:

- **`XScreen.kt`**: función `@Composable` que solo dibuja y reenvía los eventos del usuario.
- **`XViewModel.kt`**: guarda el estado en `LiveData` y llama al repositorio dentro de `viewModelScope.launch { }`.

La Screen se suscribe con `observeAsState()`. Cuando cambia el `LiveData`, Compose redibuja solo.

**Ejemplo, el login (`LoginViewModel`):**

1. Valida el correo y la contraseña.
2. Llama a `authRepository.login(...)`.
3. Recibe un `Resultado`. Si es `Exito`, guarda al usuario en `SesionManager` y activa `_loginExitoso`.
4. La pantalla reacciona y navega a la ruta inicial del rol.

### 2.3 Capa de datos

- **Interfaces** (`ReservaRepository`, `AreaRepository`, `AuthRepository`...) definen *qué* se puede hacer.
- **Implementaciones `Firebase*Repository`** definen *cómo*, hablando con Firestore.
- Las `Fake*Repository` son versiones viejas en memoria. Quedan solo de referencia, salvo `FakeRestriccionHorarioRepository`, que sí se usa porque el horario por tipo de área es configuración fija.
- **`AppContainer`** es un `object` singleton que crea cada repositorio una sola vez. Es la inyección de dependencias manual (no hay Hilt). Como todas las pantallas comparten la misma instancia, una reserva creada en "Reservar" aparece en "Mis reservas".

```kotlin
object AppContainer {
    val authRepository: AuthRepository = FirebaseAuthRepository()
    val areaRepository: AreaRepository = FirebaseAreaRepository()
    val reservaRepository: ReservaRepository = FirebaseReservaRepository()
    val membresiaRepository: MembresiaRepository = FirebaseMembresiaRepository()
    val torneoRepository: TorneoRepository = FirebaseTorneoRepository()
    // ...
}
```

- **`Resultado<T>`** es una `sealed class` con `Exito(datos)` y `Error(mensaje)`. Así los ViewModels no manejan excepciones sueltas.
- **`SesionManager`** guarda en memoria al `Usuario` logueado, para que cualquier pantalla sepa "quién soy".

**Ventajas de este diseño:**

- Se puede cambiar de base de datos sin tocar ninguna pantalla.
- Los ViewModels se pueden probar con repositorios falsos (hay tests en `src/test`).
- Las reglas de negocio están en `util/` como funciones puras y probadas: `ReglasReserva` (cupo, choques, 1 a 4 horas), `ReglasMembresia` (códigos y vigencia) y `ReglasContrasena`.

### 2.4 Navegación y control por rol

- Una sola `Activity` (`MainActivity`), que solo ejecuta `ClubDeportivoApp()`.
- `ClubDeportivoApp` arma el `Scaffold` (barra superior e inferior) y un `NavHost` que arranca en `login`.
- El menú inferior depende del rol (`menuPara(rol)`).
- `RutaProtegida` + `rutaPermitida` impiden entrar a una ruta que el rol no tiene. Si no está permitida, redirige a la ruta inicial del rol.
- Tras el login se hace `popUpTo(LOGIN) { inclusive = true }`, para que el botón "atrás" no regrese al login.

---

## 3. Cómo nos conectamos a Firebase

### 3.1 Configuración del proyecto

1. En la consola de Firebase se registra la app con su `applicationId` (`com.example.clubdeportivo`).
2. Se descarga `app/google-services.json`. Identifica el proyecto de Firebase. No es una contraseña, pero no se sube al repositorio (está en `.gitignore`).
3. En `app/build.gradle.kts`:
   - Se aplica el plugin `com.google.gms.google-services`, que lee el JSON al compilar. Solo se aplica si el archivo existe, para no romper el build de quien aún no lo tiene.
   - Se agregan `firebase-bom` (fija versiones compatibles), `firebase-auth` y `firebase-firestore`.
   - Se agrega `kotlinx-coroutines-android` para usar `.await()`.
4. `AndroidManifest.xml` declara el permiso `INTERNET`.

> **Problema que ya nos pasó:** si el package del `google-services.json` no coincide con el `applicationId`, el build falla con "no matching client".

### 3.2 Inicialización

No hay código de inicialización propio. El plugin y el SDK crean `FirebaseApp` al arrancar la app. Desde ahí, `FirebaseAuth.getInstance()` y `FirebaseFirestore.getInstance()` ya funcionan.

Cada repositorio los recibe por constructor, con esas instancias como valor por defecto:

```kotlin
class FirebaseAreaRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AreaRepository
```

### 3.3 Firestore: lectura y escritura

Firestore es una base **NoSQL de documentos**. Una colección equivale a una tabla y un documento a una fila.

**Colecciones principales:** `usuarios`, `miembros`, `registroCodigos`, `membresias`, `integrantesFamiliares`, `pagos`, `precios`, `areas`, `herramientas`, `restriccionesHorario`, `reservas`, `materialAsignado`, `checkins`, `torneos`.

**Lectura (`FirebaseAreaRepository`):**

```kotlin
private val coleccion = db.collection("areas")

override suspend fun obtenerAreas(): List<Area> =
    coleccion.get().await().documents.mapNotNull { it.toArea() }
```

- `.get()` devuelve una `Task` de Firebase. `.await()` la convierte en una función `suspend`, y así la UI no se bloquea.
- **Mapeo manual:** `toArea()` hace `getString("nombre")`, `getLong("capacidad")`, etc. Se hizo así, en vez del `toObject()` automático, porque las data classes de Kotlin sin constructor por defecto no lo soportan de forma confiable.

**Escritura:**

- Se construye un `mapOf(...)` y se usa `add()` (id automático), `set()`, `update()` o `delete()`.
- `creadoEn` y `actualizadoEn` usan `FieldValue.serverTimestamp()`, para que la hora la ponga el servidor y no el celular.
- **Batches (`db.batch()`):** varias escrituras atómicas. Por ejemplo, al borrar un área también se borran sus horarios.

**Decisiones de modelado:**

- **Ids como `String`:** los ids de documento de Firestore son texto. Excepción: `PaqueteFamiliar`, que son 3 opciones fijas en `Catalogos.kt`.
- **Desnormalización:** la reserva guarda `areaNombre` y `usuarioNombre`. Así las listas no necesitan lecturas extra por fila y no dependen de que el área siga existiendo.
- **Datos derivados:** `torneo.inscritos` no se guarda. Se calcula con una consulta `count()`, para que no quede desactualizado.

### 3.4 Autenticación (`FirebaseAuthRepository`)

| Operación | Cómo se hace |
|---|---|
| Login | `signInWithEmailAndPassword(correo, pass)` devuelve un `uid`; con él se lee `usuarios/{uid}` (nombre, rol, estado). Si el estado es `INACTIVO`, se cierra la sesión. |
| Restaurar sesión | `restaurarSesion()` revisa `auth.currentUser`. Si el teléfono recuerda la sesión, entra sin pedir contraseña. |
| Recuperar contraseña | `sendPasswordResetEmail(correo)` |
| Cerrar sesión | `auth.signOut()` |
| Errores | `traducirError()` convierte las excepciones de Firebase en mensajes en español. |

**La contraseña nunca está en Firestore.** La guarda y verifica Firebase Authentication. Firestore solo guarda el perfil en `usuarios/{uid}`, donde `uid` es el identificador que genera Authentication.

### 3.5 Registro con código de miembro

Los miembros no se registran libremente. El personal los da de alta y cada uno recibe un **código tipo `CLB-7K3M9Q`**.

1. El personal crea el miembro. Se crea una cuenta interna cuyo correo se deriva del código y cuya contraseña es el propio código.
2. El miembro escribe su código. `consultarCodigo` lee `registroCodigos/{codigo}`. Esa es la única lectura permitida sin sesión, y solo por `get`, no por `list`.
3. `registrarMiembro`:
   1. Entra con la cuenta interna, lo que prueba que conoce el código. Lo hace en una **segunda instancia de `FirebaseApp`**, para no pisar la sesión principal.
   2. Crea su cuenta real (su correo y contraseña).
   3. Marca el código como usado y apunta el miembro a la cuenta nueva, con un batch.
   4. Crea su perfil `usuarios/{uid}` con el rol que le dio el personal.
   5. **Borra la cuenta interna**, para que el código deje de servir como contraseña.
4. Si algo falla a medias, `deshacerRegistro` hace rollback y el código queda disponible otra vez.

El mismo truco de la segunda instancia lo usa `FirebasePersonalRepository` para que un administrador cree cuentas del personal sin cerrar su propia sesión.

### 3.6 Seguridad: reglas de Firestore (`firestore.rules`)

**El cliente móvil no es de confianza.** La seguridad real vive en el servidor, en las reglas de Firestore.

Las reglas consultan el rol del usuario en `usuarios/{uid}` mediante funciones auxiliares: `esAdmin()`, `esPersonal()`, `activo()`, `conPerfil()`.

Ejemplos:

- Un cliente solo puede crear reservas **a su nombre y en estado `PENDIENTE_APROBACION`**.
- Solo un admin o el encargado del área de esa reserva puede aprobarla o rechazarla.
- Un usuario puede editar su nombre, teléfono y foto, pero **nunca su rol ni su estado** (`affectedKeys().hasOnly([...])`).
- Los precios solo los cambia un administrador.
- Los pagos solo los ve el personal.
- `registroCodigos` permite `get` sin sesión, pero no listar.

Aunque alguien modifique la app, el servidor rechaza lo que no le corresponde. Las reglas se publican a mano desde la consola de Firebase. Hay pruebas en `tools/pruebas-reglas/`.

### 3.7 Cloud Function (`functions/index.js`)

Cambiar la contraseña de otra persona exige permisos de administrador del proyecto, que no pueden estar dentro de la app. Por eso existe la función `cambiarContrasenaPersonal` en Node.js con `firebase-admin`:

1. Verifica que quien llama esté autenticado y sea administrador activo.
2. Verifica que la cuenta objetivo sea del personal, no un miembro.
3. Valida la contraseña nueva.
4. La cambia con `getAuth().updateUser` y revoca las sesiones abiertas de esa persona.

La app la llama por HTTPS desde `FirebasePersonalRepository.cambiarContrasena`, enviando el token de la sesión. No usa la librería `firebase-functions` del SDK.

---

## 4. Flujo completo de ejemplo: crear una reserva

```
ReservasScreen         el usuario toca "Reservar"
      │
ReservasViewModel      valida con ReglasReserva: cupo, choques, límites, bloqueo por inasistencias
      │
FirebaseReservaRepository.crearReserva()
      │   • lee areas/{id} y usuarios/{uid}
      │   • reservas.add(mapOf(...))
      │   • personal → CONFIRMADA (y se asigna material)
      │   • cliente  → PENDIENTE_APROBACION
      ▼
Firestore + reglas     comprueba usuarioId == auth.uid y el estado permitido
      │
Repositorio devuelve Reserva → ViewModel actualiza LiveData → Compose redibuja
      │
El encargado del área ve la reserva pendiente y la aprueba o rechaza
(esa actualización también pasa por las reglas)
```

---

## 5. Limitaciones y mejoras

- **Concurrencia:** Firestore no permite transacciones sobre consultas. Dos personas que confirmen al mismo tiempo la última plaza podrían pasar ambas. El riesgo es bajo a la escala del club.
- **`SesionManager` solo guarda el usuario en memoria.** Aun así, la sesión sí se restaura al abrir la app, porque `restaurarSesion()` usa la sesión que recuerda Firebase Authentication.
- **`restriccionHorarioRepository`** sigue siendo local a propósito: es configuración fija, no datos de usuario.
- **Decisión de base de datos:** el esquema original era relacional, pero se eligió Firestore (NoSQL) por decisión del equipo. Evita montar y mantener un servidor propio.
- **Documentación previa:** `docs/arquitectura.md` está parcialmente desactualizado. Por ejemplo, dice que la sesión no se restaura al abrir la app y que no existe `restaurarSesion`. Ante una duda, manda el código.

---

## 6. Guion sugerido (≈10 minutos)

| Tiempo | Tema |
|---|---|
| 1 min | Qué hace la app y qué roles tiene |
| 1 min | Stack técnico y la idea de BaaS |
| 3 min | Las 3 capas y la regla de oro, con el diagrama |
| 2 min | Conexión a Firebase: `google-services.json`, plugin, SDK y `.await()` |
| 2 min | Autenticación y registro con código |
| 1 min | Reglas de seguridad y Cloud Function |
| Demo | Login → reservar como miembro → aprobar como encargado |

---

## 7. Preguntas probables

- **¿Por qué no hay un backend propio?** Firebase ya da autenticación, base de datos y reglas de seguridad, y Google lo mantiene en línea. Solo escribimos una función para lo que requiere privilegios de administrador.
- **¿Dónde se guarda la contraseña?** Solo en Firebase Authentication, nunca en Firestore ni en la app.
- **¿Qué impide que un cliente se haga administrador?** Las reglas de Firestore: solo permiten que cada quien edite nombre, teléfono y foto, y el rol solo lo cambia un administrador.
- **¿Por qué repositorios con interfaz?** Para desacoplar la UI de Firebase, cambiar la fuente de datos sin tocar pantallas y poder hacer pruebas unitarias.
- **¿Por qué mapeo manual de documentos?** Por confiabilidad con data classes de Kotlin y por claridad.
- **¿Qué pasa sin internet?** Los errores de red se capturan y se traducen a "Sin conexión. Revisa tu internet e intenta de nuevo."
