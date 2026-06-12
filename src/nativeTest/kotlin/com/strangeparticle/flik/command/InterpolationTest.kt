package com.strangeparticle.flik.command

import kotlin.test.Test
import kotlin.test.assertEquals

class InterpolationTest {
    @Test
    fun substitutesProjectRootWithSpaces() {
        assertEquals(
            "/proj/desktopApp/x.dmg",
            interpolate("{{ project_root }}/desktopApp/x.dmg", "/proj"),
        )
    }

    @Test
    fun substitutesWithoutInnerSpaces() {
        assertEquals("/proj/a", interpolate("{{project_root}}/a", "/proj"))
    }

    @Test
    fun leavesOtherTextUntouched() {
        assertEquals("no tokens here", interpolate("no tokens here", "/proj"))
    }
}
