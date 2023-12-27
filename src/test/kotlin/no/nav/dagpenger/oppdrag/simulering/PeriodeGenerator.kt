package no.nav.dagpenger.oppdrag.simulering

import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.Oppdragslinje
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object PeriodeGenerator {

    fun genererPerioder(oppdragslinjer: List<Oppdragslinje>): List<Periode> {
        validerOppdragslinjer(oppdragslinjer)

        val opphørsPerioder = oppdragslinjer.filter { it.kodeEndringLinje == "ENDR" }.map(Oppdragslinje::tilPeriode).sorted()
        val ytelsesPerioder = oppdragslinjer.filter { it.kodeEndringLinje == "NY" }.map(Oppdragslinje::tilPeriode).sorted()

        if (opphørsPerioder.isEmpty() && ytelsesPerioder.isEmpty()) {
            return emptyList()
        }

        if (opphørsPerioder.isEmpty()) {
            return ytelsesPerioder.map { it.copy(periodeType = PeriodeType.YTELSE) }
        }

        if (ytelsesPerioder.isEmpty()) {
            return opphørsPerioder.map { it.copy(periodeType = PeriodeType.OPPHØR) }
        }

        return (
            utledYtelsesperioder(opphørsPerioder, ytelsesPerioder) + utledOpphørsperioder(
                opphørsPerioder,
                ytelsesPerioder
            )
            ).sorted()
    }

    private fun utledOpphørsperioder(
        opphørsperioder: List<Periode>,
        ytelsesperioder: List<Periode>,
    ): List<Periode> {
        val resultat = ArrayList<Periode>()
        val temp = ArrayList(opphørsperioder)

        while (temp.isNotEmpty()) {
            val perioderSomSkalFjernes = mutableSetOf<Periode>()
            val perioderSomSkalLeggesTil = mutableListOf<Periode>()

            for (opphør in temp) {
                for (ytelse in ytelsesperioder) {
                    if (!opphør.overlapperMed(ytelse)) {
                        resultat.add(opphør.copy(periodeType = PeriodeType.OPPHØR))
                        perioderSomSkalFjernes.add(opphør)
                        break
                    } else if (opphør.tilstøterFør(ytelse) || opphør.overlapperMedFomTil(ytelse)) {
                        resultat.add(
                            opphør.copy(
                                tom = opphør.tom.minusDays(1),
                                periodeType = PeriodeType.OPPHØR,
                            )
                        )
                        perioderSomSkalFjernes.add(opphør)
                        break
                    } else if (ytelse.omslutterInklusiv(opphør)) {
                        perioderSomSkalFjernes.add(opphør)
                        break
                    } else if (opphør.omslutter(ytelse) || opphør.overlapperMedTomTil(ytelse)) {
                        perioderSomSkalFjernes.add(opphør)
                        perioderSomSkalLeggesTil.add(opphør.copy(fom = ytelse.tom.plusDays(1)))
                        break
                    }
                }
            }

            temp.removeAll(perioderSomSkalFjernes)
            temp.addAll(perioderSomSkalLeggesTil)
            temp.sort()
        }

        return resultat.sorted()
    }

    private fun utledYtelsesperioder(
        opphørsPerioder: List<Periode>,
        ytelsesPerioder: List<Periode>,
    ): List<Periode> {
        val resultat = ArrayList<Periode>()
        val temp = ArrayList(ytelsesPerioder)

        while (temp.isNotEmpty()) {
            val perioderSomSkalFjernes = mutableSetOf<Periode>()
            val perioderSomSkalLeggesTil = ArrayList<Periode>()

            for (ytelse in temp) {
                for (opphør in opphørsPerioder) {
                    if (!ytelse.overlapperMed(opphør)) {
                        resultat.add(ytelse.copy(periodeType = PeriodeType.YTELSE))
                        perioderSomSkalFjernes.add(ytelse)
                        break
                    } else if (ytelse.tilstøterFør(opphør)) {
                        resultat.add(
                            ytelse.copy(
                                tom = ytelse.tom.minusDays(1),
                                periodeType = PeriodeType.YTELSE,
                            )
                        )
                        if (opphør.sats != null) {
                            resultat.add(ytelse.copy(oldSats = opphør.sats))
                        }
                        perioderSomSkalFjernes.add(ytelse)
                        break
                    } else if (ytelse.overlapperMedFomTil(opphør)) {
                        resultat.add(
                            ytelse.copy(
                                tom = opphør.fom.minusDays(1),
                                periodeType = PeriodeType.YTELSE,
                            )
                        )
                        if (ytelse.omslutterInklusiv(opphør)) {
                            if (opphør.sats != null) {
                                resultat.add(
                                    ytelse.copy(
                                        fom = opphør.fom,
                                        tom = opphør.tom,
                                        oldSats = opphør.sats,
                                    )
                                )
                            }
                            perioderSomSkalLeggesTil.add(ytelse.copy(fom = opphør.tom.plusDays(1)))
                        } else {
                            if (opphør.sats != null) {
                                resultat.add(
                                    ytelse.copy(
                                        fom = opphør.fom,
                                        oldSats = opphør.sats,
                                    )
                                )
                            }
                        }
                        perioderSomSkalFjernes.add(ytelse)
                        break
                    } else if (opphør.omslutterInklusiv(ytelse)) {
                        if (opphør.sats != null) {
                            resultat.add(ytelse.copy(oldSats = opphør.sats))
                        }
                        perioderSomSkalFjernes.add(ytelse)
                        break
                    } else if (ytelse.overlapperMedTomTil(opphør)) {
                        if (opphør.sats != null) {
                            resultat.add(
                                ytelse.copy(
                                    tom = opphør.tom,
                                    oldSats = opphør.sats,
                                )
                            )
                        }
                        perioderSomSkalLeggesTil.add(ytelse.copy(fom = opphør.tom.plusDays(1)))
                        perioderSomSkalFjernes.add(ytelse)
                        break
                    } else if (opphør.omslutter(ytelse)) {
                        if (opphør.sats != null) {
                            resultat.add(ytelse.copy(oldSats = opphør.sats,))
                        }
                        perioderSomSkalFjernes.add(ytelse)
                        break
                    }
                }
            }

            temp.removeAll(perioderSomSkalFjernes.toSet())
            temp.addAll(perioderSomSkalLeggesTil)
            temp.sort()
        }

        return resultat
    }

    private fun validerOppdragslinjer(linjer: List<Oppdragslinje>) {
        for (linje in linjer) {
            assert(linje.typeSats == "MND" || linje.typeSats == "DAG") {
                "Forventet at typeSats er enten DAG eller MND, men typeSats var: " + linje.typeSats
            }
            assert(linje.kodeEndringLinje == "NY" || linje.kodeEndringLinje == "ENDR") {
                "Forventet kodeEndringLinje NY eller ENDR. Verdi var: " + linje.kodeEndringLinje
            }
        }
    }
}

private fun Periode.overlapperMed(periode: Periode) =
    !(tom.isBefore(periode.fom) || fom.isAfter(periode.tom))

private fun Periode.tilstøterFør(periode: Periode) =
    tom.isEqual(periode.fom)

private fun Periode.overlapperMedFomTil(periode: Periode) =
    fom.isBefore(periode.fom) && tom.isAfter(periode.fom)

private fun Periode.overlapperMedTomTil(periode: Periode) =
    (fom.isBefore(periode.tom)) && !fom.isAfter(periode.tom)

private fun Periode.omslutter(periode: Periode) =
    fom.isBefore(periode.fom) && tom.isAfter(periode.tom)

private fun Periode.omslutterInklusiv(periode: Periode) =
    (fom.isBefore(periode.fom) || fom.isEqual(periode.fom)) && (tom.isAfter(periode.tom) || tom.isEqual(periode.tom))

private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

private fun Oppdragslinje.tilPeriode() =
    Periode(
        fom = LocalDate.parse(datoStatusFom ?: datoVedtakFom, dateTimeFormatter),
        tom = LocalDate.parse(datoVedtakTom, dateTimeFormatter),
        sats = sats,
        typeSats = typeSats,
        kodeKlassifik = kodeKlassifik,
    )
