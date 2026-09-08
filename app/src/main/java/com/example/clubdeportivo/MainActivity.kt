package com.example.clubdeportivo

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.clubdeportivo.databinding.ActivityMainBinding

/**
 * Única Activity de la app. Todas las pantallas son Fragments que viven
 * dentro del NavHostFragment; esta clase solo conecta la barra superior
 * y el menú inferior con el controlador de navegación.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val idsMenuInferior = setOf(
        R.id.homeFragment,
        R.id.areasFragment,
        R.id.misReservasFragment,
        R.id.torneosFragment,
        R.id.perfilFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setSupportActionBar(binding.toolbar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController

        // Pantallas "de primer nivel": no muestran flecha de regreso y
        // corresponden a los botones del menú inferior.
        appBarConfiguration = AppBarConfiguration(idsMenuInferior)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // No se usa BottomNavigationView.setupWithNavController(): combinado con
        // popUpTo + saveState, el botón "atrás" del sistema termina reapareciendo
        // pantallas de pestañas que ya se habían cerrado (comportamiento confuso
        // y difícil de predecir). En su lugar, cada pestaña SIEMPRE regresa
        // primero a Inicio y desde ahí abre la pantalla elegida: así el botón
        // atrás se comporta siempre igual (pantalla de detalle -> pestaña -> Inicio -> salir).
        binding.bottomNav.setOnItemSelectedListener { item ->
            navegarAPestana(navController, item.itemId)
            true
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val esLogin = destination.id == R.id.loginFragment
            binding.bottomNav.visibility = if (esLogin) View.GONE else View.VISIBLE
            binding.toolbar.visibility = if (esLogin) View.GONE else View.VISIBLE

            if (destination.id in idsMenuInferior) {
                binding.bottomNav.menu.findItem(destination.id)?.isChecked = true
            }
        }
    }

    private fun navegarAPestana(navController: NavController, destinoId: Int) {
        if (navController.currentDestination?.id == destinoId) return

        val opciones = NavOptions.Builder()
            .setPopUpTo(R.id.homeFragment, destinoId == R.id.homeFragment)
            .setLaunchSingleTop(true)
            .build()
        navController.navigate(destinoId, null, opciones)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        return navHostFragment.navController.navigateUp(appBarConfiguration)
    }
}
