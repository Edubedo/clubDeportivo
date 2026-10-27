package com.example.clubdeportivo.ui.admin.reportes

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import com.example.clubdeportivo.util.Textos
import com.example.clubdeportivo.util.reportes.BloqueResumen
import com.example.clubdeportivo.util.reportes.Celda
import com.example.clubdeportivo.util.reportes.Reporte
import com.example.clubdeportivo.util.reportes.Tabla
import com.example.clubdeportivo.util.reportes.comoTexto
import java.io.OutputStream

/**
 * Escribe un [Reporte] como PDF con las herramientas de Android (PdfDocument), en hojas A4 horizontales: portada con
 * el resumen en tarjetas y después cada tabla, que continúa en la hoja siguiente repitiendo su encabezado.
 */
class EscritorPdf(private val nombreClub: String) {
    private val ancho = 842
    private val alto = 595
    private val margen = 32f
    private val anchoUtil = ancho - margen * 2
    private val pie = alto - 24f

    private val azul = Color.rgb(0x1F, 0x3A, 0x5F)
    private val gris = Color.rgb(0x5B, 0x66, 0x75)
    private val linea = Color.rgb(0xD9, 0xDE, 0xE5)
    private val zebra = Color.rgb(0xF1, 0xF4, 0xF8)

    private fun pincel(tamano: Float, color: Int = Color.BLACK, negrita: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = tamano
        this.color = color
        typeface = if (negrita) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private val texto = pincel(8f)
    private val textoNegrita = pincel(8f, negrita = true)
    private val textoEncabezado = pincel(8f, Color.WHITE, negrita = true)
    private val textoGris = pincel(8f, gris)
    private val relleno = Paint().apply { style = Paint.Style.FILL }

    private lateinit var documento: PdfDocument
    private var pagina: PdfDocument.Page? = null
    private var numeroPagina = 0
    private var y = 0f
    private lateinit var reporte: Reporte

    private val lienzo: Canvas get() = pagina!!.canvas

    fun escribir(reporte: Reporte, salida: OutputStream) {
        this.reporte = reporte
        documento = PdfDocument()
        try {
            nuevaPagina(portada = true)
            dibujarResumen(reporte.resumen)
            reporte.tablas.forEach { dibujarTabla(it) }
            terminarPagina()
            documento.writeTo(salida)
        } finally {
            documento.close()
        }
    }

    // ---------------------------------------------------------------- Páginas

    private fun nuevaPagina(portada: Boolean = false) {
        terminarPagina()
        numeroPagina++
        pagina = documento.startPage(PdfDocument.PageInfo.Builder(ancho, alto, numeroPagina).create())
        if (portada) {
            relleno.color = azul
            lienzo.drawRect(0f, 0f, ancho.toFloat(), 74f, relleno)
            lienzo.drawText(nombreClub, margen, 30f, pincel(11f, Color.rgb(0xC9, 0xD6, 0xE8), negrita = true))
            lienzo.drawText(reporte.titulo, margen, 56f, pincel(20f, Color.WHITE, negrita = true))
            lienzo.drawText(reporte.periodo, margen, 94f, pincel(10f, Color.BLACK, negrita = true))
            lienzo.drawText(reporte.generado, margen, 108f, pincel(9f, gris))
            y = 128f
        } else {
            lienzo.drawText("$nombreClub · ${reporte.titulo}", margen, 26f, textoGris)
            lienzo.drawText(reporte.periodo, ancho - margen - textoGris.measureText(reporte.periodo), 26f, textoGris)
            relleno.color = linea
            lienzo.drawRect(margen, 32f, ancho - margen, 33f, relleno)
            y = 50f
        }
    }

    private fun terminarPagina() {
        val actual = pagina ?: return
        val numero = Textos.get("rep_pagina", numeroPagina)
        actual.canvas.drawText(numero, ancho - margen - textoGris.measureText(numero), pie + 8f, textoGris)
        documento.finishPage(actual)
        pagina = null
    }

    /** Si lo que sigue ([altura]) ya no cabe, se pasa a una hoja nueva. Devuelve true si cambió de hoja. */
    private fun asegurarEspacio(altura: Float): Boolean {
        if (y + altura <= pie - 8f) return false
        nuevaPagina()
        return true
    }

    private fun recortar(valor: String, pincel: TextPaint, anchoMax: Float): String =
        TextUtils.ellipsize(valor, pincel, anchoMax, TextUtils.TruncateAt.END).toString()

    // ---------------------------------------------------------------- Resumen

    private fun dibujarResumen(bloques: List<BloqueResumen>) {
        if (bloques.isEmpty()) return
        titulo(Textos.get("rep_hoja_resumen"))
        val columnas = 3
        val separacion = 12f
        val anchoTarjeta = (anchoUtil - separacion * (columnas - 1)) / columnas
        val alturaFila = 13f
        bloques.chunked(columnas).forEach { fila ->
            val alturaTarjeta = 24f + (fila.maxOf { it.datos.size }) * alturaFila + 6f
            asegurarEspacio(alturaTarjeta)
            fila.forEachIndexed { i, bloque ->
                val x = margen + i * (anchoTarjeta + separacion)
                relleno.color = zebra
                lienzo.drawRoundRect(x, y, x + anchoTarjeta, y + alturaTarjeta, 6f, 6f, relleno)
                relleno.color = azul
                lienzo.drawRoundRect(x, y, x + 4f, y + alturaTarjeta, 2f, 2f, relleno)
                lienzo.drawText(bloque.titulo, x + 12f, y + 16f, pincel(9.5f, azul, negrita = true))
                bloque.datos.forEachIndexed { n, (concepto, valor) ->
                    val base = y + 32f + n * alturaFila
                    val anchoValor = textoNegrita.measureText(valor)
                    lienzo.drawText(recortar(concepto, texto, anchoTarjeta - anchoValor - 32f), x + 12f, base, texto)
                    lienzo.drawText(valor, x + anchoTarjeta - 10f - anchoValor, base, textoNegrita)
                }
            }
            y += alturaTarjeta + separacion
        }
        y += 6f
    }

    // ---------------------------------------------------------------- Tablas

    private fun titulo(valor: String) {
        asegurarEspacio(60f)
        lienzo.drawText(valor, margen, y + 12f, pincel(13f, azul, negrita = true))
        y += 22f
    }

    private fun dibujarTabla(tabla: Tabla) {
        // Si ya no caben el título, el encabezado y unas filas, la tabla empieza en la hoja siguiente.
        asegurarEspacio(90f)
        titulo("${tabla.titulo} (${tabla.filas.size})")
        val total = tabla.pesos.sum()
        val anchos = tabla.pesos.map { anchoUtil * it / total }
        val alturaFila = 15f

        fun encabezado() {
            relleno.color = azul
            lienzo.drawRect(margen, y, margen + anchoUtil, y + alturaFila + 2f, relleno)
            var x = margen
            tabla.columnas.forEachIndexed { i, columna ->
                lienzo.drawText(recortar(columna, textoEncabezado, anchos[i] - 8f), x + 4f, y + 11f, textoEncabezado)
                x += anchos[i]
            }
            y += alturaFila + 2f
        }

        encabezado()
        if (tabla.filas.isEmpty()) {
            lienzo.drawText(Textos.get("rep_sin_registros"), margen + 4f, y + 12f, textoGris)
            y += alturaFila + 14f
            return
        }
        tabla.filas.forEachIndexed { n, fila ->
            if (asegurarEspacio(alturaFila)) {
                lienzo.drawText(Textos.get("rep_continua", tabla.titulo), margen, y + 10f, textoGris)
                y += 16f
                encabezado()
            }
            if (n % 2 == 1) {
                relleno.color = zebra
                lienzo.drawRect(margen, y, margen + anchoUtil, y + alturaFila, relleno)
            }
            var x = margen
            fila.forEachIndexed { i, celda ->
                val anchoCelda = anchos.getOrElse(i) { 0f }
                val valor = recortar(celda.comoTexto(), texto, anchoCelda - 8f)
                // Los números se alinean a la derecha para que se lean como columna.
                val xTexto = if (celda is Celda.Numero) x + anchoCelda - 4f - texto.measureText(valor) else x + 4f
                lienzo.drawText(valor, xTexto, y + 10.5f, texto)
                x += anchoCelda
            }
            y += alturaFila
        }
        relleno.color = linea
        lienzo.drawRect(margen, y, margen + anchoUtil, y + 0.8f, relleno)
        y += 18f
    }
}
