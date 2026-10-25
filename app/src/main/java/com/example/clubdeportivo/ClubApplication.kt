package com.example.clubdeportivo

import android.app.Application
import com.example.clubdeportivo.data.repository.EmuladoresFirebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ClubApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Antes de que cualquier pantalla toque Firebase: con -PfirebaseEmulador la app usa los emuladores locales.
        if (EmuladoresFirebase.activo) {
            EmuladoresFirebase.configurar(FirebaseAuth.getInstance())
            EmuladoresFirebase.configurar(FirebaseFirestore.getInstance())
        }
    }
}
