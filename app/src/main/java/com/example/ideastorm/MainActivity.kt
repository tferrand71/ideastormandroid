package com.letotoo06.ideastorm // ⚠️ Vérifie que c'est bien le nom de ton package !

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

// ==========================================
// 🚀 POINT D'ENTRÉE PRINCIPAL
// ==========================================
class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF1A1A24))) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (viewModel.isAuthenticated) {
                        MainNavigation(viewModel)
                    } else {
                        AuthScreen(viewModel)
                    }
                }
            }
        }
    }
}

// ==========================================
// 🔐 ÉCRAN DE CONNEXION / INSCRIPTION
// ==========================================
@Composable
fun AuthScreen(viewModel: GameViewModel) {
    var isLogin by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Star, contentDescription = "Logo", tint = Color.Red, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(if (isLogin) "Connexion" else "Inscription", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = username, onValueChange = { username = it },
            label = { Text("Pseudo") },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Mot de passe") },
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator(color = Color.Red)
        } else {
            Button(
                onClick = {
                    isLoading = true
                    if (isLogin) {
                        viewModel.login(username, password) { success, msg -> message = msg; isLoading = false }
                    } else {
                        viewModel.signup(username, password) { success, msg -> message = msg; isLoading = false }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text(if (isLogin) "SE CONNECTER" else "S'INSCRIRE", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { isLogin = !isLogin }) {
            Text(if (isLogin) "Pas de compte ? S'inscrire" else "Déjà un compte ? Se connecter", color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { viewModel.loginAsGuest() }) {
            Text("Jouer en tant qu'invité", color = Color.Yellow)
        }

        if (message.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, color = if (message.contains("Erreur") || message.contains("pris")) Color.Red else Color.Green)
        }
    }
}

// ==========================================
// 📱 NAVIGATION PRINCIPALE (BOTTOM BAR)
// ==========================================
@Composable
fun MainNavigation(viewModel: GameViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF111118)) {
                NavigationBarItem(icon = { Icon(Icons.Filled.Home, "Jeu") }, label = { Text("Jeu") }, selected = selectedTab == 0, onClick = { selectedTab = 0 }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Red, selectedTextColor = Color.Red, indicatorColor = Color.Red.copy(alpha = 0.2f)))
                NavigationBarItem(icon = { Icon(Icons.Filled.ShoppingCart, "Boutique") }, label = { Text("Boutique") }, selected = selectedTab == 1, onClick = { selectedTab = 1 }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Red, selectedTextColor = Color.Red, indicatorColor = Color.Red.copy(alpha = 0.2f)))
                NavigationBarItem(icon = { Icon(Icons.Filled.Person, "Compte") }, label = { Text("Compte") }, selected = selectedTab == 2, onClick = { selectedTab = 2 }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Red, selectedTextColor = Color.Red, indicatorColor = Color.Red.copy(alpha = 0.2f)))

                if (viewModel.username.lowercase() == "letotoo06") {
                    NavigationBarItem(icon = { Icon(Icons.Filled.Warning, "Admin") }, label = { Text("Admin") }, selected = selectedTab == 3, onClick = { selectedTab = 3 }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Yellow, selectedTextColor = Color.Yellow, indicatorColor = Color.Yellow.copy(alpha = 0.2f)))
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (selectedTab) {
                0 -> GameScreen(viewModel)
                1 -> ShopScreen(viewModel)
                2 -> AccountScreen(viewModel)
                3 -> AdminScreen(viewModel)
            }
        }
    }
}

