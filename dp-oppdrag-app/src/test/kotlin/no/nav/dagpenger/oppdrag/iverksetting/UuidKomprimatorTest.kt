package no.nav.dagpenger.oppdrag.iverksetting

import no.nav.dagpenger.oppdrag.iverksetting.domene.UuidKomprimator.dekomprimer
import no.nav.dagpenger.oppdrag.iverksetting.domene.UuidKomprimator.komprimer
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class UuidKomprimatorTest {
    @Test
    fun `UUID skal være identisk etter komprimering og dekomprimering`() {
        val originalUUID = UUID.randomUUID()
        val komprimert = originalUUID.komprimer()
        val dekomprimert = komprimert.dekomprimer()

        assertEquals(originalUUID, dekomprimert)
    }

    @Test
    fun `komprimert UUID skal være 24 tegn`() {
        val originalUUID = UUID.randomUUID()
        val komprimert = originalUUID.komprimer()

        assertEquals(24, komprimert.length)
    }
}
