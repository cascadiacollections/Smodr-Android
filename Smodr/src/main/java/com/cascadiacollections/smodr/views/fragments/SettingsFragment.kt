package com.cascadiacollections.smodr.views.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.kevintcoughlin.smodr.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}