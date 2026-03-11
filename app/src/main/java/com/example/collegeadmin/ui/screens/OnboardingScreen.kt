package com.example.collegeadmin.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
    var showHistoryScreen by remember { mutableStateOf(false) }
    
    var name by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var shift by remember { mutableStateOf("Manhã") }
    var periodName by remember { mutableStateOf("2026.1") }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusMonths(5)) }

    val shifts = listOf("Manhã", "Tarde", "Noite")

    // Validação em tempo real
    val isDatesValid = !endDate.isBefore(startDate)
    val isFormValid = name.isNotBlank() && course.isNotBlank() && periodName.isNotBlank() && isDatesValid

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
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(Modifier.height(60.dp))

                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.School, 
                            null, 
                            modifier = Modifier.size(48.dp), 
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Seja bem-vindo", 
                        style = MaterialTheme.typography.headlineLarge, 
                        fontWeight = FontWeight.ExtraBold, 
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Vamos configurar seu perfil acadêmico", 
                        style = MaterialTheme.typography.bodyLarge, 
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "Preencha seus dados para que a IA possa personalizar seu cronograma e lembretes.", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Form Fields
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = name, 
                        onValueChange = { name = it }, 
                        label = { Text("Seu nome completo") }, 
                        placeholder = { Text("Como quer ser chamado?") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp)) }
                    )

                    OutlinedTextField(
                        value = course, 
                        onValueChange = { course = it }, 
                        label = { Text("Nome da Faculdade / Curso") }, 
                        placeholder = { Text("Ex: Engenharia de Software - USP") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = periodName,
                        onValueChange = { periodName = it },
                        label = { Text("Nome do Semestre / Período") },
                        placeholder = { Text("Ex: 1º Semestre 2024") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Turno das aulas", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            shifts.forEach { s ->
                                val isSelected = shift == s
                                Surface(
                                    modifier = Modifier.weight(1f).height(48.dp).clickable { shift = s },
                                    shape = RoundedCornerShape(14.dp),
                                    border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) else null,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(s, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Duração do Período Letivo", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            DatePickerField(label = "Início das Aulas", date = startDate, modifier = Modifier.weight(1f)) { startDate = it }
                            DatePickerField(label = "Fim das Aulas", date = endDate, modifier = Modifier.weight(1f)) { endDate = it }
                        }
                        if (!isDatesValid) {
                            Text(
                                "A data de fim não pode ser anterior ao início.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // History Section
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Já possui histórico?", fontWeight = FontWeight.Bold)
                            Text("Adicione matérias de períodos passados", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Button(
                            onClick = { showHistoryScreen = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Default.History, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Adicionar")
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (isFormValid) {
                            onComplete(name, course, shift, periodName, startDate, endDate)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(20.dp),
                    enabled = isFormValid
                ) {
                    Text("Começar minha jornada", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(Modifier.height(40.dp))
            }
        }
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
    var p1 by remember { mutableStateOf("") }
    var p2 by remember { mutableStateOf("") }
    var pf by remember { mutableStateOf("") }
    var absences by remember { mutableStateOf("0") }
    
    var isAiLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val apiKey = "AIzaSyAQMyzTBaQ8zBy7J_-sRF84zpbuPhZHyNU"
    val assistant = remember { AiAssistant(apiKey) }

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
                        viewModel.addHistoricalSubject(
                            result.name, 
                            result.professor, 
                            period.ifBlank { "Importado" },
                            result.p1, result.p2, result.pf, result.absences
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

    // Sincronização direta com o banco de dados via ViewModel
    val allSubjects by viewModel.subjects.collectAsState()
    val subjectsInThisPeriod = allSubjects.filter { it.period == period && period.isNotBlank() }

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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                label = { Text("Professor") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = p1,
                    onValueChange = { p1 = it },
                    label = { Text("Nota P1") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = p2,
                    onValueChange = { p2 = it },
                    label = { Text("Nota P2") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = pf,
                    onValueChange = { pf = it },
                    label = { Text("Nota Final") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
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
                        viewModel.addHistoricalSubject(
                            subjectName, professor, period,
                            p1.toDoubleOrNull(), p2.toDoubleOrNull(), pf.toDoubleOrNull(),
                            absences.toIntOrNull() ?: 0
                        )
                        subjectName = ""
                        professor = ""
                        p1 = ""
                        p2 = ""
                        pf = ""
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
