package com.gelostech.dankmemes.data.models

data class Comment(
        var commentKey: String? = null,
        var authorId: String? = null,
        var timeStamp: Long? = null,
        var comment: String? = null,
        var hates: Int? = null,
        var likes: Int? = null,
        var userName: String? = null,
        var userAvatar: String? = null,
        var picKey: String? = null
) {
    fun equals(comment: Comment): Boolean = this.commentKey == comment.commentKey
}