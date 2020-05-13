package com.example.musicdao.localserver

import com.github.se_bastiaan.torrentstream.StreamStatus
import com.github.se_bastiaan.torrentstream.Torrent
import com.github.se_bastiaan.torrentstreamserver.TorrentServerListener
import java.lang.Exception

class TorrentStreamHTTPServer: TorrentServerListener {
    override fun onStreamReady(torrent: Torrent?) {
        println("stream ready: $torrent")
    }

    override fun onStreamPrepared(torrent: Torrent?) {
        println("stream prepared: $torrent")
    }

    override fun onStreamStopped() {
        println("stream stopped")
    }

    override fun onStreamStarted(torrent: Torrent?) {
        println("stream started: $torrent")
    }

    override fun onStreamProgress(torrent: Torrent?, status: StreamStatus?) {
        println("stream progress: $torrent $status")
    }

    override fun onServerReady(url: String?) {
        println("Server ready")
    }

    override fun onStreamError(torrent: Torrent?, e: Exception?) {
        e?.printStackTrace()
    }

}
