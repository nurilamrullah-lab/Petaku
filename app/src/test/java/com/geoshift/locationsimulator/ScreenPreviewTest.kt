package com.geoshift.locationsimulator

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import org.robolectric.Robolectric
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowChoreographer
import org.robolectric.shadows.ShadowDialog
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
        val canvas = Canvas(bmp)
        root.draw(canvas)
        // Bottom sheet / dialog tampil di jendela sendiri; gambar di atas activity
        ShadowDialog.getLatestDialog()?.takeIf { it.isShowing }?.window?.decorView?.draw(canvas)
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
        // Animasi tak berujung (denyut GPS, badge) membuat vsync otomatis memajukan jam tanpa henti
        ShadowChoreographer.setPaused(true)
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(16))
        Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val tabs = listOf("Peta", "Cari", "Rute", "Satelit", "Riwayat", "Atur")
        val controller = Robolectric.buildActivity(MainActivity::class.java).create().start().resume()
        idle(500)
        controller.visible()
        val activity = controller.get()
        val root = activity.window.decorView
        val d = activity.resources.displayMetrics.density
        idle(2000)
        tabs.forEachIndexed { i, label ->
            if (i > 0) {
                val itemW = (root.width - 12 * d) / tabs.size
                tap(root, 6 * d + itemW * (i + 0.5f), root.height - 36 * d)
            }
            shot(root, "%02d-%s".format(i + 1, label))
        }

        // Peta: mock diam aktif
        val itemW = (root.width - 12 * d) / tabs.size
        fun openTab(i: Int) { tap(root, 6 * d + itemW * (i + 0.5f), root.height - 36 * d); idle(800) }
        // Koordinat diukur pada screenshot lebar 723 px
        val k = root.width / 723f
        fun tapAt(x: Float, y: Float) { tap(root, x * k, y * k); idle(600) }
        openTab(0)
        tapAt(361f, 1358f) // Aktifkan Mock Lokasi
        shot(root, "07-Peta-mock-aktif")
        tapAt(361f, 1358f) // Hentikan Mock
        idle(500)

        // Rute: pilih tujuan cepat
        openTab(2)
        tapAt(556f, 1240f) // chip "Alun-Alun Jember"
        waitReal(9000)
        idle(6000)
        shot(root, "08-Rute-tujuan")
    }

    /** Kalkulasi rute berjalan di Dispatchers.IO; beri waktu nyata sambil memproses looper. */
    private fun waitReal(ms: Long) {
        val end = System.currentTimeMillis() + ms
        while (System.currentTimeMillis() < end) {
            Thread.sleep(100)
            idle(100)
        }
    }

}
