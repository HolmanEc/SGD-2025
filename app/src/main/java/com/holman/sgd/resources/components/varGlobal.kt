package com.holman.sgd.resources.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import com.holman.sgd.R
import com.holman.sgd.Screen
import com.holman.sgd.ui.theme.*




// Margen estándar reutilizable para tus contenedores
val ContenedorPrincipal = PaddingValues(
    start = 16.dp,   // Margen izquierdo
    end = 16.dp,     // Margen derecho
    top = 24.dp,     // Margen superior
    bottom = 24.dp   // Margen inferior
)



fun getColorsCardsInicio() = listOf(
    Card1,
    Card2,
    Card3,
    Card4,
    Card5,
    Card6,
    Card7,
    Card8,
)

fun getCardsInicio() = listOf(
    Triple(
        "Nóminas",
        R.drawable.ic_nominas,
        Screen.nominas.route to "Consulta y gestión completa de la lista de estudiantes para su asistencia diaria, para llevar un control preciso y actualizado."
    ),
    Triple(
        "Calificaciones",
        R.drawable.ic_calificaciones,
        Screen.calificaciones.route to "Registro, consulta y análisis de las notas obtenidas en las diferentes evaluaciones académicas de los estudiantes."
    ),
    Triple(
        "Asistencias",
        R.drawable.ic_asistencias,
        Screen.asistencias.route to "Control y registro de asistencia de estudiantes, permitiendo llevar un seguimiento detallado de la puntualidad."
    ),
    Triple(
        "Tutoria",
        R.drawable.ic_tutoria,
        Screen.tutoria.route to "Seguimiento personalizado, permitiendo planificar, registrar y establecer acciones de apoyo para cada estudiante."
    ),
    Triple(
        "Documentos",
        R.drawable.ic_documentos,
        Screen.documentos.route to "Administración y consulta de archivos y documentos importantes relacionados con las actividades de gestión docente."
    ),
    Triple(
        "Varios",
        R.drawable.ic_varios,
        Screen.varios.route to "Espacio destinado para registrar, organizar y consultar información adicional o complementaria requerida para la gestión."
    ),
    Triple(
        "Configuración",
        R.drawable.ic_config,
        "config" to "Ajustes y preferencias personalizables para optimizar la experiencia de uso de la aplicación según las necesidades."
    ),
    Triple(
        "Acerca de",
        R.drawable.ic_acerca,
        "about" to "Información detallada sobre la aplicación, su versión, desarrolladores y datos de contacto para soporte o sugerencias."
    )
)
