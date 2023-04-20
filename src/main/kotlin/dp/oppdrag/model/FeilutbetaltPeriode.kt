package dp.oppdrag.model

import java.math.BigDecimal
import java.time.LocalDate

class FeilutbetaltPeriode(
    val feilutbetaltBeloep: BigDecimal,
    val fom: LocalDate,
    val nyttBeloep: BigDecimal,
    val tidligereUtbetaltBeloep: BigDecimal,
    val tom: LocalDate
)
