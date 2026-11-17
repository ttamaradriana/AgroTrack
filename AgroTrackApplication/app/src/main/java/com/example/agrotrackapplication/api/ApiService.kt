package com.example.agrotrackapplication.api

import com.example.agrotrackapplication.model.Lote
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @GET("lotes")
    fun obtenerLotes(): Call<List<Lote>>

    @POST("api/lotes")
    fun guardarLote(@Body lote: Lote): Call<Lote>

    @POST("lotes")
    fun crearLote(@Body lote: Lote): Call<Lote>
}