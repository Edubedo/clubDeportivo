package com.example.clubdeportivo.ui.avisos

import android.app.Application
import android.content.Context
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
import kotlinx.coroutines.launch
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
                aviso.autorId != yo.id && if (esPersonal) {
                    aviso.destinatario != DestinatarioNotificacion.SOCIOS
                } else {
                    aviso.destinatario != DestinatarioNotificacion.EMPLEADOS
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
            todos = emptyList()
            leidas = emptySet()
            return
        }
        leidas = preferencias.getStringSet(claveLeidas(yo.id), emptySet()).orEmpty().toSet()
        viewModelScope.launch {
            cargando = true
            try {
                todos = repositorio.obtener(esPersonal)
            } catch (e: Exception) {
                mensaje = Textos.get("the_notices_could_not_be_loaded")
            }
            cargando = false
        }
    }

    fun marcarLeido(id: String) {
        val yo = usuario ?: return
        if (id in leidas) return
        leidas = leidas + id
        preferencias.edit().putStringSet(claveLeidas(yo.id), leidas).apply()
    }

    fun marcarTodosLeidos() {
        val yo = usuario ?: return
        leidas = leidas + recibidos.map { it.id }
        preferencias.edit().putStringSet(claveLeidas(yo.id), leidas).apply()
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
}
