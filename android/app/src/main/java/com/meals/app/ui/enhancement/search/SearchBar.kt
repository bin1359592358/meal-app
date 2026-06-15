package com.meals.app.ui.enhancement.search

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val OrangePrimary = Color(0xFFFF6B35)
private val GrayDescription = Color(0xFF9E9E9E)

@Composable
fun MealSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var localQuery by remember(query) { mutableStateOf(query) }

    // Debounce: wait 300ms after user stops typing before triggering search
    LaunchedEffect(localQuery) {
        if (localQuery != query) {
            delay(300)
            onQueryChange(localQuery)
        }
    }

    OutlinedTextField(
        value = localQuery,
        onValueChange = { localQuery = it },
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        placeholder = {
            Text(
                text = "搜索菜品...",
                fontSize = 14.sp,
                color = GrayDescription
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                tint = GrayDescription
            )
        },
        trailingIcon = {
            if (localQuery.isNotEmpty()) {
                IconButton(onClick = {
                    localQuery = ""
                    onQueryChange("")
                }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "清除",
                        tint = GrayDescription
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = OrangePrimary,
            unfocusedBorderColor = Color(0xFFE0E0E0),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}
