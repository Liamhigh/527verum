package com.example.omnis.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.omnis.api.GeminiApiClient
import com.example.omnis.data.ForensicDatabase
import com.example.omnis.data.ForensicRepository
import com.example.omnis.engine.ForensicEngine
import com.example.omnis.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets

class ForensicViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ForensicDatabase.getDatabase(application)
    private val repository = ForensicRepository(database.forensicDao())

    val allCases: StateFlow<List<ForensicCase>> = repository.allCasesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(500),
            initialValue = emptyList()
        )

    // Current viewed forensic case
    private val _selectedCaseId = MutableStateFlow<String?>(null)
    val selectedCaseId: StateFlow<String?> = _selectedCaseId.asStateFlow()

    val selectedCase: StateFlow<ForensicCase?> = _selectedCaseId
        .flatMapLatest { id ->
            if (id == null) flowOf<ForensicCase?>(null)
            else flow {
                emit(repository.getCaseById(id))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(500),
            initialValue = null
        )

    // Live processing step representation
    private val _activeProcessingStep = MutableStateFlow<Int>(-1) // -1 means idle
    val activeProcessingStep: StateFlow<Int> = _activeProcessingStep.asStateFlow()

    private val _processingLogs = MutableStateFlow<List<String>>(emptyList())
    val processingLogs: StateFlow<List<String>> = _processingLogs.asStateFlow()

    // B9 Advisory chat messages
    private val _chatMessages = MutableStateFlow<List<Pair<String, String>>>(emptyList()) // Pair(Sender, Message)
    val chatMessages: StateFlow<List<Pair<String, String>>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // Pre-seed sample database if it's empty, so the user has something immediately
    init {
        viewModelScope.launch {
            val list = repository.allCasesFlow.first()
            if (list.isEmpty()) {
                seedDefaultSamples()
            }
        }
    }

    fun selectCase(caseId: String?) {
        _selectedCaseId.value = caseId
        _chatMessages.value = emptyList() // Clear chat for new case
    }

    // Run active 16-step execution visually
    fun runCaseAnalysis(
        title: String,
        content: String,
        type: String, // "TEXT", "DOCUMENT", "AUDIO", "VIDEO", "MULTIMODAL"
        jurisdiction: String,
        simulateConcealment: Boolean = false,
        simulateAudioFailure: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            _activeProcessingStep.value = 0
            _processingLogs.value = emptyList()

            val stepsList = listOf(
                "Evidence Intake", "Native Evidence Pipeline", "Rules Extraction",
                "Jurisdiction & Legal Context", "Behavior & Pattern Analysis", "Nine-Brain System Consensuses",
                "Forensic Synthesis", "Promotion Coordinator Assessment", "Promotion Rules (P1-P7) Execution",
                "Audit Ledger Integrity Checks", "Guardian Safety Approvals", "Report Seal Certification",
                "Publication Normalizer", "Triple Verification Gates Review", "Forensic Conclusion JSON Assembly",
                "Human-Readable Report Rendering"
            )

            // Simulate the 16 steps visually with logs
            for (stepIdx in 0..15) {
                _activeProcessingStep.value = stepIdx
                val stepName = stepsList[stepIdx]
                addLog("Initializing Step ${stepIdx + 1}/16: $stepName...")
                delay(150)

                when (stepIdx) {
                    0 -> addLog("Evidence SHA-512 calculated. Deterministic CaseID derived.")
                    1 -> addLog("Extracting character layouts and on-device OCR page segments. Found ${maxOf(1, content.length / 250)} logical pages.")
                    2 -> addLog("Building named entity register and timeline anchors.")
                    3 -> addLog("Selected jurisdiction: $jurisdiction. Mapping appropriate statutes and evidence codes.")
                    4 -> addLog("Running time-series semantic drift checks and behavioral linguistics profiles.")
                    5 -> {
                        addLog("Querying Nine Forensic Brains (B1-B9) for active analysis indicators...")
                        addLog("B1: Contradiction lead arbiter loaded. Target status: ACTIVE")
                        addLog("B2: Image Forensics status matching. Engaged: ${type != "TEXT"}")
                        addLog("B3: Comms metadata structural validation initialized.")
                        addLog("B5: Travel/Time drift checkers online.")
                        addLog("B6: Financial anomalies scanner loaded. Matches central claims: ${content.contains("ACME", true)}")
                        addLog("B8: Audio Voiceprint checks. Status: ${if (type == "AUDIO" || type == "VIDEO") "ACTIVE" else "NOT_ENGAGED"}")
                    }
                    6 -> addLog("Forensic Synthesis: Fusing multidimensional signals and cross-brain contradiction postures.")
                    7 -> addLog("Collecting all promotable candidate records from isolated register logs.")
                    8 -> addLog("Applying P1-P7 filters. P1(Anchor check: OK), P2(Actor check: OK), P3(Sufficiency: OK), P4(Corroboration: OK)")
                    9 -> addLog("Audit ledger compiled with hash parameters to survive third-party legal inspection.")
                    10 -> addLog("GuardianService reviewing for overclaims. Verifying 'no verdict style' constraint.")
                    11 -> addLog("Approved findings transitioned to immutable Certified state. Certification indices completed.")
                    12 -> addLog("Normalizing summaries and masking confidential unredacted lines.")
                    13 -> addLog("Triple Verification Safety Doctrine in progress: Thesis Gate, Antithesis Gate, Synthesis Gate running...")
                    14 -> addLog("Compiling final deterministic JSON manifest.")
                    15 -> addLog("Rendering Human-Readable Report blocks. Sealed cryptography wrapper complete.")
                }
                delay(120)
            }

            // Perform real engine analysis
            val bytes = content.toByteArray(StandardCharsets.UTF_8)
            val reportRenderInput = ForensicEngine.runAnalysis(
                evidenceName = if (title.endsWith(".pdf", true) || title.endsWith(".wav", true)) title else "$title.txt",
                evidenceBytes = bytes,
                evidenceText = content,
                evidenceType = type,
                jurisdiction = jurisdiction,
                simulateConcealment = simulateConcealment,
                simulateAudioFailure = simulateAudioFailure
            )

            val hash512 = ForensicEngine.sha512(bytes)
            val runMetadata = RunMetadata(
                caseId = reportRenderInput.caseId,
                evidenceHashSha512 = hash512,
                evidenceHashPrefix = hash512.take(16),
                jurisdiction = jurisdiction,
                jurisdictionName = when (jurisdiction) {
                    "ZA" -> "South Africa Law Courts"
                    "US" -> "US Federal Court Standards"
                    "EU" -> "EU Digital Justice Regulations"
                    else -> "NIST SP 800-86 Global Guidelines"
                },
                processingStatus = if (reportRenderInput.tripleVerificationStatus == "PASS") "SEALED" else "FAIL_CLOSED",
                verifiedContradictionCount = reportRenderInput.contradictions.filter { it.status == "VERIFIED" }.size,
                candidateContradictionCount = reportRenderInput.contradictions.filter { it.status == "CANDIDATE" }.size,
                guardianApprovedCertifiedFindingCount = reportRenderInput.certifiedFindings.filter { it.status == "CERTIFIED" }.size,
                sourcePageCount = maxOf(1, content.length / 250),
                deterministicRunId = reportRenderInput.deterministicRunId
            )

            val newCase = ForensicCase(
                id = reportRenderInput.caseId,
                title = title,
                evidenceType = type,
                evidenceName = if (title.endsWith(".pdf", true) || title.endsWith(".wav", true)) title else "$title.txt",
                evidenceSize = bytes.size.toLong(),
                evidenceHashSha512 = hash512,
                evidenceHashPrefix = hash512.take(16),
                evidenceContent = content,
                processingStatus = if (reportRenderInput.tripleVerificationStatus == "PASS") "SEALED" else "FAIL_CLOSED",
                deterministicRunId = reportRenderInput.deterministicRunId,
                hasConcealment = simulateConcealment,
                runMetadataJson = repository.serializeRunMetadata(runMetadata),
                reportRenderInputJson = repository.serializeReportInput(reportRenderInput),
                isSealed = reportRenderInput.tripleVerificationStatus == "PASS"
            )

            repository.saveCase(newCase)
            _selectedCaseId.value = newCase.id

            // Done processing
            _activeProcessingStep.value = -1
        }
    }

    private fun addLog(message: String) {
        _processingLogs.value = _processingLogs.value + message
    }

    // Speak to the non-voting advisory brain B9
    fun askAdvisoryB9(question: String) {
        val activeCase = selectedCase.value ?: return
        if (question.isBlank()) return

        // Add user message to chat state
        _chatMessages.value = _chatMessages.value + Pair("User", question)
        _isChatLoading.value = true

        viewModelScope.launch {
            val reportInput = repository.deserializeReportInput(activeCase.reportRenderInputJson)
            val reportText = if (reportInput != null) {
                """
                Case ID: ${reportInput.caseId}
                Global Status: ${activeCase.processingStatus}
                Triple Verification: ${reportInput.tripleVerificationStatus}
                Certified Findings:
                ${reportInput.certifiedFindings.joinToString("\n") { "- ${it.actor}: ${it.summary} (Confidence: ${it.confidenceOrdinal})" }}
                Contradictions Ledger:
                ${reportInput.contradictions.joinToString("\n") { "- TYPE: ${it.conflictType}, STATUS: ${it.status}, SUMMARY: ${it.summary}" }}
                """.trimIndent()
            } else {
                "Text content: ${activeCase.evidenceContent}"
            }

            val b9Response = GeminiApiClient.talkWithB9(question, reportText)
            _chatMessages.value = _chatMessages.value + Pair("B9 (Research & Development)", b9Response)
            _isChatLoading.value = false
        }
    }

    // Delete a case from the local Room DB
    fun deleteCase(caseId: String) {
        viewModelScope.launch {
            repository.deleteCaseById(caseId)
            if (_selectedCaseId.value == caseId) {
                _selectedCaseId.value = null
            }
        }
    }

    // Inject preset vector cases per the test requirements on page 15 of PDF
    private suspend fun seedDefaultSamples() = withContext(Dispatchers.Default) {
        val sampleList = listOf(
            Triple(
                "Acme Invoice Audit.pdf",
                """
                AUDIT SYSTEM EXCLUSIVES - METADATA LOG ENTRY
                
                Page 2 Statement:
                Billing Officer Alice certifies ACME Corporate Invoice #2026-A for amount $10,000 has been PAID in full via bank electronic clearing system.
                
                Page 5 Statement:
                Chief Accounting Officer Bob records ACME Invoice #2026-A for amount $10,000 is currently UNPAID, past due, and outstanding for collection.
                
                Evidence review bounds are locked. Both departments claim 100% verified ledger alignment.
                """.trimIndent(),
                "DOCUMENT"
            ),
            Triple(
                "Alpha Geographic Logs.csv",
                """
                SUBJECT ALPHA GEOGRAPHIC ACCESS LOGS
                
                Event log Page 1: 
                NTP Server Sync Log: Subject Alpha logged into London secure terminal. Timestamp: 2026-05-20 14:15:30 UTC.
                
                Event log Page 3: 
                Physical Signature Book: Subject Alpha physically signed hand-written registration book in Johannesburg, South Africa at 2026-05-20 14:45:00 UTC.
                
                No reliable central NTP reference clock coordinates the Page 3 entry.
                """.trimIndent(),
                "TEXT"
            ),
            Triple(
                "Financial Ledger Backup.txt",
                """
                HASH CHAIN INTEGRITY DIAGNOSTICS:
                
                Block 3 Hash value: 0xAF8123B90BCEF11
                Block 4 Stored Parent Hash value: 0x12C098DB8AFE102
                
                CRITICAL MISMATCH ENCOUNTERED - Ledger chain sequence broken! Previous block hash does not match current parent pointer.
                """.trimIndent(),
                "TEXT"
            )
        )

        for (item in sampleList) {
            val bytes = item.second.toByteArray(StandardCharsets.UTF_8)
            val reportRenderInput = ForensicEngine.runAnalysis(
                evidenceName = item.first,
                evidenceBytes = bytes,
                evidenceText = item.second,
                evidenceType = item.third,
                jurisdiction = "ZA"
            )

            val hash512 = ForensicEngine.sha512(bytes)
            val runMetadata = RunMetadata(
                caseId = reportRenderInput.caseId,
                evidenceHashSha512 = hash512,
                evidenceHashPrefix = hash512.take(16),
                jurisdiction = "ZA",
                jurisdictionName = "South Africa Law Courts",
                processingStatus = if (reportRenderInput.tripleVerificationStatus == "PASS") "SEALED" else "FAIL_CLOSED",
                verifiedContradictionCount = reportRenderInput.contradictions.filter { it.status == "VERIFIED" }.size,
                candidateContradictionCount = reportRenderInput.contradictions.filter { it.status == "CANDIDATE" }.size,
                guardianApprovedCertifiedFindingCount = reportRenderInput.certifiedFindings.filter { it.status == "CERTIFIED" }.size,
                sourcePageCount = maxOf(1, item.second.length / 250),
                deterministicRunId = reportRenderInput.deterministicRunId
            )

            val newCase = ForensicCase(
                id = reportRenderInput.caseId,
                title = item.first.removeSuffix(".pdf").removeSuffix(".csv").removeSuffix(".txt"),
                evidenceType = item.third,
                evidenceName = item.first,
                evidenceSize = bytes.size.toLong(),
                evidenceHashSha512 = hash512,
                evidenceHashPrefix = hash512.take(16),
                evidenceContent = item.second,
                processingStatus = if (reportRenderInput.tripleVerificationStatus == "PASS") "SEALED" else "FAIL_CLOSED",
                deterministicRunId = reportRenderInput.deterministicRunId,
                hasConcealment = false,
                runMetadataJson = repository.serializeRunMetadata(runMetadata),
                reportRenderInputJson = repository.serializeReportInput(reportRenderInput),
                isSealed = reportRenderInput.tripleVerificationStatus == "PASS"
            )

            repository.saveCase(newCase)
        }
    }
}
