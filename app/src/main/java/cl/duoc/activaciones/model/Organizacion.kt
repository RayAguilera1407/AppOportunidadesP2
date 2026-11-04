package cl.duoc.activaciones.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "organizacion")
data class Organizacion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val tipo: String,
    val rubro: String,
    val comuna: String,
    val region: String,
    val sitioWeb: String = "",
    val observaciones: String = ""
)