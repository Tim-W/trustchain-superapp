package com.example.musicdao.wallet

import com.example.musicdao.MusicService
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test
import java.io.File

class WalletServiceTest {
    @Test
    fun startup() {
        val musicService = mockk<MusicService>()
        val saveDir = File("./src/test/resources")
        Assert.assertTrue(saveDir.isDirectory)
        every {
            musicService.applicationContext.cacheDir
        } returns saveDir
        // Start-up smoke test
        val service = WalletService.getInstance(saveDir, musicService, CryptoCurrencyConfigTest.networkParams, CryptoCurrencyConfigTest.chainFileName)
        service.start()

        // Allow some time for the wallet to start-up before accessing and verifying its methods
        Thread.sleep(2000)
        Assert.assertEquals("RUNNING", service.app.state().name)
        Assert.assertEquals("Status: RUNNING", service.status())

        Assert.assertEquals(
            "Current balance: 0.00 BTC (confirmed) \nCurrent balance: 0.00 BTC (estimated)",
            service.balanceText()
        )

        Assert.assertTrue(service.publicKey().isNotEmpty())
        Assert.assertTrue(service.publicKeyText().isNotEmpty())
    }
}
