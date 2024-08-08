package com.example.todolistjetpack

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore

class todoPage : ComponentActivity() {
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)

        setContent {
            TodoPageScreen(
                sharedPreferences = sharedPreferences,
                goToLogin = {
                    startActivity(Intent(this, Login::class.java))
                    finish()
                }
            )
        }
    }
}

@Composable
fun TodoPageScreen(sharedPreferences: SharedPreferences, goToLogin: () -> Unit) {
    val context = LocalContext.current
    var tasks by remember { mutableStateOf(listOf<Task>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        val username = sharedPreferences.getString("email", null)
        if (username != null) {
            loadItemsFromFirestore(db, username) { loadedTasks ->
                tasks = loadedTasks
            }
        } else {
            goToLogin()
        }
    }

    if (showAddDialog) {
        AddItemDialog(
            sharedPreferences = sharedPreferences,
            db = db,
            onDismiss = { showAddDialog = false },
            onAddItem = { label, item, fullDescription ->
                tasks = tasks + Task(label, item, fullDescription)
                showAddDialog = false
                Toast.makeText(context, "Item added", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showEditDialog && taskToEdit != null) {
        EditItemDialog(
            sharedPreferences = sharedPreferences,
            db = db,
            task = taskToEdit!!,
            onDismiss = { showEditDialog = false },
            onEditItem = { label, item, fullDescription ->
                updateTaskInFirestore(db, sharedPreferences.getString("email", null)!!, taskToEdit!!, label, item, fullDescription) {
                    loadItemsFromFirestore(db, sharedPreferences.getString("email", null)!!) { loadedTasks ->
                        tasks = loadedTasks
                    }
                }
                showEditDialog = false
                Toast.makeText(context, "Item updated", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "To Do List",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = {
                    with(sharedPreferences.edit()) {
                        putBoolean("isLoggedIn", false)
                        putString("email", "")
                        apply()
                    }
                    goToLogin()
                }
            ) {
                Text("Logout")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TaskList(tasks = tasks, db = db, username = sharedPreferences.getString("email", null)!!, onEditTask = {
            taskToEdit = it
            showEditDialog = true
        }, onDeleteTask = {
            deleteTask(db, sharedPreferences.getString("email", null)!!, it) {
                loadItemsFromFirestore(db, sharedPreferences.getString("email", null)!!) { loadedTasks ->
                    tasks = loadedTasks
                }
            }
        }, modifier = Modifier.weight(1f))

        Button(
            onClick = {
                showAddDialog = true
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 16.dp)
        ) {
            Text("Add Task")
        }
    }
}

@Composable
fun TaskList(tasks: List<Task>, db: FirebaseFirestore, username: String, onEditTask: (Task) -> Unit, onDeleteTask: (Task) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        for (task in tasks) {
            TaskItem(task = task, db = db, username = username, onEditTask = onEditTask, onDeleteTask = onDeleteTask)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun TaskItem(task: Task, db: FirebaseFirestore, username: String, onEditTask: (Task) -> Unit, onDeleteTask: (Task) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.LightGray)
            .padding(8.dp)
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Label: ${task.label}", style = MaterialTheme.typography.labelLarge)
                Text(text = "Task: ${task.item}", style = MaterialTheme.typography.labelSmall)
                Text(text = "Description: ${task.fullDescription}", style = MaterialTheme.typography.bodySmall)
            }
            Row {
                IconButton(onClick = { onEditTask(task) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Task")
                }
                IconButton(onClick = { onDeleteTask(task) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Task")
                }
            }
        }
    }
}

@Composable
fun AddItemDialog(
    sharedPreferences: SharedPreferences,
    db: FirebaseFirestore,
    onDismiss: () -> Unit,
    onAddItem: (String, String, String) -> Unit
) {

    var label by remember { mutableStateOf("") }
    var item by remember { mutableStateOf("") }
    var fullDescription by remember { mutableStateOf("") }

    val context = LocalContext.current
    val username = sharedPreferences.getString("email", null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Task") },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = item,
                    onValueChange = { item = it },
                    label = { Text("Item") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fullDescription,
                    onValueChange = { fullDescription = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (item.isNotEmpty() && username != null) {
                        val task = hashMapOf(
                            "label" to label,
                            "item" to item,
                            "fullDescription" to fullDescription
                        )
                        db.collection("users").whereEqualTo("username", username).get()
                            .addOnSuccessListener { documents ->
                                for (document in documents) {
                                    db.collection("users").document(document.id).collection("tasks")
                                        .add(task)
                                        .addOnSuccessListener {
                                            onAddItem(label, item, fullDescription)
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(context, "Error adding task", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                    } else {
                        Toast.makeText(context, "Item cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditItemDialog(
    sharedPreferences: SharedPreferences,
    db: FirebaseFirestore,
    task: Task,
    onDismiss: () -> Unit,
    onEditItem: (String, String, String) -> Unit
) {
    var label by remember { mutableStateOf(task.label) }
    var item by remember { mutableStateOf(task.item) }
    var fullDescription by remember { mutableStateOf(task.fullDescription) }

    val context = LocalContext.current
    val username = sharedPreferences.getString("email", null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Task") },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = item,
                    onValueChange = { item = it },
                    label = { Text("Item") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fullDescription,
                    onValueChange = { fullDescription = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (item.isNotEmpty() && username != null) {
                        onEditItem(label, item, fullDescription)
                    } else {
                        Toast.makeText(context, "Item cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun loadItemsFromFirestore(
    db: FirebaseFirestore,
    username: String,
    onTasksLoaded: (List<Task>) -> Unit
) {
    db.collection("users").whereEqualTo("username", username).get()
        .addOnSuccessListener { documents ->
            for (document in documents) {
                db.collection("users").document(document.id).collection("tasks").get()
                    .addOnSuccessListener { documents ->
                        val tasks = mutableListOf<Task>()
                        for (document in documents) {
                            val newTask = Task(
                                document.getString("label")!!,
                                document.getString("item")!!,
                                document.getString("fullDescription")!!
                            )
                            tasks.add(newTask)
                        }
                        onTasksLoaded(tasks)
                    }
            }
        }
}

fun deleteTask(db: FirebaseFirestore, username: String, task: Task, onTaskDeleted: () -> Unit) {
    db.collection("users").whereEqualTo("username", username).get()
        .addOnSuccessListener { documents ->
            for (document in documents) {
                db.collection("users").document(document.id).collection("tasks")
                    .whereEqualTo("label", task.label)
                    .whereEqualTo("item", task.item)
                    .whereEqualTo("fullDescription", task.fullDescription)
                    .get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            db.collection("users").document(document.reference.parent.parent!!.id).collection("tasks").document(document.id).delete()
                                .addOnSuccessListener {
                                    Log.d("deleteTask", "Task successfully deleted!")
                                    onTaskDeleted()
                                }
                                .addOnFailureListener { e ->
                                    Log.w("deleteTask", "Error deleting task", e)
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w("deleteTask", "Error finding task to delete", e)
                    }
            }
        }
        .addOnFailureListener { e ->
            Log.w("deleteTask", "Error finding user to delete task", e)
        }
}

fun updateTaskInFirestore(
    db: FirebaseFirestore,
    username: String,
    task: Task,
    newLabel: String,
    newItem: String,
    newFullDescription: String,
    onTaskUpdated: () -> Unit
) {
    db.collection("users").whereEqualTo("username", username).get()
        .addOnSuccessListener { documents ->
            for (document in documents) {
                db.collection("users").document(document.id).collection("tasks")
                    .whereEqualTo("label", task.label)
                    .whereEqualTo("item", task.item)
                    .whereEqualTo("fullDescription", task.fullDescription)
                    .get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            db.collection("users").document(document.reference.parent.parent!!.id).collection("tasks").document(document.id)
                                .update(
                                    mapOf(
                                        "label" to newLabel,
                                        "item" to newItem,
                                        "fullDescription" to newFullDescription
                                    )
                                )
                                .addOnSuccessListener {
                                    Log.d("updateTaskInFirestore", "Task successfully updated!")
                                    onTaskUpdated()
                                }
                                .addOnFailureListener { e ->
                                    Log.w("updateTaskInFirestore", "Error updating task", e)
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w("updateTaskInFirestore", "Error finding task to update", e)
                    }
            }
        }
        .addOnFailureListener { e ->
            Log.w("updateTaskInFirestore", "Error finding user to update task", e)
        }
}