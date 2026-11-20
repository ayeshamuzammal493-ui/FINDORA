package com.example.data.repo

import com.example.data.db.FoundReportEntity
import com.example.data.db.LostReportEntity
import com.example.data.db.MatchEntity
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Deterministic, non-AI matching and geospatial calculation engine.
 * Uses Haversine distance, category matching, tokenized keyword overlap, and date proximity.
 */
object MatchingEngine {

    private val stopWords = setOf(
        "the", "and", "for", "with", "near", "from", "left", "lost", "found",
        "item", "area", "zone", "this", "that", "have", "been", "into"
    )

    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    fun tokenize(text: String): Set<String> {
        return text.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 3 && it !in stopWords }
            .toSet()
    }

    fun scoreMatch(lost: LostReportEntity, found: FoundReportEntity): Pair<Int, String>? {
        // Do not match a user's own lost item with their own found report
        if (lost.userId == found.userId) return null

        var score = 0
        val reasons = mutableListOf<String>()

        // 1. Category alignment (+35)
        if (lost.category.equals(found.category, ignoreCase = true)) {
            score += 35
            reasons += "Same category (${lost.category})"
        }

        // 2. Tokenized keyword overlap (+0..35)
        val lostTokens = tokenize("${lost.title} ${lost.description} ${lost.colorBrand}")
        val foundTokens = tokenize("${found.title} ${found.description}")
        val overlap = lostTokens.intersect(foundTokens)
        if (overlap.isNotEmpty()) {
            val kwScore = (overlap.size * 10).coerceAtMost(35)
            score += kwScore
            reasons += "${overlap.size} keyword match${if (overlap.size > 1) "es" else ""}"
        }

        // 3. Haversine geographic distance (+0..20)
        val distanceKm = haversineKm(lost.latitude, lost.longitude, found.latitude, found.longitude)
        when {
            distanceKm <= 2.0 -> {
                score += 20
                reasons += String.format("%.1f km apart", distanceKm)
            }
            distanceKm <= 10.0 -> {
                score += 14
                reasons += String.format("%.1f km apart", distanceKm)
            }
            distanceKm <= 35.0 -> {
                score += 8
                reasons += String.format("%.0f km apart", distanceKm)
            }
            distanceKm <= 100.0 -> {
                score += 3
                reasons += String.format("%.0f km apart", distanceKm)
            }
        }

        // 4. Date proximity (+0..10)
        val dayDiff = abs(lost.dateLostMillis - found.dateFoundMillis) / 86_400_000L
        when {
            dayDiff <= 2L -> score += 10
            dayDiff <= 7L -> score += 6
            dayDiff <= 21L -> score += 3
        }

        if (score < 45) return null
        return score.coerceIn(0, 99) to reasons.joinToString(" • ")
    }

    fun buildMatchEntity(
        lost: LostReportEntity,
        found: FoundReportEntity,
        existingId: String? = null
    ): MatchEntity? {
        val scored = scoreMatch(lost, found) ?: return null
        val dist = haversineKm(lost.latitude, lost.longitude, found.latitude, found.longitude)
        return MatchEntity(
            id = existingId ?: "match_${lost.id}_${found.id}",
            lostReportId = lost.id,
            foundReportId = found.id,
            score = scored.first,
            distanceKm = dist,
            reasonSummary = scored.second,
            status = "SUGGESTED"
        )
    }

    fun isNearDuplicateTitle(titleA: String, titleB: String): Boolean {
        val tokensA = tokenize(titleA)
        val tokensB = tokenize(titleB)
        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            return titleA.trim().equals(titleB.trim(), ignoreCase = true)
        }
        val overlap = tokensA.intersect(tokensB).size
        val minSize = minOf(tokensA.size, tokensB.size)
        return overlap.toDouble() / minSize.toDouble() >= 0.65
    }
}
