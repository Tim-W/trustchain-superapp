package com.example.musicdao

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.example.musicdao.ipv8.MusicCommunity
import com.example.musicdao.ipv8.SwarmHealth
import com.frostwire.jlibtorrent.Sha1Hash
import kotlinx.coroutines.*
import nl.tudelft.ipv8.android.IPv8Android
import kotlin.system.exitProcess

/**
 * This is a service that runs in the background, also when the Android app is closed. It gossips
 * data about Release blocks and swarm health with a few random peers every couple seconds
 */
class MusicGossipingService(private val musicCommunity: MusicCommunity? = IPv8Android.getInstance().getOverlay<MusicCommunity>()
) :
    Service() {
    private var swarmHealthMap: Map<Sha1Hash, SwarmHealth> = mutableMapOf()
    private val gossipTopTorrents = 5
    private val gossipRandomTorrents = 5
    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): MusicGossipingService = this@MusicGossipingService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            iterativelySendReleaseBlocks()
        }
        scope.launch {
            iterativelyGossipSwarmHealth()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()

        // We need to kill the app as IPv8 is started in Application.onCreate
        exitProcess(0)
    }

    fun setSwarmHealthMap(swarmHealthMap: Map<Sha1Hash, SwarmHealth>) {
        this.swarmHealthMap = swarmHealthMap
    }

    /**
     * This is a very simplistic way to crawl all chains from the peers you know
     */
    suspend fun iterativelySendReleaseBlocks() {
        while (scope.isActive) {
            musicCommunity?.communicateReleaseBlocks()
            delay(4000)
        }
    }

    /**
     * Every couple of seconds, gossip swarm health with other peers
     */
    suspend fun iterativelyGossipSwarmHealth() {
        while (scope.isActive) {
            sortAndGossip()
            delay(4000)
        }
    }

    fun sortAndGossip(): Map<Sha1Hash, SwarmHealth> {
        // Pick 5 of the most popular torrents and 5 random torrents, and send those stats to any neighbour
        // First, we sort the map based on swarm health
        val sortedMap = swarmHealthMap.toList()
            .sortedBy { (_, value) -> value }
            .toMap()
        gossipSwarmHealth(sortedMap, gossipTopTorrents)
        gossipSwarmHealth(swarmHealthMap, gossipRandomTorrents)
        return sortedMap
    }

    /**
     * Send SwarmHealth information to #maxIterations random peers
     */
    fun gossipSwarmHealth(map: Map<Sha1Hash, SwarmHealth>, maxInterations: Int): Int {
        var count = 0
        for (entry in map.entries) {
            if (count > maxInterations) break
            musicCommunity?.sendSwarmHealthMessage(entry.value)
            count += 1
        }
        return count
    }
}
