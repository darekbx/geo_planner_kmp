package com.darekbx.geoplanner.kmp.map

class GPXCreator {

    fun readXml(contents: String): List<Pair<Double, Double>> {
        val segmentStart = contents.indexOf("<trkseg>") + 8
        val segmentEnd = contents.indexOf("</trkseg>")
        val pointsString = contents
            .substring(segmentStart, segmentEnd)
            .trim()
            .replace("\t", "")
            .replace("\r", "")
            .replace("\n", "")
            .replace("  ", "")
        val points = mutableListOf<Pair<Double, Double>>()

        pointsString.split("><").forEach { part ->
            val latStart = part.indexOf("lat=") + 5
            val latEnd = part.indexOf("\"", startIndex = latStart + 1)
            val lat = part.substring(latStart, latEnd)

            val lonStart = part.indexOf("lon=") + 5
            val lonEnd = part.indexOf("\"", startIndex = lonStart + 1)
            val lon = part.substring(lonStart, lonEnd)

            points.add(lat.toDouble() to lon.toDouble())
        }

        return points
    }

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
