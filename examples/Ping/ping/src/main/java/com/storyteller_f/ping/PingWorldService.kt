package com.storyteller_f.ping

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.Choreographer
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.google.android.filament.Colors
import com.google.android.filament.Entity
import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
import com.google.android.filament.Renderer
import com.google.android.filament.Skybox
import com.google.android.filament.SwapChain
import com.google.android.filament.Viewport
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.FilamentHelper
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.Animator
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.MaterialProvider
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import com.google.android.filament.utils.Float3
import com.google.android.filament.utils.Manipulator
import com.google.android.filament.utils.Utils
import com.google.android.filament.utils.max
import com.google.android.filament.utils.scale
import com.google.android.filament.utils.translation
import com.google.android.filament.utils.transpose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.Buffer
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext

private const val kNearPlane = 0.05f     // 5 cm
private const val kFarPlane = 1000.0f    // 1 km

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
        private var currentThumbnail: Bitmap? = null
        var cameraFocalLength = 28f
            set(value) {
                field = value
                updateCameraProjection()
            }

        var cameraNear = kNearPlane
            set(value) {
                field = value
                updateCameraProjection()
            }

        var cameraFar = kFarPlane
            set(value) {
                field = value
                updateCameraProjection()
            }

        // UiHelper is provided by Filament to manage SurfaceHolder
        private val uiHelper by lazy {
            UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply {
                renderCallback = SurfaceCallback()
                attachTo(surfaceHolder)
            }

        }

        // DisplayHelper is provided by Filament to manage the display
        private val displayHelper = DisplayHelper(inContext)

        // Choreographer is used to schedule new frames
        private val choreographer = Choreographer.getInstance()

        // Engine creates and destroys Filament resources
        // Each engine must be accessed from a single thread of your choosing
        // Resources cannot be shared across engines
        private val engine = com.google.android.filament.Engine.create()

        // A renderer instance is tied to a single surface (SurfaceView, TextureView, etc.)
        private val renderer = engine.createRenderer()

        // A scene holds all the render-able, lights, etc. to be drawn
        private val scene1 = engine.createScene().apply {
            @Entity
            val light = EntityManager.get().create()
            skybox = Skybox.Builder().build(engine)
            val (r, g, b) = Colors.cct(6_500.0f)
            LightManager.Builder(LightManager.Type.DIRECTIONAL)
                .color(r, g, b)
                .intensity(100_000.0f)
                .direction(0.0f, -1.0f, 0.0f)
                .castShadows(true)
                .build(engine, light)

            addEntity(light)
        }

        // Should be pretty obvious :)
        private val camera1 = engine.createCamera(engine.entityManager.create()).apply {
            // Set the exposure on the camera, this exposure follows the sunny f/16 rule
            setExposure(16.0f, 1.0f / 125.0f, 100.0f)
        }

        // A view defines a viewport, a scene and a camera for rendering
        private val view = engine.createView().apply {
            // NOTE: Try to disable post-processing (tone-mapping, etc.) to see the difference
            // view.isPostProcessingEnabled = false

            // Tell the view which camera we want to use
            this.camera = camera1

            // Tell the view which scene we want to render
            this.scene = scene1
        }

        private val cameraManipulator: Manipulator by lazy {
            val width = view.viewport.width
            val height = view.viewport.height
            Manipulator.Builder()
                .targetPosition(
                    kDefaultObjectPosition.x,
                    kDefaultObjectPosition.y,
                    kDefaultObjectPosition.z
                )
                .viewport(width, height)
                .build(Manipulator.Mode.ORBIT)
        }

        private val gestureDetector: GestureDetector by lazy {
            GestureDetector(view, cameraManipulator)
        }

        var animator: Animator? = null
            private set

        var asset: FilamentAsset? = null
            private set


        private val materialProvider: MaterialProvider = UbershaderProvider(engine)
        private val assetLoader: AssetLoader = AssetLoader(
            engine,
            materialProvider,
            EntityManager.get()
        )
        private val resourceLoader: ResourceLoader = ResourceLoader(engine, true)
        private val eyePos = DoubleArray(3)
        private val target = DoubleArray(3)
        private val upward = DoubleArray(3)


        // A swap chain is Filament's representation of a surface
        private var swapChain: SwapChain? = null

        // Performs the rendering and schedules new frames
        private val frameScheduler = FrameCallback()

        // We'll use this ValueAnimator to smoothly cycle the background between hues.
        private val animator1 = ValueAnimator.ofFloat(0.0f, 360.0f).apply {
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

        private val readyRenderables = IntArray(128) // add up to 128 entities at a time
        val display = ContextCompat.getDisplayOrDefault(inContext)

        private fun observeLatestUri() {
            scope.launch {
                inContext.worldDataStore.data.mapNotNull { preferences ->
                    // No type safety.
                    preferences.selectedWallPaper()
                }.distinctUntilChanged().collectLatest { modelFile: String ->
                    val wrap = withContext(Dispatchers.IO) {
                        ByteBuffer.wrap(
                            FileInputStream(modelFile).buffered()
                                .readBytes()
                        )
                    }
                    loadModelGltf(wrap) {
                        ByteBuffer.wrap(
                            FileInputStream(File(File(modelFile).parentFile, it)).buffered()
                                .readBytes()
                        )
                    }
                    transformToUnitCube()
                }
            }
        }

        fun transformToUnitCube(centerPoint: Float3 = kDefaultObjectPosition) {
            asset?.let { asset ->
                val tm = engine.transformManager
                var center = asset.boundingBox.center.let { v -> Float3(v[0], v[1], v[2]) }
                val halfExtent = asset.boundingBox.halfExtent.let { v -> Float3(v[0], v[1], v[2]) }
                val maxExtent = 2.0f * max(halfExtent)
                val scaleFactor = 2.0f / maxExtent
                center -= centerPoint / scaleFactor
                val transform = scale(Float3(scaleFactor)) * translation(-center)
                tm.setTransform(tm.getInstance(asset.root), transpose(transform).toFloatArray())
            }
        }

        fun destroyModel() {
            resourceLoader.asyncCancelLoad()
            resourceLoader.evictResourceData()
            asset?.let { asset ->
                scene1.removeEntities(asset.entities)
                assetLoader.destroyAsset(asset)
                this.asset = null
                animator = null
            }
        }

        suspend fun loadModelGltf(buffer: Buffer, callback: suspend (String) -> Buffer?) {
            destroyModel()
            asset = assetLoader.createAsset(buffer)
            asset?.let { asset ->
                for (uri in asset.resourceUris) {
                    val resourceBuffer = callback(uri)
                    if (resourceBuffer == null) {
                        this.asset = null
                        return
                    }
                    resourceLoader.addResourceData(uri, resourceBuffer)
                }
                resourceLoader.asyncBeginLoad(asset)
                animator = asset.instance.animator
                asset.releaseSourceData()
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            surfaceHolder.setSizeFromLayout()
            surfaceHolder.setFormat(PixelFormat.RGBA_8888)

            uiHelper
            observeLatestUri()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                choreographer.postFrameCallback(frameScheduler)
                animator1.start()
            } else {
                choreographer.removeFrameCallback(frameScheduler)
                animator1.cancel()
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            destroyModel()
            assetLoader.destroy()
            materialProvider.destroyMaterials()
            materialProvider.destroy()
            resourceLoader.destroy()

            // Stop the animation and any pending frame
            choreographer.removeFrameCallback(frameScheduler)
            animator1.cancel()

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

                    // Allow the resource loader to finalize textures that have become ready.
                    resourceLoader.asyncUpdateLoad()

                    // Add renderable entities to the scene as they become ready.
                    asset?.let { populateScene(it) }

                    // Extract the camera basis from the helper and push it to the Filament camera.
                    cameraManipulator.getLookAt(eyePos, target, upward)
                    camera1.lookAt(
                        eyePos[0], eyePos[1], eyePos[2],
                        target[0], target[1], target[2],
                        upward[0], upward[1], upward[2]
                    )

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

        private fun populateScene(asset: FilamentAsset) {
            val rcm = engine.renderableManager
            var count = 0
            val popRenderables = { count = asset.popRenderables(readyRenderables); count != 0 }
            while (popRenderables()) {
                for (i in 0 until count) {
                    val ri = rcm.getInstance(readyRenderables[i])
                    rcm.setScreenSpaceContactShadows(ri, true)
                }
                scene1.addEntities(readyRenderables.take(count).toIntArray())
            }
            scene1.addEntities(asset.lightEntities)
        }


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
                Log.d(TAG, "onResized() called with: width = $width, height = $height")
                view.viewport = Viewport(0, 0, width, height)
                cameraManipulator.setViewport(width, height)
                updateCameraProjection()
                FilamentHelper.synchronizePendingFrames(engine)
            }
        }

        private fun updateCameraProjection() {
            val width = view.viewport.width
            val height = view.viewport.height
            val aspect = width.toDouble() / height.toDouble()
            camera1.setLensProjection(
                cameraFocalLength.toDouble(), aspect,
                cameraNear.toDouble(), cameraFar.toDouble()
            )
        }

        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            Log.d(TAG, "onSurfaceCreated() called with: holder = $holder")
            super.onSurfaceCreated(holder)
            gestureDetector
        }

        override fun onTouchEvent(event: MotionEvent?) {
            super.onTouchEvent(event)
            gestureDetector.onTouchEvent(event ?: return)
        }
    }


    companion object {
        private const val TAG = "PingPagerService"
        private val kDefaultObjectPosition = Float3(0.0f, 0.0f, -4.0f)

        init {
            Utils.init()
        }
    }
}