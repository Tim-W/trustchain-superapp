package com.example.musicdao

import com.example.musicdao.ipv8.MusicCommunity
import com.example.musicdao.ipv8.SwarmHealth
import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.Sha1Hash
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runBlockingTest
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.trustchain.TrustChainSettings
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainSQLiteStore
import nl.tudelft.ipv8.keyvault.JavaCryptoProvider
import nl.tudelft.ipv8.messaging.EndpointAggregator
import nl.tudelft.ipv8.peerdiscovery.Network
import nl.tudelft.ipv8.sqldelight.Database
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.util.*

class MusicServiceTest {
    private fun createTrustChainStore(): TrustChainSQLiteStore {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.create(driver)
        val database = Database(driver)
        return TrustChainSQLiteStore(database)
    }

    private fun getCommunity(): MusicCommunity {
        val settings = TrustChainSettings()
        val store = createTrustChainStore()
        val community = MusicCommunity.Factory(settings = settings, database = store).create()
        val newKey = JavaCryptoProvider.generateKey()
        community.myPeer = Peer(newKey)
        community.endpoint = spyk(EndpointAggregator(mockk(relaxed = true), null))
        community.network = Network()
        community.maxPeers = 20
        return community
    }

    @Test
    fun filterSwarmHealth() {
        val ses = SessionManager()
        val musicService = spyk<MusicService>()
        musicService.sessionManager = ses
        every {
            musicService.applicationContext.cacheDir
            musicService.cacheDir
        } returns File("./")

        val expectedSwarmHealthMap = mutableMapOf<Sha1Hash, SwarmHealth>()
        val a = SwarmHealth(Sha1Hash.min().toString(), 1.toUInt(), 0.toUInt())
        Assert.assertTrue(a.isUpToDate())

        // Outdated SwarmHealth item b; this one should be removed after filtering
        val oldDate = Date().time - 2 * 3600 * SwarmHealth.KEEP_TIME_HOURS * 1000
        val b = SwarmHealth(Sha1Hash.max().toString(), 1.toUInt(), 0.toUInt(), oldDate.toULong())

        val community = getCommunity()

        community.swarmHealthMap[Sha1Hash.min()] = a
        community.swarmHealthMap[Sha1Hash.max()] = b

        expectedSwarmHealthMap[Sha1Hash.min()] = a

        Assert.assertEquals(
            expectedSwarmHealthMap,
            musicService.filterSwarmHealthMap(community)
        )
    }

    @ExperimentalCoroutinesApi
    @Test
    fun getStatsOverview() = runBlockingTest {
        val ses = SessionManager()
        val musicService = spyk<MusicService>()
        musicService.sessionManager = ses
        Assert.assertEquals("Starting torrent client...", musicService.getStatsOverview())
        musicService.sessionManager?.start()
        // Make sure the SessionManager has started correctly
        delay(1000)
        Assert.assertNotEquals("Starting torrent client...", musicService.getStatsOverview())
    }

    @Test
    fun smokeTests() {
        val musicService = spyk<MusicService>()
        musicService.registerBlockSigner(getCommunity())
        musicService.iterativelyUpdateSwarmHealth()
    }
}
