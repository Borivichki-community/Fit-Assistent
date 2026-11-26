package dev.andre.nutritionist.presentation.coaching

import dev.andre.nutritionist.core.kotea.CommandsFlowHandler
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.repository.CoachingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class CoachStudentsCommandsFlowHandler(
    private val coachingRepository: CoachingRepository,
    private val dispatchers: CoroutineDispatchers,
) : CommandsFlowHandler<CoachStudentsCommand, CoachStudentsEvent> {

    override fun handle(commands: Flow<CoachStudentsCommand>): Flow<CoachStudentsEvent> = flow {
        commands.collect { command ->
            when (command) {
                is CoachStudentsCommand.LoadLinks -> {
                    val result = withContext(dispatchers.io) {
                        coachingRepository.getCoachLinks()
                    }
                    result.fold(
                        onSuccess = { links ->
                            val coachLinks = links.filter {
                                command.coachUserId.raw.isBlank() || it.coachId == command.coachUserId
                            }
                            emit(CoachStudentsEvent.Internal.LinksLoaded(coachLinks))
                        },
                        onFailure = { error ->
                            if (error is CancellationException) throw error
                            emit(CoachStudentsEvent.Internal.LinksFailed(error))
                        },
                    )
                }

                is CoachStudentsCommand.InviteStudent -> {
                    val result = withContext(dispatchers.io) {
                        coachingRepository.inviteStudent(
                            studentId = command.studentId,
                            canViewDiary = command.canViewDiary,
                        )
                    }
                    result.fold(
                        onSuccess = { link ->
                            emit(CoachStudentsEvent.Internal.StudentInvited(link))
                        },
                        onFailure = { error ->
                            if (error is CancellationException) throw error
                            emit(CoachStudentsEvent.Internal.InviteFailed(error))
                        },
                    )
                }
            }
        }
    }
}
