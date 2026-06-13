package com.example.portsofcall

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.pow
import kotlin.random.Random

// Ship types definition
enum class ShipType(val label: String, val capacity: Int, val priceNew: Long, val maxSpeed: Int, val fuelConsumption: Int) {
    COASTER("Küstenfrachter (Coaster)", 800, 450000L, 11, 12),
    FREIGHTER("Stückgutfrachter (Cargo)", 2500, 1250000L, 14, 22),
    BULK("Schüttgutfrachtschiff (Bulk)", 8000, 2800000L, 12, 35),
    CONTAINER("Containerschiff (Giant)", 22000, 6800000L, 19, 58)
}

// Active ship instance
data class ShipInstance(
    val id: String,
    val name: String,
    val type: ShipType,
    val health: Int = 100, // percentage 0-100
    val fuelCapacity: Long,
    val fuelRemaining: Long,
    val location: String,
    val status: String = "docked", // "docked", "sailing"
    val voyageDestination: String = "",
    val voyageDistanceLeft: Int = 0,
    val voyageTotalDistance: Int = 0,
    val voyageSpeed: Int = 10,
    val voyageDaysLeft: Int = 0,
    val daysElapsed: Int = 0,
    val activeCharter: CharterContract? = null
)

// Cargo charter contract format
data class CharterContract(
    val id: String,
    val cargoType: String,
    val origin: String,
    val destination: String,
    val weight: Int,
    val reward: Long,
    val penaltyPerDay: Long,
    val daysLimit: Int
)

// Accounting finance entry logs
data class FinanceLog(
    val week: Int,
    val description: String,
    val amount: Long
)

class GameViewModel : ViewModel() {
    private val _companyName = MutableStateFlow("Hanseatische Übersee AG")
    val companyName: StateFlow<String> = _companyName.asStateFlow()

    private val _homePort = MutableStateFlow("Hamburg")
    val homePort: StateFlow<String> = _homePort.asStateFlow()

    private val _money = MutableStateFlow(4800000L) // starts with adequate capital
    val money: StateFlow<Long> = _money.asStateFlow()

    private val _debt = MutableStateFlow(1000000L) // started mortgage
    val debt: StateFlow<Long> = _debt.asStateFlow()

    private val _reputation = MutableStateFlow(60) // 0-100 percentage
    val reputation: StateFlow<Int> = _reputation.asStateFlow()

    private val _currentWeek = MutableStateFlow(1)
    val currentWeek: StateFlow<Int> = _currentWeek.asStateFlow()

    private val _fuelIndex = MutableStateFlow(1.0)
    val fuelIndex: StateFlow<Double> = _fuelIndex.asStateFlow()

    private val _fleet = MutableStateFlow<List<ShipInstance>>(
        listOf(
            ShipInstance(
                id = "start-coaster",
                name = "Salty Seagull",
                type = ShipType.COASTER,
                health = 100,
                fuelCapacity = 8000L,
                fuelRemaining = 6000L,
                location = "Hamburg"
            )
        )
    )
    val fleet: StateFlow<List<ShipInstance>> = _fleet.asStateFlow()

    private val _contracts = MutableStateFlow<List<CharterContract>>(emptyList())
    val contracts: StateFlow<List<CharterContract>> = _contracts.asStateFlow()

    private val _financeLogs = MutableStateFlow<List<FinanceLog>>(
        listOf(FinanceLog(1, "Gründungskapital der Reederei registriert", 4800000L))
    )
    val financeLogs: StateFlow<List<FinanceLog>> = _financeLogs.asStateFlow()

    init {
        generateNewContracts()
    }

    fun modifyCompanyName(newName: String) {
        _companyName.value = newName
    }

    fun generateNewContracts() {
        val ports = listOf("Hamburg", "Rotterdam", "London", "New York", "Rio de Janeiro", "Cape Town", "Singapore", "Sydney")
        val cargoTypes = listOf("Kaffeebohnen", "Deutscher Edelstahl", "Premium Automobile", "Südfrüchte", "Computer-Chips", "Roherdöl")
        val list = mutableListOf<CharterContract>()
        
        repeat(12) {
            val origin = ports.random()
            var dest = ports.random()
            while (dest == origin) { dest = ports.random() }
            
            val cargo = cargoTypes.random()
            val weightTiers = listOf(400, 800, 2000, 6000, 18000)
            val weight = weightTiers.random()
            val rewardModifier = Random.nextLong(80, 180)
            
            val reward = weight * rewardModifier * 3
            val penalty = (reward * 0.12).toLong()
            val days = Random.nextInt(10, 25)

            list.add(
                CharterContract(
                    id = "con-\${Random.nextInt(10000, 99999)}",
                    cargoType = cargo,
                    origin = origin,
                    destination = dest,
                    weight = weight,
                    reward = reward,
                    penaltyPerDay = penalty,
                    daysLimit = days
                )
            )
        }
        _contracts.value = list
    }

    fun buyShip(type: ShipType) {
        if (_money.value < type.priceNew) return
        _money.value -= type.priceNew
        
        val newShip = ShipInstance(
            id = "ship-\${System.currentTimeMillis()}",
            name = "Hanseatic Pride \${_fleet.value.size + 1}",
            type = type,
            fuelCapacity = type.capacity * 10L,
            fuelRemaining = type.capacity * 8L,
            location = _homePort.value
        )
        
        _fleet.value = _fleet.value + newShip
        addLog("Kauf von \${type.label}", -type.priceNew)
        SoundPlayer.playCash()
    }

