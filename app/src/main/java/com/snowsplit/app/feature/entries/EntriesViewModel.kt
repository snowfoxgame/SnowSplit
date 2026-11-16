package com.snowsplit.app.feature.entries

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
import com.snowsplit.app.domain.model.PersonBubble
import com.snowsplit.app.domain.model.RawPersonBuckets
import com.snowsplit.app.domain.repository.DebtRepository
import com.snowsplit.app.domain.repository.PersonRepository
import com.snowsplit.app.domain.repository.SettingsRepository
import com.snowsplit.app.domain.usecase.DebtCalculationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.PI

enum class EntryDirectionFilter {
    ALL,
    OWED_TO_ME,
    I_OWE
}

@Immutable
data class EntryListItem(
    val entry: DebtEntry,
    val person: PersonBubble,
    val canQuickSettle: Boolean,
    val quickSettleAmountCents: Long
)

@Immutable
data class EntriesUiState(
    val settings: AppSettings = AppSettings(),
    val people: List<PersonBubble> = emptyList(),
    val allEntriesCount: Int = 0,
    val items: List<EntryListItem> = emptyList(),
    val directionFilter: EntryDirectionFilter = EntryDirectionFilter.ALL,
    val selectedPersonId: Long? = null
)

@HiltViewModel
class EntriesViewModel @Inject constructor(
    private val personRepository: PersonRepository,
    private val debtRepository: DebtRepository,
    settingsRepository: SettingsRepository,
    private val calculationEngine: DebtCalculationEngine,
    private val contactAvatarStorage: ContactAvatarStorage
) : ViewModel() {

    private val directionFilter = MutableStateFlow(EntryDirectionFilter.ALL)
    private val selectedPersonId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<EntriesUiState> = combine(
        settingsRepository.observeSettings(),
        personRepository.observePeople(),
        debtRepository.observeAllEntries(),
        directionFilter,
        selectedPersonId
    ) { settings, people, entries, filter, personFilterId ->
        val peopleById = people.associateBy { it.id }
        val entriesByPerson = entries.groupBy { it.personId }
        val rawBucketsByPerson: Map<Long, RawPersonBuckets> = entriesByPerson.mapValues { (_, list) ->
            calculationEngine.calculateRawBuckets(list)
        }

        val activePersonFilter = personFilterId?.takeIf { peopleById.containsKey(it) }

        val filtered = entries.mapNotNull { entry ->
            val person = peopleById[entry.personId] ?: return@mapNotNull null
            if (activePersonFilter != null && entry.personId != activePersonFilter) {
                return@mapNotNull null
            }
            val matchesDirection = when (filter) {
                EntryDirectionFilter.ALL -> true
                EntryDirectionFilter.OWED_TO_ME -> entry.direction == DebtDirection.THEY_OWE_ME
                EntryDirectionFilter.I_OWE -> entry.direction == DebtDirection.I_OWE_THEM
            }
            if (!matchesDirection) return@mapNotNull null

            val buckets = rawBucketsByPerson[entry.personId] ?: RawPersonBuckets()
            val openBucket = if (entry.direction == DebtDirection.THEY_OWE_ME) {
                buckets.theyOweMeCents
            } else {
                buckets.iOweThemCents
            }
            val settleable = if (entry.entryKind == EntryKind.DEBT && openBucket > 0L) {
                minOf(entry.amountCents, openBucket)
            } else {
                0L
            }

            EntryListItem(
                entry = entry,
                person = person,
                canQuickSettle = settleable > 0L,
                quickSettleAmountCents = settleable
            )
        }

        EntriesUiState(
            settings = settings,
            people = people,
            allEntriesCount = entries.size,
            items = filtered,
            directionFilter = filter,
            selectedPersonId = activePersonFilter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EntriesUiState()
    )

    fun setDirectionFilter(filter: EntryDirectionFilter) {
        directionFilter.value = filter
    }

    fun setSelectedPersonId(personId: Long?) {
        selectedPersonId.value = personId
    }

    suspend fun importAvatarPhoto(uri: Uri): Result<String> =
        contactAvatarStorage.importFromUri(uri)

    fun saveEntry(
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

    fun quickSettleEntry(item: EntryListItem, note: String) {
        if (!item.canQuickSettle || item.quickSettleAmountCents <= 0L) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            debtRepository.upsertEntry(
                DebtEntry(
                    personId = item.person.id,
                    entryKind = EntryKind.PAYMENT,
                    direction = item.entry.direction,
                    amountCents = item.quickSettleAmountCents,
                    note = note,
                    occurredAt = now,
                    createdAt = now
                )
            )
        }
    }

    fun deleteEntry(entry: DebtEntry) {
        viewModelScope.launch {
            debtRepository.deleteEntry(entry.id)
        }
    }

    fun restoreDeletedEntry(entry: DebtEntry) {
        viewModelScope.launch {
            debtRepository.upsertEntry(entry)
        }
    }
}