// ==========================================
// 🎮 ÉCRAN DE JEU (AVEC CLICS ET PARTICULES)
// ==========================================
data class ClickEffect(val id: String = UUID.randomUUID().toString(), val x: Float, val y: Float, val amount: Double)

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val clickEffects = remember { mutableStateListOf<ClickEffect>() }
    val coroutineScope = rememberCoroutineScope()

    // Animation du bouton
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.85f else 1.0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "logoScale")

    Box(modifier = Modifier.fillMaxSize()) {
        // Image de fond
        Image(
            painter = painterResource(id = R.drawable.background_game),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))

        Column(
            modifier = Modifier.fillMaxSize().padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("IdeaStorm", fontSize = 45.sp, fontWeight = FontWeight.Black, color = Color.White)

            // 👑 CORRECTION ICI : Remplacement de Heavy par Black
            Text(viewModel.formatNumber(viewModel.score), fontSize = 55.sp, fontWeight = FontWeight.Black, color = Color.Yellow)

            Text("par seconde : ${viewModel.formatNumber(viewModel.perSecond)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.9f))

            Spacer(modifier = Modifier.weight(1f))

            // Le Logo cliquable
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .scale(scale)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                            },
                            onTap = { offset ->
                                viewModel.addScore()
                                val effect = ClickEffect(x = offset.x, y = offset.y, amount = viewModel.perClick)
                                clickEffects.add(effect)
                                coroutineScope.launch {
                                    delay(1000)
                                    clickEffects.remove(effect)
                                }
                            }
                        )
                    }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ideastorm_logo),
                    contentDescription = "Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.weight(1.5f))
        }

        // Affichage des particules de clic
        clickEffects.forEach { effect ->
            FloatingText(effect)
        }
    }
}

@Composable
fun FloatingText(effect: ClickEffect) {
    var yOffset by remember { mutableFloatStateOf(0f) }
    var opacity by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(Unit) {
        animate(initialValue = 0f, targetValue = -150f, animationSpec = tween(1000)) { value, _ -> yOffset = value }
    }
    LaunchedEffect(Unit) {
        animate(initialValue = 1f, targetValue = 0f, animationSpec = tween(1000)) { value, _ -> opacity = value }
    }

    Text(
        text = "+${effect.amount.toLong()}",
        fontSize = 30.sp,
        fontWeight = FontWeight.Black,
        color = Color.White.copy(alpha = opacity),
        modifier = Modifier.offset(x = (effect.x - 50).dp, y = (effect.y + yOffset).dp)
    )
}

// ==========================================
// 🛒 BOUTIQUE GLISSANTE (SLIDING SHOP)
// ==========================================
data class UpgradeItem(val id: String, val label: String, val bonus: Double, val minSell: Double, val color: Color = Color.White)

