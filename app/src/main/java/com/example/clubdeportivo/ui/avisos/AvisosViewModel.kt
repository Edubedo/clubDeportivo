package com.example.clubdeportivo.ui.avisos

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.data.model.esAdministrador
import com.example.clubdeportivo.data.model.esCliente
import com.example.clubdeportivo.data.notificaciones.DestinatarioNotificacion
import com.example.clubdeportivo.data.notificaciones.Notificacion
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.clubdeportivo.util.Textos

/**
 * Avisos del club para todos los roles. Se comparte en toda la app (la campana de la barra superior muestra cuántos
 * faltan por leer). "Leído" se guarda en el teléfono de cada persona, no en el servidor.
 */
class AvisosViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {

    private val repositorio = AppContainer.avisosRepository
    private val preferencias = aplicacion.getSharedPreferences("avisos", Context.MODE_PRIVATE)

    private var todos by mutableStateOf<List<Notificacion>>(emptyList())
    private var leidas by mutableStateOf<Set<String>>(emptySet())
    private var listenerNotificaciones: ListenerRegistration? = null
    private var uidEscuchado: String? = null

    var cargando by mutableStateOf(false)
        private set
    var enviando by mutableStateOf(false)
        private set
    var mensaje by mutableStateOf<String?>(null)
        private set

    private val usuario get() = SesionManager.usuarioActual
    val esPersonal get() = usuario?.rol?.esCliente() == false

    /** Avisos dirigidos a esta persona (y no escritos por ella). */
    val recibidos: List<Notificacion>
        get() {
            val yo = usuario ?: return emptyList()
            return todos.filter { aviso ->
                if (aviso.esInterna) {
                    aviso.usuarioId == yo.id
                } else {
                    aviso.autorId != yo.id && if (esPersonal) {
                        aviso.destinatario != DestinatarioNotificacion.SOCIOS
                    } else {
                        aviso.destinatario != DestinatarioNotificacion.EMPLEADOS
                    }
                }
            }
        }

    /** Avisos que escribió esta persona. */
    val enviados: List<Notificacion>
        get() = usuario?.id?.let { yo -> todos.filter { it.autorId == yo } } ?: emptyList()

    val sinLeer: Int get() = recibidos.count { it.id !in leidas }

    fun esLeido(id: String) = id in leidas

    /** Vuelve a leer los avisos de quien tiene la sesión abierta (o limpia todo si no hay sesión). */
    fun cargar() {
        val yo = usuario
        if (yo == null) {
            listenerNotificaciones?.remove()
            listenerNotificaciones = null
            uidEscuchado = null
            todos = emptyList()
            leidas = emptySet()
            cargando = false
            mensaje = null
            return
        }
        if (uidEscuchado != yo.id) {
            listenerNotificaciones?.remove()
            listenerNotificaciones = null
            uidEscuchado = yo.id
            todos = emptyList()
            leidas = emptySet()
            mensaje = null
        }
        leidas = preferencias.getStringSet(claveLeidas(yo.id), emptySet()).orEmpty().toSet()
        escucharNotificaciones(yo.id)
        viewModelScope.launch {
            cargando = true
            try {
                val avisosPublicos = repositorio.obtener(esPersonal)
                if (usuario?.id != yo.id) return@launch
                todos = (avisosPublicos + todos.filter { it.esInterna }).sortedByDescending { it.fechaMillis }
            } catch (e: Exception) {
                if (usuario?.id != yo.id) return@launch
                Log.e(TAG, "No se pudieron cargar los avisos para ${yo.id} (personal=$esPersonal)", e)
                mensaje = when ((e as? FirebaseFirestoreException)?.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                        Textos.get("notices_permission_denied")
                    FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
                        Textos.get("notices_index_required")
                    FirebaseFirestoreException.Code.UNAVAILABLE ->
                        Textos.get("notices_connection_error")
                    else -> Textos.get("the_notices_could_not_be_loaded")
                }
            } finally {
                if (usuario?.id == yo.id) cargando = false
            }
        }
    }

