package com.letotoo06.ideastorm

import android.app.Application
import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

// --- CLASSES DE DONNÉES ---
data class User(val id: Int, val username: String)

data class GameUpgrade(
    val id: String,
    val label: String,
    var cost: Double,
    val bonusClick: Double = 0.0,
    val bonusAuto: Double = 0.0,
    val multiplier: Double = 1.0,
    val unlockGif: String? = null
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    // --- ÉTAT DU JEU ---
    var score by mutableDoubleStateOf(0.0)
    var perClick by mutableDoubleStateOf(1.0)
    var perSecond by mutableDoubleStateOf(0.0)
    var rebirthCount by mutableStateOf(0)
    var runStartTime by mutableLongStateOf(System.currentTimeMillis())

    // --- SYSTEME ---
    var currentUser by mutableStateOf<User?>(null)
    private val prefs = application.getSharedPreferences("IdeaStormSave", Context.MODE_PRIVATE)
    private val gson = Gson()

    // --- COMPAGNONS ---
    var hasDebile by mutableStateOf(false)
    var hasMimir by mutableStateOf(false)
    var hasPistolet by mutableStateOf(false)
    var hasSeducteur by mutableStateOf(false)
    var hasGoose by mutableStateOf(false)

    // Variables legacy
    var hasCat1 by mutableStateOf(false); var hasCat2 by mutableStateOf(false)
    var hasVolcan by mutableStateOf(false); var hasCat3 by mutableStateOf(false)

    val upgrades = mutableStateListOf<GameUpgrade>()

    // --- PRIX DE BASE ---
    private val BASE = mapOf(
        "click" to 50.0, "auto" to 100.0,
        "cat" to 250.0, "cat2" to 2500.0, "volcan" to 25000.0, "cat3" to 200000.0, "goose" to 1000000.0,
        "super" to 500000.0, "mega" to 2500000.0, "giga" to 15000000.0, "ultimate" to 2500000.0,
        "c500k" to 40000000.0, "c1m" to 1e8, "c10m" to 1e9, "c100m" to 1e10, "c1b" to 1e11, "c10b" to 1e12, "c100b" to 1e13,
        "a500k" to 40000000.0, "a1m" to 1e8, "a10m" to 1e9, "a100m" to 1e10, "a1b" to 1e11, "a10b" to 1e12, "a100b" to 1e13,
        "multX2" to 1e8, "autoMultX2" to 1e8,
        "godA" to 1e12, "godAA" to 1e15, "sext" to 1e21, "noni" to 1e27, "duo" to 1e36,
        "vigin" to 1e63, "trigin" to 1e93, "quinqua" to 1e153, "nonagin" to 1e273,
        "googol" to 1e100, "cent" to 1e303,
        "c1e120" to 1e120, "c1e180" to 1e180, "c1e240" to 1e240, "c1e300" to 1e300
    )

    init {
        loadLocalSave()
        if (upgrades.isEmpty()) generateShop()
        startPassiveIncome()
    }

    private fun startPassiveIncome() {
        viewModelScope.launch {
            var ticks = 0
            while (true) {
                delay(1000)
                if (perSecond > 0) score += perSecond
                ticks++
                if (ticks >= 10) { saveGame(); ticks = 0 }
            }
        }
    }

    // --- SAUVEGARDE ---
    fun saveGame() {
        val currentCosts = upgrades.associate { it.id to it.cost }
        val saveData = GameSaveData(score, perClick, perSecond, rebirthCount, hasDebile, hasMimir, hasPistolet, hasSeducteur, hasGoose, currentCosts)
        prefs.edit().putString("game_save", gson.toJson(saveData)).apply()

        if (currentUser != null) {
            viewModelScope.launch(Dispatchers.IO) {
                DatabaseManager.saveGame(currentUser!!.id, score, rebirthCount, saveData)
            }
        }
    }

    private fun loadLocalSave() {
        val json = prefs.getString("game_save", null)
        if (json != null) { try { applySaveData(gson.fromJson(json, GameSaveData::class.java)) } catch (e: Exception) { e.printStackTrace() } }
    }

    private fun applySaveData(data: GameSaveData) {
        score = data.score; perClick = data.perClick; perSecond = data.perSecond; rebirthCount = data.rebirthCount
        hasDebile = data.hasDebile; hasMimir = data.hasMimir; hasPistolet = data.hasPistolet; hasSeducteur = data.hasSeducteur; hasGoose = data.hasGoose
        generateShop()
        data.upgradesCost.forEach { (id, cost) -> upgrades.find { it.id == id }?.cost = cost }
    }

    // --- GESTION DES DONNÉES (RESET) ---

    // NOUVEAU : HARD RESET (Efface tout)
    fun hardReset() {
        score = 0.0
        perClick = 1.0
        perSecond = 0.0
        rebirthCount = 0
        runStartTime = System.currentTimeMillis()

        hasDebile=false; hasMimir=false; hasPistolet=false; hasSeducteur=false; hasGoose=false
        hasCat1=false; hasCat2=false; hasVolcan=false; hasCat3=false

        generateShop()
        saveGame() // Sauvegarde l'état vide
    }

    // --- CONNEXION ---
    fun login(user: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val loggedUser = DatabaseManager.login(user, pass)
            launch(Dispatchers.Main) {
                if (loggedUser != null) { currentUser = loggedUser; loadCloudSave(loggedUser.id); onResult(true, "Connecté !") }
                else onResult(false, "Erreur identifiants")
            }
        }
    }

    fun signup(user: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val newUser = DatabaseManager.signup(user, pass)
            launch(Dispatchers.Main) {
                if (newUser != null) { currentUser = newUser; saveGame(); onResult(true, "Compte créé !") }
                else onResult(false, "Erreur création")
            }
        }
    }

    fun logout() { currentUser = null }

    private fun loadCloudSave(userId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val d = DatabaseManager.loadGame(userId)
            launch(Dispatchers.Main) { if (d != null) { applySaveData(d); saveGame() } }
        }
    }

    // --- JEU ---
    fun onClick() { score += perClick }

    fun buyUpgrade(item: GameUpgrade) {
        if (score >= item.cost) {
            score -= item.cost
            val powerMult = 50.0.pow(rebirthCount)

            if (item.bonusClick > 0) perClick += item.bonusClick
            if (item.bonusAuto > 0) perSecond += item.bonusAuto * powerMult
            if (item.multiplier > 1.0) { if (item.id.contains("auto", true)) perSecond *= item.multiplier else perClick *= item.multiplier }

            if (item.unlockGif != null) {
                when(item.unlockGif) {
                    "debile" -> hasDebile = true
                    "mimir" -> hasMimir = true
                    "pistolet" -> hasPistolet = true
                    "seducteur" -> hasSeducteur = true
                    "goose" -> hasGoose = true
                }
            } else {
                val factor = if (item.multiplier > 1.0) 3.0 else 1.5
                item.cost = floor(item.cost * factor)
            }
            saveGame()
        }
    }

    fun triggerRebirth() {
        if (score >= 1e36 && rebirthCount < 6) {
            rebirthCount++; score=0.0; perSecond=0.0; runStartTime=System.currentTimeMillis()
            hasDebile=false; hasMimir=false; hasPistolet=false; hasSeducteur=false; hasGoose=false
            val isHard = rebirthCount == 0
            perClick = 1.0 * (if (isHard) 1.0 else 50.0.pow(rebirthCount))
            generateShop()
            saveGame()
        }
    }

    private fun generateShop() {
        upgrades.clear()
        val isHard = rebirthCount == 0
        val highMult = if (isHard) 1.0 else (rebirthCount + 2.0)
        val lowMult = if (isHard) 1.0 else max(1.5, highMult / 2.0)

        // Clics
        upgrades.add(GameUpgrade("click", "Doigt Musclé (+1)", floor(BASE["click"]!! * lowMult), 1.0))
        upgrades.add(GameUpgrade("super", "Super Clic (+10k)", BASE["super"]!! * highMult, 10000.0))
        upgrades.add(GameUpgrade("mega", "Mega Clic (+100k)", BASE["mega"]!! * highMult, 100000.0))
        upgrades.add(GameUpgrade("giga", "Giga Clic (+200k)", BASE["giga"]!! * highMult, 200000.0))
        upgrades.add(GameUpgrade("ultimate", "ULTIMATE (Clic x3)", BASE["ultimate"]!! * highMult, 0.0, 0.0, 3.0))

        // Diamants
        upgrades.add(GameUpgrade("c500k", "Diamant (+500k)", BASE["c500k"]!! * highMult, 500000.0))
        upgrades.add(GameUpgrade("c1m", "Diamant Pur (+1M)", BASE["c1m"]!! * highMult, 1e6))
        upgrades.add(GameUpgrade("c10m", "Rubis (+10M)", BASE["c10m"]!! * highMult, 1e7))
        upgrades.add(GameUpgrade("c100m", "Emeraude (+100M)", BASE["c100m"]!! * highMult, 1e8))
        upgrades.add(GameUpgrade("c1b", "Planète (+1B)", BASE["c1b"]!! * highMult, 1e9))
        upgrades.add(GameUpgrade("c10b", "Système (+10B)", BASE["c10b"]!! * highMult, 1e10))
        upgrades.add(GameUpgrade("c100b", "Galaxie (+100B)", BASE["c100b"]!! * highMult, 1e11))

        // Boosters
        upgrades.add(GameUpgrade("multX2", "Boost Clic (x2)", BASE["multX2"]!! * highMult, 0.0, 0.0, 2.0))
        upgrades.add(GameUpgrade("godA", "⚡ Trillion (1T)", BASE["godA"]!! * highMult, 1e12))
        upgrades.add(GameUpgrade("godAA", "🌌 Quadrillion (1Qa)", BASE["godAA"]!! * highMult, 1e15))
        upgrades.add(GameUpgrade("sext", "✨ Sextillion (1Sx)", BASE["sext"]!! * highMult, 1e21))
        upgrades.add(GameUpgrade("noni", "💫 Octillion (1Oc)", BASE["noni"]!! * highMult, 1e27))
        upgrades.add(GameUpgrade("duo", "🌀 Undécillion (1Ud)", BASE["duo"]!! * highMult, 1e36))

        // Absurde
        upgrades.add(GameUpgrade("vigin", "⚛️ Vigintillion", BASE["vigin"]!! * highMult, 1e60))
        upgrades.add(GameUpgrade("trigin", "🪐 Trigintillion", BASE["trigin"]!! * highMult, 1e90))
        upgrades.add(GameUpgrade("googol", "🔥 GOOGOL", BASE["googol"]!! * highMult, 1e100))
        upgrades.add(GameUpgrade("quinqua", "🌀 Quinqua", BASE["quinqua"]!! * highMult, 1e150))
        upgrades.add(GameUpgrade("c1e120", "🌌 1e120 Clic", BASE["c1e120"]!! * highMult, 1e120))
        upgrades.add(GameUpgrade("c1e180", "🌌 1e180 Clic", BASE["c1e180"]!! * highMult, 1e180))
        upgrades.add(GameUpgrade("c1e240", "☄️ 1e240 Clic", BASE["c1e240"]!! * highMult, 1e240))
        upgrades.add(GameUpgrade("nonagin", "🌌 Nonagintillion", BASE["nonagin"]!! * highMult, 1e270))
        upgrades.add(GameUpgrade("cent", "💀 CENTILLION", BASE["cent"]!! * highMult, 1e300))
        upgrades.add(GameUpgrade("c1e300", "💀💀 THE END", BASE["c1e300"]!! * highMult, 1e300))

        // Auto
        upgrades.add(GameUpgrade("auto", "⚙️ Auto (+2)", floor(BASE["auto"]!! * lowMult), 0.0, 2.0))
        upgrades.add(GameUpgrade("autoMultX2", "Boost Auto (x2)", BASE["autoMultX2"]!! * highMult, 0.0, 0.0, 2.0))

        // COMPAGNONS
        upgrades.add(GameUpgrade("debile", "🤪 Chat Débile", floor(BASE["cat"]!! * lowMult), 0.0, 5.0, 1.0, "debile"))
        upgrades.add(GameUpgrade("mimir", "🥷 Le Chat Ninja", floor(BASE["cat2"]!! * lowMult), 0.0, 60.0, 1.0, "mimir"))
        upgrades.add(GameUpgrade("pistolet", "🔫 Le Chat Tueur", floor(BASE["volcan"]!! * highMult), 0.0, 700.0, 1.0, "pistolet"))
        upgrades.add(GameUpgrade("seducteur", "👑 Le Roi Chat", floor(BASE["cat3"]!! * highMult), 0.0, 6000.0, 1.0, "seducteur"))
        upgrades.add(GameUpgrade("goose", "🪿 L'Oie", floor(BASE["goose"]!! * highMult), 0.0, 35000.0, 1.0, "goose"))

        // Auto High
        upgrades.add(GameUpgrade("a500k", "Usine (+500k)", BASE["a500k"]!! * highMult, 0.0, 500000.0))
        upgrades.add(GameUpgrade("a1m", "Usine Pro (+1M)", BASE["a1m"]!! * highMult, 0.0, 1e6))
        upgrades.add(GameUpgrade("a10m", "Usine Élite (+10M)", BASE["a10m"]!! * highMult, 0.0, 1e7))
        upgrades.add(GameUpgrade("a100m", "Usine Master (+100M)", BASE["a100m"]!! * highMult, 0.0, 1e8))
        upgrades.add(GameUpgrade("a1b", "Planète Auto (+1B)", BASE["a1b"]!! * highMult, 0.0, 1e9))
        upgrades.add(GameUpgrade("a10b", "Système Auto (+10B)", BASE["a10b"]!! * highMult, 0.0, 1e10))
        upgrades.add(GameUpgrade("a100b", "Galaxie Auto (+100B)", BASE["a100b"]!! * highMult, 0.0, 1e11))
        upgrades.add(GameUpgrade("aGodA", "⚡ Trillion Auto", BASE["godA"]!! * highMult, 0.0, 1e12))
        upgrades.add(GameUpgrade("aGodAA", "🌌 Quadrillion Auto", BASE["godAA"]!! * highMult, 0.0, 1e15))
        upgrades.add(GameUpgrade("aSext", "✨ Sextillion Auto", BASE["sext"]!! * highMult, 0.0, 1e21))
        upgrades.add(GameUpgrade("aNoni", "💫 Octillion Auto", BASE["noni"]!! * highMult, 0.0, 1e27))
        upgrades.add(GameUpgrade("aDuo", "🌀 Undécillion Auto", BASE["duo"]!! * highMult, 0.0, 1e36))
        upgrades.add(GameUpgrade("aGoogol", "🔥 GOOGOL Auto", BASE["googol"]!! * highMult, 0.0, 1e100))
        upgrades.add(GameUpgrade("aCent", "💀 CENTILLION Auto", BASE["cent"]!! * highMult, 0.0, 1e300))
    }

    fun formatNumber(value: Double): String {
        if (value < 1000) return String.format("%.0f", value)
        if (value.isInfinite()) return "INFINI"
        val suffixes = listOf("k", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc", "Ud", "Dd", "Td", "Qad", "Qid", "Sxd", "Spd", "Ocd", "Nod", "Vg", "UVg", "DVg", "TVg", "QaVg", "QiVg", "SxVg", "SpVg", "OcVg", "NoVg", "Tg", "UTg", "DTg", "TTg", "NoSpg")
        val index = (log10(value).toInt() / 3) - 1
        return if (index in suffixes.indices) String.format("%.2f %s", value / 10.0.pow((index + 1) * 3), suffixes[index]) else String.format("%.2e", value)
    }

    fun getRunDuration(): String {
        val diff = System.currentTimeMillis() - runStartTime
        return "${diff / 60000}m ${(diff / 1000) % 60}s"
    }
}