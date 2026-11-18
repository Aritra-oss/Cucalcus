package com.example.ui.converter

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.CurrencyRepository
import com.example.domain.converter.UnitCategory
import com.example.domain.converter.UnitConverterEngine
import com.example.domain.math.NumberFormatMode
import com.example.domain.math.NumberFormatter
import com.example.ui.components.SwissScreenTopBar
import com.example.ui.components.SwissSectionHeader
import com.example.ui.theme.CucalcusTheme
import com.example.ui.theme.LocalDisplayFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConverterScreen(
    viewModel: ConverterViewModel,
    onShowMessage: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = CucalcusTheme.colors

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        SwissScreenTopBar(
            title = "Precision Converter",
            tag = "15 Unit Systems & FX"
        ) {
            if (state.activeTab == ConverterTab.CURRENCY) {
                IconButton(
                    onClick = viewModel::refreshExchangeRates,
                    modifier = Modifier.testTag("currency_refresh_button")
                ) {
                    if (state.isLoadingRates) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = colors.accent
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh exchange rates",
                            tint = colors.accent
                        )
                    }
                }
            }
        }

        // Smooth Pill Segmented Switcher
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.secondaryBg,
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ConverterTab.entries.forEach { tab ->
                    val selected = state.activeTab == tab
                    val bgColor by animateColorAsState(
                        targetValue = if (selected) colors.accent else Color.Transparent,
                        label = "conv_tab_bg"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) colors.onAccent else colors.secondaryText,
                        label = "conv_tab_fg"
                    )
                    Surface(
                        onClick = { viewModel.selectTab(tab) },
                        shape = RoundedCornerShape(16.dp),
                        color = bgColor,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("converter_tab_${tab.name}")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (tab == ConverterTab.UNITS) {
                                    Icons.Outlined.Straighten
                                } else {
                                    Icons.Outlined.CurrencyExchange
                                },
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = contentColor
                            )
                        }
                    }
                }
            }
        }

        if (state.activeTab == ConverterTab.UNITS) {
            UnitConverterContent(
                state = state,
                viewModel = viewModel,
                onShowMessage = onShowMessage
            )
        } else {
            CurrencyConverterContent(
                state = state,
                viewModel = viewModel,
                onShowMessage = onShowMessage
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitConverterContent(
    state: ConverterUiState,
    viewModel: ConverterViewModel,
    onShowMessage: (String) -> Unit
) {
    val colors = CucalcusTheme.colors
    val displayFont = LocalDisplayFontFamily.current
    val clipboard = LocalClipboardManager.current

    var unitPickerTargetIsFrom by remember { mutableStateOf<Boolean?>(null) }
    var unitSearchQuery by remember { mutableStateOf("") }
    var swapRotationTarget by remember { mutableFloatStateOf(0f) }
    val swapRotation by animateFloatAsState(
        targetValue = swapRotationTarget,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 420f),
        label = "unit_swap_rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Horizontal 15-category pill selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UnitCategory.entries.forEach { category ->
                val selected = state.selectedCategory == category
                Surface(
                    onClick = { viewModel.selectCategory(category) },
                    shape = RoundedCornerShape(50),
                    color = if (selected) colors.accent.copy(alpha = 0.18f) else colors.cardBg,
                    border = BorderStroke(1.dp, if (selected) colors.accent else colors.border),
                    modifier = Modifier.testTag("unit_cat_${category.id}")
                ) {
                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = if (selected) colors.accent else colors.secondaryText,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Unified From -> Floating Swap -> To Studio Card Stack
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // From Unit Card
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = colors.cardBg,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SOURCE UNIT", style = MaterialTheme.typography.labelSmall, color = colors.accent)
                            Surface(
                                onClick = {
                                    unitSearchQuery = ""
                                    unitPickerTargetIsFrom = true
                                },
                                shape = RoundedCornerShape(50),
                                color = colors.elevatedSurface,
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier.testTag("unit_from_selector")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${state.fromUnit.name} (${state.fromUnit.symbol})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = colors.primaryText
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        OutlinedTextField(
                            value = state.unitInputText,
                            onValueChange = viewModel::updateUnitInput,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (state.selectedCategory == UnitCategory.TEMPERATURE) {
                                    KeyboardType.Ascii
                                } else {
                                    KeyboardType.Decimal
                                }
                            ),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.headlineMedium.copy(fontFamily = displayFont),
                            shape = RoundedCornerShape(16.dp),
                            trailingIcon = {
                                if (state.unitInputText.isNotEmpty()) {
                                    IconButton(onClick = viewModel::clearUnitInput) {
                                        Icon(Icons.Outlined.Clear, contentDescription = "Clear input", tint = colors.secondaryText)
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.accent,
                                unfocusedBorderColor = colors.border,
                                focusedTextColor = colors.primaryText,
                                unfocusedTextColor = colors.primaryText,
                                cursorColor = colors.accent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("unit_input_field")
                        )

                        // Quick Smart Preset Values
                        val quickValues = if (state.selectedCategory == UnitCategory.TEMPERATURE) {
                            listOf("-40", "0", "25", "100", "212")
                        } else {
                            listOf("1", "10", "50", "100", "1000")
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickValues.forEach { preset ->
                                Surface(
                                    onClick = { viewModel.updateUnitInput(preset) },
                                    shape = RoundedCornerShape(50),
                                    color = if (state.unitInputText == preset) colors.accent.copy(alpha = 0.16f) else colors.elevatedSurface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (state.unitInputText == preset) colors.accent else colors.border
                                    )
                                ) {
                                    Text(
                                        text = "$preset ${state.fromUnit.symbol}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (state.unitInputText == preset) colors.accent else colors.secondaryText,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // To Unit Card
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = colors.cardBg,
                    border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("CONVERTED RESULT", style = MaterialTheme.typography.labelSmall, color = colors.accent)
                            Surface(
                                onClick = {
                                    unitSearchQuery = ""
                                    unitPickerTargetIsFrom = false
                                },
                                shape = RoundedCornerShape(50),
                                color = colors.elevatedSurface,
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier.testTag("unit_to_selector")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${state.toUnit.name} (${state.toUnit.symbol})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = colors.primaryText
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        AnimatedContent(
                            targetState = "${state.unitOutputText} ${state.toUnit.symbol}",
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "unit_output_anim"
                        ) { formatted ->
                            Text(
                                text = formatted,
                                fontFamily = displayFont,
                                style = MaterialTheme.typography.displayMedium,
                                color = colors.primaryText,
                                modifier = Modifier.testTag("unit_output_text")
                            )
                        }

                        val unitOneRatio = remember(state.fromUnit, state.toUnit) {
                            val one = UnitConverterEngine.convert(1.0, state.fromUnit, state.toUnit)
                            if (one.isFinite()) {
                                "1 ${state.fromUnit.symbol} = ${NumberFormatter.formatResult(one, 6, NumberFormatMode.INTERNATIONAL)} ${state.toUnit.symbol}"
                            } else {
                                ""
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = unitOneRatio,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.secondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    onClick = {
                                        viewModel.saveCurrentUnitConversion()
                                        onShowMessage("Saved to recent conversions")
                                    },
                                    shape = RoundedCornerShape(50),
                                    color = colors.elevatedSurface,
                                    border = BorderStroke(1.dp, colors.border)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Outlined.BookmarkAdd, contentDescription = "Save conversion", tint = colors.accent, modifier = Modifier.size(15.dp))
                                        Text("Save", style = MaterialTheme.typography.labelMedium, color = colors.primaryText)
                                    }
                                }
                                Surface(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(state.unitOutputText))
                                        onShowMessage("Copied ${state.unitOutputText}")
                                    },
                                    shape = RoundedCornerShape(50),
                                    color = colors.accent.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy result", tint = colors.accent, modifier = Modifier.size(15.dp))
                                        Text("Copy", style = MaterialTheme.typography.labelMedium, color = colors.accent)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating Animated Swap Button bridging the two cards
            Surface(
                onClick = {
                    swapRotationTarget += 180f
                    viewModel.swapUnits()
                },
                shape = CircleShape,
                color = colors.elevatedSurface,
                border = BorderStroke(1.5.dp, colors.accent.copy(alpha = 0.7f)),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(46.dp)
                    .testTag("unit_swap_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.SwapVert,
                        contentDescription = "Swap units",
                        tint = colors.accent,
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer { rotationZ = swapRotation }
                    )
                }
            }
        }

        // Smart Instant Multi-Unit Comparison Card
        val siblingUnits = remember(state.selectedCategory, state.fromUnit, state.toUnit) {
            UnitConverterEngine.unitsByCategory[state.selectedCategory]
                .orEmpty()
                .filter { it.id != state.fromUnit.id && it.id != state.toUnit.id }
                .take(4)
        }
        val parsedInputVal = remember(state.unitInputText) {
            state.unitInputText.trim().replace(',', '.').replace('−', '-').toDoubleOrNull()
        }
        if (siblingUnits.isNotEmpty() && parsedInputVal != null) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = colors.secondaryBg,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "INSTANT EQUIVALENTS · TAP TO TARGET",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.accent
                    )
                    siblingUnits.forEachIndexed { idx, sibling ->
                        val converted = UnitConverterEngine.convert(parsedInputVal, state.fromUnit, sibling)
                        val formatted = if (converted.isFinite()) {
                            NumberFormatter.formatResult(converted, 6, NumberFormatMode.INTERNATIONAL)
                        } else {
                            "—"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.selectUnit(sibling, isFrom = false) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sibling.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.secondaryText
                            )
                            Text(
                                text = "$formatted ${sibling.symbol}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.primaryText
                            )
                        }
                        if (idx < siblingUnits.lastIndex) {
                            HorizontalDivider(thickness = 0.8.dp, color = colors.border.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }

        // Recent & Favourite Unit Conversions
        if (state.recentUnitConversions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            SwissSectionHeader(
                title = "Recent & Favourites",
                subtitle = "Tap to load conversion"
            )
            state.recentUnitConversions.forEach { item ->
                key(item.id) {
                    val fromName = UnitConverterEngine.findUnitById(item.fromCode)?.symbol ?: item.fromCode
                    val toName = UnitConverterEngine.findUnitById(item.toCode)?.symbol ?: item.toCode
                    RecentConversionRow(
                        title = "${item.inputValue} $fromName → ${item.outputValue} $toName",
                        subtitle = item.categoryId.replace("_", " ").uppercase(),
                        isFavorite = item.isFavorite,
                        onTap = { viewModel.applyRecentUnitConversion(item) },
                        onToggleFav = { viewModel.toggleConversionFavorite(item) },
                        onDelete = { viewModel.deleteRecentConversion(item.id) }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (unitPickerTargetIsFrom != null) {
        val isFrom = unitPickerTargetIsFrom == true
        ModalBottomSheet(
            onDismissRequest = { unitPickerTargetIsFrom = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = colors.secondaryBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isFrom) "Select Source Unit" else "Select Target Unit",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = unitSearchQuery,
                    onValueChange = { unitSearchQuery = it },
                    placeholder = { Text("Search all 15 categories & units", color = colors.secondaryText) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.secondaryText) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.primaryText,
                        unfocusedTextColor = colors.primaryText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("unit_search_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                val q = unitSearchQuery.trim()
                val candidates = if (q.isEmpty()) {
                    UnitConverterEngine.unitsByCategory[state.selectedCategory].orEmpty()
                } else {
                    UnitConverterEngine.allUnits.filter {
                        it.name.contains(q, ignoreCase = true) ||
                            it.symbol.contains(q, ignoreCase = true) ||
                            it.category.displayName.contains(q, ignoreCase = true)
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    candidates.forEach { unit ->
                        key(unit.id) {
                            Surface(
                                onClick = {
                                    viewModel.selectUnit(unit, isFrom)
                                    unitPickerTargetIsFrom = null
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = colors.cardBg,
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(unit.name, style = MaterialTheme.typography.bodyLarge, color = colors.primaryText)
                                        Text(unit.category.displayName, style = MaterialTheme.typography.labelSmall, color = colors.secondaryText)
                                    }
                                    Text(unit.symbol, style = MaterialTheme.typography.titleMedium, color = colors.accent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyConverterContent(
    state: ConverterUiState,
    viewModel: ConverterViewModel,
    onShowMessage: (String) -> Unit
) {
    val colors = CucalcusTheme.colors
    val displayFont = LocalDisplayFontFamily.current
    val clipboard = LocalClipboardManager.current

    var currencyPickerIsFrom by remember { mutableStateOf<Boolean?>(null) }
    var currencySearchQuery by remember { mutableStateOf("") }
    var swapRotationTarget by remember { mutableFloatStateOf(0f) }
    val swapRotation by animateFloatAsState(
        targetValue = swapRotationTarget,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 420f),
        label = "fx_swap_rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Rate Freshness & Network Status Banner
        val snapshot = state.ratesSnapshot
        val statusLabel = when {
            state.isLoadingRates -> "Fetching latest market rates..."
            snapshot == null && state.networkError != null -> "Offline — no cached rates available"
            snapshot == null -> "Waiting for exchange rates..."
            snapshot.isCached && snapshot.isStale -> "CACHED (STALE) · Offline rates"
            snapshot.isCached -> "CACHED · Saved offline rates"
            else -> "LIVE RATES · Freshly retrieved"
        }
        val statusColor = when {
            state.networkError != null || snapshot?.isStale == true -> colors.error
            snapshot?.isCached == true -> colors.accent
            else -> colors.success
        }
        val timeText = remember(snapshot?.fetchedAtMs) {
            snapshot?.fetchedAtMs?.let {
                SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(it))
            } ?: "—"
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.secondaryBg,
            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (state.networkError != null) {
                            Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = statusColor
                        )
                    }
                    Text(
                        text = "Updated: $timeText",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.secondaryText
                    )
                }
                if (state.networkError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Network error: ${state.networkError}. ${if (snapshot != null) "Using cached rates." else "Check connection and refresh."}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.secondaryText
                    )
                }
                if (state.exchangeRateDisplay.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.exchangeRateDisplay,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.primaryText
                    )
                }
            }
        }

        // Stacked From -> Swap -> To Currency Studio Card
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = colors.cardBg,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("FROM CURRENCY", style = MaterialTheme.typography.labelSmall, color = colors.accent)
                            Surface(
                                onClick = {
                                    currencySearchQuery = ""
                                    currencyPickerIsFrom = true
                                },
                                shape = RoundedCornerShape(50),
                                color = colors.elevatedSurface,
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier.testTag("currency_from_selector")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${state.fromCurrency.flag} ${state.fromCurrency.code} (${state.fromCurrency.symbol})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = colors.primaryText
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        OutlinedTextField(
                            value = state.currencyInputText,
                            onValueChange = viewModel::updateCurrencyInput,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            prefix = { Text("${state.fromCurrency.symbol} ", color = colors.accent) },
                            trailingIcon = {
                                if (state.currencyInputText.isNotEmpty()) {
                                    IconButton(onClick = viewModel::clearCurrencyInput) {
                                        Icon(Icons.Outlined.Clear, contentDescription = "Clear", tint = colors.secondaryText)
                                    }
                                }
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.headlineMedium.copy(fontFamily = displayFont),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.accent,
                                unfocusedBorderColor = colors.border,
                                focusedTextColor = colors.primaryText,
                                unfocusedTextColor = colors.primaryText,
                                cursorColor = colors.accent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("currency_input_field")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("1", "10", "50", "100", "1000").forEach { preset ->
                                Surface(
                                    onClick = { viewModel.updateCurrencyInput(preset) },
                                    shape = RoundedCornerShape(50),
                                    color = if (state.currencyInputText == preset) colors.accent.copy(alpha = 0.16f) else colors.elevatedSurface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (state.currencyInputText == preset) colors.accent else colors.border
                                    )
                                ) {
                                    Text(
                                        text = "${state.fromCurrency.symbol}$preset",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (state.currencyInputText == preset) colors.accent else colors.secondaryText,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = colors.cardBg,
                    border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TO CURRENCY", style = MaterialTheme.typography.labelSmall, color = colors.accent)
                            Surface(
                                onClick = {
                                    currencySearchQuery = ""
                                    currencyPickerIsFrom = false
                                },
                                shape = RoundedCornerShape(50),
                                color = colors.elevatedSurface,
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier.testTag("currency_to_selector")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${state.toCurrency.flag} ${state.toCurrency.code} (${state.toCurrency.symbol})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = colors.primaryText
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        AnimatedContent(
                            targetState = "${state.toCurrency.symbol} ${state.currencyOutputText}",
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "fx_output_anim"
                        ) { formatted ->
                            Text(
                                text = formatted,
                                fontFamily = displayFont,
                                style = MaterialTheme.typography.displayMedium,
                                color = colors.primaryText,
                                modifier = Modifier.testTag("currency_output_text")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = {
                                    viewModel.saveCurrentCurrencyConversion()
                                    onShowMessage("Saved to recent conversions")
                                },
                                shape = RoundedCornerShape(50),
                                color = colors.elevatedSurface,
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Outlined.BookmarkAdd, contentDescription = "Save", tint = colors.accent, modifier = Modifier.size(15.dp))
                                    Text("Save", style = MaterialTheme.typography.labelMedium, color = colors.primaryText)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                onClick = {
                                    clipboard.setText(AnnotatedString(state.currencyOutputText))
                                    onShowMessage("Copied ${state.currencyOutputText}")
                                },
                                shape = RoundedCornerShape(50),
                                color = colors.accent.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = colors.accent, modifier = Modifier.size(15.dp))
                                    Text("Copy", style = MaterialTheme.typography.labelMedium, color = colors.accent)
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                onClick = {
                    swapRotationTarget += 180f
                    viewModel.swapCurrencies()
                },
                shape = CircleShape,
                color = colors.elevatedSurface,
                border = BorderStroke(1.5.dp, colors.accent.copy(alpha = 0.7f)),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(46.dp)
                    .testTag("currency_swap_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.SwapVert,
                        contentDescription = "Swap currencies",
                        tint = colors.accent,
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer { rotationZ = swapRotation }
                    )
                }
            }
        }

        if (state.recentCurrencyConversions.isNotEmpty()) {
            SwissSectionHeader(title = "Recent & Favourites", subtitle = "Tap to load pair")
            state.recentCurrencyConversions.forEach { item ->
                key(item.id) {
                    val f = CurrencyRepository.findCurrency(item.fromCode)
                    val t = CurrencyRepository.findCurrency(item.toCode)
                    RecentConversionRow(
                        title = "${f.flag} ${item.inputValue} ${f.code} → ${t.flag} ${item.outputValue} ${t.code}",
                        subtitle = "CURRENCY PAIR",
                        isFavorite = item.isFavorite,
                        onTap = { viewModel.applyRecentCurrencyConversion(item) },
                        onToggleFav = { viewModel.toggleConversionFavorite(item) },
                        onDelete = { viewModel.deleteRecentConversion(item.id) }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (currencyPickerIsFrom != null) {
        val isFrom = currencyPickerIsFrom == true
        ModalBottomSheet(
            onDismissRequest = { currencyPickerIsFrom = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = colors.secondaryBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isFrom) "Select Source Currency" else "Select Target Currency",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = currencySearchQuery,
                    onValueChange = { currencySearchQuery = it },
                    placeholder = { Text("Search code, currency, or country", color = colors.secondaryText) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.secondaryText) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.primaryText,
                        unfocusedTextColor = colors.primaryText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("currency_search_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                val q = currencySearchQuery.trim()
                val itemsList = CurrencyRepository.supportedCurrencies.filter {
                    q.isEmpty() || it.code.contains(q, ignoreCase = true) || it.name.contains(q, ignoreCase = true)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsList.forEach { currency ->
                        key(currency.code) {
                            Surface(
                                onClick = {
                                    viewModel.selectCurrency(currency, isFrom)
                                    currencyPickerIsFrom = null
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = colors.cardBg,
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(currency.flag, style = MaterialTheme.typography.titleLarge)
                                        Column {
                                            Text("${currency.code} · ${currency.name}", style = MaterialTheme.typography.bodyLarge, color = colors.primaryText)
                                        }
                                    }
                                    Text(currency.symbol, style = MaterialTheme.typography.titleMedium, color = colors.accent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentConversionRow(
    title: String,
    subtitle: String,
    isFavorite: Boolean,
    onTap: () -> Unit,
    onToggleFav: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = CucalcusTheme.colors
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.cardBg,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onTap)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.secondaryText
                )
            }
            IconButton(onClick = onToggleFav, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favourite conversion",
                    tint = if (isFavorite) colors.accent else colors.secondaryText,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete recent conversion",
                    tint = colors.secondaryText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
