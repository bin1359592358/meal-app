package com.meals.app

import android.app.Application
import com.meals.app.data.local.Preferences

class MealsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        Preferences.init(this)
    }

    companion object {
        lateinit var instance: MealsApplication
            private set
    }
}
