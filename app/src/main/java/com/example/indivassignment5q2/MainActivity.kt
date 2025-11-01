package com.example.indivassignment5q2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Note
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.indivassignment5q2.ui.theme.IndivAssignment5Q2Theme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// --- Data Models ---
data class Note(val id: Int, val content: String)
data class Task(val id: Int, val description: String, var isCompleted: Boolean)

// --- ViewModel ---
class DailyHubViewModel : ViewModel() {
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes = _notes.asStateFlow()

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks = _tasks.asStateFlow()

    init {
        // Add some initial data for demonstration
        _notes.value = listOf(Note(1, "Don't forget the meeting at 3 PM."))
        _tasks.value = listOf(
            Task(1, "Buy groceries", false),
            Task(2, "Finish assignment", true)
        )
    }

    fun addNote(content: String) {
        val newId = (_notes.value.maxOfOrNull { it.id } ?: 0) + 1
        _notes.update { it + Note(newId, content) }
    }

    fun toggleTask(taskId: Int) {
        _tasks.update { currentTasks ->
            currentTasks.map { task ->
                if (task.id == taskId) {
                    task.copy(isCompleted = !task.isCompleted)
                } else {
                    task
                }
            }
        }
    }
}

// --- Navigation Routes ---
sealed class Routes(val route: String, val label: String, val icon: ImageVector) {
    object Notes : Routes("notes", "Notes", Icons.Default.Note)
    object Tasks : Routes("tasks", "Tasks", Icons.Default.List)
    object Calendar : Routes("calendar", "Calendar", Icons.Default.DateRange)
}

class MainActivity : ComponentActivity() {
    private val viewModel: DailyHubViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IndivAssignment5Q2Theme {
                DailyHubApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun DailyHubApp(viewModel: DailyHubViewModel) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { AppBottomNavigation(navController = navController) }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun AppBottomNavigation(navController: NavHostController) {
    val items = listOf(Routes.Notes, Routes.Tasks, Routes.Calendar)
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: DailyHubViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Notes.route,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        composable(Routes.Notes.route) {
            Text("Notes Screen")
        }
        composable(Routes.Tasks.route) {
            Text("Tasks Screen")
        }
        composable(Routes.Calendar.route) {
            Text("Calendar Screen")
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    IndivAssignment5Q2Theme {
        DailyHubApp(viewModel = DailyHubViewModel())
    }
}
