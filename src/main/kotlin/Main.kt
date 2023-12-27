import javazoom.jl.decoder.JavaLayerException
import javazoom.jl.player.advanced.AdvancedPlayer
import javazoom.jl.player.advanced.PlaybackEvent
import javazoom.jl.player.advanced.PlaybackListener
import java.io.*
import java.util.*

data class Subtitle(
    val index: Int,
    val startTime: String,
    val endTime: String,
    val text: String
)

class SrtEngine(private val filePath: String) {
    private val subtitles: MutableList<Subtitle> = mutableListOf()

    init {
        loadSubtitles()
    }

    private fun loadSubtitles() {
        try {
            val file = File(filePath)
            val reader = BufferedReader(FileReader(file))

            var currentIndex = 0
            var currentStartTime = ""
            var currentEndTime = ""
            val currentText = StringBuilder()

            var line: String?

            while (reader.readLine().also { line = it } != null) {

                val sanitizedLine = line!!.replace(Regex("[^\\x20-\\x7E]+"), "")

                when {
                    sanitizedLine.isBlank() -> {
                        if (currentIndex > 0) {
                            subtitles.add(
                                Subtitle(
                                    currentIndex,
                                    currentStartTime,
                                    currentEndTime,
                                    currentText.toString().trim()
                                )
                            )
                        }
                        currentIndex = 0
                        currentStartTime = ""
                        currentEndTime = ""
                        currentText.clear()
                    }

                    currentIndex == 0 -> currentIndex = sanitizedLine.toIntOrNull() ?: 0
                    currentStartTime.isEmpty() -> {
                        val timeParts = sanitizedLine.split("-->")
                        if (timeParts.size == 2) {
                            currentStartTime = timeParts[0].trim()
                            currentEndTime = timeParts[1].trim()
                        }
                    }

                    else -> currentText.append("$sanitizedLine\n")
                }
            }
            if (currentIndex > 0) {
                subtitles.add(
                    Subtitle(
                        currentIndex,
                        currentStartTime,
                        currentEndTime,
                        currentText.toString().trim()
                    )
                )
            }

            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun getSubtitles(): List<Subtitle> {
        return subtitles
    }
}

fun main() {

    val srtFilePath = "src/main/resources/assets/neffex.srt"
    val srtEngine = SrtEngine(srtFilePath)

    val subtitleList = srtEngine.getSubtitles()

    startSubtitles(subtitleList)
    startMediaPlayer()

}

fun startSubtitles(subtitles : List<Subtitle>){
    val timer = Timer()
    var currentSecond = 0
    var prevSubtitleIndex : Int = -1

    timer.scheduleAtFixedRate(object : TimerTask() {
        override fun run() {

            currentSecond++

            val subtitle = getSubtitleAtSecond(currentSecond, subtitles)

            subtitle?.let {
                if (it.index != prevSubtitleIndex){
                    println(it.text)
                    prevSubtitleIndex = it.index
                }
            }


        }
    }, 1000, 1000)
}

fun startMediaPlayer(){
    val mp3FilePath = "src/main/resources/assets/neffex.mp3"

    try {
        val fileInputStream = FileInputStream(mp3FilePath)

        val player = AdvancedPlayer(fileInputStream)

        player.play()

    } catch (e: FileNotFoundException) {

        e.printStackTrace()

    } catch (e: JavaLayerException) {

        e.printStackTrace()

    }
}

fun getSubtitleAtSecond(second: Int, subtitles : List<Subtitle>): Subtitle? {
    return subtitles.find { subtitle ->
        val startTimeInSeconds = subtitle.startTime.toSeconds()
        val endTimeInSeconds = subtitle.endTime.toSeconds()
        second in startTimeInSeconds until endTimeInSeconds
    }
}

fun String.toSeconds(): Int {
    val parts = this.split(":")
    if (parts.size == 3) {
        val hours = parts[0].toInt()
        val minutes = parts[1].toInt()
        val seconds = parts[2].split(",")[0].toInt()
        return hours * 3600 + minutes * 60 + seconds
    }
    return 0
}




