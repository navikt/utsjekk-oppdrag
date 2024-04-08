package no.nav.utsjekk.oppdrag.iverksetting.mq

import io.mockk.called
import io.mockk.verify
import no.nav.utsjekk.kontrakter.felles.Satstype
import no.nav.utsjekk.oppdrag.MQInitializer
import no.nav.utsjekk.oppdrag.MQInitializer.Companion.TESTKØ
import no.nav.utsjekk.oppdrag.iverksetting.domene.Endringskode
import no.nav.utsjekk.oppdrag.iverksetting.domene.OppdragSkjemaConstants
import no.nav.utsjekk.oppdrag.iverksetting.domene.Utbetalingsfrekvens
import no.nav.utsjekk.oppdrag.iverksetting.domene.tilOppdragskode
import no.nav.utsjekk.oppdrag.iverksetting.domene.toXMLDate
import no.trygdeetaten.skjema.oppdrag.ObjectFactory
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import no.trygdeetaten.skjema.oppdrag.Oppdrag110
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jms.core.JmsTemplate
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals

@Testcontainers
@ContextConfiguration(initializers = [MQInitializer::class])
class OppdragMQSenderTest {
    private lateinit var jmsTemplate: JmsTemplate

    companion object {
        @Container
        val ibmMQContainer = MQInitializer.container
    }

    @BeforeEach
    fun setup() {
        jmsTemplate = MQInitializer.getJmsTemplate()
    }

    @Test
    fun `skal sende oppdrag når skrudd på`() {
        val oppdragSender = OppdragSender(jmsTemplate, "true", TESTKØ)
        val delytelseId = "123456789"
        val fagsakId = oppdragSender.sendOppdrag(lagTestOppdrag(delytelseId))

        assertEquals(delytelseId, fagsakId)
    }

    @Test
    fun `skal ikke sende oppdrag når skrudd av`() {
        val oppdragSender = OppdragSender(jmsTemplate, "false", TESTKØ)

        Assertions.assertThrows(UnsupportedOperationException::class.java) {
            oppdragSender.sendOppdrag(lagTestOppdrag())
        }

        verify { jmsTemplate wasNot called }
    }

    private fun lagTestOppdrag(delytelseId: String = "123456789"): Oppdrag {
        val avstemmingsTidspunkt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS"))
        val objectFactory = ObjectFactory()

        val testOppdragsLinje150 =
            objectFactory.createOppdragsLinje150().apply {
                kodeEndringLinje = Endringskode.NY.kode
                vedtakId = avstemmingsTidspunkt
                this.delytelseId = delytelseId
                kodeKlassifik = "BATR"
                datoVedtakFom = LocalDate.now().toXMLDate()
                datoVedtakTom = LocalDate.now().plusDays(1).toXMLDate()
                sats = 1054.toBigDecimal()
                fradragTillegg = OppdragSkjemaConstants.FRADRAG_TILLEGG
                typeSats = Satstype.MÅNEDLIG.tilOppdragskode()
                brukKjoreplan = OppdragSkjemaConstants.BRUK_KJØREPLAN_DEFAULT
                saksbehId = "Z999999"
                utbetalesTilId = "12345678911"
                henvisning = "987654321"
                attestant180.add(
                    objectFactory.createAttestant180().apply {
                        attestantId = "Z999999"
                    },
                )
            }

        val testOppdrag110 =
            Oppdrag110().apply {
                kodeAksjon = "1"
                kodeEndring = Endringskode.NY.kode
                kodeFagomraade = "BA"
                fagsystemId = delytelseId
                utbetFrekvens = Utbetalingsfrekvens.MÅNEDLIG.kode
                oppdragGjelderId = "12345678911"
                datoOppdragGjelderFom = OppdragSkjemaConstants.OPPDRAG_GJELDER_DATO_FOM.toXMLDate()
                saksbehId = "Z999999"
                oppdragsEnhet120.add(
                    objectFactory.createOppdragsEnhet120().apply {
                        enhet = OppdragSkjemaConstants.ENHET
                        typeEnhet = OppdragSkjemaConstants.ENHET_TYPE_BOSTEDSENHET
                        datoEnhetFom = OppdragSkjemaConstants.ENHET_FOM.toXMLDate()
                    },
                )
                avstemming115 =
                    objectFactory.createAvstemming115().apply {
                        nokkelAvstemming = avstemmingsTidspunkt
                        kodeKomponent = "BA"
                        tidspktMelding = avstemmingsTidspunkt
                    }
                oppdragsLinje150.add(testOppdragsLinje150)
            }

        return objectFactory.createOppdrag().apply {
            oppdrag110 = testOppdrag110
        }
    }
}
