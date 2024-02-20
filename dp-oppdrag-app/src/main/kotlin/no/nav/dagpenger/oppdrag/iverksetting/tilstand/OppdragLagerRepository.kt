package no.nav.dagpenger.oppdrag.iverksetting.tilstand

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.dagpenger.kontrakter.felles.Fagsystem
import no.nav.dagpenger.kontrakter.felles.somString
import no.nav.dagpenger.kontrakter.oppdrag.OppdragStatus
import no.nav.dagpenger.oppdrag.config.objectMapper
import no.trygdeetaten.skjema.oppdrag.Mmel
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.UUID
import kotlin.NoSuchElementException

@Repository
internal class OppdragLagerRepository(val jdbcTemplate: JdbcTemplate) {
    fun hentOppdrag(
        oppdragId: OppdragId,
        versjon: Int = 0,
    ): OppdragLager {
        val listeAvOppdrag =
            if (oppdragId.iverksettingId != null) {
                val hentStatement =
                    "SELECT * FROM oppdrag_lager WHERE behandling_id = ? AND fagsak_id = ? AND fagsystem = ? AND iverksetting_id = ? AND versjon = ?"

                jdbcTemplate.query(
                    hentStatement,
                    OppdragLagerRowMapper(),
                    oppdragId.behandlingId.somString,
                    oppdragId.fagsakId.somString,
                    oppdragId.fagsystem.kode,
                    oppdragId.iverksettingId,
                    versjon,
                )
            } else {
                val hentStatement =
                    "SELECT * FROM oppdrag_lager WHERE behandling_id = ? AND fagsak_id = ? AND fagsystem = ? AND iverksetting_id is null AND versjon = ?"

                jdbcTemplate.query(
                    hentStatement,
                    OppdragLagerRowMapper(),
                    oppdragId.behandlingId.somString,
                    oppdragId.fagsakId.somString,
                    oppdragId.fagsystem.kode,
                    versjon,
                )
            }

        return when (listeAvOppdrag.size) {
            0 -> {
                logger.error("Feil ved henting av oppdrag. Fant ingen oppdrag med id $oppdragId")
                throw NoSuchElementException("Feil ved henting av oppdrag. Fant ingen oppdrag med id $oppdragId")
            }

            1 -> listeAvOppdrag[0]
            else -> {
                logger.error("Feil ved henting av oppdrag. Fant fler oppdrag med id $oppdragId")
                throw Exception("Feil ved henting av oppdrag. Fant fler oppdrag med id $oppdragId")
            }
        }
    }

    fun opprettOppdrag(
        oppdragLager: OppdragLager,
        versjon: Int = 0,
    ) {
        val insertStatement =
            """
                INSERT INTO oppdrag_lager (id, utgaaende_oppdrag, status, opprettet_tidspunkt, person_ident, fagsak_id, behandling_id, iverksetting_id, fagsystem, avstemming_tidspunkt, utbetalingsoppdrag, versjon) 
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
            """.trimMargin()

        jdbcTemplate.update(
            insertStatement,
            UUID.randomUUID(),
            oppdragLager.utgåendeOppdrag,
            oppdragLager.status.name,
            oppdragLager.opprettetTidspunkt,
            oppdragLager.personIdent,
            oppdragLager.fagsakId,
            oppdragLager.behandlingId,
            oppdragLager.iverksettingId,
            oppdragLager.fagsystem,
            oppdragLager.avstemmingTidspunkt,
            objectMapper.writeValueAsString(oppdragLager.utbetalingsoppdrag),
            versjon,
        )
    }

    fun oppdaterStatus(
        oppdragId: OppdragId,
        oppdragStatus: OppdragStatus,
        versjon: Int = 0,
    ) {
        jdbcTemplate.execute(
            """
            UPDATE oppdrag_lager SET status = '${oppdragStatus.name}' 
            WHERE fagsak_id = '${oppdragId.fagsakId.somString}' 
                AND fagsystem = '${oppdragId.fagsystem.kode}' 
                AND behandling_id = '${oppdragId.behandlingId.somString}'
                AND versjon = $versjon
            """.trimIndent(),
        )
    }

    fun oppdaterKvitteringsmelding(
        oppdragId: OppdragId,
        kvittering: Mmel,
        versjon: Int = 0,
    ) {
        val updateStatement =
            "UPDATE oppdrag_lager SET kvitteringsmelding = ? WHERE fagsak_id = ? AND fagsystem = ? AND behandling_id = ? AND versjon = ?"

        jdbcTemplate.update(
            updateStatement,
            objectMapper.writeValueAsString(kvittering),
            oppdragId.fagsakId.somString,
            oppdragId.fagsystem.kode,
            oppdragId.behandlingId.somString,
            versjon,
        )
    }

    fun hentIverksettingerForGrensesnittavstemming(
        fomTidspunkt: LocalDateTime,
        tomTidspunkt: LocalDateTime,
        fagsystem: Fagsystem,
    ): List<OppdragLager> {
        val hentStatement =
            "SELECT * FROM oppdrag_lager WHERE avstemming_tidspunkt >= ? AND avstemming_tidspunkt < ? AND fagsystem = ?"

        return jdbcTemplate.query(
            hentStatement,
            OppdragLagerRowMapper(),
            fomTidspunkt,
            tomTidspunkt,
            fagsystem.kode,
        )
    }

    fun hentAlleVersjonerAvOppdrag(oppdragId: OppdragId): List<OppdragLager> =
        jdbcTemplate.query(
            "SELECT * FROM oppdrag_lager WHERE behandling_id = ? AND fagsak_id = ? AND fagsystem = ?",
            OppdragLagerRowMapper(),
            oppdragId.behandlingId.somString,
            oppdragId.fagsakId.somString,
            oppdragId.fagsystem.kode,
        )

    companion object {
        private val logger = LoggerFactory.getLogger(OppdragLagerRepository::class.java)
    }
}

internal class OppdragLagerRowMapper : RowMapper<OppdragLager> {
    override fun mapRow(
        resultSet: ResultSet,
        rowNumbers: Int,
    ): OppdragLager {
        val kvittering = resultSet.getString("kvitteringsmelding")

        return OppdragLager(
            uuid = UUID.fromString(resultSet.getString("id") ?: UUID.randomUUID().toString()),
            fagsystem = resultSet.getString("fagsystem"),
            personIdent = resultSet.getString("person_ident"),
            fagsakId = resultSet.getString("fagsak_id"),
            behandlingId = resultSet.getString("behandling_id"),
            iverksettingId = resultSet.getString("iverksetting_id"),
            utbetalingsoppdrag = objectMapper.readValue(resultSet.getString("utbetalingsoppdrag")),
            utgåendeOppdrag = resultSet.getString("utgaaende_oppdrag"),
            status = OppdragStatus.valueOf(resultSet.getString("status")),
            avstemmingTidspunkt = resultSet.getTimestamp("avstemming_tidspunkt").toLocalDateTime(),
            opprettetTidspunkt = resultSet.getTimestamp("opprettet_tidspunkt").toLocalDateTime(),
            kvitteringsmelding = kvittering?.let { objectMapper.readValue(it) },
            versjon = resultSet.getInt("versjon"),
        )
    }
}
