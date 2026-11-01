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
    fun `English needs no translation but every other language has a DeepL code`() {
        assertTrue(SupportedLanguages.byCode("en").isEnglish)
        SupportedLanguages.ALL.filter { it.code != "en" }.forEach {
            assertTrue("${it.code} needs a deeplCode", it.deeplCode != null)
        }
    }

    @Test
    fun `byCode returns the matching language`() {
        val zulu = SupportedLanguages.byCode("zu")
        assertEquals("isiZulu", zulu.displayName)
        assertEquals("ZU", zulu.deeplCode)
    }

    @Test
    fun `only English and the DeepL-supported South African languages are offered`() {
        assertEquals(listOf("en", "af", "xh", "zu"), SupportedLanguages.ALL.map { it.code })
    }
}

