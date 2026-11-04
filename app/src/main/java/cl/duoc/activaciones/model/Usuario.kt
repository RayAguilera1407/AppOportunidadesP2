package cl.duoc.activaciones.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuario")
data class Usuario(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val correo: String,
    val clave: String,
    val perfil: Perfil,
    val zona: String,
    val area: String = "Comercial",
    val direccion: String = "",
    val estadoCuenta: EstadoCuenta = EstadoCuenta.ACTIVA,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaActualizacion: Long = System.currentTimeMillis()
)