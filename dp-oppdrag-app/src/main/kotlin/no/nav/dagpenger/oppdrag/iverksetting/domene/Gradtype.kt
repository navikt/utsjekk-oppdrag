package no.nav.dagpenger.oppdrag.iverksetting.domene

@Suppress("unused")
internal enum class Gradtype(val kode: String) {
    UFØREGRAD("UFOR"),
    UTBETALINGSGRAD("UBGR"),
    UTTAKSGRAD_ALDERSPENSJON("UTAP"),
    UTTAKSGRAD_AFP("AFPG"),
}
