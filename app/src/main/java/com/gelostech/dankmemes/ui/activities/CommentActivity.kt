package com.gelostech.dankmemes.ui.activities

import android.content.ContentResolver
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.gelostech.dankmemes.R
import com.gelostech.dankmemes.data.Status
import com.gelostech.dankmemes.ui.adapters.CommentAdapter
import com.gelostech.dankmemes.ui.base.BaseActivity
import com.gelostech.dankmemes.utils.Constants
import com.gelostech.dankmemes.data.models.Comment
import com.gelostech.dankmemes.data.responses.GenericResponse
import com.gelostech.dankmemes.ui.callbacks.CommentsCallback
import com.gelostech.dankmemes.ui.viewmodels.CommentsViewModel
import com.gelostech.dankmemes.utils.hideView
import com.gelostech.dankmemes.utils.showView
import com.mikepenz.fontawesome_typeface_library.FontAwesome
import com.mikepenz.iconics.IconicsDrawable
import kotlinx.android.synthetic.main.activity_comment.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.toast
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class CommentActivity : BaseActivity() {
    private lateinit var commentsAdapter: CommentAdapter
    private lateinit var memeId: String
    private val commentsViewModel: CommentsViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)

        memeId = intent.getStringExtra(Constants.MEME_ID)!!

        initViews()
        initResponseObserver()
        initCommentsObserver()

        commentsViewModel.fetchComments(memeId)
    }

    private fun initViews() {
        setSupportActionBar(commentToolbar)
        supportActionBar?.apply {
            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            title = null
        }

        sendComment.setImageDrawable(IconicsDrawable(this)
                .icon(FontAwesome.Icon.faw_paper_plane)
                .color(ContextCompat.getColor(this, R.color.color_secondary))
                .sizeDp(22))

        commentsAdapter = CommentAdapter(commentsCallback)

        commentsRv.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@CommentActivity)
            itemAnimator = DefaultItemAnimator()
            adapter = commentsAdapter
        }

        sendComment.setOnClickListener { postComment() }
    }

    /**
     * Initialize observer for GenericResponse LiveData
     */
    private fun initResponseObserver() {
        commentsViewModel.genericResponseLiveData.observe(this, Observer {
            when (it.status) {
                Status.LOADING -> Timber.e("Loading...")

                Status.SUCCESS -> {
                    hideLoading()

                    when (it.item) {
                        GenericResponse.ITEM_RESPONSE.DELETE_COMMENT -> {
                            toast("Comment deleted \uD83D\uDEAE")
                            commentsAdapter.removeComment(it.value!!)
                        }

                        GenericResponse.ITEM_RESPONSE.POST_COMMENT -> {
                            commentET.setText("")
                            playNotificationSound()
                            commentsViewModel.fetchComments(memeId)
                        }

                        else -> Timber.e("Success")
                    }
                }

                Status.ERROR -> {
                    hideLoading()
                    toast("${it.error}. Please try again")
                }
            }
        })
    }

    /**
     * Initialize Comments LiveData observer
     */
    private fun initCommentsObserver() {
        commentsViewModel.commentsLiveData.observe(this, Observer {
            when (it.status) {
                Status.LOADING -> {
                    commentsEmptyState.hideView()
                    loading.showView()
                }

                Status.SUCCESS -> {
                    loading.hideView()
                    commentsAdapter.clear()
                    commentsAdapter.addComments(it.data!!)
                }

                Status.ERROR -> {
                    loading.hideView()
                    commentsEmptyState.showView()
                }
            }
        })
    }

    private fun postComment() {
        if (TextUtils.isEmpty(commentET.text)) {
            toast("Please type a comment..")
            return
        }

        showLoading("Posting comment...")

        val comment = Comment()
        comment.authorId = getUid()
        comment.userName = sessionManager.getUsername()
        comment.userAvatar = sessionManager.getUserAvatar()
        comment.comment = commentET.text.toString().trim()
        comment.hates = 0
        comment.likes = 0
        comment.timeStamp = System.currentTimeMillis()
        comment.picKey = memeId

        commentsViewModel.postComment(comment)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId) {
            android.R.id.home -> onBackPressed()
        }

        return true
    }

    private val commentsCallback = object : CommentsCallback {
        override fun onCommentClicked(view: View, comment: Comment, longClick: Boolean) {
            if (longClick) handleLongClick(comment)
            else {
                if (view.id == R.id.commentIcon) handleClick(comment)
            }
        }
    }

    private fun handleClick (comment: Comment) {
        if (comment.authorId != getUid()) {
            val i = Intent(this, ProfileActivity::class.java)
            i.putExtra(Constants.USER_ID, comment.authorId)
            startActivity(i)
            overridePendingTransition(R.anim.enter_b, R.anim.exit_a)
        }
    }

    private fun handleLongClick (comment: Comment) {
        if (comment.authorId == getUid()) {
            alert ("Delete this comment?") {
                title = "Delete Comment"

                positiveButton("Delete") {
                    showLoading("Deleting comment...")
                    commentsViewModel.deleteComment(memeId, comment.commentKey!!)
                }
                negativeButton("Cancel"){}
            }.show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.enter_a, R.anim.exit_b)
    }

    private fun playNotificationSound() {
        try {
            val alarmSound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + this.packageName + "/raw/new_comment")
            val r = RingtoneManager.getRingtone(this, alarmSound)
            r.play()

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

}
