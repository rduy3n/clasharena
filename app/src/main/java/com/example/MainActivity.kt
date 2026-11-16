package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.firebase.GameRepository
import com.example.game.data.BotDifficulty
import com.example.game.data.GameMode
import com.example.game.data.TrainingOpponentMode
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutUser
import com.example.ui.screens.BattleScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MatchSetupScreen
import com.example.ui.screens.OnlineLobbyScreen
import com.example.ui.screens.RosterScreen
import com.example.ui.theme.ClashArenaTheme
import com.example.ui.viewmodel.BattleViewModel
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClashArenaTheme {
                AppNavigation()
            }
        }
    }
}

object Routes {
    const val HOME = "home"
    const val ROSTER = "roster"
    const val SETUP_BOT = "setup_bot"
    const val SETUP_TRAINING = "setup_training"
    const val ONLINE_LOBBY = "online_lobby"
    const val BATTLE_LOCAL = "battle_local/{mode}/{p1}/{p2}/{stage}/{diff}/{oppMode}"
    const val BATTLE_ONLINE = "battle_online/{roomCode}"
}

@Composable
fun AppNavigation() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedApp(
                currentUserId = user.uid,
                userDisplayName = user.displayName ?: user.email?.substringBefore("@") ?: "Combattant"
            )
        }
    }
}

@Composable
private fun AuthenticatedApp(
    currentUserId: String,
    userDisplayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val mainViewModel: MainViewModel = viewModel(
        key = "main_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                MainViewModel(GameRepository(db), currentUserId, userDisplayName)
            }
        }
    )

    val battleViewModel: BattleViewModel = viewModel(
        key = "battle_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                BattleViewModel(GameRepository(db), currentUserId)
            }
        }
    )

    val profileState by mainViewModel.profileState.collectAsStateWithLifecycle()
    val profile = (profileState as? UiState.Success)?.data
    val favFighterId = profile?.favoriteFighterId ?: "ryuji"

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                profile = profile,
                fallbackName = userDisplayName,
                onPlayVsBot = { navController.navigate(Routes.SETUP_BOT) },
                onPlayTraining = { navController.navigate(Routes.SETUP_TRAINING) },
                onPlayOnline = { navController.navigate(Routes.ONLINE_LOBBY) },
                onOpenRoster = { navController.navigate(Routes.ROSTER) },
                onSignOut = { signOutUser(credentialManager, scope) }
            )
        }

        composable(Routes.ROSTER) {
            RosterScreen(
                favoriteFighterId = favFighterId,
                onSetFavorite = { mainViewModel.setFavoriteFighter(it) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETUP_BOT) {
            MatchSetupScreen(
                mode = GameMode.VS_BOT,
                defaultFighterId = favFighterId,
                onStartMatch = { p1, p2, stage, diff, oppMode ->
                    navController.navigate("battle_local/${GameMode.VS_BOT.name}/$p1/$p2/$stage/${diff.name}/${oppMode.name}")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETUP_TRAINING) {
            MatchSetupScreen(
                mode = GameMode.TRAINING,
                defaultFighterId = favFighterId,
                onStartMatch = { p1, p2, stage, diff, oppMode ->
                    navController.navigate("battle_local/${GameMode.TRAINING.name}/$p1/$p2/$stage/${diff.name}/${oppMode.name}")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ONLINE_LOBBY) {
            OnlineLobbyScreen(
                mainViewModel = mainViewModel,
                currentUserId = currentUserId,
                favoriteFighterId = favFighterId,
                onEnterOnlineMatch = { roomCode ->
                    navController.navigate("battle_online/$roomCode") {
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.BATTLE_LOCAL) { backStackEntry ->
            val mode = GameMode.valueOf(backStackEntry.arguments?.getString("mode") ?: GameMode.VS_BOT.name)
            val p1 = backStackEntry.arguments?.getString("p1") ?: "ryuji"
            val p2 = backStackEntry.arguments?.getString("p2") ?: "kaelith"
            val stage = backStackEntry.arguments?.getString("stage") ?: "cyber_street"
            val diff = BotDifficulty.valueOf(backStackEntry.arguments?.getString("diff") ?: BotDifficulty.NORMAL.name)
            val oppMode = TrainingOpponentMode.valueOf(backStackEntry.arguments?.getString("oppMode") ?: TrainingOpponentMode.STATIONARY.name)

            LaunchedEffect(mode, p1, p2, stage, diff, oppMode) {
                battleViewModel.startLocalMatch(mode, p1, p2, stage, diff, oppMode)
            }

            BattleScreen(
                viewModel = battleViewModel,
                onRematchLocal = {
                    battleViewModel.startLocalMatch(mode, p1, p2, stage, diff, oppMode)
                },
                onExitBattle = {
                    battleViewModel.stopAll()
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.BATTLE_ONLINE) { backStackEntry ->
            val roomCode = backStackEntry.arguments?.getString("roomCode") ?: return@composable

            LaunchedEffect(roomCode) {
                battleViewModel.startOnlineMatch(roomCode)
            }

            BattleScreen(
                viewModel = battleViewModel,
                onRematchLocal = {},
                onExitBattle = {
                    battleViewModel.stopAll()
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }
    }
}
