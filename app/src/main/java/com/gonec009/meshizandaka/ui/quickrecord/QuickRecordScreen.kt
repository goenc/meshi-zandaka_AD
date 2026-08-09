package com.gonec009.meshizandaka.ui.quickrecord

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gonec009.meshizandaka.R
import com.gonec009.meshizandaka.data.AppContainer
import com.gonec009.meshizandaka.domain.model.MealTemplate
import com.gonec009.meshizandaka.domain.model.MealType
import com.gonec009.meshizandaka.ui.AppViewModelFactory
import com.gonec009.meshizandaka.ui.common.DriveCachedImage
import com.gonec009.meshizandaka.ui.common.InAppCameraCapture
import com.gonec009.meshizandaka.ui.common.MealPhotoWithDeleteAction
import com.gonec009.meshizandaka.ui.common.discardCapturedPhoto
import com.gonec009.meshizandaka.ui.common.isManagedPhotoInFolder
import com.gonec009.meshizandaka.ui.mealTypeLabel
import com.gonec009.meshizandaka.util.formatOneDecimal
import kotlinx.coroutines.launch

private const val MANUAL_TAB_INDEX = 0
private const val EATING_OUT_TAB_INDEX = 1

@Composable
fun QuickRecordRoute(
    container: AppContainer,
    innerPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
) {
    val viewModel: QuickRecordViewModel = viewModel(factory = AppViewModelFactory(container))
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    QuickRecordScreen(
        innerPadding = innerPadding,
        state = state,
        onManualTabSelected = viewModel::selectManualTab,
        onTemplateSelect = viewModel::selectTemplate,
        onDriveEatingOutCardSelect = viewModel::selectDriveEatingOutCard,
        onTemplateNameChange = viewModel::setTemplateName,
        onMealTypeSelect = viewModel::selectMealType,
        onTotalCaloriesChange = viewModel::setTotalCalories,
        onProteinChange = viewModel::setProtein,
        onFatChange = viewModel::setFat,
        onCarbChange = viewModel::setCarb,
        onMemoChange = viewModel::setMemo,
        onPhotoCaptured = viewModel::setPhotoUri,
        onPhotoRemoved = viewModel::clearPhoto,
        onSaveClick = viewModel::saveRecord,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickRecordScreen(
    innerPadding: PaddingValues,
    state: QuickRecordUiState,
    onManualTabSelected: () -> Unit,
    onTemplateSelect: (MealTemplate) -> Unit,
    onDriveEatingOutCardSelect: (QuickRecordDriveCard) -> Unit,
    onTemplateNameChange: (String) -> Unit,
    onMealTypeSelect: (MealType) -> Unit,
    onTotalCaloriesChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onFatChange: (String) -> Unit,
    onCarbChange: (String) -> Unit,
    onMemoChange: (String) -> Unit,
    onPhotoCaptured: (String?) -> Unit,
    onPhotoRemoved: () -> Unit,
    onSaveClick: () -> Unit,
) {
    var showCamera by remember { mutableStateOf(false) }
    var mealTypeExpanded by remember { mutableStateOf(false) }
    var showDeletePhotoDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by rememberSaveable { mutableStateOf(MANUAL_TAB_INDEX) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                listOf(
                    stringResource(R.string.quick_record_tab_manual),
                    stringResource(R.string.quick_record_tab_eating_out),
                ).forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                            if (index == MANUAL_TAB_INDEX) {
                                onManualTabSelected()
                            }
                        },
                        text = { Text(title) },
                    )
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val isManualTab = selectedTabIndex == MANUAL_TAB_INDEX
                if (isManualTab) {
                    val normalTemplates = state.availableTemplates.filter { it.mealType != MealType.EATING_OUT }
                    if (normalTemplates.isNotEmpty()) item {
                        TemplateSection(
                            title = stringResource(R.string.quick_record_templates),
                            templates = normalTemplates,
                            selectedTemplateId = state.selectedTemplate?.id,
                            onTemplateSelect = onTemplateSelect,
                        )
                    }
                } else if (state.driveEatingOutCards.isNotEmpty()) {
                    item {
                        DriveEatingOutCardSection(
                            cards = state.driveEatingOutCards,
                            selectedCardId = state.selectedDriveEatingOutCard?.id,
                            onCardSelect = onDriveEatingOutCardSelect,
                        )
                    }
                } else {
                    item {
                        Text(
                            text = stringResource(R.string.quick_record_no_eating_out_cards),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (!isManualTab) {
                    state.selectedDriveEatingOutCard
                        ?.takeIf { it.items.isNotEmpty() }
                        ?.let { card ->
                            item {
                                DriveEatingOutItemSection(
                                    card = card,
                                )
                            }
                        }
                }
                if (isManualTab || state.selectedDriveEatingOutCard != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                if (state.selectedDriveEatingOutCard == null) {
                                    CompactOutlinedField(
                                        value = state.templateName,
                                        onValueChange = onTemplateNameChange,
                                        label = { Text(stringResource(R.string.template_name)) },
                                        modifier = compactFieldModifier(),
                                        textStyle = compactFieldTextStyle(),
                                        singleLine = true,
                                    )
                                }
                                ExposedDropdownMenuBox(
                                    expanded = mealTypeExpanded,
                                    onExpandedChange = { mealTypeExpanded = it },
                                ) {
                                    LabeledMealSettingField(
                                        mealTypeLabel = stringResource(R.string.meal_type),
                                        selectedMealType = mealTypeLabel(state.selectedMealType),
                                        expanded = mealTypeExpanded,
                                        mealFieldModifier = Modifier.menuAnchor(),
                                    )
                                    DropdownMenu(
                                        expanded = mealTypeExpanded,
                                        onDismissRequest = { mealTypeExpanded = false },
                                    ) {
                                        MealType.entries
                                            .filterNot {
                                                it == MealType.EATING_OUT || it == MealType.SNACK
                                            }
                                            .forEach { mealType ->
                                                DropdownMenuItem(
                                                    text = { Text(mealTypeLabel(mealType)) },
                                                    onClick = {
                                                        mealTypeExpanded = false
                                                        onMealTypeSelect(mealType)
                                                    },
                                                )
                                            }
                                    }
                                }
                                if (state.selectedDriveEatingOutCard == null) {
                                    CompactOutlinedField(
                                        value = state.totalCalories,
                                        onValueChange = onTotalCaloriesChange,
                                        label = { Text(stringResource(R.string.base_calories)) },
                                        modifier = compactFieldModifier(),
                                        textStyle = compactFieldTextStyle(),
                                        singleLine = true,
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        CompactMacroField(
                                            label = stringResource(R.string.protein_short),
                                            value = state.proteinG,
                                            onValueChange = onProteinChange,
                                            modifier = Modifier.weight(1f),
                                        )
                                        CompactMacroField(
                                            label = stringResource(R.string.fat_short),
                                            value = state.fatG,
                                            onValueChange = onFatChange,
                                            modifier = Modifier.weight(1f),
                                        )
                                        CompactMacroField(
                                            label = stringResource(R.string.carb_short),
                                            value = state.carbG,
                                            onValueChange = onCarbChange,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    CompactOutlinedField(
                                        value = state.memo,
                                        onValueChange = onMemoChange,
                                        label = { Text(stringResource(R.string.memo)) },
                                        modifier = compactFieldModifier(),
                                        textStyle = compactFieldTextStyle(),
                                        minLines = 4,
                                        maxLines = 6,
                                    )
                                }
                                Button(
                                    onClick = onSaveClick,
                                    enabled = (state.selectedTemplate != null ||
                                        state.selectedDriveEatingOutCard != null) && !state.isSaving,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(stringResource(R.string.record_now))
                                }
                                if (state.selectedDriveEatingOutCard == null) {
                                    Button(
                                        onClick = { showCamera = true },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(stringResource(R.string.take_meal_photo))
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    ) {
                                        Text(
                                            text = if (state.photoUri == null) {
                                                stringResource(R.string.meal_photo_not_added)
                                            } else {
                                                stringResource(R.string.meal_photo_added)
                                            },
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    MealPhotoWithDeleteAction(
                                        uriString = state.photoUri,
                                        contentDescription = stringResource(R.string.meal_photo_added),
                                        onDeleteClick = { showDeletePhotoDialog = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 9f)
                                            .clip(RoundedCornerShape(8.dp)),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showCamera) {
            InAppCameraCapture(
                folderName = "quick_records",
                filePrefix = "quick_record",
                onCaptured = onPhotoCaptured,
                onDismiss = { showCamera = false },
            )
        }
        if (showDeletePhotoDialog && !state.photoUri.isNullOrBlank()) {
            AlertDialog(
                onDismissRequest = { showDeletePhotoDialog = false },
                text = { Text(stringResource(R.string.delete_photo_confirm_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val currentPhotoUri = state.photoUri
                            if (currentPhotoUri != state.selectedTemplate?.photoUri &&
                                isManagedPhotoInFolder(currentPhotoUri, "quick_records")
                            ) {
                                scope.launch {
                                    discardCapturedPhoto(context, currentPhotoUri)
                                }
                            }
                            onPhotoRemoved()
                            showDeletePhotoDialog = false
                        },
                    ) {
                        Text(stringResource(R.string.yes))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeletePhotoDialog = false }) {
                        Text(stringResource(R.string.no))
                    }
                },
            )
        }
    }
}

@Composable
private fun TemplateSection(
    title: String,
    templates: List<MealTemplate>,
    selectedTemplateId: Long?,
    onTemplateSelect: (MealTemplate) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(templates, key = { it.id }) { template ->
                FilterChip(
                    selected = selectedTemplateId == template.id,
                    onClick = { onTemplateSelect(template) },
                    label = { Text(template.name) },
                )
            }
        }
    }
}

@Composable
private fun DriveEatingOutCardSection(
    cards: List<QuickRecordDriveCard>,
    selectedCardId: String?,
    onCardSelect: (QuickRecordDriveCard) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(cards, key = { it.id }) { card ->
                Card(
                    modifier = Modifier
                        .width(132.dp)
                        .clickable { onCardSelect(card) },
                    border = if (selectedCardId == card.id) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        null
                    },
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        card.imagePath?.let { imagePath ->
                            DriveCachedImage(
                                path = imagePath,
                                contentDescription = card.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                        }
                        Text(
                            text = card.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DriveEatingOutItemSection(
    card: QuickRecordDriveCard,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(card.name, fontWeight = FontWeight.SemiBold)
                Text(
                    text = stringResource(R.string.kcal_format, card.calories),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "P ${formatOneDecimal(card.proteinG)}g / F ${formatOneDecimal(card.fatG)}g / C ${formatOneDecimal(card.carbG)}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                card.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        item.imagePath?.let { imagePath ->
                            DriveCachedImage(
                                path = imagePath,
                                contentDescription = item.name,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = item.amountLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = stringResource(R.string.kcal_format, item.calories),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun compactFieldModifier(): Modifier {
    return Modifier
        .fillMaxWidth()
        .heightIn(min = 38.dp)
}

private fun compactFieldTextStyle(): TextStyle {
    return TextStyle(fontSize = 14.sp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabeledMealSettingField(
    mealTypeLabel: String,
    selectedMealType: String,
    expanded: Boolean,
    mealFieldModifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(84.dp)) {
            Text(mealTypeLabel)
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = mealFieldModifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = selectedMealType,
                        modifier = Modifier.weight(1f),
                        style = compactFieldTextStyle().copy(color = MaterialTheme.colorScheme.onSurface),
                    )
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            }
        }
    }
}

@Composable
private fun CompactMacroField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier.heightIn(min = 38.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = label,
                style = compactFieldTextStyle(),
                fontWeight = FontWeight.Medium,
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = compactFieldTextStyle().copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                interactionSource = interactionSource,
            )
        }
    }
}

@Composable
private fun CompactOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    textStyle: TextStyle = compactFieldTextStyle(),
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(84.dp)
                .padding(top = 8.dp),
        ) {
            label?.invoke()
        }
        Surface(
            modifier = modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (readOnly) {
                        Text(
                            text = value,
                            style = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                            fontWeight = FontWeight.Normal,
                        )
                    } else {
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = enabled,
                            readOnly = false,
                            singleLine = singleLine,
                            minLines = minLines,
                            maxLines = maxLines,
                            textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            interactionSource = interactionSource,
                        )
                    }
                }
                trailingIcon?.invoke()
            }
        }
    }
}
