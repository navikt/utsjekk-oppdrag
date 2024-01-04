package no.nav.dagpenger.oppdrag.service

import no.nav.dagpenger.kontrakter.oppdrag.OppdragId
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsoppdrag
import no.nav.dagpenger.oppdrag.repository.OppdragLager
import no.trygdeetaten.skjema.oppdrag.Oppdrag

interface OppdragService {
    fun opprettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag, oppdrag: Oppdrag, versjon: Int)
    fun hentStatusForOppdrag(oppdragId: OppdragId): OppdragLager
}
