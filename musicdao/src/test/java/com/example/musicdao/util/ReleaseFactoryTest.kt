package com.example.musicdao.util

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test

class ReleaseFactoryTest {
    @Test
    fun uriListFromLocalFiles() {
        val intent = mockk<Intent>()
        val uri = mockk<Uri>()

        every { intent.data } returns uri
        every { intent.clipData } returns null
        val list = ReleaseFactory.uriListFromLocalFiles(intent)
        Assert.assertEquals(1, list.size)

        val cd = ClipData.newRawUri("a", uri)
        every {
            intent.clipData
        } returns cd

        val list2 = ReleaseFactory.uriListFromLocalFiles(intent)
        Assert.assertEquals(1, list2.size)
    }
}
