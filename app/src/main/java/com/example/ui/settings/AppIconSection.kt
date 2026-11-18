package com.example.ui.settings

import android.content.Context
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

@DrawableRes
fun AppIconOption.drawableRes(): Int = when (this) {
    AppIconOption.NORMAL_GOLD -> R.drawable.ic_brand_normal_gold
    AppIconOption.NORMAL_TEAL -> R.drawable.ic_brand_normal_teal
    AppIconOption.NORMAL_GREEN -> R.drawable.ic_brand_normal_green
    AppIconOption.NORMAL_MAROON -> R.drawable.ic_brand_normal_maroon
    AppIconOption.NORMAL_CRIMSON -> R.drawable.ic_brand_normal_crimson
    AppIconOption.NORMAL_BLUE -> R.drawable.ic_brand_normal_blue
    AppIconOption.NORMAL_PURPLE -> R.drawable.ic_brand_normal_purple
    AppIconOption.IPL_GT -> R.drawable.ic_brand_ipl_gt
    AppIconOption.IPL_KKR -> R.drawable.ic_brand_ipl_kkr
    AppIconOption.IPL_RCB -> R.drawable.ic_brand_ipl_rcb
    AppIconOption.IPL_SRH -> R.drawable.ic_brand_ipl_srh
    AppIconOption.IPL_MI -> R.drawable.ic_brand_ipl_mi
    AppIconOption.IPL_RR -> R.drawable.ic_brand_ipl_rr
    AppIconOption.IPL_CSK -> R.drawable.ic_brand_ipl_csk
    AppIconOption.INDIA_FLAG -> R.drawable.ic_brand_india_flag
}

/**
 * Decodes all 15 brand icons off the main thread (during Settings skeleton phase)
 * so switching to Settings or App Icons renders with zero UI thread decode stutter.
 */
object BrandIconBitmapCache {
    private val cache = ConcurrentHashMap<Int, ImageBitmap>()

    fun getOrNull(@DrawableRes resId: Int): ImageBitmap? = cache[resId]

    suspend fun warmUp(context: Context) {
        withContext(Dispatchers.IO) {
            val res = context.resources
            val opts = BitmapFactory.Options().apply {
                inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
            }
            for (option in AppIconOption.entries) {
                val resId = option.drawableRes()
                if (!cache.containsKey(resId)) {
                    runCatching {
                        BitmapFactory.decodeResource(res, resId, opts)?.asImageBitmap()
                    }.getOrNull()?.let { bmp ->
                        cache[resId] = bmp
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandIconImage(
    @DrawableRes resId: Int,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val cached = remember(resId) { BrandIconBitmapCache.getOrNull(resId) }
    if (cached != null) {
        Image(
            bitmap = cached,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(id = resId),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

@Composable
fun AppIconSelectorSection(
    activeIcon: AppIconOption,
    onApplyIcon: (AppIconOption) -> Unit,
    onResetToDefault: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val colors = CucalcusTheme.colors
    var pendingSelection by remember(activeIcon) { mutableStateOf(activeIcon) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SwissSectionHeader(
            title = "App Icon Studio",
            subtitle = "Choose from 15 custom glossy icons (Green Edition is default)"
        )

        // Live Launcher Preview Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = colors.cardBg,
            border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BrandIconImage(
                        resId = pendingSelection.drawableRes(),
                        contentDescription = "${pendingSelection.displayName} live preview",
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
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
                            text = "Currently active in app drawer",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.success
                        )
                    }
                }
            }
        }

        // Two separate built-in categories in clean 2x2 grids
        IconCategory.entries.forEach { category ->
            val itemsInCat = AppIconOption.entries.filter { it.category == category }
            Text(
                text = category.title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.primaryText
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsInCat.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { option ->
                            val isSelected = pendingSelection == option
                            val isActive = activeIcon == option
                            val cardColor = if (isSelected) colors.accent.copy(alpha = 0.14f) else colors.cardBg

                            Surface(
                                onClick = { pendingSelection = option },
                                shape = RoundedCornerShape(20.dp),
                                color = cardColor,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) colors.accent else colors.border
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("icon_option_${option.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        BrandIconImage(
                                            resId = option.drawableRes(),
                                            contentDescription = option.displayName,
                                            modifier = Modifier
                                                .size(62.dp)
                                                .aspectRatio(1f)
                                                .align(Alignment.Center)
                                                .clip(RoundedCornerShape(16.dp))
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
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.onAccent
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("apply_icon_button")
            ) {
                Text("APPLY APP ICON", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = {
                    pendingSelection = AppIconOption.NORMAL_GREEN
                    onResetToDefault()
                },
                shape = RoundedCornerShape(50),
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
