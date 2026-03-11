package com.example.collegeadmin.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.collegeadmin.ai.AiAssistant
import com.example.collegeadmin.ui.CollegeViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: CollegeViewModel,
    onComplete: (String, String, String, String, LocalDate, LocalDate) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var showHistoryScreen by remember { mutableStateOf(false) }
    
    var name by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var shift by remember { mutableStateOf("Manhã") }
    var periodName by remember { mutableStateOf("2026.1") }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusMonths(5)) }

    val shifts = listOf("Manhã", "Tarde", "Noite")

    // Validações
    val isStep0Valid = name.isNotBlank() && course.isNotBlank() && periodName.isNotBlank()
    val isDatesValid = !endDate.isBefore(startDate)
    
    BackHandler(currentStep > 0 && !showHistoryScreen) {
        currentStep--
    }

    if (showHistoryScreen) {
        HistoryEntryScreen(
            viewModel = viewModel,
            onBack = { showHistoryScreen = false }
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Background Decoration
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .offset(x = (-100).dp, y = (-100).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
            ) {
                Spacer(Modifier.height(60.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { (currentStep + 1) / 3f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
                
                Spacer(Modifier.height(32.dp))

                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                        } else {
                            (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                        }
                    },
                    label = "OnboardingStepTransition",
                    modifier = Modifier.weight(1f)
                ) { step ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        when (step) {
                            0 -> StepProfile(
                                name = name, onNameChange = { name = it },
                                course = course, onCourseChange = { course = it },
                                shift = shift, onShiftChange = { shift = it },
                                periodName = periodName, onPeriodChange = { periodName = it },
                                shifts = shifts
                            )
                            1 -> StepCalendar(
                                startDate = startDate, onStartChange = { startDate = it },
                                endDate = endDate, onEndChange = { endDate = it },
                                isDatesValid = isDatesValid
                            )
                            2 -> StepHistory(
                                onAddHistory = { showHistoryScreen = true }
                            )
                        }
                    }
                }

                // Footer Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 40.dp, top = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Voltar")
                        }
                    }
                    
                    Button(
                        onClick = {
                            if (currentStep < 2) {
                                currentStep++
                            } else {
                                onComplete(name, course, shift, periodName, startDate, endDate)
                            }
                        },
                        modifier = Modifier.weight(if (currentStep > 0) 1.5f else 1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = when(currentStep) {
                            0 -> isStep0Valid
                            1 -> isDatesValid
                            else -> true
                        }
                    ) {
                        Text(
                            if (currentStep < 2) "Próximo" else "Começar minha jornada",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StepProfile(
    name: String, onNameChange: (String) -> Unit,
    course: String, onCourseChange: (String) -> Unit,
    shift: String, onShiftChange: (String) -> Unit,
    periodName: String, onPeriodChange: (String) -> Unit,
    shifts: List<String>
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Seu Perfil", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text("Conte-nos um pouco sobre você", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }

        OutlinedTextField(
            value = name, onValueChange = onNameChange,
            label = { Text("Seu nome completo") },
            placeholder = { Text("Como quer ser chamado?") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp)) }
        )

        OutlinedTextField(
            value = course, onValueChange = onCourseChange,
            label = { Text("Nome da Faculdade / Curso") },
            placeholder = { Text("Ex: Engenharia de Software - USP") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.School, null, modifier = Modifier.size(20.dp)) }
        )

        OutlinedTextField(
            value = periodName, onValueChange = onPeriodChange,
            label = { Text("Nome do Semestre / Período") },
            placeholder = { Text("Ex: 1º Semestre 2024") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Turno das aulas", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                shifts.forEach { s ->
                    val isSelected = shift == s
                    val (icon, label) = when (s) {
                        "Manhã" -> Icons.Default.WbSunny to "Manhã"
                        "Tarde" -> Icons.Default.CloudQueue to "Tarde"
                        else -> Icons.Default.NightsStay to "Noite"
                    }
                    
                    Surface(
                        modifier = Modifier.weight(1f).height(70.dp).clickable { onShiftChange(s) },
                        shape = RoundedCornerShape(16.dp),
                        border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) else null,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        tonalElevation = if (isSelected) 0.dp else 2.dp
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(
                                icon, null, 
                                modifier = Modifier.size(20.dp),
                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                label, 
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepCalendar(
    startDate: LocalDate, onStartChange: (LocalDate) -> Unit,
    endDate: LocalDate, onEndChange: (LocalDate) -> Unit,
    isDatesValid: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.secondary)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Seu Calendário", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text("Quando começam e terminam as aulas?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }

        DatePickerField(label = "Início das Aulas", date = startDate, modifier = Modifier.fillMaxWidth()) { onStartChange(it) }
        DatePickerField(label = "Fim das Aulas", date = endDate, modifier = Modifier.fillMaxWidth()) { onEndChange(it) }
        
        if (!isDatesValid) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("A data de fim não pode ser anterior ao início.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lightbulb, null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(16.dp))
                Text("Isso ajuda a IA a organizar suas revisões semanais.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
fun StepHistory(onAddHistory: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.History, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.tertiary)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Seu Histórico", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text("Opcional: Importe notas de semestres passados", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(32.dp))
                Text(
                    "Deseja calcular seu CR Geral automaticamente?",
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Ao adicionar seu histórico, o College Admin analisa seu desempenho total desde o início do curso.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Button(
                    onClick = onAddHistory,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Adicionar Matérias")
                }
            }
        }
        
        Text(
            "Você também pode pular esta etapa e adicionar depois nas configurações de perfil.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryEntryScreen(
    viewModel: CollegeViewModel,
    onBack: () -> Unit
) {
    var subjectName by remember { mutableStateOf("") }
    var professor by remember { mutableStateOf("") }
    var period by remember { mutableStateOf("") }
    var averageGrade by remember { mutableStateOf("") }
    var absences by remember { mutableStateOf("0") }
    
    var isAiLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val assistant = remember { AiAssistant() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isAiLoading = true
            scope.launch {
                try {
                    val bitmap = if (Build.VERSION.SDK_INT < 28) {
                        MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                    } else {
                        val source = ImageDecoder.createSource(context.contentResolver, it)
                        ImageDecoder.decodeBitmap(source)
                    }
                    
                    val jsonResult = assistant.extractHistoryFromImage(bitmap)
                    val type = object : TypeToken<List<HistorySubjectResult>>() {}.type
                    val extracted: List<HistorySubjectResult> = Gson().fromJson(jsonResult, type)
                    
                    extracted.forEach { result ->
                        val finalAvg = result.pf ?: listOfNotNull(result.p1, result.p2).average().takeIf { !it.isNaN() }
                        
                        viewModel.addHistoricalSubject(
                            result.name, 
                            result.professor, 
                            period.ifBlank { "Importado" },
                            result.p1, result.p2, finalAvg, result.absences
                        )
                    }
                } catch (e: Exception) {
                    Log.e("IA", "Erro ao processar histórico", e)
                } finally {
                    isAiLoading = false
                }
            }
        }
    }

    val allSubjects by viewModel.subjects.collectAsState()
    val subjectsInThisPeriod = allSubjects.filter { it.period == period && period.isNotBlank() }
    val cr by viewModel.cr.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Adicionar Histórico", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    if (isAiLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { photoPickerLauncher.launch("image/*") }) {
                            Icon(Icons.Default.AutoAwesome, "Importar com IA", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { paddingValue ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValue)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = when {
                    cr >= 7.0 -> Color(0xFF10B981).copy(alpha = 0.1f)
                    cr >= 5.0 -> Color(0xFFF59E0B).copy(alpha = 0.1f)
                    else -> Color(0xFFEF4444).copy(alpha = 0.1f)
                },
                border = BorderStroke(1.dp, when {
                    cr >= 7.0 -> Color(0xFF10B981).copy(alpha = 0.3f)
                    cr >= 5.0 -> Color(0xFFF59E0B).copy(alpha = 0.3f)
                    else -> Color(0xFFEF4444).copy(alpha = 0.3f)
                })
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Seu CR Geral", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = String.format("%.2f", cr),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = when {
                                cr >= 7.0 -> Color(0xFF10B981)
                                cr >= 5.0 -> Color(0xFFF59E0B)
                                else -> Color(0xFFEF4444)
                            }
                        )
                    }
                    Icon(
                        Icons.Default.Analytics,
                        null,
                        modifier = Modifier.size(40.dp),
                        tint = when {
                            cr >= 7.0 -> Color(0xFF10B981)
                            cr >= 5.0 -> Color(0xFFF59E0B)
                            else -> Color(0xFFEF4444)
                        }.copy(alpha = 0.5f)
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Dica: Toque no ícone de IA no topo para importar várias matérias de uma vez tirando foto do seu boletim!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            OutlinedTextField(
                value = period,
                onValueChange = { period = it },
                label = { Text("Período (ex: 2023.2)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

            OutlinedTextField(
                value = subjectName,
                onValueChange = { subjectName = it },
                label = { Text("Nome da Disciplina") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = professor,
                onValueChange = { professor = it },
                label = { Text("Professor (Opcional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = averageGrade,
                    onValueChange = { averageGrade = it },
                    label = { Text("Média Final") },
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Icon(Icons.Default.Grade, null, modifier = Modifier.size(18.dp)) }
                )
                OutlinedTextField(
                    value = absences,
                    onValueChange = { absences = it },
                    label = { Text("Faltas") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Button(
                onClick = {
                    if (subjectName.isNotBlank() && period.isNotBlank()) {
                        val finalGrade = averageGrade.replace(",", ".").toDoubleOrNull()
                        viewModel.addHistoricalSubject(
                            subjectName, professor, period,
                            null, null, finalGrade,
                            absences.toIntOrNull() ?: 0
                        )
                        subjectName = ""
                        professor = ""
                        averageGrade = ""
                        absences = "0"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Adicionar Disciplina")
            }

            if (subjectsInThisPeriod.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Disciplinas já salvas em $period:", fontWeight = FontWeight.Bold)
                subjectsInThisPeriod.forEach { sub ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(sub.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text("Média: ${String.format("%.1f", sub.averageGrade)} | Faltas: ${sub.absences}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    date: LocalDate,
    modifier: Modifier = Modifier,
    onDateSelected: (LocalDate) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    OutlinedTextField(
        value = date.format(formatter),
        onValueChange = {},
        label = { Text(label) },
        modifier = modifier.clickable { showPicker = true },
        readOnly = true,
        enabled = false,
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        trailingIcon = { Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(18.dp)) }
    )

    if (showPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { 
                        onDateSelected(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()) 
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

data class HistorySubjectResult(
    val name: String,
    val professor: String,
    val p1: Double?,
    val p2: Double?,
    val pf: Double?,
    val absences: Int
)
