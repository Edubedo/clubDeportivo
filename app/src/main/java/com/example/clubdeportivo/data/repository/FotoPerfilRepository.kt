package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.util.Resultado

/** Fotos de perfil guardadas en la nube, una por usuario. */
interface FotoPerfilRepository {

    /** Sube [jpeg] como foto de [usuarioId], guarda su URL en el perfil y la devuelve. */
    suspend fun subir(usuarioId: String, jpeg: ByteArray): Resultado<String>

    /** Borra la foto de [usuarioId] de la nube y limpia su URL del perfil. */
    suspend fun eliminar(usuarioId: String): Resultado<Unit>
}
