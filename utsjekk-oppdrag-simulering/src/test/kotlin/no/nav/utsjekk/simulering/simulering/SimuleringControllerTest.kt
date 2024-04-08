package no.nav.utsjekk.simulering.simulering

import io.mockk.every
import io.mockk.mockk
import no.nav.system.os.entiteter.beregningskjema.Beregning
import no.nav.system.os.entiteter.beregningskjema.BeregningStoppnivaa
import no.nav.system.os.entiteter.beregningskjema.BeregningStoppnivaaDetaljer
import no.nav.system.os.entiteter.beregningskjema.BeregningsPeriode
import no.nav.system.os.tjenester.simulerfpservice.feil.FeilUnderBehandling
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningFeilUnderBehandling
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningRequest
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerBeregningResponse
import no.nav.system.os.tjenester.simulerfpservice.simulerfpservicegrensesnitt.SimulerFpService
import no.nav.utsjekk.kontrakter.felles.Personident
import no.nav.utsjekk.simulering.simulering.dto.Endringskode
import no.nav.utsjekk.simulering.simulering.dto.Satstype
import no.nav.utsjekk.simulering.simulering.dto.SimuleringRequestBody
import no.nav.utsjekk.simulering.simulering.dto.Utbetalingsfrekvens
import no.nav.utsjekk.simulering.simulering.dto.Utbetalingslinje
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.SimulerBeregningResponse as InnerSimulerBeregningResponse

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SimuleringControllerTest {
    private val simulerFpService: SimulerFpService = mockk()
    private val simuleringService = SimuleringService(simulerFpService)
    private val simuleringController = SimuleringController(simuleringService)

    @Test
    fun `svarer med 200 OK og simuleringsresultat`() {
        val requestBody = enSimuleringRequestBody()
        val request = SimuleringRequestBuilder(requestBody).build()

        every {
            simulerFpService.simulerBeregning(any())
        } answers {
            enSimulerBeregningResponse(request)
        }

        simuleringController.postSimulering(requestBody).also {
            assertEquals(200, it.statusCode.value())
        }
    }

    @Test
    fun `svarer med 400 Bad Request ved feil på request body`() {
        every {
            simulerFpService.simulerBeregning(any())
        } throws
            SimulerBeregningFeilUnderBehandling(
                "Fødselsnummeret er ugyldig",
                FeilUnderBehandling().apply {
                    errorMessage = "Fødselsnummeret er ugyldig"
                },
            )

        assertThrows<ResponseStatusException> {
            simuleringController.postSimulering(enSimuleringRequestBody())
        }
    }

    @Test
    fun `svarer med 404 Not Found når personen ikke finnes`() {
        every {
            simulerFpService.simulerBeregning(any())
        } throws
            SimulerBeregningFeilUnderBehandling(
                "Personen finnes ikke",
                FeilUnderBehandling().apply {
                    errorMessage = "Personen finnes ikke"
                },
            )

        assertThrows<ResponseStatusException> {
            simuleringController.postSimulering(enSimuleringRequestBody())
        }
    }

    private fun enSimuleringRequestBody() =
        SimuleringRequestBody(
            fagområde = "TEST",
            fagsystemId = "FAGSYSTEM",
            personident = Personident("15507600333"),
            mottaker = Personident("15507600333"),
            endringskode = Endringskode.NY,
            saksbehandler = "TEST",
            utbetalingsfrekvens = Utbetalingsfrekvens.UKENTLIG,
            utbetalingslinjer =
            listOf(
                Utbetalingslinje(
                    delytelseId = "",
                    endringskode = Endringskode.NY,
                    klassekode = "",
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 30),
                    sats = 1000,
                    grad = 100,
                    refDelytelseId = null,
                    refFagsystemId = null,
                    datoStatusFom = null,
                    statuskode = null,
                    satstype = Satstype.MÅNED,
                ),
            ),
        )

    private fun enSimulerBeregningResponse(request: SimulerBeregningRequest) =
        SimulerBeregningResponse().apply {
            response =
                InnerSimulerBeregningResponse().apply {
                    simulering =
                        Beregning().apply {
                            gjelderId = request.request.oppdrag.oppdragGjelderId
                            gjelderNavn = "Navn Navnesen"
                            datoBeregnet = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                            kodeFaggruppe = "KORTTID"
                            belop = BigDecimal("1234")
                            beregningsPeriode.add(
                                BeregningsPeriode().apply {
                                    periodeFom = request.request.simuleringsPeriode.datoSimulerFom
                                    periodeTom = request.request.simuleringsPeriode.datoSimulerTom
                                    beregningStoppnivaa.add(
                                        BeregningStoppnivaa().apply {
                                            kodeFagomraade = "et fagrområde"
                                            stoppNivaaId = BigInteger("1")
                                            behandlendeEnhet = "8052"
                                            oppdragsId = 1234
                                            fagsystemId = "en fagsystem-ID"
                                            kid = "12345"
                                            utbetalesTilId = request.request.oppdrag.oppdragGjelderId
                                            utbetalesTilNavn = "En Arbeidsgiver AS"
                                            bilagsType = "U"
                                            forfall = "2023-12-28"
                                            isFeilkonto = false
                                            beregningStoppnivaaDetaljer.add(
                                                BeregningStoppnivaaDetaljer().apply {
                                                    faktiskFom = request.request.simuleringsPeriode.datoSimulerFom
                                                    faktiskTom = request.request.simuleringsPeriode.datoSimulerTom
                                                    kontoStreng = "1235432"
                                                    behandlingskode = "2"
                                                    belop = BigDecimal("1000")
                                                    trekkVedtakId = 0
                                                    stonadId = "1234"
                                                    korrigering = ""
                                                    isTilbakeforing = false
                                                    linjeId = BigInteger("21423")
                                                    sats = BigDecimal("1000")
                                                    typeSats = "MND"
                                                    antallSats = BigDecimal("21")
                                                    saksbehId = "5323"
                                                    uforeGrad = BigInteger("100")
                                                    kravhaverId = ""
                                                    delytelseId = "5323"
                                                    bostedsenhet = "4643"
                                                    skykldnerId = ""
                                                    klassekode = ""
                                                    klasseKodeBeskrivelse = "en kladdekodebeskrivelse"
                                                    typeKlasse = "YTEL"
                                                    typeKlasseBeskrivelse = "en typeklassebeskrivelse"
                                                    refunderesOrgNr = ""
                                                },
                                            )
                                        },
                                    )
                                },
                            )
                        }
                    infomelding = null
                }
        }
}
