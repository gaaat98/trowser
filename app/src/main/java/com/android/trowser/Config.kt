package com.android.trowser

import android.annotation.SuppressLint
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.android.trowser.utils.Utils

class Config(val prefs: SharedPreferences) {
    companion object {
        const val SEARCH_ENGINE_URL_PREF_KEY = "search_engine_url"
        const val SEARCH_ENGINE_AS_HOME_PAGE_KEY = "search_engine_as_home_page"
        const val HOME_PAGE_KEY = "home_page"
        const val USER_AGENT_PREF_KEY = "user_agent"
        const val THEME_KEY = "theme"
        const val LAST_UPDATE_USER_NOTIFICATION_TIME_KEY = "last_update_notif"
        const val AUTO_CHECK_UPDATES_KEY = "auto_check_updates"
        const val UPDATE_CHANNEL_KEY = "update_channel"
        const val Trowser_UA_PREFIX = "Trowser/1.0 "
        const val DEFAULT_HOME_URL = "about:blank"
        const val KEEP_SCREEN_ON_KEY = "keep_screen_on"
        const val INCOGNITO_MODE_KEY = "incognito_mode"
        const val INCOGNITO_MODE_HINT_SUPPRESS_KEY = "incognito_mode_hint_suppress"
    }

    enum class Theme {
        SYSTEM, WHITE, BLACK
    }

    fun getSearchEngineURL(): String {
        return prefs.getString(SEARCH_ENGINE_URL_PREF_KEY, "")!!
    }

    fun getSearchEngineAsHomePage() = prefs.getBoolean(SEARCH_ENGINE_AS_HOME_PAGE_KEY, false)

    fun getHomePage() = prefs.getString(HOME_PAGE_KEY, DEFAULT_HOME_URL)!!

    fun getUserAgentString(): String {
        return prefs.getString(USER_AGENT_PREF_KEY, "")!!
    }

    fun isNeedAutoCheckUpdates(): Boolean {
        return prefs.getBoolean(AUTO_CHECK_UPDATES_KEY, Utils.isInstalledByAPK(Trowser.instance))
    }

    fun getUpdateChannel(): String {
        return prefs.getString(UPDATE_CHANNEL_KEY, "release")!!
    }

    var incognitoMode: Boolean
        get() = prefs.getBoolean(INCOGNITO_MODE_KEY, false)
        @SuppressLint("ApplySharedPref")
        set(value) {
            prefs.edit().putBoolean(INCOGNITO_MODE_KEY, value).commit()
        }

    var incognitoModeHintSuppress: Boolean
        get() = prefs.getBoolean(INCOGNITO_MODE_HINT_SUPPRESS_KEY, false)
        set(value) {
            prefs.edit().putBoolean(INCOGNITO_MODE_HINT_SUPPRESS_KEY, value).apply()
        }

    var keepScreenOn: Boolean
        get() = prefs.getBoolean(KEEP_SCREEN_ON_KEY, false)
        set(value) {
            prefs.edit().putBoolean(KEEP_SCREEN_ON_KEY, value).apply()
        }

    var theme: Theme
        get() = Theme.values()[prefs.getInt(THEME_KEY, 0)]
        set(value) {
            prefs.edit().putInt(THEME_KEY, value.ordinal).apply()

            when (theme) {
                Theme.BLACK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                Theme.WHITE -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }

    fun setSearchEngineURL(url: String) {
        prefs.edit().putString(SEARCH_ENGINE_URL_PREF_KEY, url).apply()
    }

    fun setSearchEngineAsHomePage(searchEngineIsHomePage: Boolean) {
        prefs.edit().putBoolean(SEARCH_ENGINE_AS_HOME_PAGE_KEY, searchEngineIsHomePage).apply()
    }

    fun setHomePage(url: String) {
        prefs.edit().putString(HOME_PAGE_KEY, url).apply()
    }

    fun setUserAgentString(uas: String) {
        prefs.edit().putString(USER_AGENT_PREF_KEY, uas).apply()
    }

    fun setAutoCheckUpdates(need: Boolean) {
        prefs.edit().putBoolean(AUTO_CHECK_UPDATES_KEY, need).apply()
    }

    fun setUpdateChannel(channel: String) {
        prefs.edit().putString(UPDATE_CHANNEL_KEY, channel).apply()
    }
}
