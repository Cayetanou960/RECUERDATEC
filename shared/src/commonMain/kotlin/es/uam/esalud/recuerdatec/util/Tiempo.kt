package es.uam.esalud.recuerdatec.util

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

fun ahoraMillis(): Long = Clock.System.now().toEpochMilliseconds()

fun ahoraLocal(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

private fun dos(n: Int): String = n.toString().padStart(2, '0')

fun hoyIso(): String {
    val d = ahoraLocal()
    return "${d.year}-${dos(d.monthNumber)}-${dos(d.dayOfMonth)}"
}

fun horaTexto(): String {
    val d = ahoraLocal()
    return "${dos(d.hour)}:${dos(d.minute)}"
}

fun minutosAhora(): Int {
    val d = ahoraLocal()
    return d.hour * 60 + d.minute
}

fun minutosDeHora(hora: String): Int? {
    val p = hora.trim().split(":")
    if (p.size != 2) return null
    val h = p[0].trim().toIntOrNull() ?: return null
    val m = p[1].trim().toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}

/** Devuelve la hora como HH:mm o null si no es valida. */
fun normalizarHora(hora: String): String? =
    minutosDeHora(hora)?.let { "${dos(it / 60)}:${dos(it % 60)}" }

fun fechaValida(fecha: String): Boolean = runCatching { LocalDate.parse(fecha.trim()) }.isSuccess

fun nuevoId(): String = ahoraMillis().toString(36) + Random.nextInt(0, 1_000_000).toString(36)

private val DIAS = listOf("lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo")
private val DIAS_ACENTO = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
private val MESES = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

/** Ej.: "sábado, 31 de octubre de 2026". */
fun fechaLarga(iso: String): String {
    val f = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return iso
    return "${DIAS_ACENTO[f.dayOfWeek.ordinal]}, ${f.dayOfMonth} de ${MESES[f.monthNumber - 1]} de ${f.year}"
}

fun fechaCorta(iso: String): String {
    val f = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return iso
    return "${f.dayOfMonth} ${MESES[f.monthNumber - 1].take(3)}"
}

fun fechaDeMillis(millis: Long): String {
    val d = kotlinx.datetime.Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dos(d.dayOfMonth)}/${dos(d.monthNumber)} ${dos(d.hour)}:${dos(d.minute)}"
}

fun diaDeMillis(millis: Long): String {
    val d = kotlinx.datetime.Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${d.year}-${dos(d.monthNumber)}-${dos(d.dayOfMonth)}"
}

/** Fecha ISO de hace `dias` dias. */
fun fechaHaceDias(dias: Int): String =
    diaDeMillis(ahoraMillis() - dias * 24L * 60 * 60 * 1000)

@OptIn(ExperimentalEncodingApi::class)
fun aBase64(bytes: ByteArray): String = Base64.encode(bytes)

@OptIn(ExperimentalEncodingApi::class)
fun deBase64(texto: String): ByteArray = Base64.decode(texto)

