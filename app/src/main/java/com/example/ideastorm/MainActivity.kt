package com.letotoo06.ideastorm

import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlin.math.pow

// --- COULEURS ---
val TextOrange = Color(0xFFFF6F61)
val TextRedScore = Color(0xFFFF7675)
val PrimaryBlue = Color(0xFF74B9FF)
val GreenSuccess = Color(0xFF00B894)
val GoldColor = Color(0xFFFFD700)
val CardBg = Color.White.copy(alpha = 0.92f)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GameScreen() }
    }
}

// Enum pour la navigation
enum class Screen { HOME, SHOP }

data class CompanionDisplayData(val name: String, val isUnlocked: Boolean, val videoRes: Int)

@Composable
fun GameScreen(viewModel: GameViewModel = viewModel()) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var showCompanionsGallery by remember { mutableStateOf(false) }
    var showRebirthDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) } // Pour le Reset

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background_game),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(containerColor = Color.White.copy(alpha = 0.9f)) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Jeu") },
                        label = { Text("Jeu") },
                        selected = currentScreen == Screen.HOME,
                        onClick = { currentScreen = Screen.HOME }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Boutique") },
                        label = { Text("Boutique") },
                        selected = currentScreen == Screen.SHOP,
                        onClick = { currentScreen = Screen.SHOP }
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // --- HEADER COMMUN ---
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bouton Profil
                    Button(
                        onClick = { if (viewModel.currentUser == null) showLoginDialog = true else viewModel.logout() },
                        colors = ButtonDefaults.buttonColors(containerColor = if (viewModel.currentUser != null) GreenSuccess else Color.White),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(35.dp)
                    ) {
                        Icon(Icons.Default.Person, null, tint = if (viewModel.currentUser != null) Color.White else Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(text = viewModel.currentUser?.username ?: "Login", color = if (viewModel.currentUser != null) Color.White else Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Niveau Ascension
                    if (viewModel.rebirthCount > 0) {
                        Surface(color = GoldColor, shape = RoundedCornerShape(20.dp), modifier = Modifier.height(30.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                                Icon(Icons.Default.Star, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                Text(" ${viewModel.rebirthCount}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }

                    // Boutons Droite : Galerie + Settings
                    Row {
                        Button(onClick = { showCompanionsGallery = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.White), shape = CircleShape, elevation = ButtonDefaults.buttonElevation(5.dp), contentPadding = PaddingValues(0.dp), modifier = Modifier.size(40.dp)) {
                            Text("🐾", fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { showSettingsDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.White), shape = CircleShape, elevation = ButtonDefaults.buttonElevation(5.dp), contentPadding = PaddingValues(0.dp), modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // --- CONTENU PRINCIPAL ---
                if (currentScreen == Screen.HOME) {
                    HomeContent(viewModel)
                } else {
                    ShopContent(viewModel, onRebirthClick = { showRebirthDialog = true })
                }
            }
        }

        // --- DIALOGS ---
        if (showLoginDialog) LoginDialog(viewModel) { showLoginDialog = false }
        if (showCompanionsGallery) CompanionsGalleryDialog(viewModel) { showCompanionsGallery = false }
        if (showRebirthDialog) RebirthDialog(viewModel) { showRebirthDialog = false }
        if (showSettingsDialog) SettingsDialog(viewModel) { showSettingsDialog = false }
    }
}

// --- ÉCRAN ACCUEIL (CLICKER) ---
@Composable
fun HomeContent(viewModel: GameViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(25.dp),
            modifier = Modifier.width(340.dp).shadow(15.dp, RoundedCornerShape(25.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("IdeaStorm", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextOrange)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = viewModel.formatNumber(viewModel.score),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = TextRedScore,
                    textAlign = TextAlign.Center,
                    lineHeight = 46.sp
                )
                Text("points", fontSize = 16.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(25.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatCol("⚡ Clic", viewModel.formatNumber(viewModel.perClick))
                    StatCol("⏱️ Auto", "${viewModel.formatNumber(viewModel.perSecond)}/s")
                }

                Spacer(modifier = Modifier.height(30.dp))

                Button(
                    onClick = { viewModel.onClick() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDFE6E9)),
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("CLIQUEZ ICI !", color = Color.DarkGray, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- ÉCRAN BOUTIQUE (PAGE SÉPARÉE) ---
@Composable
fun ShopContent(viewModel: GameViewModel, onRebirthClick: () -> Unit) {
    val clickUpgrades = remember(viewModel.upgrades) {
        viewModel.upgrades.filter { it.bonusAuto == 0.0 && !it.id.contains("auto", true) }
    }
    val autoUpgrades = remember(viewModel.upgrades) {
        viewModel.upgrades.filter { it.bonusAuto > 0.0 || it.id.contains("auto", true) }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxSize().padding(bottom = 10.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Boutique de Titouan", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextOrange)

                if (viewModel.score >= 1e36) {
                    Button(
                        onClick = onRebirthClick,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldColor),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("ASCENSION", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                item { HeaderText("AMÉLIORATIONS CLIC", PrimaryBlue) }
                items(clickUpgrades) { ShopItemRow(it, viewModel) }

                item { HeaderText("AUTOMATISATION & COMPAGNONS", GreenSuccess) }
                items(autoUpgrades) { ShopItemRow(it, viewModel) }
            }
        }
    }
}

// --- NOUVEAU DIALOGUE PARAMÈTRES (RESET) ---
@Composable
fun SettingsDialog(viewModel: GameViewModel, onDismiss: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Paramètres", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))

                if (!showConfirm) {
                    Button(
                        onClick = { showConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Réinitialiser la Sauvegarde", color = Color.White)
                    }
                } else {
                    Text("Êtes-vous vraiment sûr ?", color = Color.Red, fontWeight = FontWeight.Bold)
                    Text("Toute progression sera perdue !", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Button(onClick = { showConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                            Text("Annuler")
                        }
                        Button(
                            onClick = {
                                viewModel.hardReset()
                                showConfirm = false
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("CONFIRMER")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = onDismiss) { Text("Fermer", color = Color.Gray) }
            }
        }
    }
}

// ... LE RESTE DES COMPOSANTS (LoginDialog, RebirthDialog, CompanionsGalleryDialog, etc.) ...
// Copie ici le code des autres Dialogs (LoginDialog, RebirthDialog, etc.) du message précédent.
// Ils ne changent pas, mais sont nécessaires pour que le code compile.

@Composable
fun LoginDialog(viewModel: GameViewModel, onDismiss: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignup by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (isSignup) "Créer un compte" else "Connexion", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextOrange)
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Pseudo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Mot de passe") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                if (statusMessage.isNotEmpty()) { Spacer(modifier = Modifier.height(10.dp)); Text(statusMessage, color = if (statusMessage.contains("!") || statusMessage.contains("Connecté")) GreenSuccess else Color.Red, fontSize = 12.sp, textAlign = TextAlign.Center) }
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = { if (username.isNotBlank() && password.isNotBlank()) { isLoading = true; if (isSignup) viewModel.signup(username, password) { success, msg -> isLoading = false; statusMessage = msg; if (success) android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ onDismiss() }, 1000) } else viewModel.login(username, password) { success, msg -> isLoading = false; statusMessage = msg; if (success) android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ onDismiss() }, 1000) } } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue), enabled = !isLoading) { if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White) else Text(if (isSignup) "S'INSCRIRE" else "SE CONNECTER") }
                TextButton(onClick = { isSignup = !isSignup; statusMessage = "" }) { Text(if (isSignup) "Déjà un compte ? Se connecter" else "Pas de compte ? S'inscrire", fontSize = 12.sp, color = Color.Gray) }
            }
        }
    }
}

@Composable
fun RebirthDialog(viewModel: GameViewModel, onDismiss: () -> Unit) {
    val nextLevel = viewModel.rebirthCount + 1
    val currentMult = viewModel.formatNumber(50.0.pow(viewModel.rebirthCount))
    val nextMult = viewModel.formatNumber(50.0.pow(nextLevel))
    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ASCENSION", fontSize = 28.sp, fontWeight = FontWeight.Black, color = GoldColor)
                Text("Temps : ${viewModel.getRunDuration()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextOrange)
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF0F2F5), RoundedCornerShape(10.dp)).padding(15.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Actuel", fontSize = 12.sp, color = Color.Gray); Text("x$currentMult", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                    Text("➡", fontSize = 24.sp, color = GoldColor)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Nouveau", fontSize = 12.sp, color = GoldColor, fontWeight = FontWeight.Bold); Text("x$nextMult", fontWeight = FontWeight.Black, fontSize = 20.sp, color = GoldColor) }
                }
                Spacer(modifier = Modifier.height(25.dp))
                Button(onClick = { viewModel.triggerRebirth(); onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = GoldColor), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("RENAÎTRE", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun CompanionsGalleryDialog(viewModel: GameViewModel, onDismiss: () -> Unit) {
    val companions = listOf(
        CompanionDisplayData("Chat Débile", viewModel.hasDebile, R.raw.debile),
        CompanionDisplayData("Le Chat Ninja", viewModel.hasMimir, R.raw.mimir),
        CompanionDisplayData("Le Chat Tueur", viewModel.hasPistolet, R.raw.pistolet),
        CompanionDisplayData("Le Roi Chat", viewModel.hasSeducteur, R.raw.seducteur),
        CompanionDisplayData("L'Oie", viewModel.hasGoose, R.raw.goose)
    )
    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(550.dp).padding(5.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Mes Compagnons", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextOrange); IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Fermer") } }
                LazyVerticalGrid(GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { items(companions) { CompanionGridItem(it) } }
            }
        }
    }
}

