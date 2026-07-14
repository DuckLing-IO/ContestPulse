package io.duckling.contestpulse.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class ContestPulseDestinationTest {
    @Test
    fun routesAreUnique() {
        val routes = ContestPulseDestination.entries.map { it.route }

        assertEquals(routes.size, routes.distinct().size)
    }

    @Test
    fun bottomNavigationContainsOnlyThreeRequestedDestinations() {
        val routes = ContestPulseDestination.entries.map { it.route }

        assertEquals(listOf("contests", "favorites", "settings"), routes)
    }
}
