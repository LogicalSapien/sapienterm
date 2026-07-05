/*
 * SapienTerm: modern SSH client for Android
 * Copyright 2025 SapienTerm contributors
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

package com.logicalsapien.sapienterm.data.export

import org.json.JSONArray
import org.json.JSONObject

/**
 * Top-level export container for SapienTerm backup data.
 *
 * @property version Schema version for forward compatibility
 * @property exportDate ISO 8601 formatted export timestamp
 * @property app Application identifier
 * @property connections Exported connection configurations
 * @property quickCommands Exported quick commands
 * @property credentials Exported credentials (with plaintext secrets)
 */
data class ExportData(
    val version: Int = 3,
    val exportDate: String,
    val app: String = "SapienTerm",
    val connections: List<ExportConnection>? = null,
    val quickCommands: List<ExportQuickCommand>? = null,
    val credentials: List<ExportCredential>? = null,
    val groups: List<ExportConnectionGroup>? = null,
    val profiles: List<ExportProfile>? = null,
    val preferences: ExportPreferences? = null
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

        groups?.let { list ->
            val arr = JSONArray()
            list.forEach { arr.put(it.toJson()) }
            json.put("groups", arr)
        }

        profiles?.let { list ->
            val arr = JSONArray()
            list.forEach { arr.put(it.toJson()) }
            json.put("profiles", arr)
        }

        preferences?.let { json.put("preferences", it.toJson()) }

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
            val groups = json.optJSONArray("groups")?.let { arr ->
                (0 until arr.length()).map { ExportConnectionGroup.fromJson(arr.getJSONObject(it)) }
            }
            val profiles = json.optJSONArray("profiles")?.let { arr ->
                (0 until arr.length()).map { ExportProfile.fromJson(arr.getJSONObject(it)) }
            }
            val preferences = json.optJSONObject("preferences")?.let { ExportPreferences.fromJson(it) }
            return ExportData(
                version = json.optInt("version", 1),
                exportDate = json.optString("exportDate", ""),
                app = json.optString("app", "SapienTerm"),
                connections = connections,
                quickCommands = quickCommands,
                credentials = credentials,
                groups = groups,
                profiles = profiles,
                preferences = preferences
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
    val ipVersion: String,
    val groupName: String? = null,
    val credentialLabel: String? = null,
    val pinned: Boolean = false,
    val profileName: String? = null,
    val portForwards: List<ExportPortForward> = emptyList()
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
        json.put("groupName", groupName ?: JSONObject.NULL)
        json.put("credentialLabel", credentialLabel ?: JSONObject.NULL)
        json.put("pinned", pinned)
        json.put("profileName", profileName ?: JSONObject.NULL)
        if (portForwards.isNotEmpty()) {
            val arr = JSONArray()
            portForwards.forEach { arr.put(it.toJson()) }
            json.put("portForwards", arr)
        }
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
            ipVersion = json.optString("ipVersion", "IPV4_AND_IPV6"),
            groupName = json.optStringOrNull("groupName"),
            credentialLabel = json.optStringOrNull("credentialLabel"),
            pinned = json.optBoolean("pinned", false),
            profileName = json.optStringOrNull("profileName"),
            portForwards = json.optJSONArray("portForwards")?.let { arr ->
                (0 until arr.length()).map { ExportPortForward.fromJson(arr.getJSONObject(it)) }
            } ?: emptyList()
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
 * Exported connection group (project folder).
 */
data class ExportConnectionGroup(
    val name: String,
    val color: String?,
    val sortOrder: Int
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("name", name)
        json.put("color", color ?: JSONObject.NULL)
        json.put("sortOrder", sortOrder)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): ExportConnectionGroup = ExportConnectionGroup(
            name = json.optString("name", ""),
            color = json.optStringOrNull("color"),
            sortOrder = json.optInt("sortOrder", 0)
        )
    }
}

/**
 * Exported port forwarding rule.
 */
data class ExportPortForward(
    val nickname: String,
    val type: String,
    val sourcePort: Int,
    val destAddr: String?,
    val destPort: Int
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("nickname", nickname)
        json.put("type", type)
        json.put("sourcePort", sourcePort)
        json.put("destAddr", destAddr ?: JSONObject.NULL)
        json.put("destPort", destPort)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): ExportPortForward = ExportPortForward(
            nickname = json.optString("nickname", ""),
            type = json.optString("type", "local"),
            sourcePort = json.optInt("sourcePort", 0),
            destAddr = json.optStringOrNull("destAddr"),
            destPort = json.optInt("destPort", 0)
        )
    }
}

/**
 * Exported terminal profile.
 */
data class ExportProfile(
    val name: String,
    val iconColor: String?,
    val fontFamily: String?,
    val fontSize: Int,
    val delKey: String,
    val encoding: String,
    val emulation: String,
    val forceSizeRows: Int?,
    val forceSizeColumns: Int?
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("name", name)
        json.put("iconColor", iconColor ?: JSONObject.NULL)
        json.put("fontFamily", fontFamily ?: JSONObject.NULL)
        json.put("fontSize", fontSize)
        json.put("delKey", delKey)
        json.put("encoding", encoding)
        json.put("emulation", emulation)
        json.put("forceSizeRows", forceSizeRows ?: JSONObject.NULL)
        json.put("forceSizeColumns", forceSizeColumns ?: JSONObject.NULL)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): ExportProfile = ExportProfile(
            name = json.optString("name", ""),
            iconColor = json.optStringOrNull("iconColor"),
            fontFamily = json.optStringOrNull("fontFamily"),
            fontSize = json.optInt("fontSize", 10),
            delKey = json.optString("delKey", "del"),
            encoding = json.optString("encoding", "UTF-8"),
            emulation = json.optString("emulation", "xterm-256color"),
            forceSizeRows = if (json.isNull("forceSizeRows")) null else json.optInt("forceSizeRows"),
            forceSizeColumns = if (json.isNull("forceSizeColumns")) null else json.optInt("forceSizeColumns")
        )
    }
}

/**
 * Exported app preferences (keyboard layout, bottom bar, behaviour settings).
 * Only keys the user has explicitly changed are stored — absent keys restore defaults on import.
 */
data class ExportPreferences(
    val values: Map<String, String>
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        values.forEach { (k, v) -> json.put(k, v) }
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): ExportPreferences {
            val map = mutableMapOf<String, String>()
            json.keys().forEach { key -> map[key] = json.getString(key) }
            return ExportPreferences(map)
        }

        /** Preference keys that are worth backing up (excludes device-specific and privacy-sensitive). */
        val EXPORTABLE_KEYS = setOf(
            "rotation", "keymode", "scrollback", "bell", "bellVolume", "bellVibrate",
            "bellNotification", "bumpyarrows", "sortByColor", "fullscreen", "titlebarhide",
            "pgupdngesture", "camera", "keepalive", "wifilock", "shiftfkeys", "ctrlfkeys",
            "volumefont", "stickymodifiers", "connPersist", "fontFamily",
            "extended_keyboard_keys", "terminal_bottom_bar_preset", "terminal_session_keyboard",
            "custom_bottom_bar_layouts", "terminal_bracketed_paste_send"
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
