package dp.oppdrag.repository

import dp.oppdrag.model.MellomlagringKonsistensavstemming
import java.util.*
import javax.sql.DataSource

class MellomlagringKonsistensavstemmingRepositoryJdbc(
    private val dataSource: DataSource
) : MellomlagringKonsistensavstemmingRepository {

    override fun findAllByTransaksjonsId(transaksjonsId: UUID): List<MellomlagringKonsistensavstemming> {
        val sql = """
            SELECT *
            FROM mellomlagring_konsistensavstemming
            WHERE transaksjons_id = ?::uuid
            """.trimIndent()

        val list = mutableListOf<MellomlagringKonsistensavstemming>()

        dataSource.connection.use {
            it.prepareStatement(sql).apply {
                setString(1, transaksjonsId.toString())

                executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        list.add(
                            MellomlagringKonsistensavstemming(
                                id = UUID.fromString(resultSet.getString("id")),
                                fagsystem = resultSet.getString("fagsystem"),
                                transaksjonsId = UUID.fromString(resultSet.getString("transaksjons_id")),
                                antallOppdrag = resultSet.getInt("antall_oppdrag"),
                                totalBeloep = resultSet.getLong("total_belop"),
                                opprettetTidspunkt = resultSet.getTimestamp("opprettet_tidspunkt")
                                    .toLocalDateTime(),
                            )
                        )
                    }
                }
            }
        }

        return list
    }

    override fun hentAggregertAntallOppdrag(transaksjonsId: UUID): Int {
        val sql = """
            SELECT COALESCE(sum(antall_oppdrag),0)
            FROM mellomlagring_konsistensavstemming
            WHERE transaksjons_id = ?::uuid
            """.trimIndent()

        var antallOppdrag = 0

        dataSource.connection.use {
            it.prepareStatement(sql).apply {
                setString(1, transaksjonsId.toString())

                executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        antallOppdrag = resultSet.getInt(1)
                    }
                }
            }
        }

        return antallOppdrag
    }

    override fun hentAggregertTotalBeloep(transaksjonsId: UUID): Long {
        val sql = """
            SELECT COALESCE(sum(total_belop),0)
            FROM mellomlagring_konsistensavstemming
            WHERE transaksjons_id = ?::uuid
            """.trimIndent()

        var totalBelop: Long = 0

        dataSource.connection.use {
            it.prepareStatement(sql).apply {
                setString(1, transaksjonsId.toString())

                executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        totalBelop = resultSet.getLong(1)
                    }
                }
            }
        }

        return totalBelop
    }

    override fun insert(mellomlagringKonsistensavstemming: MellomlagringKonsistensavstemming) {
        val sql = """
            INSERT INTO oppdrag_lager
            (id, fagsystem, transaksjons_id, antall_oppdrag, total_belop)
            VALUES (?::uuid, ?, ?::uuid, ?, ?)
            """.trimIndent()

        dataSource.connection.use {
            it.prepareStatement(sql).apply {
                setString(1, UUID.randomUUID().toString())
                setString(2, mellomlagringKonsistensavstemming.fagsystem)
                setString(3, mellomlagringKonsistensavstemming.transaksjonsId.toString())
                setInt(4, mellomlagringKonsistensavstemming.antallOppdrag)
                setLong(5, mellomlagringKonsistensavstemming.totalBeloep)

                executeUpdate()
            }
        }
    }
}
