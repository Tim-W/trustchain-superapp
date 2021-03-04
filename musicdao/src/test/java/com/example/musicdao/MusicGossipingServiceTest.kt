package com.example.musicdao

import com.example.musicdao.ipv8.MusicCommunity
import com.example.musicdao.ipv8.SwarmHealth
import com.frostwire.jlibtorrent.Sha1Hash
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

class MusicGossipingServiceTest {
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

    @ExperimentalCoroutinesApi
    @Test
    fun gossipSwarmHealthToOnePeer() = runBlockingTest {
        val community = spyk(getCommunity())
        val service = MusicGossipingService(community)

        val swarmHealthMap = mutableMapOf<Sha1Hash, SwarmHealth>()
        val newHash = Sha1Hash.min()
        swarmHealthMap[newHash] = SwarmHealth(newHash.toString(), 5.toUInt(), 5.toUInt())
        service.setSwarmHealthMap(swarmHealthMap)
        Assert.assertEquals(
            1,
            service.gossipSwarmHealth(swarmHealthMap, 34)
        )

//        val scope = CoroutineScope(Dispatchers.IO)
//
//
//        scope.launch {
//            service.iterativelySendReleaseBlocks()
//        }
//        Thread.sleep(500)
//        scope.cancel()
    }

    @Test
    fun sortGossipMap() {
        val community = spyk(getCommunity())
        val service = MusicGossipingService(community)

        val swarmHealthMap = mutableMapOf<Sha1Hash, SwarmHealth>()
        val newHash = Sha1Hash.min()
        val newHash2 = Sha1Hash.max()
        swarmHealthMap[newHash] = SwarmHealth(newHash.toString(), 0.toUInt(), 0.toUInt())
        swarmHealthMap[newHash2] = SwarmHealth(newHash2.toString(), 5.toUInt(), 5.toUInt())

        service.setSwarmHealthMap(swarmHealthMap)

        Assert.assertEquals(
            swarmHealthMap,
            service.sortAndGossip()
        )
    }
}
