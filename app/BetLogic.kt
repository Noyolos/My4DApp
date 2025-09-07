package com.example.s66

data class Bet(val number: String, val b: Int = 0, val s: Int = 0, val a1: Int = 0)

data class BetGroup(val placeDigits: String, val betList: List<Bet>)

data class BetType(val b: Int, val s: Int, val a1: Int, val updateDefault: Boolean, val freezeDefault: Boolean)

fun detectType(
    line: String,
    defaultB: Int,
    defaultS: Int,
    defaultA1: Int,
    defaultFroze: Boolean
): BetType? {
    val parts = line.split("-")
    val number = parts[0].trim()

    val b = parts.getOrNull(1)?.takeIf { it.isNotBlank() }?.toIntOrNull()
    val s = parts.getOrNull(2)?.takeIf { it.isNotBlank() }?.toIntOrNull()
    val a1 = parts.getOrNull(3)?.takeIf { it.isNotBlank() }?.toIntOrNull()

    val count = listOf(b, s, a1).count { it != null }

    return when {
        count == 0 -> {
            if (defaultFroze) null
            else BetType(defaultB, defaultS, defaultA1, updateDefault = false, freezeDefault = false)
        }

        count == 1 -> {
            BetType(b ?: 0, s ?: 0, a1 ?: 0, updateDefault = true, freezeDefault = false)
        }

        count == 3 && b == s && s == a1 -> {
            BetType(b ?: 0, s ?: 0, a1 ?: 0, updateDefault = true, freezeDefault = false)
        }

        else -> {
            BetType(b ?: 0, s ?: 0, a1 ?: 0, updateDefault = false, freezeDefault = true)
        }
    }
}

fun parsePlaceAndNumberList(placeDigits: String, inputLine: List<String>): BetGroup {
    val betList = mutableListOf<Bet>()

    var defaultB = 0
    var defaultS = 0
    var defaultA1 = 0
    var defaultFroze = false

    for (line in inputLine) {
        val cleanline = line.trim()
        val parts = cleanline.split("-")
        val number = parts[0].trim()
        if (number.isEmpty()) continue

        val betType = detectType(cleanline, defaultB, defaultS, defaultA1, defaultFroze)
        if (betType == null) continue

        if (betType.updateDefault) {
            defaultB = betType.b
            defaultS = betType.s
            defaultA1 = betType.a1
        }

        if (betType.freezeDefault) {
            defaultFroze = true
        }

        betList.add(Bet(number, betType.b, betType.s, betType.a1))
    }

    return BetGroup(placeDigits, betList)
}
