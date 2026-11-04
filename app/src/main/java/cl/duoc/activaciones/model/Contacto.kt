package cl.duoc.activaciones.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "contacto", indices = [Index("organizacionId")])
data class Contacto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val organizacionId: Long,
    val nombre: String,
    val cargo: String,
    val area: String,
    val correo: String,
    val telefono: String,
    val medioPreferente: String = "Correo",
    val estado: String = "Sin contactar",
    val fechaUltimoContacto: Long? = null,
    val observaciones: String = ""
)