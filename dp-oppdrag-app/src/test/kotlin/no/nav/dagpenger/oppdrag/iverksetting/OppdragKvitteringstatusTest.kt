package no.nav.dagpenger.oppdrag.iverksetting

import no.nav.dagpenger.kontrakter.oppdrag.OppdragStatus
import no.nav.dagpenger.oppdrag.iverksetting.domene.status
import no.trygdeetaten.skjema.oppdrag.Mmel
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class OppdragKvitteringstatusTest {
    @Test
    fun `konverterer status`() {
        assertEquals(OppdragStatus.KVITTERT_OK, lagOppdrag("00").status)
        assertEquals(OppdragStatus.KVITTERT_MED_MANGLER, lagOppdrag("04").status)
        assertEquals(OppdragStatus.KVITTERT_FUNKSJONELL_FEIL, lagOppdrag("08").status)
        assertEquals(OppdragStatus.KVITTERT_TEKNISK_FEIL, lagOppdrag("12").status)
        assertEquals(OppdragStatus.KVITTERT_UKJENT, lagOppdrag("Ukjent").status)
    }

    private fun lagOppdrag(alvorlighetsgrad: String) =
        Oppdrag().apply {
            mmel = Mmel()
            mmel.alvorlighetsgrad = alvorlighetsgrad
        }
}
