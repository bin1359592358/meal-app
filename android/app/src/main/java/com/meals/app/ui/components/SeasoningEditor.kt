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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val OrangePrimary = Color(0xFFFF6B35)
private val PriceRed = Color(0xFFE53935)
private val GrayDescription = Color(0xFF9E9E9E)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SeasoningEditor(
    seasonings: List<Map<String, Any>>,
    onSeasoningsChanged: (List<Map<String, Any>>) -> Unit
) {
    var showPreview by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "口味配置",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showPreview = !showPreview },
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangePrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Preview,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "预览", fontSize = 12.sp)
                }
            }
        }

        // Seasoning items list
        seasonings.forEachIndexed { index, seasoning ->
            SeasoningItemCard(
                seasoning = seasoning,
                index = index,
                totalCount = seasonings.size,
                onUpdated = { updated ->
                    val newList = seasonings.toMutableList()
                    newList[index] = updated
                    onSeasoningsChanged(newList)
                },
                onRemoved = {
                    val newList = seasonings.toMutableList()
                    newList.removeAt(index)
                    onSeasoningsChanged(newList)
                },
                onMoveUp = {
                    if (index > 0) {
                        val newList = seasonings.toMutableList()
                        val item = newList.removeAt(index)
                        newList.add(index - 1, item)
                        onSeasoningsChanged(newList)
                    }
                },
                onMoveDown = {
                    if (index < seasonings.size - 1) {
                        val newList = seasonings.toMutableList()
                        val item = newList.removeAt(index)
                        newList.add(index + 1, item)
                        onSeasoningsChanged(newList)
                    }
                }
            )
        }

        // Add button
        Button(
            onClick = {
                val newSeasoning = mapOf<String, Any>(
                    "name" to "",
                    "type" to "single",
                    "options" to listOf("选项1", "选项2")
                )
                onSeasoningsChanged(seasonings + newSeasoning)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary.copy(alpha = 0.1f), contentColor = OrangePrimary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "添加口味选项", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeasoningItemCard(
    seasoning: Map<String, Any>,
    index: Int,
    totalCount: Int,
    onUpdated: (Map<String, Any>) -> Unit,
    onRemoved: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var name by remember(seasoning) { mutableStateOf(seasoning["name"] as? String ?: "") }
    var type by remember(seasoning) { mutableStateOf(seasoning["type"] as? String ?: "single") }
    var options by remember(seasoning) {
        mutableStateOf(
            (seasoning["options"] as? List<*>)?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf("选项1", "选项2")
        )
    }
    var minValue by remember(seasoning) {
        mutableIntStateOf((seasoning["min"] as? Number)?.toInt() ?: 0)
    }
    var maxValue by remember(seasoning) {
        mutableIntStateOf((seasoning["max"] as? Number)?.toInt() ?: 10)
    }
    var placeholder by remember(seasoning) { mutableStateOf(seasoning["placeholder"] as? String ?: "") }
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top row: reorder buttons + delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = index > 0,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "上移",
                            tint = if (index > 0) GrayDescription else GrayDescription.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalCount - 1,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "下移",
                            tint = if (index < totalCount - 1) GrayDescription else GrayDescription.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onRemoved,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = PriceRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Name + Type row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onUpdated(buildSeasoningMap(name = it, type = type, options = options, min = minValue, max = maxValue, placeholder = placeholder))
                    },
                    label = { Text("名称", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        focusedLabelColor = OrangePrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                // Type dropdown
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.height(56.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = when (type) {
                                "single" -> "单选"
                                "multi" -> "多选"
                                "scale" -> "刻度"
                                "text" -> "文本"
                                else -> type
                            },
                            fontSize = 14.sp
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("single" to "单选", "multi" to "多选", "scale" to "刻度", "text" to "文本").forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    type = value
                                    expanded = false
                                    val defaultOptions = when (value) {
                                        "single", "multi" -> listOf("选项1", "选项2")
                                        else -> emptyList()
                                    }
                                    if (value in listOf("single", "multi")) options = defaultOptions.toMutableList()
                                    onUpdated(buildSeasoningMap(name = name, type = value, options = options, min = minValue, max = maxValue, placeholder = placeholder))
                                }
                            )
                        }
                    }
                }
            }

            Divider(color = Color(0xFFEEEEEE))

            // Type-specific configuration
            when (type) {
                "single", "multi" -> {
                    Text(
                        text = "选项列表",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF616161)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        options.forEachIndexed { optIndex, option ->
                            Box(
                                modifier = Modifier
                                    .background(OrangePrimary.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = option,
                                        fontSize = 13.sp,
                                        color = OrangePrimary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "移除",
                                        tint = OrangePrimary.copy(alpha = 0.7f),
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable {
                                                options = options.toMutableList().also { it.removeAt(optIndex) }
                                                onUpdated(buildSeasoningMap(name = name, type = type, options = options, min = minValue, max = maxValue, placeholder = placeholder))
                                            }
                                    )
                                }
                            }
                        }

                        // Add option chip
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFEEEEEE))
                                .clickable {
                                    options = options.toMutableList().also { it.add("选项${it.size + 1}") }
                                    onUpdated(buildSeasoningMap(name = name, type = type, options = options, min = minValue, max = maxValue, placeholder = placeholder))
                                }
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "添加选项",
                                tint = GrayDescription,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                "scale" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = minValue.toString(),
                            onValueChange = {
                                minValue = it.toIntOrNull() ?: 0
                                onUpdated(buildSeasoningMap(name = name, type = type, options = options, min = minValue, max = maxValue, placeholder = placeholder))
                            },
                            label = { Text("最小值", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangePrimary,
                                focusedLabelColor = OrangePrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = maxValue.toString(),
                            onValueChange = {
                                maxValue = it.toIntOrNull() ?: 10
                                onUpdated(buildSeasoningMap(name = name, type = type, options = options, min = minValue, max = maxValue, placeholder = placeholder))
                            },
                            label = { Text("最大值", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangePrimary,
                                focusedLabelColor = OrangePrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }
                }

                "text" -> {
                    OutlinedTextField(
                        value = placeholder,
                        onValueChange = {
                            placeholder = it
                            onUpdated(buildSeasoningMap(name = name, type = type, options = options, min = minValue, max = maxValue, placeholder = it))
                        },
                        label = { Text("占位提示文字", fontSize = 12.sp) },
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

private fun buildSeasoningMap(
    name: String,
    type: String,
    options: List<String>,
    min: Int,
    max: Int,
    placeholder: String
): Map<String, Any> {
    val map = mutableMapOf<String, Any>(
        "name" to name,
        "type" to type
    )
    when (type) {
        "single", "multi" -> map["options"] = options
        "scale" -> {
            map["min"] = min
            map["max"] = max
        }
        "text" -> map["placeholder"] = placeholder
    }
    return map
}
