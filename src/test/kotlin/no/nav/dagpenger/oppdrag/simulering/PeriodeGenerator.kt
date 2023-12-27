package no.nav.dagpenger.oppdrag.simulering

import no.nav.system.os.entiteter.typer.simpletypes.KodeStatusLinje
import no.nav.system.os.tjenester.simulerfpservice.simulerfpserviceservicetypes.Oppdragslinje
import java.lang.IllegalArgumentException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Collections

class PeriodeGenerator {
    var opphørsPerioder: MutableList<Periode> = ArrayList()
    var ytelsesPerioder: MutableList<Periode> = ArrayList()
    fun genererPerioder(oppdragslinjeList: List<Oppdragslinje>): List<Periode>? {
        lagOpphørOgYtelse(oppdragslinjeList)
        Collections.sort(ytelsesPerioder)
        Collections.sort(opphørsPerioder)
        if (opphørsPerioder.isEmpty() && ytelsesPerioder.isEmpty()) {
            return null
        }
        if (opphørsPerioder.isEmpty()) {
            for (periode in ytelsesPerioder) {
                periode.periodeType = PeriodeType.YTEL
            }
            return ytelsesPerioder
        }
        if (ytelsesPerioder.isEmpty()) {
            for (periode in opphørsPerioder) {
                periode.periodeType = PeriodeType.OPPH
            }
            return opphørsPerioder
        }
        val perioder: MutableList<Periode> = ArrayList(definerYtelseOgOmpostPerioder())
        perioder.addAll(definerOpphørPerioder())
        Collections.sort(perioder)
        return perioder
    }

    private fun definerOpphørPerioder(): List<Periode> {
        val periodeList: MutableList<Periode> = ArrayList()
        val opphørPerioderTemp: MutableList<Periode> = ArrayList(opphørsPerioder)
        val removeOpphørPerioder: MutableList<Periode> = ArrayList()
        val addOpphørPerioder: MutableList<Periode> = ArrayList()
        while (!opphørPerioderTemp.isEmpty()) {
            opphørloop@ for (opphør in opphørPerioderTemp) {
                // ytelseloop:
                for (ytelse in ytelsesPerioder) {
                    // Scenario 1
                    if (opphør.fom.isBefore(ytelse.fom) && opphør.tom.isBefore(ytelse.fom)) {
                        periodeList.add(
                            Periode(
                                fom = opphør.fom,
                                tom = opphør.tom,
                                sats = opphør.sats,
                                typeSats = opphør.typeSats,
                                kodeKlassifik = opphør.kodeKlassifik,
                                periodeType = PeriodeType.OPPH,
                            ),
                        )
                        removeOpphørPerioder.add(opphør)
                        continue@opphørloop
                        // Scenario 2
                    } else if (opphør.fom.isBefore(ytelse.fom) && opphør.tom.isEqual(ytelse.fom)) {
                        periodeList.add(
                            Periode(
                                fom = opphør.fom,
                                tom = opphør.tom.minusDays(1),
                                sats = opphør.sats,
                                typeSats = opphør.typeSats,
                                kodeKlassifik = opphør.kodeKlassifik,
                                periodeType = PeriodeType.OPPH,
                            ),
                        )
                        removeOpphørPerioder.add(opphør)
                        continue@opphørloop
                        // Scenario 3,4 og 5
                    } else if (opphør.fom.isBefore(ytelse.fom) && opphør.tom.isAfter(ytelse.fom)) {
                        periodeList.add(
                            Periode(
                                fom = opphør.fom,
                                tom = ytelse.fom.minusDays(1),
                                sats = opphør.sats,
                                typeSats = opphør.typeSats,
                                kodeKlassifik = opphør.kodeKlassifik,
                                periodeType = PeriodeType.OPPH,
                            ),
                        )
                        removeOpphørPerioder.add(opphør)
                        // Scenario 3 & 4
                        if (!opphør.tom.isAfter(ytelse.tom)) {
                            continue@opphørloop
                        } else { // if opphør.getTom().isAfter(ytelse.getTom())
                            addOpphørPerioder.add(
                                Periode(
                                    fom = ytelse.tom.plusDays(1),
                                    tom = opphør.tom,
                                    sats = opphør.sats,
                                    typeSats = opphør.typeSats,
                                    kodeKlassifik = opphør.kodeKlassifik,
                                ),
                            )
                            continue@opphørloop
                        }
                    } else if (!opphør.fom.isBefore(ytelse.fom) && !opphør.tom.isAfter(ytelse.tom)) {
                        removeOpphørPerioder.add(opphør)
                        continue@opphørloop
                    } else if (opphør.fom.isEqual(ytelse.fom) && opphør.tom.isAfter(ytelse.tom)) {
                        removeOpphørPerioder.add(opphør)
                        addOpphørPerioder.add(
                            Periode(
                                fom = ytelse.tom.plusDays(1),
                                tom = opphør.tom,
                                sats = opphør.sats,
                                typeSats = opphør.typeSats,
                                kodeKlassifik = opphør.kodeKlassifik,
                            ),
                        )
                        continue@opphørloop
                    } else if (opphør.fom.isAfter(ytelse.fom) && !opphør.fom.isAfter(ytelse.tom)) {
                        removeOpphørPerioder.add(opphør)
                        addOpphørPerioder.add(
                            Periode(
                                fom = ytelse.tom.plusDays(1),
                                tom = opphør.tom,
                                sats = opphør.sats,
                                typeSats = opphør.typeSats,
                                kodeKlassifik = opphør.kodeKlassifik,
                            ),
                        )
                        continue@opphørloop
                    }
                    // Scenario 12 - sjekkes mot neste ytelsesperiode
                }
                // Scenario 12 - hvis den ikke treffer noen ytelsesperioder.
                periodeList.add(
                    Periode(
                        fom = opphør.fom,
                        tom = opphør.tom,
                        sats = opphør.sats,
                        typeSats = opphør.typeSats,
                        kodeKlassifik = opphør.kodeKlassifik,
                        periodeType = PeriodeType.OPPH,
                    ),
                )
                removeOpphørPerioder.add(opphør)
            }
            opphørPerioderTemp.removeAll(removeOpphørPerioder)
            removeOpphørPerioder.clear()
            opphørPerioderTemp.addAll(addOpphørPerioder)
            addOpphørPerioder.clear()
            Collections.sort(opphørPerioderTemp)
        }
        Collections.sort(periodeList)
        return periodeList
    }

