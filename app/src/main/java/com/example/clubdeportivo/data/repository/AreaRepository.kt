package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.data.model.DisponibilidadArea
import kotlinx.coroutines.delay

interface AreaRepository {
    suspend fun obtenerAreas(): List<Area>
    suspend fun obtenerAreaPorId(id: String): Area?
    suspend fun crearArea(nombre: String, tipo: String, capacidad: Int, emoji: String): Area
    suspend fun actualizarArea(area: Area): Area
    suspend fun eliminarArea(id: String)
}

/**
 * Datos de prueba en memoria — ya no se usa (ver FirebaseAreaRepository), se deja como
 * referencia simple del patrón repositorio.
 *
 * Inventario inicial del club (2 áreas de fútbol, básquetbol, natación y tenis). El club puede agregar áreas nuevas después: "tipo" es texto
 * libre y no hay ningún límite de cuántas áreas puede haber, así que un
 * futuro panel de administración solo necesita agregar filas aquí (o en la
 * base de datos real) sin tocar el resto de la app.
 */
class FakeAreaRepository : AreaRepository {

    private var areas = listOf(
        Area("1", "Cancha de fútbol 1", "Fútbol", 22, DisponibilidadArea.DISPONIBLE, permiteExternos = true),
        Area("2", "Cancha de fútbol 2", "Fútbol", 22, DisponibilidadArea.OCUPADA, permiteExternos = true),
        Area("3", "Cancha de básquetbol 1", "Básquetbol", 10, DisponibilidadArea.DISPONIBLE, permiteExternos = true),
        Area("4", "Cancha de básquetbol 2", "Básquetbol", 10, DisponibilidadArea.DISPONIBLE),
        Area("5", "Alberca 1", "Natación", 40, DisponibilidadArea.DISPONIBLE),
        Area("6", "Alberca 2", "Natación", 40, DisponibilidadArea.MANTENIMIENTO),
        Area("7", "Cancha de tenis 1", "Tenis", 4, DisponibilidadArea.OCUPADA, permiteExternos = true),
        Area("8", "Cancha de tenis 2", "Tenis", 4, DisponibilidadArea.DISPONIBLE, permiteExternos = true)
    )

    override suspend fun obtenerAreas(): List<Area> {
        delay(400)
        return areas
    }

    override suspend fun obtenerAreaPorId(id: String): Area? {
        delay(200)
        return areas.find { it.id == id }
    }

    override suspend fun crearArea(nombre: String, tipo: String, capacidad: Int, emoji: String): Area {
        val area = Area((areas.size + 1).toString(), nombre, tipo, capacidad, DisponibilidadArea.DISPONIBLE, emoji = emoji)
        areas = areas + area
        return area
    }

    override suspend fun actualizarArea(area: Area): Area {
        areas = areas.map { if (it.id == area.id) area else it }
        return area
    }

    override suspend fun eliminarArea(id: String) {
        areas = areas.filterNot { it.id == id }
    }
}
