package app.cinephile.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cinephile.data.Api
import app.cinephile.data.ApiException
import app.cinephile.data.Session
import app.cinephile.data.SessionManager
import kotlinx.coroutines.launch

@Composable
fun AuthScreen() {
    var mode by remember { mutableStateOf("welcome") } // welcome | login | signup
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submitGuest() {
        SessionManager.set(Session(method = "guest"))
    }

    fun submitAuth() {
        if (!email.contains("@") || password.length < 6) {
            error = if (password.length < 6) "Password must be at least 6 characters" else "Enter a valid email"
            return
        }
        scope.launch {
            loading = true; error = null
            try {
                val res = if (mode == "login") Api.login(email.trim(), password) else Api.signup(email.trim(), password)
                SessionManager.set(Session(method = "email", token = res.token, email = res.user.email, role = res.user.role))
            } catch (e: ApiException) {
                error = e.message
            } catch (e: Exception) {
                error = "Network error — try again"
            } finally {
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeamColors.bg)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BeamLogo()
        Spacer(Modifier.height(12.dp))
        Text("beamPlay", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = BeamColors.textPrimary)
        Spacer(Modifier.height(6.dp))
        Text(
            "Movies & series, your way",
            fontSize = 15.sp, color = BeamColors.textSecondary, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))

        when (mode) {
            "welcome" -> {
                AuthButton("Continue as Guest", filled = true) { submitGuest() }
                Spacer(Modifier.height(14.dp))
                AuthButton("Sign in with Email", filled = false) { mode = "login" }
                Spacer(Modifier.height(10.dp))
                AuthButton("Create Account", filled = false) { mode = "signup" }
            }
            else -> {
                OutlinedTextField(
                    value = email, onValueChange = { email = it; error = null },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it; error = null },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = Color(0xFFF87171), fontSize = 13.sp)
                }
                Spacer(Modifier.height(20.dp))
                AuthButton(if (mode == "login") "Sign In" else "Sign Up", filled = true, enabled = !loading) { submitAuth() }
                Spacer(Modifier.height(10.dp))
                AuthButton("Back", filled = false) { mode = "welcome"; error = null }
            }
        }

        if (loading) {
            Spacer(Modifier.height(18.dp))
            CircularProgressIndicator(color = BeamColors.primary, modifier = Modifier.size(26.dp))
        }

        Spacer(Modifier.height(30.dp))
        Text(
            "Guest mode keeps everything on this device.\nSign in to sync your library and progress.",
            fontSize = 12.sp, color = BeamColors.textSecondary, textAlign = TextAlign.Center, lineHeight = 17.sp,
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BeamColors.primary,
    unfocusedBorderColor = Color(0xFF2A3040),
    focusedTextColor = BeamColors.textPrimary,
    unfocusedTextColor = BeamColors.textPrimary,
    cursorColor = BeamColors.primary,
)

@Composable
private fun AuthButton(text: String, filled: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    if (filled) {
        Button(
            onClick = onClick, enabled = enabled,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BeamColors.primary),
        ) {
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = BeamColors.textPrimary),
        ) {
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun BeamLogo(size: Int = 72) {
    val boxSize = (size / 1.4).dp
    Box(
        modifier = Modifier
            .size(boxSize)
            .clip(RoundedCornerShape(18.dp))
            .background(BeamColors.surface),
        contentAlignment = Alignment.Center,
    ) {
        Text("▶", fontSize = (size / 1.8).sp, color = BeamColors.primary)
    }
}
