package com.storyteller_f.ping

import com.storyteller_f.annotation_defination.BindItemHolder
import com.storyteller_f.annotation_defination.ItemHolder
import com.storyteller_f.ping.databinding.ViewHolderTestBinding
import com.storyteller_f.ui_list.core.BindingViewHolder
import com.storyteller_f.ui_list.core.DataItemHolder

@ItemHolder(type = "user card")
interface UserCard : DataItemHolder

class TestItemHolder : UserCard {
    override fun areItemsTheSame(other: DataItemHolder): Boolean {
        return false
    }
}

@BindItemHolder(TestItemHolder::class)
class TestViewHolder(private val binding: ViewHolderTestBinding) : BindingViewHolder<TestItemHolder>(binding) {
    override fun bindData(itemHolder: TestItemHolder) {
        binding.info.text = "hello"
    }

}