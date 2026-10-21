package com.aminsambung.videoeditor

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Hanya menampilkan pesan saat tombol diklik (Tanpa OpenGL & ExoPlayer)
        findViewById<Button>(R.id.btnVideo).setOnClickListener {
            Toast.makeText(this, "Tombol Video diklik (Mode Aman)", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnPhoto).setOnClickListener {
            Toast.makeText(this, "Tombol Foto diklik (Mode Aman)", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnAdjust).setOnClickListener {
            showAdjustDialog()
        }

        findViewById<Button>(R.id.btnFilters).setOnClickListener {
            showFiltersDialog()
        }
    }

    private fun showAdjustDialog() {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        dialog.setContentView(R.layout.dialog_adjust)
        dialog.show()
    }

    private fun showFiltersDialog() {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        dialog.setContentView(R.layout.dialog_filters)
        dialog.show()
    }
}
