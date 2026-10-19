package com.minimalstudio.app

import android.app.Application
import com.minimalstudio.app.data.StudioDatabase
import com.minimalstudio.app.data.StudioRepository

class StudioApplication : Application() {
    val database by lazy { StudioDatabase.get(this) }
    val repository by lazy { StudioRepository(this, database) }
}
