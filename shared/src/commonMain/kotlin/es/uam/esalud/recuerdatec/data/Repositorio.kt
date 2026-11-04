package es.uam.esalud.recuerdatec.data

import es.uam.esalud.recuerdatec.model.ConfigNube
import es.uam.esalud.recuerdatec.model.ConfigPorDefecto
import es.uam.esalud.recuerdatec.model.DatosCuidador
import es.uam.esalud.recuerdatec.model.DatosPaciente
import es.uam.esalud.recuerdatec.model.JsonApp
import es.uam.esalud.recuerdatec.model.Rol
import es.uam.esalud.recuerdatec.model.SesionGuardada
import es.uam.esalud.recuerdatec.model.UsuarioNube
import es.uam.esalud.recuerdatec.util.horaTexto
import es.uam.esalud.recuerdatec.util.ahoraMillis
import es.uam.esalud.recuerdatec.util.nuevoId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.random.Random

/**
 * Fuente unica de datos. Todo se guarda siempre en local; si hay sesion con espacio
 * compartido, ademas se sincroniza con Firestore. Cada parte (cuidador / paciente) la
 * escribe un solo rol, asi que no hay conflictos: gana siempre la revision mas alta.
 */
class Repositorio(
    private val almacen: AlmacenLocal,
    private val scope: CoroutineScope,
) {
    val config = MutableStateFlow(cargarConfig())
    private val nube = FirebaseRest { config.value }

    val cuidador = MutableStateFlow(leerLocal(CLAVE_CUIDADOR, DatosCuidador.serializer()) ?: DatosCuidador())
    val paciente = MutableStateFlow(leerLocal(CLAVE_PACIENTE, DatosPaciente.serializer()) ?: DatosPaciente())
    val sesion = MutableStateFlow(leerLocal(CLAVE_SESION, SesionGuardada.serializer()))
    val estadoSync = MutableStateFlow("")

    private val candado = Mutex()
    private var auth: SesionAuth? = null

    // ---------- almacenamiento local ----------

    private fun <T> leerLocal(clave: String, ser: KSerializer<T>): T? =
        almacen.leer(clave)?.let { runCatching { JsonApp.decodeFromString(ser, it) }.getOrNull() }

    private fun <T> guardarLocal(clave: String, ser: KSerializer<T>, valor: T) {
        runCatching { almacen.escribir(clave, JsonApp.encodeToString(ser, valor)) }
    }

    private fun cargarConfig(): ConfigNube =
        leerLocal(CLAVE_CONFIG, ConfigNube.serializer()) ?: ConfigNube(
            apiKey = ConfigPorDefecto.API_KEY,
            projectId = ConfigPorDefecto.PROJECT_ID,
            urlAsistente = ConfigPorDefecto.URL_ASISTENTE,
        )

    fun guardarConfig(nueva: ConfigNube) {
        config.value = nueva.copy(
            apiKey = nueva.apiKey.trim(),
            projectId = nueva.projectId.trim(),
            urlAsistente = nueva.urlAsistente.trim(),
        )
        guardarLocal(CLAVE_CONFIG, ConfigNube.serializer(), config.value)
    }

    private fun guardarSesion() {
        val s = sesion.value
        if (s == null) almacen.borrar(CLAVE_SESION) else guardarLocal(CLAVE_SESION, SesionGuardada.serializer(), s)
    }

    // ---------- modificar datos ----------

    fun modificarCuidador(cambio: (DatosCuidador) -> DatosCuidador) {
        cuidador.update { cambio(it).copy(rev = it.rev + 1) }
        guardarLocal(CLAVE_CUIDADOR, DatosCuidador.serializer(), cuidador.value)
        empujar()
    }

    fun modificarPaciente(cambio: (DatosPaciente) -> DatosPaciente) {
        paciente.update { cambio(it).copy(rev = it.rev + 1) }
        guardarLocal(CLAVE_PACIENTE, DatosPaciente.serializer(), paciente.value)
        empujar()
    }

    private fun empujar() {
        if (sesion.value?.espacio != null) scope.launch { sincronizar() }
    }

    // ---------- sincronizacion ----------

    suspend fun sincronizar() {
        candado.withLock {
            val s = sesion.value ?: return
            val espacio = s.espacio ?: return
            if (!config.value.configurada) return
            try {
                val tk = token()
                sincParte(
                    tk, "espacios/$espacio/datos/cuidador", cuidador, DatosCuidador.serializer(),
                    CLAVE_CUIDADOR, propio = s.rol == Rol.CUIDADOR,
                ) { it.rev }
                sincParte(
                    tk, "espacios/$espacio/datos/paciente", paciente, DatosPaciente.serializer(),
                    CLAVE_PACIENTE, propio = s.rol == Rol.PACIENTE,
                ) { it.rev }
                estadoSync.value = "Sincronizado a las ${horaTexto()}"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                estadoSync.value = e.message ?: "Error de sincronizacion"
            }
        }
    }

    private suspend fun <T> sincParte(
        tk: String,
        ruta: String,
        flujo: MutableStateFlow<T>,
        ser: KSerializer<T>,
        claveLocal: String,
        propio: Boolean,
        rev: (T) -> Long,
    ) {
        val texto = nube.leerJson(tk, ruta)
        val remoto = texto?.let { runCatching { JsonApp.decodeFromString(ser, it) }.getOrNull() }
        val local = flujo.value
        val revRemota = if (remoto != null) rev(remoto) else -1L
        if (remoto != null && revRemota > rev(local)) {
            flujo.value = remoto
            guardarLocal(claveLocal, ser, remoto)
        } else if (propio && rev(local) > revRemota) {
            nube.escribirJson(tk, ruta, JsonApp.encodeToString(ser, local))
        }
    }

    private suspend fun token(): String {
        val a = auth
        if (a != null && ahoraMillis() < a.expira - 60_000) return a.idToken
        val s = sesion.value ?: throw ErrorNube("No hay sesion iniciada.")
        val nuevo = nube.refrescar(s.refreshToken)
        auth = nuevo
        if (nuevo.refreshToken.isNotBlank() && nuevo.refreshToken != s.refreshToken) {
            sesion.value = s.copy(refreshToken = nuevo.refreshToken)
            guardarSesion()
        }
        return nuevo.idToken
    }

    // ---------- cuentas ----------

    suspend fun registrar(email: String, pass: String, nombre: String, rol: Rol) {
        val a = nube.registrar(email.trim(), pass)
        auth = a
        var espacio: String? = null
        var codigo: String? = null
        if (rol == Rol.CUIDADOR) {
            val par = crearEspacio(a.idToken, a.uid)
            espacio = par.first
            codigo = par.second
        }
        val usuario = UsuarioNube(nombre, rol, espacio, codigo)
        nube.escribirJson(a.idToken, "usuarios/${a.uid}", JsonApp.encodeToString(UsuarioNube.serializer(), usuario))
        sesion.value = SesionGuardada(a.uid, email.trim(), nombre, rol, espacio, codigo, a.refreshToken)
        guardarSesion()
        // Se conservan los datos locales propios; la otra parte empieza vacia.
        if (rol == Rol.CUIDADOR) {
            reiniciarPaciente()
        } else {
            reiniciarCuidador()
            modificarPaciente { it.copy(nombre = nombre) }
        }
        sincronizar()
    }

    suspend fun iniciarSesion(email: String, pass: String) {
        val a = nube.iniciarSesion(email.trim(), pass)
        auth = a
        val texto = nube.leerJson(a.idToken, "usuarios/${a.uid}")
            ?: throw ErrorNube("Esta cuenta no tiene perfil en RecuerdaTec.")
        val u = JsonApp.decodeFromString(UsuarioNube.serializer(), texto)
        sesion.value = SesionGuardada(a.uid, email.trim(), u.nombre, u.rol, u.espacio, u.codigo, a.refreshToken)
        guardarSesion()
        reiniciarCuidador()
        reiniciarPaciente()
        sincronizar()
    }

    suspend fun vincular(codigoTexto: String) {
        val s = sesion.value ?: throw ErrorNube("Primero inicia sesion.")
        val codigo = codigoTexto.trim().uppercase()
        if (codigo.isEmpty()) throw ErrorNube("Escribe el codigo que te ha dado tu cuidador/a.")
        val tk = token()
        val texto = nube.leerJson(tk, "codigos/$codigo") ?: throw ErrorNube("El codigo no es valido.")
        val espacio = JsonApp.parseToJsonElement(texto).jsonObject["espacio"]?.jsonPrimitive?.content
            ?: throw ErrorNube("El codigo no es valido.")
        nube.escribirJson(tk, "espacios/$espacio/miembros/${s.uid}", """{"rol":"PACIENTE"}""")
        val usuario = UsuarioNube(s.nombre, s.rol, espacio, codigo)
        nube.escribirJson(tk, "usuarios/${s.uid}", JsonApp.encodeToString(UsuarioNube.serializer(), usuario))
        sesion.value = s.copy(espacio = espacio, codigo = codigo)
        guardarSesion()
        reiniciarCuidador()
        sincronizar()
    }

    fun cerrarSesion() {
        sesion.value = null
        auth = null
        guardarSesion()
        estadoSync.value = ""
    }

    private fun reiniciarCuidador() {
        cuidador.value = DatosCuidador()
        guardarLocal(CLAVE_CUIDADOR, DatosCuidador.serializer(), cuidador.value)
    }

    private fun reiniciarPaciente() {
        paciente.value = DatosPaciente()
        guardarLocal(CLAVE_PACIENTE, DatosPaciente.serializer(), paciente.value)
    }

    private suspend fun crearEspacio(tk: String, uid: String): Pair<String, String> {
        val espacio = nuevoId() + nuevoId()
        nube.escribirJson(tk, "espacios/$espacio/miembros/$uid", """{"rol":"CUIDADOR"}""")
        var codigo: String
        var intentos = 0
        do {
            codigo = codigoAleatorio()
            intentos++
        } while (nube.leerJson(tk, "codigos/$codigo") != null && intentos < 10)
        nube.escribirJson(
            tk,
            "codigos/$codigo",
            buildJsonObject { put("espacio", espacio) }.toString(),
        )
        return espacio to codigo
    }

    private fun codigoAleatorio(): String {
        val letras = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { letras[Random.nextInt(letras.length)] }.joinToString("")
    }

    companion object {
        private const val CLAVE_CUIDADOR = "datos_cuidador"
        private const val CLAVE_PACIENTE = "datos_paciente"
        private const val CLAVE_SESION = "sesion"
        private const val CLAVE_CONFIG = "config_nube"
    }
}
