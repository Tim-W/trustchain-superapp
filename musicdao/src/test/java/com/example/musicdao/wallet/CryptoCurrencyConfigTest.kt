package com.example.musicdao.wallet

import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.MainNetParams

object CryptoCurrencyConfigTest {
    val networkParams: NetworkParameters = MainNetParams.get()
    const val chainFileName: String = "forwarding-service"
}
