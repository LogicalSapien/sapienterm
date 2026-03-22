/*
 * SapienSSH: modern SSH client for Android
 * Copyright 2025 SapienSSH contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.logicalsapien.sapienssh.data.export

import org.json.JSONArray
import org.json.JSONObject

/**
 * Top-level export container for SapienSSH backup data.
 *
 * @property version Schema version for forward compatibility
 * @property exportDate ISO 8601 formatted export timestamp
 * @property app Application identifier
 * @property connections Exported connection configurations
 * @property quickCommands Exported quick commands
 * @property credentials Exported credentials (with plaintext secrets)
 */
data class ExportData(
    val version: Int = 1,
    val exportDate: String,
    val app: String = "SapienSSH",
    val connections: List<ExportConnection>? = null,
    val quickCommands: List<ExportQuickCommand>? = null,
    val credentials: List<ExportCredential>? = null
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("version", version)
        json.put("exportDate", exportDate)
        json.put("app", app)

        connections?.let { list ->
            val arr = JSONArray()
            list.forEach { arr.put(it.toJson()) }
            json.put("connections", arr)
        }

        quickCommands?.let { list ->
            val arr = JSONArray()
            list.forEach { arr.put(it.toJson()) }
            json.put("quickCommands", arr)
        }

        credentials?.let { list ->
            val arr = JSONArray()
            list.forEach { arr.put(it.toJson()) }
            json.put("credentials", arr)
        }

        return json
    }

    companion object {
        fun fromJson(json: JSONObject): ExportData {
            val connections = json.optJSONArray("connections")?.let { arr ->
                (0 until arr.length()).map { ExportConnection.fromJson(arr.getJSONObject(it)) }
            }
            val quickCommands = json.optJSONArray("quickCommands")?.let { arr ->
                (0 until arr.length()).map { ExportQuickCommand.fromJson(arr.getJSONObject(it)) }
            }
            val credentials = json.optJSONArray("credentials")?.let { arr ->
                (0 until arr.length()).map { ExportCredential.fromJson(arr.getJSONObject(it)) }
            }
            return ExportData(
                version = json.optInt("version", 1),
                exportDate = json.optString("exportDate", ""),
                app = json.optString("app", "SapienSSH"),
                connections = connections,
                quickCommands = quickCommands,
                credentials = credentials
            )
        }
    }
}

/**
 * Exported connection configuration.
 * Does not include credentialId (foreign key) or lastConnect/hostKeyAlgo (device-specific).
 */
data class ExportConnection(
    val name: String,
    val hostname: String,
    val port: Int,
    val username: String,
    val protocol: String,
    val color: String?,
    val useKeys: Boolean,
    val useAuthAgent: String?,
    val postLogin: String?,
    val wantSession: Boolean,
    val compression: Boolean,
    val stayConnected: Boolean,
    val quickDisconnect: Boolean,
    val scrollbackLines: Int,
    val useCtrlAltAsMetaKey: Boolean,
    val ipVersion: String
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("name", name)
        json.put("hostname", hostname)
        json.put("port", port)
        json.put("username", username)
        json.put("protocol", protocol)
        json.put("color", color ?: JSONObject.NULL)
        json.put("useKeys", useKeys)
        json.put("useAuthAgent", useAuthAgent ?: JSONObject.NULL)
        json.put("postLogin", postLogin ?: JSONObject.NULL)
        json.put("wantSession", wantSession)
        json.put("compression", compression)
        json.put("stayConnected", stayConnected)
        json.put("quickDisconnect", quickDisconnect)
        json.put("scrollbackLines", scrollbackLines)
        json.put("useCtrlAltAsMetaKey", useCtrlAltAsMetaKey)
        json.put("ipVersion", ipVersion)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): ExportConnection = ExportConnection(
            name = json.optString("name", ""),
            hostname = json.optString("hostname", ""),
            port = json.optInt("port", 22),
            username = json.optString("username", ""),
            protocol = json.optString("protocol", "ssh"),
            color = json.optStringOrNull("color"),
            useKeys = json.optBoolean("useKeys", true),
            useAuthAgent = json.optStringOrNull("useAuthAgent"),
            postLogin = json.optStringOrNull("postLogin"),
            wantSession = json.optBoolean("wantSession", true),
            compression = json.optBoolean("compression", false),
            stayConnected = json.optBoolean("stayConnected", false),
            quickDisconnect = json.optBoolean("quickDisconnect", false),
            scrollbackLines = json.optInt("scrollbackLines", 140),
            useCtrlAltAsMetaKey = json.optBoolean("useCtrlAltAsMetaKey", false),
            ipVersion = json.optString("ipVersion", "IPV4_AND_IPV6")
        )
    }
}

/**
 * Exported quick command.
 */
data class ExportQuickCommand(
    val title: String,
    val command: String,
    val category: String?,
    val sortOrder: Int
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("title", title)
        json.put("command", command)
        json.put("category", category ?: JSONObject.NULL)
        json.put("sortOrder", sortOrder)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): ExportQuickCommand = ExportQuickCommand(
            title = json.optString("title", ""),
            command = json.optString("command", ""),
            category = json.optStringOrNull("category"),
            sortOrder = json.optInt("sortOrder", 0)
        )
    }
}

/**
 * Exported credential with plaintext secrets.
 * Passwords/keys are decrypted from Keystore for export and re-encrypted
 * with the user's export passphrase via CryptoUtils.
 */
data class ExportCredential(
    val label: String,
    val type: String,
    val password: String?,
    val privateKey: String?,
    val publicKey: String?,
    val passphrase: String?
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("label", label)
        json.put("type", type)
        json.put("password", password ?: JSONObject.NULL)
        json.put("privateKey", privateKey ?: JSONObject.NULL)
        json.put("publicKey", publicKey ?: JSONObject.NULL)
        json.put("passphrase", passphrase ?: JSONObject.NULL)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): ExportCredential = ExportCredential(
            label = json.optString("label", ""),
            type = json.optString("type", "PASSWORD"),
            password = json.optStringOrNull("password"),
            privateKey = json.optStringOrNull("privateKey"),
            publicKey = json.optStringOrNull("publicKey"),
            passphrase = json.optStringOrNull("passphrase")
        )
    }
}

/**
 * Extension to handle JSON null strings properly.
 * Returns null if the value is missing or JSONObject.NULL.
 */
private fun JSONObject.optStringOrNull(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key)
}