@Composable
fun CompanionGridItem(data: CompanionDisplayData) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F2F5)), modifier = Modifier.height(150.dp)) {
        if (data.isUnlocked) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Box(modifier = Modifier.weight(1f).fillMaxWidth()) { VideoDisplay(data.videoRes, Modifier.fillMaxSize()) }; Text(data.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(5.dp), color = Color.Black) }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Lock, "Locked", tint = Color.Gray, modifier = Modifier.size(40.dp)); Text("???", fontWeight = FontWeight.Bold, color = Color.Gray) } }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoDisplay(resId: Int, modifier: Modifier) {
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(Uri.parse("android.resource://${context.packageName}/$resId"))); prepare(); repeatMode = Player.REPEAT_MODE_ONE; volume = 0f; playWhenReady = true } }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }
    AndroidView({ PlayerView(context).apply { player = exoPlayer; useController = false; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM; layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT); setBackgroundColor(0x00000000) } }, modifier.clip(RoundedCornerShape(10.dp)))
}

@Composable
fun StatCol(label: String, value: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(label, fontSize = 12.sp, color = Color.Gray); Text(value, fontWeight = FontWeight.Bold) } }
@Composable
fun HeaderText(text: String, color: Color) { Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.padding(top = 10.dp, bottom = 5.dp)) }

