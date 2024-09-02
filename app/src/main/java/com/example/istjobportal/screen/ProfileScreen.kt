package com.example.istjobportal.screen

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.database.*
import com.google.firebase.database.DatabaseError
import com.google.firebase.storage.FirebaseStorage

@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var schoolBackground by remember { mutableStateOf("") }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var isEditing by remember { mutableStateOf(false) }

    // Firebase Database reference
    val databaseReference = FirebaseDatabase.getInstance().getReference("users/userId") // Replace 'userId' with actual user ID

    // Firebase Storage reference
    val storageReference = FirebaseStorage.getInstance().getReference("profile_images/userId.jpg") // Replace 'userId' with actual user ID

    // Retrieve user data from Firebase
    LaunchedEffect(Unit) {
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                name = dataSnapshot.child("name").getValue(String::class.java) ?: ""
                email = dataSnapshot.child("email").getValue(String::class.java) ?: ""
                phoneNumber = dataSnapshot.child("phoneNumber").getValue(String::class.java) ?: ""
                skills = dataSnapshot.child("skills").getValue(String::class.java) ?: ""
                experience = dataSnapshot.child("experience").getValue(String::class.java) ?: ""
                schoolBackground = dataSnapshot.child("schoolBackground").getValue(String::class.java) ?: ""
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(context, "Failed to load data.", Toast.LENGTH_SHORT).show()
            }
        })

        // Retrieve profile image URI from Firebase Storage
        storageReference.downloadUrl.addOnSuccessListener { uri ->
            profileImageUri = uri
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to load profile image.", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher to pick images
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                profileImageUri = uri
                // Upload the new image to Firebase Storage
                storageReference.putFile(uri).addOnSuccessListener {
                    Toast.makeText(context, "Image uploaded successfully.", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    Toast.makeText(context, "Failed to upload image.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "No image selected", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Image
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable {
                    imagePickerLauncher.launch("image/*") // Launch the image picker
                }
        ) {
            profileImageUri?.let { uri ->
                Image(
                    painter = rememberAsyncImagePainter(model = uri),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // User Information
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isEditing) {
                ProfileTextField(value = name, onValueChange = { name = it }, label = "Name")
                ProfileTextField(value = email, onValueChange = { email = it }, label = "Email")
                ProfileTextField(value = phoneNumber, onValueChange = { phoneNumber = it }, label = "Phone Number")
                ProfileTextField(value = skills, onValueChange = { skills = it }, label = "Skills")
                ProfileTextField(value = experience, onValueChange = { experience = it }, label = "Experience")
                ProfileTextField(value = schoolBackground, onValueChange = { schoolBackground = it }, label = "School Background")
            } else {
                ProfileText(text = name, label = "Name")
                ProfileText(text = email, label = "Email")
                ProfileText(text = phoneNumber, label = "Phone Number")
                ProfileText(text = skills, label = "Skills")
                ProfileText(text = experience, label = "Experience")
                ProfileText(text = schoolBackground, label = "School Background")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Edit/Save Button
        Button(
            onClick = {
                if (isEditing) {
                    // Save changes to Firebase
                    val userData = mapOf(
                        "name" to name,
                        "email" to email,
                        "phoneNumber" to phoneNumber,
                        "skills" to skills,
                        "experience" to experience,
                        "schoolBackground" to schoolBackground
                    )
                    databaseReference.updateChildren(userData).addOnSuccessListener {
                        Toast.makeText(context, "Profile updated successfully.", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener {
                        Toast.makeText(context, "Failed to update profile.", Toast.LENGTH_SHORT).show()
                    }
                }
                isEditing = !isEditing
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (isEditing) "Save" else "Edit")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Delete Button
        Button(
            onClick = {
                // Handle delete action
                databaseReference.removeValue().addOnSuccessListener {
                    Toast.makeText(context, "Profile deleted successfully.", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    Toast.makeText(context, "Failed to delete profile.", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Delete", color = Color.White)
        }
    }
}

@Composable
fun ProfileTextField(value: String, onValueChange: (String) -> Unit, label: String) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp),
            singleLine = true
        )
    }
}

@Composable
fun ProfileText(text: String, label: String) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
