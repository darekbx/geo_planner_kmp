package com.darekbx.geoplanner.kmp.map

class GPXCreator {

    fun createXml(points: List<Pair<Double, Double>>): String {
        return buildString {
            append(TOP)
            points.forEach { (lat, lng) ->
                append("<trkpt lat=\"$lat\" lon=\"$lng\" />")
            }
            append(BOTTOM)
        }
    }

    private val TOP = """
<?xml version="1.0" encoding="UTF-8"?>
<gpx>
  <metadata>
    <name>New track (2025-12-07 09:36:24)</name>
  </metadata>
  <trk>
    <trkseg>
""".trimIndent()

    private val BOTTOM = """
    </trkseg>
  </trk>
</gpx> 
""".trimIndent()
}
