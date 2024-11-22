package io.github.skydynamic.maiproberplus.core.data.maimai

import android.content.Context
import io.github.skydynamic.maiproberplus.Application
import io.github.skydynamic.maiproberplus.core.prober.client
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

val JSON = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

class MaimaiData {
    @Serializable
    data class Notes(
        val total: Int,
        val tap: Int,
        val hold: Int,
        val slide: Int,
        val touch: Int,
        @SerialName("break") val breakTotal: Int
    )

    @Serializable
    data class SongDiffculty(
        val type: MaimaiEnums.SongType,
        val difficulty: Int,
        val level: String,
        @SerialName("level_value") val levelValue: Float,
        @SerialName("note_designer") val noteDesigner: String,
        val version: Int,
        val notes: Notes
    )

    @Serializable
    data class SongDifficulties(val standard: List<SongDiffculty>, val dx: List<SongDiffculty>)

    @Serializable
    data class SongInfo(
        val id: Int, val title: String, val artist: String, val genre: String,
        val bpm: Int, val version: Int, val difficulties: SongDifficulties,
        val disabled: Boolean = false
    )

    @Serializable
    data class MusicDetail(
        val name: String, val level: Float,
        val score: Float, val dxScore: Int,
        val rating: Int, val version: Int,
        val type: MaimaiEnums.SongType, val diff: MaimaiEnums.Difficulty,
        val rankType: MaimaiEnums.RankType, val syncType: MaimaiEnums.SyncType,
        val fullComboType: MaimaiEnums.FullComboType
    )

    @Serializable
    data class LxnsSongListResponse(val songs: List<SongInfo>)

    companion object {
        var MAIMAI_SONG_LIST = readMaimaiSongList()

        @OptIn(DelicateCoroutinesApi::class)
        fun syncMaimaiSongList() {
            val context = Application.application
            var listFile = File(context.filesDir, "maimai_song_list.json")

            GlobalScope.launch(Dispatchers.IO) {
                val result =
                    client.get("https://maimai.lxns.net/api/v0/maimai/song/list?notes=true")
                listFile.deleteOnExit()
                listFile.createNewFile()
                val bufferedWriter =
                    context.openFileOutput("maimai_song_list.json", Context.MODE_PRIVATE)
                        .bufferedWriter()
                bufferedWriter.write(result.bodyAsText())
                bufferedWriter.close()
            }

            MAIMAI_SONG_LIST = readMaimaiSongList()
        }

        private fun readMaimaiSongList(): List<SongInfo> {
            return JSON.decodeFromString<LxnsSongListResponse>(
                Application.application.getFilesDirInputStream("maimai_song_list.json")
                    .bufferedReader().use { it.readText() }
            ).songs
        }

        fun getSongIdFromTitle(title: String): Int {
            val id = MAIMAI_SONG_LIST.find { it.title == title }?.id ?: -1
            return id
        }
    }
}