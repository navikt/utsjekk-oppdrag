package no.nav.dagpenger.oppdrag.grensesnittavstemming

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.Marshaller
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import java.io.StringWriter

object JaxbGrensesnittAvstemmingsdata {

    private val jaxbContext: JAXBContext = JAXBContext.newInstance(Avstemmingsdata::class.java)

    fun tilXml(avstemmingsmelding: Avstemmingsdata): String {
        val stringWriter = StringWriter()
        jaxbContext.createMarshaller().apply {
            setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
        }.marshal(avstemmingsmelding, stringWriter)
        return stringWriter.toString()
    }
}
