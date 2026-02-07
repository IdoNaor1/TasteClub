package com.tasteclub.app.ui.profile
import android.app.Dialog
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.tasteclub.app.R
import com.tasteclub.app.databinding.DialogEmailPreferencesBinding
class EmailPreferencesDialog : DialogFragment() {
    private var _binding: DialogEmailPreferencesBinding? = null
    private val binding get() = _binding!!
    private val prefs: SharedPreferences by lazy {
        requireContext().getSharedPreferences("email_prefs", android.content.Context.MODE_PRIVATE)
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogEmailPreferencesBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadPreferences()
        setupClickListeners()
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }
    private fun loadPreferences() {
        binding.apply {
            newFollowerSwitch.isChecked = prefs.getBoolean("new_follower", true)
            commentsSwitch.isChecked = prefs.getBoolean("comments", true)
            likesSwitch.isChecked = prefs.getBoolean("likes", false)
            weeklyDigestSwitch.isChecked = prefs.getBoolean("weekly_digest", true)
            recommendationsSwitch.isChecked = prefs.getBoolean("recommendations", true)
        }
    }
    private fun setupClickListeners() {
        binding.savePreferencesButton.setOnClickListener {
            savePreferences()
        }
    }
    private fun savePreferences() {
        prefs.edit().apply {
            putBoolean("new_follower", binding.newFollowerSwitch.isChecked)
            putBoolean("comments", binding.commentsSwitch.isChecked)
            putBoolean("likes", binding.likesSwitch.isChecked)
            putBoolean("weekly_digest", binding.weeklyDigestSwitch.isChecked)
            putBoolean("recommendations", binding.recommendationsSwitch.isChecked)
            apply()
        }
        Toast.makeText(context, R.string.preferences_saved, Toast.LENGTH_SHORT).show()
        dismiss()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
