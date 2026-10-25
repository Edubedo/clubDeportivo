package com.example.clubdeportivo.data.model

import com.example.clubdeportivo.R
import com.example.clubdeportivo.util.Textos

/*
 * Algunos datos se guardan en Firestore con su texto en español ("Titular", "Matutino"...) y así se comparan,
 * por eso no se traducen al guardarlos. Estas funciones solo cambian cómo se MUESTRAN; lo que no conocen
 * (por ejemplo un parentesco escrito a mano, como "Cónyuge") se devuelve tal cual.
 */

fun etiquetaParentesco(valor: String): String = when (valor) {
    MiembroClub.PARENTESCO_TITULAR -> Textos.get(R.string.parentesco_titular)
    "Integrante" -> Textos.get(R.string.parentesco_integrante)
    else -> valor
}

fun etiquetaTurno(valor: String): String = when (valor) {
    "Matutino" -> Textos.get(R.string.turno_matutino)
    "Vespertino" -> Textos.get(R.string.turno_vespertino)
    else -> valor
}

fun etiquetaTipoPersonal(valor: String): String = when (valor) {
    "Limpieza" -> Textos.get(R.string.tipo_personal_limpieza)
    "Ayudante de área" -> Textos.get(R.string.tipo_personal_ayudante)
    "Administrador de área" -> Textos.get(R.string.tipo_personal_administrador)
    else -> valor
}
