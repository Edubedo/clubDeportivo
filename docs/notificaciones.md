# Notificaciones y recordatorios

Todas las personas con sesión reciben recordatorios en el teléfono, pero **cada tipo de usuario recibe solo los que le
sirven** (así no se llena de avisos que no le tocan). En **Perfil → Notificaciones** cada quien apaga o enciende cada
categoría, y desde los ajustes de Android también puede silenciar una categoría por separado (cada una es un canal).

## Qué recibe cada tipo de usuario

| Categoría | Miembro / visitante | Encargado de área | Administrador | Cuándo se avisa |
|---|:-:|:-:|:-:|---|
| **Recordatorio de reservas** | ✅ | | | 1 hora antes de que empiece una reserva confirmada suya |
| **Respuesta a tus solicitudes** | ✅ | | | Cuando una reserva "en revisión" se aprueba o se rechaza |
| **Tu membresía** | ✅ | | | 3 días antes de vencer (y "mañana"), el día que vence, y si la suspenden (con el motivo) |
| **Solicitudes por revisar** | | ✅ (su área) | ✅ (todas) | Reservas nuevas en revisión; varias se resumen en una sola notificación |
| **Asistencia por registrar** | | ✅ | | Reservas confirmadas de su área que ya empezaron (hasta 3 h) y no tienen asistencia |
| **Stock bajo** | | ✅ (su deporte) | ✅ | Un resumen al día (desde las 8:00) de artículos en o por debajo de su mínimo |
| **Membresías por vencer** | | | ✅ | Un resumen al día (desde las 8:00) de las que vencen en los próximos 7 días |
| **Torneos** | ✅ (si está inscrito) | ✅ (su deporte) | ✅ | 1 día antes de que empiece el torneo |
| **Avisos del club** | ✅ | ✅ | ✅ | Cuando alguien más publica un aviso dirigido a esa persona |

Reglas para no molestar: cada cosa se avisa **una sola vez**; no se envía nada entre las **22:00 y las 5:00** (sale en la
primera revisión de la mañana); los resúmenes de stock y vencimientos no salen antes de las 8:00; y en la primera revisión
tras instalar no se avisa de los avisos del club que ya existían. Tocar una notificación abre la pantalla relacionada.

## Cómo funciona (y su límite)

Son recordatorios **generados en el teléfono**: una tarea de segundo plano (WorkManager, `notificaciones/RecordatoriosWorker.kt`)
revisa Firestore con la sesión guardada cada ~15 minutos (el mínimo que permite Android) y también al abrir la app. No
necesita servidor ni plan de pago. La lógica de "qué recordar" está en `ReglasRecordatorios` y tiene pruebas unitarias.

Por eso un aviso puede llegar con hasta ~15 minutos de retraso, y si Android ahorra batería en exceso puede espaciar las
revisiones. Para eventos que deban llegar **al instante** (por ejemplo, "tu reserva fue aprobada" en el mismo segundo)
haría falta **push de servidor (Firebase Cloud Messaging)**:

1. Plan Blaze de Firebase y desplegar Cloud Functions (ya existe `functions/`).
2. Guardar el token FCM de cada teléfono en `usuarios/{uid}` (dependencia `firebase-messaging`).
3. Funciones que escuchan `reservas`, `avisos` y `membresias` y envían el mensaje, más una programada para los recordatorios por hora.
4. Reutilizar las mismas categorías, textos y canales de esta carpeta.

## Probar en el emulador

`MotorRecordatorios` acepta una hora simulada **solo en compilaciones con `-PfirebaseEmulador`**: escribe `ahora_simulada`
(milisegundos) en `shared_prefs/notificaciones_ajustes.xml` con `adb shell run-as com.example.clubdeportivo`, reabre la app
y la revisión corre "a esa hora" (útil de noche, cuando el silencio nocturno oculta todo).
