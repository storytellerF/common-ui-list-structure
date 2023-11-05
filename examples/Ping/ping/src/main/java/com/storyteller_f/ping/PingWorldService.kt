package com.storyteller_f.ping

import android.animation.ValueAnimator
import android.app.ActivityManager
import android.app.WallpaperColors
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceHolder
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.google.android.filament.Camera
import com.google.android.filament.EntityManager
import com.google.android.filament.Filament
import com.google.android.filament.Renderer
import com.google.android.filament.Skybox
import com.google.android.filament.SwapChain
import com.google.android.filament.Viewport
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.FilamentHelper
import com.google.android.filament.android.UiHelper
import com.storyteller_f.ping.shader.GLES20WallpaperRenderer
import com.storyteller_f.ping.shader.GLES30WallpaperRenderer
import com.storyteller_f.ping.shader.GLWallpaperRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.CoroutineContext

class PingWorldService : WallpaperService() {
    val job = Job()
    val scope = object : CoroutineScope {
        override val coroutineContext: CoroutineContext
            get() = job + Dispatchers.Main

    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind() called with: intent = $intent")
        return super.onUnbind(intent)
    }

    override fun onCreateEngine(): Engine {
        Log.d(TAG, "onCreateEngine() called")
        return FilamentWallpaperEngine(this)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy() called")
        super.onDestroy()
        job.cancel()
    }

    private inner class FilamentWallpaperEngine(val inContext: Context) : Engine() {

        // UiHelper is provided by Filament to manage SurfaceHolder
        private val uiHelper by lazy {
            UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply {
                renderCallback = SurfaceCallback()
                attachTo(surfaceHolder)
            }

        }

        // DisplayHelper is provided by Filament to manage the display
        private val displayHelper by lazy {
            DisplayHelper(inContext)
        }

        // Choreographer is used to schedule new frames
        private val choreographer by lazy {
            Choreographer.getInstance()
        }

        // Engine creates and destroys Filament resources
        // Each engine must be accessed from a single thread of your choosing
        // Resources cannot be shared across engines
        private val engine by lazy {
            com.google.android.filament.Engine.create()
        }

        // A renderer instance is tied to a single surface (SurfaceView, TextureView, etc.)
        private val renderer by lazy {
            engine.createRenderer()
        }

        // A scene holds all the render-able, lights, etc. to be drawn
        private val scene1 by lazy {
            engine.createScene().apply {
                skybox = Skybox.Builder().build(engine)
            }
        }

        // Should be pretty obvious :)
        private val camera1 by lazy {
            engine.createCamera(engine.entityManager.create()).apply {
                // Set the exposure on the camera, this exposure follows the sunny f/16 rule
                setExposure(16.0f, 1.0f / 125.0f, 100.0f)
            }
        }

        // A view defines a viewport, a scene and a camera for rendering
        private val view by lazy {
            engine.createView().apply {
                // NOTE: Try to disable post-processing (tone-mapping, etc.) to see the difference
                // view.isPostProcessingEnabled = false

                // Tell the view which camera we want to use
                this.camera = camera1

                // Tell the view which scene we want to render
                this.scene = scene1
            }
        }

        // A swap chain is Filament's representation of a surface
        private var swapChain: SwapChain? = null

        // Performs the rendering and schedules new frames
        private val frameScheduler = FrameCallback()

        // We'll use this ValueAnimator to smoothly cycle the background between hues.
        private val animator by lazy {
            ValueAnimator.ofFloat(0.0f, 360.0f).apply {
                interpolator = LinearInterpolator()
                duration = 10000
                repeatMode = ValueAnimator.RESTART
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener { a ->
                    val hue = a.animatedValue as Float
                    val color = Color.HSVToColor(floatArrayOf(hue, 1.0f, 1.0f))
                    scene1.skybox?.setColor(
                        floatArrayOf(
                            Color.red(color) / 255.0f,
                            Color.green(color) / 255.0f,
                            Color.blue(color) / 255.0f,
                            1.0f
                        )
                    )
                }
                start()
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            surfaceHolder.setSizeFromLayout()
            surfaceHolder.setFormat(PixelFormat.RGBA_8888)

            //init
            renderer
            view
            displayHelper
            choreographer
            uiHelper
            //启动动画
            animator
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                choreographer.postFrameCallback(frameScheduler)
                animator.start()
            } else {
                choreographer.removeFrameCallback(frameScheduler)
                animator.cancel()
            }
        }

        override fun onDestroy() {
            super.onDestroy()

            // Stop the animation and any pending frame
            choreographer.removeFrameCallback(frameScheduler)
            animator.cancel()

            // Always detach the surface before destroying the engine
            uiHelper.detach()

            engine.cleanUpEngine()
        }

        private fun com.google.android.filament.Engine.cleanUpEngine() {
            // Cleanup all resources
            destroyRenderer(renderer)
            destroyView(view)
            destroyScene(scene1)
            val entity = camera1.entity
            destroyCameraComponent(entity)
            EntityManager.get().destroy(entity)

            // Destroying the engine will free up any resource you may have forgotten
            // to destroy, but it's recommended to do the cleanup properly
            destroy()
        }

        private fun com.google.android.filament.Engine.safeDestroySwapChain(chain: SwapChain) {
            destroySwapChain(chain)
            // Required to ensure we don't return before Filament is done executing the
            // destroySwapChain command, otherwise Android might destroy the Surface
            // too early
            flushAndWait()
        }

        inner class FrameCallback : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                // Schedule the next frame
                choreographer.postFrameCallback(this)

                // This check guarantees that we have a swap chain
                if (uiHelper.isReadyToRender) {
                    renderer.drawFrame(frameTimeNanos)
                }
            }

            private fun Renderer.drawFrame(frameTimeNanos: Long) {
                // If beginFrame() returns false you should skip the frame
                // This means you are sending frames too quickly to the GPU
                if (beginFrame(swapChain!!, frameTimeNanos)) {
                    render(view)
                    endFrame()
                }
            }
        }

        val display by lazy { ContextCompat.getDisplayOrDefault(inContext) }

        inner class SurfaceCallback : UiHelper.RendererCallback {
            override fun onNativeWindowChanged(surface: Surface) {
                swapChain?.let { engine.destroySwapChain(it) }
                swapChain = engine.createSwapChain(surface)

                displayHelper.attach(renderer, display)
            }

            override fun onDetachedFromSurface() {
                displayHelper.detach()
                swapChain?.let {
                    engine.safeDestroySwapChain(it)
                    swapChain = null
                }
            }

            override fun onResized(width: Int, height: Int) {
                val aspect = width.toDouble() / height.toDouble()
                camera1.setProjection(45.0, aspect, 0.1, 20.0, Camera.Fov.VERTICAL)

                view.viewport = Viewport(0, 0, width, height)

                FilamentHelper.synchronizePendingFrames(engine)
            }
        }
    }

    companion object {
        private const val TAG = "PingPagerService"

        init {
            Filament.init()
        }
    }
}