package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.GlassCard
import com.example.ui.theme.SpotificBluePrimary
import com.example.ui.theme.SpotificBlueSecondary
import com.example.ui.theme.SpotificDarkBg
import com.example.ui.theme.SpotificTextMuted
import com.example.ui.theme.SpotificTextPrimary
import com.example.ui.theme.SpotificTextSecondary

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpotificDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 110.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        // App Icon
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(Color(0xFF141420)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.spotific_logo),
                contentDescription = "Spotific",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(92.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Spotific Title with Electric Blue gradient
        Text(
            text = "Spotific",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            style = androidx.compose.ui.text.TextStyle(
                brush = Brush.horizontalGradient(
                    colors = listOf(SpotificBlueSecondary, SpotificBluePrimary)
                )
            )
        )

        Text(
            text = "Version 1.0.0 (Build 2026.1)",
            color = SpotificTextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Spotific delivers high-fidelity music streaming with zero interruptions. Featuring persistent background playback with system media controls, offline downloads, and electric blue styling.",
            color = SpotificTextSecondary,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Info Rows Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp
        ) {
            Column {
                AboutInfoRow(
                    icon = Icons.Default.Star,
                    title = "Rate Spotific",
                    subtitle = "Leave your feedback on the app",
                    onClick = {
                        Toast.makeText(context, "Thank you for supporting Spotific!", Toast.LENGTH_SHORT).show()
                    }
                )

                HorizontalDivider(color = Color(0x14FFFFFF))

                AboutInfoRow(
                    icon = Icons.Default.Share,
                    title = "Share App",
                    subtitle = "Invite friends to stream together",
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Spotific Music App")
                            putExtra(Intent.EXTRA_TEXT, "Stream unlimited music on Spotific with zero ads and true background playback!")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Spotific"))
                    }
                )

                HorizontalDivider(color = Color(0x14FFFFFF))

                AboutInfoRow(
                    icon = Icons.Default.Policy,
                    title = "Privacy Policy",
                    subtitle = "Read our data handling guidelines",
                    onClick = { showPrivacyDialog = true }
                )

                HorizontalDivider(color = Color(0x14FFFFFF))

                AboutInfoRow(
                    icon = Icons.Default.Email,
                    title = "Contact Support",
                    subtitle = "Get help or report stream issues",
                    onClick = { showContactDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Footer credit line
        Text(
            text = "Crafted with passion for music lovers worldwide",
            color = SpotificTextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy", color = SpotificTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Spotific respects your privacy. All your liked songs and downloaded audio files are stored locally on your device storage. We do not track or sell your personal playback habits.",
                    color = SpotificTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("OK", color = SpotificBluePrimary)
                }
            },
            containerColor = Color(0xFF181824)
        )
    }

    if (showContactDialog) {
        AlertDialog(
            onDismissRequest = { showContactDialog = false },
            title = { Text("Contact Support", color = SpotificTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Need assistance? Contact our engineering team at support@spotific.stream.\nWe appreciate your bug reports and feature requests!",
                    color = SpotificTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showContactDialog = false }) {
                    Text("Close", color = SpotificBluePrimary)
                }
            },
            containerColor = Color(0xFF181824)
        )
    }
}

@Composable
fun AboutInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0x1F1E90FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = SpotificBluePrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = SpotificTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = SpotificTextSecondary,
                fontSize = 12.sp
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = SpotificTextMuted,
            modifier = Modifier.size(14.dp)
        )
    }
}
