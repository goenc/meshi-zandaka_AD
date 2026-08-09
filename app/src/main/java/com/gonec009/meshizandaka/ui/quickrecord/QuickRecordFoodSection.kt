package com.gonec009.meshizandaka.ui.quickrecord

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gonec009.meshizandaka.R
import com.gonec009.meshizandaka.ui.common.DriveCachedImage
import com.gonec009.meshizandaka.util.formatOneDecimal

internal data class QuickRecordFoodCategoryTab(
    val mealCategory: Int,
    val titleResId: Int,
)

internal val quickRecordFoodCategoryTabs = listOf(
    QuickRecordFoodCategoryTab(
        mealCategory = 2,
        titleResId = R.string.quick_record_food_category_main_dish,
    ),
    QuickRecordFoodCategoryTab(
        mealCategory = 1,
        titleResId = R.string.quick_record_food_category_side_dish,
    ),
    QuickRecordFoodCategoryTab(
        mealCategory = 0,
        titleResId = R.string.quick_record_food_category_staple,
    ),
    QuickRecordFoodCategoryTab(
        mealCategory = 3,
        titleResId = R.string.quick_record_food_category_snack,
    ),
    QuickRecordFoodCategoryTab(
        mealCategory = 4,
        titleResId = R.string.quick_record_food_category_other,
    ),
)

@Composable
internal fun QuickRecordFoodCategoryTabRow(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    TabRow(selectedTabIndex = selectedIndex) {
        quickRecordFoodCategoryTabs.forEachIndexed { index, tab ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onSelected(index) },
                text = {
                    Text(
                        text = stringResource(tab.titleResId),
                        fontSize = 12.sp,
                        maxLines = 1,
                    )
                },
            )
        }
    }
}

@Composable
internal fun QuickRecordFoodRow(
    food: QuickRecordFood,
    enabled: Boolean,
    onRegister: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            food.imagePath?.let { imagePath ->
                DriveCachedImage(
                    path = imagePath,
                    contentDescription = food.name,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = food.name,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                food.amountLabel.takeIf { it.isNotBlank() }?.let { amountLabel ->
                    Text(
                        text = amountLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(R.string.kcal_format, food.calories),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "P ${formatOneDecimal(food.proteinG)}g / " +
                        "F ${formatOneDecimal(food.fatG)}g / C ${formatOneDecimal(food.carbG)}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onRegister,
                enabled = enabled,
            ) {
                Text(stringResource(R.string.quick_record_food_register))
            }
        }
    }
}
