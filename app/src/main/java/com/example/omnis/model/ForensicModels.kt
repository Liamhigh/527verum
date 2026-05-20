package com.example.omnis.model

import androidx.room.*
import com.squareup.moshi.JsonClass

@Entity(tableName = "forensic_cases")
@JsonClass(generateAdapter = true)
data class ForensicCase(
    @PrimaryKey val id: String, // CaseId (computed as deterministic prefix)
    val title: String,
    val dateCreated: Long = System.currentTimeMillis(),
    val evidenceType: String, // "TEXT", "DOCUMENT", "AUDIO", "VIDEO", "MULTIMODAL"
    val evidenceName: String,
    val evidenceSize: Long,
    val evidenceHashSha512: String,
    val evidenceHashPrefix: String,
    val evidenceContent: String, // Raw extracted or simulated payload
    val processingStatus: String, // "PENDING", "PROCESSING", "SEALED", "FAILED"
    val deterministicRunId: String,
    val hasConcealment: Boolean = false,
    val runMetadataJson: String = "",
    val reportRenderInputJson: String = "",
    val isSealed: Boolean = false
)

@JsonClass(generateAdapter = true)
data class RunMetadata(
    val caseId: String,
    val evidenceHashSha512: String,
    val evidenceHashPrefix: String,
    val jurisdiction: String,
    val jurisdictionName: String,
    val processingStatus: String,
    val verifiedContradictionCount: Int,
    val candidateContradictionCount: Int,
    val guardianApprovedCertifiedFindingCount: Int,
    val sourcePageCount: Int,
    val engineVersion: String = "vo-forensic-engine-2026.03.26",
    val rulesVersion: String = "vo-rules-2026.03.26",
    val deterministicRunId: String
)

@JsonClass(generateAdapter = true)
data class RawFinding(
    val id: String,
    val findingType: String, // "semantic", "timestamp", "metadata", "chain", "financial", "legal", "voice", "handwriting", "novelty"
    val status: String, // "NONE", "CANDIDATE", "VERIFIED", "REJECTED"
    val summary: String,
    val actor: String,
    val anchorPages: List<Int>,
    val sourceAnchors: List<String>,
    val excerpt: String,
    val confidenceOrdinal: String, // "VERY_HIGH", "HIGH", "MODERATE", "LOW", "INSUFFICIENT"
    val sourcePath: String,
    val brainId: String
)

