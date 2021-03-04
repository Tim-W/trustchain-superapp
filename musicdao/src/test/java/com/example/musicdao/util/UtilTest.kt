package com.example.musicdao.util

import com.frostwire.jlibtorrent.Priority
import com.frostwire.jlibtorrent.Sha1Hash
import com.frostwire.jlibtorrent.TorrentHandle
import com.frostwire.jlibtorrent.TorrentInfo
import com.mpatric.mp3agic.InvalidDataException
import com.mpatric.mp3agic.Mp3File
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test
import java.io.File

class UtilTest {
    @Test
    fun calculatePieceIndex() {
        val torrentFile = this.javaClass.getResource("/RFBMP.torrent")?.path
        Assert.assertNotNull(torrentFile)
        if (torrentFile == null) return
        Assert.assertNotNull(File(torrentFile))
        val fileIndex = 1
        val torrentInfo = TorrentInfo(File(torrentFile))
        val x = Util.calculatePieceIndex(fileIndex, torrentInfo)
        Assert.assertEquals(82, x)
    }

    @Test
    fun extractInfoHashFromMagnet() {
        val magnet = "magnet:?xt=urn:btih:a83cc13bf4a07e85b938dcf06aa707955687ca7c&dn=displayname"
        val name = Util.extractInfoHash(magnet)
        Assert.assertEquals(name, Sha1Hash("a83cc13bf4a07e85b938dcf06aa707955687ca7c"))
        Assert.assertEquals(
            name.toString(),
            Sha1Hash("a83cc13bf4a07e85b938dcf06aa707955687ca7c").toString()
        )
        Assert.assertNotEquals(name, "somethingelse")
    }

    @Test
    fun extractNameFromMagnet() {
        val magnet = "magnet:?xt=urn:btih:a83cc13bf4a07e85b938dcf06aa707955687ca7c&dn=displayname"
        val name = Util.extractNameFromMagnet(magnet)
        Assert.assertEquals(name, "displayname")
        Assert.assertNotEquals(name, "somethingelse")
    }

    @Test
    fun readableBytes() {
        val eightMbs: Long = 1024 * 1024 * 8
        val eightKbs: Long = 1024 * 8
        val eightBytes: Long = 8
        Assert.assertEquals(Util.readableBytes(eightMbs), "8Mb")
        Assert.assertEquals(Util.readableBytes(eightKbs), "8Kb")
        Assert.assertEquals(Util.readableBytes(eightBytes), "8B")
    }

    @Test
    fun setSequentialPriorities() {
        val torrentFile = this.javaClass.getResource("/RFBMP.torrent")?.path
        Assert.assertNotNull(torrentFile)
        if (torrentFile == null) return
        Assert.assertNotNull(File(torrentFile))
        val torrentInfo = TorrentInfo(File(torrentFile))

        val priorities: Array<Priority> = arrayOf(
            Priority.SEVEN,
            Priority.SEVEN,
            Priority.SEVEN,
            Priority.NORMAL,
            Priority.IGNORE,
            Priority.NORMAL
            )
        val torrent = mockk<TorrentHandle>()
        every { torrent.torrentFile() } returns torrentInfo
        every { torrent.piecePriorities() } returns priorities
        val expectedPriorites: Array<Priority> = arrayOf(
            Priority.SIX,
            Priority.SIX,
            Priority.SIX,
            Priority.SIX,
            Priority.SIX,
            Priority.NORMAL
        )
        val answer = Util.setTorrentPriorities(torrent, onlyCalculating = true)
        Assert.assertArrayEquals(expectedPriorites, answer)
    }

    @Test
    fun findCoverArt() {
        Assert.assertNotNull(Util.findCoverArt(File("./src/test/resources"), true))
        Assert.assertNotNull(Util.findCoverArt(File("./src/test/resources/Royalty Free Background Music Pack"), true))
        Assert.assertNotNull(Util.findCoverArt(File("./src/test/resources/album"), true))
        Assert.assertNull(Util.findCoverArt(File("./src/test/resources/empty"), true))
    }

    @Test
    fun calculateDownloadProgress() {
        val progress = 10L
        val fileSize = 100L
        Assert.assertEquals(
            10,
            Util.calculateDownloadProgress(progress, fileSize)
        )
    }

    @Test
    fun trackMetadata() {
        val input = "Some-title.mp3"
        Assert.assertEquals(
            "title",
            Util.checkAndSanitizeTrackNames(input)
        )

        Assert.assertNull(
            Util.checkAndSanitizeTrackNames("invalid")
        )

        Assert.assertEquals(
            "DC10",
            Util.getTitle(Mp3File("./src/test/resources/Acoustic.mp3"))
        )

        Assert.assertEquals(
            "Aching",
            Util.getTitle(Mp3File("./src/test/resources/Alles-Aching.mp3"))
        )
    }

    @Test(expected = InvalidDataException::class)
    fun trackMetadataException() {
        Util.getTitle(Mp3File("./src/test/resources/RFBMP.torrent"))
    }

    @Test
    fun isTorrentCompleted() {
        Assert.assertFalse(
            Util.isTorrentCompleted(TorrentInfo(File("./src/test/resources/RFBMP.torrent")), File("./src/test/resources"))
        )
    }

    @Test
    fun addTrackersToMagnet() {
        val original = "asdf"
        val changedMagnet = Util.addTrackersToMagnet(original)
        Assert.assertTrue(changedMagnet.length > original.length)
    }

    @Test
    fun sanitizeString() {
        Assert.assertEquals(
            "Hello world",
            Util.sanitizeString("Hello%20world")
        )
    }
}
