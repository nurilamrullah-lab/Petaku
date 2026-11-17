package com.geoshift.locationsimulator

import android.Manifest
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xxhdpi")
class ScreenPreviewTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val outDir = File(System.getProperty("previewOut") ?: "build/screen-previews").apply { mkdirs() }

    private fun shot(name: String) {
        repeat(5) { ShadowLooper.idleMainLooper(); rule.mainClock.advanceTimeBy(400) }
        rule.waitForIdle()
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        File(outDir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun tab(label: String) {
        rule.onNodeWithText(label, useUnmergedTree = true).performClick()
    }

    @Test
    fun captureScreens() {
        Shadows.shadowOf(rule.activity.application).grantPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
        )
        rule.mainClock.autoAdvance = false
        val labels = (System.getProperty("previewTabs") ?: "Beranda,Cari,Rute,Satelit,Riwayat,Atur").split(",")
        labels.forEachIndexed { i, label ->
            if (i > 0) tab(label)
            shot("%02d-%s".format(i + 1, label))
        }
    }
}
