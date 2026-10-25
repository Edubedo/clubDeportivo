package com.example.clubdeportivo.data

import com.example.clubdeportivo.R
import com.example.clubdeportivo.data.model.Rol
import com.example.clubdeportivo.util.Textos
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration

/**
 * Vigila en tiempo real el perfil (`usuarios/{uid}`) de la persona que tiene la sesión abierta.
 *
 * El estado de la cuenta se revisaba solo al iniciar sesión, así que quien ya estaba dentro seguía usando la app
 * aunque un administrador lo hubiera suspendido. Ahora, en cuanto el perfil cambia a INACTIVO (o se borra), la sesión
 * se cierra sola y la persona vuelve al login con un aviso. Si cambian su rol, su área o su nombre, la sesión se
 * actualiza para que el menú y los permisos de la app coincidan con lo que decidió el administrador.
 */
object GuardianSesion {
    private var registro: ListenerRegistration? = null
    private var uidVigilado: String? = null

    fun vigilar(uid: String) {
        if (uidVigilado == uid && registro != null) return
        detener()
        uidVigilado = uid
        registro = FirebaseFirestore.getInstance().collection("usuarios").document(uid)
            .addSnapshotListener { perfil, error ->
                // Si la persona ya cerró sesión por su cuenta, el aviso de "sesión no válida" no aplica.
                if (uidVigilado != uid || SesionManager.usuarioActual?.id != uid) return@addSnapshotListener

                if (error != null) {
                    // Sin conexión no se saca a nadie; solo si el servidor ya no reconoce la cuenta.
                    val sinAcceso = error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                        error.code == FirebaseFirestoreException.Code.UNAUTHENTICATED
                    if (sinAcceso) expulsar(Textos.get(R.string.sesion_no_valida))
                    return@addSnapshotListener
                }
                if (perfil == null) return@addSnapshotListener

                if (!perfil.exists()) {
                    // Un "no existe" que viene de la caché del teléfono no prueba nada: se espera al servidor.
                    if (!perfil.metadata.isFromCache) expulsar(Textos.get(R.string.sesion_cuenta_eliminada))
                    return@addSnapshotListener
                }

                if (perfil.getString("estado") == "INACTIVO") {
                    expulsar(Textos.get(R.string.sesion_cuenta_suspendida))
                    return@addSnapshotListener
                }

                actualizarSesion(perfil.getString("nombre"), perfil.getString("rol"), perfil.getString("areaTrabajo"))
            }
    }

    fun detener() {
        registro?.remove()
        registro = null
        uidVigilado = null
    }

    /** Refleja en la sesión los cambios que un administrador le hizo al perfil mientras la persona usaba la app. */
    private fun actualizarSesion(nombre: String?, rolGuardado: String?, area: String?) {
        val actual = SesionManager.usuarioActual ?: return
        val rol = runCatching { Rol.valueOf(rolGuardado.orEmpty()) }.getOrDefault(actual.rol)
        val nuevo = actual.copy(
            nombre = nombre ?: actual.nombre,
            rol = rol,
            areaTrabajo = area
        )
        if (nuevo != actual) SesionManager.iniciarSesion(nuevo)
    }

    private fun expulsar(motivo: String) {
        detener()
        runCatching { FirebaseAuth.getInstance().signOut() }
        SesionManager.expulsar(motivo)
    }
}
