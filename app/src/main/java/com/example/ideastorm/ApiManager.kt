package com.letotoo06.ideastorm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class PlayerData(
    val id: Int,
    val username: String,
    val score: Double,
    val rebirthCount: Int,
    val perClick: Double = 1.0,
    val perSecond: Double = 0.0
)

object ApiManager {
    private const val BASE_URL = "https://www.tobias-ferrand.ovh/ideastorm/api.php"

    private suspend fun postRequest(action: String, jsonBody: JSONObject): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL?action=$action")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            OutputStreamWriter(conn.outputStream).use { it.write(jsonBody.toString()) }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
        } catch (e: Exception) { null }
    }

    private suspend fun getRequest(action: String, extraParams: String = ""): String? = withContext(Dispatchers.IO) {
        try {
            // L'équivalent de request.cachePolicy = .reloadIgnoringLocalCacheData
            val url = URL("$BASE_URL?action=$action$extraParams&t=${System.currentTimeMillis()}")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
        } catch (e: Exception) { null }
    }

    suspend fun login(username: String, passwordRaw: String): User? {
        val body = JSONObject().put("username", username).put("password", passwordRaw)
        val response = postRequest("login", body) ?: return null

        android.util.Log.d("API_TITAN", "Réponse Login OVH : $response")

        return try {
            val json = JSONObject(response)
            if (json.optBoolean("success")) {
                // 👑 LA CORRECTION : On lit directement à la racine du JSON !
                val idStr = json.optString("userId", json.optString("id", json.optString("user_id", "0")))
                val id = idStr.toIntOrNull() ?: json.optInt("userId", json.optInt("id", 0))

                User(id, json.optString("username", username))
            } else null
        } catch (e: Exception) {
            android.util.Log.e("API_TITAN", "Crash JSON Login : ${e.message}")
            null
        }
    }

    suspend fun signup(username: String, passwordRaw: String): User? {
        val body = JSONObject().put("username", username).put("password", passwordRaw)
        val response = postRequest("signup", body) ?: return null

        android.util.Log.d("API_TITAN", "Réponse Signup OVH : $response")

        return try {
            val json = JSONObject(response)
            if (json.optBoolean("success")) {
                // 👑 Gère le cas où les infos sont à la racine ou dans "user"
                val dataObj = if (json.has("user")) json.getJSONObject("user") else json

                val idStr = dataObj.optString("userId", dataObj.optString("id", dataObj.optString("user_id", "0")))
                val id = idStr.toIntOrNull() ?: dataObj.optInt("userId", dataObj.optInt("id", 0))

                User(id, dataObj.optString("username", username))
            } else null
        } catch (e: Exception) {
            android.util.Log.e("API_TITAN", "Crash JSON Signup : ${e.message}")
            null
        }
    }

    suspend fun loadGame(userId: Int): PlayerData? {
        val response = getRequest("load", "&userId=$userId") ?: return null
        return try {
            val json = JSONObject(response)
            PlayerData(
                id = userId,
                username = "",
                score = json.optDouble("score", 0.0),
                rebirthCount = json.optInt("rebirthCount", 0),
                perClick = json.optDouble("perClick", 1.0),
                perSecond = json.optDouble("perSecond", 0.0)
            )
        } catch (e: Exception) {
            try {
                // Triple radar array fallback
                val array = JSONArray(response)
                if (array.length() > 0) {
                    val obj = array.getJSONObject(0)
                    PlayerData(
                        id = userId,
                        username = "",
                        score = obj.optDouble("score", 0.0),
                        rebirthCount = obj.optInt("rebirthCount", 0),
                        perClick = obj.optDouble("perClick", 1.0),
                        perSecond = obj.optDouble("perSecond", 0.0)
                    )
                } else null
            } catch (ex: Exception) { null }
        }
    }

    suspend fun saveGame(userId: Int, score: Double, perClick: Double, perSecond: Double, rebirths: Int): Boolean {
        val body = JSONObject().apply {
            put("user_id", userId)
            put("score", score)
            val saveData = JSONObject().apply {
                put("score", score)
                put("perClick", perClick)
                put("perSecond", perSecond)
                put("rebirthCount", rebirths)
            }
            put("save_data", saveData)
        }
        val response = postRequest("save", body) ?: return false
        return response.contains("\"success\":true")
    }

    suspend fun fetchAdminList(): List<PlayerData> {
        val response = getRequest("admin_list") ?: return emptyList()
        val players = mutableListOf<PlayerData>()
        try {
            val array = JSONArray(response)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                // LE TRIPLE RADAR
                val id = obj.optInt("id", obj.optInt("user_id", obj.optInt("userId", 0)))
                val score = obj.optDouble("score", obj.optString("score", "0").toDoubleOrNull() ?: 0.0)
                players.add(PlayerData(id, obj.optString("username", "Inconnu"), score, obj.optInt("rebirth_count", 0)))
            }
        } catch (e: Exception) { }
        return players
    }
}