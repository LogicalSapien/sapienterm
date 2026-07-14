/*
 * SapienTerm: simple, powerful, open-source SSH client for Android
 * Copyright 2026 LogicalSapien
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.logicalsapien.sapienterm.data

import com.logicalsapien.sapienterm.ui.screens.console.BottomBarShortcutAction
import org.json.JSONArray
import org.json.JSONObject

/**
 * User-defined bottom bar shortcut strip (stored as JSON in SharedPreferences).
 *
 * @param actionIds Stable ids matching [BottomBarShortcutAction.id], in display order (max [MAX_ACTIONS]).
 */
data class CustomBottomBarLayout(
    val id: String,
    val name: String,
    val actionIds: List<String>
) {
    init {
        require(actionIds.size <= MAX_ACTIONS) { "At most $MAX_ACTIONS shortcuts" }
    }

    val actions: List<BottomBarShortcutAction>
        get() = actionIds.mapNotNull { BottomBarShortcutAction.fromId(it) }

    companion object {
        const val MAX_ACTIONS: Int = 10

        fun fromJson(o: JSONObject): CustomBottomBarLayout {
            val id = o.getString("id")
            val name = o.optString("name", "Layout")
            val arr = o.optJSONArray("actions") ?: JSONArray()
            val ids = buildList {
                for (i in 0 until arr.length()) {
                    val s = arr.optString(i, null) ?: continue
                    if (s.isNotBlank()) add(s)
                }
            }.take(MAX_ACTIONS)
            return CustomBottomBarLayout(id = id, name = name, actionIds = ids)
        }
    }
}

fun List<CustomBottomBarLayout>.toJsonString(): String {
    val arr = JSONArray()
    for (layout in this) {
        val o = JSONObject()
        o.put("id", layout.id)
        o.put("name", layout.name)
        val a = JSONArray()
        for (id in layout.actionIds) a.put(id)
        o.put("actions", a)
        arr.put(o)
    }
    return arr.toString()
}

fun String.parseCustomBottomBarLayouts(): List<CustomBottomBarLayout> {
    if (isBlank()) return emptyList()
    return try {
        val arr = JSONArray(this)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(CustomBottomBarLayout.fromJson(o))
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}
