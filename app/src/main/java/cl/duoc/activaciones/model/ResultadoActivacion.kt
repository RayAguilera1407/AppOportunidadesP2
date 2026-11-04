package cl.duoc.activaciones.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resultado_activacion")
data class ResultadoActivacion(
    @PrimaryKey val oportunidadId: Long,
    val asistentes: Int,
    val contactosObtenidos: Int,
    val inscripciones: Int,
    val evaluacion: String
)