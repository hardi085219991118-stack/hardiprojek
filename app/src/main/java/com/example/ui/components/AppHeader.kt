package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.FarmGreenPrimary

@Composable
fun AppHeader(
    title: String = "SEJAHTERA BERSAMA",
    subtitle: String = "REZEKI LANCAR, USAHA MAKMUR",
    onAboutClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = FarmGreenPrimary,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Official Logo
            Image(
                painter = painterResource(id = R.drawable.logo_sejahtera_bersama),
                contentDescription = "Logo Resmi SEJAHTERA BERSAMA",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .padding(2.dp)
                    .testTag("app_logo_header")
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFFFD54F),
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                )
            }

            if (onAboutClick != null) {
                IconButton(
                    onClick = onAboutClick,
                    modifier = Modifier.testTag("btn_about_app")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Tentang Aplikasi",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
