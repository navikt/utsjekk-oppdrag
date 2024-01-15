package no.nav.dagpenger.oppdrag.iverksetting.tilstand

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.util.UUID

internal interface OppdragRepository : CrudRepository<OppdragLager, UUID> {
    @Query("SELECT * FROM oppdrag_lager WHERE id IS null LIMIT 10000")
    fun findWhereUuidIsNull(): Iterable<OppdragLager>

    @Modifying
    @Query(
        """
        UPDATE oppdrag_lager SET id = :uuid 
        WHERE behandling_id = :behandlingsId 
            AND person_ident = :personIdent 
            AND fagsystem = :fagsystem 
            AND versjon = :versjon
        """,
    )
    fun updateUuid(
        behandlingsId: String,
        personIdent: String,
        fagsystem: String,
        versjon: Int,
        uuid: UUID = UUID.randomUUID(),
    )

    @Deprecated("Støttes ikke, bruk insert/update")
    override fun <S : OppdragLager> save(entity: S): S {
        error("Not implemented - Use InsertUpdateRepository - insert/update")
    }

    @Deprecated("Støttes ikke, bruk insertAll/updateAll")
    override fun <S : OppdragLager> saveAll(entities: Iterable<S>): Iterable<S> {
        error("Not implemented - Use InsertUpdateRepository - insertAll/updateAll")
    }
}
