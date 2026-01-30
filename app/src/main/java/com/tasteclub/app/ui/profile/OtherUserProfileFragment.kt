package com.tasteclub.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tasteclub.app.R

/**
 * OtherUserProfileFragment - View another user's profile (Phase 7)
 */
class OtherUserProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_other_user_profile, container, false)
    }
}

