package heitezy.ledstripcontroller.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import heitezy.ledstripcontroller.BuildConfig
import heitezy.ledstripcontroller.R
import androidx.core.net.toUri

// ── Project links ─────────────────────────────────────────────────────────────

internal object AboutLinks {
    const val REPO          = "https://github.com/Heitezy/LEDStripController"
    const val LICENSE       = "https://github.com/Heitezy/LEDStripController/blob/main/LICENSE"
    const val CONTRIBUTORS  = "https://github.com/Heitezy/LEDStripController/graphs/contributors"
}

// ── Third-party libraries ─────────────────────────────────────────────────────

internal data class LibraryInfo(
    val name: String,
    val license: String,
)

internal val AppLibraries = listOf(
    LibraryInfo("Jetpack Compose UI",        "Apache License 2.0"),
    LibraryInfo("Compose Material 3",        "Apache License 2.0"),
    LibraryInfo("Material Icons Extended",   "Apache License 2.0"),
    LibraryInfo("AndroidX Core KTX",         "Apache License 2.0"),
    LibraryInfo("AndroidX Lifecycle",        "Apache License 2.0"),
    LibraryInfo("AndroidX Activity Compose", "Apache License 2.0"),
    LibraryInfo("Kotlin Standard Library",   "Apache License 2.0"),
    LibraryInfo("Kotlinx Coroutines",        "Apache License 2.0"),
)

// ── URL opener ────────────────────────────────────────────────────────────────

@Composable
internal fun rememberUrlOpener(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { url: String ->
            // TV boxes and stripped-down ROMs may have no browser at all —
            // swallow the failure instead of crashing the app.
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, url.toUri())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: ActivityNotFoundException) {
            }
        }
    }
}

internal fun appVersionLabel(): String = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

// ── Phone: About screen ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    var showLibraries by remember { mutableStateOf(false) }
    val openUrl = rememberUrlOpener()

    BackHandler { if (showLibraries) showLibraries = false else onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (showLibraries) R.string.about_libraries else R.string.label_about
                        ),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (showLibraries) showLibraries = false else onBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (showLibraries) {
                LibrariesList()
            } else {
                AppHeader()
                SourceCard(openUrl)
                LicenseCard(openUrl)
                ContributorsCard(openUrl)
                LibrariesEntryCard(onClick = { showLibraries = true })
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun AppHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    )
                )
            )
            .padding(vertical = 28.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = Color.Unspecified,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.about_version, appVersionLabel()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.about_tagline),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Cards ─────────────────────────────────────────────────────────────────────

@Composable
private fun SourceCard(openUrl: (String) -> Unit) {
    AboutCard {
        CardHeader(stringResource(R.string.about_source), Icons.Default.Code)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.about_source_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        LinkRow(
            title = stringResource(R.string.about_repository),
            subtitle = "github.com/Heitezy/LEDStripController",
            onClick = { openUrl(AboutLinks.REPO) },
        )
    }
}

@Composable
private fun LicenseCard(openUrl: (String) -> Unit) {
    AboutCard {
        CardHeader(stringResource(R.string.about_license), Icons.Default.Gavel)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.about_license_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        LinkRow(
            title = stringResource(R.string.about_license_mit),
            subtitle = stringResource(R.string.about_license_read),
            onClick = { openUrl(AboutLinks.LICENSE) },
        )
    }
}

@Composable
private fun ContributorsCard(openUrl: (String) -> Unit) {
    AboutCard {
        CardHeader(stringResource(R.string.about_contributors), Icons.Default.People)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.about_contributors_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        LinkRow(
            title = stringResource(R.string.about_contributors_all),
            subtitle = stringResource(R.string.about_contributors_all_desc),
            onClick = { openUrl(AboutLinks.CONTRIBUTORS) },
        )
    }
}

@Composable
private fun LibrariesEntryCard(onClick: () -> Unit) {
    AboutCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.LibraryBooks,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.about_libraries),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.about_libraries_count, AppLibraries.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ── Libraries subpage ─────────────────────────────────────────────────────────

@Composable
private fun LibrariesList() {
    Text(
        stringResource(R.string.about_libraries_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    AboutCard {
        AppLibraries.forEachIndexed { index, lib ->
            Column(Modifier.padding(vertical = 10.dp)) {
                Text(
                    lib.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    lib.license,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (index != AppLibraries.lastIndex) {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            }
        }
    }
}

// ── Shared building blocks ────────────────────────────────────────────────────

@Composable
private fun CardHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun LinkRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Mirrors `SectionCard` in MainScreen so the About page reads as part of the app. */
@Composable
private fun AboutCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}
