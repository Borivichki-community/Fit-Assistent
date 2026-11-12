package dev.andre.nutritionist.profile.service

import com.budgetntr.proto.profile.v1.UpdateProfileRequest
import dev.andre.nutritionist.profile.domain.mapper.applyUpdates
import dev.andre.nutritionist.profile.domain.mapper.toNewEntity
import dev.andre.nutritionist.profile.persistence.entity.ProfileEntity
import dev.andre.nutritionist.profile.persistence.repository.ProfileJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProfileService(
    private val profileRepository: ProfileJpaRepository,
) {
    @Transactional(readOnly = true)
    fun getProfileByUserId(userId: String): ProfileEntity? {
        return profileRepository.findByUserId(userId)
    }

    @Transactional
    fun upsertProfile(request: UpdateProfileRequest): ProfileEntity {
        val existing = profileRepository.findByUserId(request.userId)
        val entity =
            if (existing != null) {
                existing.applyUpdates(request)
            } else {
                request.toNewEntity()
            }
        return profileRepository.save(entity)
    }
}
