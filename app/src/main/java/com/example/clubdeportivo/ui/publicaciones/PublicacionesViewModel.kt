package com.example.clubdeportivo.ui.publicaciones

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.esCliente
import com.example.clubdeportivo.data.model.esAdministrador
import com.example.clubdeportivo.data.publicaciones.AudienciaPublicacion
import com.example.clubdeportivo.data.publicaciones.Publicacion
import com.example.clubdeportivo.util.Fechas
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.launch
import com.example.clubdeportivo.util.Textos

class PublicacionesViewModel : ViewModel() {
    private val repository = AppContainer.publicacionesRepository
    private var publicaciones by mutableStateOf<List<Publicacion>>(emptyList())

    var cargando by mutableStateOf(false)
        private set
    var enviando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    val propias: List<Publicacion>
        get() = SesionManager.usuarioActual?.id?.let { id -> publicaciones.filter { it.autorId == id } }.orEmpty()

    val visibles: List<Publicacion>
        get() {
            val usuario = SesionManager.usuarioActual ?: return emptyList()
            return publicaciones.filter {
                it.audiencia == AudienciaPublicacion.TODOS ||
                    (usuario.rol.esCliente() && it.audiencia == AudienciaPublicacion.SOCIOS) ||
                    (!usuario.rol.esCliente() && it.audiencia == AudienciaPublicacion.PERSONAL)
            }
        }

    init {
        cargar()
    }

    fun cargar() {
        val usuario = SesionManager.usuarioActual ?: return
        val audiencias = when {
            usuario.rol.esAdministrador() -> AudienciaPublicacion.entries
            usuario.rol.esCliente() -> listOf(AudienciaPublicacion.TODOS, AudienciaPublicacion.SOCIOS)
            else -> listOf(AudienciaPublicacion.TODOS, AudienciaPublicacion.PERSONAL)
        }
        viewModelScope.launch {
            cargando = true
            error = null
            try {
                publicaciones = repository.obtener(audiencias)
            } catch (e: FirebaseFirestoreException) {
                Log.e(TAG, Textos.get("error_loading_posts_from_firestore"), e)
                error = if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Textos.get("firebase_rejected_access_publish_the_u")
                } else {
                    Textos.get("the_posts_could_not_be_loaded")
                }
            } catch (e: Exception) {
                Log.e(TAG, Textos.get("unexpected_error_loading_posts"), e)
                error = Textos.get("the_posts_could_not_be_loaded")
            } finally {
                cargando = false
            }
        }
    }

    fun guardar(
        id: String?,
        titulo: String,
        descripcion: String,
        imagenUrl: String,
        audiencia: AudienciaPublicacion,
        vigencia: String,
        alTerminar: () -> Unit
    ) {
        val usuario = SesionManager.usuarioActual ?: return
        if (vigencia.isNotBlank() && vigencia < Fechas.hoy()) {
            error = Textos.get("the_end_date_must_be_today")
            return
        }
        if (enviando || titulo.isBlank() || descripcion.isBlank() || imagenUrl.isBlank()) return
        viewModelScope.launch {
            enviando = true
            error = null
            try {
                val previa = id?.let { existente -> publicaciones.firstOrNull { it.id == existente } }
                val publicacion = Publicacion(
                    id = id.orEmpty(),
                    titulo = titulo.trim(),
                    descripcion = descripcion.trim(),
                    imagenUrl = imagenUrl,
                    audiencia = audiencia,
                    vigencia = vigencia,
                    autorId = previa?.autorId ?: usuario.id,
                    fechaMillis = previa?.fechaMillis ?: System.currentTimeMillis()
                )
                if (id == null) {
                    publicaciones = listOf(repository.crear(publicacion)) + publicaciones
                } else {
                    repository.actualizar(publicacion)
                    publicaciones = publicaciones.map { if (it.id == id) publicacion else it }
                }
                alTerminar()
            } catch (e: Exception) {
                error = Textos.get("the_post_could_not_be_saved")
            } finally {
                enviando = false
            }
        }
    }

    fun eliminar(publicacion: Publicacion, alTerminar: () -> Unit = {}) {
        viewModelScope.launch {
            error = null
            try {
                repository.eliminar(publicacion.id)
                publicaciones = publicaciones.filterNot { it.id == publicacion.id }
                alTerminar()
            } catch (e: Exception) {
                error = Textos.get("the_post_could_not_be_deleted")
            }
        }
    }

    fun limpiarError() {
        error = null
    }

    private companion object {
        const val TAG = "PublicacionesVM"
    }
}
