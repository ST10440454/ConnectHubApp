package com.connecthub.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportedLanguagesTest {

    @Test
    fun `English is included and is the fallback`() {
        assertTrue(SupportedLanguages.ALL.any { it.code == "en" })
        assertEquals("en", SupportedLanguages.byCode("unknown-code").code)
    }

    @Test
    fun `Venda is included now that translation is static-resource based, not Google Translate dependent`() {
        assertTrue(SupportedLanguages.ALL.any { it.code == "ve" })
    }

    @Test
    fun `byCode returns the matching language`() {
        val zulu = SupportedLanguages.byCode("zu")
        assertEquals("isiZulu", zulu.displayName)
    }

    @Test
    fun `all eleven official South African languages are present`() {
        assertEquals(11, SupportedLanguages.ALL.size)
    }
}
