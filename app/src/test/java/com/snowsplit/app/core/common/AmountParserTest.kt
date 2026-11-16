package com.snowsplit.app.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountParserTest {

    @Test
    fun `parses whole and decimal comma or dot amounts to exact cents`() {
        assertEquals(2000L, AmountParser.parsePositiveCents("20"))
        assertEquals(1250L, AmountParser.parsePositiveCents("12,50"))
        assertEquals(1250L, AmountParser.parsePositiveCents("12.50"))
        assertEquals(1999L, AmountParser.parsePositiveCents(" 19.99 "))
        assertEquals(50L, AmountParser.parsePositiveCents("0,5"))
    }

    @Test
    fun `rejects zero negative or malformed inputs`() {
        assertNull(AmountParser.parsePositiveCents(""))
        assertNull(AmountParser.parsePositiveCents("0"))
        assertNull(AmountParser.parsePositiveCents("0.00"))
        assertNull(AmountParser.parsePositiveCents("-5"))
        assertNull(AmountParser.parsePositiveCents("abc"))
    }

    @Test
    fun `formats cents back to clean editable string`() {
        assertEquals("20", AmountParser.formatInputFromCents(2000L))
        assertEquals("12.50", AmountParser.formatInputFromCents(1250L))
        assertEquals("0.05", AmountParser.formatInputFromCents(5L))
    }
}
