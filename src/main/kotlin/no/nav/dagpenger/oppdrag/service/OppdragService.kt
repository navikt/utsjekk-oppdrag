package no.nav.dagpenger.oppdrag.service

import no.nav.dagpenger.kontrakter.utbetaling.OppdragId
import no.nav.dagpenger.kontrakter.utbetaling.Utbetalingsoppdrag
import no.nav.dagpenger.oppdrag.repository.OppdragLager
import no.trygdeetaten.skjema.oppdrag.Oppdrag

interface OppdragService {
    fun opprettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag, oppdrag: Oppdrag, versjon: Int)
    fun hentStatusForOppdrag(oppdragId: OppdragId): OppdragLager
}
