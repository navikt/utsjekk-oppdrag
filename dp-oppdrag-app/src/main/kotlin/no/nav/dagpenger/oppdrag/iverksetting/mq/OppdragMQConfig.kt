package no.nav.dagpenger.oppdrag.iverksetting.mq

import com.ibm.mq.constants.CMQC.MQENC_NATIVE
import com.ibm.mq.jakarta.jms.MQQueueConnectionFactory
import com.ibm.msg.client.jakarta.jms.JmsConstants
import com.ibm.msg.client.jakarta.jms.JmsConstants.JMS_IBM_CHARACTER_SET
import com.ibm.msg.client.jakarta.jms.JmsConstants.JMS_IBM_ENCODING
import com.ibm.msg.client.jakarta.wmq.common.CommonConstants.WMQ_CM_CLIENT
import jakarta.jms.ConnectionFactory
import jakarta.jms.JMSException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer
import org.springframework.boot.autoconfigure.jms.JmsPoolConnectionFactoryFactory
import org.springframework.boot.autoconfigure.jms.JmsPoolConnectionFactoryProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jms.config.DefaultJmsListenerContainerFactory
import org.springframework.jms.connection.JmsTransactionManager
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter
import org.springframework.jms.core.JmsTemplate

private const val UTF_8_WITH_PUA = 1208

@Configuration
internal class OppdragMQConfig(
    @Value("\${oppdrag.mq.hostname}") hostname: String,
    @Value("\${oppdrag.mq.queuemanager}") queuemanager: String,
    @Value("\${oppdrag.mq.channel}") mqChannel: String,
    @Value("\${oppdrag.mq.send}") sendQueue: String,
    @Value("\${oppdrag.mq.avstemming}") avstemmingQueue: String,
    @Value("\${oppdrag.mq.port}") mqPort: Int,
    @Value("\${oppdrag.mq.user}") user: String,
    @Value("\${oppdrag.mq.password}") password: String,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(OppdragMQConfig::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }

    private val mqProperties =
        MQConfigProperties(
            hostname = hostname,
            queuemanager = queuemanager,
            channel = mqChannel,
            send = sendQueue,
            avstemming = avstemmingQueue,
            port = mqPort,
            user = user,
            password = password,
        )

    @Bean
    @Throws(JMSException::class)
    fun mqQueueConnectionFactory(): ConnectionFactory {
        val targetFactory =
            MQQueueConnectionFactory().apply {
                this.hostName = mqProperties.hostname
                this.queueManager = mqProperties.queuemanager
                this.channel = mqProperties.channel
                this.port = mqProperties.port
                this.transportType = WMQ_CM_CLIENT
                this.ccsid = UTF_8_WITH_PUA
                setIntProperty(JMS_IBM_ENCODING, MQENC_NATIVE)
                setBooleanProperty(JmsConstants.USER_AUTHENTICATION_MQCSP, true)
                setIntProperty(JMS_IBM_CHARACTER_SET, UTF_8_WITH_PUA)
            }

        val cf =
            UserCredentialsConnectionFactoryAdapter().apply {
                setUsername(mqProperties.user)
                setPassword(mqProperties.password)
                setTargetConnectionFactory(targetFactory)
            }

        val pooledFactoryConfig =
            JmsPoolConnectionFactoryProperties().apply {
                maxConnections = 10
                maxSessionsPerConnection = 10
            }
        val pooledFactoryFactory = JmsPoolConnectionFactoryFactory(pooledFactoryConfig)

        return pooledFactoryFactory.createPooledConnectionFactory(cf)
    }

    @Bean
    fun jmsTemplateUtgående(mqQueueConnectionFactory: ConnectionFactory) =
        JmsTemplate(mqQueueConnectionFactory).apply {
            defaultDestinationName = mqProperties.send
            isSessionTransacted = true
        }

    @Bean
    fun jmsListenerContainerFactory(
        mqQueueConnectionFactory: ConnectionFactory,
        configurer: DefaultJmsListenerContainerFactoryConfigurer,
    ) = DefaultJmsListenerContainerFactory().apply {
        configurer.configure(this, mqQueueConnectionFactory)
        setSessionTransacted(true)
        setTransactionManager(
            JmsTransactionManager().apply {
                connectionFactory = mqQueueConnectionFactory
            },
        )
        setErrorHandler {
            logger.error("Feilet håndtering av melding, se secureLogs")
            secureLogger.error("Feilet håndtering av melding", it)
        }
        setExceptionListener {
            logger.error("Feilet lytting av kø, se secureLogs")
            secureLogger.error("Feilet lytting av kø", it)
        }
    }

    @Bean
    fun jmsTemplateAvstemming(mqQueueConnectionFactory: ConnectionFactory) =
        JmsTemplate(mqQueueConnectionFactory).apply { defaultDestinationName = mqProperties.avstemming }
}

private data class MQConfigProperties(
    val hostname: String,
    val queuemanager: String,
    val channel: String,
    val send: String,
    val avstemming: String,
    val port: Int,
    val user: String,
    val password: String,
)
