package com.example.clubdeportivo

import android.app.Application
import com.example.clubdeportivo.data.repository.EmuladoresFirebase
import com.example.clubdeportivo.notificaciones.Notificador
import com.example.clubdeportivo.notificaciones.ProgramadorRecordatorios
import com.example.clubdeportivo.util.PreferenciaIdioma
import com.example.clubdeportivo.util.Textos
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ClubApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PreferenciaIdioma.cargar(this)
        Textos.iniciar(this)
        // Recordatorios: canales de notificación y la revisión periódica en segundo plano (solo actúa si hay sesión guardada).
        Notificador.crearCanales(this)
        ProgramadorRecordatorios.programar(this)
        // Antes de que cualquier pantalla toque Firebase: con -PfirebaseEmulador la app usa los emuladores locales.
        if (EmuladoresFirebase.activo) {
            EmuladoresFirebase.configurar(FirebaseAuth.getInstance())
            EmuladoresFirebase.configurar(FirebaseFirestore.getInstance())
        }
    }
}
