package com.geoshift.locationsimulator.util

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.util.MapTileIndex

object MapTileSources {

    // Google Maps Road (Tampilan standar seperti Grab / Gojek)
    val GOOGLE_ROAD: ITileSource = object : OnlineTileSourceBase(
        "GoogleRoad",
        0, 20, 256, ".png",
        arrayOf(
            "https://mt0.google.com/vt/lyrs=m&hl=id&",
            "https://mt1.google.com/vt/lyrs=m&hl=id&",
            "https://mt2.google.com/vt/lyrs=m&hl=id&",
            "https://mt3.google.com/vt/lyrs=m&hl=id&"
        )
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val zoom = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return "${baseUrl}x=$x&y=$y&z=$zoom"
        }
    }

    // Google Maps Hybrid (Satelit + Label Jalan seperti Grab/Gojek mode satelit)
    val GOOGLE_HYBRID: ITileSource = object : OnlineTileSourceBase(
        "GoogleHybrid",
        0, 20, 256, ".png",
        arrayOf(
            "https://mt0.google.com/vt/lyrs=y&hl=id&",
            "https://mt1.google.com/vt/lyrs=y&hl=id&",
            "https://mt2.google.com/vt/lyrs=y&hl=id&",
            "https://mt3.google.com/vt/lyrs=y&hl=id&"
        )
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val zoom = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return "${baseUrl}x=$x&y=$y&z=$zoom"
        }
    }

    // Google Maps Terrain / Medan
    val GOOGLE_TERRAIN: ITileSource = object : OnlineTileSourceBase(
        "GoogleTerrain",
        0, 20, 256, ".png",
        arrayOf(
            "https://mt0.google.com/vt/lyrs=p&hl=id&",
            "https://mt1.google.com/vt/lyrs=p&hl=id&",
            "https://mt2.google.com/vt/lyrs=p&hl=id&",
            "https://mt3.google.com/vt/lyrs=p&hl=id&"
        )
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val zoom = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return "${baseUrl}x=$x&y=$y&z=$zoom"
        }
    }

    // CartoDB Dark (Tactical Dark Mode)
    val CARTO_DARK: ITileSource = object : OnlineTileSourceBase(
        "CartoDark",
        0, 19, 256, ".png",
        arrayOf(
            "https://a.basemaps.cartocdn.com/dark_all/",
            "https://b.basemaps.cartocdn.com/dark_all/",
            "https://c.basemaps.cartocdn.com/dark_all/",
            "https://d.basemaps.cartocdn.com/dark_all/"
        )
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val zoom = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return "$baseUrl$zoom/$x/$y.png"
        }
    }

    // OpenStreetMap Standard
    val OSM_STANDARD: ITileSource = TileSourceFactory.MAPNIK
}