    private fun escucharNotificaciones(uid: String) {
        listenerNotificaciones?.remove()
        listenerNotificaciones = FirebaseFirestore.getInstance().collection("notificaciones")
            .whereEqualTo("usuarioId", uid)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (usuario?.id != uid) return@addSnapshotListener
                    Log.e(TAG, "No se pudieron escuchar las notificaciones para $uid", error)
                    if ((error as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        mensaje = Textos.get("notices_permission_denied")
                    }
                    return@addSnapshotListener
                }
                if (usuario?.id != uid || snapshot == null) return@addSnapshotListener
                val internas = snapshot.documents.mapNotNull { documento ->
                    val titulo = documento.getString("titulo") ?: return@mapNotNull null
                    Notificacion(
                        id = "interna:${documento.id}",
                        titulo = titulo,
                        mensaje = documento.getString("mensaje").orEmpty(),
                        destinatario = if (esPersonal) DestinatarioNotificacion.EMPLEADOS else DestinatarioNotificacion.SOCIOS,
                        fechaMillis = documento.getLong("createdAt") ?: 0L,
                        usuarioId = uid,
                        tipo = documento.getString("tipo").orEmpty(),
                        leida = documento.getBoolean("leida") == true,
                        esInterna = true
                    )
                }
                todos = (todos.filterNot { it.esInterna } + internas).sortedByDescending { it.fechaMillis }
                leidas = preferencias.getStringSet(claveLeidas(uid), emptySet()).orEmpty().toSet() +
                    internas.filter { it.leida }.map { it.id }
            }
    }

    override fun onCleared() {
        listenerNotificaciones?.remove()
        super.onCleared()
    }

    fun marcarLeido(id: String) {
        val yo = usuario ?: return
        if (id in leidas) return
        leidas = leidas + id
        val aviso = todos.firstOrNull { it.id == id }
        if (aviso?.esInterna == true) {
            viewModelScope.launch {
                try {
                    FirebaseFirestore.getInstance().collection("notificaciones")
                        .document(id.removePrefix("interna:"))
                        .update(mapOf("leida" to true, "leidaEn" to System.currentTimeMillis())).await()
                    todos = todos.map { if (it.id == id) it.copy(leida = true) else it }
                } catch (e: Exception) {
                    leidas = leidas - id
                    Log.e(TAG, "No se pudo marcar como leída la notificación $id para ${yo.id}", e)
                    mensaje = Textos.get("the_change_could_not_be_saved")
                }
            }
        } else {
            preferencias.edit().putStringSet(claveLeidas(yo.id), leidas - todos.filter { it.esInterna }.map { it.id }.toSet()).apply()
        }
    }

    fun marcarTodosLeidos() {
        val yo = usuario ?: return
        val pendientes = recibidos.filterNot { it.id in leidas }
        leidas = leidas + pendientes.map { it.id }
        val internas = pendientes.filter { it.esInterna }
        preferencias.edit().putStringSet(
            claveLeidas(yo.id),
            leidas - todos.filter { it.esInterna }.map { it.id }.toSet()
        ).apply()
        if (internas.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    val batch = FirebaseFirestore.getInstance().batch()
                    internas.forEach { aviso ->
                        batch.update(
                            FirebaseFirestore.getInstance().collection("notificaciones")
                                .document(aviso.id.removePrefix("interna:")),
                            mapOf("leida" to true, "leidaEn" to System.currentTimeMillis())
                        )
                    }
                    batch.commit().await()
                    val ids = internas.map { it.id }.toSet()
                    todos = todos.map { if (it.id in ids) it.copy(leida = true) else it }
                } catch (e: Exception) {
                    leidas = leidas - internas.map { it.id }.toSet()
                    Log.e(TAG, "No se pudieron marcar las notificaciones como leídas para ${yo.id}", e)
                    mensaje = Textos.get("the_change_could_not_be_saved")
                }
            }
        }
    }

    /** Los avisos internos se dirigen al personal o a los socios, no a una audiencia abierta. */
    fun destinatariosPermitidos(): List<DestinatarioNotificacion> =
        if (usuario?.rol?.esAdministrador() == true) {
            listOf(DestinatarioNotificacion.TODOS, DestinatarioNotificacion.EMPLEADOS, DestinatarioNotificacion.SOCIOS)
        } else {
            listOf(DestinatarioNotificacion.SOCIOS)
        }

    fun publicar(
        titulo: String,
        texto: String,
        destinatario: DestinatarioNotificacion,
        imagenUrl: String = "",
        vigencia: String = "",
        alTerminar: () -> Unit
    ) {
        val yo = usuario ?: return
        if (enviando || titulo.isBlank() || texto.isBlank()) return
        viewModelScope.launch {
            enviando = true
            try {
                val nuevo = repositorio.publicar(
                    Notificacion(
                        titulo = titulo.trim(),
                        mensaje = texto.trim(),
                        destinatario = destinatario,
                        autorId = yo.id,
                        autorNombre = yo.nombre,
                        imagenUrl = imagenUrl,
                        vigencia = vigencia
                    )
                )
                todos = listOf(nuevo) + todos
                mensaje = Textos.get("post_saved")
                alTerminar()
            } catch (e: Exception) {
                mensaje = Textos.get("the_notice_could_not_be_sent")
            }
            enviando = false
        }
    }

    fun editar(
        id: String,
        titulo: String,
        texto: String,
        imagenUrl: String = "",
        vigencia: String = "",
        destinatario: DestinatarioNotificacion = DestinatarioNotificacion.TODOS,
        alTerminar: () -> Unit
    ) {
        if (enviando || titulo.isBlank() || texto.isBlank()) return
        viewModelScope.launch {
            enviando = true
            try {
                repositorio.actualizar(id, titulo.trim(), texto.trim(), imagenUrl, vigencia, destinatario)
                todos = todos.map {
                    if (it.id == id) it.copy(
                        titulo = titulo.trim(),
                        mensaje = texto.trim(),
                        imagenUrl = imagenUrl,
                        vigencia = vigencia,
                        destinatario = destinatario
                    ) else it
                }
                mensaje = Textos.get("post_updated")
                alTerminar()
            } catch (e: Exception) {
                mensaje = Textos.get("the_change_could_not_be_saved")
            }
            enviando = false
        }
    }

    fun eliminar(id: String) {
        viewModelScope.launch {
            try {
                repositorio.eliminar(id)
                todos = todos.filterNot { it.id == id }
                mensaje = Textos.get("notice_deleted")
            } catch (e: Exception) {
                mensaje = Textos.get("the_notice_could_not_be_deleted")
            }
        }
    }

    fun onMensajeMostrado() {
        mensaje = null
    }

    private fun claveLeidas(uid: String) = "leidas_$uid"

    private companion object {
        const val TAG = "AvisosViewModel"
    }
}
