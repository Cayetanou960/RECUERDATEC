package es.uam.esalud.recuerdatec.logica

import es.uam.esalud.recuerdatec.model.EventoAgenda
import es.uam.esalud.recuerdatec.model.Medicamento
import es.uam.esalud.recuerdatec.model.Recuerdo
import es.uam.esalud.recuerdatec.model.ResultadoEjercicio
import es.uam.esalud.recuerdatec.model.Toma
import es.uam.esalud.recuerdatec.util.minutosDeHora
import kotlin.random.Random

// ---------- agenda ----------

fun eventosDeHoy(agenda: List<EventoAgenda>, hoy: String): List<EventoAgenda> =
    agenda.filter { it.fecha == hoy }.sortedBy { it.hora }

/** Eventos de hoy en adelante, ordenados por fecha y hora. */
fun proximosEventos(agenda: List<EventoAgenda>, hoy: String): List<EventoAgenda> =
    agenda.filter { it.fecha >= hoy }.sortedWith(compareBy({ it.fecha }, { it.hora }))

// ---------- medicacion ----------

data class TomaPendiente(
    val medicamento: Medicamento,
    val hora: String,
    val retrasada: Boolean,
)

/** Tomas de hoy que aun no se han confirmado. `retrasada` si ya paso mas de media hora. */
fun tomasPendientes(
    medicamentos: List<Medicamento>,
    tomas: List<Toma>,
    hoy: String,
    minutosAhora: Int,
): List<TomaPendiente> {
    val hechas = tomas.filter { it.fecha == hoy }.map { it.medicamentoId to it.hora }.toSet()
    return medicamentos.flatMap { m ->
        m.horas.mapNotNull { h ->
            val min = minutosDeHora(h) ?: return@mapNotNull null
            if ((m.id to h) in hechas) null else TomaPendiente(m, h, minutosAhora > min + 30)
        }
    }.sortedBy { it.hora }
}

/** Porcentaje (0..100) de tomas confirmadas sobre las programadas en las fechas dadas. */
fun adherencia(medicamentos: List<Medicamento>, tomas: List<Toma>, fechas: List<String>): Int {
    val previstas = medicamentos.sumOf { it.horas.size } * fechas.size
    if (previstas == 0) return 0
    val hechas = tomas.count { it.fecha in fechas }
    return (hechas * 100 / previstas).coerceAtMost(100)
}

// ---------- resultados ----------

fun porcentajeAciertos(resultados: List<ResultadoEjercicio>): Int {
    val intentos = resultados.sumOf { it.intentos }
    if (intentos == 0) return 0
    return resultados.sumOf { it.aciertos } * 100 / intentos
}

// ---------- ejercicio: parejas ----------

data class Carta(
    val id: Int,
    val par: String,
    val visible: Boolean = false,
    val resuelta: Boolean = false,
)

data class EstadoParejas(
    val cartas: List<Carta>,
    val intentos: Int = 0,
    val aciertos: Int = 0,
    val seleccion: List<Int> = emptyList(),
    val inicioMillis: Long = 0,
) {
    val completado: Boolean get() = cartas.isNotEmpty() && cartas.all { it.resuelta }
    val hayFallo: Boolean get() = seleccion.size == 2
}

private val PALABRAS_POR_DEFECTO = listOf("Sol", "Flor", "Casa", "Perro", "Libro", "Taza")

/** Crea un juego con hasta 6 parejas, usando los titulos de los recuerdos cuando hay. */
fun nuevoJuego(recuerdos: List<Recuerdo>, inicioMillis: Long, azar: Random = Random): EstadoParejas {
    val propios = recuerdos.map { it.titulo.trim() }.filter { it.isNotEmpty() }.distinct()
    val palabras = (propios + PALABRAS_POR_DEFECTO.filter { it !in propios }).take(6)
    val cartas = (palabras + palabras).shuffled(azar).mapIndexed { i, p -> Carta(i, p) }
    return EstadoParejas(cartas, inicioMillis = inicioMillis)
}

fun tocarCarta(e: EstadoParejas, indice: Int): EstadoParejas {
    if (e.hayFallo || indice !in e.cartas.indices) return e
    val c = e.cartas[indice]
    if (c.resuelta || c.visible) return e
    val cartas = e.cartas.toMutableList()
    cartas[indice] = c.copy(visible = true)
    val sel = e.seleccion + indice
    if (sel.size < 2) return e.copy(cartas = cartas, seleccion = sel)
    val a = cartas[sel[0]]
    val b = cartas[sel[1]]
    return if (a.par == b.par) {
        cartas[sel[0]] = a.copy(resuelta = true)
        cartas[sel[1]] = b.copy(resuelta = true)
        e.copy(cartas = cartas, intentos = e.intentos + 1, aciertos = e.aciertos + 1, seleccion = emptyList())
    } else {
        e.copy(cartas = cartas, intentos = e.intentos + 1, seleccion = sel)
    }
}

/** Da la vuelta a las dos cartas del fallo. */
fun ocultarFallo(e: EstadoParejas): EstadoParejas {
    if (!e.hayFallo) return e
    val cartas = e.cartas.map { if (it.id in e.seleccion) it.copy(visible = false) else it }
    return e.copy(cartas = cartas, seleccion = emptyList())
}

// ---------- ejercicio: quien es ----------

data class PreguntaQuien(
    val recuerdo: Recuerdo,
    val opciones: List<String>,
    val correcta: String,
)

private val NOMBRES_POR_DEFECTO = listOf("Ana", "Luis", "Marta", "Pedro", "Rosa", "Juan")

/** Preguntas con los recuerdos que tienen foto y nombre de persona. */
fun preguntasQuien(recuerdos: List<Recuerdo>, azar: Random = Random, maximo: Int = 5): List<PreguntaQuien> {
    val validos = recuerdos.filter { it.foto.isNotBlank() && it.persona.isNotBlank() }.shuffled(azar).take(maximo)
    val personas = recuerdos.map { it.persona.trim() }.filter { it.isNotEmpty() }.distinct()
    return validos.map { r ->
        val correcta = r.persona.trim()
        val otros = (personas + NOMBRES_POR_DEFECTO).filter { it != correcta }.distinct().shuffled(azar).take(2)
        PreguntaQuien(r, (otros + correcta).shuffled(azar), correcta)
    }
}
