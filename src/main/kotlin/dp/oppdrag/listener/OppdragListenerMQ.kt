package dp.oppdrag.listener

import com.ibm.mq.jms.MQQueue
import dp.oppdrag.defaultLogger
import dp.oppdrag.utils.createQueueConnection
import javax.jms.*


class OppdragListenerMQ : MessageListener {

    init {
        val queue = MQQueue(System.getenv("MQ_MOTTAK"))
        val queueConnection = createQueueConnection()
        val queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE)
        val queueReceiver = queueSession.createReceiver(queue)
        queueReceiver.messageListener = this
        queueConnection.start()
    }

    override fun onMessage(message: Message?) {

        try {
            if (message is TextMessage) {
                defaultLogger.info("String message recieved >> " + message.text)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