    private fun definerYtelseOgOmpostPerioder(): List<Periode> {
        val periodeList: MutableList<Periode> = ArrayList()
        val ytelsesPerioderTemp: MutableList<Periode> = ArrayList(ytelsesPerioder)
        val removeYtelsesPerioder: MutableList<Periode> = ArrayList()
        val addYtelsesPerioder: MutableList<Periode> = ArrayList()
        while (!ytelsesPerioderTemp.isEmpty()) {
            ytelseloop@ for (ytelse in ytelsesPerioderTemp) {
                // opphørloop:
                for (opphør in opphørsPerioder) {
                    // Scenarioer beskrevet i readme.md - Ytelse sjekkes mot opphør
                    // Scenario 1
                    if (ytelse.fom.isBefore(opphør.fom) && ytelse.tom.isBefore(opphør.fom)) {
                        periodeList.add(
                            Periode(
                                fom = ytelse.fom,
                                tom = ytelse.tom,
                                sats = ytelse.sats,
                                typeSats = ytelse.typeSats,
                                kodeKlassifik = ytelse.kodeKlassifik,
                                periodeType = PeriodeType.YTEL,
                            ),
                        )
                        removeYtelsesPerioder.add(ytelse)
                        continue@ytelseloop
                        // Scenario 2
                    } else if (ytelse.fom.isBefore(opphør.fom) && ytelse.tom.isEqual(opphør.fom)) {
                        periodeList.add(
                            Periode(
                                fom = ytelse.fom,
                                tom = ytelse.tom.minusDays(1),
                                sats = ytelse.sats,
                                typeSats = ytelse.typeSats,
                                kodeKlassifik = ytelse.kodeKlassifik,
                                periodeType = PeriodeType.YTEL,
                            ),
                        )
                        opphør.sats?.let {
                            Periode(
                                fom = ytelse.tom,
                                tom = ytelse.tom,
                                sats = it,
                                gammelSats = ytelse.sats,
                                typeSats = ytelse.typeSats,
                                kodeKlassifik = ytelse.kodeKlassifik,
                            )
                        }?.let { periodeList.add(it) }
                        removeYtelsesPerioder.add(ytelse)
                        continue@ytelseloop
                        // Scenario 3,4 og 5
                    } else if (ytelse.fom.isBefore(opphør.fom) && ytelse.tom.isAfter(opphør.fom)) {
                        periodeList.add(
                            Periode(
                                fom = ytelse.fom,
                                tom = opphør.fom.minusDays(1),
                                sats = ytelse.sats,
                                typeSats = ytelse.typeSats,
                                kodeKlassifik = ytelse.kodeKlassifik,
                                periodeType = PeriodeType.YTEL,
                            ),
                        )
                        // Scenario 3 & 4
                        if (!ytelse.tom.isAfter(opphør.tom)) {
                            opphør.sats?.let {
                                Periode(
                                    fom = opphør.fom,
                                    tom = ytelse.tom,
                                    sats = it,
                                    gammelSats = ytelse.sats,
                                    typeSats = ytelse.typeSats,
                                    kodeKlassifik = ytelse.kodeKlassifik,
                                )
                            }?.let {
                                periodeList.add(
                                    it,
                                )
                            }
                            removeYtelsesPerioder.add(ytelse)
                            continue@ytelseloop
                            // Scenario 5
                        } else { // if (ytelse.getTom().isAfter(opphør.getTom())
                            opphør.sats?.let {
                                Periode(
                                    fom = opphør.fom,
                                    tom = opphør.tom,
                                    sats = it,
                                    gammelSats = ytelse.sats,
                                    typeSats = ytelse.typeSats,
                                    kodeKlassifik = ytelse.kodeKlassifik,
                                )
                            }?.let {
                                periodeList.add(
                                    it,
                                )
                            }
                            addYtelsesPerioder.add(
                                Periode(
                                    fom = opphør.tom.plusDays(1),
                                    tom = ytelse.fom,
                                    sats = ytelse.sats,
                                    typeSats = ytelse.typeSats,
                                    kodeKlassifik = ytelse.kodeKlassifik,
                                ),
                            ) // Nytt objekt på slutten til samme loop
                            removeYtelsesPerioder.add(ytelse)
                            continue@ytelseloop
                        }
                        // Scenario 6 & 7
                    } else if (ytelse.fom.isEqual(opphør.fom) && !ytelse.tom.isAfter(opphør.tom)) {
                        opphør.sats?.let {
                            Periode(
                                fom = ytelse.fom,
                                tom = ytelse.tom,
                                sats = it,
                                gammelSats = ytelse.sats,
                                typeSats = ytelse.typeSats,
                                kodeKlassifik = ytelse.kodeKlassifik,
                            )
                        }?.let { periodeList.add(it) }
                        removeYtelsesPerioder.add(ytelse)
                        continue@ytelseloop
                        // Scenario 8
                    } else if (ytelse.fom.isEqual(opphør.fom) && ytelse.tom.isAfter(opphør.tom)) {
                        opphør.sats?.let {
                            Periode(
                                fom = ytelse.fom,
                                tom = opphør.tom,
                                sats = it,
                                gammelSats = ytelse.sats,
                                typeSats = ytelse.typeSats,
                                kodeKlassifik = ytelse.kodeKlassifik,
                            )
                        }?.let { periodeList.add(it) }
                        addYtelsesPerioder.add(
                            Periode(
                                fom = opphør.tom.plusDays(1),
                                tom = ytelse.tom,
                                sats = ytelse.sats,
                                typeSats = ytelse.typeSats,
                                kodeKlassifik = ytelse.kodeKlassifik,
                            ),
                        ) // //Nytt objekt på slutten til samme loop
                        removeYtelsesPerioder.add(ytelse)
                        continue@ytelseloop
                        // Scenario 9
                    } else if (ytelse.fom.isAfter(opphør.fom) && !ytelse.tom.isAfter(opphør.tom)) { // ytelse.getFom() er implisit før opphør.getTom() da ytelse.getFom() ikke kan være etter ytelse.getTom()
                        opphør.sats?.let {
                            Periode(
                                fom = ytelse.fom,
                                tom = ytelse.tom,
                                sats = it,
                                gammelSats = ytelse.sats,
                                typeSats = ytelse.typeSats,
                                kodeKlassifik = ytelse.kodeKlassifik,
                            )
                        }?.let { periodeList.add(it) }
                        removeYtelsesPerioder.add(ytelse)
                        continue@ytelseloop
                        // Scenario 10 & 11
                    } else if (ytelse.fom.isAfter(opphør.fom) && !ytelse.fom.isAfter(opphør.tom)) { // ytelse.getTom() er implisit after opphør.getTom() ellers ville den truffet forrige if-statement
                        opphør.sats?.let {
                            Periode(
                                fom = ytelse.fom,
                                tom = opphør.tom,
                                sats = it,
                                gammelSats = ytelse.sats,
                                typeSats = ytelse.typeSats,
                                kodeKlassifik = ytelse.kodeKlassifik,
                            )
                        }?.let { periodeList.add(it) }
                        addYtelsesPerioder.add(
                            Periode(
                                fom = opphør.tom.plusDays(1),
                                tom = ytelse.tom,
                                sats = ytelse.sats,
                                typeSats = ytelse.typeSats,
                                kodeKlassifik = ytelse.kodeKlassifik,
                            ),
                        )
                        removeYtelsesPerioder.add(ytelse)
                        continue@ytelseloop
                    }
                    // Scenario 12 - prøver igjen mot neste opphørsperiode
                }
                // Scenario 12 - hvis ytelsen ikke treffer noen opphørsperioder.
                periodeList.add(
                    Periode(
                        fom = ytelse.fom,
                        tom = ytelse.tom,
                        sats = ytelse.sats,
                        typeSats = ytelse.typeSats,
                        kodeKlassifik = ytelse.kodeKlassifik,
                        periodeType = PeriodeType.YTEL,
                    ),
                ) // Hvis ytelsen ikke treffer noen opphør
                removeYtelsesPerioder.add(ytelse)
            }
            ytelsesPerioderTemp.removeAll(removeYtelsesPerioder)
            removeYtelsesPerioder.clear()
            ytelsesPerioderTemp.addAll(addYtelsesPerioder)
            addYtelsesPerioder.clear()
            Collections.sort(ytelsesPerioderTemp)
        }
        return periodeList
    }

