package com.example.clubdeportivo.notificaciones

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** Tarea en segundo plano que revisa si hay algo que recordarle a la persona. */
class RecordatoriosWorker(contexto: Context, parametros: WorkerParameters) : CoroutineWorker(contexto, parametros) {
    override suspend fun doWork(): Result {
        // Un fallo (sin red, sesión cerrada...) no se reintenta con espera: la próxima revisión periódica lo vuelve a intentar.
        runCatching { MotorRecordatorios.ejecutar(applicationContext) }
        return Result.success()
    }
}

object ProgramadorRecordatorios {
    private const val PERIODICO = "recordatorios_periodico"
    private const val AHORA = "recordatorios_ahora"
    private val conRed = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Deja programada la revisión cada 15 minutos (el mínimo que permite Android). Si ya existe, no la duplica. */
    fun programar(contexto: Context) {
        val peticion = PeriodicWorkRequestBuilder<RecordatoriosWorker>(15, TimeUnit.MINUTES).setConstraints(conRed).build()
        WorkManager.getInstance(contexto).enqueueUniquePeriodicWork(PERIODICO, ExistingPeriodicWorkPolicy.KEEP, peticion)
    }

    /** Revisa ya mismo (al abrir la app o al iniciar sesión) sin esperar a la próxima revisión periódica. */
    fun revisarAhora(contexto: Context) {
        val peticion = OneTimeWorkRequestBuilder<RecordatoriosWorker>().setConstraints(conRed).build()
        WorkManager.getInstance(contexto).enqueueUniqueWork(AHORA, ExistingWorkPolicy.REPLACE, peticion)
    }
}
