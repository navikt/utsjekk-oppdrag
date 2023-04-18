package dp.oppdrag.repository

import com.fasterxml.jackson.module.kotlin.readValue
import dp.oppdrag.defaultLogger
import dp.oppdrag.defaultObjectMapper
import dp.oppdrag.model.*
import dp.oppdrag.model.OppdragSkjemaConstants.Companion.FAGSYSTEM
import no.trygdeetaten.skjema.oppdrag.Mmel
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

class OppdragLagerRepositoryJdbc(private val dataSource: DataSource) : OppdragLagerRepository {

    override fun hentOppdrag(oppdragId: OppdragId, versjon: Int): OppdragLager {
        val hentStatement = """
            SELECT * 
            FROM oppdrag_lager
            WHERE behandling_id = ?
            AND person_ident = ?
            AND fagsystem = ?
            AND versjon = ?
            """.trimIndent()

        val list = mutableListOf<OppdragLager>()
        dataSource.connection.prepareStatement(hentStatement)
            .use { preparedStatement ->
                preparedStatement.setString(1, oppdragId.behandlingsId)
                preparedStatement.setString(2, oppdragId.personIdent)
                preparedStatement.setString(3, oppdragId.fagsystem)
                preparedStatement.setInt(4, versjon)

                preparedStatement.mapOppdragLagerRows(list)
            }

        return when (list.size) {
            0 -> {
                throw NoSuchElementException("Feil ved henting av oppdrag. Fant ingen oppdrag med id $oppdragId og versjon $versjon")
            }

            1 -> list[0]

            else -> {
                throw Exception("Feil ved henting av oppdrag. Fant fler oppdrag med id $oppdragId og versjon $versjon")
            }
        }
    }

    override fun opprettOppdrag(oppdragLager: OppdragLager, versjon: Int) {
        val insertStatement = """
            INSERT INTO oppdrag_lager
            (id, utgaaende_oppdrag, status, opprettet_tidspunkt, person_ident, fagsak_id, behandling_id, fagsystem, avstemming_tidspunkt, utbetalingsoppdrag, versjon)
            VALUES (?::uuid,?,?,?,?,?,?,?,?,?::json,?)
            """.trimIndent()

        dataSource.connection.prepareStatement(insertStatement)
            .use {
                it.setString(1, UUID.randomUUID().toString())
                it.setString(2, oppdragLager.utgaaendeOppdrag)
                it.setString(3, oppdragLager.status.name)
                it.setTimestamp(4, Timestamp.valueOf(oppdragLager.opprettetTidspunkt))
                it.setString(5, oppdragLager.personIdent)
                it.setString(6, oppdragLager.fagsakId)
                it.setString(7, oppdragLager.behandlingId)
                it.setString(8, oppdragLager.fagsystem)
                it.setTimestamp(9, Timestamp.valueOf(oppdragLager.avstemmingTidspunkt))
                it.setString(10, defaultObjectMapper.writeValueAsString(oppdragLager.utbetalingsoppdrag))
                it.setInt(11, versjon)

                it.executeUpdate()
            }
    }

    override fun oppdaterStatus(oppdragId: OppdragId, oppdragLagerStatus: OppdragLagerStatus, versjon: Int) {
        val updateStatement = """
            UPDATE oppdrag_lager SET status = ?
            WHERE person_ident = ?
            AND fagsystem = ?
            AND behandling_id = ?
            AND versjon = ?
            """.trimIndent()

        dataSource.connection.prepareStatement(updateStatement).use {
            it.setString(1, oppdragLagerStatus.name)
            it.setString(2, oppdragId.personIdent)
            it.setString(3, oppdragId.fagsystem)
            it.setString(4, oppdragId.behandlingsId)
            it.setInt(5, versjon)

            it.executeUpdate()
        }
    }

    override fun oppdaterKvitteringsmelding(oppdragId: OppdragId, kvittering: Mmel, versjon: Int) {
        val updateStatement = """
            UPDATE oppdrag_lager SET kvitteringsmelding = ?::json
            WHERE person_ident = ?
            AND fagsystem = ?
            AND behandling_id = ?
            AND versjon = ?
            """.trimIndent()

        dataSource.connection.prepareStatement(updateStatement).use {
            it.setString(1, defaultObjectMapper.writeValueAsString(kvittering))
            it.setString(2, oppdragId.personIdent)
            it.setString(3, oppdragId.fagsystem)
            it.setString(4, oppdragId.behandlingsId)
            it.setInt(5, versjon)

            it.executeUpdate()
        }
    }

    override fun hentIverksettingerForGrensesnittavstemming(
        fomTidspunkt: LocalDateTime,
        tomTidspunkt: LocalDateTime
    ): List<OppdragLager> {
        val hentStatement = """
            SELECT *
            FROM oppdrag_lager
            WHERE avstemming_tidspunkt >= ?
            AND avstemming_tidspunkt < ?
            AND fagsystem = ?
            """.trimIndent()

        val list = mutableListOf<OppdragLager>()
        dataSource.connection.prepareStatement(hentStatement)
            .use { preparedStatement ->
                preparedStatement.setTimestamp(1, Timestamp.valueOf(fomTidspunkt))
                preparedStatement.setTimestamp(2, Timestamp.valueOf(tomTidspunkt))
                preparedStatement.setString(3, FAGSYSTEM)

                preparedStatement.mapOppdragLagerRows(list)
            }

        return list
    }

