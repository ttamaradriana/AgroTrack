package com.example.agrotrackapplication.model

data class AnimalItem(
    val id: Int,
    val nombre: String,
    val emoji: String,
    var cantidad: Int,
    var vacuna: String,
    var comida: String,
    var alimentadoHoy: Boolean = false
)

data class CultivoItem(
    val id: Int,
    val nombre: String,
    val emoji: String,
    val tipoCosecha: String,
    val hectareas: Double,
    var regadoHoy: Boolean = false
)

data class ProductoInventario(
    val nombre: String,
    val unidad: String,
    val emoji: String,
    var cantidadRecogida: Int = 0
)