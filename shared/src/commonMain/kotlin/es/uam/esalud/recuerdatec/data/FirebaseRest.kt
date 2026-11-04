package es.uam.esalud.recuerdatec.data

import es.uam.esalud.recuerdatec.model.ConfigNube
import es.uam.esalud.recuerdatec.model.JsonApp
import es.uam.esalud.recuerdatec.util.ahoraMillis
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Error con un mensaje que se puede ensenar tal cual al usuario. */
class ErrorNube(mensaje: String) : Exception(mensaje)

data class SesionAuth(
    val uid: String,
    val idToken: String,
    val refreshToken: String,
    val expira: Long,
)

/**
 * Cliente minimo de Firebase por REST (Authentication + Firestore).
 * Sirve igual en Android, escritorio y web, sin SDK de Firebase.
 * Cada documento guarda su contenido como un unico campo de texto "json".
 */
class FirebaseRest(private val config: () -> ConfigNube) {
    private val http = HttpClient()

    private suspend fun llamar(
        metodo: HttpMethod,
        url: String,
        cuerpo: String? = null,
        token: String? = null,
        tipo: ContentType = ContentType.Application.Json,
    ): Pair<HttpStatusCode, String> {
        try {
            val r = http.request(url) {
                method = metodo
                if (token != null) header("Authorization", "Bearer $token")
                if (cuerpo != null) {
                    contentType(tipo)
                    setBody(cuerpo)
                }
            }
            return r.status to r.bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw ErrorNube("No hay conexion con el servidor. Revisa Internet.")
        }
    }

    private fun comprobarConfig(): ConfigNube {
        val c = config()
        if (!c.configurada) {
            throw ErrorNube("Falta configurar Firebase (clave API e id de proyecto) en Ajustes.")
        }
        return c
    }

    private fun errorDe(texto: String): ErrorNube {
        val m = runCatching {
            JsonApp.parseToJsonElement(texto).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
        }.getOrNull() ?: "Error desconocido"
        return ErrorNube(traducir(m))
    }

    private fun traducir(m: String): String = when {
        m.startsWith("EMAIL_EXISTS") -> "Ya existe una cuenta con ese correo."
        m.startsWith("INVALID_LOGIN_CREDENTIALS") || m.startsWith("INVALID_PASSWORD") ||
            m.startsWith("EMAIL_NOT_FOUND") -> "Correo o contraseña incorrectos."
        m.startsWith("WEAK_PASSWORD") -> "La contraseña debe tener al menos 6 caracteres."
        m.startsWith("INVALID_EMAIL") -> "El correo no es valido."
        m.startsWith("TOO_MANY_ATTEMPTS") -> "Demasiados intentos. Prueba mas tarde."
        m.contains("API key", ignoreCase = true) -> "La clave API de Firebase no es valida."
        m.contains("permission", ignoreCase = true) -> "Sin permiso en Firestore. Revisa las reglas de seguridad."
        m.startsWith("OPERATION_NOT_ALLOWED") -> "Activa el acceso con correo y contraseña en Firebase Authentication."
        else -> m
    }

    private fun leerAuth(texto: String, claveToken: String, claveRefresh: String, claveUid: String, claveExp: String): SesionAuth {
        val o = JsonApp.parseToJsonElement(texto).jsonObject
        val dur = o[claveExp]?.jsonPrimitive?.content?.toLongOrNull() ?: 3600L
        return SesionAuth(
            uid = o[claveUid]?.jsonPrimitive?.content ?: throw ErrorNube("Respuesta de Firebase incompleta."),
            idToken = o[claveToken]?.jsonPrimitive?.content ?: throw ErrorNube("Respuesta de Firebase incompleta."),
            refreshToken = o[claveRefresh]?.jsonPrimitive?.content ?: "",
            expira = ahoraMillis() + dur * 1000,
        )
    }

    private suspend fun identidad(accion: String, email: String, pass: String): SesionAuth {
        val c = comprobarConfig()
        val cuerpo = buildJsonObject {
            put("email", email)
            put("password", pass)
            put("returnSecureToken", true)
        }.toString()
        val (estado, texto) = llamar(
            HttpMethod.Post,
            "https://identitytoolkit.googleapis.com/v1/accounts:$accion?key=${c.apiKey}",
            cuerpo,
        )
        if (!estado.isSuccess()) throw errorDe(texto)
        return leerAuth(texto, "idToken", "refreshToken", "localId", "expiresIn")
    }

    suspend fun registrar(email: String, pass: String): SesionAuth = identidad("signUp", email, pass)

    suspend fun iniciarSesion(email: String, pass: String): SesionAuth = identidad("signInWithPassword", email, pass)

    suspend fun refrescar(refreshToken: String): SesionAuth {
        val c = comprobarConfig()
        val (estado, texto) = llamar(
            HttpMethod.Post,
            "https://securetoken.googleapis.com/v1/token?key=${c.apiKey}",
            "grant_type=refresh_token&refresh_token=$refreshToken",
            tipo = ContentType.Application.FormUrlEncoded,
        )
        if (!estado.isSuccess()) throw errorDe(texto)
        return leerAuth(texto, "id_token", "refresh_token", "user_id", "expires_in")
    }

    private fun urlDoc(ruta: String): String {
        val c = comprobarConfig()
        return "https://firestore.googleapis.com/v1/projects/${c.projectId}/databases/(default)/documents/$ruta"
    }

    /** Lee el campo "json" de un documento; null si el documento no existe. */
    suspend fun leerJson(token: String, ruta: String): String? {
        val (estado, texto) = llamar(HttpMethod.Get, urlDoc(ruta), token = token)
        if (estado == HttpStatusCode.NotFound) return null
        if (!estado.isSuccess()) throw errorDe(texto)
        val o: JsonObject = JsonApp.parseToJsonElement(texto).jsonObject
        return o["fields"]?.jsonObject?.get("json")?.jsonObject?.get("stringValue")?.jsonPrimitive?.content
    }

    /** Crea o reemplaza el campo "json" de un documento. */
    suspend fun escribirJson(token: String, ruta: String, json: String) {
        val cuerpo = buildJsonObject {
            put(
                "fields",
                buildJsonObject {
                    put("json", buildJsonObject { put("stringValue", json) })
                },
            )
        }.toString()
        val (estado, texto) = llamar(
            HttpMethod.Patch,
            urlDoc(ruta) + "?updateMask.fieldPaths=json",
            cuerpo,
            token,
        )
        if (!estado.isSuccess()) throw errorDe(texto)
    }
}