    override fun hentUtbetalingsoppdrag(oppdragId: OppdragId, versjon: Int): Utbetalingsoppdrag {
        val hentStatement = """
            SELECT utbetalingsoppdrag
            FROM oppdrag_lager
            WHERE behandling_id = ?
            AND person_ident = ?
            AND fagsystem = ?
            AND versjon = ?
            """.trimMargin()

        val list = mutableListOf<String>()
        dataSource.connection.prepareStatement(hentStatement)
            .use { preparedStatement ->
                preparedStatement.setString(1, oppdragId.behandlingsId)
                preparedStatement.setString(2, oppdragId.personIdent)
                preparedStatement.setString(3, oppdragId.fagsystem)
                preparedStatement.setInt(4, versjon)

                preparedStatement.executeQuery()
                    .use { resultSet ->
                        while (resultSet.next()) {
                            list.add(resultSet.getString("utbetalingsoppdrag"))
                        }
                    }
            }

        return when (list.size) {
            0 -> {
                throw NoSuchElementException("Feil ved henting av Utbetalingsoppdrag. Fant ingen oppdrag med id $oppdragId og versjon $versjon")
            }

            1 -> defaultObjectMapper.readValue(list[0])

            else -> {
                throw Exception("Feil ved henting av Utbetalingsoppdrag. Fant fler oppdrag med id $oppdragId og versjon $versjon")
            }
        }
    }

    override fun hentAlleVersjonerAvOppdrag(oppdragId: OppdragId): List<OppdragLager> {
        val hentStatement = """
            SELECT *
            FROM oppdrag_lager
            WHERE behandling_id = ?
            AND person_ident = ?
            AND fagsystem = ?
            """.trimIndent()

        val list = mutableListOf<OppdragLager>()
        dataSource.connection.prepareStatement(hentStatement)
            .use { preparedStatement ->
                preparedStatement.setString(1, oppdragId.behandlingsId)
                preparedStatement.setString(2, oppdragId.personIdent)
                preparedStatement.setString(3, oppdragId.fagsystem)

                preparedStatement.mapOppdragLagerRows(list)
            }

        return list
    }

    override fun hentUtbetalingsoppdragForKonsistensavstemming(
        fagsystem: String,
        behandlingIder: Set<String>
    ): List<UtbetalingsoppdragForKonsistensavstemming> {

        val query = """SELECT fagsak_id, behandling_id, utbetalingsoppdrag FROM (
                            SELECT fagsak_id,
                                behandling_id,
                                utbetalingsoppdrag, 
                                row_number() OVER (PARTITION BY fagsak_id, behandling_id ORDER BY versjon DESC) rn
                            FROM oppdrag_lager
                            WHERE fagsystem = ?
                                AND behandling_id = ANY(string_to_array(?, ','))
                                AND status = ANY(string_to_array(?, ','))
                        ) q 
                        WHERE rn = 1"""
        val statement = dataSource.connection.prepareStatement(query)

        val status = setOf(OppdragLagerStatus.KVITTERT_OK, OppdragLagerStatus.KVITTERT_MED_MANGLER)
            .map { it.name }
            .toTypedArray()

        val list = mutableListOf<UtbetalingsoppdragForKonsistensavstemming>()

        behandlingIder.chunked(3000).map { behandlingIderChunked ->
            statement
                .use { preparedStatement ->
                    preparedStatement.setString(1, fagsystem)
                    preparedStatement.setString(2, behandlingIderChunked.joinToString(","))
                    preparedStatement.setString(3, status.joinToString(","))

                    preparedStatement.executeQuery()
                        .use { resultSet ->
                            while (resultSet.next()) {
                                list.add(
                                    UtbetalingsoppdragForKonsistensavstemming(
                                        resultSet.getString("fagsak_id"),
                                        resultSet.getString("behandling_id"),
                                        defaultObjectMapper.readValue(resultSet.getString("utbetalingsoppdrag"))
                                    )
                                )
                            }
                        }
                }
        }

        return list
    }

    private fun PreparedStatement.mapOppdragLagerRows(list: MutableList<OppdragLager>) {
        this.executeQuery()
            .use { resultSet ->
                while (resultSet.next()) {
                    val kvitteringsmeldingStr = resultSet.getString(11)
                    val kvitteringsmelding = if (kvitteringsmeldingStr != null) {
                        defaultObjectMapper.readValue<Mmel>(kvitteringsmeldingStr)
                    } else {
                        null
                    }

                    list.add(
                        OppdragLager(
                            uuid = UUID.fromString(resultSet.getString(1) ?: UUID.randomUUID().toString()),
                            fagsystem = resultSet.getString(8),
                            personIdent = resultSet.getString(5),
                            fagsakId = resultSet.getString(6),
                            behandlingId = resultSet.getString(7),
                            utbetalingsoppdrag = defaultObjectMapper.readValue(resultSet.getString(10)),
                            utgaaendeOppdrag = resultSet.getString(2),
                            status = OppdragLagerStatus.valueOf(resultSet.getString(3)),
                            avstemmingTidspunkt = resultSet.getTimestamp(9).toLocalDateTime(),
                            opprettetTidspunkt = resultSet.getTimestamp(4).toLocalDateTime(),
                            kvitteringsmelding = kvitteringsmelding,
                            versjon = resultSet.getInt(12)
                        )
                    )
                }
            }
    }
}

class OppdragAlleredeSendtException : RuntimeException()
