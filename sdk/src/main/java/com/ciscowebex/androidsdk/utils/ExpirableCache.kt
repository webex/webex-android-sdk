package com.ciscowebex.androidsdk.utils

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class ExpirableCache<T> (autoReleaseInSeconds: Int = WITHOUT_AUTORELEASE, val defaultKeepaliveInMillis: Long = KEEPALIVE_FOREVER) {

    companion object {
        const val WITHOUT_AUTORELEASE: Int = 0
        const val KEEPALIVE_FOREVER: Long = 0
    }

    internal class CacheValue<T>(
            val value: T,
            private val creationDate: Long,
            private val keepAliveInMillis: Long) {

        fun isAlive(now: Long): Boolean {
            return when (keepAliveInMillis) {
                KEEPALIVE_FOREVER -> true
                else -> creationDate + keepAliveInMillis > now
            }
        }
    }

    private val cache: ConcurrentHashMap<String, CacheValue<T>> = ConcurrentHashMap()

    private var executorService: ScheduledExecutorService? = null

    init {
        if (autoReleaseInSeconds > 0) {
            executorService = Executors.newSingleThreadScheduledExecutor()
            executorService?.scheduleAtFixedRate({ purge() }, autoReleaseInSeconds.toLong(), autoReleaseInSeconds.toLong(), TimeUnit.SECONDS)
        }
    }

    fun shutdown() {
        clear()
        executorService?.shutdown()
    }

    operator fun set(key: String, value: T) {
        set(key, value, defaultKeepaliveInMillis)
    }

    operator fun set(key: String, value: T, keepAliveUnits: Long, timeUnit: TimeUnit) {
        set(key, value, timeUnit.toMillis(keepAliveUnits))
    }

    operator fun set(key: String, value: T, keepAliveInMillis: Long) {
        if (keepAliveInMillis >= 0) {
            cache[key] = CacheValue(value, now(), keepAliveInMillis)
        }
    }

    fun getOrDefault(key: String, defaultValue: T): T = get(key) ?: defaultValue

    fun getAndPurgeIfDead(key: String) = get(key, purgeIfDead = true)

    operator fun get(key: String) = get(key, purgeIfDead = false)

    fun size() = sizeAliveElements()

    operator fun contains(key: String) = get(key) != null

    fun remove(key: String) {
        cache.remove(key)
    }

    fun isEmpty() = sizeAliveElements() == 0

    fun clear() = cache.clear()

    fun keySet() = keySetAlive()

    private fun keySetDeadAndAlive(): List<String> = cache.keys().toList()

    private fun keySetAlive(): List<String> {
        return keySetDeadAndAlive().filter {
            isKeyAlive(it)
        }
    }

    private fun keySetDead(): List<String> {
        return keySetDeadAndAlive().filter {
            isKeyDead(it)
        }
    }

    private fun keySetStartingWith(start: String?): List<String> {
        if (start == null) return Collections.emptyList()

        return keySetDeadAndAlive().filter {
            it.startsWith(start)
        }
    }

    private fun keySetAliveStartingWith(start: String?): List<String> {
        if (start == null) return Collections.emptyList()

        return keySetStartingWith(start).filter {
            isKeyAlive(it)
        }
    }

    private fun get(key: String, purgeIfDead: Boolean): T? {
        val retrievedValue = cache[key]
        return when {
            retrievedValue == null -> null
            retrievedValue.isAlive(now()) -> retrievedValue.value
            else -> {
                if (purgeIfDead) {
                    cache.remove(key)
                }
                null
            }
        }
    }

    private fun isKeyAlive(key: String): Boolean {
        val value = cache[key] ?: return false
        return value.isAlive(now())
    }

    private fun isKeyDead(key: String) = !isKeyAlive(key)

    private fun sizeAliveElements(): Int = cache.values.filter { it.isAlive(now()) }.count()

    private fun sizeDeadElements() = cache.size - sizeAliveElements()

    private fun sizeDeadAndAliveElements() = cache.size

    private fun purge() {
        val it = cache.entries.iterator()
        while (it.hasNext()) {
            val next = it.next()
            if (isKeyDead(next.key)) {
                it.remove()
            }
        }
    }

    private fun now(): Long = System.currentTimeMillis()

}