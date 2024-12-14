package com.example.audioandvideoeditor.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class ComposeLifecycleObserver : DefaultLifecycleObserver {

    private var create:(() -> Unit)? = null
    private var start:(() -> Unit)? = null
    private var resume:(() -> Unit)? = null
    private var pause:(() -> Unit)? = null
    private var stop:(() -> Unit)? = null
    private var destroy:(() -> Unit)? = null

    fun onLifeCreate(scope:() -> Unit){
        this.create=scope
    }

    fun onLifeStart(scope:() -> Unit){
        this.start=scope
    }

    fun onLifeResume(scope:() -> Unit){
        this.resume=scope
    }

    fun onLifePause(scope:() -> Unit){
        this.pause=scope
    }

    fun onLifeStop(scope:() -> Unit){
        this.stop=scope
    }

    fun onLifeDestroy( scope:() -> Unit){
        this.destroy=scope
    }
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        create?.invoke()
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        start?.invoke()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        resume?.invoke()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        pause?.invoke()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        stop?.invoke()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        destroy?.invoke()
    }
}