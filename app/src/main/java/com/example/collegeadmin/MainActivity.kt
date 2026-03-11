package com.example.collegeadmin

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.*
import com.example.collegeadmin.data.CollegeRepository
import com.example.collegeadmin.data.local.AppDatabase
import com.example.collegeadmin.notifications.NotificationWorker
import com.example.collegeadmin.ui.CollegeViewModel
import com.example.collegeadmin.ui.screens.*
import com.example.collegeadmin.ui.theme.CollegeAdminTheme
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicialização segura do repositório
        val repository = try {
            val database = AppDatabase.getDatabase(this)
            CollegeRepository(database.collegeDao())
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao inicializar o banco de dados", e)
            null
        }
        
        if (repository != null) {
            setupBackgroundNotifications()
        }
        
        enableEdgeToEdge()
        setContent {
            if (repository == null) {
                // Tela de erro caso o banco falhe
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Erro ao carregar banco de dados. Tente limpar os dados do app.")
                }
                return@setContent
            }

            val viewModel: CollegeViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return CollegeViewModel(repository) as T
                    }
                }
            )

            val darkModeSetting by viewModel.isDarkMode.collectAsState()
            val useDarkTheme = darkModeSetting ?: isSystemInDarkTheme()

            CollegeAdminTheme(darkTheme = useDarkTheme) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val launcher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { }
                    LaunchedEffect(Unit) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                
                val userInfo by viewModel.userInfo.collectAsState()
                
                if (userInfo == null) {
                    OnboardingScreen(
                        viewModel = viewModel,
                        onComplete = { name, course, shift, period, start, end ->
                            viewModel.saveUserInfo(name, course, shift, period, start, end)
                        }
                    )
                } else {
                    CollegeAdminApp(viewModel)
                }
            }
        }
    }

    private fun setupBackgroundNotifications() {
        try {
            val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
                .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "college_notification_work",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao configurar WorkManager", e)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollegeAdminApp(viewModel: CollegeViewModel) {
    val destinations = AppDestinations.entries
    val pagerState = rememberPagerState(pageCount = { destinations.size })
    val scope = rememberCoroutineScope()
    var showMenuSheet by remember { mutableStateOf(false) }

    val onIndicatorClick = { showMenuSheet = true }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true,
            beyondViewportPageCount = 1 // Melhora a estabilidade ao deslizar
        ) { page ->
            CompositionLocalProvider(LocalIndicatorClick provides onIndicatorClick) {
                when (destinations[page]) {
                    AppDestinations.DASHBOARD -> DashboardScreen(viewModel, innerPadding) { destination ->
                        scope.launch {
                            pagerState.animateScrollToPage(destination.ordinal)
                        }
                    }
                    AppDestinations.SUBJECTS -> SubjectsScreen(viewModel, innerPadding)
                    AppDestinations.SCHEDULE -> ScheduleScreen(viewModel, innerPadding)
                    AppDestinations.ROUTINE -> RoutineScreen(viewModel, innerPadding)
                    AppDestinations.AI_TUTOR -> AiTutorScreen(viewModel, innerPadding)
                    AppDestinations.PROFILE -> ProfileScreen(viewModel, innerPadding)
                }
            }
        }
    }

    if (showMenuSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMenuSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Navegação", 
                    style = MaterialTheme.typography.titleLarge, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                destinations.forEach { destination ->
                    val isSelected = pagerState.currentPage == destination.ordinal
                    Surface(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(destination.ordinal)
                                showMenuSheet = false
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                destination.icon, 
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                destination.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

val LocalIndicatorClick = staticCompositionLocalOf<() -> Unit> { { } }

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    DASHBOARD("Início", Icons.Default.Home),
    SUBJECTS("Disciplinas", Icons.Default.Info),
    SCHEDULE("Aulas", Icons.Default.DateRange),
    ROUTINE("Estudos", Icons.AutoMirrored.Filled.List),
    AI_TUTOR("Tutor", Icons.Default.AutoAwesome),
    PROFILE("Perfil", Icons.Default.Person),
}
