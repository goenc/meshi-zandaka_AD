package com.gonec009.meshizandaka.ui.template

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gonec009.meshizandaka.R
import com.gonec009.meshizandaka.data.AppContainer
import com.gonec009.meshizandaka.data.drive.DrivePlan
import com.gonec009.meshizandaka.data.drive.DrivePlanItem
import com.gonec009.meshizandaka.data.drive.DrivePlanMeal
import com.gonec009.meshizandaka.data.drive.DrivePlanPhase
import com.gonec009.meshizandaka.data.drive.DrivePlanState
import com.gonec009.meshizandaka.util.formatOneDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TemplateManagementRoute(
    container: AppContainer,
    innerPadding: PaddingValues,
    onDriveConnect: () -> Unit,
) {
    val state by container.driveAccessManager.planState.collectAsState()
    val scope = rememberCoroutineScope()
    TemplateManagementScreen(
        innerPadding = innerPadding,
        state = state,
        onDriveConnect = onDriveConnect,
        onSelectPlan = { planId ->
            scope.launch { container.driveAccessManager.selectPlan(planId) }
        },
    )
}

@Composable
private fun TemplateManagementScreen(
    innerPadding: PaddingValues,
    state: DrivePlanState,
    onDriveConnect: () -> Unit,
    onSelectPlan: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.template_management_drive_description),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        when (state.phase) {
            DrivePlanPhase.IDLE -> {
                item {
                    DriveConnectionPrompt(onDriveConnect = onDriveConnect)
                }
            }
            DrivePlanPhase.LOADING -> {
                item {
                    DriveLoadingState()
                }
            }
            DrivePlanPhase.FAILED -> {
                item {
                    DriveErrorState(
                        message = state.errorMessage,
                        onDriveConnect = onDriveConnect,
                    )
                }
            }
            DrivePlanPhase.READY -> {
                if (state.plans.isEmpty()) {
                    item {
                        Text(stringResource(R.string.template_management_no_plans))
                        Button(onClick = onDriveConnect, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.template_management_reload))
                        }
                    }
                } else {
                    item {
                        DrivePlanSelector(
                            plans = state.plans,
                            selectedPlanId = state.selectedPlanId,
                            onSelectPlan = onSelectPlan,
                        )
                    }
                    state.errorMessage?.let { message ->
                        item {
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    state.selectedPlan?.let { plan ->
                        item {
                            DrivePlanHeader(plan = plan)
                        }
                        items(plan.meals, key = { meal -> meal.slot }) { meal ->
                            DrivePlanMealCard(
                                meal = meal,
                                imageLoading = state.imageLoading,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DriveConnectionPrompt(onDriveConnect: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.template_management_drive_connect_required))
            Button(onClick = onDriveConnect, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.google_drive_connect))
            }
        }
    }
}

@Composable
private fun DriveLoadingState() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        Text(stringResource(R.string.template_management_drive_loading))
    }
}

@Composable
private fun DriveErrorState(
    message: String?,
    onDriveConnect: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = message ?: stringResource(R.string.template_management_drive_error),
                color = MaterialTheme.colorScheme.error,
            )
            Button(onClick = onDriveConnect, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.template_management_reload))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrivePlanSelector(
    plans: List<DrivePlan>,
    selectedPlanId: String?,
    onSelectPlan: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedPlan = plans.firstOrNull { it.id == selectedPlanId }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedPlan?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.template_management_plan_selector)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            plans.forEach { plan ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(plan.name)
                            if (plan.targetDate != null) {
                                Text(
                                    text = plan.targetDate,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelectPlan(plan.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun DrivePlanHeader(plan: DrivePlan) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(plan.name, style = MaterialTheme.typography.titleMedium)
                if (plan.isFavorite) {
                    Text(
                        text = stringResource(R.string.template_management_favorite),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            plan.targetDate?.takeIf { it.isNotBlank() }?.let { date ->
                Text(stringResource(R.string.template_management_plan_date, date))
            }
            plan.memo.takeIf { it.isNotBlank() }?.let { memo ->
                Text(stringResource(R.string.template_management_plan_memo, memo))
            }
            Text(
                text = stringResource(R.string.template_management_read_only_notice),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun DrivePlanMealCard(
    meal: DrivePlanMeal,
    imageLoading: Boolean,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(meal.label, style = MaterialTheme.typography.titleMedium)
            if (meal.nutritionDataAvailable) {
                Text(
                    text = stringResource(
                        R.string.template_management_meal_nutrition,
                        meal.totalCalories,
                        formatOneDecimal(meal.proteinG),
                        formatOneDecimal(meal.fatG),
                        formatOneDecimal(meal.carbG),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(meal.name, fontWeight = FontWeight.Bold)
            meal.imagePath?.let { path ->
                DriveCachedImage(
                    path = path,
                    contentDescription = meal.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
            }
            if (imageLoading && meal.imageContentHash != null && meal.imagePath == null) {
                Text(stringResource(R.string.template_management_image_loading))
            }
            meal.memo.takeIf { it.isNotBlank() }?.let { memo ->
                Text(stringResource(R.string.template_management_meal_memo, memo))
            }
            if (meal.items.isEmpty()) {
                Text(stringResource(R.string.template_management_no_items))
            } else {
                meal.items.forEach { item ->
                    DrivePlanItemRow(item = item)
                }
            }
        }
    }
}

@Composable
private fun DrivePlanItemRow(item: DrivePlanItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item.imagePath?.let { path ->
            DriveCachedImage(
                path = path,
                contentDescription = item.name,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp)),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name)
            Text(item.amountLabel, style = MaterialTheme.typography.bodySmall)
        }
        if (item.isMainDish) {
            Text(
                text = stringResource(R.string.template_management_main_dish),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun DriveCachedImage(
    path: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val bitmap by androidx.compose.runtime.produceState<ImageBitmap?>(
        initialValue = null,
        key1 = path,
    ) {
        value = withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(path)?.asImageBitmap()
        }
    }
    bitmap?.let { image ->
        Image(
            bitmap = image,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    }
}
