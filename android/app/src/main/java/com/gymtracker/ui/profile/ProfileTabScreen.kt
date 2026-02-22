package com.gymtracker.ui.profile

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.data.models.Profile
import com.gymtracker.data.models.ProfileUpdate
import com.gymtracker.ui.MainViewModel
import com.gymtracker.ui.theme.*

@Composable
fun ProfileTabScreen(viewModel: MainViewModel) {
    val profile by viewModel.currentProfile.collectAsState()
    val photoUri by viewModel.profilePhotoUri.collectAsState()
    val context = LocalContext.current

    var showWeightDialog by remember { mutableStateOf(false) }
    var showCurrentDimsDialog by remember { mutableStateOf(false) }
    var showGoalDimsDialog by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { /* URI not persistable — still usable this session */ }
            viewModel.saveProfilePhotoUri(uri.toString())
        }
    }

    val bitmap = remember(photoUri) {
        if (photoUri == null) null
        else runCatching {
            BitmapFactory.decodeStream(context.contentResolver.openInputStream(Uri.parse(photoUri)))
        }.getOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Header with photo + name
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Circular profile photo — tap to change
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Card)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Profile photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Neon,
                        modifier = Modifier.size(40.dp)
                    )
                }
                // Camera badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(22.dp)
                        .background(Neon, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Change photo",
                        tint = Color.Black,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    profile?.name ?: "",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = OnSurface,
                    letterSpacing = 1.sp
                )
                Text("Profile", color = SubText, fontSize = 12.sp, letterSpacing = 2.sp)
            }

            IconButton(onClick = { showNameDialog = true }) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit name",
                    tint = SubText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // Weight Card
        SectionCard(
            title = "Weight",
            icon = Icons.Default.Scale,
            onEdit = { showWeightDialog = true }
        ) {
            WeightRow("Starting", profile?.startingWeight)
            WeightRow("Current", profile?.currentWeight)
            WeightRow("Goal", profile?.goalWeight)
        }

        Spacer(Modifier.height(16.dp))

        // Current Dimensions Card
        SectionCard(
            title = "Current Dimensions",
            icon = Icons.Default.TableChart,
            onEdit = { showCurrentDimsDialog = true }
        ) {
            DimRow("Waist", profile?.currentWaist)
            DimRow("Arm", profile?.currentArm)
            DimRow("Chest", profile?.currentChest)
            DimRow("Thigh", profile?.currentThigh)
            DimRow("Neck", profile?.currentNeck)
            DimRow("Hip", profile?.currentHip)
        }

        Spacer(Modifier.height(16.dp))

        // Goal Dimensions Card
        SectionCard(
            title = "Goal Dimensions",
            icon = Icons.Default.Flag,
            onEdit = { showGoalDimsDialog = true }
        ) {
            DimRow("Waist", profile?.goalWaist)
            DimRow("Arm", profile?.goalArm)
            DimRow("Chest", profile?.goalChest)
            DimRow("Thigh", profile?.goalThigh)
            DimRow("Neck", profile?.goalNeck)
            DimRow("Hip", profile?.goalHip)
        }

        Spacer(Modifier.height(16.dp))

        // Account Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Card),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPasswordDialog = true }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, null, tint = Neon, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Change Password",
                        color = OnSurface,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ChevronRight, null, tint = SubText)
                }

                HorizontalDivider(color = Surface, thickness = 1.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDeleteConfirmDialog = true }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DeleteForever, null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Delete Profile", color = ErrorRed, fontSize = 15.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }

    // Weight edit dialog
    if (showWeightDialog && profile != null) {
        WeightEditDialog(
            profile = profile!!,
            onDismiss = { showWeightDialog = false },
            onSave = { update ->
                viewModel.updateCurrentProfile(update)
                showWeightDialog = false
            }
        )
    }

    // Current dimensions edit dialog
    if (showCurrentDimsDialog && profile != null) {
        DimensionsEditDialog(
            title = "Current Dimensions",
            waist = profile!!.currentWaist,
            arm = profile!!.currentArm,
            chest = profile!!.currentChest,
            thigh = profile!!.currentThigh,
            neck = profile!!.currentNeck,
            hip = profile!!.currentHip,
            onDismiss = { showCurrentDimsDialog = false },
            onSave = { w, a, c, t, n, h ->
                viewModel.updateCurrentProfile(
                    ProfileUpdate(
                        currentWaist = w, currentArm = a, currentChest = c,
                        currentThigh = t, currentNeck = n, currentHip = h
                    )
                )
                showCurrentDimsDialog = false
            }
        )
    }

    // Goal dimensions edit dialog
    if (showGoalDimsDialog && profile != null) {
        DimensionsEditDialog(
            title = "Goal Dimensions",
            waist = profile!!.goalWaist,
            arm = profile!!.goalArm,
            chest = profile!!.goalChest,
            thigh = profile!!.goalThigh,
            neck = profile!!.goalNeck,
            hip = profile!!.goalHip,
            onDismiss = { showGoalDimsDialog = false },
            onSave = { w, a, c, t, n, h ->
                viewModel.updateCurrentProfile(
                    ProfileUpdate(
                        goalWaist = w, goalArm = a, goalChest = c,
                        goalThigh = t, goalNeck = n, goalHip = h
                    )
                )
                showGoalDimsDialog = false
            }
        )
    }

    // Name edit dialog
    if (showNameDialog && profile != null) {
        NameEditDialog(
            currentName = profile!!.name,
            onDismiss = { showNameDialog = false },
            onSave = { newName ->
                viewModel.updateCurrentProfile(ProfileUpdate(name = newName))
                showNameDialog = false
            }
        )
    }

    // Change password dialog
    if (showPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onSave = { newPassword ->
                viewModel.changePassword(newPassword)
                showPasswordDialog = false
            }
        )
    }

    // Delete profile confirmation dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = Card,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete Profile", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${profile?.name}\"? " +
                            "All workout and metric data will be permanently deleted. This cannot be undone.",
                    color = OnSurface,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCurrentProfile()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = SubText)
                }
            }
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onEdit: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Card),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = Neon, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(title, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = SubText, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun WeightRow(label: String, value: Float?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = SubText, fontSize = 14.sp)
        Text(
            if (value != null) "${"%.1f".format(value)} lbs" else "—",
            color = if (value != null) OnSurface else SubText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DimRow(label: String, value: Float?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = SubText, fontSize = 14.sp)
        Text(
            if (value != null) "${"%.1f".format(value)} in" else "—",
            color = if (value != null) OnSurface else SubText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun WeightEditDialog(
    profile: Profile,
    onDismiss: () -> Unit,
    onSave: (ProfileUpdate) -> Unit
) {
    var starting by remember { mutableStateOf(profile.startingWeight?.toString() ?: "") }
    var current by remember { mutableStateOf(profile.currentWeight?.toString() ?: "") }
    var goal by remember { mutableStateOf(profile.goalWeight?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Card,
        title = { Text("Edit Weight", color = OnSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WeightField("Starting weight (lbs)", starting) { starting = it }
                WeightField("Current weight (lbs)", current) { current = it }
                WeightField("Goal weight (lbs)", goal) { goal = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    ProfileUpdate(
                        startingWeight = starting.toFloatOrNull(),
                        currentWeight = current.toFloatOrNull(),
                        goalWeight = goal.toFloatOrNull()
                    )
                )
            }) {
                Text("Save", color = Neon, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SubText) }
        }
    )
}

@Composable
private fun DimensionsEditDialog(
    title: String,
    waist: Float?,
    arm: Float?,
    chest: Float?,
    thigh: Float?,
    neck: Float?,
    hip: Float?,
    onDismiss: () -> Unit,
    onSave: (Float?, Float?, Float?, Float?, Float?, Float?) -> Unit
) {
    var waistStr by remember { mutableStateOf(waist?.toString() ?: "") }
    var armStr by remember { mutableStateOf(arm?.toString() ?: "") }
    var chestStr by remember { mutableStateOf(chest?.toString() ?: "") }
    var thighStr by remember { mutableStateOf(thigh?.toString() ?: "") }
    var neckStr by remember { mutableStateOf(neck?.toString() ?: "") }
    var hipStr by remember { mutableStateOf(hip?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Card,
        title = { Text("Edit $title", color = OnSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WeightField("Waist (in)", waistStr) { waistStr = it }
                WeightField("Arm (in)", armStr) { armStr = it }
                WeightField("Chest (in)", chestStr) { chestStr = it }
                WeightField("Thigh (in)", thighStr) { thighStr = it }
                WeightField("Neck (in)", neckStr) { neckStr = it }
                WeightField("Hip (in)", hipStr) { hipStr = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    waistStr.toFloatOrNull(), armStr.toFloatOrNull(), chestStr.toFloatOrNull(),
                    thighStr.toFloatOrNull(), neckStr.toFloatOrNull(), hipStr.toFloatOrNull()
                )
            }) {
                Text("Save", color = Neon, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SubText) }
        }
    )
}

@Composable
private fun NameEditDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Card,
        title = { Text("Edit Name", color = OnSurface, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name", color = SubText) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Neon,
                    unfocusedBorderColor = SubText,
                    focusedTextColor = OnSurface,
                    unfocusedTextColor = OnSurface,
                    cursorColor = Neon
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Save", color = Neon, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SubText) }
        }
    )
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val mismatch = confirmPassword.isNotEmpty() && newPassword != confirmPassword

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Card,
        title = { Text("Change Password", color = OnSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New password", color = SubText) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Neon,
                        unfocusedBorderColor = SubText,
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface,
                        cursorColor = Neon
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm password", color = SubText) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = mismatch,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (mismatch) ErrorRed else Neon,
                        unfocusedBorderColor = if (mismatch) ErrorRed else SubText,
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface,
                        cursorColor = Neon
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (mismatch) {
                    Text("Passwords do not match", color = ErrorRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(newPassword) },
                enabled = newPassword.isNotBlank() && newPassword == confirmPassword
            ) {
                Text("Save", color = Neon, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SubText) }
        }
    )
}

@Composable
private fun WeightField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, color = SubText) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Neon,
            unfocusedBorderColor = SubText,
            focusedTextColor = OnSurface,
            unfocusedTextColor = OnSurface,
            cursorColor = Neon
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