    fun refuelShip(shipId: String, percentage: Int) {
        val ship = _fleet.value.find { it.id == shipId } ?: return
        val pricePerLiter = (0.55 * _fuelIndex.value).coerceAtLeast(0.3)
        val needed = ship.fuelCapacity - ship.fuelRemaining
        val toBuy = ((ship.fuelCapacity * percentage) / 100).coerceAtMost(needed)
        if (toBuy <= 0) return

        val cost = (toBuy * pricePerLiter).toLong()
        if (_money.value < cost) return

        _money.value -= cost
        _fleet.value = _fleet.value.map {
            if (it.id == shipId) {
                it.copy(fuelRemaining = it.fuelRemaining + toBuy)
            } else it
        }
        addLog("Bunkern von \$toBuy Litern (" + ship.name + ")", -cost)
        SoundPlayer.playCash()
    }

    fun repairShip(shipId: String) {
        val ship = _fleet.value.find { it.id == shipId } ?: return
        if (ship.health >= 100) return
        val cost = (100 - ship.health) * 1500L
        if (_money.value < cost) return

        _money.value -= cost
        _fleet.value = _fleet.value.map {
            if (it.id == shipId) {
                it.copy(health = 100)
            } else it
        }
        addLog("Gesamtreparatur von \${ship.name} im Trockendock", -cost)
        SoundPlayer.playCash()
    }

    fun acceptCharter(shipId: String, contract: CharterContract) {
        val ship = _fleet.value.find { it.id == shipId } ?: return
        if (ship.status != "docked" || ship.activeCharter != null) return
        if (ship.location != contract.origin) return
        if (ship.type.capacity < contract.weight) return

        _fleet.value = _fleet.value.map {
            if (it.id == shipId) {
                it.copy(activeCharter = contract)
            } else it
        }
        _contracts.value = _contracts.value.filter { it.id != contract.id }
        SoundPlayer.playClick()
    }

    fun shipCastOff(shipId: String, speedKnots: Int) {
        val ship = _fleet.value.find { it.id == shipId } ?: return
        if (ship.status != "docked") return
        
        val dest = ship.activeCharter?.destination ?: _homePort.value
        val distance = 3500 // simulated nautical miles
        val fuelNeeded = (distance * ship.type.fuelConsumption * 0.1 * (speedKnots.toDouble() / ship.type.maxSpeed).pow(2)).toLong()
        if (ship.fuelRemaining < fuelNeeded) return

        val totalDays = (distance / (speedKnots * 24)).coerceAtLeast(1)

        _fleet.value = _fleet.value.map {
            if (it.id == shipId) {
                it.copy(
                    status = "sailing",
                    location = "Auf See",
                    voyageDestination = dest,
                    voyageDistanceLeft = distance,
                    voyageTotalDistance = distance,
                    voyageSpeed = speedKnots,
                    voyageDaysLeft = totalDays,
                    daysElapsed = 0
                )
            } else it
        }
        SoundPlayer.playHorn()
    }

    fun handleTakeLoan(amount: Long) {
        val creditLimit = _reputation.value * 45000L
        if (_debt.value + amount > creditLimit) return
        _debt.value += amount
        _money.value += amount
        SoundPlayer.playCash()
    }

    fun handleRepayLoan(amount: Long) {
        if (_money.value < amount || _debt.value < amount) return
        _debt.value -= amount
        _money.value -= amount
        SoundPlayer.playCash()
    }

    fun advanceTimeOneDay() {
        _currentWeek.value += 1
        _fuelIndex.value = 0.8 + Random.nextDouble() * 0.5
        generateNewContracts()

        _fleet.value = _fleet.value.map { ship ->
            if (ship.status == "sailing") {
                val dailyMiles = ship.voyageSpeed * 24
                val newMilesLeft = (ship.voyageDistanceLeft - dailyMiles).coerceAtLeast(0)
                val burnRate = (dailyMiles * ship.type.fuelConsumption * 0.1).toLong()
                val remFuel = (ship.fuelRemaining - burnRate).coerceAtLeast(0L)
                val elapsed = ship.daysElapsed + 1
                val daysLeft = (ship.voyageDaysLeft - 1).coerceAtLeast(0)

                if (newMilesLeft <= 0) {
                    val charter = ship.activeCharter
                    if (charter != null) {
                        val finalPayout = charter.reward
                        _money.value += finalPayout
                        _reputation.value = (_reputation.value + 4).coerceAtMost(100)
                        addLog("Lieferung abgeschlossen: \${charter.cargoType} nach \${charter.destination}", finalPayout)
                    }
                    ship.copy(
                        status = "docked",
                        location = ship.voyageDestination,
                        fuelRemaining = remFuel,
                        voyageDistanceLeft = 0,
                        activeCharter = null,
                        daysElapsed = 0
                    )
                } else {
                    ship.copy(
                        voyageDistanceLeft = newMilesLeft,
                        fuelRemaining = remFuel,
                        daysElapsed = elapsed,
                        voyageDaysLeft = daysLeft
                    )
                }
            } else ship
        }
        SoundPlayer.playRadar()
    }

    private fun addLog(description: String, amount: Long) {
        _financeLogs.value = listOf(FinanceLog(_currentWeek.value, description, amount)) + _financeLogs.value
    }
}
