package com.example.todolistjetpack

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat.startActivity
import com.google.firebase.firestore.FirebaseFirestore

class SignUp : ComponentActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var db: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)

        setContent {
            SignUpScreen(db = db)
        }
    }
}

@Composable
fun SignUpScreen(db: FirebaseFirestore) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var termsSwitch by remember { mutableStateOf(false) }

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
                .padding(vertical = 16.dp).scale(1.6f)
        )

        Text(
            text = "Sign Up",
            color = Color.Black,
            fontSize = 40.sp,
            fontFamily = FontFamily.Default,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = termsSwitch,
                onCheckedChange = { termsSwitch = it }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Accept Terms and Conditions")
        }

        Button(
            onClick = {
                handleSignUp(
                    username,
                    email,
                    password,
                    confirmPassword,
                    name,
                    phone,
                    address,
                    termsSwitch,
                    db,
                    onSignUpSuccess = {
                        Toast.makeText(context, "Sign up successful!", Toast.LENGTH_SHORT).show()
                        username = ""
                        email = ""
                        password = ""
                        confirmPassword = ""
                        name = ""
                        phone = ""
                        address = ""

                        with(context.getSharedPreferences("LoginPrefs", MODE_PRIVATE).edit()) {
                            putBoolean("isLoggedIn", true)
                            putString("email", email)
                            apply()
                        }
                        startActivity(context, Intent(context, todoPage::class.java), null)
                    },
                    onSignUpFailure = { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC))
        ) {
            Text("Sign Up")
        }
    }
}

private fun handleSignUp(
    username: String,
    email: String,
    password: String,
    confirmPassword: String,
    name: String,
    phone: String,
    address: String,
    termsSwitch: Boolean,
    db: FirebaseFirestore,
    onSignUpSuccess: () -> Unit,
    onSignUpFailure: (String) -> Unit
) {
    if (username.isEmpty()) {
        onSignUpFailure("Username is required")
        return
    } else if (email.isEmpty()) {
        onSignUpFailure("Email is required")
        return
    } else if (password.isEmpty()) {
        onSignUpFailure("Password is required")
        return
    } else if (confirmPassword.isEmpty()) {
        onSignUpFailure("Please confirm your password")
        return
    } else if (password.length < 6 || !password.contains(Regex(".*[0-9].*")) || !password.contains(Regex(".*[!@#\$%^&*].*"))) {
        onSignUpFailure("Password must be at least 6 characters long and contain at least one number and one special character")
        return
    } else if (name.isEmpty()) {
        onSignUpFailure("Name is required")
        return
    } else if (phone.isEmpty()) {
        onSignUpFailure("Phone number is required")
        return
    } else if (address.isEmpty()) {
        onSignUpFailure("Address is required")
        return
    } else if (password != confirmPassword) {
        onSignUpFailure("Passwords do not match")
        return
    } else if (!termsSwitch) {
        onSignUpFailure("Please accept the terms and conditions")
        return
    } else {
        db.collection("users").whereEqualTo("username", username).get()
            .addOnSuccessListener { usernameDocuments ->
                if (!usernameDocuments.isEmpty) {
                    onSignUpFailure("Username already exists")
                    return@addOnSuccessListener
                }
                db.collection("users").add(
                    hashMapOf(
                        "username" to username,
                        "email" to email,
                        "password" to password,
                        "name" to name,
                        "phone" to phone,
                        "address" to address
                    )
                ).addOnSuccessListener {
                    onSignUpSuccess()
                }.addOnFailureListener { e ->
                    onSignUpFailure("Error adding user: ${e.message}")
                }
            }.addOnFailureListener { e ->
                onSignUpFailure("Error checking username: ${e.message}")
            }
    }
}
