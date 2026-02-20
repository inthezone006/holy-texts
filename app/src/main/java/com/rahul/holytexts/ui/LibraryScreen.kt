package com.rahul.holytexts.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LibraryScreen(onNavigateToBible: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Library",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                LibraryBentoItem(
                    title = "Bible",
                    subtitle = "The Holy Bible",
                    icon = Icons.Default.Book,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onNavigateToBible,
                    modifier = Modifier.height(180.dp)
                )
            }
            item {
                LibraryBentoItem(
                    title = "Quran",
                    subtitle = "The Holy Quran",
                    icon = Icons.Default.Book,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = { /* TODO */ },
                    modifier = Modifier.height(180.dp)
                )
            }
            item {
                LibraryBentoItem(
                    title = "Bhagavad Gita",
                    subtitle = "The Song of God",
                    icon = Icons.Default.Book,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = { /* TODO */ },
                    modifier = Modifier.height(180.dp)
                )
            }
            item {
                LibraryBentoItem(
                    title = "Torah",
                    subtitle = "The Five Books",
                    icon = Icons.Default.Book,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { /* TODO */ },
                    modifier = Modifier.height(180.dp)
                )
            }
            item {
                LibraryBentoItem(
                    title = "Guru Granth Sahib",
                    subtitle = "The Living Guru",
                    icon = Icons.Default.Book,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = { /* TODO */ },
                    modifier = Modifier.height(180.dp)
                )
            }
            item {
                LibraryBentoItem(
                    title = "Dhammapada",
                    subtitle = "Path of Wisdom",
                    icon = Icons.Default.Book,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = { /* TODO */ },
                    modifier = Modifier.height(180.dp)
                )
            }
        }
    }
}

@Composable
fun LibraryBentoItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.TopStart)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.BottomEnd),
                tint = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}
