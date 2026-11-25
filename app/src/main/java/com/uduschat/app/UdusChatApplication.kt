package com.uduschat.app

import android.app.Application
import com.uduschat.app.di.UdusAppContainer

class UdusChatApplication : Application() {
    lateinit var container: UdusAppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = UdusAppContainer(this)
    }
}
