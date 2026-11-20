package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueTint
import com.example.ui.theme.LostCrimson
import com.example.ui.theme.LostCrimsonSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateText

private enum class AuthMode { LOGIN, SIGNUP, RESET }

@Composable
fun AuthScreen(
    isBusy: Boolean,
    errorMessage: String?,
    onClearError: () -> Unit,
    onLogin: (identifier: String, password: String, usePhone: Boolean) -> Unit,
    onSignUp: (fullName: String, email: String, phone: String, city: String, password: String) -> Unit,
    onResetPassword: (identifier: String, usePhone: Boolean, newPassword: String, onDone: () -> Unit) -> Unit,
    onQuickDemoLogin: (userId: String) -> Unit
) {
    var mode by remember { mutableStateOf(AuthMode.LOGIN) }
    var usePhone by remember { mutableStateOf(false) }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("New York") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 520.dp)) {
            // Hero Banner Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DeepNavy)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_findora_mark),
                            contentDescription = "FINDORA emblem",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "FINDORA",
                                style = MaterialTheme.typography.headlineMedium,
                                color = PureWhite
                            )
                            Text(
                                text = "Lost Something? Find It Anywhere.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PureWhite.copy(alpha = 0.78f)
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_lost_found),
                        contentDescription = "FINDORA transit map illustration",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(126.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Privacy-first global lost & found network. Free reporting, deterministic matching, and verified handovers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PureWhite.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Mode Tabs: Log In / Sign Up / Reset
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SlateSurface,
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    listOf(
                        AuthMode.LOGIN to "Log In",
                        AuthMode.SIGNUP to "Sign Up",
                        AuthMode.RESET to "Reset"
                    ).forEach { (itemMode, label) ->
                        val active = mode == itemMode
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("auth_tab_${itemMode.name.lowercase()}")
                                .clickable {
                                    mode = itemMode
                                    onClearError()
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) DeepNavy else PureWhite.copy(alpha = 0f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (active) PureWhite else SlateText
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            if (mode != AuthMode.SIGNUP) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { usePhone = false; onClearError() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("auth_method_email"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (!usePhone) ElectricBlueTint else PureWhite
                        ),
                        border = BorderStroke(1.dp, if (!usePhone) ElectricBlue else SlateBorder)
                    ) {
                        Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Email", color = DeepNavy)
                    }
                    OutlinedButton(
                        onClick = { usePhone = true; onClearError() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("auth_method_phone"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (usePhone) ElectricBlueTint else PureWhite
                        ),
                        border = BorderStroke(1.dp, if (usePhone) ElectricBlue else SlateBorder)
                    ) {
                        Icon(Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Phone", color = DeepNavy)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    color = LostCrimsonSoft,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = LostCrimson,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            if (mode == AuthMode.SIGNUP) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_fullname_input")
                )
                Spacer(Modifier.height(10.dp))
            }

            if (mode == AuthMode.SIGNUP || !usePhone) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_email_input")
                )
                Spacer(Modifier.height(10.dp))
            }

            if (mode == AuthMode.SIGNUP || usePhone) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (e.g. +15550142)") },
                    leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_phone_input")
                )
                Spacer(Modifier.height(10.dp))
            }

            if (mode == AuthMode.SIGNUP) {
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Primary City") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
            }

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(if (mode == AuthMode.RESET) "New Password (6+ chars)" else "Password") },
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_password_input")
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val identifier = if (usePhone) phone else email
                    when (mode) {
                        AuthMode.LOGIN -> onLogin(identifier, password, usePhone)
                        AuthMode.SIGNUP -> onSignUp(fullName, email, phone, city, password)
                        AuthMode.RESET -> onResetPassword(identifier, usePhone, password) {
                            mode = AuthMode.LOGIN
                        }
                    }
                },
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("auth_submit_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PureWhite, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = when (mode) {
                            AuthMode.LOGIN -> "Log In to FINDORA"
                            AuthMode.SIGNUP -> "Create Free Account"
                            AuthMode.RESET -> "Update Password"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            // Quick Demo Role Switcher for Testing
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SlateSurface,
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Instant Test Accounts (Seeded Local Database)",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepNavy
                    )
                    Text(
                        text = "Try any role immediately, or create your own account above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateMuted
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DemoRoleChip(
                            title = "Maya Lin",
                            subtitle = "Lost Owner • Free",
                            icon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp)) },
                            onClick = { onQuickDemoLogin("user_maya") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("demo_login_maya")
                        )
                        DemoRoleChip(
                            title = "Liam Patel",
                            subtitle = "Finder • Premium",
                            icon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = DeepNavy, modifier = Modifier.size(16.dp)) },
                            onClick = { onQuickDemoLogin("user_liam") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("demo_login_liam")
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DemoRoleChip(
                            title = "JFK T4 Desk",
                            subtitle = "Partner Org",
                            icon = { Icon(Icons.Outlined.Apartment, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp)) },
                            onClick = { onQuickDemoLogin("user_jfk_desk") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("demo_login_org")
                        )
                        DemoRoleChip(
                            title = "Admin Mod",
                            subtitle = "Trust & Safety",
                            icon = { Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null, tint = DeepNavy, modifier = Modifier.size(16.dp)) },
                            onClick = { onQuickDemoLogin("user_admin") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("demo_login_admin")
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DemoRoleChip(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(ElectricBlueTint),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.labelLarge, color = DeepNavy)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = SlateMuted)
            }
        }
    }
}
