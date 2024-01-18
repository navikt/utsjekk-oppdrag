package no.nav.dagpenger.oppdrag.iverksetting.tilstand

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.dagpenger.kontrakter.felles.Fagsystem
import no.nav.dagpenger.kontrakter.felles.somString
import no.nav.dagpenger.kontrakter.oppdrag.OppdragId
import no.nav.dagpenger.oppdrag.config.objectMapper
import no.nav.dagpenger.oppdrag.iverksetting.domene.OppdragStatus
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
        val hentStatement =
            "SELECT * FROM oppdrag_lager WHERE behandling_id = ? AND person_ident = ? AND fagsystem = ? AND versjon = ?"

        val listeAvOppdrag =
            jdbcTemplate.query(
                hentStatement,
                OppdragLagerRowMapper(),
                oppdragId.behandlingId.somString,
                oppdragId.personIdent,
                oppdragId.fagsystem.kode,
                versjon,
            )

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
                INSERT INTO oppdrag_lager (id, utgaaende_oppdrag, status, opprettet_tidspunkt, person_ident, fagsak_id, behandling_id, fagsystem, avstemming_tidspunkt, utbetalingsoppdrag, versjon) 
                VALUES (?,?,?,?,?,?,?,?,?,?,?)
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
            WHERE person_ident = '${oppdragId.personIdent}' 
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
            "UPDATE oppdrag_lager SET kvitteringsmelding = ? WHERE person_ident = ? AND fagsystem = ? AND behandling_id = ? AND versjon = ?"

        jdbcTemplate.update(
            updateStatement,
            objectMapper.writeValueAsString(kvittering),
            oppdragId.personIdent,
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
            "SELECT * FROM oppdrag_lager WHERE behandling_id = ? AND person_ident = ? AND fagsystem = ?",
            OppdragLagerRowMapper(),
            oppdragId.behandlingId.somString,
            oppdragId.personIdent,
            oppdragId.fagsystem.kode,
        )

    companion object {
        private val logger = LoggerFactory.getLogger(OppdragLagerRepository::class.java)
    }
}

internal class OppdragLagerRowMapper : RowMapper<OppdragLager> {
    /*
    1    id                   UUID PRIMARY KEY,
    2 utgaaende_oppdrag    TEXT                                NOT NULL,
    3 status               VARCHAR(150) DEFAULT 'LAGT_PAA_KOE':: character varying NOT NULL,
    4 opprettet_tidspunkt  TIMESTAMP(6) DEFAULT LOCALTIMESTAMP NOT NULL,
    5 person_ident         VARCHAR(50)                         NOT NULL,
    6 fagsak_id            VARCHAR(50)                         NOT NULL,
    7 behandling_id        VARCHAR(50)                         NOT NULL,
    8 fagsystem            VARCHAR(10)                         NOT NULL,
    9 avstemming_tidspunkt TIMESTAMP(6)                        NOT NULL,
    10 utbetalingsoppdrag   JSON                                NOT NULL,
    11 kvitteringsmelding   JSON,
    12 versjon              BIGINT       DEFAULT 0              NOT NULL

     */

    override fun mapRow(
        resultSet: ResultSet,
        rowNumbers: Int,
    ): OppdragLager {
        val kvittering = resultSet.getString(11)

        return OppdragLager(
            uuid = UUID.fromString(resultSet.getString(1) ?: UUID.randomUUID().toString()),
            fagsystem = resultSet.getString(8),
            personIdent = resultSet.getString(5),
            fagsakId = resultSet.getString(6),
            behandlingId = resultSet.getString(7),
            utbetalingsoppdrag = objectMapper.readValue(resultSet.getString(10)),
            utgåendeOppdrag = resultSet.getString(2),
            status = OppdragStatus.valueOf(resultSet.getString(3)),
            avstemmingTidspunkt = resultSet.getTimestamp(9).toLocalDateTime(),
            opprettetTidspunkt = resultSet.getTimestamp(4).toLocalDateTime(),
            kvitteringsmelding = if (kvittering == null) null else objectMapper.readValue(kvittering),
            versjon = resultSet.getInt(12),
        )
    }
}
