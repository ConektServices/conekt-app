package com.conekt.suite.feature.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.conekt.suite.ui.theme.BrandEnd
import com.conekt.suite.ui.theme.BrandStart
import com.conekt.suite.ui.theme.ConektGradient

@Composable
fun AuthScreen(
    onSignInSuccess: () -> Unit,
    onSignUpSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            if (state.mode == AuthMode.SIGN_UP) onSignUpSuccess() else onSignInSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF07080C), Color(0xFF0D0E14), Color(0xFF0A0B10))
                )
            )
    ) {
        // Ambient glow
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colors = listOf(BrandStart.copy(alpha = 0.18f), Color.Transparent)
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Branding
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(ConektGradient.brandHorizontal),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Rounded.AccountCircle, contentDescription = "Conekt", tint = Color.White, modifier = Modifier.size(38.dp))
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(text = "Conekt", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "your connected digital space", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.56f), modifier = Modifier.padding(top = 6.dp))
                }
            }

            item { ModeToggle(selectedMode = state.mode, onModeChange = viewModel::onModeChange) }

            item {
                AnimatedContent(
                    targetState = state.mode,
                    transitionSpec = { (fadeIn(tween(260)) + slideInVertically { it / 12 }).togetherWith(fadeOut(tween(180))) },
                    label = "formTransition"
                ) { mode ->
                    FormCard(
                        mode = mode, state = state,
                        onEmailChange = viewModel::onEmailChange,
                        onPasswordChange = viewModel::onPasswordChange,
                        onPasswordVisibilityToggle = viewModel::onPasswordVisibilityToggle,
                        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
                        onConfirmPasswordVisibilityToggle = viewModel::onConfirmPasswordVisibilityToggle,
                        onUsernameChange = viewModel::onUsernameChange,
                        onDisplayNameChange = viewModel::onDisplayNameChange,
                        onSubmit = viewModel::submit
                    )
                }
            }

            item {
                AnimatedVisibility(visible = state.errorMessage != null, enter = fadeIn() + slideInVertically { -it / 2 }, exit = fadeOut()) {
                    state.errorMessage?.let { ErrorBanner(message = it, onDismiss = viewModel::clearError) }
                }
            }

            item { SubmitButton(mode = state.mode, isLoading = state.isLoading, onClick = viewModel::submit) }

            item {
                FooterToggle(mode = state.mode, onSwitch = {
                    viewModel.onModeChange(if (state.mode == AuthMode.SIGN_IN) AuthMode.SIGN_UP else AuthMode.SIGN_IN)
                })
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ModeToggle(selectedMode: AuthMode, onModeChange: (AuthMode) -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.06f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AuthMode.entries.forEach { mode ->
                val selected = selectedMode == mode
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                        .background(if (selected) ConektGradient.brandHorizontal else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)))
                        .clickable { onModeChange(mode) }.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (mode == AuthMode.SIGN_IN) "Sign In" else "Create Account",
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                        color = if (selected) Color.White else Color.White.copy(alpha = 0.50f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FormCard(
    mode: AuthMode, state: AuthUiState,
    onEmailChange: (String) -> Unit, onPasswordChange: (String) -> Unit, onPasswordVisibilityToggle: () -> Unit,
    onConfirmPasswordChange: (String) -> Unit, onConfirmPasswordVisibilityToggle: () -> Unit,
    onUsernameChange: (String) -> Unit, onDisplayNameChange: (String) -> Unit, onSubmit: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (mode == AuthMode.SIGN_UP) {
                AuthField(value = state.displayName, onValueChange = onDisplayNameChange, placeholder = "Display name", icon = Icons.Rounded.Person, imeAction = ImeAction.Next, onImeAction = { focusManager.moveFocus(FocusDirection.Down) }, capitalization = KeyboardCapitalization.Words)
                AuthField(value = state.username, onValueChange = onUsernameChange, placeholder = "Username (e.g. byron)", icon = Icons.Rounded.AlternateEmail, imeAction = ImeAction.Next, onImeAction = { focusManager.moveFocus(FocusDirection.Down) })
            }
            AuthField(value = state.email, onValueChange = onEmailChange, placeholder = "Email address", icon = Icons.Rounded.Email, keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, onImeAction = { focusManager.moveFocus(FocusDirection.Down) })
            AuthField(
                value = state.password, onValueChange = onPasswordChange, placeholder = "Password", icon = Icons.Rounded.Lock,
                keyboardType = KeyboardType.Password, isPassword = true, passwordVisible = state.passwordVisible, onPasswordVisibilityToggle = onPasswordVisibilityToggle,
                imeAction = if (mode == AuthMode.SIGN_IN) ImeAction.Done else ImeAction.Next,
                onImeAction = { if (mode == AuthMode.SIGN_IN) { focusManager.clearFocus(); onSubmit() } else focusManager.moveFocus(FocusDirection.Down) }
            )
            if (mode == AuthMode.SIGN_UP) {
                AuthField(value = state.confirmPassword, onValueChange = onConfirmPasswordChange, placeholder = "Confirm password", icon = Icons.Rounded.Lock, keyboardType = KeyboardType.Password, isPassword = true, passwordVisible = state.confirmPasswordVisible, onPasswordVisibilityToggle = onConfirmPasswordVisibilityToggle, imeAction = ImeAction.Done, onImeAction = { focusManager.clearFocus(); onSubmit() })
            }
        }
    }
}

@Composable
private fun AuthField(
    value: String, onValueChange: (String) -> Unit, placeholder: String, icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text, capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    isPassword: Boolean = false, passwordVisible: Boolean = false, onPasswordVisibilityToggle: (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Next, onImeAction: () -> Unit = {}
) {
    TextField(
        value = value, onValueChange = onValueChange,
        placeholder = { Text(text = placeholder, color = Color.White.copy(alpha = 0.36f)) },
        leadingIcon = { Icon(imageVector = icon, contentDescription = placeholder, tint = Color.White.copy(alpha = 0.50f), modifier = Modifier.size(20.dp)) },
        trailingIcon = if (isPassword) ({
            Icon(imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                contentDescription = null, tint = Color.White.copy(alpha = 0.44f),
                modifier = Modifier.size(20.dp).clickable { onPasswordVisibilityToggle?.invoke() })
        }) else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction, capitalization = capitalization),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.08f), unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            cursorColor = BrandEnd, focusedTextColor = Color.White, unfocusedTextColor = Color.White.copy(alpha = 0.90f)
        )
    )
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp), color = BrandEnd.copy(alpha = 0.14f),
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandEnd.copy(alpha = 0.30f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = message, style = MaterialTheme.typography.bodySmall, color = Color(0xFFFF8B8B), modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "✕", color = Color(0xFFFF8B8B).copy(alpha = 0.70f), modifier = Modifier.clickable { onDismiss() })
        }
    }
}

@Composable
private fun SubmitButton(mode: AuthMode, isLoading: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(20.dp))
            .background(ConektGradient.brandHorizontal).clickable(enabled = !isLoading) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.5.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = if (mode == AuthMode.SIGN_IN) "Sign In" else "Create Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Icon(imageVector = Icons.Rounded.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun FooterToggle(mode: AuthMode, onSwitch: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(text = if (mode == AuthMode.SIGN_IN) "Don't have an account? " else "Already have an account? ", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.48f))
        Text(text = if (mode == AuthMode.SIGN_IN) "Create one" else "Sign in", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = BrandEnd, modifier = Modifier.clickable { onSwitch() })
    }
}
