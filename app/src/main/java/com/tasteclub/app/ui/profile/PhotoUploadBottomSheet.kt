package com.tasteclub.app.ui.profile
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tasteclub.app.databinding.BottomSheetPhotoUploadBinding
class PhotoUploadBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetPhotoUploadBinding? = null
    private val binding get() = _binding!!
    private var listener: OnPhotoSourceSelectedListener? = null
    interface OnPhotoSourceSelectedListener {
        fun onCameraSelected()
        fun onGallerySelected()
    }
    fun setOnPhotoSourceSelectedListener(listener: OnPhotoSourceSelectedListener) {
        this.listener = listener
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetPhotoUploadBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            takePhotoOption.setOnClickListener {
                listener?.onCameraSelected()
                dismiss()
            }
            chooseGalleryOption.setOnClickListener {
                listener?.onGallerySelected()
                dismiss()
            }
            cancelOption.setOnClickListener {
                dismiss()
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
