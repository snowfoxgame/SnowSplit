package com.snowsplit.app.feature.bubbles

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snowsplit.app.data.local.media.ContactAvatarStorage
import com.snowsplit.app.domain.model.AppSettings
import com.snowsplit.app.domain.model.AvatarType
import com.snowsplit.app.domain.model.DebtDirection
import com.snowsplit.app.domain.model.DebtEntry
import com.snowsplit.app.domain.model.EntryKind
import com.snowsplit.app.domain.model.PersonBalanceSummary
import com.snowsplit.app.domain.model.PersonBubble
import com.snowsplit.app.domain.model.SettleTarget
import com.snowsplit.app.domain.model.TotalBalanceOverview
import com.snowsplit.app.domain.repository.DebtRepository
import com.snowsplit.app.domain.repository.PersonRepository
import com.snowsplit.app.domain.repository.SettingsRepository
import com.snowsplit.app.domain.usecase.DebtCalculationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.PI

@Immutable
data class BubblesUiState(
    val settings: AppSettings = AppSettings(),
    val people: List<PersonBubble> = emptyList(),
    val summaries: List<PersonBalanceSummary> = emptyList(),
    val overview: TotalBalanceOverview = TotalBalanceOverview()
)

@HiltViewModel
class BubblesViewModel @Inject constructor(
    private val personRepository: PersonRepository,
    private val debtRepository: DebtRepository,
    private val settingsRepository: SettingsRepository,
    private val calculationEngine: DebtCalculationEngine,
    private val contactAvatarStorage: ContactAvatarStorage
) : ViewModel() {

    val uiState: StateFlow<BubblesUiState> = combine(
        settingsRepository.observeSettings(),
        personRepository.observePeople(),
        debtRepository.observeAllEntries()
    ) { settings, people, entries ->
        val summaries = calculationEngine.calculateAllSummaries(
            people = people,
            entries = entries,
            mode = settings.debtCalculationMode
        )
        val overview = calculationEngine.calculateTotalOverview(summaries)
        BubblesUiState(
            settings = settings,
            people = people,
            summaries = summaries,
            overview = overview
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BubblesUiState()
    )

    suspend fun importAvatarPhoto(uri: Uri, previousLocalUri: String? = null): Result<String> =
        contactAvatarStorage.importFromUri(uri, previousLocalUri = previousLocalUri)

    fun savePerson(
        existingPerson: PersonBubble?,
        name: String,
        avatarType: AvatarType,
        iconKey: String,
        photoUri: String?
    ) {
        viewModelScope.launch {
            val cleanPhotoUri = if (avatarType == AvatarType.PHOTO) photoUri else null
            if (existingPerson?.photoUri != null && existingPerson.photoUri != cleanPhotoUri) {
                contactAvatarStorage.deleteLocalAvatarIfManaged(existingPerson.photoUri)
            }
            val person = if (existingPerson != null) {
                existingPerson.copy(
                    name = name.trim(),
                    avatarType = avatarType,
                    iconKey = iconKey,
                    photoUri = cleanPhotoUri
                )
            } else {
                val count = uiState.value.people.size
                val goldenAngle = ((count * 137.508) % 360.0) * PI / 180.0
                PersonBubble(
                    name = name.trim(),
                    avatarType = avatarType,
                    iconKey = iconKey,
                    photoUri = cleanPhotoUri,
                    anchorAngleRad = goldenAngle.toFloat(),
                    anchorRingFactor = 1f
                )
            }
            personRepository.upsertPerson(person)
        }
    }

    fun deletePerson(person: PersonBubble) {
        viewModelScope.launch {
            personRepository.deletePerson(person.id)
            contactAvatarStorage.deleteLocalAvatarIfManaged(person.photoUri)
        }
    }

    fun setMeProfile(
        displayName: String,
        avatarType: AvatarType,
        iconKey: String,
        photoUri: String?
    ) {
        viewModelScope.launch {
            val previousPhoto = uiState.value.settings.mePhotoUri
            val cleanPhotoUri = if (avatarType == AvatarType.PHOTO) photoUri else null
            if (previousPhoto != null && previousPhoto != cleanPhotoUri) {
                contactAvatarStorage.deleteLocalAvatarIfManaged(previousPhoto)
            }
            settingsRepository.setMeProfile(displayName, avatarType, iconKey, cleanPhotoUri)
        }
    }

    fun saveQuickEntry(
        personId: Long,
        entryKind: EntryKind,
        direction: DebtDirection,
        amountCents: Long,
        note: String?
    ) {
        if (amountCents <= 0L) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            debtRepository.upsertEntry(
                DebtEntry(
                    personId = personId,
                    entryKind = entryKind,
                    direction = direction,
                    amountCents = amountCents,
                    note = note?.trim()?.takeIf { it.isNotEmpty() },
                    occurredAt = now,
                    createdAt = now
                )
            )
        }
    }

    fun settlePerson(
        personId: Long,
        target: SettleTarget,
        note: String
    ) {
        viewModelScope.launch {
            val entries = debtRepository.observeEntriesForPerson(personId).first()
            val settlements = calculationEngine.buildSettlementEntries(
                personId = personId,
                existingEntries = entries,
                target = target,
                note = note.trim().takeIf { it.isNotEmpty() }
            )
            if (settlements.isNotEmpty()) {
                debtRepository.insertEntries(settlements)
            }
        }
    }

    fun saveFullEntry(
        existingEntryId: Long?,
        existingPersonId: Long?,
        newPersonName: String,
        newPersonAvatarType: AvatarType,
        newPersonIconKey: String,
        newPersonPhotoUri: String?,
        entryKind: EntryKind,
        direction: DebtDirection,
        amountCents: Long,
        note: String?,
        occurredAt: Long
    ) {
        viewModelScope.launch {
            val resolvedPersonId = if (existingPersonId != null) {
                existingPersonId
            } else {
                val count = uiState.value.people.size
                val goldenAngle = ((count * 137.508) % 360.0) * PI / 180.0
                personRepository.upsertPerson(
                    PersonBubble(
                        name = newPersonName.trim(),
                        avatarType = newPersonAvatarType,
                        iconKey = newPersonIconKey,
                        photoUri = if (newPersonAvatarType == AvatarType.PHOTO) newPersonPhotoUri else null,
                        anchorAngleRad = goldenAngle.toFloat(),
                        anchorRingFactor = 1f
                    )
                )
            }

            val existing = existingEntryId?.let { debtRepository.getEntryById(it) }
            debtRepository.upsertEntry(
                DebtEntry(
                    id = existingEntryId ?: 0L,
                    personId = resolvedPersonId,
                    entryKind = entryKind,
                    direction = direction,
                    amountCents = amountCents,
                    note = note?.trim()?.takeIf { it.isNotEmpty() },
                    occurredAt = occurredAt,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                    settlementBatchId = existing?.settlementBatchId
                )
            )
        }
    }
}
