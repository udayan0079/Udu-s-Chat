package com.uduschat.app.ui.screens.auth

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uduschat.app.core.CountryCode
import com.uduschat.app.core.PhoneNumberUtils
import com.uduschat.app.domain.model.AuthState
import com.uduschat.app.ui.components.AvatarCircle
import com.uduschat.app.ui.components.CloudStatusPill
import com.uduschat.app.ui.components.CountryPickerDialog
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(
    isFirebaseConfigured: Boolean,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.height(12.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "udu's chat",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            Text(
                text = "Welcome to udu's chat",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Connect any valid phone number and chat, send voice notes, share media, post Status updates, and call friends anywhere in the world over the internet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            CloudStatusPill(isFirebaseConfigured = isFirebaseConfigured)
            Spacer(Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (isFirebaseConfigured) {
                            "Firebase global cloud is active. Verify your phone number via SMS OTP to chat worldwide."
                        } else {
                            "Running in Local Demo Mode until app/google-services.json is added. All features are interactive right now."
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tap \"Agree and continue\" to connect with your phone number.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Agree and continue", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneInputScreen(
    authState: AuthState,
    isFirebaseConfigured: Boolean,
    onSendOtp: (rawPhone: String, country: CountryCode, activity: Activity?) -> Unit,
    onClearError: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var selectedCountry by remember { mutableStateOf(PhoneNumberUtils.defaultCountry) }
    var rawPhone by remember { mutableStateOf("") }
    var showCountryPicker by remember { mutableStateOf(false) }
    var confirmE164 by remember { mutableStateOf<String?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }

    val previewE164 = remember(rawPhone, selectedCountry) {
        PhoneNumberUtils.normalizeToE164(rawPhone, selectedCountry)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enter your phone number", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CloudStatusPill(isFirebaseConfigured = isFirebaseConfigured)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "udu's chat will need to verify your phone number so friends around the world can reach you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // Country selector row
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCountryPicker = true },
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selectedCountry.flag, fontSize = 22.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = selectedCountry.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = selectedCountry.dialCode,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Pick country")
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = selectedCountry.dialCode,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Code") },
                    modifier = Modifier
                        .width(96.dp)
                        .clickable { showCountryPicker = true },
                    singleLine = true
                )
                Spacer(Modifier.width(10.dp))
                OutlinedTextField(
                    value = rawPhone,
                    onValueChange = {
                        rawPhone = it
                        localError = null
                        onClearError()
                    },
                    label = { Text("Phone number") },
                    placeholder = { Text(selectedCountry.exampleLocal) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            if (previewE164 != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Global ID: ${PhoneNumberUtils.formatDisplayPhone(previewE164)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            val errorText = localError ?: authState.errorMessage
            if (!errorText.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    val normalized = PhoneNumberUtils.normalizeToE164(rawPhone, selectedCountry)
                    if (normalized == null) {
                        localError = "Enter a valid phone number for ${selectedCountry.name} (e.g. ${selectedCountry.exampleLocal})."
                    } else {
                        confirmE164 = normalized
                    }
                },
                enabled = !authState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                if (authState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Next", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showCountryPicker) {
        CountryPickerDialog(
            selectedCountry = selectedCountry,
            onSelect = {
                selectedCountry = it
                showCountryPicker = false
            },
            onDismiss = { showCountryPicker = false }
        )
    }

    val pendingConfirm = confirmE164
    if (pendingConfirm != null) {
        AlertDialog(
            onDismissRequest = { confirmE164 = null },
            title = { Text("Confirm phone number") },
            text = {
                Column {
                    Text("We will verify this phone number:")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = PhoneNumberUtils.formatDisplayPhone(pendingConfirm),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (isFirebaseConfigured) {
                            "An SMS verification code will be sent via Firebase."
                        } else {
                            "Demo Mode: no real SMS will be sent until Firebase google-services.json is configured."
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    confirmE164 = null
                    onSendOtp(pendingConfirm, selectedCountry, activity)
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmE164 = null }) {
                    Text("Edit")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerifyScreen(
    authState: AuthState,
    onVerifyOtp: (String) -> Unit,
    onResendOtp: (Activity?) -> Unit,
    onEditNumber: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var otpCode by remember { mutableStateOf("") }
    var countdown by remember { mutableIntStateOf(60) }

    LaunchedEffect(authState.pendingPhoneE164, authState.demoOtpHint) {
        countdown = 60
        while (countdown > 0) {
            delay(1000L)
            countdown--
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onEditNumber) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text(
                        text = "Verifying ${PhoneNumberUtils.formatDisplayPhone(authState.pendingPhoneE164)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Enter the 6-digit verification code sent to ${PhoneNumberUtils.formatDisplayPhone(authState.pendingPhoneE164)}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            TextButton(onClick = onEditNumber) {
                Text("Wrong number?")
            }

            if (authState.isDemoMode && !authState.demoOtpHint.isNullOrBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x1AF57C00)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Local Demo Mode OTP: ${authState.demoOtpHint}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                        Text(
                            text = "Add app/google-services.json to switch to real global SMS OTP.",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // 6-digit visual OTP boxes backed by clean OutlinedTextField
            OutlinedTextField(
                value = otpCode,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }.take(6)
                    otpCode = digits
                    if (digits.length == 6) {
                        onVerifyOtp(digits)
                    }
                },
                label = { Text("6-digit code") },
                placeholder = { Text("• • • • • •") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.width(220.dp)
            )

            if (!authState.errorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = authState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { onVerifyOtp(otpCode) },
                enabled = otpCode.length == 6 && !authState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                if (authState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Verify & Continue")
                }
            }

            Spacer(Modifier.height(14.dp))
            TextButton(
                onClick = {
                    countdown = 60
                    onResendOtp(activity)
                },
                enabled = countdown == 0 && !authState.isLoading
            ) {
                Text(
                    if (countdown > 0) "Resend SMS in ${countdown}s" else "Resend SMS"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    phoneNumber: String,
    isLoading: Boolean,
    onComplete: (name: String, about: String, avatarUri: String?) -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("Hey there! I am using udu's chat.") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) avatarUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Profile info", fontWeight = FontWeight.SemiBold) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Please provide your name and an optional profile photo for ${PhoneNumberUtils.formatDisplayPhone(phoneNumber)}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clickable {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.BottomEnd
            ) {
                AvatarCircle(
                    name = displayName.ifBlank { "Udu" },
                    avatarUrl = avatarUri?.toString(),
                    size = 104.dp
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Choose photo",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Type your name here") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = about,
                onValueChange = { about = it },
                label = { Text("About") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { onComplete(displayName, about, avatarUri?.toString()) },
                enabled = displayName.trim().isNotEmpty() && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Next", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun TwoStepPinLockScreen(
    expectedPin: String,
    onUnlocked: () -> Unit
) {
    var inputPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Two-step verification",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Enter your 6-digit PIN to unlock udu's chat.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = inputPin,
            onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(6)
                inputPin = digits
                error = null
                if (digits.length == 6) {
                    if (digits == expectedPin) onUnlocked()
                    else error = "Incorrect PIN. Try again."
                }
            },
            label = { Text("6-digit PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.width(200.dp)
        )
        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
        }
    }
}
