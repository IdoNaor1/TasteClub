package com.tasteclub.app.ui.comment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tasteclub.app.databinding.BottomSheetCommentsBinding
import com.tasteclub.app.ui.feed.CommentAdapter
import com.tasteclub.app.util.ServiceLocator
import kotlinx.coroutines.launch

class CommentsBottomSheetFragment : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_REVIEW_ID = "reviewId"

        fun newInstance(reviewId: String): CommentsBottomSheetFragment {
            return CommentsBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_REVIEW_ID, reviewId)
                }
            }
        }
    }

    private var _binding: BottomSheetCommentsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CommentsViewModel
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var reviewId: String

    /** Called whenever the comment count changes (post or delete). */
    var onCommentCountChanged: ((reviewId: String, newCount: Int) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        reviewId = arguments?.getString(ARG_REVIEW_ID)
            ?: throw IllegalArgumentException("reviewId is required")

        // Bug 1 fix: load user from Room before constructing the ViewModel
        // so userName/userImageUrl are populated when comments are posted.
        lifecycleScope.launch {
            val authRepo = ServiceLocator.provideAuthRepository(requireContext())
            val userId = authRepo.getCurrentUserOnce()?.uid ?: authRepo.currentUserId() ?: ""

            setupViewModel(userId)
            setupRecyclerView()
            setupInput()
            viewModel.loadComments(reviewId)
        }
    }

    private fun setupViewModel(userId: String) {
        val commentRepo = ServiceLocator.provideCommentRepository(requireContext())

        val factory = CommentsViewModelFactory(
            commentRepository = commentRepo,
            currentUserId     = userId
        )
        viewModel = ViewModelProvider(this, factory)[CommentsViewModel::class.java]
    }

    private fun setupRecyclerView() {
        commentAdapter = CommentAdapter(
            currentUserId = viewModel.currentUserId,
            onDeleteClick = { comment -> viewModel.deleteComment(reviewId, comment) }
        )

        binding.rvComments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = commentAdapter
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is CommentsViewModel.State.Loading -> {
                    binding.pbComments.visibility = View.VISIBLE
                    binding.tvNoComments.visibility = View.GONE
                    binding.rvComments.visibility = View.GONE
                }
                is CommentsViewModel.State.Success -> {
                    binding.pbComments.visibility = View.GONE
                    if (state.comments.isEmpty()) {
                        binding.tvNoComments.visibility = View.VISIBLE
                        binding.rvComments.visibility = View.GONE
                    } else {
                        binding.tvNoComments.visibility = View.GONE
                        binding.rvComments.visibility = View.VISIBLE
                        commentAdapter.submitList(state.comments)
                        binding.rvComments.scrollToPosition(state.comments.size - 1)
                    }
                    // Bug 2 fix: notify caller whenever count changes
                    onCommentCountChanged?.invoke(reviewId, state.comments.size)
                }
                is CommentsViewModel.State.Error -> {
                    binding.pbComments.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
                else -> Unit
            }
        }
    }

    private fun setupInput() {
        binding.btnSendComment.setOnClickListener { submitComment() }

        binding.etCommentInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitComment()
                true
            } else {
                false
            }
        }
    }

    private fun submitComment() {
        val text = binding.etCommentInput.text?.toString().orEmpty()
        if (text.isBlank()) return
        viewModel.postComment(reviewId, text)
        binding.etCommentInput.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
