package com.example.clubdeportivo.util.reportes

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Escribe un [Reporte] como libro de Excel (.xlsx) sin librerías externas: un .xlsx es un zip con unos cuantos XML.
 * La primera hoja es el resumen y después va una hoja por tabla, con el encabezado fijo y filtros para ordenar y
 * buscar. Los montos van como números (con formato de pesos) para que se puedan sumar en Excel.
 */
object EscritorXlsx {
    private const val ESTILO_ENCABEZADO = 1
    private const val ESTILO_DINERO = 2
    private const val ESTILO_NEGRITA = 3
    private const val ESTILO_TITULO = 4

    fun escribir(reporte: Reporte, salida: OutputStream) {
        val hojas = mutableListOf<Pair<String, String>>()
        val nombres = mutableSetOf<String>()
        hojas += nombreUnico(com.example.clubdeportivo.util.Textos.get("rep_hoja_resumen"), nombres) to hojaResumen(reporte)
        reporte.tablas.forEach { hojas += nombreUnico(it.titulo, nombres) to hojaTabla(it) }

        ZipOutputStream(salida).use { zip ->
            fun archivo(ruta: String, contenido: String) {
                zip.putNextEntry(ZipEntry(ruta))
                zip.write(contenido.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            archivo("[Content_Types].xml", tiposDeContenido(hojas.size))
            archivo("_rels/.rels", RELS_RAIZ)
            archivo("xl/workbook.xml", libro(hojas.map { it.first }))
            archivo("xl/_rels/workbook.xml.rels", relsLibro(hojas.size))
            archivo("xl/styles.xml", ESTILOS)
            hojas.forEachIndexed { i, (_, xml) -> archivo("xl/worksheets/sheet${i + 1}.xml", xml) }
        }
    }

    // ---------------------------------------------------------------- Hojas

    private fun hojaResumen(reporte: Reporte): String {
        val filas = mutableListOf<List<Pair<Celda, Int>>>()
        filas += listOf(texto(reporte.titulo) to ESTILO_TITULO)
        filas += listOf(texto(reporte.periodo) to 0)
        filas += listOf(texto(reporte.generado) to 0)
        reporte.resumen.forEach { bloque ->
            filas += emptyList<Pair<Celda, Int>>()
            filas += listOf(texto(bloque.titulo) to ESTILO_ENCABEZADO, texto("") to ESTILO_ENCABEZADO)
            bloque.datos.forEach { (concepto, valor) -> filas += listOf(texto(concepto) to ESTILO_NEGRITA, texto(valor) to 0) }
        }
        return hoja(filas, anchos = listOf(42.0, 28.0), congelarEncabezado = false, filtro = null)
    }

    private fun hojaTabla(tabla: Tabla): String {
        val filas = mutableListOf<List<Pair<Celda, Int>>>()
        filas += tabla.columnas.map { texto(it) to ESTILO_ENCABEZADO }
        tabla.filas.forEach { fila ->
            filas += fila.map { celda -> celda to if (celda is Celda.Numero && celda.dinero) ESTILO_DINERO else 0 }
        }
        // Ancho según el texto más largo de cada columna (sin pasarse, para que no queden columnas enormes).
        val anchos = tabla.columnas.indices.map { col ->
            val maximo = (listOf(tabla.columnas[col]) + tabla.filas.mapNotNull { it.getOrNull(col)?.comoTexto() }).maxOf { it.length }
            (maximo + 3).coerceIn(8, 48).toDouble()
        }
        val ultima = "${columna(tabla.columnas.size - 1)}${maxOf(1, tabla.filas.size + 1)}"
        return hoja(filas, anchos, congelarEncabezado = true, filtro = "A1:$ultima")
    }

    private fun hoja(filas: List<List<Pair<Celda, Int>>>, anchos: List<Double>, congelarEncabezado: Boolean, filtro: String?): String =
        buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
            if (congelarEncabezado) {
                append("""<sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>""")
            }
            append("<cols>")
            anchos.forEachIndexed { i, ancho -> append("""<col min="${i + 1}" max="${i + 1}" width="$ancho" customWidth="1"/>""") }
            append("</cols><sheetData>")
            filas.forEachIndexed { f, fila ->
                append("""<row r="${f + 1}">""")
                fila.forEachIndexed { c, (celda, estilo) ->
                    val ref = "${columna(c)}${f + 1}"
                    val s = if (estilo != 0) """ s="$estilo"""" else ""
                    when (celda) {
                        is Celda.Numero -> append("""<c r="$ref"$s><v>${celda.valor}</v></c>""")
                        is Celda.Texto -> append("""<c r="$ref"$s t="inlineStr"><is><t xml:space="preserve">${escapar(celda.valor)}</t></is></c>""")
                    }
                }
                append("</row>")
            }
            append("</sheetData>")
            if (filtro != null) append("""<autoFilter ref="$filtro"/>""")
            append("</worksheet>")
        }

    // ---------------------------------------------------------------- Partes fijas del libro

    private fun libro(nombres: List<String>) = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>""")
        nombres.forEachIndexed { i, nombre -> append("""<sheet name="${escapar(nombre)}" sheetId="${i + 1}" r:id="rId${i + 1}"/>""") }
        append("</sheets></workbook>")
    }

    private fun relsLibro(hojas: Int) = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        (1..hojas).forEach {
            append("""<Relationship Id="rId$it" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$it.xml"/>""")
        }
        append("""<Relationship Id="rId${hojas + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
        append("</Relationships>")
    }

    private fun tiposDeContenido(hojas: Int) = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
        (1..hojas).forEach {
            append("""<Override PartName="/xl/worksheets/sheet$it.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
        }
        append("</Types>")
    }

    private const val RELS_RAIZ = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""

    /** 0 normal · 1 encabezado (negritas blancas sobre azul marino) · 2 pesos · 3 negritas · 4 título. */
    private const val ESTILOS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><numFmts count="1"><numFmt numFmtId="164" formatCode="&quot;$&quot;#,##0.00"/></numFmts><fonts count="4"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font><font><b/><sz val="15"/><color rgb="FF1F3A5F"/><name val="Calibri"/></font></fonts><fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FF1F3A5F"/><bgColor indexed="64"/></patternFill></fill></fills><borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="5"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="2" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/><xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/><xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/><xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0" applyFont="1"/></cellXfs><cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles></styleSheet>"""

    // ---------------------------------------------------------------- Utilidades

    /** 0 -> "A", 25 -> "Z", 26 -> "AA". */
    fun columna(indice: Int): String {
        var n = indice + 1
        val letras = StringBuilder()
        while (n > 0) {
            val resto = (n - 1) % 26
            letras.insert(0, 'A' + resto)
            n = (n - 1) / 26
        }
        return letras.toString()
    }

    /** Excel no acepta en el nombre de una hoja \ / ? * [ ] : ni más de 31 caracteres, y no se pueden repetir. */
    fun nombreUnico(nombre: String, usados: MutableSet<String>): String {
        val base = nombre.replace(Regex("""[\\/?*\[\]:]"""), " ").trim().ifBlank { "Hoja" }.take(31)
        var candidato = base
        var n = 2
        while (candidato.lowercase() in usados) {
            val sufijo = " ($n)"
            candidato = base.take(31 - sufijo.length) + sufijo
            n++
        }
        usados += candidato.lowercase()
        return candidato
    }

    /** Escapa para XML y quita los caracteres de control que Excel no acepta. */
    private fun escapar(texto: String): String = buildString {
        texto.forEach { c ->
            when {
                c == '&' -> append("&amp;")
                c == '<' -> append("&lt;")
                c == '>' -> append("&gt;")
                c == '"' -> append("&quot;")
                c < ' ' && c != '\t' && c != '\n' && c != '\r' -> Unit
                else -> append(c)
            }
        }
    }
}
