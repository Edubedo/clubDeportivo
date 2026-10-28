package com.example.clubdeportivo.ui.admin.reportes

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubdeportivo.R
import com.example.clubdeportivo.data.AppContainer
import com.example.clubdeportivo.util.Fechas
import com.example.clubdeportivo.util.Textos
import com.example.clubdeportivo.util.reportes.DatosReporte
import com.example.clubdeportivo.util.reportes.EscritorXlsx
import com.example.clubdeportivo.util.reportes.GeneradorReporte
import com.example.clubdeportivo.util.reportes.PeriodoReporte
import com.example.clubdeportivo.util.reportes.Reporte
import com.example.clubdeportivo.util.reportes.SeccionReporte
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FormatoReporte(val extension: String, val mime: String) {
    PDF("pdf", "application/pdf"),
    EXCEL("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
}

/** Archivo ya generado, listo para compartir, abrir o guardar. */
data class ArchivoReporte(val archivo: File, val uri: android.net.Uri, val formato: FormatoReporte)

/**
 * Reportes del administrador. Lee una sola vez todos los datos del club y arma el reporte con el periodo y las
 * secciones elegidas; al exportar escribe el PDF o el Excel en la caché de la app para compartirlo.
 */
class ReportesViewModel : ViewModel() {
    var periodo by mutableStateOf(PeriodoReporte.ESTE_MES)
        private set
    var secciones by mutableStateOf(SeccionReporte.entries.toSet())
        private set
    var cargando by mutableStateOf(true)
        private set
    /** Secciones que no se pudieron leer (sin red, sin permiso): se avisa y no se exportan. */
    var seccionesConError by mutableStateOf(emptySet<SeccionReporte>())
        private set
    var reporte by mutableStateOf<Reporte?>(null)
        private set
    var exportando by mutableStateOf<FormatoReporte?>(null)
        private set
    var archivoListo by mutableStateOf<ArchivoReporte?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var datos = DatosReporte()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            cargando = true
            val historico = PeriodoReporte.TODO.rango(Fechas.hoy())
            coroutineScope {
                fun <T> leer(bloque: suspend () -> T) = async {
                    runCatching { bloque() }.onFailure { Log.w("Reportes", "No se pudo leer una sección", it) }.getOrNull()
                }
                val pagos = leer { AppContainer.pagosRepository.obtenerDesde(historico.first) }
                val membresias = leer { AppContainer.gestionMembresiasRepository.obtenerTodas() }
                val reservas = leer { AppContainer.reservaRepository.obtenerReservasEntre(historico.first, historico.second) }
                val asistencias = leer { AppContainer.reservaRepository.obtenerAsistencias() }
                val areas = leer { AppContainer.areaRepository.obtenerAreas() }
                val inventario = leer { AppContainer.inventarioRepository.obtenerArticulos() }
                val personal = leer { AppContainer.personalRepository.obtenerPersonal() }
                val torneos = leer { AppContainer.torneoRepository.obtenerTorneos() }
                datos = DatosReporte(
                    pagos = pagos.await(),
                    membresias = membresias.await(),
                    reservas = reservas.await(),
                    asistencias = asistencias.await().orEmpty(),
                    areas = areas.await(),
                    inventario = inventario.await(),
                    personal = personal.await(),
                    torneos = torneos.await()
                )
            }
            seccionesConError = buildSet {
                if (datos.pagos == null) add(SeccionReporte.INGRESOS)
                if (datos.membresias == null) add(SeccionReporte.MEMBRESIAS)
                if (datos.reservas == null) add(SeccionReporte.RESERVAS)
                if (datos.areas == null) add(SeccionReporte.AREAS)
                if (datos.inventario == null) add(SeccionReporte.INVENTARIO)
                if (datos.personal == null) add(SeccionReporte.PERSONAL)
                if (datos.torneos == null) add(SeccionReporte.TORNEOS)
            }
            cargando = false
            regenerar()
        }
    }

    fun elegirPeriodo(nuevo: PeriodoReporte) {
        periodo = nuevo
        regenerar()
    }

    fun alternarSeccion(seccion: SeccionReporte) {
        secciones = if (seccion in secciones) secciones - seccion else secciones + seccion
        regenerar()
    }

    private fun regenerar() {
        val elegidos = DatosReporte(
            pagos = datos.pagos.takeIf { SeccionReporte.INGRESOS in secciones },
            membresias = datos.membresias.takeIf { SeccionReporte.MEMBRESIAS in secciones },
            reservas = datos.reservas.takeIf { SeccionReporte.RESERVAS in secciones },
            asistencias = datos.asistencias,
            areas = datos.areas.takeIf { SeccionReporte.AREAS in secciones },
            inventario = datos.inventario.takeIf { SeccionReporte.INVENTARIO in secciones },
            personal = datos.personal.takeIf { SeccionReporte.PERSONAL in secciones },
            torneos = datos.torneos.takeIf { SeccionReporte.TORNEOS in secciones }
        )
        val ahora = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        reporte = GeneradorReporte.generar(elegidos, periodo, Fechas.hoy(), Textos.get("rep_generado", ahora))
    }

    fun exportar(contexto: Context, formato: FormatoReporte) {
        val actual = reporte ?: return
        if (exportando != null) return
        viewModelScope.launch {
            exportando = formato
            try {
                val archivo = withContext(Dispatchers.IO) {
                    val carpeta = File(contexto.cacheDir, "reportes").apply { mkdirs() }
                    // Solo se guarda el último de cada formato: no se acumulan archivos viejos en la caché.
                    carpeta.listFiles()?.filter { it.extension == formato.extension }?.forEach { it.delete() }
                    val sello = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
                    val archivo = File(carpeta, "${Textos.get("rep_nombre_archivo")}_${periodo.name.lowercase()}_$sello.${formato.extension}")
                    archivo.outputStream().use { salida ->
                        when (formato) {
                            FormatoReporte.PDF -> EscritorPdf(contexto.getString(R.string.app_name)).escribir(actual, salida)
                            FormatoReporte.EXCEL -> EscritorXlsx.escribir(actual, salida)
                        }
                    }
                    archivo
                }
                val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", archivo)
                archivoListo = ArchivoReporte(archivo, uri, formato)
            } catch (e: Exception) {
                Log.e("Reportes", "No se pudo generar el reporte", e)
                error = Textos.get("rep_error_exportar")
            } finally {
                exportando = null
            }
        }
    }

    /** Copia el archivo generado al destino que eligió la persona (Descargas, Drive...). */
    fun guardarEn(contexto: Context, destino: android.net.Uri) {
        val listo = archivoListo ?: return
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    contexto.contentResolver.openOutputStream(destino)?.use { salida ->
                        listo.archivo.inputStream().use { it.copyTo(salida) }
                    } ?: error("sin salida")
                }.isSuccess
            }
            error = if (ok) null else Textos.get("rep_error_guardar")
            if (ok) avisoGuardado = true
        }
    }

    var avisoGuardado by mutableStateOf(false)
        private set

    fun cerrarArchivo() { archivoListo = null }
    fun limpiarAvisos() { error = null; avisoGuardado = false }
}
