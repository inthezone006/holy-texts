package com.rahul.holytexts.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.rahul.holytexts.R

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    text: String,
    isDark: Boolean, // Added to respect manual app theme
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isDark) Color(0xFF131314) else Color.White,
            contentColor = if (isDark) Color.White else Color(0xFF1F1F1F)
        ),
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Select icon based on app's manual theme state
            val iconRes = if (isDark) R.drawable.ic_google_logo else R.drawable.ic_google_logo
            
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.Unspecified
            )
            Spacer(Modifier.width(12.dp))
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
