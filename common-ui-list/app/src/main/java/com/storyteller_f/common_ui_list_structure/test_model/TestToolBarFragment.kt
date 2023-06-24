package com.storyteller_f.common_ui_list_structure.test_model

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.storyteller_f.common_ui.RegularFragment
import com.storyteller_f.common_ui.toolbarCompose
import com.storyteller_f.common_ui_list_structure.databinding.FragmentTestToolbarBinding

class TestToolBarFragment : RegularFragment<FragmentTestToolbarBinding>(FragmentTestToolbarBinding::inflate) {

    override fun onBindViewEvent(binding: FragmentTestToolbarBinding) {

    }

    override fun onStart() {
        super.onStart()
        toolbarCompose.setContent {
            ToolBar()
        }
    }

    override fun up() = true

    @Preview
    @Composable
    fun ToolBar() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {},
            ) {
                Text(text = "Reset")
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {},
            ) {
                Text(text = "Submit")
            }
        }
    }
}