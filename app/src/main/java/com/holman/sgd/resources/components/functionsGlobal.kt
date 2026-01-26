package com.holman.sgd.resources.components

import com.holman.sgd.resources.NominaResumen
import com.google.firebase.firestore.Query
import java.security.MessageDigest




// ✅ Nombre cambiado para coincidir con tu llamada
fun generarIdUnicoEstudianteNominaFirebase(cedula: String, nombre: String): String {
    // 1. Limpieza de datos
    val base = (cedula.trim() + nombre.trim())
        .lowercase()
        .replace(Regex("[^a-z0-9]"), "")

    // 2. Respaldo si los datos fallan
    if (base.isEmpty()) {
        return "std_" + java.util.UUID.randomUUID().toString().replace("-", "").take(16)
    }

    // 3. Generación del Hash MD5 (ID único y constante)
    val bytes = MessageDigest.getInstance("MD5").digest(base.toByteArray())
    val hash = bytes.joinToString("") { "%02x".format(it) }.take(16)

    return "std_$hash"
}










// Pégala en tu archivo de funciones globales o de datos
fun cargarListaNominasFirestore(
    uid: String,
    onSuccess: (List<NominaResumen>) -> Unit,
    onError: (String) -> Unit
) {
    // ✅ Se utiliza FirestorePaths para apuntar a la ruta del usuario específico
    FirestorePaths.cursos(uid)
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .get()
        .addOnSuccessListener { documents ->
            val listaNominas = documents.map { document ->
                NominaResumen(
                    id = document.id,
                    institucion = document.getString("institucion") ?: "",
                    docente = document.getString("docente") ?: "",
                    curso = document.getString("curso") ?: "",
                    paralelo = document.getString("paralelo") ?: "",
                    asignatura = document.getString("asignatura") ?: "",
                    especialidad = document.getString("especialidad") ?: "",
                    periodo = document.getString("periodo") ?: "",
                    timestamp = document.getLong("timestamp") ?: 0L
                )
            }
            onSuccess(listaNominas)
        }
        .addOnFailureListener { exception ->
            onError(exception.localizedMessage ?: "Error desconocido")
        }
}