package es.uam.esalud.recuerdatec.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val JsonApp = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}

enum class Rol { PACIENTE, CUIDADOR }

/** Recuerdo (persona, lugar, momento). La foto va como JPEG reducido en Base64. */
@Serializable
data class Recuerdo(
    val id: String,
    val titulo: String,
    val persona: String = "",
    val descripcion: String = "",
    val foto: String = "",
)

@Serializable
data class EventoAgenda(
    val id: String,
    val titulo: String,
    val fecha: String, // yyyy-MM-dd
    val hora: String = "", // HH:mm
    val notas: String = "",
)

@Serializable
data class Medicamento(
    val id: String,
    val nombre: String,
    val dosis: String = "",
    val horas: List<String> = emptyList(), // HH:mm
    val instrucciones: String = "",
)

/** Toma confirmada por el paciente. `hora` es la hora programada. */
@Serializable
data class Toma(
    val id: String,
    val medicamentoId: String,
    val nombreMedicamento: String,
    val fecha: String,
    val hora: String,
    val confirmadaEn: Long,
)

@Serializable
data class ResultadoEjercicio(
    val id: String,
    val ejercicio: String,
    val fechaMillis: Long,
    val aciertos: Int,
    val intentos: Int,
    val segundos: Int,
)

@Serializable
data class AnimoRegistro(
    val id: String,
    val fechaMillis: Long,
    val valor: Int, // 1 (mal) .. 4 (muy bien)
)

/** Información que el cuidador da al asistente para personalizar las respuestas. */
@Serializable
data class PerfilIA(
    val nombrePaciente: String = "",
    val nombreCuidador: String = "",
    val telefonoCuidador: String = "",
    val familiares: String = "",
    val aficiones: String = "",
    val lugares: String = "",
    val tono: String = "",
    val notas: String = "",
)

/** Parte de los datos que escribe el cuidador. */
@Serializable
data class DatosCuidador(
    val rev: Long = 0,
    val perfil: PerfilIA = PerfilIA(),
    val recuerdos: List<Recuerdo> = emptyList(),
    val agenda: List<EventoAgenda> = emptyList(),
    val medicamentos: List<Medicamento> = emptyList(),
)

/** Parte de los datos que escribe el paciente. */
@Serializable
data class DatosPaciente(
    val rev: Long = 0,
    val nombre: String = "",
    val tomas: List<Toma> = emptyList(),
    val resultados: List<ResultadoEjercicio> = emptyList(),
    val animos: List<AnimoRegistro> = emptyList(),
    val ultimaInteraccion: Long = 0,
)

@Serializable
data class SesionGuardada(
    val uid: String,
    val email: String,
    val nombre: String,
    val rol: Rol,
    val espacio: String? = null,
    val codigo: String? = null,
    val refreshToken: String,
)

/** Documento usuarios/{uid} en Firestore. */
@Serializable
data class UsuarioNube(
    val nombre: String,
    val rol: Rol,
    val espacio: String? = null,
    val codigo: String? = null,
)

@Serializable
data class ConfigNube(
    val apiKey: String = "",
    val projectId: String = "",
    val urlAsistente: String = "",
) {
    val configurada: Boolean get() = apiKey.isNotBlank() && projectId.isNotBlank()
}

/**
 * Valores por defecto de la configuracion. Si los rellenas aqui, la app arranca ya
 * conectada a tu proyecto de Firebase (tambien se pueden cambiar desde Ajustes).
 */
object ConfigPorDefecto {
    const val API_KEY = ""
    const val PROJECT_ID = ""
    const val URL_ASISTENTE = ""
}
