package com.nuvio.tv.ui.screens.account

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.core.qr.QrCodeGenerator
import com.nuvio.tv.ui.theme.NuvioTheme

private const val MIGRATION_WEB_URL = "https://nuvio.calibreforge.dev/"

@Composable
fun LegacyMigrationHandoffScreen(onBackPress: () -> Unit = {}) {
    BackHandler(onBack = onBackPress)
    val qr = remember { runCatching { QrCodeGenerator.generate(MIGRATION_WEB_URL, 512, margin = 1) }.getOrNull() }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.76f)
                .background(NuvioTheme.colors.BackgroundElevated, RoundedCornerShape(20.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Sync your existing Nuvio account",
                style = MaterialTheme.typography.headlineSmall,
                color = NuvioTheme.colors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            qr?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "QR code for CalibreForge Nuvio account migration",
                    modifier = Modifier.size(220.dp).background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(8.dp)).padding(8.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Scan with your phone, sign in to your CalibreForge account, then open Account and choose “Sync existing Nuvio account.” Your old password is sent only to Nuvio’s official service.",
                style = MaterialTheme.typography.bodyMedium,
                color = NuvioTheme.colors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(MIGRATION_WEB_URL, color = NuvioTheme.colors.TextSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onBackPress, shape = androidx.tv.material3.ButtonDefaults.shape(RoundedCornerShape(50))) {
                Text("Back")
            }
        }
    }
}
