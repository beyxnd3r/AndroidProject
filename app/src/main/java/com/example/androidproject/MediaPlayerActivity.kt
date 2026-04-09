package com.example.androidproject

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.provider.DocumentsContract
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.random.Random

class MediaPlayerActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var btnPlay: Button
    private lateinit var btnPause: Button
    private lateinit var btnNext: Button
    private lateinit var btnPrev: Button
    private lateinit var btnChooseFolder: Button
    private lateinit var btnShuffle: Button
    private lateinit var btnSort: Button
    private lateinit var seekBar: SeekBar

    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var currentTrackIndex = -1

    private var trackUris = mutableListOf<Uri>()
    private var trackNames = mutableListOf<String>()

    companion object {
        private const val PERMISSION_REQUEST_CODE = 123
        private const val FOLDER_PICKER_CODE = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_player)
        val btnBack = findViewById<Button>(R.id.btnBackMain)
        btnBack.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        findViewById<Button>(R.id.btnBackMain).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        listView = findViewById(R.id.lvTracks)
        btnPlay = findViewById(R.id.btnPlay)
        btnPause = findViewById(R.id.btnPause)
        btnNext = findViewById(R.id.btnNext)
        btnPrev = findViewById(R.id.btnPrev)
        btnChooseFolder = findViewById(R.id.btnChooseFolder)
        btnShuffle = findViewById(R.id.btnShuffle)
        btnSort = findViewById(R.id.btnSort)
        seekBar = findViewById(R.id.seekBar)

        checkPermission()

        btnChooseFolder.setOnClickListener { chooseFolder() }
        btnPlay.setOnClickListener { playTrack() }
        btnPause.setOnClickListener { pauseTrack() }
        btnNext.setOnClickListener { nextTrack() }
        btnPrev.setOnClickListener { previousTrack() }
        btnShuffle.setOnClickListener { shuffleTracks() }
        btnSort.setOnClickListener { sortTracksByDuration() }

        listView.setOnItemClickListener { _, _, pos, _ ->
            startTrack(pos)
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && mediaPlayer != null) mediaPlayer?.seekTo(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    private fun checkPermission() {
        val perm = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(perm), PERMISSION_REQUEST_CODE)
        }
    }

    private fun chooseFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, FOLDER_PICKER_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FOLDER_PICKER_CODE && resultCode == Activity.RESULT_OK) {
            val treeUri = data?.data ?: return
            contentResolver.takePersistableUriPermission(
                treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            loadTracksFromFolder(treeUri)
        }
    }


    private fun loadTracksFromFolder(treeUri: Uri) {
        trackUris.clear()
        trackNames.clear()

        val children = contentResolver.query(
            DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri)
            ),
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null, null, null
        )

        children?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

            while (cursor.moveToNext()) {
                val mime = cursor.getString(mimeCol)
                if (mime != null && mime.startsWith("audio/")) {
                    val docId = cursor.getString(idCol)
                    val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    trackUris.add(uri)
                    trackNames.add(cursor.getString(nameCol))
                }
            }
        }

        if (trackUris.isEmpty()) {
            Toast.makeText(this, "Нет аудиофайлов в папке", Toast.LENGTH_SHORT).show()
            return
        }

        updateListView()
        Toast.makeText(this, "Загружено треков: ${trackUris.size}", Toast.LENGTH_SHORT).show()
    }

    private fun updateListView() {
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, trackNames) {
            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setSingleLine(false)
                // view.textSize = 16f
                view.setPadding(16, 12, 16, 12)
                view.setTextColor(0xFFFFFFFF.toInt())
                return view
            }
        }
        listView.adapter = adapter
    }


    private fun startTrack(index: Int) {
        if (index !in trackUris.indices) return

        try {
            mediaPlayer?.release()
            val uri = trackUris[index]
            val afd = contentResolver.openAssetFileDescriptor(uri, "r") ?: return

            mediaPlayer = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor)
                prepare()
                start()
            }
            afd.close()

            isPlaying = true
            currentTrackIndex = index
            seekBar.max = mediaPlayer!!.duration
            updateSeekBar()

            Toast.makeText(this, "▶ ${trackNames[index]}", Toast.LENGTH_SHORT).show()

            mediaPlayer?.setOnCompletionListener { nextTrack() }

        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun playTrack() {
        if (trackUris.isEmpty()) {
            Toast.makeText(this, "Выберите папку с треками", Toast.LENGTH_SHORT).show()
            return
        }

        if (mediaPlayer == null) {
            startTrack(0)
        } else {
            mediaPlayer?.start()
            isPlaying = true
        }
    }

    private fun pauseTrack() {
        mediaPlayer?.pause()
        isPlaying = false
    }

    private fun nextTrack() {
        if (trackUris.isEmpty()) return
        currentTrackIndex = (currentTrackIndex + 1) % trackUris.size
        startTrack(currentTrackIndex)
    }

    private fun previousTrack() {
        if (trackUris.isEmpty()) return
        currentTrackIndex = if (currentTrackIndex - 1 < 0) trackUris.size - 1 else currentTrackIndex - 1
        startTrack(currentTrackIndex)
    }


    private fun shuffleTracks() {
        if (trackUris.isEmpty()) return

        val currentUri = if (currentTrackIndex in trackUris.indices) trackUris[currentTrackIndex] else null
        val zipped = trackUris.zip(trackNames).shuffled(Random(System.currentTimeMillis()))
        trackUris = zipped.map { it.first }.toMutableList()
        trackNames = zipped.map { it.second }.toMutableList()

        updateListView()
        Toast.makeText(this, "🔀 Перемешано", Toast.LENGTH_SHORT).show()

        currentUri?.let {
            val newIndex = trackUris.indexOf(it)
            if (newIndex != -1) currentTrackIndex = newIndex
        }
    }


    private fun sortTracksByDuration() {
        if (trackUris.isEmpty()) return

        val retriever = MediaMetadataRetriever()


        val durations = MutableList(trackUris.size) { index ->
            try {
                retriever.setDataSource(this, trackUris[index])
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
                    ?: Long.MAX_VALUE
            } catch (e: Exception) {
                Long.MAX_VALUE
            }
        }

        retriever.release()


        for (i in 0 until durations.size - 1) {
            for (j in 0 until durations.size - i - 1) {
                if (durations[j] > durations[j + 1]) {


                    val tempDur = durations[j]
                    durations[j] = durations[j + 1]
                    durations[j + 1] = tempDur


                    val tempUri = trackUris[j]
                    trackUris[j] = trackUris[j + 1]
                    trackUris[j + 1] = tempUri


                    val tempName = trackNames[j]
                    trackNames[j] = trackNames[j + 1]
                    trackNames[j + 1] = tempName
                }
            }
        }


        updateListView()
        Toast.makeText(this, "⏱ Отсортировано по длительности", Toast.LENGTH_SHORT).show()
    }


    private fun updateSeekBar() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (mediaPlayer != null && isPlaying) {
                    seekBar.progress = mediaPlayer!!.currentPosition
                    handler.postDelayed(this, 500)
                }
            }
        }, 500)
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
        isPlaying = false
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
