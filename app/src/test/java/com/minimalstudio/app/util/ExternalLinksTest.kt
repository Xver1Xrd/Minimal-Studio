package com.minimalstudio.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExternalLinksTest {
    @Test fun addsHttpsToPlainHost() {
        assertEquals("https://store.steampowered.com/app/123", ExternalLinks.safeHttpUrl("store.steampowered.com/app/123"))
    }
    @Test fun preservesHttps() {
        assertEquals("https://example.com/a", ExternalLinks.safeHttpUrl("https://example.com/a"))
    }
    @Test fun blocksUnsafeSchemes() {
        assertNull(ExternalLinks.safeHttpUrl("javascript:alert(1)"))
        assertNull(ExternalLinks.safeHttpUrl("file:///etc/passwd"))
        assertNull(ExternalLinks.safeHttpUrl(""))
    }
}
