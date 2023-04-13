package dp.oppdrag.model

data class PerioderForBehandling(
    val behandlingId: String,
    val perioder: Set<Long>,
    val aktivFoedselsnummer: String
)
