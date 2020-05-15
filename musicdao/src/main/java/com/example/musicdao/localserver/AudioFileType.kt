package com.example.musicdao.localserver

import com.github.se_bastiaan.torrentstreamserver.FileType
import com.github.se_bastiaan.torrentstreamserver.nanohttpd.NanoHTTPD

class AudioFileType private constructor(
    val extension: String,
    val mimeType: String,
    val dlnaContentFeatures: String,
    val dlnaTransferMode: String
) {
    fun setHeaders(response: NanoHTTPD.Response) {
        setHeaders(response, null)
    }

    fun setHeaders(response: NanoHTTPD.Response, subtitlesLocation: String?) {
        response.addHeader("contentFeatures.dlna.org", dlnaContentFeatures)
        response.addHeader("TransferMode.DLNA.ORG", dlnaTransferMode)
        response.addHeader("DAAP-Server", "iTunes/11.0.5 (OS X)")
        response.addHeader("Last-Modified", "2015-01-01T10:00:00Z")
        response.mimeType = mimeType
        if (subtitlesLocation != null) {
            response.addHeader("CaptionInfo.sec", subtitlesLocation)
        }
    }

    companion object {
        val MP3 =
            AudioFileType(
                "mp3",
                "audio/mp3",
                "*",
                "Streaming"
            )
        val FLAC =
            AudioFileType(
                "flac",
                "audio/flac",
                "*",
                "Streaming"
            )
    }

}
