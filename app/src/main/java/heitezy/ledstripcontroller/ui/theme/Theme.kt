package heitezy.ledstripcontroller.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9C7BFF),
    onPrimary = Color(0xFF1A1A2E),
    primaryContainer = Color(0xFF3D2B7A),
    onPrimaryContainer = Color(0xFFD8BBFF),
    secondary = Color(0xFF6DF0D2),
    onSecondary = Color(0xFF003829),
    background = Color(0xFF0D0D14),
    onBackground = Color(0xFFE8E8F0),
    surface = Color(0xFF1A1A28),
    onSurface = Color(0xFFE8E8F0),
    surfaceVariant = Color(0xFF252535),
    onSurfaceVariant = Color(0xFFB0B0C8),
    outline = Color(0xFF50507A),
    error = Color(0xFFFF6B6B),
)

@Composable
fun LedStripController(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
