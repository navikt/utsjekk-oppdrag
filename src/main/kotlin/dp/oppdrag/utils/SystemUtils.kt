package dp.oppdrag.utils

fun getProperty(name: String): String? {
    var value = System.getenv(name)
    if (value == null || value.isEmpty()) value = System.getProperty(name)

    return value
}
