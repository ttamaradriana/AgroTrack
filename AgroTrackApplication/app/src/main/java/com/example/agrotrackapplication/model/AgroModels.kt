package com.example.agrotrackapplication.model

data class AnimalItem(
    val id: Int,
    val nombre: String,
    val emoji: String,
    var cantidad: Int,
    val vacunaInfo: String,
    val dieta: String,
    val productoAsociado: String,
    val unidadProducto: String,
    var alimentadoHoy: Boolean = false,
    var vacunado: Boolean = false
)

data class CultivoItem(
    val id: Int,
    val nombre: String,
    val emoji: String,
    var metros: Double,
    val podaInfo: String,
    val productoAsociado: String,
    val unidadProducto: String,
    val mesesSiembra: List<Int>,    // 1..12
    val mesesCrecimiento: List<Int>,
    val mesesCosecha: List<Int>,
    var regadoHoy: Boolean = false,
    var podado: Boolean = false
) {
    fun obtenerFaseEnMes(mes: Int): String {
        return when {
            mes in mesesCosecha -> "Cosecha"
            mes in mesesSiembra -> "Siembra"
            else -> "Crecimiento"
        }
    }
}

data class ProductoInventario(
    val nombre: String,
    val unidad: String,
    val emoji: String,
    val categoria: String, // "ANIMAL" o "CULTIVO"
    var cantidadRecogida: Int = 0
)