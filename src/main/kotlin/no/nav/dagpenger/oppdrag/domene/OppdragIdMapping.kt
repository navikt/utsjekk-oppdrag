package no.nav.dagpenger.oppdrag.domene

import no.nav.dagpenger.kontrakter.utbetaling.tilFagsystem
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import java.util.*

val Oppdrag.id: OppdragId
    get() = OppdragId(
        this.oppdrag110.kodeFagomraade.tilFagsystem(),
        this.oppdrag110.oppdragGjelderId,
        UUID.fromString(this.oppdrag110.oppdragsLinje150?.get(0)?.henvisning!!)
    )
