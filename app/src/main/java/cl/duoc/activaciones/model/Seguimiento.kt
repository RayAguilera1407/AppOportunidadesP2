package cl.duoc.activaciones.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "seguimiento", indices = [Index("oportunidadId")])
data class Seguimiento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val oportunidadId: Long,
    val tipoAccion: TipoAccion,
    val fechaHora: Long,
    val resultado: String,
    val proximaAccion: String = "",
    val fechaProximoSeguimiento: Long? = null
)