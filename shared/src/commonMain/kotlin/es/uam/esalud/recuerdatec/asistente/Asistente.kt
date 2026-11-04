package es.uam.esalud.recuerdatec.asistente

import es.uam.esalud.recuerdatec.logica.eventosDeHoy
import es.uam.esalud.recuerdatec.logica.tomasPendientes
import es.uam.esalud.recuerdatec.model.EventoAgenda
import es.uam.esalud.recuerdatec.model.JsonApp
import es.uam.esalud.recuerdatec.model.Medicamento
import es.uam.esalud.recuerdatec.model.PerfilIA
import es.uam.esalud.recuerdatec.model.Recuerdo
import es.uam.esalud.recuerdatec.model.Toma
import es.uam.esalud.recuerdatec.util.fechaLarga
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Todo lo que el asistente puede saber del paciente en este momento. */
data class ContextoAsistente(
    val perfil: PerfilIA,
    val recuerdos: List<Recuerdo>,
    val agenda: List<EventoAgenda>,
    val medicamentos: List<Medicamento>,
    val tomas: List<Toma>,
    val hoy: String,
    val hora: String,
    val minutosAhora: Int,
)

interface Asistente {
    suspend fun responder(mensaje: String, ctx: ContextoAsistente): String
}

/** Asistente sin Internet: reglas sencillas sobre los datos que el cuidador ha cargado. */
class AsistenteLocal : Asistente {
    override suspend fun responder(mensaje: String, ctx: ContextoAsistente): String {
        val t = mensaje.lowercase()
        val nombre = ctx.perfil.nombrePaciente.trim()
        val saludo = if (nombre.isNotEmpty()) "$nombre, " else ""
        fun tiene(vararg palabras: String) = palabras.any { t.contains(it) }

        return when {
            tiene("hora") ->
                "${saludo}ahora son las ${ctx.hora}."

            tiene("dia es", "día es", "fecha", "que dia", "qué día", "hoy es") ->
                "${saludo}hoy es ${fechaLarga(ctx.hoy)}."

            tiene("medic", "pastilla", "pildora", "píldora", "tomar") -> {
                val p = tomasPendientes(ctx.medicamentos, ctx.tomas, ctx.hoy, ctx.minutosAhora)
                if (p.isEmpty()) {
                    "${saludo}no tienes ninguna toma pendiente hoy. ¡Muy bien!"
                } else {
                    "${saludo}te quedan por tomar: " +
                        p.joinToString("; ") { "${it.medicamento.nombre} a las ${it.hora}" } + "."
                }
            }

            tiene("agenda", "cita", "hoy tengo", "que tengo", "qué tengo", "plan") -> {
                val e = eventosDeHoy(ctx.agenda, ctx.hoy)
                if (e.isEmpty()) {
                    "${saludo}hoy no tienes nada apuntado en la agenda. Puedes descansar."
                } else {
                    "${saludo}hoy tienes: " +
                        e.joinToString("; ") { (if (it.hora.isNotBlank()) it.hora + " " else "") + it.titulo } + "."
                }
            }

            tiene("familia", "hijo", "hija", "nieto", "nieta", "quien es", "quién es") ->
                if (ctx.perfil.familiares.isNotBlank()) {
                    "${saludo}tu familia: ${ctx.perfil.familiares}. Puedes ver sus fotos en \"Mis recuerdos\"."
                } else {
                    "${saludo}en \"Mis recuerdos\" puedes ver las fotos de tu familia."
                }

            tiene("perdid", "no se donde", "no sé dónde", "ayuda", "miedo", "asust", "nervios", "ansios") -> {
                val tel = ctx.perfil.telefonoCuidador.trim()
                val cuidador = ctx.perfil.nombreCuidador.trim().ifEmpty { "tu cuidador/a" }
                "${saludo}tranquilo/a, respira hondo. Estas a salvo. " +
                    if (tel.isNotEmpty()) "Puedes llamar a $cuidador al $tel desde el boton \"Llamar\"."
                    else "Avisa a $cuidador cuando puedas."
            }

            tiene("gracias") -> "¡De nada! Aqui estoy para lo que necesites."

            tiene("hola", "buenas", "buenos") -> "¡Hola ${nombre.ifEmpty { "amigo/a" }}! ¿En que puedo ayudarte?"

            else -> {
                val gusto = ctx.perfil.aficiones.trim()
                "${saludo}puedes preguntarme la hora, el dia, tus medicinas o lo que tienes hoy." +
                    if (gusto.isNotEmpty()) " Tambien podemos hablar de: $gusto." else ""
            }
        }
    }
}

/**
 * Asistente con IA: envia la pregunta al servidor `backend/servidor.mjs` (la clave de la API
 * vive alli, nunca en la app). Si falla, cae al asistente local.
 */
class AsistenteRemoto(
    private val url: String,
    private val respaldo: Asistente = AsistenteLocal(),
) : Asistente {
    private val http = HttpClient()

    override suspend fun responder(mensaje: String, ctx: ContextoAsistente): String {
        return try {
            val resumen = buildString {
                appendLine("Hoy: ${fechaLarga(ctx.hoy)}, hora ${ctx.hora}.")
                appendLine("Perfil: ${JsonApp.encodeToString(PerfilIA.serializer(), ctx.perfil)}")
                appendLine("Agenda de hoy: " + eventosDeHoy(ctx.agenda, ctx.hoy).joinToString("; ") { "${it.hora} ${it.titulo}" })
                appendLine(
                    "Medicacion pendiente: " +
                        tomasPendientes(ctx.medicamentos, ctx.tomas, ctx.hoy, ctx.minutosAhora)
                            .joinToString("; ") { "${it.medicamento.nombre} ${it.hora}" },
                )
                appendLine("Recuerdos: " + ctx.recuerdos.joinToString("; ") { "${it.titulo} (${it.persona})" })
            }
            val cuerpo = buildJsonObject {
                put("mensaje", mensaje)
                put("contexto", resumen)
            }.toString()
            val r = http.post(url.trimEnd('/') + "/chat") {
                contentType(ContentType.Application.Json)
                setBody(cuerpo)
            }
            if (!r.status.isSuccess()) return respaldo.responder(mensaje, ctx)
            val texto = JsonApp.parseToJsonElement(r.bodyAsText()).jsonObject["respuesta"]?.jsonPrimitive?.content
            texto?.takeIf { it.isNotBlank() } ?: respaldo.responder(mensaje, ctx)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            respaldo.responder(mensaje, ctx)
        }
    }
}