@Composable
fun ShopItemRow(item: GameUpgrade, viewModel: GameViewModel) {
    val canBuy = viewModel.score >= item.cost
    val isOwned = item.unlockGif != null && when(item.unlockGif) {
        "debile" -> viewModel.hasDebile
        "mimir" -> viewModel.hasMimir
        "pistolet" -> viewModel.hasPistolet
        "seducteur" -> viewModel.hasSeducteur
        "goose" -> viewModel.hasGoose
        else -> false
    }

    Row(modifier = Modifier.fillMaxWidth().height(70.dp).shadow(2.dp, RoundedCornerShape(15.dp)).clip(RoundedCornerShape(15.dp)).background(if (isOwned) Color(0xFFE0E0E0) else Color.White).clickable(enabled = canBuy && !isOwned) { viewModel.buyUpgrade(item) }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(if (isOwned) "✅ ${item.label}" else item.label, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (isOwned) Color.Gray else Color.Black)
            val bonusText = if (item.multiplier > 1.0) {
                if (item.bonusClick > 0) "Clic x${item.multiplier}" else if (item.bonusAuto > 0) "Auto x${item.multiplier}" else "Boost x${item.multiplier}"
            } else if (item.bonusClick > 0) "+${viewModel.formatNumber(item.bonusClick)} Clic"
            else "+${viewModel.formatNumber(item.bonusAuto)} /s"
            Text(bonusText, fontSize = 12.sp, color = Color.Gray)
        }
        if (!isOwned) Surface(color = if (canBuy) GreenSuccess.copy(0.1f) else Color.Red.copy(0.1f), shape = RoundedCornerShape(8.dp)) { Text(viewModel.formatNumber(item.cost), fontWeight = FontWeight.Bold, color = if (canBuy) GreenSuccess else Color.Red, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) } else Text("Acquis", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
    }
}