    private fun lagOpphørOgYtelse(oppdragslinjeList: List<Oppdragslinje>) {
        for (oppdragslinje in oppdragslinjeList) {
            assert(oppdragslinje.typeSats == "MND" || oppdragslinje.typeSats == "DAG") { "Forventet at typeSats er enten DAG eller MND, men typeSats var: " + oppdragslinje.typeSats }
            if (oppdragslinje.kodeEndringLinje == "ENDR") {
                assert(oppdragslinje.kodeStatusLinje == KodeStatusLinje.OPPH) { "Forventet at KodeStatusLinje er OPPH når KodeEndringLinje er ENDR" }
                if (oppdragslinje.datoStatusFom != null) {
                    opphørsPerioder.add(
                        Periode(
                            fom = LocalDate.parse(oppdragslinje.datoStatusFom, dateTimeFormatter),
                            tom = LocalDate.parse(oppdragslinje.datoVedtakTom, dateTimeFormatter),
                            sats = oppdragslinje.sats,
                            typeSats = oppdragslinje.typeSats,
                            kodeKlassifik = oppdragslinje.kodeKlassifik,
                        ),
                    )
                } else {
                    opphørsPerioder.add(
                        Periode(
                            fom = LocalDate.parse(oppdragslinje.datoVedtakFom, dateTimeFormatter),
                            tom = LocalDate.parse(oppdragslinje.datoVedtakTom, dateTimeFormatter),
                            sats = oppdragslinje.sats,
                            typeSats = oppdragslinje.typeSats,
                            kodeKlassifik = oppdragslinje.kodeKlassifik,
                        ),
                    )
                }
            } else if (oppdragslinje.kodeEndringLinje == "NY") {
                ytelsesPerioder.add(
                    Periode(
                        fom = LocalDate.parse(oppdragslinje.datoVedtakFom, dateTimeFormatter),
                        tom = LocalDate.parse(oppdragslinje.datoVedtakTom, dateTimeFormatter),
                        sats = oppdragslinje.sats,
                        typeSats = oppdragslinje.typeSats,
                        kodeKlassifik = oppdragslinje.kodeKlassifik,
                    ),
                )
            } else {
                throw IllegalArgumentException("Forventet kodeEndringLinje NY eller ENDR. Verdi var: " + oppdragslinje.kodeEndringLinje)
            }
        }
    }

    companion object {
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}
