package com.example.agrotrackapplication

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.agrotrackapplication.model.AnimalItem
import com.example.agrotrackapplication.model.CultivoItem
import com.example.agrotrackapplication.model.ProductoInventario
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var pantallaActual = "MENU"

    private val catalogoAnimales = listOf(
        AnimalItem(1, "Vacas", "🐄", 32, "Brucelosis Bovina", "Forraje verde", "Leche fresca", "litros"),
        AnimalItem(2, "Gallinas", "🐔", 150, "Newcastle", "Maíz y grano", "Huevos de corral", "huevos"),
        AnimalItem(3, "Ovejas", "🐑", 65, "Enterotoxemia", "Pasto y heno", "Lana y leche", "kg"),
        AnimalItem(4, "Cerdos", "🐖", 40, "Peste Porcina", "Pienso compuesto", "Carne porcina", "kg"),
        AnimalItem(5, "Cabras", "🐐", 28, "Agalaxia Contagiosa", "Heno de alfalfa", "Leche de cabra", "litros"),
        AnimalItem(6, "Caballos", "🐎", 6, "Tétanos e Influenza", "Avena y paja", "Estiércol compost", "sacos"),
        AnimalItem(7, "Conejos", "🐇", 85, "Mixomatosis", "Granulado vegetal", "Carne conejo", "kg"),
        AnimalItem(8, "Patos", "🦆", 30, "Cólera Aviar", "Grano húmedo", "Huevos de pato", "docenas"),
        AnimalItem(9, "Pavos", "🦃", 22, "Viruela Aviar", "Pienso proteico", "Carne pavo", "kg"),
        AnimalItem(10, "Abejas", "🐝", 18, "Tratamiento Varroa", "Polen y jarabe", "Miel pura", "kg")
    )

    private val db by lazy { AgroDatabase.getInstance(this) }
    private val syncRepo by lazy { AgroSyncRepository(db.loteSyncDao(), AgroRetrofitClient.api) }

    private val prefsSesion by lazy { getSharedPreferences("agro_sesion_usuario", MODE_PRIVATE) }

    private var usuarioActualId: Long
        get() = prefsSesion.getLong("usuario_id", -1L)
        set(value) { prefsSesion.edit().putLong("usuario_id", value).apply() }

    private var nombreUsuarioActual: String
        get() = prefsSesion.getString("usuario_nombre", "Invitado") ?: "Invitado"
        set(value) { prefsSesion.edit().putString("usuario_nombre", value).apply() }

    private var emailUsuarioActual: String
        get() = prefsSesion.getString("usuario_email", "") ?: ""
        set(value) { prefsSesion.edit().putString("usuario_email", value).apply() }

    private var idiomaApp: String
        get() = prefsSesion.getString("idioma_app", "es") ?: "es"
        set(value) { prefsSesion.edit().putString("idioma_app", value).apply() }

    private fun registrarCambioAnimalEnRoom(animal: AnimalItem, estado: String = "ACTIVO") {
        if (usuarioActualId <= 0L) return
        lifecycleScope.launch {
            syncRepo.guardarCambioLocal(
                localKey = "animal_${animal.id}",
                usuarioId = usuarioActualId,
                especieId = animal.id.toLong(),
                nombreLote = animal.nombre,
                cantidad = animal.cantidad.toDouble(),
                estado = estado
            )
        }
    }

    private fun registrarCambioCultivoEnRoom(cultivo: CultivoItem, estado: String = "ACTIVO") {
        if (usuarioActualId <= 0L) return
        lifecycleScope.launch {
            syncRepo.guardarCambioLocal(
                localKey = "cultivo_${cultivo.id}",
                usuarioId = usuarioActualId,
                especieId = cultivo.id.toLong(),
                nombreLote = cultivo.nombre,
                cantidad = cultivo.metros,
                estado = estado
            )
        }
    }

    private fun sincronizarConServidor() {
        if (usuarioActualId <= 0L) return
        lifecycleScope.launch {
            val resultado = syncRepo.sincronizar(usuarioActualId)
            if (resultado.isSuccess) {
                Toast.makeText(this@MainActivity, "Sincronización completada", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Sin conexión o error de servidor (guardado en local)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun hashPassword(texto: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256")
            .digest(texto.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun mostrarDialogoAuth(
        obligatorio: Boolean = true,
        onLoginExitoso: () -> Unit = {}
    ) {
        var modoRegistro = false

        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F4F8F4"))
            setPadding(40, 36, 40, 28)
        }

        val tvCabecera = TextView(this).apply {
            text = "🌾 Cuenta AgroTrack"
            textSize = 21f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#1B4332"))
        }

        val tvSubtitulo = TextView(this).apply {
            text = "Accede a tu explotación o crea una cuenta nueva"
            textSize = 13f
            setTextColor(Color.parseColor("#4F6D5B"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 6; bottomMargin = 20 }
        }

        val selectorModo = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 20 }
        }

        val btnTabLogin = Button(this).apply {
            text = "Iniciar sesión"
            textSize = 13f
            transformationMethod = null
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginEnd = 10 }
        }

        val btnTabRegistro = Button(this).apply {
            text = "Registrarme"
            textSize = 13f
            transformationMethod = null
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginStart = 10 }
        }

        selectorModo.addView(btnTabLogin)
        selectorModo.addView(btnTabRegistro)

        fun estiloCampo(hintTexto: String, inputTypeFlag: Int): EditText {
            return EditText(this).apply {
                hint = hintTexto
                inputType = inputTypeFlag
                textSize = 14f
                setTextColor(Color.parseColor("#1B2E1E"))
                setHintTextColor(Color.parseColor("#7A8E7E"))
                setBackgroundResource(R.drawable.bg_bento_card)
                setPadding(32, 24, 32, 24)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 14 }
            }
        }

        val etNombre = estiloCampo(
            "Nombre completo",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        )
        val etEmail = estiloCampo(
            "Correo electrónico",
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        )

        val filaPassword = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 14 }
        }

        val etPassword = estiloCampo(
            "Contraseña",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        ).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply { marginEnd = 10 }
        }

        var passwordVisible = false
        val btnVerPassword = Button(this).apply {
            text = "👁"
            textSize = 16f
            transformationMethod = null
            setBackgroundColor(Color.parseColor("#E8F5E9"))
            setTextColor(Color.parseColor("#1B5E20"))
            minWidth = 0
            minimumWidth = 0
            setPadding(24, 18, 24, 18)
            setOnClickListener {
                passwordVisible = !passwordVisible
                val posCursor = etPassword.selectionEnd.coerceAtLeast(0)
                if (passwordVisible) {
                    etPassword.inputType =
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    text = "🙈"
                } else {
                    etPassword.inputType =
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    text = "👁"
                }
                etPassword.setSelection(posCursor.coerceAtMost(etPassword.text.length))
            }
        }

        filaPassword.addView(etPassword)
        filaPassword.addView(btnVerPassword)

        val tvErrorAuth = TextView(this).apply {
            visibility = View.GONE
            textSize = 13f
            setTextColor(Color.parseColor("#B71C1C"))
            setBackgroundColor(Color.parseColor("#FFEBEE"))
            setPadding(24, 16, 24, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }

        val btnAccionPrincipal = Button(this).apply {
            text = "Entrar a mi cuenta"
            textSize = 14f
            transformationMethod = null
            setBackgroundColor(Color.parseColor("#2E7D32"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        fun aplicarModoVisual() {
            tvErrorAuth.visibility = View.GONE
            if (modoRegistro) {
                etNombre.visibility = View.VISIBLE
                btnAccionPrincipal.text = "Crear cuenta"
                btnTabRegistro.setBackgroundColor(Color.parseColor("#2E7D32"))
                btnTabRegistro.setTextColor(Color.WHITE)
                btnTabLogin.setBackgroundColor(Color.parseColor("#E8F5E9"))
                btnTabLogin.setTextColor(Color.parseColor("#1B5E20"))
            } else {
                etNombre.visibility = View.GONE
                btnAccionPrincipal.text = "Entrar a mi cuenta"
                btnTabLogin.setBackgroundColor(Color.parseColor("#2E7D32"))
                btnTabLogin.setTextColor(Color.WHITE)
                btnTabRegistro.setBackgroundColor(Color.parseColor("#E8F5E9"))
                btnTabRegistro.setTextColor(Color.parseColor("#1B5E20"))
            }
        }

        btnTabLogin.setOnClickListener {
            modoRegistro = false
            aplicarModoVisual()
        }
        btnTabRegistro.setOnClickListener {
            modoRegistro = true
            aplicarModoVisual()
        }

        raiz.addView(tvCabecera)
        raiz.addView(tvSubtitulo)
        raiz.addView(selectorModo)
        raiz.addView(etNombre)
        raiz.addView(etEmail)
        raiz.addView(filaPassword)
        raiz.addView(tvErrorAuth)
        raiz.addView(btnAccionPrincipal)

        aplicarModoVisual()

        val dialogo = AlertDialog.Builder(this)
            .setView(raiz)
            .setCancelable(!obligatorio)
            .create()

        fun mostrarError(msg: String) {
            tvErrorAuth.text = "⚠️ $msg"
            tvErrorAuth.visibility = View.VISIBLE
        }

        btnAccionPrincipal.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString()

            if (modoRegistro) {
                if (nombre.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                    mostrarError("Rellena nombre, correo y contraseña")
                    return@setOnClickListener
                }
                btnAccionPrincipal.isEnabled = false
                lifecycleScope.launch {
                    try {
                        val resp = AgroRetrofitClient.api.registrarUsuario(
                            UsuarioDto(
                                nombre = nombre,
                                email = email,
                                passwordHash = hashPassword(pass),
                                rol = "USER"
                            )
                        )
                        if (resp.isSuccessful && resp.body()?.id != null) {
                            val user = resp.body()!!
                            usuarioActualId = user.id!!
                            nombreUsuarioActual = user.nombre
                            emailUsuarioActual = user.email
                            dialogo.dismiss()
                            Toast.makeText(this@MainActivity, "Cuenta creada: ${user.nombre}", Toast.LENGTH_SHORT).show()
                            onLoginExitoso()
                            sincronizarConServidor()
                        } else {
                            mostrarError("No se pudo registrar. Comprueba el correo o si ya existe.")
                        }
                    } catch (_: Exception) {
                        mostrarError("Error de conexión con el servidor")
                    } finally {
                        btnAccionPrincipal.isEnabled = true
                    }
                }
            } else {
                if (email.isEmpty() || pass.isEmpty()) {
                    mostrarError("Introduce tu correo y contraseña")
                    return@setOnClickListener
                }
                btnAccionPrincipal.isEnabled = false
                lifecycleScope.launch {
                    try {
                        val resp = AgroRetrofitClient.api.loginUsuario(
                            LoginRequest(email = email, password = pass)
                        )
                        if (resp.isSuccessful && resp.body()?.id != null) {
                            val user = resp.body()!!
                            usuarioActualId = user.id!!
                            nombreUsuarioActual = user.nombre
                            emailUsuarioActual = user.email
                            dialogo.dismiss()
                            Toast.makeText(this@MainActivity, "Bienvenido, ${user.nombre}", Toast.LENGTH_SHORT).show()
                            onLoginExitoso()
                            sincronizarConServidor()
                        } else {
                            mostrarError("Credenciales incorrectas. Revisa correo y contraseña.")
                        }
                    } catch (_: Exception) {
                        mostrarError("No se pudo conectar para validar credenciales")
                    } finally {
                        btnAccionPrincipal.isEnabled = true
                    }
                }
            }
        }

        dialogo.show()
    }

    private val catalogoCultivos = listOf(
        CultivoItem(1, "Tomates", "🍅", 25000.0, "Poda de brotes", "Tomates", "kg", listOf(2, 3), listOf(4, 5, 6), listOf(7, 8, 9, 10, 11)),
        CultivoItem(2, "Trigo", "🌾", 150000.0, "Siega estacional", "Trigo", "kg", listOf(10, 11), listOf(12, 1, 2, 3, 4, 5), listOf(6, 7)),
        CultivoItem(3, "Maíz", "🌽", 100000.0, "Aclareo hojas", "Maíz", "kg", listOf(4, 5), listOf(6, 7, 8), listOf(9, 10)),
        CultivoItem(4, "Patatas", "🥔", 40000.0, "Aporcado tierra", "Patatas", "kg", listOf(2, 3), listOf(4, 5, 6), listOf(7, 8, 9, 11)),
        CultivoItem(5, "Olivos", "🫒", 200000.0, "Poda anual", "Aceitunas", "kg", listOf(3, 4), listOf(5, 6, 7, 8, 9, 10), listOf(11, 12, 1)),
        CultivoItem(6, "Vid (Uvas)", "🍇", 120000.0, "Poda sarmientos", "Uvas", "kg", listOf(2, 3), listOf(4, 5, 6, 7, 8), listOf(9, 10)),
        CultivoItem(7, "Lechugas", "🥬", 12000.0, "Limpieza hojas", "Lechugas", "uds", listOf(2, 3, 8, 9), listOf(4, 10), listOf(5, 6, 11, 12)),
        CultivoItem(8, "Cebollas", "\uD83E\uDDC5", 18000.0, "Sin poda", "Cebollas", "kg", listOf(1, 2, 9), listOf(3, 4, 5, 10, 11), listOf(6, 7, 8)),
        CultivoItem(9, "Manzanos", "🍎", 55000.0, "Poda fructificación", "Manzanas", "kg", listOf(2, 3), listOf(4, 5, 6, 7), listOf(8, 9, 10)),
        CultivoItem(10, "Girasoles", "🌻", 80000.0, "Despuntado", "Pipas girasol", "kg", listOf(3, 4), listOf(5, 6, 7), listOf(8, 9))
    )

    private val listaInventario = mutableListOf(
        ProductoInventario("Leche fresca", "litros", "🥛", "ANIMAL", 240),
        ProductoInventario("Huevos de corral", "huevos", "🥚", "ANIMAL", 18),
        ProductoInventario("Lana y leche", "kg", "🧶", "ANIMAL", 50),
        ProductoInventario("Leche de cabra", "litros", "🍶", "ANIMAL", 65),
        ProductoInventario("Miel pura", "kg", "🍯", "ANIMAL", 15),
        ProductoInventario("Tomates", "kg", "🍅", "CULTIVO", 85),
        ProductoInventario("Patatas", "kg", "🥔", "CULTIVO", 300),
        ProductoInventario("Aceitunas", "kg", "🫒", "CULTIVO", 500),
        ProductoInventario("Manzanas", "kg", "🍎", "CULTIVO", 120),
        ProductoInventario("Cebollas", "kg", "\uD83E\uDDC5", "CULTIVO", 95),
        ProductoInventario("Trigo", "kg", "🌾", "CULTIVO", 1200),
        ProductoInventario("Uvas", "kg", "🍇", "CULTIVO", 400)
    )

    private fun formatoM2(valor: Double): String {
        val entero = valor.toLong()
        return if (valor == entero.toDouble()) "$entero m²" else "$valor m²"
    }

    private fun animarEntrada(vista: View) {
        vista.alpha = 0f
        vista.visibility = View.VISIBLE
        vista.animate().alpha(1f).setDuration(220).start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = 0

        val storage = AgroStorage(this)
        storage.cargarEstados(catalogoAnimales, catalogoCultivos, listaInventario)

        val layoutMenu = findViewById<ScrollView>(R.id.layoutMenuPrincipal)
        val layoutSeccionGrid = findViewById<LinearLayout>(R.id.layoutSeccionGrid)
        val recyclerGrid = findViewById<RecyclerView>(R.id.recyclerGrid)
        val layoutDetalle = findViewById<ScrollView>(R.id.layoutDetalle)
        val layoutRegistro = findViewById<ScrollView>(R.id.layoutRegistro)
        val layoutInv = findViewById<LinearLayout>(R.id.layoutInventario)
        val btnVolver = findViewById<Button>(R.id.btnVolver)
        val tvTitulo = findViewById<TextView>(R.id.tvTituloPantalla)

        recyclerGrid.layoutManager = GridLayoutManager(this, 2)

        fun animalesDelCliente(): List<AnimalItem> {
            val ids = storage.obtenerIdsAnimalesElegidos()
            return catalogoAnimales.filter { ids.contains(it.id.toString()) }
        }

        fun cultivosDelCliente(): List<CultivoItem> {
            val ids = storage.obtenerIdsCultivosElegidos()
            return catalogoCultivos.filter { ids.contains(it.id.toString()) }
        }

        fun ocultarTodo() {
            layoutMenu.visibility = View.GONE
            layoutSeccionGrid.visibility = View.GONE
            layoutDetalle.visibility = View.GONE
            layoutRegistro.visibility = View.GONE
            layoutInv.visibility = View.GONE
        }

        val contenedorMenuInterno = layoutMenu.getChildAt(0) as? LinearLayout

        lateinit var refrescarMenuPrincipal: () -> Unit

        fun abrirPantallaPerfil() {
            if (usuarioActualId <= 0L) {
                mostrarDialogoAuth(obligatorio = true) { refrescarMenuPrincipal() }
                return
            }

            ocultarTodo()
            pantallaActual = "PERFIL"
            animarEntrada(layoutDetalle)
            btnVolver.visibility = View.VISIBLE
            tvTitulo.text = "👤 Mi Perfil"

            findViewById<TextView>(R.id.tvDetalleEmoji).text = "👤"
            findViewById<TextView>(R.id.tvDetalleNombre).text = nombreUsuarioActual
            val tvInfoPerfil = findViewById<TextView>(R.id.tvDetalleCantidad)
            tvInfoPerfil.text =
                "Correo: ${emailUsuarioActual.ifBlank { "No registrado" }} • ID #$usuarioActualId"

            findViewById<LinearLayout>(R.id.cajaRecogidaDetalle).visibility = View.GONE
            findViewById<Button>(R.id.btnTareaDiariaDetalle).visibility = View.GONE
            findViewById<Button>(R.id.btnVacunaPodaDetalle).visibility = View.GONE

            val padrePerfil = tvInfoPerfil.parent as LinearLayout
            padrePerfil.findViewWithTag<View>("botonesGestionAnimal")
                ?.let { padrePerfil.removeView(it) }
            padrePerfil.findViewWithTag<View>("botonesGestionCultivo")
                ?.let { padrePerfil.removeView(it) }
            padrePerfil.findViewWithTag<View>("accionesPerfilExtra")
                ?.let { padrePerfil.removeView(it) }

            val cajaAcciones = LinearLayout(this).apply {
                tag = "accionesPerfilExtra"
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 20 }
            }

            fun crearBotonPerfil(
                texto: String,
                bgHex: String,
                fgHex: String,
                accion: () -> Unit
            ): Button {
                return Button(this).apply {
                    text = texto
                    textSize = 13f
                    transformationMethod = null
                    setBackgroundColor(Color.parseColor(bgHex))
                    setTextColor(Color.parseColor(fgHex))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 14 }
                    setOnClickListener { accion() }
                }
            }

            cajaAcciones.addView(
                crearBotonPerfil("✏️ Cambiar credenciales", "#E8F5E9", "#1B5E20") {
                    val form = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(48, 24, 48, 12)
                    }
                    val inNombre = EditText(this).apply {
                        hint = "Nuevo nombre"
                        setText(nombreUsuarioActual)
                    }
                    val inEmail = EditText(this).apply {
                        hint = "Nuevo correo"
                        inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        setText(emailUsuarioActual)
                    }
                    val inPass = EditText(this).apply {
                        hint = "Nueva contraseña (opcional)"
                        inputType =
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    }
                    form.addView(inNombre)
                    form.addView(inEmail)
                    form.addView(inPass)

                    AlertDialog.Builder(this)
                        .setTitle("Cambiar credenciales")
                        .setView(form)
                        .setPositiveButton("Guardar") { _, _ ->
                            val nuevoNombre = inNombre.text.toString().trim()
                            val nuevoEmail = inEmail.text.toString().trim()
                            val nuevaPass = inPass.text.toString()

                            if (nuevoNombre.isEmpty() || nuevoEmail.isEmpty()) {
                                Toast.makeText(
                                    this,
                                    "Nombre y correo no pueden estar vacíos",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@setPositiveButton
                            }

                            nombreUsuarioActual = nuevoNombre
                            emailUsuarioActual = nuevoEmail
                            tvInfoPerfil.text =
                                "Correo: $emailUsuarioActual • ID #$usuarioActualId"
                            findViewById<TextView>(R.id.tvDetalleNombre).text =
                                nombreUsuarioActual

                            if (nuevaPass.isNotBlank()) {
                                lifecycleScope.launch {
                                    try {
                                        AgroRetrofitClient.api.registrarUsuario(
                                            UsuarioDto(
                                                id = usuarioActualId,
                                                nombre = nuevoNombre,
                                                email = nuevoEmail,
                                                passwordHash = hashPassword(nuevaPass),
                                                rol = "USER"
                                            )
                                        )
                                    } catch (_: Exception) {
                                    }
                                }
                            }
                            refrescarMenuPrincipal()
                            Toast.makeText(
                                this,
                                "Credenciales actualizadas",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            )

            val idiomas = listOf(
                "es" to "Español",
                "en" to "English",
                "ca" to "Català",
                "gl" to "Galego",
                "eu" to "Euskara"
            )
            val nombreIdiomaActual = idiomas.find { it.first == idiomaApp }?.second ?: "Español"
            cajaAcciones.addView(
                crearBotonPerfil("🌐 Idioma: $nombreIdiomaActual", "#FFF8E1", "#E65100") {
                    val etiquetas = idiomas.map { it.second }.toTypedArray()
                    val indiceActual =
                        idiomas.indexOfFirst { it.first == idiomaApp }.coerceAtLeast(0)
                    AlertDialog.Builder(this)
                        .setTitle("Seleccionar idioma")
                        .setSingleChoiceItems(etiquetas, indiceActual) { dlg, which ->
                            val elegido = idiomas[which]
                            idiomaApp = elegido.first
                            val locale = Locale(elegido.first)
                            Locale.setDefault(locale)
                            @Suppress("DEPRECATION")
                            resources.configuration.setLocale(locale)
                            @Suppress("DEPRECATION")
                            resources.updateConfiguration(
                                resources.configuration,
                                resources.displayMetrics
                            )
                            dlg.dismiss()
                            Toast.makeText(
                                this,
                                "Idioma: ${elegido.second}",
                                Toast.LENGTH_SHORT
                            ).show()
                            abrirPantallaPerfil()
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            )

            cajaAcciones.addView(
                crearBotonPerfil("🚪 Salir de sesión", "#FFEBEE", "#B71C1C") {
                    usuarioActualId = -1L
                    nombreUsuarioActual = "Invitado"
                    emailUsuarioActual = ""
                    refrescarMenuPrincipal()
                    mostrarDialogoAuth(obligatorio = true) { refrescarMenuPrincipal() }
                }
            )

            padrePerfil.addView(cajaAcciones)
        }

        refrescarMenuPrincipal = {
            val misAnimales = animalesDelCliente()
            val misCultivos = cultivosDelCliente()

            val calHoy = Calendar.getInstance()
            val mesActual = calHoy.get(Calendar.MONTH) + 1
            val mesesCortos = listOf(
                "ENE",
                "FEB",
                "MAR",
                "ABR",
                "MAY",
                "JUN",
                "JUL",
                "AGO",
                "SEP",
                "OCT",
                "NOV",
                "DIC"
            )
            findViewById<TextView>(R.id.tvMesPortada).text =
                "${mesesCortos[mesActual - 1]} ${calHoy.get(Calendar.YEAR)}"

            val porcA = storage.calcularPorcentajeAnimales(misAnimales)
            findViewById<ProgressBar>(R.id.pbAnimales).progress = porcA
            findViewById<TextView>(R.id.tvPorcentajeAnimales).text = "$porcA% · Tareas completadas"

            val porcC = storage.calcularPorcentajeCultivos(misCultivos)
            findViewById<ProgressBar>(R.id.pbCultivos).progress = porcC
            findViewById<TextView>(R.id.tvPorcentajeCultivos).text = "$porcC% · Tareas completadas"

            val avisosPendientes = mutableListOf<String>()
            for (a in misAnimales) {
                if (!a.vacunado) avisosPendientes.add("💉 ${a.nombre}: ${a.vacunaInfo}")
            }
            for (c in misCultivos) {
                if (!c.podado) avisosPendientes.add("✂️ ${c.nombre}: ${c.podaInfo}")
            }

            val tvAvisosBase = findViewById<TextView>(R.id.tvNotificacionesMenu)
            val contenedorAvisos = tvAvisosBase.parent as LinearLayout
            val indiceBase = contenedorAvisos.indexOfChild(tvAvisosBase)
            val tagExtra = "avisoLineaUnica"

            for (i in contenedorAvisos.childCount - 1 downTo 0) {
                val hijo = contenedorAvisos.getChildAt(i)
                if (hijo.tag == tagExtra) {
                    contenedorAvisos.removeViewAt(i)
                }
            }

            if (avisosPendientes.isEmpty()) {
                tvAvisosBase.visibility = View.VISIBLE
                tvAvisosBase.maxLines = 1
                tvAvisosBase.ellipsize = TextUtils.TruncateAt.END
                tvAvisosBase.text = "✓ Vacunas y podas al día"
            } else {
                tvAvisosBase.visibility = View.VISIBLE
                tvAvisosBase.maxLines = 1
                tvAvisosBase.ellipsize = TextUtils.TruncateAt.END
                tvAvisosBase.text = avisosPendientes.first()

                for (idx in 1 until avisosPendientes.size) {
                    val lineaExtra = TextView(this).apply {
                        tag = tagExtra
                        text = avisosPendientes[idx]
                        textSize = tvAvisosBase.textSize / resources.displayMetrics.scaledDensity
                        setTextColor(tvAvisosBase.currentTextColor)
                        maxLines = 1
                        ellipsize = TextUtils.TruncateAt.END
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { topMargin = 6 }
                    }
                    contenedorAvisos.addView(lineaExtra, indiceBase + idx)
                }
            }
        }

        fun mostrarMenu() {
            ocultarTodo()
            pantallaActual = "MENU"

            val menuView = findViewById<View>(R.id.layoutMenuPrincipal)
            menuView.visibility = View.VISIBLE
            menuView.bringToFront()

            btnVolver.visibility = View.GONE
            tvTitulo.text = "🌾 AgroTrack - Conectado al campo..."

            refrescarMenuPrincipal()
            sincronizarConServidor()
        }

        fun configurarBotonIzquierdo(btn: Button) {
            btn.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            btn.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            btn.transformationMethod = null
        }

        fun crearTresBotonesGestion(
            padre: LinearLayout,
            tagContenedor: String,
            textoAnadir: String,
            textoQuitar: String,
            onAnadir: () -> Unit,
            onQuitar: () -> Unit,
            onEliminar: () -> Unit
        ) {
            padre.findViewWithTag<View>(tagContenedor)?.let { padre.removeView(it) }

            val filaBotones = LinearLayout(this).apply {
                tag = tagContenedor
                orientation = LinearLayout.HORIZONTAL
                weightSum = 3f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 20 }
            }

            fun hacerBtn(titulo: String, bgHex: String, fgHex: String, marginEnd: Int, action: () -> Unit): Button {
                return Button(this).apply {
                    text = titulo
                    textSize = 12f
                    transformationMethod = null
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    setBackgroundColor(Color.parseColor(bgHex))
                    setTextColor(Color.parseColor(fgHex))
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply { this.marginEnd = marginEnd }
                    setOnClickListener { action() }
                }
            }

            filaBotones.addView(hacerBtn(textoAnadir, "#E8F5E9", "#1B5E20", 12, onAnadir))
            filaBotones.addView(hacerBtn(textoQuitar, "#FFF8E1", "#E65100", 12, onQuitar))
            filaBotones.addView(hacerBtn("Eliminar", "#FFEBEE", "#B71C1C", 0, onEliminar))

            padre.addView(filaBotones)
        }

        lateinit var abrirGridAnimales: () -> Unit
        lateinit var abrirGridCultivos: () -> Unit

        fun abrirDetalleAnimal(animal: AnimalItem) {
            ocultarTodo()
            pantallaActual = "DETALLE_ANIMAL"
            animarEntrada(layoutDetalle)
            btnVolver.visibility = View.VISIBLE
            tvTitulo.text = animal.nombre

            findViewById<TextView>(R.id.tvDetalleEmoji).text = animal.emoji
            findViewById<TextView>(R.id.tvDetalleNombre).text = animal.nombre
            val tvCant = findViewById<TextView>(R.id.tvDetalleCantidad)
            tvCant.text = "Cantidad: ${animal.cantidad} uds."

            val btnTarea = findViewById<Button>(R.id.btnTareaDiariaDetalle)
            configurarBotonIzquierdo(btnTarea)
            fun pintarBotonComida() {
                if (animal.alimentadoHoy) {
                    btnTarea.text = "Comida completada ✓"
                    btnTarea.setBackgroundColor(Color.parseColor("#2E7D32"))
                    btnTarea.setTextColor(Color.WHITE)
                } else {
                    btnTarea.text = "Comida Pendiente"
                    btnTarea.setBackgroundColor(Color.WHITE)
                    btnTarea.setTextColor(Color.parseColor("#1B5E20"))
                }
            }
            pintarBotonComida()
            btnTarea.setOnClickListener {
                animal.alimentadoHoy = !animal.alimentadoHoy
                storage.guardarAlimentacion(animal.id, animal.alimentadoHoy)
                pintarBotonComida()
                refrescarMenuPrincipal()
            }

            val cajaRecogida = findViewById<LinearLayout>(R.id.cajaRecogidaDetalle)
            cajaRecogida.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvTituloRecogidaDetalle).text =
                "Recogida de hoy (${animal.productoAsociado} - ${animal.unidadProducto}):"
            val etCant = findViewById<EditText>(R.id.etCantidadRecogidaDetalle)
            findViewById<Button>(R.id.btnGuardarRecogidaDetalle).setOnClickListener {
                val c = etCant.text.toString().toIntOrNull() ?: 0
                if (c > 0) {
                    storage.restaurarProductoInventario(animal.productoAsociado)
                    storage.sumarStockInventario(animal.productoAsociado, c, listaInventario)
                    etCant.text.clear()
                    Toast.makeText(this, "+$c ${animal.unidadProducto} añadidos al inventario", Toast.LENGTH_SHORT).show()
                }
            }

            val btnVac = findViewById<Button>(R.id.btnVacunaPodaDetalle)
            configurarBotonIzquierdo(btnVac)
            fun pintarBotonVacuna() {
                if (animal.vacunado) {
                    btnVac.text = "Vacuna: ${animal.vacunaInfo} ✓"
                    btnVac.setBackgroundColor(Color.parseColor("#1B5E20"))
                    btnVac.setTextColor(Color.WHITE)
                } else {
                    btnVac.text = "Vacuna: ${animal.vacunaInfo}"
                    btnVac.setBackgroundColor(Color.WHITE)
                    btnVac.setTextColor(Color.parseColor("#D84315"))
                }
            }
            pintarBotonVacuna()
            btnVac.setOnClickListener {
                animal.vacunado = !animal.vacunado
                storage.guardarVacuna(animal.id, animal.vacunado)
                pintarBotonVacuna()
                refrescarMenuPrincipal()
            }

            val padreDetalle = tvCant.parent as LinearLayout
            padreDetalle.findViewWithTag<View>("botonesGestionCultivo")?.let { padreDetalle.removeView(it) }
            crearTresBotonesGestion(
                padre = padreDetalle,
                tagContenedor = "botonesGestionAnimal",
                textoAnadir = "+ uds.",
                textoQuitar = "Quitar uds.",
                onAnadir = {
                    val input = EditText(this).apply {
                        inputType = InputType.TYPE_CLASS_NUMBER
                        hint = "uds. a añadir"
                    }
                    AlertDialog.Builder(this)
                        .setTitle("Añadir ${animal.nombre}")
                        .setView(input)
                        .setPositiveButton("Añadir") { _, _ ->
                            val sumar = input.text.toString().toIntOrNull() ?: 0
                            if (sumar > 0) {
                                animal.cantidad += sumar
                                storage.guardarCantidadAnimal(animal.id, animal.cantidad)
                                registrarCambioAnimalEnRoom(animal, "ACTIVO")
                                tvCant.text = "Cantidad: ${animal.cantidad} uds."
                                refrescarMenuPrincipal()
                            }
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                },
                onQuitar = {
                    val input = EditText(this).apply {
                        inputType = InputType.TYPE_CLASS_NUMBER
                        hint = "uds. a quitar (actual: ${animal.cantidad})"
                    }
                    AlertDialog.Builder(this)
                        .setTitle("Quitar uds. de ${animal.nombre}")
                        .setView(input)
                        .setPositiveButton("Quitar uds.") { _, _ ->
                            val quitar = input.text.toString().toIntOrNull() ?: 0
                            if (quitar > 0) {
                                val nuevaCant = (animal.cantidad - quitar).coerceAtLeast(0)
                                if (nuevaCant == 0) {
                                    storage.eliminarAnimalElegido(animal.id)
                                    registrarCambioAnimalEnRoom(animal.copy(cantidad = 0), "CERRADO")
                                    refrescarMenuPrincipal()
                                    abrirGridAnimales()
                                } else {
                                    animal.cantidad = nuevaCant
                                    storage.guardarCantidadAnimal(animal.id, nuevaCant)
                                    registrarCambioAnimalEnRoom(animal, "ACTIVO")
                                    tvCant.text = "Cantidad: ${animal.cantidad} uds."
                                    refrescarMenuPrincipal()
                                }
                            }
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                },
                onEliminar = {
                    AlertDialog.Builder(this)
                        .setTitle("Eliminar ${animal.nombre}")
                        .setMessage("¿Eliminar ${animal.nombre} por completo?")
                        .setPositiveButton("Eliminar") { _, _ ->
                            storage.eliminarAnimalElegido(animal.id)
                            registrarCambioAnimalEnRoom(animal.copy(cantidad = 0), "CERRADO")
                            refrescarMenuPrincipal()
                            abrirGridAnimales()
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            )
        }

        fun abrirDetalleCultivo(cultivo: CultivoItem) {
            ocultarTodo()
            pantallaActual = "DETALLE_CULTIVO"
            animarEntrada(layoutDetalle)
            btnVolver.visibility = View.VISIBLE
            tvTitulo.text = cultivo.nombre

            val mes = storage.obtenerMesActual()
            val fase = cultivo.obtenerFaseEnMes(mes)

            findViewById<TextView>(R.id.tvDetalleEmoji).text = cultivo.emoji
            findViewById<TextView>(R.id.tvDetalleNombre).text = cultivo.nombre
            val tvCant = findViewById<TextView>(R.id.tvDetalleCantidad)
            tvCant.text = "${formatoM2(cultivo.metros)} • Fase: $fase"

            val btnTarea = findViewById<Button>(R.id.btnTareaDiariaDetalle)
            configurarBotonIzquierdo(btnTarea)
            fun pintarBotonRiego() {
                if (cultivo.regadoHoy) {
                    btnTarea.text = "Riego completado ✓"
                    btnTarea.setBackgroundColor(Color.parseColor("#2E7D32"))
                    btnTarea.setTextColor(Color.WHITE)
                } else {
                    btnTarea.text = "Riego pendiente"
                    btnTarea.setBackgroundColor(Color.WHITE)
                    btnTarea.setTextColor(Color.parseColor("#1B5E20"))
                }
            }
            pintarBotonRiego()
            btnTarea.setOnClickListener {
                cultivo.regadoHoy = !cultivo.regadoHoy
                storage.guardarRiego(cultivo.id, cultivo.regadoHoy)
                pintarBotonRiego()
                refrescarMenuPrincipal()
            }

            val cajaRecogida = findViewById<LinearLayout>(R.id.cajaRecogidaDetalle)
            if (fase == "Cosecha") {
                cajaRecogida.visibility = View.VISIBLE
                findViewById<TextView>(R.id.tvTituloRecogidaDetalle).text =
                    "¡Tiempo de cosecha! Añadir ${cultivo.productoAsociado} (${cultivo.unidadProducto}):"
                val etCant = findViewById<EditText>(R.id.etCantidadRecogidaDetalle)
                findViewById<Button>(R.id.btnGuardarRecogidaDetalle).setOnClickListener {
                    val c = etCant.text.toString().toIntOrNull() ?: 0
                    if (c > 0) {
                        storage.restaurarProductoInventario(cultivo.productoAsociado)
                        storage.sumarStockInventario(cultivo.productoAsociado, c, listaInventario)
                        etCant.text.clear()
                        Toast.makeText(this, "+$c ${cultivo.unidadProducto} añadidos al inventario", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                cajaRecogida.visibility = View.GONE
            }

            val btnPoda = findViewById<Button>(R.id.btnVacunaPodaDetalle)
            configurarBotonIzquierdo(btnPoda)
            fun pintarBotonPoda() {
                if (cultivo.podado) {
                    btnPoda.text = "Poda: ${cultivo.podaInfo} ✓"
                    btnPoda.setBackgroundColor(Color.parseColor("#1B5E20"))
                    btnPoda.setTextColor(Color.WHITE)
                } else {
                    btnPoda.text = "Poda: ${cultivo.podaInfo}"
                    btnPoda.setBackgroundColor(Color.WHITE)
                    btnPoda.setTextColor(Color.parseColor("#D84315"))
                }
            }
            pintarBotonPoda()
            btnPoda.setOnClickListener {
                cultivo.podado = !cultivo.podado
                storage.guardarPoda(cultivo.id, cultivo.podado)
                pintarBotonPoda()
                refrescarMenuPrincipal()
            }

            val padreDetalle = tvCant.parent as LinearLayout
            padreDetalle.findViewWithTag<View>("botonesGestionAnimal")?.let { padreDetalle.removeView(it) }
            crearTresBotonesGestion(
                padre = padreDetalle,
                tagContenedor = "botonesGestionCultivo",
                textoAnadir = "+ m²",
                textoQuitar = "Quitar m²",
                onAnadir = {
                    val input = EditText(this).apply {
                        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                        hint = "m² a añadir"
                    }
                    AlertDialog.Builder(this)
                        .setTitle("Añadir superficie de ${cultivo.nombre}")
                        .setView(input)
                        .setPositiveButton("Añadir") { _, _ ->
                            val sumar = input.text.toString().toDoubleOrNull() ?: 0.0
                            if (sumar > 0.0) {
                                cultivo.metros += sumar
                                storage.guardarMetrosCultivo(cultivo.id, cultivo.metros)
                                registrarCambioCultivoEnRoom(cultivo, "ACTIVO")
                                tvCant.text = "${formatoM2(cultivo.metros)} • Fase: $fase"
                                refrescarMenuPrincipal()
                            }
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                },
                onQuitar = {
                    val input = EditText(this).apply {
                        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                        hint = "m² a quitar (actual: ${cultivo.metros.toInt()})"
                    }
                    AlertDialog.Builder(this)
                        .setTitle("Quitar m² de ${cultivo.nombre}")
                        .setView(input)
                        .setPositiveButton("Quitar m²") { _, _ ->
                            val quitar = input.text.toString().toDoubleOrNull() ?: 0.0
                            if (quitar > 0.0) {
                                val nuevosM2 = (cultivo.metros - quitar).coerceAtLeast(0.0)
                                if (nuevosM2 <= 0.0) {
                                    storage.eliminarCultivoElegido(cultivo.id)
                                    registrarCambioCultivoEnRoom(cultivo.copy(metros = 0.0), "CERRADO")
                                    refrescarMenuPrincipal()
                                    abrirGridCultivos()
                                } else {
                                    cultivo.metros = nuevosM2
                                    storage.guardarMetrosCultivo(cultivo.id, nuevosM2)
                                    registrarCambioCultivoEnRoom(cultivo, "ACTIVO")
                                    tvCant.text = "${formatoM2(cultivo.metros)} • Fase: $fase"
                                    refrescarMenuPrincipal()
                                }
                            }
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                },
                onEliminar = {
                    AlertDialog.Builder(this)
                        .setTitle("Eliminar ${cultivo.nombre}")
                        .setMessage("¿Eliminar ${cultivo.nombre} por completo?")
                        .setPositiveButton("Eliminar") { _, _ ->
                            storage.eliminarCultivoElegido(cultivo.id)
                            registrarCambioCultivoEnRoom(cultivo.copy(metros = 0.0), "CERRADO")
                            refrescarMenuPrincipal()
                            abrirGridCultivos()
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            )
        }

        abrirGridAnimales = {
            ocultarTodo()
            pantallaActual = "GRID_ANIMALES"
            animarEntrada(layoutSeccionGrid)
            btnVolver.visibility = View.VISIBLE
            tvTitulo.text = "🐄 Mis Animales"

            val autoBuscar = findViewById<AutoCompleteTextView>(R.id.autoBuscarElemento)
            autoBuscar.setText("")
            autoBuscar.hint = "Añadir..."
            autoBuscar.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, catalogoAnimales.map { it.nombre }))

            fun recargarGrid() {
                recyclerGrid.adapter = GridAgroAdapter(
                    animales = animalesDelCliente(),
                    onAnimalClick = { abrirDetalleAnimal(it) }
                )
            }
            recargarGrid()

            findViewById<Button>(R.id.btnAnadirElemento).setOnClickListener {
                val texto = autoBuscar.text.toString().trim()
                val encontrado = catalogoAnimales.find { it.nombre.equals(texto, ignoreCase = true) }
                if (encontrado != null) {
                    val inputCant = EditText(this).apply {
                        inputType = InputType.TYPE_CLASS_NUMBER
                        hint = "Cantidad inicial (ej: ${encontrado.cantidad})"
                        setText(encontrado.cantidad.toString())
                    }
                    AlertDialog.Builder(this)
                        .setTitle("Cantidad de ${encontrado.nombre}")
                        .setView(inputCant)
                        .setPositiveButton("Añadir") { _, _ ->
                            val cantReal = inputCant.text.toString().toIntOrNull() ?: encontrado.cantidad
                            encontrado.cantidad = cantReal
                            storage.guardarCantidadAnimal(encontrado.id, cantReal)
                            storage.agregarAnimalElegido(encontrado.id)
                            registrarCambioAnimalEnRoom(encontrado, "ACTIVO")
                            autoBuscar.setText("")
                            recargarGrid()
                            refrescarMenuPrincipal()
                            Toast.makeText(this, "${encontrado.nombre} ($cantReal uds.) añadido", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                } else {
                    Toast.makeText(this, "Elige un animal de la lista", Toast.LENGTH_SHORT).show()
                }
            }
        }

        abrirGridCultivos = {
            ocultarTodo()
            pantallaActual = "GRID_CULTIVOS"
            animarEntrada(layoutSeccionGrid)
            btnVolver.visibility = View.VISIBLE
            tvTitulo.text = "🌱 Mis Cultivos"

            val autoBuscar = findViewById<AutoCompleteTextView>(R.id.autoBuscarElemento)
            autoBuscar.setText("")
            autoBuscar.hint = "Añadir..."
            autoBuscar.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, catalogoCultivos.map { it.nombre }))

            fun recargarGrid() {
                recyclerGrid.adapter = GridAgroAdapter(
                    cultivos = cultivosDelCliente(),
                    mesActual = storage.obtenerMesActual(),
                    onCultivoClick = { abrirDetalleCultivo(it) }
                )
            }
            recargarGrid()

            findViewById<Button>(R.id.btnAnadirElemento).setOnClickListener {
                val texto = autoBuscar.text.toString().trim()
                val encontrado = catalogoCultivos.find { it.nombre.equals(texto, ignoreCase = true) }
                if (encontrado != null) {
                    val inputCant = EditText(this).apply {
                        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                        hint = "m² (ej: ${encontrado.metros.toInt()})"
                        setText(encontrado.metros.toInt().toString())
                    }
                    AlertDialog.Builder(this)
                        .setTitle("Superficie de ${encontrado.nombre}")
                        .setView(inputCant)
                        .setPositiveButton("Añadir") { _, _ ->
                            val m2Real = inputCant.text.toString().toDoubleOrNull() ?: encontrado.metros
                            encontrado.metros = m2Real
                            storage.guardarMetrosCultivo(encontrado.id, m2Real)
                            storage.agregarCultivoElegido(encontrado.id)
                            registrarCambioCultivoEnRoom(encontrado, "ACTIVO")
                            autoBuscar.setText("")
                            recargarGrid()
                            refrescarMenuPrincipal()
                            Toast.makeText(this, "${encontrado.nombre} (${formatoM2(m2Real)}) añadido", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                } else {
                    Toast.makeText(this, "Elige un cultivo de la lista", Toast.LENGTH_SHORT).show()
                }
            }
        }

        fun abrirRegistro() {
            ocultarTodo()
            pantallaActual = "REGISTRO"
            animarEntrada(layoutRegistro)
            btnVolver.visibility = View.VISIBLE
            tvTitulo.text = "📅 Registro"

            val tvMes = findViewById<TextView>(R.id.tvMesActualCalendario)
            val tvFechaAgenda = findViewById<TextView>(R.id.tvFechaDetalleAgenda)
            val recyclerMes = findViewById<RecyclerView>(R.id.recyclerCalendarioMes)
            val contEventos = findViewById<LinearLayout>(R.id.contenedorEventosDia)
            recyclerMes.layoutManager = GridLayoutManager(this, 7)

            val cal = Calendar.getInstance()
            var diaSel = cal.get(Calendar.DAY_OF_MONTH)
            val prefsEventos = getSharedPreferences("agro_eventos_custom", MODE_PRIVATE)

            fun agregarTarjetaEvento(
                etiquetaIzq: String,
                titulo: String,
                colorBarra: String,
                onClickEliminar: (() -> Unit)? = null
            ) {
                val fila = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setBackgroundResource(R.drawable.bg_bento_card)
                    setPadding(28, 24, 28, 24)
                    val p = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    p.setMargins(0, 0, 0, 14)
                    layoutParams = p
                }
                val tvTag = TextView(this).apply {
                    text = etiquetaIzq
                    textSize = 12f
                    setTextColor(Color.parseColor("#666666"))
                    setPadding(0, 0, 20, 0)
                }
                val barra = View(this).apply {
                    setBackgroundColor(Color.parseColor(colorBarra))
                    layoutParams = LinearLayout.LayoutParams(10, 52).apply { setMargins(0, 0, 24, 0) }
                }
                val tvTxt = TextView(this).apply {
                    text = titulo
                    textSize = 15f
                    setTextColor(Color.parseColor("#1B2E1E"))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                fila.addView(tvTag)
                fila.addView(barra)
                fila.addView(tvTxt)

                if (onClickEliminar != null) {
                    val btnBorrar = Button(this).apply {
                        text = "Eliminar"
                        textSize = 11f
                        transformationMethod = null
                        setBackgroundColor(Color.parseColor("#FFEBEE"))
                        setTextColor(Color.parseColor("#B71C1C"))
                        setOnClickListener { onClickEliminar() }
                    }
                    fila.addView(btnBorrar)
                }

                contEventos.addView(fila)
            }

            lateinit var construirMes: () -> Unit

            fun mostrarEventosDelDia(fechaStr: String, mes1a12: Int, diaNum: Int) {
                val mesesCortos = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
                tvFechaAgenda.text = "$diaNum ${mesesCortos[mes1a12 - 1]} ${cal.get(Calendar.YEAR)}"
                contEventos.removeAllViews()

                for (c in cultivosDelCliente()) {
                    val fase = c.obtenerFaseEnMes(mes1a12)
                    if (fase == "Cosecha") {
                        agregarTarjetaEvento("Cosecha", "${c.emoji} Recogida de ${c.nombre} activa", "#2E7D32")
                    } else if (fase == "Siembra") {
                        agregarTarjetaEvento("Siembra", "${c.emoji} Época de siembra: ${c.nombre}", "#EF6C00")
                    }
                }

                val prefs = getSharedPreferences("agrotrack_v3_prefs", MODE_PRIVATE)
                for (a in animalesDelCliente()) {
                    if (prefs.getBoolean("f_${fechaStr}_a_${a.id}_alim", false)) {
                        agregarTarjetaEvento("Hecho", "${a.emoji} ${a.nombre} alimentados", "#2E7D32")
                    }
                }
                for (c in cultivosDelCliente()) {
                    if (prefs.getBoolean("f_${fechaStr}_c_${c.id}_riego", false)) {
                        agregarTarjetaEvento("Hecho", "${c.emoji} ${c.nombre} regados", "#2E7D32")
                    }
                }

                val custom = prefsEventos.getString("ev_$fechaStr", null)
                if (!custom.isNullOrBlank()) {
                    agregarTarjetaEvento("Nota", custom, "#1565C0") {
                        prefsEventos.edit().remove("ev_$fechaStr").apply()
                        construirMes()
                        refrescarMenuPrincipal()
                        Toast.makeText(this, "Evento eliminado", Toast.LENGTH_SHORT).show()
                    }
                }

                if (contEventos.childCount == 0) {
                    agregarTarjetaEvento("Libre", "Sin tareas ni cosechas especiales este día", "#BDBDBD")
                }
            }

            construirMes = {
                val anio = cal.get(Calendar.YEAR)
                val mes0 = cal.get(Calendar.MONTH)
                val mes1 = mes0 + 1
                val nombresMes = listOf("ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC")
                tvMes.text = "${nombresMes[mes0]} $anio"

                val temp = cal.clone() as Calendar
                temp.set(Calendar.DAY_OF_MONTH, 1)
                val primerDiaSemana = (temp.get(Calendar.DAY_OF_WEEK) + 5) % 7
                val maxDias = temp.getActualMaximum(Calendar.DAY_OF_MONTH)

                val misCultivos = cultivosDelCliente()
                val hayCosecha = misCultivos.any { it.obtenerFaseEnMes(mes1) == "Cosecha" }
                val haySiembra = misCultivos.any { it.obtenerFaseEnMes(mes1) == "Siembra" }

                val listaDias = mutableListOf<DiaCalendarioUI>()
                repeat(primerDiaSemana) {
                    listaDias.add(DiaCalendarioUI(0, "", null, null))
                }
                for (d in 1..maxDias) {
                    val fStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", anio, mes1, d)
                    val evCustom = prefsEventos.getString("ev_$fStr", null)
                    val tagVerde = if (hayCosecha && (d % 3 == 0 || d == 15)) "Cosecha" else null
                    val tagNaranja = if (haySiembra && (d % 5 == 0)) "Siembra" else null
                    val tagAzul = if (!evCustom.isNullOrBlank()) "Evento" else null
                    listaDias.add(DiaCalendarioUI(d, fStr, tagVerde ?: tagAzul, tagNaranja))
                }

                val diaInicial = diaSel.coerceIn(1, maxDias)
                val fechaInicial = String.format(Locale.getDefault(), "%04d-%02d-%02d", anio, mes1, diaInicial)
                mostrarEventosDelDia(fechaInicial, mes1, diaInicial)

                recyclerMes.adapter = CalendarioMesAdapter(listaDias, diaInicial) { diaItem ->
                    if (diaItem.dia <= 0) return@CalendarioMesAdapter
                    diaSel = diaItem.dia
                    val fStr = diaItem.fechaIso
                    mostrarEventosDelDia(fStr, mes1, diaItem.dia)

                    val eventoExistente = prefsEventos.getString("ev_$fStr", "") ?: ""
                    val inputEvento = EditText(this).apply {
                        hint = "Añadir nota o evento para el día ${diaItem.dia}"
                        setText(eventoExistente)
                    }

                    val builder = AlertDialog.Builder(this)
                        .setTitle("Evento · ${diaItem.dia}/${mes1}/$anio")
                        .setView(inputEvento)
                        .setPositiveButton("Guardar") { _, _ ->
                            val texto = inputEvento.text.toString().trim()
                            if (texto.isNotEmpty()) {
                                prefsEventos.edit().putString("ev_$fStr", texto).apply()
                                construirMes()
                                refrescarMenuPrincipal()
                                Toast.makeText(this, "Evento guardado", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton("Cancelar", null)

                    if (eventoExistente.isNotBlank()) {
                        builder.setNeutralButton("Eliminar") { _, _ ->
                            prefsEventos.edit().remove("ev_$fStr").apply()
                            construirMes()
                            refrescarMenuPrincipal()
                            Toast.makeText(this, "Evento eliminado", Toast.LENGTH_SHORT).show()
                        }
                    }
                    builder.show()
                }
                val btnNuevoEventoCalendario = findViewById<View>(R.id.btnNuevoEventoCalendario)
                btnNuevoEventoCalendario.setOnClickListener {
                    val etAviso = EditText(this).apply {
                        hint = "Escribe el evento o aviso..."
                        setPadding(40, 28, 40, 28)
                    }

                    AlertDialog.Builder(this)
                        .setTitle("Nuevo evento / recordatorio")
                        .setView(etAviso)
                        .setPositiveButton("Guardar") { _, _ ->
                            val texto = etAviso.text.toString().trim()
                            if (texto.isNotEmpty()) {
                                // Guarda `texto` en la lista/estructura de eventos de la fecha seleccionada
                                // y llama aquí a tu función que refresca `contenedorEventosDia` y `tvNotificacionesMenu`
                                Toast.makeText(this, "Aviso añadido: $texto", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }

            construirMes()

            val contChecks = findViewById<LinearLayout>(R.id.contenedorChecksRegistro)
            contChecks.removeAllViews()

            val densidad = resources.displayMetrics.density
            val altoBotonPx = (32 * densidad).toInt()
            val padHorizontalPx = (12 * densidad).toInt()

            fun agregarFilaEstado(emoji: String, titulo: String, estadoInicial: Boolean, onCambio: (Boolean) -> Unit) {
                val fila = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setBackgroundResource(R.drawable.bg_bento_card)
                    setPadding(24, 16, 20, 16)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 0, 0, 12) }
                }

                val tvInfo = TextView(this).apply {
                    text = "$emoji  $titulo"
                    textSize = 13f
                    setTextColor(Color.parseColor("#1B2E1E"))
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(0, 0, 12, 0)
                    }
                }

                val btnAccion = Button(this).apply {
                    var activo = estadoInicial
                    fun actualizarEstilo() {
                        text = if (activo) "Hecho ✓" else "Pendiente"
                        textSize = 11f
                        transformationMethod = null
                        setPadding(padHorizontalPx, 0, padHorizontalPx, 0)
                        minWidth = 0
                        minimumWidth = 0
                        minHeight = altoBotonPx
                        minimumHeight = altoBotonPx
                        if (activo) {
                            setBackgroundColor(Color.parseColor("#2E7D32"))
                            setTextColor(Color.WHITE)
                        } else {
                            setBackgroundColor(Color.parseColor("#E8F5E9"))
                            setTextColor(Color.parseColor("#1B5E20"))
                        }
                    }
                    actualizarEstilo()
                    setOnClickListener {
                        activo = !activo
                        actualizarEstilo()
                        onCambio(activo)
                        refrescarMenuPrincipal()
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        altoBotonPx
                    )
                }

                fila.addView(tvInfo)
                fila.addView(btnAccion)
                contChecks.addView(fila)
            }

            for (a in animalesDelCliente()) {
                agregarFilaEstado("💉", "${a.nombre} · ${a.vacunaInfo}", a.vacunado) { isChecked ->
                    a.vacunado = isChecked
                    storage.guardarVacuna(a.id, isChecked)
                }
            }
            for (c in cultivosDelCliente()) {
                agregarFilaEstado("✂️", "${c.nombre} · ${c.podaInfo}", c.podado) { isChecked ->
                    c.podado = isChecked
                    storage.guardarPoda(c.id, isChecked)
                }
            }
        }

        fun abrirInventario() {
            ocultarTodo()
            pantallaActual = "INVENTARIO"
            animarEntrada(layoutInv)
            btnVolver.visibility = View.VISIBLE
            tvTitulo.text = "📦 Inventario"

            storage.cargarEstados(catalogoAnimales, catalogoCultivos, listaInventario)

            val recyclerInv = findViewById<RecyclerView>(R.id.recyclerInventarioGrid)
            recyclerInv.layoutManager = GridLayoutManager(this, 2)

            val btnAnim = findViewById<Button>(R.id.btnTabInvAnimales)
            val btnCult = findViewById<Button>(R.id.btnTabInvCultivos)
            var categoriaActual = "ANIMAL"

            fun cargarCategoria(cat: String) {
                categoriaActual = cat
                if (cat == "ANIMAL") {
                    btnAnim.setBackgroundColor(Color.parseColor("#2E7D32"))
                    btnAnim.setTextColor(Color.WHITE)
                    btnCult.setBackgroundColor(Color.parseColor("#E8F5E9"))
                    btnCult.setTextColor(Color.parseColor("#1B5E20"))
                } else {
                    btnCult.setBackgroundColor(Color.parseColor("#2E7D32"))
                    btnCult.setTextColor(Color.WHITE)
                    btnAnim.setBackgroundColor(Color.parseColor("#E8F5E9"))
                    btnAnim.setTextColor(Color.parseColor("#1B5E20"))
                }

                val ocultos = storage.obtenerIdsInventarioOcultos()
                val filtrados = listaInventario.filter { it.categoria == cat && !ocultos.contains(it.nombre) }
                recyclerInv.adapter = GridAgroAdapter(
                    productos = filtrados,
                    onProductoClick = { prod ->
                        val contenedorDialogo = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(48, 24, 48, 12)
                        }

                        val input = EditText(this).apply {
                            inputType = InputType.TYPE_CLASS_NUMBER
                            hint = "Cantidad (${prod.unidad})"
                        }

                        val filaBotones = LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            weightSum = 3f
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { topMargin = 24 }
                        }

                        fun crearBtnAccion(texto: String, bgHex: String, fgHex: String, marginEnd: Int): Button {
                            return Button(this).apply {
                                text = texto
                                textSize = 12f
                                transformationMethod = null
                                maxLines = 1
                                ellipsize = TextUtils.TruncateAt.END
                                setBackgroundColor(Color.parseColor(bgHex))
                                setTextColor(Color.parseColor(fgHex))
                                layoutParams = LinearLayout.LayoutParams(
                                    0,
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    1f
                                ).apply { this.marginEnd = marginEnd }
                            }
                        }

                        val btnAnadir = crearBtnAccion("Añadir", "#E8F5E9", "#1B5E20", 12)
                        val btnQuitar = crearBtnAccion("Quitar", "#FFF8E1", "#E65100", 12)
                        val btnEliminar = crearBtnAccion("Eliminar", "#FFEBEE", "#B71C1C", 0)

                        filaBotones.addView(btnAnadir)
                        filaBotones.addView(btnQuitar)
                        filaBotones.addView(btnEliminar)

                        contenedorDialogo.addView(input)
                        contenedorDialogo.addView(filaBotones)

                        val dialogo = AlertDialog.Builder(this)
                            .setTitle("${prod.emoji} ${prod.nombre} (${prod.cantidadRecogida} ${prod.unidad})")
                            .setMessage("Elige cuánto añadir, quitar o eliminar el producto.")
                            .setView(contenedorDialogo)
                            .setNegativeButton("Cerrar", null)
                            .create()

                        btnAnadir.setOnClickListener {
                            val cant = input.text.toString().toIntOrNull() ?: 0
                            if (cant > 0) {
                                storage.sumarStockInventario(prod.nombre, cant, listaInventario)
                                cargarCategoria(categoriaActual)
                                Toast.makeText(this, "+$cant ${prod.unidad} añadidos", Toast.LENGTH_SHORT).show()
                                dialogo.dismiss()
                            } else {
                                Toast.makeText(this, "Escribe una cantidad mayor que 0", Toast.LENGTH_SHORT).show()
                            }
                        }

                        btnQuitar.setOnClickListener {
                            val cant = input.text.toString().toIntOrNull() ?: 0
                            if (cant > 0) {
                                val quitarReal = cant.coerceAtMost(prod.cantidadRecogida)
                                storage.sumarStockInventario(prod.nombre, -quitarReal, listaInventario)
                                cargarCategoria(categoriaActual)
                                Toast.makeText(this, "-$quitarReal ${prod.unidad} quitados", Toast.LENGTH_SHORT).show()
                                dialogo.dismiss()
                            } else {
                                Toast.makeText(this, "Escribe una cantidad mayor que 0", Toast.LENGTH_SHORT).show()
                            }
                        }

                        btnEliminar.setOnClickListener {
                            storage.sumarStockInventario(prod.nombre, -prod.cantidadRecogida, listaInventario)
                            storage.ocultarProductoInventario(prod.nombre)
                            cargarCategoria(categoriaActual)
                            Toast.makeText(this, "${prod.nombre} eliminado del inventario", Toast.LENGTH_SHORT).show()
                            dialogo.dismiss()
                        }

                        dialogo.show()
                    }
                )
            }

            cargarCategoria("ANIMAL")
            btnAnim.setOnClickListener { cargarCategoria("ANIMAL") }
            btnCult.setOnClickListener { cargarCategoria("CULTIVO") }
        }

        findViewById<View>(R.id.cardCalendario).setOnClickListener { abrirRegistro() }
        findViewById<View>(R.id.cardAnimales).setOnClickListener { abrirGridAnimales() }
        findViewById<View>(R.id.cardCultivos).setOnClickListener { abrirGridCultivos() }
        findViewById<View>(R.id.cardInventario).setOnClickListener { abrirInventario() }
        findViewById<View>(R.id.btnPerfilCabecera).setOnClickListener { abrirPantallaPerfil() }
        findViewById<View>(R.id.btnNuevoEventoCalendario)

        btnVolver.setOnClickListener {
            when (pantallaActual) {
                "DETALLE_ANIMAL" -> abrirGridAnimales()
                "DETALLE_CULTIVO" -> abrirGridCultivos()
                else -> mostrarMenu()
            }
        }

        mostrarMenu()
        if (usuarioActualId <= 0L) {
            mostrarDialogoAuth(obligatorio = true) { refrescarMenuPrincipal() }
        }
    }
}