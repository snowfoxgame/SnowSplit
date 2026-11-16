package com.snowsplit.app.data.repository

import com.snowsplit.app.data.local.db.PersonDao
import com.snowsplit.app.data.local.db.toDomain
import com.snowsplit.app.data.local.db.toEntity
import com.snowsplit.app.data.local.media.ContactAvatarStorage
import com.snowsplit.app.domain.model.PersonBubble
import com.snowsplit.app.domain.repository.PersonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflinePersonRepository @Inject constructor(
    private val personDao: PersonDao,
    private val contactAvatarStorage: ContactAvatarStorage
) : PersonRepository {

    override fun observePeople(): Flow<List<PersonBubble>> =
        personDao.observePeople().map { list -> list.map { it.toDomain() } }

    override suspend fun getPersonById(id: Long): PersonBubble? =
        personDao.getPersonById(id)?.toDomain()

    override suspend fun upsertPerson(person: PersonBubble): Long {
        val entity = person.toEntity()
        return if (entity.id == 0L) {
            personDao.insert(entity)
        } else {
            personDao.update(entity)
            entity.id
        }
    }

    override suspend fun deletePerson(personId: Long) {
        val existing = personDao.getPersonById(personId)
        personDao.deleteById(personId)
        contactAvatarStorage.deleteLocalAvatarIfManaged(existing?.photoUri)
    }
}
