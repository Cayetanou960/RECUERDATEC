package es.uam.esalud.recuerdatec.data

/** Almacen clave/valor persistente en el dispositivo (cada plataforma tiene el suyo). */
interface AlmacenLocal {
    fun leer(clave: String): String?
    fun escribir(clave: String, valor: String)
    fun borrar(clave: String)
}

expect fun crearAlmacenLocal(): AlmacenLocal