@Composable
fun ShopScreen(viewModel: GameViewModel) {
    val clickUpgrades = listOf(
        UpgradeItem("clickUpgradeCost", "👆 +1 Clic", 1.0, 50.0),
        UpgradeItem("superClickCost", "🌟 Super (+10k)", 10000.0, 5e5),
        UpgradeItem("megaClickCost", "🔥 Méga (+100k)", 100000.0, 2.5e6),
        UpgradeItem("gigaClickCost", "🚀 Giga (+200k)", 200000.0, 1.5e7),
        UpgradeItem("click500kCost", "💎 +500k Clic", 500000.0, 4e7),
        UpgradeItem("click1mCost", "💎 +1M Clic", 1e6, 1e8),
        UpgradeItem("click10mCost", "💎 +10M Clic", 1e7, 1e9),
        UpgradeItem("click100mCost", "💎 +100M Clic", 1e8, 1e10),
        UpgradeItem("click1bCost", "🪐 +1B Clic", 1e9, 1e11),
        UpgradeItem("click10bCost", "🪐 +10B Clic", 1e10, 1e12),
        UpgradeItem("click100bCost", "🪐 +100B Clic", 1e11, 1e13),
        UpgradeItem("godClickACost", "⚡ Trillion (1T)", 1e12, 1e12),
        UpgradeItem("godClickAACost", "🌌 Quadrillion (1Qa)", 1e15, 1e15),
        UpgradeItem("clickSextillionCost", "✨ Sextillion (1Sx)", 1e21, 1e21),
        UpgradeItem("clickNonillionCost", "💫 Octillion (1e27)", 1e27, 1e27),
        UpgradeItem("clickDuodecillionCost", "🌀 Undécillion (1e36)", 1e36, 1e36),
        UpgradeItem("clickVigintillionCost", "⚛️ Vigintillion (1e63)", 1e60, 1e63, Color.Cyan),
        UpgradeItem("clickTrigintillionCost", "🪐 Trigintillion (1e93)", 1e90, 1e93),
        UpgradeItem("clickGoogolCost", "🔥 GOOGOL (1e100)", 1e100, 1e100, Color(0xFFFFA500)),
        UpgradeItem("click1e120Cost", "💠 1e120 Clic", 1e120, 1e120),
        UpgradeItem("clickCentillionCost", "💀 CENTILLION (1e303)", 1e303, 1e303, Color.Yellow)
    )

    val autoUpgrades = listOf(
        UpgradeItem("autoUpgradeCost", "🔄 +2 Auto", 2.0, 100.0),
        UpgradeItem("auto500kCost", "⚙️ +500k Auto", 500000.0, 4e7),
        UpgradeItem("auto1mCost", "🏭 +1M Auto", 1e6, 1e8),
        UpgradeItem("auto10mCost", "🏭 +10M Auto", 1e7, 1e9),
        UpgradeItem("auto100mCost", "🏭 +100M Auto", 1e8, 1e10),
        UpgradeItem("auto1bCost", "🤖 +1B Auto", 1e9, 1e11),
        UpgradeItem("auto10bCost", "🤖 +10B Auto", 1e10, 1e12),
        UpgradeItem("auto100bCost", "🤖 +100B Auto", 1e11, 1e13),
        UpgradeItem("godAutoACost", "⚡ 1T Auto", 1e12, 1e12),
        UpgradeItem("godAutoAACost", "🌌 1Qa Auto", 1e15, 1e15),
        UpgradeItem("autoSextillionCost", "✨ 1Sx Auto", 1e21, 1e21),
        UpgradeItem("autoNonillionCost", "💫 1e27 Auto", 1e27, 1e27),
        UpgradeItem("autoDuodecillionCost", "🌀 1e36 Auto", 1e36, 1e36),
        UpgradeItem("autoGoogolCost", "🔥 GOOGOL Auto", 1e100, 1e100, Color(0xFFFFA500)),
        UpgradeItem("auto1e120Cost", "💠 1e120 Auto", 1e120, 1e120),
        UpgradeItem("autoCentillionCost", "💀 CENTILLION Auto", 1e303, 1e303, Color.Yellow)
    )

    // La Fenêtre Glissante
    fun getVisibleItems(list: List<UpgradeItem>): List<UpgradeItem> {
        val factor = if (viewModel.rebirthCount == 0) 1.0 else (viewModel.rebirthCount + 2.0)
        val lastPurchasedIndex = list.indexOfLast { item ->
            (viewModel.costs[item.id] ?: 0.0) > (item.minSell * factor)
        }.coerceAtLeast(0)
        return list.subList(lastPurchasedIndex, minOf(lastPurchasedIndex + 4, list.size))
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            Column(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(15.dp)).padding(16.dp)) {
                Text("💰 Score: ${viewModel.formatNumber(viewModel.score)}", fontWeight = FontWeight.Bold, color = Color.White)
                Text("👆 / Clic: ${viewModel.formatNumber(viewModel.perClick)}", color = Color.Gray)
                Text("⏱️ / Sec: ${viewModel.formatNumber(viewModel.perSecond)}", color = Color.Gray)
            }
        }

        item { Text("Améliorations Clics", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Red) }
        items(getVisibleItems(clickUpgrades)) { item -> ShopRow(viewModel, item, false) }

        item { Text("Automatisation", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Blue) }
        items(getVisibleItems(autoUpgrades)) { item -> ShopRow(viewModel, item, true) }

        if (viewModel.score >= 1e6) {
            item {
                Text("Puissance Maximale", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9C27B0))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { viewModel.buyMultX2(false) }, enabled = viewModel.score >= (viewModel.costs["multX2Cost"] ?: 1e8), modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                        Text("⚡ Clic x2\n${viewModel.formatNumber(viewModel.costs["multX2Cost"] ?: 1e8)}", textAlign = TextAlign.Center)
                    }
                    Button(onClick = { viewModel.buyMultX2(true) }, enabled = viewModel.score >= (viewModel.costs["autoMultX2Cost"] ?: 1e8), modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                        Text("⚡ Auto x2\n${viewModel.formatNumber(viewModel.costs["autoMultX2Cost"] ?: 1e8)}", textAlign = TextAlign.Center)
                    }
                }
            }
        }

        item {
            Text("Compagnons", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Green)
            UniqueCompanionBtn(viewModel, "🤪 Chat Débile", "catUpgradeCost", viewModel.catBought) { viewModel.buyCat() }
            UniqueCompanionBtn(viewModel, "🥷 Chat Ninja", "cat2UpgradeCost", viewModel.cat2Bought) { viewModel.buyCat2() }
            UniqueCompanionBtn(viewModel, "🔫 Chat Tueur", "volcanCost", viewModel.volcanBought) { viewModel.buyVolcan() }
            UniqueCompanionBtn(viewModel, "👑 Roi Chat", "cat3UpgradeCost", viewModel.cat3Bought) { viewModel.buyCat3() }
            UniqueCompanionBtn(viewModel, "🪿 L'Oie d'Or", "gooseCost", viewModel.gooseBought) { viewModel.buyGoose() }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth().border(3.dp, Color.Yellow, RoundedCornerShape(15.dp)).background(Color.Black, RoundedCornerShape(15.dp)).padding(16.dp)) {
                Text("🌟 ASCENSION ${viewModel.rebirthCount} / 6", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.Yellow)
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.triggerRebirth() },
                    enabled = viewModel.score >= 1e36,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow, disabledContainerColor = Color.DarkGray)
                ) {
                    Text(if (viewModel.score >= 1e36) "☄️ DÉCLENCHER LE REBIRTH" else "Requis : 1 Undécillion", color = Color.Black, fontWeight = FontWeight.Black)
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun ShopRow(viewModel: GameViewModel, item: UpgradeItem, isAuto: Boolean) {
    val cost = viewModel.costs[item.id] ?: item.minSell
    val canBuy = viewModel.score >= cost
    val factor = if (viewModel.rebirthCount == 0) 1.0 else (viewModel.rebirthCount + 2.0)
    val canSell = cost > (item.minSell * factor) && !item.id.contains("Centillion")

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = { viewModel.buyUpgrade(item.id, item.bonus, isAuto) },
            enabled = canBuy,
            modifier = Modifier.weight(3f).height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.4f), disabledContainerColor = Color.Black.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.label, color = item.color)
                Text(viewModel.formatNumber(cost), fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        if (canSell) {
            Button(
                onClick = { viewModel.sellUpgrade(item.id, item.bonus, isAuto) },
                modifier = Modifier.weight(1f).height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Vendre", color = Color.Red, fontSize = 12.sp)
            }
        } else {
            Box(modifier = Modifier.weight(1f).height(60.dp).border(1.dp, Color.Gray.copy(alpha=0.5f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Text("-", color = Color.Gray)
            }
        }
    }
}

@Composable
fun UniqueCompanionBtn(viewModel: GameViewModel, label: String, costKey: String, isBought: Boolean, action: () -> Unit) {
    val cost = viewModel.costs[costKey] ?: 0.0
    val canBuy = viewModel.score >= cost

    if (!isBought) {
        Button(
            onClick = action, enabled = canBuy, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), disabledContainerColor = Color(0xFF1B5E20))
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, color = Color.White)
                Text(viewModel.formatNumber(cost), color = Color.White)
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).height(50.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Text("✅ $label acquis", color = Color.White.copy(alpha = 0.7f))
        }
    }
}

