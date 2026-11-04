package es.uam.esalud.recuerdatec

import es.uam.esalud.recuerdatec.logica.adherencia
import es.uam.esalud.recuerdatec.logica.nuevoJuego
import es.uam.esalud.recuerdatec.logica.ocultarFallo
import es.uam.esalud.recuerdatec.logica.proximosEventos
import es.uam.esalud.recuerdatec.logica.tocarCarta
import es.uam.esalud.recuerdatec.logica.tomasPendientes
import es.uam.esalud.recuerdatec.model.EventoAgenda
import es.uam.esalud.recuerdatec.model.Medicamento
import es.uam.esalud.recuerdatec.model.Toma
import es.uam.esalud.recuerdatec.util.fechaValida
import es.uam.esalud.recuerdatec.util.normalizarHora
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LogicaTest {
    @Test
    fun horasSeNormalizan() {
        assertEquals("08:05", normalizarHora("8:05"))
        assertNull(normalizarHora("25:00"))
        assertNull(normalizarHora("abc"))
    }

    @Test
    fun fechasSeValidan() {
        assertTrue(fechaValida("2026-11-26"))
        assertFalse(fechaValida("26/11/2026"))
    }

    @Test
    fun tomasPendientesIgnoranLasConfirmadas() {
        val m = Medicamento("m1", "Donepezilo", "5 mg", listOf("08:00", "20:00"))
        val hecha = Toma("t1", "m1", "Donepezilo", "2026-11-04", "08:00", 0)
        val p = tomasPendientes(listOf(m), listOf(hecha), "2026-11-04", 9 * 60)
        assertEquals(1, p.size)
        assertEquals("20:00", p[0].hora)
        assertFalse(p[0].retrasada)
    }

    @Test
    fun tomaRetrasadaPasadaLaMediaHora() {
        val m = Medicamento("m1", "X", "", listOf("08:00"))
        val p = tomasPendientes(listOf(m), emptyList(), "2026-11-04", 8 * 60 + 31)
        assertTrue(p.single().retrasada)
    }

    @Test
    fun adherenciaCalculaPorcentaje() {
        val m = Medicamento("m1", "X", "", listOf("08:00", "20:00"))
        val t = Toma("t", "m1", "X", "2026-11-04", "08:00", 0)
        assertEquals(50, adherencia(listOf(m), listOf(t), listOf("2026-11-04")))
    }

    @Test
    fun agendaOrdenadaYSinPasado() {
        val a = listOf(
            EventoAgenda("1", "Pasado", "2026-11-01"),
            EventoAgenda("2", "Tarde", "2026-11-04", "18:00"),
            EventoAgenda("3", "Pronto", "2026-11-04", "09:00"),
        )
        assertEquals(listOf("Pronto", "Tarde"), proximosEventos(a, "2026-11-04").map { it.titulo })
    }

    @Test
    fun parejasSeCompletan() {
        var e = nuevoJuego(emptyList(), 0)
        assertEquals(12, e.cartas.size)
        val porPalabra = e.cartas.groupBy { it.par }
        for ((_, cs) in porPalabra) {
            e = tocarCarta(e, cs[0].id)
            e = tocarCarta(e, cs[1].id)
        }
        assertTrue(e.completado)
        assertEquals(6, e.aciertos)
        assertEquals(6, e.intentos)
    }

    @Test
    fun parejasFalloSeOculta() {
        var e = nuevoJuego(emptyList(), 0)
        val a = e.cartas[0]
        val b = e.cartas.first { it.par != a.par }
        e = tocarCarta(e, a.id)
        e = tocarCarta(e, b.id)
        assertTrue(e.hayFallo)
        // No se puede tocar una tercera carta mientras hay fallo.
        val c = e.cartas.first { it.id != a.id && it.id != b.id }
        assertEquals(e, tocarCarta(e, c.id))
        e = ocultarFallo(e)
        assertFalse(e.hayFallo)
        assertTrue(e.cartas.none { it.visible })
    }
}
