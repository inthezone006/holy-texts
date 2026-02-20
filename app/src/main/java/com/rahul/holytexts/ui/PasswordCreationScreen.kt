package com.rahul.holytexts.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordCreationScreen(
    email: String,
    fullName: String,
    googleIdToken: String?, // Receive token if Google signup
    onBackClick: () -> Unit,
    onComplete: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Password") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Secure Your Account",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Create a password for $email",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        if (password.length < 6) {
                            Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (password != confirmPassword) {
                            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isLoading = true
                        
                        if (googleIdToken != null) {
                            // 1. Sign in with Google
                            val googleCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                            auth.signInWithCredential(googleCredential)
                                .addOnCompleteListener { googleTask ->
                                    if (googleTask.isSuccessful) {
                                        val user = auth.currentUser
                                        // 2. Link with Email/Password
                                        val emailCredential = EmailAuthProvider.getCredential(email, password)
                                        user?.linkWithCredential(emailCredential)
                                            ?.addOnCompleteListener { linkTask ->
                                                if (linkTask.isSuccessful) {
                                                    saveUserToFirestore(user.uid, fullName, email, password, db) {
                                                        isLoading = false
                                                        onComplete()
                                                    }
                                                } else {
                                                    isLoading = false
                                                    Toast.makeText(context, "Linking failed: ${linkTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                    } else {
                                        isLoading = false
                                        Toast.makeText(context, "Google Auth failed: ${googleTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        } else {
                            // Standard Email/Password Sign Up
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val userId = auth.currentUser?.uid
                                        if (userId != null) {
                                            saveUserToFirestore(userId, fullName, email, password, db) {
                                                isLoading = false
                                                onComplete()
                                            }
                                        }
                                    } else {
                                        isLoading = false
                                        Toast.makeText(context, "Auth failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Complete Sign Up")
                }
            }
        }
    }
}

private fun saveUserToFirestore(
    userId: String,
    fullName: String,
    email: String,
    password: String,
    db: FirebaseFirestore,
    onComplete: () -> Unit
) {
    val userMap = hashMapOf(
        "fullName" to fullName,
        "email" to email,
        "password" to password
    )
    db.collection("users").document(userId)
        .set(userMap)
        .addOnSuccessListener { onComplete() }
}
