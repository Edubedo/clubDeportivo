package com.example.clubdeportivo.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Paleta de la app: azules como identidad (marca, fondos, campos) y colores cálidos solo para comunicar:
 * Ámbar como acento y destacados, Naranja para alertas/pendientes, Rojo para peligro y Verde para éxito.
 * No agregues más tonos: si necesitas algo nuevo, elige uno de estos.
 */

// Paleta azul base (7 colores)
private val AzulNoche = Color(0xFF001D39)
private val AzulProfundo = Color(0xFF0A4174)
private val AzulAcero = Color(0xFF49769F)
private val VerdeAzulado = Color(0xFF4E8EA2)
private val AzulGrisaceo = Color(0xFF6EA2B3)
private val AzulCielo = Color(0xFF7BBDE8)
private val AzulHielo = Color(0xFFBDD8E9)

// Marca
val Marca = AzulProfundo // contraste con texto blanco: ~9:1
val MarcaOscura = AzulNoche // presionado, avisos emergentes, texto sobre fondos suaves
val MarcaSuave = Color(0xFFE4EFF7) // avatares e iconos sobre blanco
val SobreMarca = Color(0xFFFFFFFF)

// Azules de apoyo
val Trigo = AzulCielo // elemento seleccionado (chips, navegación), con texto AzulNoche
val Perla = AzulHielo // campos y filas dentro de tarjetas
val Acero = AzulAcero // detalles sutiles, texto de énfasis azul (4.6:1 sobre blanco)
val Turquesa = VerdeAzulado // iconos secundarios, gráficas

// Acentos cálidos
val Caledula = Color(0xFFF5B82E) // Ámbar. ACENTO: iconos activos, destacados, insignias. Con texto OSCURO (10:1)
val CaledulaSuave = Color(0xFFFFF1CC) // fondo de datos destacados
val Citrino = Color(0xFFB45309) // Naranja: alertas y texto de énfasis (5:1 sobre blanco)

// Neutros (azulados)
val FondoApp = Color(0xFFEEF5FA)
val Superficie = Color(0xFFFFFFFF)
val Borde = Color(0xFFD3E4F0)
val BordeCampo = AzulGrisaceo
val TextoPrincipal = AzulNoche
val TextoSecundario = Color(0xFF2F4A63)
val TextoTenue = Color(0xFF3F5B75)

// Estados: cada uno tiene su color, no se reutilizan para decoración
val Exito = Color(0xFF15803D)
val ExitoSuave = Color(0xFFDCFCE7)
val Alerta = Citrino
val AlertaSuave = Color(0xFFFEF3C7)
val Peligro = Color(0xFFDC2626)
val PeligroSuave = Color(0xFFFEE2E2)