// ==========================================
// 👤 MON COMPTE
// ==========================================
@Composable
fun AccountScreen(viewModel: GameViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Person, contentDescription = "Avatar", tint = if (viewModel.username.lowercase() == "letotoo06") Color.Red else Color.Gray, modifier = Modifier.size(100.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(viewModel.username, fontSize = 35.sp, fontWeight = FontWeight.Black, color = Color.White)

        // 👑 CORRECTION ICI : Remplacement de Color.Orange par son code Hexadécimal
        if (viewModel.isGuest) Text("Joueur non sauvegardé", color = Color(0xFFFFA500))

        Spacer(modifier = Modifier.height(32.dp))

        Column(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(15.dp)).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Score actuel", color = Color.Gray)
                Text(viewModel.formatNumber(viewModel.score), fontWeight = FontWeight.Bold, color = Color.Yellow)
            }
            Divider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Ascensions", color = Color.Gray)
                Text("${viewModel.rebirthCount} / 6", fontWeight = FontWeight.Bold, color = Color(0xFF9C27B0))
            }
        }

        Spacer(modifier = Modifier.height(50.dp))

        Button(
            onClick = {
                viewModel.saveGameToServer()
                viewModel.logout()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
        ) {
            Text("Se déconnecter", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// ==========================================
// 👑 PANNEAU TITAN (ADMIN)
// ==========================================
@Composable
fun AdminScreen(viewModel: GameViewModel) {
    var players by remember { mutableStateOf<List<PlayerData>>(emptyList()) }
    var selectedPlayer by remember { mutableStateOf<PlayerData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        players = ApiManager.fetchAdminList()
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Warning, contentDescription = "Titan", tint = Color.Red, modifier = Modifier.size(50.dp).padding(top = 20.dp))
        Text("PANNEAU TITAN", fontSize = 35.sp, fontWeight = FontWeight.Black, color = Color.White)
        Spacer(modifier = Modifier.height(20.dp))

        if (isLoading) {
            CircularProgressIndicator(color = Color.Red)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(players) { player ->
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp)).border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(10.dp)).padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(player.username, fontWeight = FontWeight.Black, color = Color.Red, fontSize = 20.sp)
                            Text("Score: ${viewModel.formatNumber(player.score)}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        }
                        IconButton(onClick = { selectedPlayer = player }, modifier = Modifier.background(Color.Blue.copy(alpha = 0.8f), CircleShape)) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Color.White)
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    selectedPlayer?.let { player ->
        AdminEditDialog(viewModel, player, onDismiss = {
            selectedPlayer = null
            coroutineScope.launch { players = ApiManager.fetchAdminList() } // Refresh
        })
    }
}

@Composable
fun AdminEditDialog(viewModel: GameViewModel, player: PlayerData, onDismiss: () -> Unit) {
    var scoreStr by remember { mutableStateOf(player.score.toLong().toString()) }
    var rCount by remember { mutableIntStateOf(player.rebirthCount) }
    var pClick by remember { mutableStateOf("1") }
    var pSec by remember { mutableStateOf("0") }
    var isSaving by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(player.id) {
        val fullData = ApiManager.loadGame(player.id)
        if (fullData != null) {
            pClick = fullData.perClick.toLong().toString()
            pSec = fullData.perSecond.toLong().toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A24),
        title = { Text("Éditer ${player.username}", color = Color.Red, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = scoreStr, onValueChange = { scoreStr = it }, label = { Text("Score") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = rCount.toString(), onValueChange = { rCount = it.toIntOrNull() ?: 0 }, label = { Text("Ascensions") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = pClick, onValueChange = { pClick = it }, label = { Text("Clic") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = pSec, onValueChange = { pSec = it }, label = { Text("Auto") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSaving = true
                    coroutineScope.launch {
                        val newScore = scoreStr.toDoubleOrNull() ?: 0.0
                        val newClick = pClick.toDoubleOrNull() ?: 1.0
                        val newSec = pSec.toDoubleOrNull() ?: 0.0

                        val success = ApiManager.saveGame(player.id, newScore, newClick, newSec, rCount)
                        if (success && player.id == viewModel.userId) {
                            viewModel.score = newScore
                            viewModel.perClick = newClick
                            viewModel.perSecond = newSec
                            viewModel.rebirthCount = rCount
                            viewModel.saveShopLocally()
                        }
                        isSaving = false
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White) else Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = Color.Gray) }
        }
    )
}