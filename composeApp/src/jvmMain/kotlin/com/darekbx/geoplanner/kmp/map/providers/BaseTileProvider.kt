package com.darekbx.geoplanner.kmp.map.providers

import ovh.plrapps.mapcompose.core.TileStreamProvider

interface BaseTileProvider {
    fun create(): TileStreamProvider
}