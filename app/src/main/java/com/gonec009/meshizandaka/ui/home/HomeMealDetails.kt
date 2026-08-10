package com.gonec009.meshizandaka.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gonec009.meshizandaka.R
import com.gonec009.meshizandaka.data.drive.DriveExternalCard
import com.gonec009.meshizandaka.data.drive.DriveFood
import com.gonec009.meshizandaka.data.drive.DrivePlan
import com.gonec009.meshizandaka.data.drive.DrivePlanItem
import com.gonec009.meshizandaka.data.drive.DrivePlanMeal
import com.gonec009.meshizandaka.data.drive.recordKey
import com.gonec009.meshizandaka.domain.model.MealRecord
import com.gonec009.meshizandaka.domain.model.MealRecordOption
import com.gonec009.meshizandaka.domain.model.MealType
import com.gonec009.meshizandaka.ui.common.DriveCachedImage
import com.gonec009.meshizandaka.ui.common.MealPhoto
import com.gonec009.meshizandaka.ui.registeredMealLabel
import com.gonec009.meshizandaka.util.formatOneDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class NutritionTotals(
    val proteinG: Double,
    val fatG: Double,
    val carbG: Double,
)

private const val FoodOptionGroupName = "食品"

internal fun List<MealRecord>.pfcTotals(): NutritionTotals {
    return NutritionTotals(
        proteinG = sumOf { it.proteinG },
        fatG = sumOf { it.fatG },
        carbG = sumOf { it.carbG },
    )
}

private fun formatPfcSummary(totals: NutritionTotals): String {
    return "P ${formatOneDecimal(totals.proteinG)}g / F ${formatOneDecimal(totals.fatG)}g / C ${formatOneDecimal(totals.carbG)}g"
}

