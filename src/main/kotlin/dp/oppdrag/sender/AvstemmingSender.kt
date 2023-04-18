package dp.oppdrag.sender

import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata

interface AvstemmingSender {
    fun sendGrensesnittAvstemming(avstemmingsdata: Avstemmingsdata)
}
