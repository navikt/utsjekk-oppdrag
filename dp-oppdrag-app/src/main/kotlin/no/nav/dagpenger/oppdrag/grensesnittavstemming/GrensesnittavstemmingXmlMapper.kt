package no.nav.dagpenger.oppdrag.grensesnittavstemming

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.Marshaller
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import java.io.StringWriter

internal object GrensesnittavstemmingXmlMapper {
    private val jaxbContext: JAXBContext = JAXBContext.newInstance(Avstemmingsdata::class.java)

    fun tilXml(avstemming: Avstemmingsdata) =
        StringWriter().also { stringWriter ->
            jaxbContext.createMarshaller().apply {
                setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
            }.marshal(avstemming, stringWriter)
        }.toString()
}
