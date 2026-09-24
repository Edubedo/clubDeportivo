package com.example.clubdeportivo.ui.areas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.clubdeportivo.data.model.Area
import com.example.clubdeportivo.ui.inventario.InventarioScreen
import com.example.clubdeportivo.ui.torneos.TorneosScreen

@Composable
fun AreasDetailScreen(
    onAreaClick: (Area) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Campos", "Inventario", "Torneos")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = selectedTab == index,
                    onClick = { selectedTab = index }
                )
            }
        }

        when (selectedTab) {
            0 -> AreasScreen(onAreaClick = onAreaClick)
            1 -> InventarioScreen()
            2 -> TorneosScreen()
        }
    }
}
