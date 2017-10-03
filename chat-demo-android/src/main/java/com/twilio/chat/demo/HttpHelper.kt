package com.twilio.chat.demo

import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import org.json.JSONException

object HttpHelper {
    @Throws(Exception::class)
    fun httpGet(username: String, url: String): String {
        val urlObj = URL(url)

        val conn = urlObj.openConnection() as HttpURLConnection

        conn.connectTimeout = 45000
        conn.readTimeout = 30000
        conn.doInput = true

        val responseCode = conn.responseCode

        if (responseCode == HttpURLConnection.HTTP_OK) {
            // Try to get access token from "token" field in the JSON format response
            // If response cannot be parsed as JSON, use it as-is.

            var accessToken = conn.inputStream.reader().readText()
            conn.inputStream.close()
            conn.disconnect()

            try {
                JSONObject(accessToken).apply {
                    accessToken = this.getString("token")
                }
            } catch (xcp: JSONException) {
                // Do nothing
            }

            return accessToken
        } else {
            conn.disconnect()
            throw Exception("Got error code $responseCode from server")
        }
    }
}
