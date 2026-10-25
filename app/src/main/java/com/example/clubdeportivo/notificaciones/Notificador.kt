package com.example.clubdeportivo.notificaciones

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.clubdeportivo.MainActivity
import com.example.clubdeportivo.R
import com.example.clubdeportivo.util.Textos

/** Crea los canales de notificación y muestra los recordatorios. */
object Notificador {
    const val EXTRA_RUTA = "ruta_notificacion"

    /** Crea (o actualiza el nombre de) un canal por categoría; es seguro llamarlo muchas veces. */
    fun crearCanales(contexto: Context) {
        val manager = NotificationManagerCompat.from(contexto)
        CategoriaNotificacion.entries.forEach { categoria ->
            manager.createNotificationChannel(
                NotificationChannelCompat.Builder(categoria.canalId, categoria.importancia)
                    .setName(Textos.get(categoria.nombre))
                    .setDescription(Textos.get(categoria.descripcion))
                    .build()
            )
        }
    }

    fun puedeNotificar(contexto: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(contexto).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission") // puedeNotificar() ya comprobó el permiso
    fun mostrar(contexto: Context, recordatorio: Recordatorio) {
        if (!puedeNotificar(contexto)) return
        val abrir = Intent(contexto, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            recordatorio.ruta?.let { putExtra(EXTRA_RUTA, it) }
        }
        val id = recordatorio.claves.first().hashCode()
        val toque = PendingIntent.getActivity(contexto, id, abrir, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notificacion = NotificationCompat.Builder(contexto, recordatorio.categoria.canalId)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setColor(ContextCompat.getColor(contexto, R.color.primary_navy))
            .setContentTitle(recordatorio.titulo)
            .setContentText(recordatorio.mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(recordatorio.mensaje))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(toque)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(contexto).notify(id, notificacion)
    }
}