@Composable
private fun MealRecordOptionRows(options: List<MealRecordOption>) {
    options.forEachIndexed { index, option ->
        key(option.id.takeIf { it > 0L } ?: "${option.optionGroupNameSnapshot}:${option.optionNameSnapshot}:$index") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = option.optionNameSnapshot,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun MealRecordSection(
    records: List<MealRecord>,
    total: NutritionTotals,
    detailsReady: Boolean,
    onRecordClick: (MealRecord) -> Unit,
    onDeleteClick: (MealRecord) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatPfcSummary(total),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (records.isEmpty()) {
            Text(
                text = stringResource(R.string.no_records),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            records.forEachIndexed { index, record ->
                key(record.id.takeIf { it > 0L } ?: "${record.eatenAt}:$index") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = detailsReady) { onRecordClick(record) }
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = registeredMealLabel(record.mealType),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(R.string.kcal_format, record.totalCalories),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = "${stringResource(R.string.protein_short)} ${formatOneDecimal(record.proteinG)}g / ${stringResource(R.string.fat_short)} ${formatOneDecimal(record.fatG)}g / ${stringResource(R.string.carb_short)} ${formatOneDecimal(record.carbG)}g",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onDeleteClick(record) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MealRecordContentDialog(
    record: MealRecord,
    selectedDrivePlan: DrivePlan?,
    externalCards: List<DriveExternalCard>,
    foods: List<DriveFood>,
    onDismiss: () -> Unit,
    onDeleteDrivePlanItemClick: (MealRecord, DrivePlanItem, String) -> Unit,
    onDeleteRecordOptionClick: (MealRecord, MealRecordOption) -> Unit,
    onDeleteRecordPhotoClick: (MealRecord) -> Unit,
    onSelectMainDishClick: (DrivePlanItem?, DrivePlanItem, String) -> Unit,
) {
    val timeFormatter = remember { SimpleDateFormat("yyyy/M/d HH:mm", Locale.JAPAN) }
    val planMeal = selectedDrivePlan?.mealForRecord(record)
    val displayOptions = record.displayOptions(externalCards)
    val externalCard = record.externalCard(externalCards)
    val externalOptions = externalCard?.let { card ->
        displayOptions.filter { option -> option.optionGroupNameSnapshot == card.name }
    }.orEmpty()
    val otherOptions = if (externalCard == null) {
        displayOptions
    } else {
        displayOptions.filterNot { option -> option.optionGroupNameSnapshot == externalCard.name }
    }
    val foodOptions = otherOptions.filter { option ->
        option.optionGroupNameSnapshot == FoodOptionGroupName
    }
    val nonFoodOptions = otherOptions.filterNot { option ->
        option.optionGroupNameSnapshot == FoodOptionGroupName
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
        title = {
            Text(registeredMealLabel(record.mealType))
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = timeFormatter.format(Date(record.eatenAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.kcal_format, record.totalCalories),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${stringResource(R.string.protein_short)} ${formatOneDecimal(record.proteinG)}g / " +
                        "${stringResource(R.string.fat_short)} ${formatOneDecimal(record.fatG)}g / " +
                        "${stringResource(R.string.carb_short)} ${formatOneDecimal(record.carbG)}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val recordPhotoUri = record.photoUri?.takeIf { it.isNotBlank() }
                if (planMeal != null) {
                    DrivePlanMealContent(
                        meal = planMeal,
                        excludedItemKeys = record.excludedDrivePlanItemKeys,
                        record = record,
                        recordPhotoUri = recordPhotoUri,
                        onDeleteRecordPhotoClick = { onDeleteRecordPhotoClick(record) },
                        onDeleteItemClick = { item, itemKey ->
                            onDeleteDrivePlanItemClick(record, item, itemKey)
                        },
                        onSelectMainDishClick = onSelectMainDishClick,
                    )
                    if (externalCard != null && externalOptions.isNotEmpty()) {
                        DriveExternalCardContent(
                            card = externalCard,
                            options = externalOptions,
                            onDeleteOptionClick = { option ->
                                onDeleteRecordOptionClick(record, option)
                            },
                        )
                    }
                } else if (externalCard != null && externalOptions.isNotEmpty()) {
                    DriveExternalCardContent(
                        card = externalCard,
                        options = externalOptions,
                        onDeleteOptionClick = { option ->
                            onDeleteRecordOptionClick(record, option)
                        },
                    )
                } else if (recordPhotoUri != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.meal_detail_contents),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        MealRecordPhotoRow(
                            uriString = recordPhotoUri,
                            label = recordPhotoLabel(record.templateNameSnapshot),
                            onDeleteClick = { onDeleteRecordPhotoClick(record) },
                        )
                    }
                } else if (foodOptions.isEmpty() && nonFoodOptions.isEmpty()) {
                    Text(
                        text = stringResource(R.string.meal_detail_drive_content_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (foodOptions.isNotEmpty()) {
                    MealRecordFoodContent(
                        foods = foods,
                        options = foodOptions,
                        showTitle = planMeal == null && externalOptions.isEmpty() && recordPhotoUri == null,
                        onDeleteOptionClick = { option ->
                            onDeleteRecordOptionClick(record, option)
                        },
                    )
                }
                if (nonFoodOptions.isNotEmpty()) {
                    MealRecordOptionRows(nonFoodOptions)
                }
            }
        },
    )
}

@Composable
private fun DriveExternalCardContent(
    card: DriveExternalCard,
    options: List<MealRecordOption>,
    onDeleteOptionClick: (MealRecordOption) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.meal_detail_contents),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        options.forEachIndexed { index, option ->
            key(option.id.takeIf { it > 0L } ?: "${option.optionNameSnapshot}:$index") {
                val driveItem = card.items.firstOrNull { item -> item.name == option.optionNameSnapshot }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    driveItem?.imagePath?.let { imagePath ->
                        DriveCachedImage(
                            path = imagePath,
                            contentDescription = option.optionNameSnapshot,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(option.optionNameSnapshot, fontWeight = FontWeight.SemiBold)
                        driveItem?.amountLabel?.takeIf { it.isNotBlank() }?.let { amountLabel ->
                            Text(
                                text = amountLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (option.id > 0L) {
                        IconButton(onClick = { onDeleteOptionClick(option) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.delete_meal_item),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealRecordFoodContent(
    foods: List<DriveFood>,
    options: List<MealRecordOption>,
    showTitle: Boolean,
    onDeleteOptionClick: (MealRecordOption) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showTitle) {
            Text(
                text = stringResource(R.string.meal_detail_contents),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        options.forEachIndexed { index, option ->
            key(option.id.takeIf { it > 0L } ?: "${option.optionNameSnapshot}:$index") {
                val food = foodForRecordOption(option, foods)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    food?.imagePath?.let { imagePath ->
                        DriveCachedImage(
                            path = imagePath,
                            contentDescription = option.optionNameSnapshot,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(option.optionNameSnapshot, fontWeight = FontWeight.SemiBold)
                        food?.amountLabel?.takeIf { it.isNotBlank() }?.let { amountLabel ->
                            Text(
                                text = amountLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (option.id > 0L) {
                        IconButton(onClick = { onDeleteOptionClick(option) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.delete_meal_item),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrivePlanMealContent(
    meal: DrivePlanMeal,
    excludedItemKeys: Set<String>,
    record: MealRecord,
    recordPhotoUri: String?,
    onDeleteRecordPhotoClick: () -> Unit,
    onDeleteItemClick: (DrivePlanItem, String) -> Unit,
    onSelectMainDishClick: (DrivePlanItem?, DrivePlanItem, String) -> Unit,
) {
    val itemEntries = meal.items.mapIndexed { index, item -> item to item.recordKey(index) }
    val selectedMainDishKey = record.selectedDrivePlanMainDishItemKey
        ?: itemEntries.firstOrNull { (item, _) -> item.isMainDish }?.second
    val selectedMainDishItem = itemEntries.firstOrNull { (item, itemKey) ->
        itemKey == selectedMainDishKey && itemKey !in excludedItemKeys
    }?.first
    val canSelectMainDish = record.templateId?.let { it < 0L } == true ||
        record.selectedDrivePlanMainDishItemKey != null
    val visibleItems = itemEntries.filterNot { (_, itemKey) -> itemKey in excludedItemKeys }
    val orderedItems = orderDrivePlanMealItems(visibleItems, selectedMainDishKey)
    val hasMultipleMainDishCandidates = visibleItems.count { (item, _) ->
        item.isMainDishCandidate
    } > 1
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.meal_detail_contents),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (hasMultipleMainDishCandidates) {
                Text(
                    text = stringResource(R.string.meal_detail_multiple_main_dishes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        meal.imagePath?.let { path ->
            DriveCachedImage(
                path = path,
                contentDescription = meal.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
        }
        recordPhotoUri?.let { uriString ->
            MealRecordPhotoRow(
                uriString = uriString,
                label = recordPhotoLabel(record.templateNameSnapshot),
                onDeleteClick = onDeleteRecordPhotoClick,
            )
        }
        if (visibleItems.isEmpty()) {
            Text(
                text = stringResource(R.string.meal_detail_no_items),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        orderedItems.forEach { (item, itemKey) ->
            key(itemKey) {
                val isUnselectedMainDish = item.isMainDishCandidate && itemKey != selectedMainDishKey
                val itemColor = if (isUnselectedMainDish) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isUnselectedMainDish) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            } else {
                                Color.Transparent
                            },
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item.imagePath?.let { path ->
                        DriveCachedImage(
                            path = path,
                            contentDescription = item.name,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .alpha(if (isUnselectedMainDish) 0.55f else 1f),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, color = itemColor, fontWeight = FontWeight.SemiBold)
                        item.amountLabel.takeIf { it.isNotBlank() }?.let { amountLabel ->
                            Text(
                                text = amountLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = itemColor,
                            )
                        }
                    }
                    if (isUnselectedMainDish && canSelectMainDish) {
                        TextButton(
                            onClick = {
                                onSelectMainDishClick(selectedMainDishItem, item, itemKey)
                            },
                        ) {
                            Text(stringResource(R.string.select_main_dish))
                        }
                    } else {
                        IconButton(onClick = { onDeleteItemClick(item, itemKey) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.delete_meal_item),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealRecordPhotoRow(
    uriString: String,
    label: String?,
    onDeleteClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MealPhoto(
            uriString = uriString,
            contentDescription = stringResource(R.string.meal_detail_record_photo),
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
            maxSizePx = 160,
        )
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = label ?: stringResource(R.string.meal_detail_record_photo),
                fontWeight = FontWeight.SemiBold,
            )
        }
        IconButton(onClick = onDeleteClick) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

internal fun recordPhotoLabel(templateNameSnapshot: String): String? {
    return templateNameSnapshot
        .substringAfterLast(" / ")
        .trim()
        .takeIf { it.isNotBlank() }
}

internal fun MealRecord.externalCard(externalCards: List<DriveExternalCard>): DriveExternalCard? {
    val recordedNames = templateNameSnapshot
        .split(" / ")
        .map(String::trim)
        .filter(String::isNotBlank)
    return externalCards.firstOrNull { card -> card.name in recordedNames }
}

internal fun foodForRecordOption(
    option: MealRecordOption,
    foods: List<DriveFood>,
): DriveFood? {
    if (option.optionGroupNameSnapshot != FoodOptionGroupName) return null
    return foods.firstOrNull { food -> food.name == option.optionNameSnapshot }
}

private fun MealRecord.displayOptions(externalCards: List<DriveExternalCard>): List<MealRecordOption> {
    if (recordPhotoLabel(templateNameSnapshot) == null) return selectedOptions
    val externalCard = externalCard(externalCards)
        ?.takeIf { card -> card.items.isNotEmpty() }
        ?: return selectedOptions
    if (selectedOptions.any { option -> option.optionGroupNameSnapshot == externalCard.name }) {
        return selectedOptions
    }
    return selectedOptions + externalCard.items.map { item ->
        MealRecordOption(
            optionGroupNameSnapshot = externalCard.name,
            optionNameSnapshot = item.name,
            calorieDelta = item.calories,
            proteinDeltaG = item.proteinG,
            fatDeltaG = item.fatG,
            carbDeltaG = item.carbG,
        )
    }
}

internal fun orderDrivePlanMealItems(
    itemEntries: List<Pair<DrivePlanItem, String>>,
    selectedMainDishKey: String?,
): List<Pair<DrivePlanItem, String>> {
    val (selectedItems, remainingItems) = itemEntries.partition { (_, itemKey) ->
        itemKey == selectedMainDishKey
    }
    val (regularItems, unselectedCandidateItems) = remainingItems.partition { (item, _) ->
        !item.isMainDishCandidate
    }
    return selectedItems + regularItems + unselectedCandidateItems
}

internal fun DrivePlan.mealForRecord(record: MealRecord): DrivePlanMeal? {
    if (record.templateId == null) return null
    val slot = when (record.mealType) {
        MealType.BREAKFAST -> 0
        MealType.MORNING_SNACK -> 1
        MealType.LUNCH -> 2
        MealType.DINNER -> 3
        MealType.DAYTIME_SNACK -> 4
        MealType.FREE_SNACK,
        MealType.SNACK,
        -> 5
        MealType.EATING_OUT -> null
    }
    return slot?.let { mealSlot -> meals.firstOrNull { it.slot == mealSlot } }
        ?: meals.firstOrNull { it.name == record.templateNameSnapshot }
}

internal fun DrivePlanMeal.imagePaths(): List<String> = buildList {
    imagePath?.let(::add)
    items.mapNotNull { it.imagePath }.forEach(::add)
}
