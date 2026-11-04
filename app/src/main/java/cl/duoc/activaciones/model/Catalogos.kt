package cl.duoc.activaciones.model

// Medio de contacto preferente (sección 3.2, información de contactos)
val MEDIOS_CONTACTO = listOf("Correo", "Teléfono", "WhatsApp", "Presencial")

// Estado del contacto
val ESTADOS_CONTACTO = listOf("Sin contactar", "Contactado", "Interesado", "No interesado")

// Áreas de contacto definidas en el caso
val AREAS_CONTACTO = listOf(
    "Eventos", "Activaciones", "Marketing", "Comunicaciones",
    "Recursos Humanos", "Bienestar", "Beneficios", "Administración"
)

// Tipos de organización definidos en el caso
val TIPOS_ORGANIZACION = listOf(
    "Empresa", "Municipalidad", "Institución pública", "Institución privada",
    "Organizador de eventos", "Centro comercial", "Asociación", "Otra entidad"
)