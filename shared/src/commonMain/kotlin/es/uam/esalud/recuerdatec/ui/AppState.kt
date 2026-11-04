package es.uam.esalud.recuerdatec.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import es.uam.esalud.recuerdatec.data.Repositorio
import es.uam.esalud.recuerdatec.data.crearAlmacenLocal
import es.uam.esalud.recuerdatec.model.Rol
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

sealed interface Destino {
    data object Inicio : Destino
    data object Cuenta : Destino
    data object Ajustes : Destino
    data object PacienteHome : Destino
    data object CuidadorHome : Destino
    data object Recuerdos : Destino
    data class EditarRecuerdo(val id: String?) : Destino
    data object Agenda : Destino
    data class EditarEvento(val id: String?) : Destino
    data object Medicacion : Destino
    data class EditarMedicamento(val id: String?) : Destino
    data object Ejercicios : Destino
    data object Parejas : Destino
    data object QuienEs : Destino
    data object Seguimiento : Destino
    data object Asistente : Destino
    data object Animo : Destino
    data object PerfilIA : Destino
}

/** Estado global de la interfaz: navegacion, rol activo, mensajes y acceso a los datos. */
class AppState {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val almacen = crearAlmacenLocal()
    val repo = Repositorio(almacen, scope)

    val pila = mutableStateListOf<Destino>()
    val actual: Destino get() = pila.last()

    /** Perfil elegido en modo local (sin cuenta). */
    var rolLocal by mutableStateOf<Rol?>(null)
    var mensaje by mutableStateOf<String?>(null)
    var escala by mutableStateOf(almacen.leer("escala")?.toFloatOrNull() ?: 1f)
        private set

    init {
        val s = repo.sesion.value
        pila.add(Destino.Inicio)
        if (s != null && (s.rol == Rol.CUIDADOR || s.espacio != null)) {
            pila.clear()
            pila.add(if (s.rol == Rol.CUIDADOR) Destino.CuidadorHome else Destino.PacienteHome)
        }
    }

    /** Rol con el que se usa la app ahora: el de la cuenta, o el elegido en modo local. */
    fun rol(): Rol? = repo.sesion.value?.rol ?: rolLocal

    fun esCuidador(): Boolean = rol() == Rol.CUIDADOR

    fun inicio(): Destino = if (esCuidador()) Destino.CuidadorHome else Destino.PacienteHome

    fun ir(d: Destino) {
        pila.add(d)
    }

    fun volver() {
        if (pila.size > 1) pila.removeAt(pila.lastIndex)
    }

    fun irRaiz(d: Destino) {
        pila.clear()
        pila.add(d)
    }

    fun cambiarEscala(valor: Float) {
        escala = valor
        almacen.escribir("escala", valor.toString())
    }

    /** Lanza trabajo en segundo plano y ensena el error (si lo hay) como mensaje. */
    fun ejecutar(bloque: suspend () -> Unit) {
        scope.launch {
            try {
                bloque()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mensaje = e.message ?: "Ha ocurrido un error."
            }
        }
    }
}
