package no.nav.dagpenger.oppdrag.iverksetting

import java.nio.ByteBuffer
import java.util.Base64
import java.util.UUID

object UuidUtils {

    fun UUID.komprimer(): String {
        val bb: ByteBuffer = ByteBuffer.allocate(java.lang.Long.BYTES * 2)
        bb.putLong(this.mostSignificantBits)
        bb.putLong(this.leastSignificantBits)
        val array: ByteArray = bb.array()
        return Base64.getEncoder().encodeToString(array)
    }

    fun String.dekomprimer(): UUID {
        val byteBuffer: ByteBuffer = ByteBuffer.wrap(Base64.getDecoder().decode(this))
        return UUID(byteBuffer.long, byteBuffer.long)
    }
}
