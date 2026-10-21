package com.example.notesappp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.notesappp.ui.theme.NotesAppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: NotesViewModel = viewModel()
            val dark by vm.darkMode.collectAsState()

            // Keeps the status/navigation bar icons readable in both themes
            DisposableEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    )
                }
                this@MainActivity.enableEdgeToEdge(
                    statusBarStyle = style,
                    navigationBarStyle = style
                )
                onDispose { }
            }

            NotesAppTheme(darkTheme = dark) {
                AppNav(vm)
            }
        }
    }
}

@Composable
fun AppNav(vm: NotesViewModel) {
    val nav = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                vm = vm,
                snackbarHostState = snackbarHostState,
                onAddNote = { nav.navigate("edit/-1") },
                onOpenNote = { id -> nav.navigate("edit/$id") }
            )
        }
        composable(
            route = "edit/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { entry ->
            NoteEditScreen(
                vm = vm,
                noteId = entry.arguments?.getInt("id") ?: -1,
                onBack = { nav.popBackStack() },
                onSaved = { msg ->
                    nav.popBackStack()
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            )
        }
    }
}