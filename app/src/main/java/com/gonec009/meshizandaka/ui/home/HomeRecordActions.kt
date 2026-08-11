package com.gonec009.meshizandaka.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gonec009.meshizandaka.R
import java.time.LocalDate

@Composable
internal fun HomeRecordActions(
    selectedDate: LocalDate,
    onPreviousDateClick: () -> Unit,
    onNextDateClick: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onBreakfastClick: () -> Unit,
    onMorningSnackClick: () -> Unit,
    onLunchClick: () -> Unit,
    onDinnerClick: () -> Unit,
    onDaytimeSnackClick: () -> Unit,
    onFreeSnackClick: () -> Unit,
    onQuickRecordClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RecordDateSelector(
            selectedDate = selectedDate,
            onPreviousDateClick = onPreviousDateClick,
            onNextDateClick = onNextDateClick,
            onOpenDatePicker = onOpenDatePicker,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onBreakfastClick, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.breakfast_set))
            }
            Button(onClick = onLunchClick, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.lunch_set))
            }
            Button(onClick = onDinnerClick, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.dinner_set))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onMorningSnackClick, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.morning_snack_set))
            }
            Button(onClick = onDaytimeSnackClick, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.daytime_snack_set))
            }
            Button(onClick = onFreeSnackClick, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.free_snack_set))
            }
        }
        Button(onClick = onQuickRecordClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.go_to_record))
        }
    }
}
