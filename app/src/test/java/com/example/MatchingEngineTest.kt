package com.example

import com.example.data.db.FoundReportEntity
import com.example.data.db.LostReportEntity
import com.example.data.repo.MatchingEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchingEngineTest {

    @Test
    fun haversineKm_calculatesReasonableCityDistance() {
        // JFK to Midtown Manhattan is roughly ~20-22 km
        val km = MatchingEngine.haversineKm(40.6413, -73.7781, 40.7527, -73.9772)
        assertTrue("Expected ~20.8 km, got $km", km in 19.0..23.0)
    }

    @Test
    fun scoreMatch_highScoreForMatchingCategoryKeywordsLocationDate() {
        val now = 1_700_000_000_000L
        val lost = LostReportEntity(
            id = "lost_1",
            userId = "user_a",
            title = "Navy Canvas Backpack",
            category = "Bags & Luggage",
            description = "Minimalist navy canvas backpack with leather straps",
            dateLostMillis = now,
            approxLocationName = "Midtown",
            city = "New York",
            latitude = 40.7527,
            longitude = -73.9772
        )
        val found = FoundReportEntity(
            id = "found_1",
            userId = "user_b",
            title = "Dark Navy Canvas Backpack",
            category = "Bags & Luggage",
            description = "Found navy backpack near Midtown bench",
            dateFoundMillis = now + 3_600_000L,
            approxLocationName = "Midtown",
            city = "New York",
            latitude = 40.7520,
            longitude = -73.9765,
            hiddenIdentifyingDetails = "Pin inside pocket",
            verificationQuestion = "What pin is inside?"
        )
        val result = MatchingEngine.scoreMatch(lost, found)
        assertNotNull(result)
        assertTrue("Expected score >= 80, got ${result!!.first}", result.first >= 80)
    }

    @Test
    fun scoreMatch_excludesSameUserReports() {
        val now = 1_700_000_000_000L
        val lost = LostReportEntity(
            id = "lost_1",
            userId = "same_user",
            title = "Silver Watch",
            category = "Watches & Jewelry",
            description = "Silver watch",
            dateLostMillis = now,
            approxLocationName = "JFK",
            city = "New York",
            latitude = 40.6413,
            longitude = -73.7781
        )
        val found = FoundReportEntity(
            id = "found_1",
            userId = "same_user",
            title = "Silver Watch",
            category = "Watches & Jewelry",
            description = "Silver watch",
            dateFoundMillis = now,
            approxLocationName = "JFK",
            city = "New York",
            latitude = 40.6413,
            longitude = -73.7781,
            hiddenIdentifyingDetails = "Engraved",
            verificationQuestion = "Engraving?"
        )
        assertNull(MatchingEngine.scoreMatch(lost, found))
    }

    @Test
    fun isNearDuplicateTitle_detectsSimilarTitles() {
        assertTrue(MatchingEngine.isNearDuplicateTitle("Navy Canvas Commuter Backpack", "Navy Canvas Backpack"))
        assertFalse(MatchingEngine.isNearDuplicateTitle("Navy Canvas Backpack", "Silver Wireless Headphones"))
    }
}
