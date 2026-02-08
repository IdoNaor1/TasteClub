package com.tasteclub.app.ui.profile
import android.app.Dialog
import android.os.Bundle
import android.util.Patterns
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
        private const val ARG_EMAIL = "email"
        fun newInstance(name: String, email: String): EditProfileDialog {
            val fragment = EditProfileDialog()
            val args = Bundle()
            args.putString(ARG_NAME, name)
            args.putString(ARG_EMAIL, email)
            fragment.arguments = args
            return fragment
        }
    }
    interface OnProfileUpdatedListener {
        fun onProfileUpdated(name: String, email: String)
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
            binding.emailEditText.setText(it.getString(ARG_EMAIL))
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
                    val email = emailEditText.text.toString().trim()
                    listener?.onProfileUpdated(name, email)
                    dismiss()
                }
            }
        }
    }
    private fun validateInputs(): Boolean {
        val name = binding.nameEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        if (name.isEmpty()) {
            binding.nameLayout.error = "Name is required"
            return false
        }
        if (name.length < 2) {
            binding.nameLayout.error = "Name must be at least 2 characters"
            return false
        }
        binding.nameLayout.error = null
        if (email.isEmpty()) {
            binding.emailLayout.error = getString(R.string.error_email_required)
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = getString(R.string.error_email_invalid)
            return false
        }
        binding.emailLayout.error = null
        return true
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
