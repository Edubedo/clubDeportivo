package com.example.clubdeportivo.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Paleta única de la app: UN naranja deportivo de marca (quemado, no neón), grises cálidos para fondos, texto
 * y bordes, blanco para tarjetas, y tres colores de estado (verde, amarillo, rojo) que solo se usan para
 * comunicar un estado. No agregues más tonos: si necesitas algo nuevo, elige uno de estos.
 */

// Marca
val Marca = Color(0xFFCC4A0C) // contraste 4.6:1 con texto blanco
val MarcaSuave = Color(0xFFFDEBDD) // fondo de elementos seleccionados, avatares e iconos sobre blanco
val SobreMarca = Color(0xFFFFFFFF)

// Neutros (grises cálidos: combinan con el naranja mejor que los grises azulados)
val FondoApp = Color(0xFFF5F5F4)
val Superficie = Color(0xFFFFFFFF)
val Borde = Color(0xFFE7E5E4)
val BordeCampo = Color(0xFFD6D3D1)
val TextoPrincipal = Color(0xFF1C1917)
val TextoSecundario = Color(0xFF57534E)
val TextoTenue = Color(0xFF78716C)

// Estados. El rojo de peligro es claramente distinto del naranja de marca: los errores se reconocen por su color Y por
// su icono/mensaje, y el amarillo de alerta se separa del naranja de marca para que "pendiente" nunca parezca un botón.
val Exito = Color(0xFF15803D)
val ExitoSuave = Color(0xFFDCFCE7)
val Alerta = Color(0xFF854D0E)
val AlertaSuave = Color(0xFFFEF3C7)
val Peligro = Color(0xFFDC2626)
val PeligroSuave = Color(0xFFFEE2E2)
