package com.example.musicdao.localserver

import com.github.se_bastiaan.torrentstream.Torrent
import com.github.se_bastiaan.torrentstreamserver.FileType
import com.github.se_bastiaan.torrentstreamserver.nanohttpd.NanoHTTPD
import com.github.se_bastiaan.torrentstreamserver.nanohttpd.SimpleWebServer
import java.io.File
import java.io.IOException
import java.util.*

class TorrentStreamWebAudioServer(host: String?, port: Int) :
    SimpleWebServer(host, port, true) {
    private val EXTENSIONS: HashMap<String, AudioFileType> = HashMap()
    private var torrent: Torrent? = null
    private var srtSubtitleFile: File? = null
    private var vttSubtitleFile: File? = null
    fun setVideoTorrent(torrent: Torrent?) {
        this.torrent = torrent
    }

    fun setSrtSubtitleLocation(srtSubtitleFile: File?) {
        this.srtSubtitleFile = srtSubtitleFile
    }

    fun setVttSubtitleLocation(vttSubtitleFile: File?) {
        this.vttSubtitleFile = vttSubtitleFile
    }

    val streamUrl: String
        get() {
            val file = torrent!!.videoFile
            return "http://$hostname:$listeningPort/video" + file.absolutePath
                .substring(file.absolutePath.lastIndexOf('.'))
        }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val response: Response
        val extension = uri.substring(uri.lastIndexOf('.') + 1)
        val fileType: AudioFileType?
        if (EXTENSIONS.containsKey(extension)) {
            fileType = EXTENSIONS[extension]
            if (torrent != null) {
                response = serveTorrent(torrent!!, session)
                fileType!!.setHeaders(response)
            } else {
                response = notFoundResponse
            }
        } else {
            response = getForbiddenResponse("You can't access this location")
        }
        return response
    }

    private fun serveTorrent(
        torrent: Torrent,
        session: IHTTPSession
    ): Response {
        val file = torrent.videoFile
            ?: return newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                "",
                ""
            )
        val header = session.headers
        val mime = NanoHTTPD.getMimeTypeForFile(file.absolutePath)
        return try {
            val res: Response
            val etag =
                Integer.toHexString((file.absolutePath + file.length()).hashCode())
            var startFrom: Long = 0
            var endAt: Long = -1
            var range = header["range"]
            if (range != null) {
                if (range.startsWith("bytes=")) {
                    range = range.substring("bytes=".length)
                    val minus = range.indexOf('-')
                    try {
                        if (minus > 0) {
                            startFrom = range.substring(0, minus).toLong()
                            endAt = range.substring(minus + 1).toLong()
                        }
                    } catch (ignored: NumberFormatException) {
                    }
                }
            }

            // get if-range header. If present, it must match etag or else we
            // should ignore the range request
            val ifRange = header["if-range"]
            val headerIfRangeMissingOrMatching =
                ifRange == null || etag == ifRange
            val ifNoneMatch = header["if-none-match"]
            val headerIfNoneMatchPresentAndMatching =
                ifNoneMatch != null && ("*" == ifNoneMatch || ifNoneMatch == etag)

            // Change return code and add Content-Range header when skipping is
            // requested
            val fileLen = file.length()
            if (headerIfRangeMissingOrMatching && range != null && startFrom >= 0 && startFrom < fileLen) {
                // range request that matches current etag
                // and the startFrom of the range is satisfiable
                if (headerIfNoneMatchPresentAndMatching) {
                    // range request that matches current etag
                    // and the startFrom of the range is satisfiable
                    // would return range from file
                    // respond with not-modified
                    res = newFixedLengthResponse(
                        Response.Status.NOT_MODIFIED,
                        mime,
                        ""
                    )
                    res.addHeader("ETag", etag)
                } else {
                    if (endAt < 0) {
                        endAt = fileLen - 1
                    }
                    var newLen = endAt - startFrom + 1
                    if (newLen < 0) {
                        newLen = 0
                    }
                    torrent.setInterestedBytes(startFrom)
                    val inputStream = torrent.videoStream
                    inputStream.skip(startFrom)
                    res = NanoHTTPD.newFixedLengthResponse(
                        Response.Status.PARTIAL_CONTENT,
                        mime,
                        inputStream,
                        newLen
                    )
                    res.addHeader("Accept-Ranges", "bytes")
                    res.addHeader("Content-Length", "" + newLen)
                    res.addHeader(
                        "Content-Range",
                        "bytes $startFrom-$endAt/$fileLen"
                    )
                    res.addHeader("ETag", etag)
                }
            } else {
                if (headerIfRangeMissingOrMatching && range != null && startFrom >= fileLen) {
                    // return the size of the file
                    // 4xx responses are not trumped by if-none-match
                    res = newFixedLengthResponse(
                        Response.Status.RANGE_NOT_SATISFIABLE,
                        NanoHTTPD.MIME_PLAINTEXT,
                        ""
                    )
                    res.addHeader("Content-Range", "bytes */$fileLen")
                    res.addHeader("ETag", etag)
                } else if (range == null && headerIfNoneMatchPresentAndMatching) {
                    // full-file-fetch request
                    // would return entire file
                    // respond with not-modified
                    res = newFixedLengthResponse(
                        Response.Status.NOT_MODIFIED,
                        mime,
                        ""
                    )
                    res.addHeader("ETag", etag)
                } else if (!headerIfRangeMissingOrMatching && headerIfNoneMatchPresentAndMatching) {
                    // range request that doesn't match current etag
                    // would return entire (different) file
                    // respond with not-modified
                    res = newFixedLengthResponse(
                        Response.Status.NOT_MODIFIED,
                        mime,
                        ""
                    )
                    res.addHeader("ETag", etag)
                } else {
                    torrent.setInterestedBytes(0)
                    val inputStream = torrent.videoStream
                    res = NanoHTTPD.newFixedLengthResponse(
                        Response.Status.OK,
                        mime,
                        inputStream,
                        file.length()
                    )
                    res.addHeader("Accept-Ranges", "bytes")
                    res.addHeader("Content-Length", "" + fileLen)
                    res.addHeader("ETag", etag)
                }
            }
            res
        } catch (ioe: IOException) {
            newFixedLengthResponse(
                Response.Status.FORBIDDEN,
                NanoHTTPD.MIME_PLAINTEXT,
                "Forbidden"
            )
        }
    }

    init {
        val fileTypes =
            arrayOf(AudioFileType.MP3, AudioFileType.FLAC)
        for (localFileType in fileTypes) {
            EXTENSIONS[localFileType.extension] = localFileType
        }
    }
}
