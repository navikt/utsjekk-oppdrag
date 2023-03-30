package dp.oppdrag.utils

import com.ibm.mq.jms.MQQueueConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import javax.jms.QueueConnection

fun createQueueConnection(): QueueConnection {
    val qcf = MQQueueConnectionFactory()
    qcf.hostName = getProperty("MQ_HOSTNAME")
    qcf.port = getProperty("MQ_PORT")?.toInt() ?: 0
    qcf.channel = getProperty("MQ_CHANNEL")
    qcf.transportType = WMQConstants.WMQ_CM_CLIENT
    qcf.queueManager = getProperty("MQ_QUEUEMANAGER")

    return qcf.createQueueConnection(getProperty("MQ_USER"), getProperty("MQ_PASSWORD"))
}
