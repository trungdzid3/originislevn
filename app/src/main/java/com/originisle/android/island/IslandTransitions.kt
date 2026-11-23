package com.originisle.android.island

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe state coordinator for OriginIsland card generations, pending removals,
 * and service shutdown generations so stale delayed callbacks cannot cancel reposted cards.
 */
object IslandTransitions {

    private val genSeq = AtomicLong(0L)
    private val shutdownGen = AtomicLong(0L)
    private val activeGenerations = ConcurrentHashMap<Int, Long>()
    private val pendingRemovals = ConcurrentHashMap<Int, Long>()

    /**
     * Records a new post/repost for [id], clears any pending delayed removal for [id],
     * and returns the new monotonically increasing generation token.
     */
    fun onPostStarted(id: Int): Long {
        val nextGen = genSeq.incrementAndGet()
        activeGenerations[id] = nextGen
        pendingRemovals.remove(id)
        return nextGen
    }

    /** Returns the current active generation for [id], or `null` if none is active. */
    fun currentGeneration(id: Int): Long? = activeGenerations[id]

    /**
     * Validates and applies a cancel request for [id].
     *
     * When [expectedGen] is non-null, returns `false` if [id] is not currently at [expectedGen]
     * (stale auto-dismiss or delayed cancel). Otherwise removes active state for [id] and
     * returns `true`.
     */
    fun onCancelRequested(id: Int, expectedGen: Long? = null): Boolean {
        if (expectedGen != null) {
            val removed = activeGenerations.remove(id, expectedGen)
            if (!removed) return false
            pendingRemovals.remove(id)
            return true
        }
        activeGenerations.remove(id)
        pendingRemovals.remove(id)
        return true
    }

    /** Marks [id] as having a pending delayed removal associated with [token]. */
    fun markPendingRemoval(id: Int, token: Long) {
        pendingRemovals[id] = token
    }

    /**
     * Returns `true` and consumes the pending removal only if [id] still has a matching
     * [token] and has not been superseded by [onPostStarted] or another removal.
     */
    fun shouldExecutePendingRemoval(id: Int, token: Long): Boolean {
        return pendingRemovals.remove(id, token)
    }

    /** Advances and returns the service shutdown generation. */
    fun nextShutdownGeneration(): Long = shutdownGen.incrementAndGet()

    /** Returns `true` if [gen] matches the latest shutdown generation. */
    fun isShutdownCurrent(gen: Long): Boolean = shutdownGen.get() == gen

    /** Clears active card generations and pending removals (for shutdown and unit tests). */
    fun clearAll() {
        activeGenerations.clear()
        pendingRemovals.clear()
    }
}
