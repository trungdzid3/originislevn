package com.originisle.android.share

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.originisle.android.cards.ScoreCard
import com.originisle.android.sports.FlashscoreShare

/**
 * Receives `text/plain` shares from Flashscore ("Show on island") and posts the match
 * onto Origin Island.
 */
class ShareToIslandActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty().trim()
            val shared = if (text.isNotEmpty()) FlashscoreShare.parse(text) else null
            if (shared != null) {
                ScoreCard.fromShare(this, shared)
                Toast.makeText(
                    this,
                    "${shared.home} - ${shared.away} is on the island. Follow it in Flashscore to get its updates.",
                    Toast.LENGTH_LONG,
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Chỉ hỗ trợ chia sẻ trận đấu từ Flashscore",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
        finish()
    }
}
