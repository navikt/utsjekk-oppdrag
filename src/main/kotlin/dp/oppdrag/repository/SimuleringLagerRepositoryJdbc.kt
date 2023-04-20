package dp.oppdrag.repository

import dp.oppdrag.model.OppdragSkjemaConstants.Companion.FAGSYSTEM
import dp.oppdrag.model.SimuleringLager
import java.sql.ResultSet
import java.util.*
import javax.sql.DataSource

class SimuleringLagerRepositoryJdbc(private val dataSource: DataSource) : SimuleringLagerRepository {

    override fun lagreINyTransaksjon(simuleringLager: SimuleringLager) {
        val sql = """
            INSERT INTO simulering_lager
            (id, fagsak_id, behandling_id, fagsystem, utbetalingsoppdrag, request_xml, response_xml)
            VALUES (?::uuid, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

        dataSource.connection.use {
            it.prepareStatement(sql).apply {
                setString(1, UUID.randomUUID().toString())
                setString(2, simuleringLager.fagsakId)
                setString(3, simuleringLager.behandlingId)
                setString(4, simuleringLager.fagsystem)
                setString(5, simuleringLager.utbetalingsoppdrag)
                setString(6, simuleringLager.requestXml)
                setString(7, simuleringLager.responseXml)

                executeUpdate()
            }
        }
    }

    override fun oppdater(simuleringLager: SimuleringLager) {
        val sql = """
            UPDATE simulering_lager
            SET
            fagsak_id = ?, behandling_id = ?, fagsystem = ?, utbetalingsoppdrag = ?, request_xml = ?, response_xml = ?
            WHERE id = ?::uuid
            """.trimIndent()

        dataSource.connection.use {
            it.prepareStatement(sql).apply {
                setString(1, simuleringLager.fagsakId)
                setString(2, simuleringLager.behandlingId)
                setString(3, simuleringLager.fagsystem)
                setString(4, simuleringLager.utbetalingsoppdrag)
                setString(5, simuleringLager.requestXml)
                setString(6, simuleringLager.responseXml)
                setString(7, simuleringLager.id.toString())

                executeUpdate()
            }
        }
    }

    override fun finnAlleSimuleringsLager(): List<SimuleringLager> {
        val sql = "SELECT *  FROM simulering_lager"

        val list = mutableListOf<SimuleringLager>()

        dataSource.connection.use {
            it.prepareStatement(sql).apply {
                executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        list.add(mapRow(resultSet))
                    }
                }
            }
        }

        return list
    }

    override fun hentSisteSimuleringsresultat(
        fagsakId: String,
        behandlingId: String
    ): SimuleringLager {
        val sql = """
            SELECT sim.* FROM simulering_lager sim 
             WHERE sim.fagsystem = ? AND sim.fagsak_id = ? AND sim.behandling_id = ?
             AND sim.response_xml IS NOT NULL
             ORDER BY sim.opprettet_tidspunkt DESC LIMIT 1
            """.trimIndent()

        var simuleringLager: SimuleringLager? = null

        dataSource.connection.use {
            it.prepareStatement(sql).apply {
                setString(1, FAGSYSTEM)
                setString(2, fagsakId)
                setString(3, behandlingId)

                executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        simuleringLager = mapRow(resultSet)
                    }
                }
            }
        }

        if (simuleringLager == null) {
            throw Exception("Kan ikke finne simulering med fagsakId=$fagsakId og behandlingId=$behandlingId")
        }

        return simuleringLager!!
    }

    private fun mapRow(resultSet: ResultSet): SimuleringLager {
        return SimuleringLager(
            id = UUID.fromString(resultSet.getString("id")),
            fagsystem = resultSet.getString("fagsystem"),
            fagsakId = resultSet.getString("fagsak_id"),
            behandlingId = resultSet.getString("behandling_id"),
            utbetalingsoppdrag = resultSet.getString("utbetalingsoppdrag"),
            requestXml = resultSet.getString("request_xml"),
            responseXml = resultSet.getString("response_xml"),
            opprettetTidspunkt = resultSet.getTimestamp("opprettet_tidspunkt").toLocalDateTime()
        )
    }
}
