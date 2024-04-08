package no.nav.utsjekk.simulering.simulering.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.utsjekk.kontrakter.felles.Ident
import no.nav.utsjekk.kontrakter.felles.Personident
import java.time.LocalDate

data class SimuleringRequestBody(
    val fagområde: String,
    val fagsystemId: String,
    @Schema(example = "15507600333", type = "string")
    val personident: Personident,
    @Schema(example = "15507600333", type = "string")
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
