package no.nav.dagpenger.oppdrag.domene

import no.nav.dagpenger.kontrakter.felles.tilFagsystem
import no.nav.dagpenger.kontrakter.oppdrag.OppdragId
import no.nav.dagpenger.oppdrag.iverksetting.UuidUtils.dekomprimer
import no.trygdeetaten.skjema.oppdrag.Oppdrag

val Oppdrag.id: OppdragId
    get() = OppdragId(
        this.oppdrag110.kodeFagomraade.tilFagsystem(),
        this.oppdrag110.oppdragGjelderId,
        this.oppdrag110.oppdragsLinje150?.get(0)?.henvisning!!.dekomprimer()
    )
