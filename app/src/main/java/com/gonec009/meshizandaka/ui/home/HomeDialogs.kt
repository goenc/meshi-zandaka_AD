package com.gonec009.meshizandaka.ui.home

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gonec009.meshizandaka.R
import com.gonec009.meshizandaka.data.drive.DrivePlanItem
import com.gonec009.meshizandaka.domain.model.MealRecord
import com.gonec009.meshizandaka.domain.model.MealRecordOption
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal data class PendingDeleteRecord(
    val record: MealRecord,
)

internal data class PendingDeleteRecordPhoto(
    val record: MealRecord,
)

internal data class PendingDeleteDrivePlanItem(
    val record: MealRecord,
    val item: DrivePlanItem,
    val itemKey: String,
)

internal data class PendingDeleteRecordOption(
    val record: MealRecord,
    val option: MealRecordOption,
)

@Composable
internal fun HomeDatePickerDialog(
    selectedDate: LocalDate,
    onDismissRequest: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val zoneId = ZoneId.systemDefault()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate())
                    }
                    onDismissRequest()
                },
            ) {
                Text(stringResource(R.string.confirm_date))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
internal fun HomeDeleteDialogs(
    pendingDeleteRecord: PendingDeleteRecord?,
    pendingDeleteRecordPhoto: PendingDeleteRecordPhoto?,
    pendingDeleteDrivePlanItem: PendingDeleteDrivePlanItem?,
    pendingDeleteRecordOption: PendingDeleteRecordOption?,
    onDismissRecord: () -> Unit,
    onDismissRecordPhoto: () -> Unit,
    onDismissDrivePlanItem: () -> Unit,
    onDismissRecordOption: () -> Unit,
    onDeleteRecord: (Long, () -> Unit) -> Unit,
    onDeleteRecordPhoto: (Long, () -> Unit) -> Unit,
    onDeleteDrivePlanItem: (Long, String, DrivePlanItem, () -> Unit) -> Unit,
    onDeleteRecordOption: (Long, MealRecordOption, () -> Unit) -> Unit,
) {
    pendingDeleteRecord?.let { pending ->
        DeleteConfirmationDialog(
            message = stringResource(R.string.delete_record_confirm_message),
            onDismissRequest = onDismissRecord,
            onConfirm = { onDeleteRecord(pending.record.id, onDismissRecord) },
        )
    }
    pendingDeleteRecordPhoto?.let { pending ->
        DeleteConfirmationDialog(
            message = stringResource(R.string.delete_photo_confirm_message),
            onDismissRequest = onDismissRecordPhoto,
            onConfirm = { onDeleteRecordPhoto(pending.record.id, onDismissRecordPhoto) },
        )
    }
    pendingDeleteDrivePlanItem?.let { pending ->
        DeleteConfirmationDialog(
            message = stringResource(
                R.string.delete_meal_item_confirm_message,
                pending.item.name,
            ),
            onDismissRequest = onDismissDrivePlanItem,
            onConfirm = {
                onDeleteDrivePlanItem(
                    pending.record.id,
                    pending.itemKey,
                    pending.item,
                    onDismissDrivePlanItem,
                )
            },
        )
    }
    pendingDeleteRecordOption?.let { pending ->
        DeleteConfirmationDialog(
            message = stringResource(
                R.string.delete_meal_item_confirm_message,
                pending.option.optionNameSnapshot,
            ),
            onDismissRequest = onDismissRecordOption,
            onConfirm = {
                onDeleteRecordOption(
                    pending.record.id,
                    pending.option,
                    onDismissRecordOption,
                )
            },
        )
    }
}

@Composable
private fun DeleteConfirmationDialog(
    message: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.delete)) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.no))
            }
        },
    )
}
