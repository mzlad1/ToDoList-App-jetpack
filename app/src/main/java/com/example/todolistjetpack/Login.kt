package com.example.todolistjetpack

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

class Login : ComponentActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)

        setContent {
            LoginScreen(
                sharedPreferences = sharedPreferences,
                goToTodoPage = {
                    startActivity(Intent(this, todoPage::class.java))
                    finish()
                },
                goToSignUp = {
                    startActivity(Intent(this, SignUp::class.java))
                }
            )
        }
    }
}

@Composable
fun LoginScreen(
    sharedPreferences: SharedPreferences,
    goToTodoPage: () -> Unit,
    goToSignUp: () -> Unit
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (sharedPreferences.getBoolean("isLoggedIn", false)) {
            goToTodoPage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(color = Color.White)
            .padding(16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.header),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(169.dp)
                .padding(vertical = 16.dp)
                .scale(1.6f)
        )

        Text(
            text = "Sign In",
            color = Color.Black,
            textAlign = TextAlign.Center,
            fontSize = 40.sp,
            fontFamily = FontFamily.Default,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Button(
            onClick = {
                handleLogin(
                    username,
                    password,
                    sharedPreferences,
                    context,
                    FirebaseFirestore.getInstance(),
                    goToTodoPage
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC))
        ) {
            Text("Login")
        }

        Text(
            text = "Don't have an account? Sign Up",
            color = Color(0xFF6200EE),
            fontSize = 20.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { goToSignUp() },
            textAlign = TextAlign.Center
        )
    }
}

private fun handleLogin(
    username: String,
    password: String,
    sharedPreferences: SharedPreferences,
    context: android.content.Context,
    db : FirebaseFirestore,
    goToTodoPage: () -> Unit
) {
    if (username.isEmpty()) {
        Toast.makeText(context, "Username is required", Toast.LENGTH_SHORT).show()
    } else if (password.isEmpty()) {
        Toast.makeText(context, "Password is required", Toast.LENGTH_SHORT).show()
    } else {
        db.collection("users").whereEqualTo("username", username).whereEqualTo("password", password).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(context, "username or password are not correct", Toast.LENGTH_SHORT).show()
                } else {
                    with(sharedPreferences.edit()) {
                        putBoolean("isLoggedIn", true)
                        putString("email", username)
                        apply()
                    }
                    goToTodoPage()
                }
            }
    }
}
