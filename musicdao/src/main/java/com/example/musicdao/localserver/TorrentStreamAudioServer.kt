package com.example.musicdao.localserver

import android.content.Context
import com.example.musicdao.MusicService
import com.example.musicdao.instance
import com.github.se_bastiaan.torrentstream.StreamStatus
import com.github.se_bastiaan.torrentstream.Torrent
import com.github.se_bastiaan.torrentstream.TorrentOptions
import com.github.se_bastiaan.torrentstream.TorrentStream
import com.github.se_bastiaan.torrentstream.listeners.TorrentListener
import com.github.se_bastiaan.torrentstreamserver.TorrentServerListener
import com.github.se_bastiaan.torrentstreamserver.TorrentStreamNotInitializedException
import com.github.se_bastiaan.torrentstreamserver.TorrentStreamWebServer
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.random.Random.Default.Companion

lateinit var torrentStreamAudioServerInstance: TorrentStreamAudioServer

class TorrentStreamAudioServer private constructor() {
    private var serverHost: String? = null
    private var serverPort: Int? = null
    private val listeners: MutableList<TorrentServerListener> =
        ArrayList()
    var options: TorrentOptions
        private set
    private var torrentStream: TorrentStream? = null
    private var torrentStreamWebServer: TorrentStreamWebAudioServer? = null
    private val internalListener: TorrentListener = InternalTorrentServerListener()

    fun setTorrentOptions(torrentOptions: TorrentOptions) {
        options = torrentOptions
        if (torrentStream != null) {
            torrentStream!!.options = torrentOptions
        }
    }

    fun setServerHost(serverHost: String?) {
        this.serverHost = serverHost
    }

    fun setServerPort(serverPort: Int?) {
        this.serverPort = serverPort
    }

    val isStreaming: Boolean
        get() = if (torrentStream == null) {
            false
        } else torrentStream!!.isStreaming

    fun resumeSession() {
        if (torrentStream != null) {
            torrentStream!!.resumeSession()
        }
    }

    fun pauseSession() {
        if (torrentStream != null) {
            torrentStream!!.pauseSession()
        }
    }

    val currentTorrentUrl: String?
        get() {
            return if (torrentStream == null) {
                null
            } else torrentStream!!.currentTorrentUrl
        }

    val totalDhtNodes: Int
        get() {
            return if (torrentStream == null) {
                0
            } else torrentStream!!.totalDhtNodes
        }

    val currentTorrent: Torrent?
        get() {
            return if (torrentStream == null) {
                null
            } else torrentStream!!.currentTorrent
        }

    val currentStreamUrl: String?
        get() {
            return if (torrentStreamWebServer == null || !torrentStreamWebServer!!.wasStarted()) {
                null
            } else torrentStreamWebServer!!.streamUrl
        }

    fun addListener(listener: TorrentServerListener?) {
        if (listener != null) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: TorrentServerListener?) {
        if (listener != null) {
            listeners.remove(listener)
        }
    }

    fun startTorrentStream() {
        torrentStream = TorrentStream.init(options)
        torrentStream?.addListener(internalListener)
    }

    fun stopTorrentStream() {
        if (torrentStream != null && torrentStream!!.isStreaming) {
            torrentStream!!.stopStream()
        }
        torrentStream = null
    }
    /**
     * Start stream download for specified torrent
     *
     * @param torrentUrl [String] .torrent or magnet link
     * @param srtSubtitleFile [File] SRT subtitle
     * @param vttSubtitleFile [File] VTT subtitle
     */
    /**
     * Start stream download for specified torrent
     *
     * @param torrentUrl [String] .torrent or magnet link
     */
    @JvmOverloads
    @Throws(TorrentStreamNotInitializedException::class, IOException::class)
    fun startStream(
        torrentUrl: String?,
        srtSubtitleFile: File? = null,
        vttSubtitleFile: File? = null
    ) {
        if (torrentStream == null) {
            throw TorrentStreamNotInitializedException()
        }
        torrentStream!!.startStream(torrentUrl)
        torrentStreamWebServer = TorrentStreamWebAudioServer(serverHost, serverPort!!)
        torrentStreamWebServer!!.setSrtSubtitleLocation(srtSubtitleFile)
        torrentStreamWebServer!!.setVttSubtitleLocation(vttSubtitleFile)
        torrentStreamWebServer!!.start()
    }

    /**
     * Set SRT subtitle file of active stream
     * @param file [File] SRT subtitle
     */
    fun setStreamSrtSubtitle(file: File?) {
        if (torrentStreamWebServer != null && torrentStreamWebServer!!.wasStarted()) {
            torrentStreamWebServer!!.setSrtSubtitleLocation(file)
        }
    }

    /**
     * Set SRT subtitle file of active stream
     * @param file [File] VTT subtitle
     */
    fun setStreamVttSubtitle(file: File?) {
        if (torrentStreamWebServer != null && torrentStreamWebServer!!.wasStarted()) {
            torrentStreamWebServer!!.setVttSubtitleLocation(file)
        }
    }

    /**
     * Stop current torrent stream
     */
    fun stopStream() {
        if (torrentStreamWebServer != null && torrentStreamWebServer!!.wasStarted()) {
            torrentStreamWebServer!!.stop()
        }
        if (torrentStream != null && torrentStream!!.isStreaming) {
            torrentStream!!.stopStream()
        }
    }

    private inner class InternalTorrentServerListener : TorrentServerListener {
        override fun onServerReady(url: String) {
            for (listener in listeners) {
                listener.onServerReady(url)
            }
        }

        override fun onStreamPrepared(torrent: Torrent) {
            for (listener in listeners) {
                listener.onStreamPrepared(torrent)
            }
        }

        override fun onStreamStarted(torrent: Torrent) {
            for (listener in listeners) {
                listener.onStreamStarted(torrent)
            }
        }

        override fun onStreamError(
            torrent: Torrent,
            e: Exception
        ) {
            for (listener in listeners) {
                listener.onStreamError(torrent, e)
            }
        }

        override fun onStreamReady(torrent: Torrent) {
            for (listener in listeners) {
                listener.onStreamReady(torrent)
            }
            torrentStreamWebServer!!.setVideoTorrent(torrent)
            onServerReady(torrentStreamWebServer!!.streamUrl)
        }

        override fun onStreamProgress(
            torrent: Torrent,
            streamStatus: StreamStatus
        ) {
            for (listener in listeners) {
                listener.onStreamProgress(torrent, streamStatus)
            }
        }

        override fun onStreamStopped() {
            for (listener in listeners) {
                listener.onStreamStopped()
            }
        }
    }

    companion object {
        fun getInstance(): TorrentStreamAudioServer {
            if (!::torrentStreamAudioServerInstance.isInitialized) {
                createInstance()
            }
            return torrentStreamAudioServerInstance
        }

        @Synchronized
        private fun createInstance() {
            torrentStreamAudioServerInstance = TorrentStreamAudioServer()
        }
    }

    init {
        options = TorrentOptions.Builder().build()
    }
}
