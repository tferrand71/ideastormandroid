package com.letotoo06.ideastorm

import android.app.Application
import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

data class User(val id: Int, val username: String)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    // --- ÉTAT DU JEU ---
    var username by mutableStateOf("")
    var userId by mutableIntStateOf(0)
    var score by mutableDoubleStateOf(0.0)
    var perClick by mutableDoubleStateOf(1.0)
    var perSecond by mutableDoubleStateOf(0.0)
    var rebirthCount by mutableIntStateOf(0)

    var isAuthenticated by mutableStateOf(false)
    var isGuest by mutableStateOf(false)

    // --- BOUTIQUE ET FAMILIERS ---
    var costs = mutableStateMapOf<String, Double>()
    var catBought by mutableStateOf(false)
    var cat2Bought by mutableStateOf(false)
    var volcanBought by mutableStateOf(false)
    var cat3Bought by mutableStateOf(false)
    var gooseBought by mutableStateOf(false)

    private val prefs = application.getSharedPreferences("IdeaStormApp", Context.MODE_PRIVATE)

    val BASE_COSTS = mapOf(
        "clickUpgradeCost" to 50.0, "autoUpgradeCost" to 100.0,
        "superClickCost" to 500000.0, "megaClickCost" to 2500000.0, "gigaClickCost" to 15000000.0,
        "click500kCost" to 40000000.0, "click1mCost" to 1e8, "click10mCost" to 1e9, "click100mCost" to 1e10, "click1bCost" to 1e11, "click10bCost" to 1e12, "click100bCost" to 1e13,
        "godClickACost" to 1e12, "godClickAACost" to 1e15, "clickSextillionCost" to 1e21, "clickNonillionCost" to 1e27, "clickDuodecillionCost" to 1e36,
        "clickVigintillionCost" to 1e63, "clickTrigintillionCost" to 1e93, "clickQuinquagintillionCost" to 1e153, "clickNonagintillionCost" to 1e273,
        "clickGoogolCost" to 1e100, "clickCentillionCost" to 1e303,
        "click1e120Cost" to 1e120, "click1e180Cost" to 1e180, "click1e240Cost" to 1e240, "click1e300Cost" to 1e300,

        "auto500kCost" to 40000000.0, "auto1mCost" to 1e8, "auto10mCost" to 1e9, "auto100mCost" to 1e10, "auto1bCost" to 1e11, "auto10bCost" to 1e12, "auto100bCost" to 1e13,
        "godAutoACost" to 1e12, "godAutoAACost" to 1e15, "autoSextillionCost" to 1e21, "autoNonillionCost" to 1e27, "autoDuodecillionCost" to 1e36,
        "autoGoogolCost" to 1e100, "auto1e120Cost" to 1e120, "auto1e180Cost" to 1e180, "auto1e240Cost" to 1e240, "auto1e300Cost" to 1e300, "autoCentillionCost" to 1e303,

        "multX2Cost" to 1e8, "autoMultX2Cost" to 1e8, "ultimateClickCost" to 2500000.0,
        "catUpgradeCost" to 250.0, "cat2UpgradeCost" to 2500.0, "volcanCost" to 25000.0, "cat3UpgradeCost" to 200000.0, "gooseCost" to 1000000.0
    )

    init {
        BASE_COSTS.forEach { costs[it.key] = it.value }
        val savedUserId = prefs.getInt("app_userId", 0)
        val savedUsername = prefs.getString("app_username", "")
        if (savedUserId != 0 && !savedUsername.isNullOrEmpty()) {
            username = savedUsername
            userId = savedUserId
            isAuthenticated = true
            loadGameFromServer()
        }
        startLoops()
    }

    fun highMult(): Double = if (rebirthCount == 0) 1.0 else (rebirthCount + 2.0)
    fun lowMult(): Double = if (rebirthCount == 0) 1.0 else max(1.5, (rebirthCount + 2.0) / 2.0)
    fun powerMult(): Double = if (rebirthCount == 0) 1.0 else 50.0.pow(rebirthCount)

    // --- CONNEXION ---
    fun login(user: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            // 👑 .trim() détruit les espaces invisibles ajoutés par le clavier !
            val loggedUser = ApiManager.login(user.trim(), pass.trim())
            if (loggedUser != null) {
                username = loggedUser.username; userId = loggedUser.id; isAuthenticated = true
                prefs.edit().putString("app_username", username).putInt("app_userId", userId).apply()
                loadGameFromServer()
                onResult(true, "Connecté !")
            } else onResult(false, "Erreur identifiants")
        }
    }

    fun signup(user: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            // 👑 Pareil ici
            val newUser = ApiManager.signup(user.trim(), pass.trim())
            if (newUser != null) {
                username = newUser.username; userId = newUser.id; isAuthenticated = true
                prefs.edit().putString("app_username", username).putInt("app_userId", userId).apply()
                saveGameToServer()
                onResult(true, "Compte créé !")
            } else onResult(false, "Pseudo déjà pris")
        }
    }

    fun loginAsGuest() {
        username = "Invité"; userId = 0; isAuthenticated = true; isGuest = true
        score = 0.0; perClick = 1.0; perSecond = 0.0; BASE_COSTS.forEach { costs[it.key] = it.value }
    }

    fun logout() {
        isAuthenticated = false; isGuest = false; username = ""; userId = 0
        catBought = false; cat2Bought = false; volcanBought = false; cat3Bought = false; gooseBought = false
        BASE_COSTS.forEach { costs[it.key] = it.value }
        prefs.edit().remove("app_username").remove("app_userId").apply()
    }

    // --- GAMEPLAY ---
    fun addScore() { score += perClick }

    fun buyUpgrade(key: String, bonus: Double, isAuto: Boolean) {
        val cost = costs[key] ?: return
        if (score >= cost) {
            score -= cost
            if (isAuto) perSecond += (bonus * powerMult()) else perClick += bonus
            costs[key] = cost * 1.5
            saveShopLocally()
        }
    }

    fun sellUpgrade(key: String, statBonus: Double, isAuto: Boolean) {
        val currentCost = costs[key] ?: return
        val previousCost = currentCost / 1.5
        score += (previousCost / 2.0)
        costs[key] = previousCost
        if (isAuto) perSecond = max(0.0, perSecond - (statBonus * powerMult())) else perClick = max(0.0, perClick - statBonus)
        saveShopLocally()
    }

    fun buyMultX2(isAuto: Boolean) {
        val key = if (isAuto) "autoMultX2Cost" else "multX2Cost"
        val cost = costs[key] ?: return
        if (score >= cost) { score -= cost; if (isAuto) perSecond *= 2 else perClick *= 2; costs[key] = cost * 3; saveShopLocally() }
    }

    fun buyUltimateClick() {
        val cost = costs["ultimateClickCost"] ?: return
        if (score >= cost) { score -= cost; perClick *= 3; costs["ultimateClickCost"] = cost * 4; saveShopLocally() }
    }

    // --- FAMILIERS ---
    fun buyCat() { val c = costs["catUpgradeCost"] ?: 0.0; if (score >= c && !catBought) { score -= c; perSecond += (5 * powerMult()); catBought = true; saveShopLocally() } }
    fun buyCat2() { val c = costs["cat2UpgradeCost"] ?: 0.0; if (score >= c && !cat2Bought) { score -= c; perSecond += (60 * powerMult()); cat2Bought = true; saveShopLocally() } }
    fun buyVolcan() { val c = costs["volcanCost"] ?: 0.0; if (score >= c && !volcanBought) { score -= c; perSecond += (700 * powerMult()); volcanBought = true; saveShopLocally() } }
    fun buyCat3() { val c = costs["cat3UpgradeCost"] ?: 0.0; if (score >= c && !cat3Bought) { score -= c; perSecond += (6000 * powerMult()); cat3Bought = true; saveShopLocally() } }
    fun buyGoose() { val c = costs["gooseCost"] ?: 0.0; if (score >= c && !gooseBought) { score -= c; perSecond += (35000 * powerMult()); gooseBought = true; saveShopLocally() } }

    fun triggerRebirth() {
        if (score >= 1e36 && rebirthCount < 6) {
            rebirthCount++
            score = 0.0; perClick = 1.0 * powerMult(); perSecond = 0.0
            catBought = false; cat2Bought = false; volcanBought = false; cat3Bought = false; gooseBought = false

            val hm = highMult(); val lm = lowMult()
            BASE_COSTS.forEach { (k, v) ->
                costs[k] = when {
                    listOf("clickUpgradeCost", "autoUpgradeCost", "catUpgradeCost", "cat2UpgradeCost").contains(k) -> v * lm
                    k == "volcanCost" -> v * ((lm + hm) / 2.0)
                    else -> v * hm
                }
            }
            saveShopLocally()
            saveGameToServer()
        }
    }

    // --- SAUVEGARDES ET BOUCLES ---
    private fun startLoops() {
        viewModelScope.launch {
            var ticks = 0
            while (true) {
                delay(1000)
                if (perSecond > 0) score += perSecond
                ticks++
                if (ticks >= 10) { if (isAuthenticated && userId != 0) saveGameToServer(); ticks = 0 }
            }
        }
    }

    fun saveShopLocally() {
        if (userId == 0) return
        val jsonCosts = JSONObject()
        costs.forEach { jsonCosts.put(it.key, it.value) }
        prefs.edit()
            .putString("shop_costs_$userId", jsonCosts.toString())
            .putBoolean("catBought_$userId", catBought).putBoolean("cat2Bought_$userId", cat2Bought)
            .putBoolean("volcanBought_$userId", volcanBought).putBoolean("cat3Bought_$userId", cat3Bought).putBoolean("gooseBought_$userId", gooseBought)
            .apply()
    }

    private fun loadShopLocally() {
        if (userId == 0) return
        BASE_COSTS.forEach { costs[it.key] = it.value } // Fusion de base

        val savedCostsStr = prefs.getString("shop_costs_$userId", null)
        if (savedCostsStr != null) {
            val json = JSONObject(savedCostsStr)
            json.keys().forEach { costs[it] = json.getDouble(it) }
        }

        catBought = prefs.getBoolean("catBought_$userId", false)
        cat2Bought = prefs.getBoolean("cat2Bought_$userId", false)
        volcanBought = prefs.getBoolean("volcanBought_$userId", false)
        cat3Bought = prefs.getBoolean("cat3Bought_$userId", false)
        gooseBought = prefs.getBoolean("gooseBought_$userId", false)
    }

    fun loadGameFromServer() {
        if (userId == 0) return
        viewModelScope.launch {
            val data = ApiManager.loadGame(userId)
            if (data != null) {
                score = data.score; perClick = data.perClick; perSecond = data.perSecond; rebirthCount = data.rebirthCount
                loadShopLocally()
            } else {
                saveGameToServer()
                saveShopLocally()
            }
        }
    }

    fun saveGameToServer() {
        if (userId == 0) return
        viewModelScope.launch { ApiManager.saveGame(userId, score, perClick, perSecond, rebirthCount) }
    }

    fun formatNumber(value: Double): String {
        if (value < 1000) return String.format("%.0f", value)
        if (value.isInfinite()) return "INFINI"
        val suffixes = listOf("k", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc", "Ud", "Dd", "Td", "Qad", "Qid", "Sxd", "Spd", "Ocd", "Nod", "Vg", "UVg", "DVg", "TVg", "QaVg", "QiVg", "SxVg", "SpVg", "OcVg", "NoVg", "Tg", "UTg", "DTg", "TTg", "NoSpg")
        val index = (log10(value).toInt() / 3) - 1
        return if (index in suffixes.indices) String.format("%.2f %s", value / 10.0.pow((index + 1) * 3), suffixes[index]) else String.format("%.2e", value)
    }
}