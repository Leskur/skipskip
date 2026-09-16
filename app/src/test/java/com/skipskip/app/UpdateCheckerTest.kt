package com.skipskip.app

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun compareVersions_numeric() {
        assertEquals(1, UpdateChecker.compareVersions("0.0.2", "0.0.1"))
        assertEquals(0, UpdateChecker.compareVersions("v0.0.1", "0.0.1"))
        assertEquals(-1, UpdateChecker.compareVersions("0.0.1", "0.1.0"))
    }

    @Test
    fun newestRelease_skipsDraft() {
        val json = JSONArray(
            """
            [
              {"tag_name":"v0.0.2","draft":true,"html_url":"https://example.com/2"},
              {"tag_name":"v0.0.1","draft":false,"prerelease":true,"html_url":"https://example.com/1"}
            ]
            """.trimIndent(),
        )
        val latest = UpdateChecker.newestRelease(json)!!
        assertEquals("0.0.1", latest.tag)
        assertEquals("https://example.com/1", latest.url)
    }

    @Test
    fun newestRelease_empty() {
        assertNull(UpdateChecker.newestRelease(JSONArray("[]")))
    }
}