@JsonClass(generateAdapter = true)
data class CertifiedFinding(
    val id: String,
    val findingType: String,
    val status: String,
    val summary: String,
    val actor: String,
    val anchorPages: List<Int>,
    val confidenceOrdinal: String,
    val guardianDecision: String, // stringified GuardianDecision details
    val contradictionStatus: String,
    val audit: String, // stringified audit details
    val renderWarnings: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PublicationFinding(
    val id: String,
    val findingType: String,
    val publicationRoleStatus: String, // role / publication readiness
    val publicSummary: String,
    val publicActorLabel: String,
    val anchorPages: List<Int>,
    val confidenceOrdinal: String,
    val guardianApproved: Boolean,
    val rawFindingStatus: String,
    val contradictionStatus: String,
    val withheldFields: List<String> = emptyList(),
    val renderWarnings: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ContradictionEntry(
    val id: String,
    val status: String, // "NONE", "CANDIDATE", "VERIFIED", "REJECTED"
    val conflictType: String, // Semantic, timestamp, metadata, chain, financial, legal, voice, handwriting, novelty
    val summary: String,
    val actors: List<String>,
    val propositionA: String,
    val propositionB: String,
    val anchorPages: List<Int>,
    val sourceAnchors: List<String>,
    val confidenceOrdinal: String,
    val supportOnly: Boolean,
    val neededEvidence: String,
    val ruleHits: List<String> // list of ruleIds and severities: "Rule ID|Severity"
)

@JsonClass(generateAdapter = true)
data class CorrelationEntity(
    val entityType: String, // "PHONE", "DEVICE_ID", "IP_ADDRESS", "LOCATION", "EMAIL"
    val valValue: String,
    val sourceAnnotations: List<String>,
    val linkedActors: List<String>,
    val riskRating: String // "HIGH", "MEDIUM", "LOW"
)

@JsonClass(generateAdapter = true)
data class BlockchainEvent(
    val walletAddress: String,
    val coin: String, // "BTC", "ETH", "USDT"
    val txHash: String,
    val value: String,
    val direction: String, // "IN", "OUT"
    val status: String // "RESOLVED", "UNVERIFIED"
)

@JsonClass(generateAdapter = true)
data class StatementEvolution(
    val actor: String,
    val topic: String,
    val dateOrSource: String,
    val statementText: String,
    val alignmentDrift: String // "CONSISTENT", "MODERATE_SHIFT", "CONTRADICTION"
)

@JsonClass(generateAdapter = true)
data class WitnessCluster(
    val topicId: String,
    val topicSummary: String,
    val actorsInvolved: List<String>,
    val clashDescription: String,
    val conflictDensityScore: Double
)

@JsonClass(generateAdapter = true)
data class CommitmentDegradation(
    val actor: String,
    val chronologicalPhrases: List<String>, // ["I will pay", "I plan to pay", "No deal existed"]
    val shiftPath: String, // "OBLIGATION -> INTENTION -> DENIAL"
    val signalStrength: String // "SEVERE", "STABLE"
)

@JsonClass(generateAdapter = true)
data class StatuteAlignedFinding(
    val findingId: String,
    val findingSummary: String,
    val proposedStatute: String,
    val codeSection: String,
    val elementsMetMapping: List<String> // ["Element X: Evidence Reference"]
)

@JsonClass(generateAdapter = true)
data class LiabilityScorecard(
    val actor: String,
    val verifiedContradictionsCount: Int,
    val candidateContradictionsCount: Int,
    val automatedTags: List<String>, // ["#Cybercrime", "#Fraud"]
    val redFlagsCount: Int,
    val confidenceWeightedDishonestyIndex: Double // 0.0 to 100.0%
)

@JsonClass(generateAdapter = true)
data class ChainOfCustodyCert(
    val evidenceHashPrefix: String,
    val sealingOfficer: String = "Verum Omnis Autoseal",
    val utcTimestamp: String,
    val cryptographicSignature: String
)

@JsonClass(generateAdapter = true)
data class HumanReviewRequest(
    val isTriggered: Boolean,
    val anomalyType: String,
    val supportingAnchors: List<String>,
    val proposedHypotheses: List<String>,
    val resolutionStatus: String // "PENDING", "RESOLVED"
)

@JsonClass(generateAdapter = true)
data class ReportRenderInput(
    val caseId: String,
    val evidenceHash: String,
    val engineVersion: String = "vo-forensic-engine-2026.03.26",
    val deterministicRunId: String,
    val jurisdiction: String,
    val tripleVerificationStatus: String, // "PASS", "FAIL"
    val thesisReason: String,
    val thesisPass: Boolean,
    val antithesisReason: String,
    val antithesisPass: Boolean,
    val synthesisReason: String,
    val synthesisPass: Boolean,
    val tieBreaker: String,
    val certifiedFindings: List<CertifiedFinding>,
    val contradictions: List<ContradictionEntry> = emptyList(),
    val legalMappings: List<String> = emptyList(),
    val legalIssueHints: List<String> = emptyList(),
    val boundaryNote: String = "This is a forensic conclusion, not a judicial verdict.",
    val b10CorrelationFindings: List<CorrelationEntity> = emptyList(),
    val blockchainTraces: List<BlockchainEvent> = emptyList(),
    val statementEvolutionLedger: List<StatementEvolution> = emptyList(),
    val crossWitnessClusters: List<WitnessCluster> = emptyList(),
    val commitmentDegradationSignals: List<CommitmentDegradation> = emptyList(),
    val statuteMappings: List<StatuteAlignedFinding> = emptyList(),
    val actorLiabilityScorecards: List<LiabilityScorecard> = emptyList(),
    val chainOfCustody: ChainOfCustodyCert? = null,
    val constitutionalConstraintHeader: String = "{}",
    val humanReviewRequest: HumanReviewRequest? = null
)

@JsonClass(generateAdapter = true)
data class BrainOutput(
    val brainId: String, // B1 - B9
    val role: String,
    val status: String, // "ACTIVE", "CANDIDATE_ONLY", "NO_SIGNAL", "NOT_ENGAGED"
    val voting: Boolean,
    val primarySignals: String,
    val publicationMeaning: String,
    val confidence: String,
    val limitations: String,
    val contributionToQuorum: Boolean
)

@JsonClass(generateAdapter = true)
data class BrainAnalysis(
    val brains: List<BrainOutput>,
    val quorumSatisfied: Boolean,
    val activeBrainsCount: Int,
    val coverageGaps: List<String>
)
