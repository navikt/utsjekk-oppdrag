package no.nav.dagpenger.oppdrag.repository

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.dagpenger.kontrakter.felles.Fagsystem
import no.nav.dagpenger.kontrakter.oppdrag.OppdragId
import no.nav.dagpenger.kontrakter.oppdrag.Utbetalingsoppdrag
import no.nav.dagpenger.oppdrag.domene.OppdragStatus
import no.nav.dagpenger.oppdrag.domene.objectMapper
import no.trygdeetaten.skjema.oppdrag.Mmel
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.*

@Repository
class OppdragLagerRepositoryJdbc(
    val jdbcTemplate: JdbcTemplate,
    val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) : OppdragLagerRepository {

    internal var LOG = LoggerFactory.getLogger(OppdragLagerRepositoryJdbc::class.java)

    override fun hentOppdrag(oppdragId: OppdragId, versjon: Int): OppdragLager {
        val hentStatement =
            "SELECT * FROM oppdrag_lager WHERE behandling_id = ? AND person_ident = ? AND fagsystem = ? AND versjon = ?"

        val listeAvOppdrag = jdbcTemplate.query(
            hentStatement,
            OppdragLagerRowMapper(),
            oppdragId.behandlingsId.toString(),
            oppdragId.personIdent,
            oppdragId.fagsystem.kode,
            versjon,
        )

        return when (listeAvOppdrag.size) {
            0 -> {
                LOG.error("Feil ved henting av oppdrag. Fant ingen oppdrag med id $oppdragId")
                throw NoSuchElementException("Feil ved henting av oppdrag. Fant ingen oppdrag med id $oppdragId")
            }

            1 -> listeAvOppdrag[0]
            else -> {
                LOG.error("Feil ved henting av oppdrag. Fant fler oppdrag med id $oppdragId")
                throw Exception("Feil ved henting av oppdrag. Fant fler oppdrag med id $oppdragId")
            }
        }
    }

    override fun opprettOppdrag(oppdragLager: OppdragLager, versjon: Int) {
        val insertStatement =
            "INSERT INTO oppdrag_lager (id, utgaaende_oppdrag, status, opprettet_tidspunkt, person_ident, fagsak_id, behandling_id, fagsystem, avstemming_tidspunkt, utbetalingsoppdrag, versjon) VALUES (?,?,?,?,?,?,?,?,?,?,?)"

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

    override fun oppdaterStatus(oppdragId: OppdragId, oppdragStatus: OppdragStatus, versjon: Int) {

        val update =
            "UPDATE oppdrag_lager SET status = '${oppdragStatus.name}' WHERE person_ident = '${oppdragId.personIdent}' AND fagsystem = '${oppdragId.fagsystem.kode}' AND behandling_id = '${oppdragId.behandlingsId}'AND versjon = $versjon"

        jdbcTemplate.execute(update)
    }

    override fun oppdaterKvitteringsmelding(oppdragId: OppdragId, kvittering: Mmel, versjon: Int) {
        val updateStatement =
            "UPDATE oppdrag_lager SET kvitteringsmelding = ? WHERE person_ident = ? AND fagsystem = ? AND behandling_id = ? AND versjon = ?"

        jdbcTemplate.update(
            updateStatement,
            objectMapper.writeValueAsString(kvittering),
            oppdragId.personIdent,
            oppdragId.fagsystem.kode,
            oppdragId.behandlingsId.toString(),
            versjon
        )
    }

    override fun hentIverksettingerForGrensesnittavstemming(
        fomTidspunkt: LocalDateTime,
        tomTidspunkt: LocalDateTime,
        fagsystem: Fagsystem
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

    override fun hentUtbetalingsoppdrag(oppdragId: OppdragId, versjon: Int): Utbetalingsoppdrag {
        val hentStatement =
            "SELECT utbetalingsoppdrag FROM oppdrag_lager WHERE behandling_id = ? AND person_ident = ? AND fagsystem = ? AND versjon = ?"

        val jsonUtbetalingsoppdrag = jdbcTemplate.queryForObject(
            hentStatement,
            String::class.java,
            oppdragId.behandlingsId,
            oppdragId.personIdent,
            oppdragId.fagsystem.kode,
            versjon,
        )

        return objectMapper.readValue(jsonUtbetalingsoppdrag)
    }

    override fun hentAlleVersjonerAvOppdrag(oppdragId: OppdragId): List<OppdragLager> {
        val hentStatement = "SELECT * FROM oppdrag_lager WHERE behandling_id = ? AND person_ident = ? AND fagsystem = ?"

        return jdbcTemplate.query(
            hentStatement,
            OppdragLagerRowMapper(),
            oppdragId.behandlingsId.toString(),
            oppdragId.personIdent,
            oppdragId.fagsystem.kode,
        )
    }

    override fun hentUtbetalingsoppdragForKonsistensavstemming(
        fagsystem: String,
        behandlingIder: Set<String>
    ): List<UtbetalingsoppdragForKonsistensavstemming> {

        val query = """SELECT fagsak_id, behandling_id, utbetalingsoppdrag FROM (
                        SELECT fagsak_id, behandling_id, utbetalingsoppdrag, 
                          row_number() OVER (PARTITION BY fagsak_id, behandling_id ORDER BY versjon DESC) rn
                          FROM oppdrag_lager WHERE fagsystem=:fagsystem AND behandling_id IN (:behandlingIder)
                          AND status IN (:status)) q 
                        WHERE rn = 1"""

        val status = setOf(OppdragStatus.KVITTERT_OK, OppdragStatus.KVITTERT_MED_MANGLER).map { it.name }

        return behandlingIder.chunked(3000).map { behandlingIderChunked ->
            val values = MapSqlParameterSource().addValue("fagsystem", fagsystem)
                .addValue("behandlingIder", behandlingIderChunked).addValue("status", status)

            namedParameterJdbcTemplate.query(query, values) { resultSet, _ ->
                UtbetalingsoppdragForKonsistensavstemming(
                    resultSet.getString("fagsak_id"),
                    resultSet.getString("behandling_id"),
                    objectMapper.readValue(resultSet.getString("utbetalingsoppdrag"))
                )
            }
        }.flatten()
    }
}

class OppdragLagerRowMapper : RowMapper<OppdragLager> {

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

    override fun mapRow(resultSet: ResultSet, rowNumbers: Int): OppdragLager {
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
            versjon = resultSet.getInt(12)
        )
    }
}
