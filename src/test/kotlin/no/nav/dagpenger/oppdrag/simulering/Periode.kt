package no.nav.dagpenger.oppdrag.simulering

import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate

enum class PeriodeType {
    YTEL, OPPH, ØKNING, REDUKSJON
}

data class Periode(
    val fom: LocalDate,
    var tom: LocalDate,
    val sats: BigDecimal? = null,
    val gammelSats: BigDecimal? = null,
    val typeSats: String,
    var periodeType: PeriodeType? = null,
    val kodeKlassifik: String? = null,
) : Comparable<Periode> {
    override fun compareTo(other: Periode): Int {
        return fom.compareTo(other.fom)
    }

    val antallVirkedager: Int
        get() {
            (fom..tom)
            var startDato = fom
            var antallVirkedager = 0
            while (startDato.isBefore(tom) || startDato.isEqual(tom)) {
                if (startDato.dayOfWeek != DayOfWeek.SATURDAY && startDato.dayOfWeek != DayOfWeek.SUNDAY) {
                    antallVirkedager++
                }
                startDato = startDato.plusDays(1)
            }
            return antallVirkedager
        }
}
