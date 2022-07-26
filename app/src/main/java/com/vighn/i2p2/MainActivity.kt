package com.vighn.i2p2

import android.Manifest
import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.os.Bundle
import androidx.core.app.ActivityCompat
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.content.Context
import android.opengl.Visibility
import androidx.recyclerview.widget.GridLayoutManager
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    var imagePaths: ArrayList<String>? = null
    var recyclerView: RecyclerView? = null
    var myAdapter: RecylerViewAdapter? = null
    lateinit var fab: FloatingActionButton
    var viewModel:PdfViewModel?=null
    lateinit var progressDialog:AlertDialog



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        installSplashScreen()
        setContentView(R.layout.activity_main)
        imagePaths = ArrayList()
        recyclerView = findViewById(R.id.recyler)

        viewModel=ViewModelProvider(this,ViewModelProvider.AndroidViewModelFactory.getInstance(application)).get(PdfViewModel::class.java)

        fab = findViewById(R.id.floatingActionButton2)
        if (hasPermission(this)) {
            imagePaths=viewModel!!.getImagePaths()
            prepareRecyclerView()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                100
            )
        }

        setupProgressAlertDialog()

        fab.setOnClickListener( View.OnClickListener {
            Log.i("mytag","before pb")
            Log.i("mytag","after pb")
            val dialog_layout = layoutInflater.inflate(R.layout.dialog_box, null, true)
            showDialogBox(dialog_layout,myAdapter!!)
        })
    }



    fun hasPermission(ctx: Context?): Boolean {
        return ContextCompat.checkSelfPermission(
            ctx!!,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == 100) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                imagePaths=viewModel!!.getImagePaths()
                prepareRecyclerView()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ),
                    100
                )
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }



    fun prepareRecyclerView() {
        myAdapter = imagePaths?.let { RecylerViewAdapter(this, it) }
        val manager = GridLayoutManager(this, 3)
        recyclerView!!.layoutManager = manager
        recyclerView!!.adapter = myAdapter
    }


    fun showDialogBox(dialog_layout:View,myAdapter:RecylerViewAdapter) {
        val list: LinkedList<String> = myAdapter.getList()
        if (list.size > 0) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Enter the name for PDF:")

            builder.setView(dialog_layout)
            val editText = dialog_layout.findViewById<EditText>(R.id.pdf_name)
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATA),
                MediaStore.Images.Media.DATA + "=?",
                arrayOf(
                    list[0]
                ),
                null
            ).use { cursor ->
                val index = cursor!!.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    editText.setText("i2p-"+cursor.getString(index))
                }
            }
            val seekbar=dialog_layout.findViewById<SeekBar>(R.id.seekBar)
            val switchButton=dialog_layout.findViewById<Switch>(R.id.switch1)
            val quality=dialog_layout.findViewById<TextView>(R.id.editTextTextPersonName)
            switchButton.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->

                if(isChecked){
                    seekbar.visibility=View.VISIBLE
                    quality.visibility=View.VISIBLE
                }
                else{
                    seekbar.visibility=View.INVISIBLE
                    quality.visibility=View.INVISIBLE
                }
            })

            builder.setPositiveButton("Convert To Pdf") { dialog, which ->
                    progressDialog.show()
                try {
                    viewModel?.viewModelScope?.launch(Dispatchers.IO) {
                        viewModel!!.createPDFiText(list, editText.text.toString(), progressDialog ,seekbar.progress,switchButton.isChecked)
                    }

                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            builder.create().show()
        } else {
            Toast.makeText(this, "Select at least 1 image", Toast.LENGTH_SHORT).show()
        }


    }

    private fun setupProgressAlertDialog(){
        val builder=AlertDialog.Builder(this)
        val dialogView=layoutInflater.inflate(R.layout.alert_dialog,null)
        builder.setView(dialogView)
        builder.setCancelable(false)
        progressDialog=builder.create()
    }


}