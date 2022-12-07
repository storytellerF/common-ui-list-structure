package com.storyteller_f.giant_explorer.control

import android.os.Bundle
import com.bumptech.glide.Glide
import com.storyteller_f.common_ui.CommonActivity
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.databinding.ActivityAboutBinding
import com.storyteller_f.ui_list.event.viewBinding

class AboutActivity : CommonActivity() {
    private val binding by viewBinding(ActivityAboutBinding::inflate)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Glide.with(this).load(R.drawable.storytellerf_github_business_card).into(binding.image)
    }
}