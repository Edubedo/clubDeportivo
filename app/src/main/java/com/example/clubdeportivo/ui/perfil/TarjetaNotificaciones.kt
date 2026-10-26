package com.example.clubdeportivo.ui.perfil

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.clubdeportivo.util.tr
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.clubdeportivo.data.SesionManager
import com.example.clubdeportivo.notificaciones.CategoriaNotificacion
import com.example.clubdeportivo.notificaciones.Notificador
import com.example.clubdeportivo.notificaciones.PreferenciasNotificaciones
import com.example.clubdeportivo.notificaciones.TipoUsuario
import com.example.clubdeportivo.ui.components.BotonSecundario
import com.example.clubdeportivo.ui.components.TarjetaClub
import com.example.clubdeportivo.ui.theme.Marca
import com.example.clubdeportivo.ui.theme.SobreMarca
import com.example.clubdeportivo.ui.theme.TextoPrincipal
import com.example.clubdeportivo.ui.theme.TextoSecundario
import com.example.clubdeportivo.ui.theme.Alerta
import com.example.clubdeportivo.ui.theme.Borde

/** Perfil → Notificaciones: un interruptor por cada tipo de recordatorio que le toca a esta persona según su rol. */
@Composable
fun TarjetaNotificaciones() {
    val contexto = LocalContext.current
    val rol = SesionManager.usuarioActual?.rol ?: return
    val categorias = CategoriaNotificacion.de(TipoUsuario.de(rol))
    val estados = remember(categorias) {
        mutableStateMapOf<CategoriaNotificacion, Boolean>().apply { categorias.forEach { put(it, PreferenciasNotificaciones.activa(contexto, it)) } }
    }
    // Se vuelve a mirar al regresar de los ajustes del teléfono, por si la persona activó las notificaciones allá.
    var revision by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        revision++
        onPauseOrDispose { }
    }
    val permitidas = remember(revision) { Notificador.puedeNotificar(contexto) }

    TarjetaClub(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = tr("notif_perfil_titulo"),
                style = MaterialTheme.typography.titleMedium,
                color = TextoPrincipal
            )
            Text(
                text = tr("notif_perfil_descripcion"),
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            if (!permitidas) {
                Text(
                    text = tr("notif_perfil_desactivadas"),
                    style = MaterialTheme.typography.bodySmall,
                    color = Alerta,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                BotonSecundario(
                    texto = tr("notif_perfil_abrir_ajustes"),
                    conReflejo = false,
                    onClick = {
                        contexto.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            categorias.forEachIndexed { indice, categoria ->
                if (indice > 0) androidx.compose.material3.HorizontalDivider(color = Borde)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tr(categoria.nombre), style = MaterialTheme.typography.titleSmall, color = TextoPrincipal)
                        Text(tr(categoria.descripcion), style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                    }
                    Switch(
                        checked = estados[categoria] == true,
                        onCheckedChange = {
                            estados[categoria] = it
                            PreferenciasNotificaciones.activar(contexto, categoria, it)
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = Marca, checkedThumbColor = SobreMarca)
                    )
                }
            }
        }
    }
}
