package no.nav.dagpenger.oppdrag.iverksetting.domene

import java.nio.ByteBuffer
import java.util.Base64
import java.util.UUID

internal object UuidKomprimator {
    fun UUID.komprimer(): String {
        val byteBuffer =
            ByteBuffer.allocate(java.lang.Long.BYTES * 2).apply {
                putLong(mostSignificantBits)
                putLong(leastSignificantBits)
            }

        return Base64.getEncoder().encodeToString(byteBuffer.array())
    }

    fun String.dekomprimer(): UUID {
        val byteBuffer: ByteBuffer = ByteBuffer.wrap(Base64.getDecoder().decode(this))

        return UUID(byteBuffer.long, byteBuffer.long)
    }
}
