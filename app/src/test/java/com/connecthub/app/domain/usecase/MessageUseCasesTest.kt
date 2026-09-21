package com.connecthub.app.domain.usecase

import com.connecthub.app.domain.model.Message
import com.connecthub.app.domain.repository.MessageRepository
import com.connecthub.app.util.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeMessageRepository : MessageRepository {
    var lastSentContent: String? = null
    var lastConversationId: String? = null
    var lastReceiverId: String? = null
    var shouldFail = false
    var markedAsReadFor: String? = null

    override suspend fun sendMessage(conversationId: String, receiverId: String, content: String): AppResult<Unit> {
        if (shouldFail) return AppResult.Error("Failed to send.")
        lastConversationId = conversationId
        lastReceiverId = receiverId
        lastSentContent = content
        return AppResult.Success(Unit)
    }

    override fun observeMessages(conversationId: String): Flow<List<Message>> = flowOf(emptyList())

    override fun observeAllMessagesForCurrentUser(): Flow<List<Message>> = flowOf(emptyList())

    override suspend fun markMessagesAsRead(conversationId: String) {
        markedAsReadFor = conversationId
    }
}

class MessageUseCasesTest {

    @Test
    fun `sending blank message is rejected before hitting repository`() = runTest {
        val repo = FakeMessageRepository()
        val useCase = SendMessageUseCase(repo)

        val result = useCase("conv1", "receiverUid", "   ")

        assertTrue(result is AppResult.Error)
        assertEquals("Message cannot be empty.", (result as AppResult.Error).message)
        assertEquals(null, repo.lastSentContent)
    }

    @Test
    fun `sending valid message reaches repository trimmed with correct ids`() = runTest {
        val repo = FakeMessageRepository()
        val useCase = SendMessageUseCase(repo)

        val result = useCase("conv1", "receiverUid", "  Hello ConnectHub  ")

        assertTrue(result is AppResult.Success)
        assertEquals("Hello ConnectHub", repo.lastSentContent)
        assertEquals("conv1", repo.lastConversationId)
        assertEquals("receiverUid", repo.lastReceiverId)
    }

    @Test
    fun `repository failure is propagated as error`() = runTest {
        val repo = FakeMessageRepository().apply { shouldFail = true }
        val useCase = SendMessageUseCase(repo)

        val result = useCase("conv1", "receiverUid", "Hello")

        assertTrue(result is AppResult.Error)
        assertEquals("Failed to send.", (result as AppResult.Error).message)
    }
}
