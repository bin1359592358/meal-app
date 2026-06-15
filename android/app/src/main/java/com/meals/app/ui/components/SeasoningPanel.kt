package com.meals.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meals.app.data.dto.DishDto

private val OrangePrimary = Color(0xFFFF6B35)
private val GrayDescription = Color(0xFF9E9E9E)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SeasoningPanel(
    dish: DishDto,
    onConfirm: (Map<String, Any>) -> Unit,
    onDismiss: () -> Unit
) {
    val selections = remember { mutableStateMapOf<String, Any>() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "选择口味 - ${dish.name}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121)
            )

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭",
                tint = GrayDescription,
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(4.dp)
            )
        }

        Divider(modifier = Modifier.padding(vertical = 12.dp))

        // Seasoning options scrollable area
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            dish.seasonings.forEach { seasoning ->
                val name = seasoning["name"] as? String ?: ""
                val type = seasoning["type"] as? String ?: ""

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Section title
                    Text(
                        text = name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF424242)
                    )

                    when (type) {
                        "single" -> {
                            val options = (seasoning["options"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                            val selectedOption = selections[name] as? String ?: options.firstOrNull() ?: ""

                            if (selectedOption.isNotBlank() && selections[name] == null) {
                                selections[name] = selectedOption
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                options.forEach { option ->
                                    Row(
                                        modifier = Modifier
                                            .selectable(
                                                selected = selectedOption == option,
                                                onClick = { selections[name] = option }
                                            )
                                            .background(
                                                color = if (selectedOption == option) OrangePrimary.copy(alpha = 0.1f)
                                                else Color(0xFFF5F5F5),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        RadioButton(
                                            selected = selectedOption == option,
                                            onClick = { selections[name] = option },
                                            colors = RadioButtonDefaults.colors(selectedColor = OrangePrimary),
                                            modifier = Modifier.height(20.dp)
                                        )
                                        Text(
                                            text = option,
                                            fontSize = 14.sp,
                                            color = if (selectedOption == option) OrangePrimary else Color(0xFF424242)
                                        )
                                    }
                                }
                            }
                        }

                        "multi" -> {
                            val options = (seasoning["options"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

                            @Suppress("UNCHECKED_CAST")
                            val selectedOptions = (selections[name] as? List<*>)?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf()

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                options.forEach { option ->
                                    val isChecked = option in selectedOptions
                                    Row(
                                        modifier = Modifier
                                            .toggleable(
                                                value = isChecked,
                                                onValueChange = { checked ->
                                                    @Suppress("UNCHECKED_CAST")
                                                    val current = (selections[name] as? List<*>)?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf()
                                                    if (checked) current.add(option) else current.remove(option)
                                                    selections[name] = current
                                                }
                                            )
                                            .background(
                                                color = if (isChecked) OrangePrimary.copy(alpha = 0.1f)
                                                else Color(0xFFF5F5F5),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                @Suppress("UNCHECKED_CAST")
                                                val current = (selections[name] as? List<*>)?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf()
                                                if (checked) current.add(option) else current.remove(option)
                                                selections[name] = current
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = OrangePrimary),
                                            modifier = Modifier.height(20.dp)
                                        )
                                        Text(
                                            text = option,
                                            fontSize = 14.sp,
                                            color = if (isChecked) OrangePrimary else Color(0xFF424242)
                                        )
                                    }
                                }
                            }
                        }

                        "scale" -> {
                            val min = (seasoning["min"] as? Number)?.toFloat() ?: 0f
                            val max = (seasoning["max"] as? Number)?.toFloat() ?: 10f
                            val currentValue = selections[name] as? Float ?: min

                            if (selections[name] == null) {
                                selections[name] = min
                            }

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${min.toInt()}",
                                        fontSize = 12.sp,
                                        color = GrayDescription
                                    )
                                    Text(
                                        text = "${currentValue.toInt()}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OrangePrimary
                                    )
                                    Text(
                                        text = "${max.toInt()}",
                                        fontSize = 12.sp,
                                        color = GrayDescription
                                    )
                                }

                                Slider(
                                    value = currentValue,
                                    onValueChange = { selections[name] = it },
                                    valueRange = min..max,
                                    steps = (max - min).toInt() - 1,
                                    colors = SliderDefaults.colors(
                                        thumbColor = OrangePrimary,
                                        activeTrackColor = OrangePrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        "text" -> {
                            val currentText = selections[name] as? String ?: ""
                            val placeholder = seasoning["placeholder"] as? String ?: "请输入..."

                            OutlinedTextField(
                                value = currentText,
                                onValueChange = { selections[name] = it },
                                placeholder = {
                                    Text(text = placeholder, fontSize = 14.sp)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangePrimary,
                                    focusedLabelColor = OrangePrimary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Confirm button
        Button(
            onClick = { onConfirm(selections.toMap()) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "确认选择",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
