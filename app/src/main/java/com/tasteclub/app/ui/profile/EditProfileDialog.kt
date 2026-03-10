package com.tasteclub.app.ui.profile
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.tasteclub.app.R
import com.tasteclub.app.databinding.DialogEditProfileBinding
class EditProfileDialog : DialogFragment() {
    private var _binding: DialogEditProfileBinding? = null
    private val binding get() = _binding!!
    private var listener: OnProfileUpdatedListener? = null
    companion object {
        private const val ARG_NAME = "name"
        private const val ARG_BIO = "bio"
        fun newInstance(name: String, bio: String): EditProfileDialog {
            val fragment = EditProfileDialog()
            val args = Bundle()
            args.putString(ARG_NAME, name)
            args.putString(ARG_BIO, bio)
            fragment.arguments = args
            return fragment
        }
    }
    interface OnProfileUpdatedListener {
        fun onProfileUpdated(name: String, bio: String)
    }
    fun setOnProfileUpdatedListener(listener: OnProfileUpdatedListener) {
        this.listener = listener
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            binding.nameEditText.setText(it.getString(ARG_NAME))
            binding.bioEditText.setText(it.getString(ARG_BIO))
        }
        setupClickListeners()
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }
    private fun setupClickListeners() {
        binding.apply {
            cancelButton.setOnClickListener {
                dismiss()
            }
            saveButton.setOnClickListener {
                if (validateInputs()) {
                    val name = nameEditText.text.toString().trim()
                    val bio = bioEditText.text.toString().trim()
                    listener?.onProfileUpdated(name, bio)
                    dismiss()
                }
            }
        }
    }
    private fun validateInputs(): Boolean {
        val name = binding.nameEditText.text.toString().trim()
        val bio = binding.bioEditText.text.toString().trim()
        if (name.isEmpty()) {
            binding.nameLayout.error = "Name is required"
            return false
        }
        if (name.length < 2) {
            binding.nameLayout.error = "Name must be at least 2 characters"
            return false
        }
        binding.nameLayout.error = null
        if (bio.length > 150) {
            binding.bioLayout.error = "Bio must be 150 characters or less"
            return false
        }
        binding.bioLayout.error = null
        return true
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
