package com.example.indivassignment5q2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

//Data Models
data class NoteItem(val id: Int, val content: String)
data class Task(val id: Int, val description: String, var isCompleted: Boolean)

//ViewModel
// The ViewModel holds the app's state, surviving screen rotations.
class DailyHubViewModel : ViewModel() {
    private val _notes = MutableStateFlow<List<NoteItem>>(emptyList())
    // A public, read-only flow for the UI to observe state changes safely.
    val notes = _notes.asStateFlow()

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks = _tasks.asStateFlow()

    init {
        // Add some initial data for demonstration
        _notes.value = listOf(NoteItem(1, "Don't forget CS501 at 6:30PM."))
        _tasks.value = listOf(
            Task(1, "Go to the gym", false),
            Task(2, "Finish hw", true)
        )
    }

    fun addNote(content: String) {
        val newId = (_notes.value.maxOfOrNull { it.id } ?: 0) + 1
        _notes.update { it + NoteItem(newId, content) }
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

//Navigation Routes
// A sealed class provides type-safe navigation, preventing route typos.
sealed class Routes(val route: String, val label: String, val icon: ImageVector) {
    object Notes : Routes("notes", "Notes", Icons.Default.Edit)
    object Tasks : Routes("tasks", "Tasks", Icons.AutoMirrored.Filled.List)
    object Calendar : Routes("calendar", "Calendar", Icons.Default.DateRange)
}

class MainActivity : ComponentActivity() {
    // `by viewModels()` ties the ViewModel to the Activity's lifecycle.
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
    // `rememberNavController` creates a persistent NavController for the app.
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
        // Observes the back stack to highlight the currently selected item.
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        // These options ensure a clean navigation history.
                        // `popUpTo` avoids building up a large stack of destinations.
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        // `launchSingleTop` prevents creating a new screen if it's already visible.
                        launchSingleTop = true
                        // `restoreState` restores the UI state (like scroll position) on re-selection.
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
            NotesScreen(viewModel = viewModel)
        }
        composable(Routes.Tasks.route) {
            TasksScreen(viewModel = viewModel)
        }
        composable(Routes.Calendar.route) {
            CalendarScreen()
        }
    }
}

@Composable
fun NotesScreen(viewModel: DailyHubViewModel) {
    // `collectAsState` makes the UI react to changes in the ViewModel's state.
    val notes by viewModel.notes.collectAsState()
    // `remember` preserves UI state (like text field input) across recompositions.
    var newNoteContent by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("My Notes", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = newNoteContent,
            onValueChange = { newNoteContent = it },
            label = { Text("New note") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                if (newNoteContent.isNotBlank()) {
                    viewModel.addNote(newNoteContent)
                    newNoteContent = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Note")
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn {
            items(notes) { note ->
                Text(note.content, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
fun TasksScreen(viewModel: DailyHubViewModel) {
    val tasks by viewModel.tasks.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text("My Tasks", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        LazyColumn {
            items(tasks) { task ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { viewModel.toggleTask(task.id) }
                    )
                    Text(task.description)
                }
            }
        }
    }
}

@Composable
fun CalendarScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("My Calendar", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text("This is where a calendar view will be implemented.")
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    IndivAssignment5Q2Theme {
        DailyHubApp(viewModel = DailyHubViewModel())
    }
}
