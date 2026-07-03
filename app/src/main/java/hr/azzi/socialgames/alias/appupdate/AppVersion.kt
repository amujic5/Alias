package hr.azzi.socialgames.alias.appupdate

/**
 * A dotted numeric version ("1.10.0") compared component-by-component so that
 * `1.10.0` is correctly newer than `1.9.0` (numeric, not string, comparison).
 * Missing trailing components count as `0` ("1.2" == "1.2.0"); non-numeric parts
 * count as `0`.
 */
data class AppVersion(val components: List<Int>) : Comparable<AppVersion> {

    constructor(raw: String) : this(raw.split(".").map { it.trim().toIntOrNull() ?: 0 })

    override fun compareTo(other: AppVersion): Int {
        val count = maxOf(components.size, other.components.size)
        for (i in 0 until count) {
            val l = components.getOrElse(i) { 0 }
            val r = other.components.getOrElse(i) { 0 }
            if (l != r) return l.compareTo(r)
        }
        return 0
    }
}
