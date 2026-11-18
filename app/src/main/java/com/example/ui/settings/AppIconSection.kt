package com.example.ui.settings

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.outlined.AddToHomeScreen
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.preferences.AppIconOption
import com.example.data.preferences.IconCategory
import com.example.platform.CustomIconManager
import com.example.platform.PinShortcutOutcome
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
    customIconPath: String?,
    onApplyIcon: (AppIconOption) -> Unit,
    onResetToDefault: () -> Unit,
    onImportGalleryIcon: (Uri, (Boolean) -> Unit) -> Unit,
    onPinGalleryIcon: ((PinShortcutOutcome) -> Unit) -> Unit,
    onClearGalleryIcon: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val colors = CucalcusTheme.colors
    val context = LocalContext.current
    var pendingSelection by remember(activeIcon) { mutableStateOf(activeIcon) }
    var customIconBitmap by remember(customIconPath) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(customIconPath) {
        customIconBitmap = if (!customIconPath.isNullOrBlank()) {
            CustomIconManager(context).loadPreviewBitmap(customIconPath)
        } else {
            null
        }
    }

    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onImportGalleryIcon(uri) { ok ->
                if (ok) {
                    onShowMessage("Gallery icon ready — tap Pin to Home to add it")
                } else {
                    onShowMessage("Could not load selected icon image")
                }
            }
        }
    }

    val activeCustom = customIconBitmap
    val hasCustomIcon = activeCustom != null

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SwissSectionHeader(
            title = "App Icon Studio",
            subtitle = "8 custom glossy icons + gallery launcher shortcut icon"
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
                    if (activeCustom != null) {
                        Image(
                            bitmap = activeCustom.asImageBitmap(),
                            contentDescription = "Custom gallery icon preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .border(1.dp, colors.accent.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        )
                    } else {
                        Image(
                            painter = painterResource(id = pendingSelection.drawableRes()),
                            contentDescription = "${pendingSelection.displayName} live preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(18.dp))
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Cucalcus",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primaryText
                    )
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (hasCustomIcon) "CUSTOM GALLERY ICON" else "LIVE LAUNCHER PREVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.accent
                    )
                    Text(
                        text = if (hasCustomIcon) "Gallery Icon" else pendingSelection.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.primaryText
                    )
                    Text(
                        text = if (hasCustomIcon) {
                            "Pin this custom photo as an app shortcut icon on your home screen."
                        } else {
                            pendingSelection.subtitle
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.secondaryText
                    )
                    if (!hasCustomIcon && pendingSelection == activeIcon) {
                        Text(
                            text = "Currently active in app drawer",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.success
                        )
                    }
                }
            }
        }

        // Custom Icon from Gallery Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = colors.cardBg,
            border = BorderStroke(1.dp, if (hasCustomIcon) colors.accent.copy(alpha = 0.65f) else colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.accent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoLibrary,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Custom Icon from Gallery",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.primaryText
                        )
                        Text(
                            text = "Pick any photo to create a custom Cucalcus icon shortcut on your home screen.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.secondaryText
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            iconPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.onAccent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_gallery_icon_button")
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (hasCustomIcon) "CHANGE PHOTO" else "PICK FROM GALLERY", fontWeight = FontWeight.Bold)
                    }

                    if (hasCustomIcon) {
                        OutlinedButton(
                            onClick = {
                                onPinGalleryIcon { outcome ->
                                    when (outcome) {
                                        PinShortcutOutcome.REQUESTED ->
                                            onShowMessage("Confirm adding Cucalcus shortcut icon to your home screen")
                                        PinShortcutOutcome.UNSUPPORTED_LAUNCHER ->
                                            onShowMessage("Your launcher does not support pinned custom icons")
                                        PinShortcutOutcome.MISSING_IMAGE ->
                                            onShowMessage("Pick a gallery icon photo first")
                                        PinShortcutOutcome.FAILED ->
                                            onShowMessage("Could not create home screen icon shortcut")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, colors.accent),
                            modifier = Modifier.testTag("pin_gallery_icon_button")
                        ) {
                            Icon(Icons.Outlined.AddToHomeScreen, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PIN TO HOME", color = colors.accent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (hasCustomIcon) {
                    OutlinedButton(
                        onClick = {
                            onClearGalleryIcon()
                            onShowMessage("Custom gallery icon removed")
                        },
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, colors.error.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("remove_gallery_icon_button")
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = colors.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("REMOVE GALLERY ICON", color = colors.error)
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
                            val scale by animateFloatAsState(
                                targetValue = if (isSelected) 1.02f else 1f,
                                animationSpec = spring(dampingRatio = 0.72f, stiffness = 450f),
                                label = "icon_card_scale"
                            )
                            val cardColor by animateColorAsState(
                                targetValue = if (isSelected) colors.accent.copy(alpha = 0.14f) else colors.cardBg,
                                label = "icon_card_bg"
                            )
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
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .testTag("icon_option_${option.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Image(
                                            painter = painterResource(id = option.drawableRes()),
                                            contentDescription = option.displayName,
                                            contentScale = ContentScale.Crop,
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
                    pendingSelection = AppIconOption.QUADRANT
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
