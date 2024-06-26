package no.nav.utsjekk.felles.log

import java.util.UUID

object IdUtils {
    @JvmStatic fun generateId(): String {
        val uuid = UUID.randomUUID()
        return java.lang.Long.toHexString(uuid.mostSignificantBits) + java.lang.Long.toHexString(uuid.leastSignificantBits)
    }
}
