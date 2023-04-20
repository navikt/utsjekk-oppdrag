package dp.oppdrag.repository

import dp.oppdrag.model.SimuleringLager

interface SimuleringLagerRepository {

    fun lagreINyTransaksjon(simuleringLager: SimuleringLager)
    fun oppdater(simuleringLager: SimuleringLager)
    fun finnAlleSimuleringsLager(): List<SimuleringLager>
    fun hentSisteSimuleringsresultat(fagsakId: String, behandlingId: String): SimuleringLager
}
