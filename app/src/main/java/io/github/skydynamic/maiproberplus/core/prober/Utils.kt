package io.github.skydynamic.maiproberplus.core.prober

import android.util.Log
import io.github.skydynamic.maiproberplus.Application.Companion.application
import io.github.skydynamic.maiproberplus.GlobalViewModel
import io.github.skydynamic.maiproberplus.core.data.chuni.ChuniData
import io.github.skydynamic.maiproberplus.core.data.chuni.ChuniEnums
import io.github.skydynamic.maiproberplus.core.data.maimai.MaimaiData
import io.github.skydynamic.maiproberplus.core.data.maimai.MaimaiEnums
import io.github.skydynamic.maiproberplus.core.utils.ParseScorePageUtil
import io.github.skydynamic.maiproberplus.core.utils.WechatRequestUtil.WX_WINDOWS_UA
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.cookies.get
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.setCookie
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 30000
        connectTimeoutMillis = 30000
    }
    install(HttpCookies) {
        storage = AcceptAllCookiesStorage()
    }
}

val chuniUrls = listOf(
    listOf("record/musicGenre/sendBasic", "record/musicGenre/basic"),
    listOf("record/musicGenre/sendAdvanced", "record/musicGenre/advanced"),
    listOf("record/musicGenre/sendExpert", "record/musicGenre/expert"),
    listOf("record/musicGenre/sendMaster", "record/musicGenre/master"),
    listOf("record/musicGenre/sendUltima", "record/musicGenre/ultima"),
    listOf(null, "record/worldsEndList/"),
    listOf(null, "home/playerData/ratingDetailRecent/")
)

fun sendMessageToUi(message: String) {
     CoroutineScope(Dispatchers.Main).launch {
        GlobalViewModel.sendAndShowMessage(message)
     }
}

suspend fun getMaimaiScoreData(authUrl: String) : List<MaimaiData.MusicDetail> {
    val scores = mutableListOf<MaimaiData.MusicDetail>()
    fetchMaimaiScorePage(authUrl) { diff, body ->
        scores.addAll(ParseScorePageUtil.parseMaimai(body, diff))
    }
    if (application.configManager.config.localConfig.cacheScore) {
        writeMaimaiScoreCache(scores)
    }
    return scores
}

suspend fun fetchMaimaiScorePage(
    authUrl: String,
    processBody: suspend (MaimaiEnums.Difficulty, String) -> Unit
) {
    client.get(authUrl) {
        getDefaultWahlapRequestBuilder()
    }

    val result = client.get("https://maimai.wahlap.com/maimai-mobile/home/")

    if (result.bodyAsText().contains("错误")) {
        sendMessageToUi("获取舞萌成绩失败: 登录失败")
        Log.e("ProberUtil", "登录失败, 抓取成绩停止")
        return
    }

    for (diff in application.configManager.config.syncConfig.maimaiSyncDifficulty) {
        val difficulty = MaimaiEnums.Difficulty.getDifficultyWithIndex(diff)

        Log.i("ProberUtil", "开始抓取${difficulty.diffName}成绩")
        try {
            with(client) {
                val scoreResp = get(
                    "https://maimai.wahlap.com/maimai-mobile/record/" +
                            "musicGenre/search/?genre=99&diff=${diff}"
                )
                val body = scoreResp.bodyAsText()

                val data = Regex("<html.*>([\\s\\S]*)</html>")
                    .find(body)?.groupValues?.get(1)?.replace("\\s+/g", " ")

                processBody(difficulty, data ?: "")
            }
        } catch (e: Exception) {
            Log.e("ProberUtil", "抓取${difficulty.diffName}成绩失败: ${e.message}")
        }
    }
}

suspend fun getChuniScoreData(authUrl: String) : List<ChuniData.MusicDetail> {
    val scores = mutableListOf<ChuniData.MusicDetail>()
    fetchChuniScores(authUrl) { diff, body ->
        scores.addAll(ParseScorePageUtil.parseChuni(body, diff))
    }
    if (application.configManager.config.localConfig.cacheScore) {
        writeChuniScoreCache(scores)
    }
    return scores
}

suspend fun fetchChuniScores(
    authUrl: String,
    processBody: suspend (ChuniEnums.Difficulty, String) -> Unit
) {
    val result = client.get(authUrl) {
        getDefaultWahlapRequestBuilder()
    }

    if (result.bodyAsText().contains("错误")) {
        Log.e("ProberUtil", "登录公众号失败")
        sendMessageToUi("获取中二节奏成绩失败: 登录公众号失败")
        return
    }

    val token = result.setCookie()["_t"]?.value

    for (diff in application.configManager.config.syncConfig.chuniSyncDifficulty) {
        val difficulty = ChuniEnums.Difficulty.getDifficultyWithIndex(diff)
        val url = chuniUrls[diff]

        Log.i("ProberUtil", "开始抓取${difficulty.diffName}成绩")

        try {
            with(client) {
                if (url[0] != null) {
                    post("https://chunithm.wahlap.com/mobile/${url[0]}") {
                        headers {
                            append(HttpHeaders.ContentType, "application/x-www-form-urlencoded")
                        }
                        contentType(ContentType.Application.FormUrlEncoded)
                        setBody("genre=99&token=$token")
                    }
                }

                val resp: HttpResponse = get("https://chunithm.wahlap.com/mobile/${url[1]}")
                val body = resp.bodyAsText()

                processBody(difficulty, body)
            }
        } catch (e: Exception) {
            Log.e("ProberUtil", "抓取${difficulty.diffName}成绩失败: ${e.message}")
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun writeMaimaiScoreCache(data: List<MaimaiData.MusicDetail>) {
    val outputStream = application.getFilesDirOutputStream("maimai_score_cache.json")
    Json.encodeToStream(data, outputStream)
}

@OptIn(ExperimentalSerializationApi::class)
fun writeChuniScoreCache(data: List<ChuniData.MusicDetail>) {
    val outputStream = application.getFilesDirOutputStream("chuni_score_cache.json")
    Json.encodeToStream(data, outputStream)
}

@OptIn(ExperimentalSerializationApi::class)
fun getMaimaiScoreCache(): List<MaimaiData.MusicDetail> {
    if (application.checkFilesDirPathExist("maimai_score_cache.json")) {
        val inputStream = application.getFilesDirInputStream("maimai_score_cache.json")
        return Json.decodeFromStream<List<MaimaiData.MusicDetail>>(inputStream)
    } else {
        return emptyList()
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun getChuniScoreCache(): List<ChuniData.MusicDetail> {
    if (application.checkFilesDirPathExist("chuni_score_cache.json")) {
        val inputStream = application.getFilesDirInputStream("chuni_score_cache.json")
        return Json.decodeFromStream<List<ChuniData.MusicDetail>>(inputStream)
    } else {
        return emptyList()
    }
}

private fun HttpRequestBuilder.getDefaultWahlapRequestBuilder() {
    headers {
        append(HttpHeaders.Connection, "keep-alive")
        append("Upgrade-Insecure-Requests", "1")
        append(HttpHeaders.UserAgent, WX_WINDOWS_UA)
        append(
            HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9," +
                    "image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
        )
        append("Sec-Fetch-Site", "none")
        append("Sec-Fetch-Mode", "navigate")
        append("Sec-Fetch-User", "?1")
        append("Sec-Fetch-Dest", "document")
        append(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
        append(HttpHeaders.AcceptLanguage, "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7")
    }
}