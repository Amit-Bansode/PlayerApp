package com.hurix.playerapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.hurix.playerapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.styledPlayerView.preparePlayer()
        binding.button.setOnClickListener {
            binding.styledPlayerView.stopPlayer()
            binding.styledPlayerView.play()
        }
    }

    override fun onStop() {
        super.onStop()
        binding.styledPlayerView.stopPlayer()
    }
}