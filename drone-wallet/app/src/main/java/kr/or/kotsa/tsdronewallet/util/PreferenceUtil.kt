package kr.or.kotsa.tsdronewallet.util

import android.content.Context
import androidx.core.content.edit

class PreferenceUtil(context: Context) {
    private val preference = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)

    fun putInt(type: PreferenceKey, value: Int) {
        preference.edit()
            .putInt(type.keyName, value)
            .apply()
    }

    fun getInt(type: PreferenceKey, default: Int? = -1): Int {
        return preference.getInt(type.keyName, default!!)
    }


    fun putString(type: PreferenceKey, value: String) {
        preference.edit()
            .putString(type.keyName, value)
            .apply()
    }

    fun putString(key: String, value: String) {
        preference.edit()
            .putString(key, value)
            .apply()
    }

    fun getString(type: PreferenceKey, default: String? = null): String? {
        return preference.getString(type.keyName, default)
    }

    fun getString(key: String): String? = preference.getString(key, null)

    fun getBoolean(type: PreferenceKey, value: Boolean): Boolean {
        return preference.getBoolean(type.keyName, value)
    }

    fun getBoolean(type: PreferenceKey): Boolean {
        return getBoolean(type, false)
    }

    fun putBoolean(type: PreferenceKey, value: Boolean) {
        preference.edit().putBoolean(type.keyName, value).apply()
    }

    fun getStringSet(key: PreferenceKey): Set<String>? {
        return preference.getStringSet(key.keyName, null)
    }

    fun putStringSet(key: PreferenceKey, value: Set<String>) {
        preference.edit().putStringSet(key.keyName, value).apply()
    }

    fun removeValue(key: PreferenceKey) {
        preference.edit().remove(key.keyName).apply()
    }

    fun removeValue(key: String) {
        preference.edit().remove(key).apply()
    }

    fun removeAll() {
        preference.edit {
            clear()
        }
    }

}

enum class PreferenceKey(val keyName: String) {
    MASTER_KEY("master_key")
}