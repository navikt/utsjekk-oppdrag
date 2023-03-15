package dp.oppdrag.listener

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.ibm.mq.jms.MQQueue
import dp.oppdrag.defaultLogger
import dp.oppdrag.model.OppdragId
import dp.oppdrag.model.OppdragLagerStatus
import dp.oppdrag.model.OppdragStatus
import dp.oppdrag.repository.OppdragLagerRepositoryJdbc
import dp.oppdrag.utils.createQueueConnection
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import javax.jms.*


class OppdragListenerMQ(private val oppdragLagerRepository: OppdragLagerRepositoryJdbc) : MessageListener {

    init {
        if (!System.getenv("MQ_ENABLED").toBoolean()) {
            defaultLogger.info { "MQ-integrasjon mot oppdrag er skrudd av" }
        } else {
            val queue = MQQueue(System.getenv("MQ_MOTTAK"))
            val queueConnection = createQueueConnection()
            val queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE)
            val queueReceiver = queueSession.createReceiver(queue)
            queueReceiver.messageListener = this
            queueConnection.start()
        }
    }

    override fun onMessage(message: Message?) {

        try {
            if (message is TextMessage) {
                processMessage(message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun processMessage(message: TextMessage) {
        defaultLogger.info("String message recieved >> " + message.text)

        val xmlMapper = XmlMapper()
        val svarFraOppdrag = message.text
        val kvittering = xmlMapper.readValue(svarFraOppdrag, Oppdrag::class.java)
        val oppdragId = OppdragId(
            kvittering.oppdrag110.kodeFagomraade,
            kvittering.oppdrag110.oppdragGjelderId,
            kvittering.oppdrag110.oppdragsLinje150?.get(0)?.henvisning!!
        )
        val oppdragStatus = mapOppdragStatus(kvittering)

        val foersteOppdragUtenKvittering = oppdragLagerRepository.hentAlleVersjonerAvOppdrag(oppdragId)
            .find { oppdrag -> oppdrag.status == OppdragLagerStatus.LAGT_PAA_KOE }
        if (foersteOppdragUtenKvittering == null) {
            defaultLogger.warn { "Oppdraget tilknyttet mottatt kvittering har uventet status i databasen. Oppdraget er: $oppdragId" }
            return
        }

        if (kvittering.mmel != null) {
            oppdragLagerRepository.oppdaterKvitteringsmelding(
                oppdragId,
                kvittering.mmel,
                foersteOppdragUtenKvittering.versjon
            )
        }

        oppdragLagerRepository.oppdaterStatus(
            oppdragId,
            oppdragStatus,
            foersteOppdragUtenKvittering.versjon
        )
    }

    private fun mapOppdragStatus(kvittering: Oppdrag): OppdragLagerStatus {

        return when (OppdragStatus.fraKode(kvittering.mmel?.alvorlighetsgrad ?: "Ukjent")) {
            OppdragStatus.OK -> OppdragLagerStatus.KVITTERT_OK
            OppdragStatus.AKSEPTERT_MEN_NOE_ER_FEIL -> OppdragLagerStatus.KVITTERT_MED_MANGLER
            OppdragStatus.AVVIST_FUNKSJONELLE_FEIL -> OppdragLagerStatus.KVITTERT_FUNKSJONELL_FEIL
            OppdragStatus.AVVIST_TEKNISK_FEIL -> OppdragLagerStatus.KVITTERT_TEKNISK_FEIL
            OppdragStatus.UKJENT -> OppdragLagerStatus.KVITTERT_UKJENT
        }
    }
}
