package no.nav.utsjekk.oppdrag

import com.ibm.mq.jakarta.jms.MQConnectionFactory
import com.ibm.msg.client.jakarta.wmq.WMQConstants
import io.mockk.spyk
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter
import org.springframework.jms.core.JmsTemplate
import org.testcontainers.containers.GenericContainer

class MQInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    companion object {
        const val TESTKØ = "DEV.QUEUE.1"

        val container: GenericMQContainer =
            GenericMQContainer("ibmcom/mq")
                .withEnv("LICENSE", "accept")
                .withEnv("MQ_QMGR_NAME", "QM1")
                .withEnv("persistence.enabled", "true")
                .withExposedPorts(1414)

        fun getJmsTemplate() =
            MQConnectionFactory().apply {
                hostName = "localhost"
                port = container.getMappedPort(1414)
                channel = "DEV.ADMIN.SVRCONN"
                queueManager = "QM1"
                transportType = WMQConstants.WMQ_CM_CLIENT
            }.let { factory ->
                UserCredentialsConnectionFactoryAdapter().apply {
                    setUsername("admin")
                    setPassword("passw0rd")
                    setTargetConnectionFactory(factory)
                }
            }.let { adapter ->
                spyk(JmsTemplate(adapter).apply { defaultDestinationName = TESTKØ })
            }
    }

    override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
        TestPropertyValues.of(
            "oppdrag.mq.port=" + container.getMappedPort(1414),
            "oppdrag.mq.queuemanager=QM1",
            "oppdrag.mq.send=DEV.QUEUE.1",
            "oppdrag.mq.mottak=DEV.QUEUE.1",
            "oppdrag.mq.channel=DEV.ADMIN.SVRCONN",
            "oppdrag.mq.hostname=localhost",
            "oppdrag.mq.user=admin",
            "oppdrag.mq.password: passw0rd",
            "oppdrag.mq.enabled: true",
        ).applyTo(configurableApplicationContext.environment)
    }
}

class GenericMQContainer(imageName: String) : GenericContainer<GenericMQContainer>(imageName)
