package no.nav.dagpenger.simulering.simulering.dto

import no.nav.dagpenger.kontrakter.felles.Ident
import no.nav.dagpenger.kontrakter.felles.Personident
import java.time.LocalDate

data class SimuleringRequestBody(
    val fagområde: String,
    val fagsystemId: String,
    val personident: Personident,
    val mottaker: Ident,
    val endringskode: Endringskode,
    val saksbehandler: String,
    val utbetalingsfrekvens: Utbetalingsfrekvens,
    val utbetalingslinjer: List<Utbetalingslinje>,
)

data class Utbetalingslinje(
    val delytelseId: String,
    val endringskode: Endringskode,
    val klassekode: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val sats: Int,
    val grad: Int?,
    val refDelytelseId: String?,
    val refFagsystemId: String?,
    val datoStatusFom: LocalDate?,
    val statuskode: String?,
    val satstype: Satstype,
)
