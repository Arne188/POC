package com.example.portsofcall

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortsOfCallApp(viewModel: GameViewModel) {
    val companyName by viewModel.companyName.collectAsState()
    val money by viewModel.money.collectAsState()
    val debt by viewModel.debt.collectAsState()
    val reputation by viewModel.reputation.collectAsState()
    val currentWeek by viewModel.currentWeek.collectAsState()
    val fleet by viewModel.fleet.collectAsState()
    val contracts by viewModel.contracts.collectAsState()
    val financeLogs by viewModel.financeLogs.collectAsState()

    var activeTab by remember { mutableStateOf("fleet") }
    var selectedShipId by remember { mutableStateOf(fleet.firstOrNull()?.id ?: "") }
    val currentShip = fleet.find { it.id == selectedShipId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = companyName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Kapital: $\${String.format("%,d", money)}  |  Schulden: $\${String.format("%,d", debt)}  |  Ruf: \$reputation%",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFA0AEC0)
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.advanceTimeOneDay() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD69E2E))
                    ) {
                        Text("⏳ Tag +1", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A202C))
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF171923)) {
                NavigationBarItem(
                    selected = activeTab == "fleet",
                    onClick = { activeTab = "fleet" },
                    icon = { Text("⚓") },
                    label = { Text("Flotte", color = Color.White, fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = activeTab == "charters",
                    onClick = { activeTab = "charters" },
                    icon = { Text("📦") },
                    label = { Text("Fracht", color = Color.White, fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = activeTab == "broker",
                    onClick = { activeTab = "broker" },
                    icon = { Text("🚢") },
                    label = { Text("Broker", color = Color.White, fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = activeTab == "office",
                    onClick = { activeTab = "office" },
                    icon = { Text("🏦") },
                    label = { Text("Kontor", color = Color.White, fontSize = 11.sp) }
                )
            }
        },
        containerColor = Color(0xFF0F1219)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            when (activeTab) {
                "fleet" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("DOCK-OFFICE", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF3182CE))
                        
                        if (currentShip != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2530)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text(currentShip.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                        Text(currentShip.type.label, fontSize = 11.sp, color = Color(0xFF90CDF4), fontFamily = FontFamily.Monospace)
                                    }
                                    
                                    Divider(color = Color(0xFF2D3748))
                                    
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Rumpfstärke: \${currentShip.health}%", color = Color.White, fontSize = 12.sp)
                                        Text("Treibstoff: \${currentShip.fuelRemaining} / \${currentShip.fuelCapacity} L", color = Color.White, fontSize = 12.sp)
                                    }

                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Standort: \${currentShip.location}", color = Color.White, fontSize = 12.sp)
                                        Text("Status: \${currentShip.status.uppercase()}", color = if (currentShip.status == "sailing") Color.Yellow else Color.Green, fontSize = 12.sp)
                                    }

                                    if (currentShip.activeCharter != null) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF0F1E36), RoundedCornerShape(8.dp))
                                                .border(1.dp, Color(0xFF254B7D), RoundedCornerShape(8.dp))
                                                .padding(12.dp)
                                                .fillMaxWidth()
                                        ) {
                                            Column {
                                                Text("⚓ Aktive Charter", color = Color(0xFF63B3ED), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                Text("Ladung: \${currentShip.activeCharter.cargoType} nach \${currentShip.activeCharter.destination}", color = Color.White, fontSize = 12.sp)
                                                Text("Frachtlohn: $\${String.format("%,d", currentShip.activeCharter.reward)}", color = Color.Green, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                    }
                                }
                            }

                            if (currentShip.status == "docked") {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = { viewModel.refuelShip(currentShip.id, 50) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B6CB0)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("⛽ Bunkern +50%", fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = { viewModel.repairShip(currentShip.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3748)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("🔧 Rumpf reparieren", fontSize = 11.sp)
                                    }
                                }

                                Button(
                                    onClick = { viewModel.shipCastOff(currentShip.id, currentShip.type.maxSpeed - 2) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38A169)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("⚓ Ablegen & Auslaufen", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF2D3748), RoundedCornerShape(8.dp))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🚢 AUF ZEISEWEG NACH \${currentShip.voyageDestination}", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Noch \${currentShip.voyageDaysLeft} Tage verbleibend", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                "charters" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("MORTGAGE & FREIGHT CHANNELS", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF3182CE))
                        
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(contracts.filter { it.origin == (currentShip?.location ?: "Hamburg") }) { contract ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2530)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text(contract.cargoType, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                            Text("$\${String.format("%,d", contract.reward)}", color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                        Text("Route: \${contract.origin} ➔ \${contract.destination}", color = Color.LightGray, fontSize = 12.sp)
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("Gewicht: \${contract.weight} t", color = Color.Gray, fontSize = 11.sp)
                                            Text("Frist: \${contract.daysLimit} Tage", color = Color.Gray, fontSize = 11.sp)
                                        }
                                        
                                        if (currentShip != null && currentShip.status == "docked" && currentShip.activeCharter == null) {
                                            Button(
                                                onClick = { viewModel.acceptCharter(currentShip.id, contract) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3182CE)),
                                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                            ) {
                                                Text("Charter annehmen", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "broker" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("HANSA SHIP-BROKER DECK", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF3182CE))
                        
                        ShipType.values().forEach { shipType ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2530)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(shipType.label, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                        Text("Max: \${shipType.capacity} t | Speed: \${shipType.maxSpeed} kn", color = Color.Gray, fontSize = 11.sp)
                                        Text("Preis: $\${String.format("%,d", shipType.priceNew)}", color = Color.Yellow, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    Button(
                                        onClick = { viewModel.buyShip(shipType) },
                                        enabled = money >= shipType.priceNew,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38A169))
                                    ) {
                                        Text("Kaufen", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                "office" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("HAMBURGER REEDEREI KONTOR", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF3182CE))
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2530)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Maritime Credit Bank Hamburg", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text("Aufgenommener Kredit: $\${String.format("%,d", debt)}", color = Color.White, fontSize = 12.sp)
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = { viewModel.handleTakeLoan(100000L) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B6CB0)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("+ $100K leihen", fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = { viewModel.handleRepayLoan(100000L) },
                                        enabled = debt >= 100000L && money >= 100000L,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53E3E)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("- $100K tilgen", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        Text("BUCHHALTUNGS-AUSTRIEG", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(financeLogs) { log ->
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("W\${log.week}: \${log.description}", color = Color.LightGray, fontSize = 11.sp)
                                    Text(
                                        text = "\${if (log.amount >= 0) "+" else ""}$\${String.format("%,d", log.amount)}",
                                        color = if (log.amount >= 0) Color.Green else Color.Red,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
