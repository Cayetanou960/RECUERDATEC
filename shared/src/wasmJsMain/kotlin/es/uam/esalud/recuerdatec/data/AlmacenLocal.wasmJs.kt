package es.uam.esalud.recuerdatec.data

import kotlinx.browser.localStorage

actual fun crearAlmacenLocal(): AlmacenLocal = object : AlmacenLocal {
    override fun leer(clave: String): String? = localStorage.getItem("recuerdatec_$clave")
    override fun escribir(clave: String, valor: String) {
        localStorage.setItem("recuerdatec_$clave", valor)
    }
    override fun borrar(clave: String) {
        localStorage.removeItem("recuerdatec_$clave")
    }
}
