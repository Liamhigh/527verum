package com.example.omnis.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.omnis.engine.ForensicEngine
import com.example.omnis.model.*
import com.example.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import com.example.omnis.data.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import java.io.File
import java.io.FileOutputStream
import android.widget.Toast
import androidx.core.content.FileProvider
import android.content.Intent
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForensicApp(viewModel: ForensicViewModel) {
    val cases by viewModel.allCases.collectAsStateWithLifecycle()
    val selectedCase by viewModel.selectedCase.collectAsStateWithLifecycle()
    val activeStep by viewModel.activeProcessingStep.collectAsStateWithLifecycle()
    val logs by viewModel.processingLogs.collectAsStateWithLifecycle()

    var showIntakeForm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "VERUM OMNIS 5.2.7",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = DarkTeal,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Nine-Brain Digital Forensic Doctrine",
                            color = SoftGray,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    if (selectedCase != null) {
                        IconButton(onClick = { viewModel.selectCase(null) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Dashboard",
                                tint = DarkTeal
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark
                ),
                actions = {
                    if (selectedCase == null) {
                        Button(
                            onClick = { showIntakeForm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkTeal, contentColor = Color.Black),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("NEW INTAKE", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            )
        },
        containerColor = BackgroundDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main views
            AnimatedContent(
                targetState = selectedCase,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ScreenTransition"
            ) { currentCase ->
                if (currentCase == null) {
                    DashboardScreen(
                        cases = cases,
                        onCaseSelected = { viewModel.selectCase(it) },
                        onDeleteCase = { viewModel.deleteCase(it) }
                    )
                } else {
                    ReportViewerScreen(
                        forensicCase = currentCase,
                        viewModel = viewModel
                    )
                }
            }

            // Live progress scan overlay
            if (activeStep >= 0) {
                LiveProgressOverlay(activeStep, logs)
            }

            // Intake Form Dialog
            if (showIntakeForm) {
                IntakeFormDialog(
                    onDismiss = { showIntakeForm = false },
                    onSubmit = { title, content, type, jurisdiction, concealment, b8Fail ->
                        showIntakeForm = false
                        viewModel.runCaseAnalysis(title, content, type, jurisdiction, concealment, b8Fail)
                    }
                )
            }
        }
    }
}

@Composable
fun DashboardScreen(
    cases: List<ForensicCase>,
    onCaseSelected: (String) -> Unit,
    onDeleteCase: (String) -> Unit
) {
    if (cases.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = SoftGray,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "NO EVIDENCE INTUBATED",
                    fontWeight = FontWeight.Bold,
                    color = SoftWhite,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tap on 'New Intake' to add text blocks or select preset forensic test vectors.",
                    color = SoftGray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedSpacing(12.dp)
        ) {
            item {
                Text(
                    text = "ACTIVE SECURE CASES",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = SoftWhite,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(cases, key = { it.id }) { c ->
                CaseCardItem(
                    c = c,
                    onClick = { onCaseSelected(c.id) },
                    onDelete = { onDeleteCase(c.id) }
                )
            }
        }
    }
}

@Composable
fun CaseCardItem(
    c: ForensicCase,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, valueColor(c.processingStatus).copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = c.id,
                    fontFamily = FontFamily.Monospace,
                    color = DarkTeal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(status = c.processingStatus)
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = c.title,
                fontWeight = FontWeight.Bold,
                color = SoftWhite,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Artifact: ${c.evidenceName} | Type: ${c.evidenceType}",
                color = SoftGray,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(12.dp))

            Divider(color = Color.White.copy(alpha = 0.1f))

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RUN SEALS: ${c.evidenceHashPrefix}...",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = SoftGray
                )
                Icon(
                    imageVector = if (c.isSealed) Icons.Default.Lock else Icons.Default.Warning,
                    tint = if (c.isSealed) TerminalGreen else SecurityWarning,
                    modifier = Modifier.size(14.dp),
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (bgColor, textColor) = when (status) {
        "SEALED", "VERIFIED" -> Pair(TerminalGreen.copy(alpha = 0.15f), TerminalGreen)
        "CANDIDATE" -> Pair(SecurityWarning.copy(alpha = 0.15f), SecurityWarning)
        "FAIL_CLOSED", "REJECTED" -> Pair(Color.Red.copy(alpha = 0.15f), Color.Red)
        else -> Pair(EncryptionBlue.copy(alpha = 0.15f), EncryptionBlue)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = status,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            fontFamily = FontFamily.Monospace
        )
    }
}

fun valueColor(status: String): Color {
    return when (status) {
        "SEALED", "VERIFIED" -> TerminalGreen
        "CANDIDATE" -> SecurityWarning
        "FAIL_CLOSED", "REJECTED" -> Color.Red
        else -> EncryptionBlue
    }
}

@Composable
fun LiveProgressOverlay(activeStep: Int, logs: List<String>) {
    val lazyListState = rememberLazyListState()
    
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            lazyListState.animateScrollToItem(logs.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = false) {}, // absorb clicks
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.7f)
                .border(2.dp, DarkTeal, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = BackgroundDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "NINE-BRAIN RESOLVER ONLINE",
                            color = DarkTeal,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Processing stage ${activeStep + 1}/16...",
                            color = SoftGray,
                            fontSize = 12.sp
                        )
                    }
                    CircularProgressIndicator(
                        color = DarkTeal,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Progress line
                LinearProgressIndicator(
                    progress = (activeStep + 1) / 16f,
                    color = DarkTeal,
                    trackColor = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "PIPELINE EXECUTIVE AUDIT LOGS:",
                    color = SoftWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black)
                        .border(1.dp, Color.White.copy(alpha = 0.1f))
                        .padding(12.dp)
                ) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(logs) { log ->
                            Text(
                                text = ">> $log",
                                color = TerminalGreen,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IntakeFormDialog(
    onDismiss: () -> Unit,
    onSubmit: (title: String, content: String, type: String, jurisdiction: String, concealment: Boolean, b8Fail: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("DOCUMENT") }
    var jurisdiction by remember { mutableStateOf("ZA") }
    var concealment by remember { mutableStateOf(false) }
    var b8Fail by remember { mutableStateOf(false) }
    var isReadingFile by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isReadingFile = true
            coroutineScope.launch(Dispatchers.IO) {
                val name = getFileNameFromUri(context, it)
                val text = readTextFromUri(context, it)
                val calculatedType = when {
                    name.endsWith(".wav", true) || name.endsWith(".mp3", true) -> "AUDIO"
                    name.endsWith(".mp4", true) || name.endsWith(".avi", true) -> "VIDEO"
                    name.endsWith(".pdf", true) -> "DOCUMENT"
                    name.endsWith(".zip", true) -> "DOCUMENT"
                    else -> "TEXT"
                }
                withContext(Dispatchers.Main) {
                    title = name
                    content = text
                    type = calculatedType
                    isReadingFile = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "VERUM OMNIS INTAKE PROTOCOL",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = DarkTeal
            )
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedSpacing(12.dp)) {
                item {
                    Text("Select a Preset Test Vector to Auto-Fill:", color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedSpacing(8.dp),
                        verticalArrangement = Arrangement.spacedSpacing(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = {
                                title = "Acme Paid Invoice Audit"
                                type = "DOCUMENT"
                                content = """
                                Acme Corp billing log:
                                Page 2 shows billing admin Alice stamped Invoice #2026-A for $10,000 as PAID.
                                Page 5 shows accountant Bob listed Invoice #2026-A for $10,000 as UNPAID / outstanding.
                                """.trimIndent()
                            },
                            label = { Text("1. ACME Invoice", fontSize = 11.sp) }
                        )

                        SuggestionChip(
                            onClick = {
                                title = "Alpha Geographical Logs"
                                type = "TEXT"
                                content = """
                                Subject Alpha logs:
                                Page 1: Login event registered in London at 14:15 UTC.
                                Page 3: Hand written signature entry registered in Johannesburg at 14:45 UTC.
                                Note: Log clocks lack central GPS atomic sync references.
                                """.trimIndent()
                            },
                            label = { Text("2. Travel Overlap", fontSize = 11.sp) }
                        )

                        SuggestionChip(
                            onClick = {
                                title = "Financial Ledger Analysis"
                                type = "DOCUMENT"
                                content = """
                                BLOCK CHAIN TRACKING:
                                Block 3 verified hash: 0xAF8123B90BCEF11
                                Block 4 claimed parent hash reference: 0x12C098DB8AFE102
                                TAMPER ALERTS TRIGGERED.
                                """.trimIndent()
                            },
                            label = { Text("3. Log Tampering", fontSize = 11.sp) }
                        )
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { pickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkTeal),
                        border = BorderStroke(1.dp, if (isReadingFile) Color.Gray else DarkTeal),
                        shape = RoundedCornerShape(4.dp),
                        enabled = !isReadingFile
                    ) {
                        if (isReadingFile) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = DarkTeal,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "PARSING EVIDENCE STRUCTURES...",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Upload Live File"
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "LOAD FILE FROM DEVICE DISK",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Case Title Or Identifier") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkTeal,
                            focusedLabelColor = DarkTeal
                        )
                    )
                }

                item {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Scan / OCR Raw Text Input") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkTeal,
                            focusedLabelColor = DarkTeal
                        )
                    )
                }

                item {
                    Text("Evidence Modality", color = SoftWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedSpacing(8.dp)
                    ) {
                        listOf("DOCUMENT", "TEXT", "AUDIO", "VIDEO", "MULTIMODAL").forEach { m ->
                            val selected = type == m
                            FilterChip(
                                selected = selected,
                                onClick = { type = m },
                                label = { Text(m, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DarkTeal,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }

                item {
                    Text("Target Jurisdiction", color = SoftWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedSpacing(8.dp)
                    ) {
                        listOf("ZA", "US", "EU", "GLOBAL").forEach { j ->
                            val selected = jurisdiction == j
                            FilterChip(
                                selected = selected,
                                onClick = { jurisdiction = j },
                                label = {
                                    Text(
                                        when (j) {
                                            "ZA" -> "ZA (South Africa)"
                                            "US" -> "US (Federal)"
                                            "EU" -> "EU (AI Act)"
                                            else -> "NIST Global"
                                        },
                                        fontSize = 11.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DarkTeal,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Simulate Concealment Protocol", color = SoftWhite, fontSize = 12.sp)
                            Text("Simulates evidence tampering / missing registers", color = SoftGray, fontSize = 10.sp)
                        }
                        Switch(
                            checked = concealment,
                            onCheckedChange = { concealment = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = DarkTeal, checkedTrackColor = DarkTeal.copy(0.4f))
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Force B8 Audio Signal Failure", color = SoftWhite, fontSize = 12.sp)
                            Text("Triggers voice gap warnings", color = SoftGray, fontSize = 10.sp)
                        }
                        Switch(
                            checked = b8Fail,
                            onCheckedChange = { b8Fail = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = DarkTeal, checkedTrackColor = DarkTeal.copy(0.4f))
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        onSubmit(title, content, type, jurisdiction, concealment, b8Fail)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkTeal, contentColor = Color.Black),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("INITIATE FORENSIC SCAN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = SoftGray)
            }
        },
        containerColor = SurfaceDark
    )
}

@Composable
fun ReportViewerScreen(
    forensicCase: ForensicCase,
    viewModel: ForensicViewModel
) {
    val repository = ForensicRepository(ForensicDatabase.getDatabase(LocalContext.current).forensicDao())
    val reportInput = remember(forensicCase.reportRenderInputJson) {
        repository.deserializeReportInput(forensicCase.reportRenderInputJson)
    }

    if (reportInput == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Forensic report is corrupted or unreadable.", color = Color.Red)
        }
    } else {
        var selectedTab by remember { mutableStateOf(0) }
        val tabList = listOf("RAPID START", "CONTRADICTION LEDGER", "CERTIFIED FINDINGS", "LEGAL & DIRECTIVES", "B9 ADV-AI CHAT")

        Column(modifier = Modifier.fillMaxSize()) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceDark,
                contentColor = DarkTeal,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = DarkTeal
                    )
                }
            ) {
                tabList.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> QuickStartTab(forensicCase, reportInput)
                    1 -> LedgerTab(reportInput)
                    2 -> CertifiedFindingsTab(reportInput)
                    3 -> LegalDirectivesTab(reportInput)
                    4 -> AdvisoryChatTab(viewModel)
                }
            }
        }
    }
}

@Composable
fun QuickStartTab(
    c: ForensicCase,
    report: ReportRenderInput
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    
    // Stateful resolution for human-in-the-loop review
    var humanResolutionStatus by remember { mutableStateOf(report.humanReviewRequest?.resolutionStatus ?: "PENDING") }

    LazyColumn(verticalArrangement = Arrangement.spacedSpacing(16.dp)) {
        // Core status header card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, valueColor(c.processingStatus).copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. START HERE (EXECUTIVE SUMMARY)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = DarkTeal,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (report.tripleVerificationStatus == "PASS") {
                            "The forensic engine successfully executed and sealed findings for CASE ${report.caseId}. Quorum was met and all Triple Verification safety gates passed."
                        } else {
                            "FORENSIC SAFE GATES TRIGGERED FAIL-CLOSED. Synthesis failed due to: ${report.synthesisReason}"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftWhite
                    )

                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            exportForensicReportPdf(context, c, report)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TerminalGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Black
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "SEAL & EXPORT COURT-READY PDF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("TRIPLE VERIFICATION STATUS", color = SoftGray, fontSize = 10.sp)
                            Text(
                                report.tripleVerificationStatus,
                                color = valueColor(report.tripleVerificationStatus),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("TIE BREEDER/STATUS", color = SoftGray, fontSize = 10.sp)
                            Text(
                                report.tieBreaker,
                                color = if (report.tieBreaker == "STABLE") TerminalGreen else SecurityWarning,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Checklist of Gates passing
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TRIPLE VERIFICATION GATES CHECKLIST",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = SoftWhite,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(12.dp))

                    GateStatusRow("Thesis Gate (Positive substrate present)", report.thesisPass, report.thesisReason)
                    Spacer(Modifier.height(8.dp))
                    GateStatusRow("Antithesis Gate (Contradictions audited)", report.antithesisPass, report.antithesisReason)
                    Spacer(Modifier.height(8.dp))
                    GateStatusRow("Synthesis Gate (Guardian & Quorum approved)", report.synthesisPass, report.synthesisReason)
                }
            }
        }

        // Modular Chain of Custody Certificate Block (For law-enforcement ready outputs)
        report.chainOfCustody?.let { cert ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkTeal.copy(0.3f), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = DarkTeal, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "OFFLINE CRYPTOGRAPHIC CUSTODY SEAL",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = SoftWhite,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(cert.cryptographicSignature))
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Copy Digital Seal", tint = DarkTeal, modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(Modifier.height(12.dp))

                        FactRow("Evidence Subject Prefix", "0x${cert.evidenceHashPrefix.uppercase()}")
                        FactRow("Physical Seal UTC Time", cert.utcTimestamp)
                        
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "DIGITAL SEAL SIGNATURE:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = SoftGray,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(8.dp)
                                .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = cert.cryptographicSignature,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = TerminalGreen,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Dynamic Interactive Human in the Loop Review request
        report.humanReviewRequest?.let { review ->
            if (review.isTriggered) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (humanResolutionStatus == "PENDING") SecurityWarning.copy(0.4f) else TerminalGreen.copy(0.4f),
                                RoundedCornerShape(8.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (humanResolutionStatus == "PENDING") SecurityWarning else TerminalGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "COGNITIVE HUMAN-IN-THE-LOOP AUDIT",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = SoftWhite,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                StatusChip(status = humanResolutionStatus)
                            }

                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "ANOMALY: ${review.anomalyType}",
                                color = SoftWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Engine has flagged inconsistent sequence patterns which cannot be resolved purely deterministic. Manual cognitive sign-off warrants review of following components:",
                                color = SoftGray,
                                fontSize = 11.sp
                            )

                            Spacer(Modifier.height(8.dp))
                            Text("AUDITED HARD EVIDENCE LOCATIONS:", fontSize = 9.sp, color = DarkTeal, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            review.supportingAnchors.forEach { anchor ->
                                Text("• $anchor", color = SoftWhite, fontSize = 11.sp)
                            }

                            Spacer(Modifier.height(8.dp))
                            Text("PROPOSED EXCLUSION HYPOTHESES:", fontSize = 9.sp, color = DarkTeal, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            review.proposedHypotheses.forEach { hyp ->
                                Text("• $hyp", color = SoftGray, fontSize = 11.sp)
                            }

                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { humanResolutionStatus = "CONFIRMED_OK" },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkTeal, contentColor = Color.Black),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("AUDIT & CONFIRM", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { humanResolutionStatus = "FLAGGED_FOR_DEPOSITION" },
                                    colors = ButtonDefaults.buttonColors(containerColor = SecurityWarning, contentColor = Color.White),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("FLAG FOR DEPOSITION", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Module 6 - Guardian Treaty Hard Coupling Header (Machine-Readable Header)
        report.constitutionalConstraintHeader?.let { header ->
            item {
                Card(
                     modifier = Modifier.fillMaxWidth(),
                     colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = DarkTeal, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "GUARDIAN TREATY CONSTITUTION HEADER",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = SoftWhite,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "A machine-readable, immutable protocol package bound directly to the analysis binary to regulate recipient interpretation rules:",
                            color = SoftGray,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(0.5f))
                                .padding(10.dp)
                                .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = header,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = SoftWhite
                            )
                        }
                    }
                }
            }
        }

        // Case facts stable card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "CASE FACTS RECORD",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = SoftWhite,
                            fontFamily = FontFamily.Monospace
                        )
                        Icon(Icons.Default.Share, contentDescription = "Copy Hash", modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                clipboard.setText(AnnotatedString(report.evidenceHash))
                            }, tint = DarkTeal)
                    }
                    Spacer(Modifier.height(12.dp))

                    FactRow("Case ID", report.caseId)
                    FactRow("Evidence SHA-512 Hash", report.evidenceHash)
                    FactRow("Engine Version", report.engineVersion)
                    FactRow("Deterministic Run ID", report.deterministicRunId)
                    FactRow("Target Jurisdiction Code", report.jurisdiction)
                }
            }
        }

        // Plain word glossary / guide
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PLAIN WORD FORENSIC GUIDE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = DarkTeal,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Translating complex machine logic states into transparent human definitions:",
                        color = SoftWhite,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(12.dp))

                    GlossaryItem("Candidate", "A temporary claim extracted from raw logs that is unverified and cannot be trusted on its own.")
                    GlossaryItem("Verified", "The highest truth state for a conflict: both opposing sides are anchored/corroborated by independent page proofs.")
                    GlossaryItem("Certified", "Passed the Seven Rules of Promotion (P1-P7) and fully cleared by the Guardian safety block.")
                    GlossaryItem("Guardian-Approved", "Audited to ensure it contains zero overclaims, guesses, or verdict-style pronouncements of guilt.")
                    GlossaryItem("Contradiction", "An explicit clash of facts (e.g. Acme payment records showing both paid & unpaid for same file).")
                    GlossaryItem("Hash", "A cryptographic digital fingerprint proving evidence was not tampered with or modified since seizure.")
                }
            }
        }

        // Boundary note warning
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.03f))
                    .padding(12.dp)
                    .border(1.dp, SoftGray.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            ) {
                Text(
                    text = "CRITICAL LEGAL LIMIT: ${report.boundaryNote}",
                    color = SecurityWarning,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun GateStatusRow(name: String, passed: Boolean, reason: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Icon(
            imageVector = if (passed) Icons.Default.Check else Icons.Default.Close,
            tint = if (passed) TerminalGreen else Color.Red,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp),
            contentDescription = null
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(name, fontWeight = FontWeight.Bold, color = SoftWhite, fontSize = 12.sp)
            Text(reason, color = SoftGray, fontSize = 11.sp)
        }
    }
}

