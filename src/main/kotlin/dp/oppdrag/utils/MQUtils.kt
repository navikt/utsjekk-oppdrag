package dp.oppdrag.utils

import com.ibm.mq.jms.MQQueueConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import javax.jms.QueueConnection

fun createQueueConnection(): QueueConnection {
    val qcf = MQQueueConnectionFactory()
    qcf.hostName = System.getenv("MQ_HOSTNAME")
    qcf.port = System.getenv("MQ_PORT").toInt()
    qcf.channel = System.getenv("MQ_CHANNEL")
    qcf.transportType = WMQConstants.WMQ_CM_CLIENT
    qcf.queueManager = System.getenv("MQ_QUEUEMANAGER")

    return qcf.createQueueConnection(System.getenv("MQ_USER"), System.getenv("MQ_PASSWORD"))
}
