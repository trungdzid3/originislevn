package com.originisle.android.island

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IslandTransitionsTest {

    @Before
    fun setUp() {
        IslandTransitions.clearAll()
    }

    @Test
    fun onPostStarted_incrementsGenerationAndClearsPendingRemoval() {
        val cardId = 35001
        val gen1 = IslandTransitions.onPostStarted(cardId)
        IslandTransitions.markPendingRemoval(cardId, gen1)

        val gen2 = IslandTransitions.onPostStarted(cardId)

        assertTrue(gen2 > gen1)
        assertEquals(gen2, IslandTransitions.currentGeneration(cardId))
        assertFalse(IslandTransitions.shouldExecutePendingRemoval(cardId, gen1))
    }

    @Test
    fun pendingRemoval_executesOnlyOnceForMatchingToken() {
        val cardId = 35004
        val gen = IslandTransitions.onPostStarted(cardId)
        IslandTransitions.markPendingRemoval(cardId, gen)

        assertFalse(IslandTransitions.shouldExecutePendingRemoval(cardId, gen + 1))
        assertTrue(IslandTransitions.shouldExecutePendingRemoval(cardId, gen))
        assertFalse(IslandTransitions.shouldExecutePendingRemoval(cardId, gen))
    }

    @Test
    fun onCancelRequested_ignoresStaleExpectedGen() {
        val cardId = 35008
        val staleGen = IslandTransitions.onPostStarted(cardId)
        val currentGen = IslandTransitions.onPostStarted(cardId)

        assertFalse(IslandTransitions.onCancelRequested(cardId, staleGen))
        assertEquals(currentGen, IslandTransitions.currentGeneration(cardId))

        assertTrue(IslandTransitions.onCancelRequested(cardId, currentGen))
        assertNull(IslandTransitions.currentGeneration(cardId))
        assertFalse(IslandTransitions.onCancelRequested(cardId, currentGen))
    }

    @Test
    fun onCancelRequested_withoutExpectedGenClearsState() {
        val cardId = 35009
        val gen = IslandTransitions.onPostStarted(cardId)
        IslandTransitions.markPendingRemoval(cardId, gen)

        assertTrue(IslandTransitions.onCancelRequested(cardId))
        assertNull(IslandTransitions.currentGeneration(cardId))
        assertFalse(IslandTransitions.shouldExecutePendingRemoval(cardId, gen))
    }

    @Test
    fun shutdownGeneration_tracksLatestGeneration() {
        val gen1 = IslandTransitions.nextShutdownGeneration()
        assertTrue(IslandTransitions.isShutdownCurrent(gen1))

        val gen2 = IslandTransitions.nextShutdownGeneration()
        assertFalse(IslandTransitions.isShutdownCurrent(gen1))
        assertTrue(IslandTransitions.isShutdownCurrent(gen2))
    }

    @Test
    fun clearAll_resetsActiveAndPendingState() {
        val cardId = 40001
        val gen = IslandTransitions.onPostStarted(cardId)
        IslandTransitions.markPendingRemoval(cardId, gen)
        val shutdownGen = IslandTransitions.nextShutdownGeneration()

        IslandTransitions.clearAll()

        assertNull(IslandTransitions.currentGeneration(cardId))
        assertFalse(IslandTransitions.shouldExecutePendingRemoval(cardId, gen))
        assertFalse(IslandTransitions.onCancelRequested(cardId, gen))
        assertTrue(IslandTransitions.isShutdownCurrent(shutdownGen))
    }
}
