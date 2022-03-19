package com.storyteller_f.common_ui_list_structure.ui.dashboard

import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import com.storyteller_f.common_ui.RegularFragment
import com.storyteller_f.common_ui.toolbarCompose
import com.storyteller_f.common_ui_list_structure.databinding.FragmentDashboardBinding

class DashboardFragment : RegularFragment<FragmentDashboardBinding>(FragmentDashboardBinding::inflate) {

    private val dashboardViewModel by viewModels<DashboardViewModel>()

    override fun onBindViewEvent(binding: FragmentDashboardBinding) {
        val textView: TextView = binding.textDashboard
        dashboardViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        toolbarCompose().setContent {
            ToolBar()
        }
    }

    override fun up() = true

    @Preview
    @Composable
    fun ToolBar() {
        Row(
            modifier = Modifier.background(
                Brush.linearGradient(listOf(Color.Black, Color.Red)),
                RoundedCornerShape(3)
            )
        ) {
            Button(
                onClick = {

                },
                border = BorderStroke(1.dp, Color.Black)
            ) {
                Text(text = "full")
            }
            Button(
                onClick = {

                },
                border = BorderStroke(1.dp, Color.Red)
            ) {
                Text(text = "recovery")
            }
        }
    }
}