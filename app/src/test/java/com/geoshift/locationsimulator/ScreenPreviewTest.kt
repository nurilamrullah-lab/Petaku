package com.geoshift.locationsimulator

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ActivityScenario
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xxhdpi")
class ScreenPreviewTest {

    private val outDir = File(System.getProperty("previewOut") ?: "build/screen-previews").apply { mkdirs() }

    private fun idle(ms: Long) {
        var t = 0L
        while (t < ms) {
            ShadowLooper.idleMainLooper(100, java.util.concurrent.TimeUnit.MILLISECONDS)
            t += 100
        }
    }

    private fun shot(root: View, name: String) {
        idle(1500)
        val bmp = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bmp))
        File(outDir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun tap(root: View, x: Float, y: Float) {
        val t = SystemClock.uptimeMillis()
        root.dispatchTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y, 0))
        idle(50)
        root.dispatchTouchEvent(MotionEvent.obtain(t, t + 50, MotionEvent.ACTION_UP, x, y, 0))
    }

    @Test
    fun captureScreens() {
        Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val tabs = listOf("Beranda", "Cari", "Rute", "Satelit", "Riwayat", "Atur")
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.window.decorView
                val d = activity.resources.displayMetrics.density
                idle(2000)
                tabs.forEachIndexed { i, label ->
                    if (i > 0) {
                        val itemW = (root.width - 8 * d) / tabs.size
                        tap(root, 4 * d + itemW * (i + 0.5f), root.height - 32 * d)
                    }
                    shot(root, "%02d-%s".format(i + 1, label))
                }
            }
        }
    }
}
