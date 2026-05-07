package com.demo.kmp.android

import android.app.Application
import com.demo.kmp.android.dao.UserDao
import com.demo.kmp.android.dao.UserDaoLocalDataSource
import com.demo.kmp.platform.UserLocalDataSource
import com.demo.kmp.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.dsl.module

private val androidAppModule = module {
    single { UserDao() }
    single<UserLocalDataSource> { UserDaoLocalDataSource(get()) }
}

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@MainApplication)
            androidLogger()
            modules(androidAppModule)
        }
    }
}
