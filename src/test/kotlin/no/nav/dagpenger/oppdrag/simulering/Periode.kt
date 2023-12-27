package no.nav.dagpenger.oppdrag.simulering

import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate

enum class PeriodeType {
    YTELSE, OPPHØR, ØKNING, REDUKSJON
}

data class Periode(
    var fom: LocalDate,
    var tom: LocalDate,
    val sats: BigDecimal? = null,
    var oldSats: BigDecimal? = null,
    val typeSats: String? = null,
    var periodeType: PeriodeType? = null,
    val kodeKlassifik: String? = null,
) : Comparable<Periode> {
    init {
        require(fom.isBefore(tom) || fom.isEqual(tom)) { "fom må være før eller lik tom" }

        oldSats?.let { oldSats ->
            periodeType = if (sats != null && oldSats <= sats) {
                PeriodeType.ØKNING
            } else {
                PeriodeType.REDUKSJON
            }
        }
    }

    override fun compareTo(other: Periode): Int {
        return fom.compareTo(other.fom)
    }

    val antallVirkedager: Int
        get() {
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
