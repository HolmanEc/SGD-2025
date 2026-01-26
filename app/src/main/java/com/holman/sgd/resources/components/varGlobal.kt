package com.holman.sgd.resources.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.holman.sgd.R
import com.holman.sgd.Screen
import com.holman.sgd.ui.theme.*


// VARIABLE DE CONTROL DE RUTAS FIREBASE
object FirestorePaths {

    object CatalogKeys {
        // Estructura: Label (Interfaz) y Colección (Base de Datos en MAYÚSCULAS)
        const val INSTITUCIONES_LABEL = "Instituciones"
        const val INSTITUCIONES_COL = "INSTITUCIONES"

        const val DOCENTES_LABEL = "Docentes"
        const val DOCENTES_COL = "DOCENTES"

        const val CURSOS_LABEL = "Cursos"
        const val CURSOS_COL = "CURSOS"

        const val PARALELOS_LABEL = "Paralelos"
        const val PARALELOS_COL = "PARALELOS"

        const val ASIGNATURAS_LABEL = "Asignaturas"
        const val ASIGNATURAS_COL = "ASIGNATURAS"

        const val ESPECIALIDADES_LABEL = "Especialidades"
        const val ESPECIALIDADES_COL = "ESPECIALIDADES"

        const val PERIODOS_LABEL = "Periodos Lectivos"
        const val PERIODOS_COL = "PERIODOS"
    }

    private const val COL_GESTION_ACADEMICA = "SGD.DB"

    // ✅ Subcolecciones por usuario
    private const val SUBCOL_DATOS_GENERALES = "DATOS.GENERALES"   // (colección)
    private const val DOC_DATOS_GENERALES_ROOT = "CATALOGO"      // (doc fijo)
    private const val SUBCOL_CURSOS = "CURSOS"             // aquí estás guardando NÓMINAS
    private const val SUBCOL_CONTACTOS = "CONTACTOS"

    // ✅ Dentro de una nómina (documento)
    private const val SUBCOL_CALIFICACIONES = "CALIFICACIONES"
    private const val SUBCOL_ASISTENCIAS = "ASISTENCIAS"

    // -------------------------
    // ✅ Variables solicitadas
    // -------------------------
    const val SECCION_INFORME = "INFORME.ANUAL"
    val SECCIONES_TABLAS_INSUMOS = listOf(
        "PRIMER.TRIMESTRE",
        "SEGUNDO.TRIMESTRE",
        "TERCER.TRIMESTRE",
        SECCION_INFORME
    )

    private const val SUBCOLECCION_ALUMNOS = "INSUMOS"

    private const val DOC_META = "META"
    private const val DOC_CONFIG = "CONFIG"

    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    // -------------------------
    // ✅ Base por usuario
    // -------------------------
    fun userDoc(uid: String): DocumentReference =
        db.collection(COL_GESTION_ACADEMICA).document("USER:$uid")

    // -------------------------
    // ✅ Nóminas (SUBCOL_CURSOS)
    // -------------------------
    fun cursos(uid: String): CollectionReference =
        userDoc(uid).collection(SUBCOL_CURSOS)

    fun nominaDoc(uid: String, nominaId: String): DocumentReference =
        cursos(uid).document(nominaId)

    // -------------------------
    // ✅ Contactos
    // -------------------------
    fun contactos(uid: String): CollectionReference =
        userDoc(uid).collection(SUBCOL_CONTACTOS)

    // -------------------------
    // ✅ Datos Generales (POR USUARIO)
    // Ruta real: gestionAcademica/usuario:{uid}/datosGenerales/catalogos/{coleccion}/{item}
    // -------------------------
    fun datosGeneralesRoot(uid: String): DocumentReference =
        userDoc(uid)
            .collection(SUBCOL_DATOS_GENERALES)
            .document(DOC_DATOS_GENERALES_ROOT)

    fun datosGeneralesColeccion(uid: String, nombreColeccion: String): CollectionReference =
        datosGeneralesRoot(uid).collection(nombreColeccion)

    // -------------------------
    // ✅ Calificaciones dentro de una nómina
    // -------------------------
    fun calificaciones(nominaDocRef: DocumentReference): CollectionReference =
        nominaDocRef.collection(SUBCOL_CALIFICACIONES)

    fun calificacionesSeccion(nominaDocRef: DocumentReference, seccionId: String): DocumentReference =
        calificaciones(nominaDocRef).document(seccionId)

    fun calificacionesMeta(nominaDocRef: DocumentReference): DocumentReference =
        calificaciones(nominaDocRef).document(DOC_META)

    fun calificacionesConfig(nominaDocRef: DocumentReference): DocumentReference =
        calificaciones(nominaDocRef).document(DOC_CONFIG)

    // ✅ Insumos/Alumnos dentro de una sección de calificaciones
    fun insumos(nominaDocRef: DocumentReference, seccionId: String): CollectionReference =
        calificacionesSeccion(nominaDocRef, seccionId).collection(SUBCOLECCION_ALUMNOS)

    fun insumoDoc(nominaDocRef: DocumentReference, seccionId: String, alumnoId: String): DocumentReference =
        insumos(nominaDocRef, seccionId).document(alumnoId)

    // (Opcional) Si en algún punto quieres validar secciones permitidas:
    // fun esSeccionValida(seccionId: String): Boolean = seccionId in SECCIONES_CALIF

    // -------------------------
    // ✅ Asistencias dentro de una nómina
    // -------------------------
    fun asistencias(uid: String, nominaId: String): CollectionReference =
        nominaDoc(uid, nominaId).collection(SUBCOL_ASISTENCIAS)

    fun asistenciaDoc(uid: String, nominaId: String, fecha: String): DocumentReference =
        asistencias(uid, nominaId).document(fecha)
}

// Margen estándar reutilizable para tus contenedores
val ContenedorPrincipal = PaddingValues(
    start = 16.dp,   // Margen izquierdo
    end = 16.dp,     // Margen derecho
    top = 24.dp,     // Margen superior
    bottom = 24.dp   // Margen inferior
)


object Transparencia {
    const val SHADOW = 0.05f      // Extra sutil
    const val SOFT = 0.35f      // Muy sutil
    const val DEFAULT = 0.55f   // ✅ estándar (tu valor actual)
    const val SOLID = 0.75f     // Presencia clara
    const val STRONG = 0.90f    // Muy marcado
}

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

fun getCardsNominasMenu(): List<Triple<String, Int, String>> {
    return listOf(
        Triple(
            "Crear nómina",
            R.drawable.ic_crear,
            "Genera una nueva nómina desde cero.\nAgrega y organiza estudiantes de un período."
        ),
        Triple(
            "Revisar nómina",
            R.drawable.ic_revisar,
            "Accede a nóminas registradas.\nPermite editar, borrar y actualizar estudiantes."
        )
    )
}

fun getCardsVariosMenu(): List<Triple<String, Int, String>> {
    return listOf(
        Triple(
            "Gestionar contactos",
            R.drawable.ic_revisar,
            "Crea, edita y organiza contactos.\nBúsqueda rápida y acceso directo."
        ),
        Triple(
            "Datos generales",
            R.drawable.ic_crear,
            "Administra información general del sistema,\nparámetros y configuraciones."
        )
    )
}







