package com.example.agrotrackapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.agrotrackapplication.api.ApiClient
import com.example.agrotrackapplication.model.Lote
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : ComponentActivity() {

    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerViewLotes)
        recyclerView.layoutManager = LinearLayoutManager(this)

        cargarLotes()

        val etNombre = findViewById<android.widget.EditText>(R.id.etNombreLote)
        val etSuperficie = findViewById<android.widget.EditText>(R.id.etSuperficie)
        val btnGuardar = findViewById<android.widget.Button>(R.id.btnGuardarLote)

        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString()
            val superficie = etSuperficie.text.toString().toDoubleOrNull() ?: 0.0

            if (nombre.isNotEmpty() && superficie > 0) {
                val nuevoLote = Lote(
                    id = null,
                    nombre = nombre,
                    superficie = superficie,
                    estado = "Activo"
                )
                ApiClient.instance.guardarLote(nuevoLote).enqueue(object : Callback<Lote> {
                    override fun onResponse(call: Call<Lote>, response: Response<Lote>) {
                        if (response.isSuccessful) {
                            etNombre.text.clear()
                            etSuperficie.text.clear()
                            cargarLotes() // Recarga la lista automáticamente
                        }
                    }
                    override fun onFailure(call: Call<Lote>, t: Throwable) {
                        Log.e("AGROTRACK", "Error al guardar: ${t.message}")
                    }
                })
            }
        }
    }

    private fun cargarLotes() {
        ApiClient.instance.obtenerLotes().enqueue(object : Callback<List<Lote>> {
            override fun onResponse(call: Call<List<Lote>>, response: Response<List<Lote>>) {
                if (response.isSuccessful) {
                    val lotes = response.body() ?: emptyList()
                    recyclerView.adapter = LoteAdapter(lotes)
                } else {
                    Log.e("AGROTRACK", "Error en servidor: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<Lote>>, t: Throwable) {
                Log.e("AGROTRACK", "Fallo de conexión: ${t.message}")
            }
        })
    }
}