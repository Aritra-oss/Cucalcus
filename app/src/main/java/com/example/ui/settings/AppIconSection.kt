package com.example.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.preferences.AppIconOption
import com.example.data.preferences.IconCategory
import com.example.ui.components.SwissSectionHeader
import com.example.ui.theme.CucalcusTheme

@DrawableRes
fun AppIconOption.drawableRes(): Int = when (this) {
    AppIconOption.QUADRANT -> R.drawable.ic_brand_quadrant
    AppIconOption.INTERSECTION -> R.drawable.ic_brand_intersection
    AppIconOption.NEGATIVE_SPACE -> R.drawable.ic_brand_negative_space
    AppIconOption.PRECISION -> R.drawable.ic_brand_precision
    AppIconOption.KKR -> R.drawable.ic_brand_kkr
    AppIconOption.RCB -> R.drawable.ic_brand_rcb
    AppIconOption.MI -> R.drawable.ic_brand_mi
    AppIconOption.CSK -> R.drawable.ic_brand_csk
}

@Composable
fun AppIconSelectorSection(
    activeIcon: AppIconOption,
    onApplyIcon: (AppIconOption) -> Unit,
    onResetToDefault: () -> Unit
) {
    val colors = CucalcusTheme.colors
    var pendingSelection by remember(activeIcon) { mutableStateOf(activeIcon) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SwissSectionHeader(
            title = "App Icon Studio",
            subtitle = "8 Swiss-minimalist & IPL-inspired launcher icons"
        )

        // Live Launcher Preview Card
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = colors.cardBg,
            border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = pendingSelection.drawableRes()),
                        contentDescription = "${pendingSelection.displayName} live preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cucalcus",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primaryText
                    )
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "LIVE LAUNCHER PREVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.accent
                    )
                    Text(
                        text = pendingSelection.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.primaryText
                    )
                    Text(
                        text = pendingSelection.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.secondaryText
                    )
                    if (pendingSelection == activeIcon) {
                        Text(
                            text = "Currently active on home screen",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.success
                        )
                    }
                }
            }
        }

        // Two separate categories in responsive grids
        IconCategory.entries.forEach { category ->
            val itemsInCat = AppIconOption.entries.filter { it.category == category }
            Text(
                text = category.title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.primaryText
            )

            // 2x2 responsive grid inside verticalScroll
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsInCat.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { option ->
                            val isSelected = pendingSelection == option
                            val isActive = activeIcon == option
                            Surface(
                                onClick = { pendingSelection = option },
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSelected) colors.accent.copy(alpha = 0.14f) else colors.cardBg,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) colors.accent else colors.border
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("icon_option_${option.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Image(
                                            painter = painterResource(id = option.drawableRes()),
                                            contentDescription = option.displayName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(56.dp)
                                                .aspectRatio(1f)
                                                .align(Alignment.Center)
                                                .clip(RoundedCornerShape(12.dp))
                                        )
                                        if (isActive) {
                                            Icon(
                                                imageVector = Icons.Outlined.CheckCircle,
                                                contentDescription = "Active icon",
                                                tint = colors.accent,
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .align(Alignment.TopEnd)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = option.displayName,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isSelected) colors.accent else colors.primaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = option.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.secondaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Apply & Reset Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { onApplyIcon(pendingSelection) },
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.onAccent
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("apply_icon_button")
            ) {
                Text("APPLY ICON", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = {
                    pendingSelection = AppIconOption.QUADRANT
                    onResetToDefault()
                },
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.testTag("reset_icon_button")
            ) {
                Icon(Icons.Outlined.RestartAlt, contentDescription = null, tint = colors.primaryText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("DEFAULT", color = colors.primaryText)
            }
        }
    }
}
