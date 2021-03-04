package com.example.musicdao.catalog

import com.example.musicdao.ipv8.MusicCommunity
import com.example.musicdao.ipv8.SwarmHealth
import com.frostwire.jlibtorrent.Sha1Hash
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import io.mockk.mockk
import io.mockk.spyk
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainSQLiteStore
import nl.tudelft.ipv8.keyvault.JavaCryptoProvider
import nl.tudelft.ipv8.messaging.EndpointAggregator
import nl.tudelft.ipv8.peerdiscovery.Network
import nl.tudelft.ipv8.sqldelight.Database
import nl.tudelft.ipv8.util.hexToBytes
import org.junit.Assert
import org.junit.Test
import java.util.*

class PlaylistsOverviewFragmentTest {
    private val privateKey =
        JavaCryptoProvider.keyFromPrivateBin("4c69624e61434c534b3a069c289bd6031de93d49a8c35c7b2f0758c77c7b24b97842d08097abb894d8e98ba8a91ebc063f0687909f390b7ed9ec1d78fcc462298b81a51b2e3b5b9f77f2".hexToBytes())
    private val bitcoinPublicKey = "some-key"
    private val wellStructuredBlock = TrustChainBlock(
        "publish_release",
        TransactionEncoding.encode(
            mapOf(
                "magnet" to "magnet:?xt=urn:btih:a83cc13bf4a07e85b938dcf06aa707955687ca7c",
                "title" to "title",
                "artists" to "artists",
                "date" to "date",
                "torrentInfoName" to "torrentInfoName",
                "publisher" to bitcoinPublicKey
            )
        ),
        privateKey.pub().keyToBin(),
        GENESIS_SEQ,
        ANY_COUNTERPARTY_PK,
        UNKNOWN_SEQ,
        GENESIS_HASH,
        EMPTY_SIG,
        Date(0)
    )

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

    private val wrongStructuredBlock = TrustChainBlock(
        "publish_release",
        TransactionEncoding.encode(
            mapOf(
                "magnet" to "",
                "title" to "",
                "artists" to "",
                "date" to "",
                "torrentInfoName" to ""
            )
        ),
        privateKey.pub().keyToBin(),
        GENESIS_SEQ,
        ANY_COUNTERPARTY_PK,
        UNKNOWN_SEQ,
        GENESIS_HASH,
        EMPTY_SIG,
        Date(0)
    )

    @Test
    fun showAllReleases() {
        val releaseOverviewFragment = PlaylistsOverviewFragment()
        val releaseBlocks = mapOf<TrustChainBlock, Int>(
            wellStructuredBlock to 0, wrongStructuredBlock to 0
        )

        val community = getCommunity()
        val a = SwarmHealth(Sha1Hash.max().toString(), 1.toUInt(), 0.toUInt())
        val swarmHealthMap = mutableMapOf<Sha1Hash, SwarmHealth>()
        swarmHealthMap[Sha1Hash.max()] = a

        community.database.addBlock(wellStructuredBlock)

        releaseOverviewFragment.showAllReleases(swarmHealthMap, community)
        Assert.assertEquals(
            1,
            releaseOverviewFragment.refreshReleaseBlocks(releaseBlocks)
        )
    }

    @Test
    fun publish() {
        val releaseOverviewFragment = PlaylistsOverviewFragment()

        val community = getCommunity()
        Assert.assertTrue(
            releaseOverviewFragment.publish("a", "b", "c", "d", "e", community)
        )
    }
}
