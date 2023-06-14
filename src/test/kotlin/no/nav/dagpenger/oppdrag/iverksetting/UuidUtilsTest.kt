package no.nav.dagpenger.oppdrag.iverksetting

import no.nav.dagpenger.oppdrag.iverksetting.UuidUtils.dekomprimer
import no.nav.dagpenger.oppdrag.iverksetting.UuidUtils.komprimer
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class UuidUtilsTest {

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

        val uuid2 = UUID.fromString("62640ece-f941-11ed-be56-0242ac120002")
        println(uuid2.komprimer())
    }
}
