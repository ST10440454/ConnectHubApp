package com.connecthub.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationIdTest {

    @Test
    fun `conversation id is the same regardless of argument order`() {
        val idOneWay = conversationIdFor("uidA", "uidB")
        val idOtherWay = conversationIdFor("uidB", "uidA")
        assertEquals(idOneWay, idOtherWay)
    }

    @Test
    fun `conversation id combines both uids sorted`() {
        val id = conversationIdFor("zzz", "aaa")
        assertEquals("aaa_zzz", id)
    }
}