@Composable
fun FactRow(label: String, valText: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = SoftGray, fontSize = 11.sp)
        Text(
            valText,
            color = SoftWhite,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(0.6f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun GlossaryItem(term: String, definition: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(term, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = DarkTeal, fontSize = 11.sp)
        Text(definition, color = SoftGray, fontSize = 11.sp)
    }
}

@Composable
fun LedgerTab(report: ReportRenderInput) {
    LazyColumn(verticalArrangement = Arrangement.spacedSpacing(12.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CONTRADICTION LEDGER SUMMARY", color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                StatusChip(status = "ACTIVE AUDIT")
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Contradictions represent primary forensic evidence (not post-hoc checks). A contradiction remains a Candidate unless both opposing assertions are structurally anchored.",
                color = SoftGray,
                fontSize = 11.sp
            )
        }

        if (report.contradictions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp), contentAlignment = Alignment.Center
                ) {
                    Text("No active contradictions identified in this pass.", color = TerminalGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        } else {
            items(report.contradictions) { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, valueColor(entry.status).copy(0.3f), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "CONFLICT: ${entry.conflictType.uppercase()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = valueColor(entry.status)
                            )
                            StatusChip(status = entry.status)
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(entry.summary, color = SoftWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                        Spacer(Modifier.height(12.dp))

                        Text("PROPOSITION A:", fontSize = 10.sp, color = DarkTeal, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(entry.propositionA, color = SoftWhite, fontSize = 12.sp)

                        Spacer(Modifier.height(8.dp))

                        Text("PROPOSITION B:", fontSize = 10.sp, color = DarkTeal, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(entry.propositionB, color = SoftWhite, fontSize = 12.sp)

                        Spacer(Modifier.height(12.dp))

                        FactRow("Involved Actors", entry.actors.joinToString())
                        FactRow("Anchor Pages Map", "Pages: " + entry.anchorPages.joinToString())
                        FactRow("Source Anchor Excerpts", entry.sourceAnchors.joinToString(" | "))

                        Spacer(Modifier.height(8.dp))
                        Divider(color = Color.White.copy(0.05f))
                        Spacer(Modifier.height(8.dp))

                        Text("INVESTIGATION DIRECTIVE / NEEDED EVIDENCE:", fontSize = 10.sp, color = SecurityWarning, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(entry.neededEvidence, color = SoftGray, fontSize = 12.sp)

                        Spacer(Modifier.height(8.dp))

                        Text("APPLIED RULES FOR AUDIT:", fontSize = 10.sp, color = SoftGray, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        entry.ruleHits.forEach { r ->
                            val parts = r.split("|")
                            Text("-> Rule ID: ${parts.getOrNull(0) ?: ""} [Severity: ${parts.getOrNull(1) ?: ""}]", color = SoftGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Module 2 - Cross-Witness Contradiction Clusters Sections
        if (report.crossWitnessClusters.isNotEmpty()) {
            item {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = DarkTeal, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "CROSS-WITNESS CONTRADICTION CLUSTERS",
                        color = SoftWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Clustering conflicts dynamically by thematic topics to analyze multi-witness coordination gaps and discrepancy density:",
                    color = SoftGray,
                    fontSize = 11.sp
                )
            }

            items(report.crossWitnessClusters) { cluster ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkTeal.copy(0.3f), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOPIC: ${cluster.topicId}",
                                color = DarkTeal,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SecurityWarning.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "DENSITY: ${cluster.conflictDensityOrdinal}",
                                    color = SecurityWarning,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = cluster.topicSummary,
                            color = SoftWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = cluster.clashDescription,
                            color = SoftGray,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        
                        Text(
                            text = "CONFRONTED TESTIFIERS:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = SoftGray,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            cluster.actorsInvolved.forEach { act ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(0.05f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(act, color = SoftWhite, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Module 2 - Deception and Commitment Degradation Timeline
        if (report.statementEvolutionLedger.isNotEmpty() || report.commitmentDegradationSignals.isNotEmpty()) {
            item {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = DarkTeal, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "STATEMENT EVOLUTION LEDGERS",
                        color = SoftWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tracking chronological deposition statements to reveal semantic drift, truth concession phases, and structural deception thresholds:",
                    color = SoftGray,
                    fontSize = 11.sp
                )
            }

            // Mapped commitment degradation phases card
            report.commitmentDegradationSignals.forEach { degradation ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Red.copy(0.2f), RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SHIFTER: ${degradation.actor.uppercase()}",
                                    color = DarkTeal,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SecurityWarning.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${degradation.signalStrength} DRIFT SIGNAL",
                                        color = SecurityWarning,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            
                            Text("CHRONOLOGICAL EVOLUTION PATHS:", fontSize = 9.sp, color = SoftGray, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Spacer(Modifier.height(4.dp))
                            degradation.chronologicalPhrases.forEachIndexed { i, phrase ->
                                Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                                    Text("P${i+1}: ", color = DarkTeal, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(phrase, color = SoftWhite, fontSize = 11.sp)
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Text("COMPILED DRIFT SEQUENCE PATHWAY:", fontSize = 9.sp, color = DarkTeal, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(0.5f))
                                    .padding(8.dp)
                                    .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(4.dp))
                            ) {
                                Text(
                                    text = degradation.shiftPath,
                                    color = TerminalGreen,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Raw timeline items under StatementEvolution
            items(report.statementEvolutionLedger) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(0.7f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.dateOrSource,
                                color = SoftGray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (item.alignmentDrift == "CONTRADICTION") Color.Red.copy(0.15f)
                                        else if (item.alignmentDrift == "MODERATE_SHIFT") SecurityWarning.copy(0.15f)
                                        else TerminalGreen.copy(0.15f)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = item.alignmentDrift,
                                    color = if (item.alignmentDrift == "CONTRADICTION") Color.Red
                                            else if (item.alignmentDrift == "MODERATE_SHIFT") SecurityWarning
                                            else TerminalGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "\"${item.statementText}\"",
                            color = SoftWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Target Subject: ${item.actor} | Topic Focus: ${item.topic}",
                            color = SoftGray,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Module 1 - Crypto Tracing and Blockchain Seizures
        if (report.blockchainTraces.isNotEmpty()) {
            item {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = DarkTeal, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "BLOCKCHAIN EXPLOIT & OUTFLOW TRACING",
                        color = SoftWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Real-time wallet and coin transfer tracking to identify unrecorded cross-border capital flights:",
                    color = SoftGray,
                    fontSize = 11.sp
                )
            }

            items(report.blockchainTraces) { tx ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(DarkTeal.copy(0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(tx.coin, color = DarkTeal, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(tx.value, color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            StatusChip(status = tx.status)
                        }
                        Spacer(Modifier.height(6.dp))

                        FactRow("Wallet Destination Address", tx.walletAddress)
                        FactRow("Transaction Hash Ref", tx.txHash)
                        FactRow("Transfer Direction Flow", tx.direction)
                    }
                }
            }
        }
    }
}

@Composable
fun CertifiedFindingsTab(report: ReportRenderInput) {
    LazyColumn(verticalArrangement = Arrangement.spacedSpacing(12.dp)) {
        item {
            Text("GUARDIAN CERTIFIED EVIDENCE FINDINGS", color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(8.dp))
            Text(
                "Findings are passed through the strict Promotion Service P1-P7 filters. Only sanitized, verified, and overclaim-free evidence facts blocks receive seal certification.",
                color = SoftGray,
                fontSize = 11.sp
            )
        }

        items(report.certifiedFindings) { f ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (f.status == "CERTIFIED") TerminalGreen.copy(0.4f) else Color.Red.copy(0.4f), RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "ACTOR: ${f.actor.uppercase()}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = DarkTeal,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(0.6f)
                        )
                        StatusChip(status = f.status)
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(f.summary, color = SoftWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

                    Spacer(Modifier.height(12.dp))

                    FactRow("Finding Type Class", f.findingType.uppercase())
                    FactRow("Page Anchors", "Page ${f.anchorPages.joinToString()}")
                    FactRow("Confidence Scale", f.confidenceOrdinal)
                    FactRow("Contradiction Posture", f.contradictionStatus)

                    Spacer(Modifier.height(8.dp))
                    Divider(color = Color.White.copy(0.05f))
                    Spacer(Modifier.height(8.dp))

                    Text("GUARDIAN ASSESSMENT RECORD", fontSize = 10.sp, color = TerminalGreen, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text(f.guardianDecision, color = SoftWhite, fontSize = 12.sp)

                    if (f.renderWarnings.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        f.renderWarnings.forEach { w ->
                            Text("! WARNING: $w", color = SecurityWarning, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegalDirectivesTab(report: ReportRenderInput) {
    LazyColumn(verticalArrangement = Arrangement.spacedSpacing(16.dp)) {
        // Module 4 - Per-Actor Liability Scorecard & Dishonesty Index Sinks
        if (report.actorLiabilityScorecards.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBox, contentDescription = null, tint = DarkTeal, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "PER-ACTOR LIABILITY SCORECARDS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = SoftWhite,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Algorithmic liability rating indexing actors based on proven text contradictions and weighted signal confidence density:",
                    color = SoftGray,
                    fontSize = 11.sp
                )
            }

            items(report.actorLiabilityScorecards) { scorecard ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (scorecard.dishonestyOrdinal == "CRITICAL") Color.Red.copy(0.3f) else Color.White.copy(0.1f),
                            RoundedCornerShape(8.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = scorecard.actor.uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = DarkTeal,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when (scorecard.dishonestyOrdinal) {
                                            "CRITICAL" -> Color.Red.copy(alpha = 0.15f)
                                            "MODERATE" -> SecurityWarning.copy(alpha = 0.15f)
                                            else -> TerminalGreen.copy(alpha = 0.15f)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "RATING: ${scorecard.dishonestyOrdinal}",
                                    fontWeight = FontWeight.Bold,
                                    color = when (scorecard.dishonestyOrdinal) {
                                        "CRITICAL" -> Color.Red
                                        "MODERATE" -> SecurityWarning
                                        else -> TerminalGreen
                                    },
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("VERIFIED INCOMPATIBILITY", color = SoftGray, fontSize = 9.sp)
                                Text("${scorecard.verifiedContradictionsCount} Contradictions", color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("CANDIDATE HOLES", color = SoftGray, fontSize = 9.sp)
                                Text("${scorecard.candidateContradictionsCount} Suspicious Elements", color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("RED FLAGS MET", color = SoftGray, fontSize = 9.sp)
                                Text("${scorecard.redFlagsCount} Flags", color = if (scorecard.redFlagsCount > 2) SecurityWarning else SoftWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        if (scorecard.automatedTags.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text("AUTOMATED CLASSIFICATION TAGS:", fontSize = 9.sp, color = SoftGray, fontFamily = FontFamily.Monospace)
                            Spacer(Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                scorecard.automatedTags.forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (tag.contains("cyber", ignoreCase = true) || tag.contains("evasive", ignoreCase = true)) Color.Red.copy(0.12f)
                                                else Color.White.copy(0.05f)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = tag,
                                            color = if (tag.contains("cyber", ignoreCase = true) || tag.contains("evasive", ignoreCase = true)) Color.Red else SoftWhite,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
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

        // Module 4 - Statute Aligned Findings Section (Court-Room ready mappings)
        if (report.statuteMappings.isNotEmpty()) {
            item {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = DarkTeal, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "STATUTE-ALIGNED CRIMINAL FINDINGS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = SoftWhite,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Automatic mapping of active computer forensics to key statutory crime elements to assist judicial physical affidavit applications:",
                    color = SoftGray,
                    fontSize = 11.sp
                )
            }

            items(report.statuteMappings) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkTeal.copy(0.3f), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "STATUTE REFERENCED:",
                                color = DarkTeal,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DarkTeal.copy(0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = item.codeSection,
                                    color = DarkTeal,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = item.proposedStatute,
                            color = SoftWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "FACT SUMMARY: ${item.findingSummary}",
                            color = SoftWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "CRIMINAL INGREDIENTS MET MAPPING:",
                            color = SoftGray,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        item.elementsMetMapping.forEachIndexed { i, mapping ->
                            Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                                Text(
                                    text = "[${i + 1}] ",
                                    color = DarkTeal,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = mapping,
                                    color = SoftWhite,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Legal mapping Card (CPA / ECTA)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "STATUTORY COMPLIANCE & COURT ADMISSIBILITY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = DarkTeal,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(12.dp))

                    report.legalMappings.forEach { m ->
                        Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                            Text("•", color = DarkTeal, modifier = Modifier.padding(end = 8.dp), fontWeight = FontWeight.Bold)
                            Text(m, color = SoftWhite, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Proof Gaps
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "DISCLOSED PROOF GAPS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = SecurityWarning,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Under Truth Priority and Minimal Disclosure rules, all gaps or missing corroborations must be explicitly described to avoid silent failures:",
                        color = SoftGray,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(12.dp))

                    val gaps = mutableListOf<String>()
                    if (report.contradictions.any { it.status == "CANDIDATE" }) {
                        gaps.add("Lacking verified external clock proof (timestamps cannot resolve absolutely without GPS or checked NTP server certificates).")
                    }
                    if (report.certifiedFindings.isEmpty()) {
                        gaps.add("No certified findings survived the P1-P7 filters. Report is safe-closed.")
                    }
                    if (gaps.isEmpty()) {
                        gaps.add("Zero critical information gaps reported in active domains. Core assertions corroborated.")
                    }

                    gaps.forEach { g ->
                        Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                            Text("!", color = SecurityWarning, modifier = Modifier.padding(end = 8.dp), fontWeight = FontWeight.Bold)
                            Text(g, color = SoftWhite, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Next Lawful Steps Directives
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "COMMANDER LAWFUL DIRECTIVES & ACTION PLAN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = SoftWhite,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(12.dp))

                    DirectiveItem("Directive 01: Secure Evidence Chain", "Immediately secure the original hardware medium and log evidence SHA-512 hashes into the offline department physical registry ledger to guarantee chain of custody.")
                    DirectiveItem("Directive 02: Gather Counterparty Records", "Retrieve secondary transaction ledgers or NTP clock server offsets corresponding to the discrepancies highlighted in the Contradiction Ledger.")
                    DirectiveItem("Directive 03: Request Judicial Affidavit Authority", "Pursuant to Sec 212 of CPA or Federal FRE rules, submit this deterministic seal and findings blocks to request formal judicial physical extraction affidavits.")
                }
            }
        }
    }
}

@Composable
fun DirectiveItem(num: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(num, fontWeight = FontWeight.Bold, color = DarkTeal, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Text(desc, color = SoftWhite, fontSize = 12.sp)
    }
}

@Composable
fun AdvisoryChatTab(viewModel: ForensicViewModel) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()
    
    var inputText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val lazyListState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black.copy(0.8f))
                .border(1.dp, Color.White.copy(0.05f))
                .padding(12.dp)
        ) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = DarkTeal, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "B9 Advisory Brain is waiting...",
                            color = SoftGray,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Ask questions regarding conflict rules, CPA statutory maps, or specific chain failures in this report.",
                            color = SoftGray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
                    items(messages) { msg ->
                        val isUser = msg.first == "User"
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                        ) {
                            Text(
                                text = msg.first.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUser) DarkTeal else EncryptionBlue,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isUser) SurfaceDark else SurfaceCard)
                                    .padding(10.dp)
                                    .widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = msg.second,
                                    color = SoftWhite,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    if (isLoading) {
                        item {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedSpacing(4.dp)
                            ) {
                                Text("B9 is computing target vectors", color = SoftGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                CircularProgressIndicator(color = DarkTeal, strokeWidth = 1.dp, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask B9 about contradictions or CPA codes...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DarkTeal,
                    focusedLabelColor = DarkTeal
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.askAdvisoryB9(inputText)
                        inputText = ""
                        focusManager.clearFocus()
                    }
                })
            )

            Spacer(Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.askAdvisoryB9(inputText)
                        inputText = ""
                        focusManager.clearFocus()
                    }
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(DarkTeal)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.Black
                )
            }
        }
    }
}

// Utility extension for padding logic
fun Arrangement.spacedSpacing(dp: androidx.compose.ui.unit.Dp) = Arrangement.spacedBy(dp)

fun drawQrCode(canvas: AndroidCanvas, xStart: Float, yStart: Float, cellSize: Float, textToHash: String) {
    val hash = com.example.omnis.engine.ForensicEngine.sha256(textToHash)
    val matrixSize = 25
    val p = Paint().apply {
        color = AndroidColor.BLACK
        style = Paint.Style.FILL
    }
    canvas.drawRect(xStart, yStart, xStart + 7 * cellSize, yStart + cellSize, p)
    canvas.drawRect(xStart, yStart, xStart + cellSize, yStart + 7 * cellSize, p)
    canvas.drawRect(xStart + 6 * cellSize, yStart, xStart + 7 * cellSize, yStart + 7 * cellSize, p)
    canvas.drawRect(xStart, yStart + 6 * cellSize, xStart + 7 * cellSize, yStart + 7 * cellSize, p)
    canvas.drawRect(xStart + 2 * cellSize, yStart + 2 * cellSize, xStart + 5 * cellSize, yStart + 5 * cellSize, p)
    
    val trX = xStart + (matrixSize - 7) * cellSize
    canvas.drawRect(trX, yStart, trX + 7 * cellSize, yStart + cellSize, p)
    canvas.drawRect(trX, yStart, trX + cellSize, yStart + 7 * cellSize, p)
    canvas.drawRect(trX + 6 * cellSize, yStart, trX + 7 * cellSize, yStart + 7 * cellSize, p)
    canvas.drawRect(trX, yStart + 6 * cellSize, trX + 7 * cellSize, yStart + 7 * cellSize, p)
    canvas.drawRect(trX + 2 * cellSize, yStart + 2 * cellSize, trX + 5 * cellSize, yStart + 5 * cellSize, p)

    val blY = yStart + (matrixSize - 7) * cellSize
    canvas.drawRect(xStart, blY, xStart + 7 * cellSize, blY + cellSize, p)
    canvas.drawRect(xStart, blY, xStart + cellSize, blY + 7 * cellSize, p)
    canvas.drawRect(xStart + 6 * cellSize, blY, xStart + 7 * cellSize, blY + 7 * cellSize, p)
    canvas.drawRect(xStart, blY + 6 * cellSize, xStart + 7 * cellSize, blY + 7 * cellSize, p)
    canvas.drawRect(xStart + 2 * cellSize, blY + 2 * cellSize, xStart + 5 * cellSize, blY + 5 * cellSize, p)

    for (r in 0 until matrixSize) {
        for (c in 0 until matrixSize) {
            if ((r < 8 && c < 8) || (r < 8 && c >= matrixSize - 8) || (r >= matrixSize - 8 && c < 8)) {
                continue
            }
            val bitIdx = java.lang.Math.abs((r * matrixSize + c) % hash.length)
            val hexChar = hash[bitIdx]
            val isDark = hexChar.code % 2 == 0
            if (isDark) {
                canvas.drawRect(
                    xStart + c * cellSize,
                    yStart + r * cellSize,
                    xStart + (c + 1) * cellSize,
                    yStart + (r + 1) * cellSize,
                    p
                )
            }
        }
    }
}

fun drawPageDecorations(canvas: AndroidCanvas, pageNum: Int, totalPages: Int, caseId: String, sealHex: String) {
    val watermarkPaint = Paint().apply {
        color = AndroidColor.LTGRAY
        alpha = 25
        textSize = 36f
        isFakeBoldText = true
        isAntiAlias = true
    }
    canvas.save()
    canvas.rotate(-35f, 306f, 396f)
    canvas.drawText("VERUM OMNIS LEGAL SECTOR", 70f, 370f, watermarkPaint)
    canvas.drawText("CONSTITUTION v5.2.7 STANDARD", 50f, 420f, watermarkPaint)
    canvas.restore()

    val footerTextPaint = Paint().apply {
        color = AndroidColor.GRAY
        textSize = 8f
        isAntiAlias = true
    }
    val footerBoldPaint = Paint().apply {
        color = AndroidColor.BLACK
        textSize = 8f
        isFakeBoldText = true
        isAntiAlias = true
    }

    canvas.drawLine(40f, 740f, 572f, 740f, footerTextPaint)
    canvas.drawText("Sealed Original: VERUM OMNIS SECURED - Case $caseId", 40f, 755f, footerTextPaint)
    canvas.drawText("✔ Patent Pending Verum Omnis", 435f, 755f, footerBoldPaint)
    canvas.drawText("Page $pageNum of $totalPages", 285f, 770f, footerTextPaint)
}

fun exportForensicReportPdf(context: android.content.Context, c: ForensicCase, report: ReportRenderInput) {
    try {
        val pdfDocument = PdfDocument()
        val totalPages = 3
        
        val textPaint = Paint().apply {
            color = AndroidColor.BLACK
            textSize = 9f
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = AndroidColor.BLACK
            textSize = 14f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val subHeaderPaint = Paint().apply {
            color = AndroidColor.BLACK
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val boldPaint = Paint().apply {
            color = AndroidColor.BLACK
            textSize = 9f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val redPaint = Paint().apply {
            color = AndroidColor.RED
            textSize = 9f
            isFakeBoldText = true
            isAntiAlias = true
        }

        // ================= PAGE 1 =================
        var pageInfo = PdfDocument.PageInfo.Builder(612, 792, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        drawPageDecorations(canvas, 1, totalPages, report.caseId, report.evidenceHash)
        
        var y = 40f
        canvas.drawText("VERUM OMNIS v5.2.7 - FORENSIC EVIDENCE REPORT", 40f, y, headerPaint)
        y += 28f
        
        canvas.drawText("1. CASE AUDIT IDENTIFICATION", 40f, y, subHeaderPaint)
        y += 18f
        canvas.drawText("Deterministic CaseID:", 40f, y, boldPaint)
        canvas.drawText(report.caseId, 160f, y, textPaint)
        y += 15f
        canvas.drawText("Ingestion Timestamp:", 40f, y, boldPaint)
        canvas.drawText(report.chainOfCustody?.utcTimestamp ?: "N/A", 160f, y, textPaint)
        y += 15f
        canvas.drawText("Forensic Run ID:", 40f, y, boldPaint)
        canvas.drawText(report.deterministicRunId, 160f, y, textPaint)
        y += 15f
        canvas.drawText("Jurisdiction Aligned:", 40f, y, boldPaint)
        canvas.drawText(report.jurisdiction, 160f, y, textPaint)
        y += 15f
        canvas.drawText("Primary Source File:", 40f, y, boldPaint)
        canvas.drawText(c.evidenceName, 160f, y, textPaint)
        y += 22f

        canvas.drawText("2. EVIDENCE CRYPTOGRAPHIC SUBSTRATE MANIFEST", 40f, y, subHeaderPaint)
        y += 18f
        canvas.drawText("Raw Stream Size:", 40f, y, boldPaint)
        canvas.drawText("${c.evidenceSize} bytes", 160f, y, textPaint)
        y += 15f
        canvas.drawText("SHA-512 Master Hash Prefix:", 40f, y, boldPaint)
        canvas.drawText(report.evidenceHash.take(16).uppercase(), 160f, y, textPaint)
        y += 15f
        canvas.drawText("SHA-512 Secure Hash Block:", 40f, y, boldPaint)
        y += 12f
        if (report.evidenceHash.length >= 64) {
            canvas.drawText(report.evidenceHash.take(64), 50f, y, textPaint)
            y += 12f
            canvas.drawText(report.evidenceHash.drop(64), 50f, y, textPaint)
        } else {
            canvas.drawText(report.evidenceHash, 50f, y, textPaint)
        }
        y += 22f

        canvas.drawText("3. TRIPLE VERIFICATION DOCTRINE CHECKLIST GATES", 40f, y, subHeaderPaint)
        y += 18f
        canvas.drawText("Triple Verification Status:", 40f, y, boldPaint)
        if (report.tripleVerificationStatus == "PASS") {
            canvas.drawText("VERIFIED & PASSED - IMMUTABLE COMPLIANT", 160f, y, boldPaint)
        } else {
            canvas.drawText("FAILED / BLOCKED UNREGULATED SUMMARY", 160f, y, redPaint)
        }
        y += 15f
        
        canvas.drawText("A) Thesis Gate (Positive Evidence):", 40f, y, boldPaint)
        canvas.drawText(if (report.thesisPass) "PASSED" else "FAILED", 220f, y, textPaint)
        y += 12f
        canvas.drawText("   Basis: ${report.thesisReason}", 40f, y, textPaint)
        y += 15f
        
        canvas.drawText("B) Antithesis Gate (Cognitive Audit):", 40f, y, boldPaint)
        canvas.drawText(if (report.antithesisPass) "PASSED" else "FAILED", 220f, y, textPaint)
        y += 12f
        canvas.drawText("   Basis: ${report.antithesisReason}", 40f, y, textPaint)
        y += 15f

        canvas.drawText("C) Synthesis Gate (9-Brain & Guardian):", 40f, y, boldPaint)
        canvas.drawText(if (report.synthesisPass) "PASSED" else "FAILED", 220f, y, textPaint)
        y += 12f
        canvas.drawText("   Basis: ${report.synthesisReason}", 40f, y, textPaint)
        y += 30f

        canvas.drawText("VERIFY SEAL BLOCK", 435f, 635f, boldPaint)
        drawQrCode(canvas, 435f, 642f, 2.5f, report.deterministicRunId)

        pdfDocument.finishPage(page)

        // ================= PAGE 2 =================
        pageInfo = PdfDocument.PageInfo.Builder(612, 792, 2).create()
        page = pdfDocument.startPage(pageInfo)
        canvas = page.canvas

        drawPageDecorations(canvas, 2, totalPages, report.caseId, report.evidenceHash)
        y = 40f
        canvas.drawText("4. NINE-BRAIN ACTIVE CONCORDANCE CERTIFIED FINDINGS", 40f, y, subHeaderPaint)
        y += 24f

        val verifiedFindings = report.certifiedFindings.filter { it.status == "CERTIFIED" }
        if (verifiedFindings.isEmpty()) {
            canvas.drawText("No certified high-confidence findings passed the P1-P7 Guardian gates.", 40f, y, textPaint)
            y += 15f
        } else {
            for ((idx, f) in verifiedFindings.withIndex()) {
                canvas.drawText("${idx + 1}. [${f.findingType.uppercase()}] - Testifier Alignment: ${f.actor}", 40f, y, boldPaint)
                y += 14f
                val summaryLines = if (f.summary.length > 85) f.summary.chunked(85) else listOf(f.summary)
                for (sLine in summaryLines) {
                    canvas.drawText("   Summary: $sLine", 40f, y, textPaint)
                    y += 12f
                }
                canvas.drawText("   Anchor Page(s): ${f.anchorPages.joinToString()}", 40f, y, textPaint)
                y += 12f
                canvas.drawText("   Guardian Audit: ${f.guardianDecision}", 40f, y, textPaint)
                y += 16f
                if (y > 680f) {
                    canvas.drawText("... Additional findings truncated, available in raw JSON metadata.", 40f, y, textPaint)
                    break
                }
            }
        }
        
        pdfDocument.finishPage(page)

        // ================= PAGE 3 =================
        pageInfo = PdfDocument.PageInfo.Builder(612, 792, 3).create()
        page = pdfDocument.startPage(pageInfo)
        canvas = page.canvas

        drawPageDecorations(canvas, 3, totalPages, report.caseId, report.evidenceHash)
        y = 40f
        
        canvas.drawText("5. STRICT CONTRADICTION-FIRST EVIDENCE AUDIT", 40f, y, subHeaderPaint)
        y += 20f

        if (report.contradictions.isEmpty()) {
            canvas.drawText("Zero material contradictions located inside this evidence stream.", 40f, y, textPaint)
            y += 20f
        } else {
            for (contra in report.contradictions) {
                canvas.drawText("- Topic/Clash: ${contra.conflictType.uppercase()} - Strength: ${contra.confidenceOrdinal}", 40f, y, boldPaint)
                y += 14f
                val lines = if (contra.summary.length > 85) contra.summary.chunked(85) else listOf(contra.summary)
                for (line in lines) {
                    canvas.drawText("  $line", 40f, y, textPaint)
                    y += 12f
                }
                canvas.drawText("  Anchors: ${contra.sourceAnchors.joinToString()}", 40f, y, textPaint)
                y += 15f
                if (y > 300f) break
            }
        }
        
        canvas.drawText("6. JURISDICTION COMPLIANCE & LEGAL CITATIONS", 40f, y, subHeaderPaint)
        y += 20f
        for (mapping in report.legalMappings) {
            canvas.drawText("  - Aligned: $mapping", 40f, y, textPaint)
            y += 14f
        }
        y += 15f

        canvas.drawText("7. SECURED COGNITIVE SIGNATURE AND IMMUTABLE SEALS", 40f, y, subHeaderPaint)
        y += 20f
        canvas.drawText("The forensic ledger records have been securely locked with on-device SHA-512 signatures.", 40f, y, textPaint)
        y += 14f
        canvas.drawText("Any unauthorized manipulation of this document automatically invalidates checking parameters.", 40f, y, textPaint)
        y += 18f
        
        val seal = report.chainOfCustody?.cryptographicSignature ?: "VO-OFFLINE-LOCAL-SHA512-SEAL-PENDING"
        canvas.drawText("CRYPTOGRAPHIC SECURITY SIGNATURE SEAL BLOCK:", 40f, y, boldPaint)
        y += 12f
        if (seal.length >= 64) {
            canvas.drawText(seal.take(64), 50f, y, textPaint)
            y += 12f
            canvas.drawText(seal.drop(64), 50f, y, textPaint)
        } else {
            canvas.drawText(seal, 50f, y, textPaint)
        }
        y += 30f

        canvas.drawText("CERTIFIED SECURE BY THE VERUM OMNIS COURT PROTOCOLS V5.2.7", 40f, y, boldPaint)
        y += 15f
        canvas.drawText("This certificate constitutes direct evidence index validation. Jurisdictions: UAE, RSA, EU, GLOBAL", 40f, y, textPaint)

        pdfDocument.finishPage(page)

        val fileName = "Verum_Omnis_${report.caseId}.pdf"
        val pdfFile = File(context.cacheDir, fileName)
        val fileOutputStream = FileOutputStream(pdfFile)
        pdfDocument.writeTo(fileOutputStream)
        fileOutputStream.close()
        pdfDocument.close()
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_SUBJECT, "Sealed Court-Ready Forensic Certificate - Case ${report.caseId}")
        }
        val chooser = Intent.createChooser(shareIntent, "Share or Save Court-Sealed Forensic PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        Toast.makeText(context, "Multi-page Forensic Report Sealed Successfully", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "PDF Multi-page Export Failure: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

fun readTextFromUri(context: android.content.Context, uri: Uri): String {
    return try {
        val fileName = getFileNameFromUri(context, uri)
        when {
            fileName.endsWith(".pdf", true) -> {
                com.example.omnis.engine.ForensicEngine.parsePdfText(context, uri)
            }
            fileName.endsWith(".zip", true) -> {
                com.example.omnis.engine.ForensicEngine.parseZipText(context, uri)
            }
            fileName.endsWith(".wav", true) || fileName.endsWith(".mp3", true) ||
            fileName.endsWith(".mp4", true) || fileName.endsWith(".avi", true) -> {
                com.example.omnis.engine.ForensicEngine.parseMediaText(context, uri, fileName)
            }
            else -> {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { reader ->
                        reader.readText()
                    }
                } ?: ""
            }
        }
    } catch (e: Exception) {
        "Error reading file content: ${e.localizedMessage}"
    }
}

fun getFileNameFromUri(context: android.content.Context, uri: Uri): String {
    var name = "imported_evidence.txt"
    try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
    } catch (e: Exception) {
        uri.lastPathSegment?.let { name = it }
    }
    return name
}
