package no.nav.dagpenger.oppdrag.domene

import no.nav.dagpenger.kontrakter.utbetaling.Fagsystem
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import java.lang.IllegalArgumentException
import java.util.UUID

val Oppdrag.id: OppdragId
    get() = OppdragId(
        this.oppdrag110.kodeFagomraade.tilFagsystem(),
        this.oppdrag110.oppdragGjelderId,
        UUID.fromString(this.oppdrag110.oppdragsLinje150?.get(0)?.henvisning!!)
    )

fun String.tilFagsystem(): Fagsystem = Fagsystem.values().find { it.kode == this }
    ?: throw IllegalArgumentException("$this er ukjent fagsystem")
