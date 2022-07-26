package com.vighn.i2p2

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import com.itextpdf.io.image.ImageData
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.CompressionConstants
import com.itextpdf.kernel.pdf.DocumentProperties
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.AreaBreak
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.properties.AreaBreakType
import com.itextpdf.layout.properties.HorizontalAlignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.*



class PdfViewModel(val context: Application) : AndroidViewModel(context) {

    var contentResolver:ContentResolver = context.contentResolver
    suspend fun createPDFiText(l1: LinkedList<String>, name: String,progressDialog: AlertDialog,quality:Int,comp:Boolean){


        lateinit var outputStream: OutputStream
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val contentValues = ContentValues()
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.pdf")
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            contentValues.put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOCUMENTS
            )
            val uri =
                contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            try {
                  outputStream = contentResolver.openOutputStream(uri!!)!!
            } catch (e: IOException) {
                Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        } else {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "$name.pdf"
            )
            try {
                //file.createNewFile()
                outputStream=FileOutputStream(file)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }


        val pdfWriter=PdfWriter(outputStream)
        pdfWriter.compressionLevel=CompressionConstants.BEST_COMPRESSION
        pdfWriter.setSmartMode(true)
        val pdfDocument=com.itextpdf.kernel.pdf.PdfDocument(pdfWriter)
        val document=Document(pdfDocument, PageSize.A4)
        var Quality=100
        if(comp){
            Quality=quality
        }


        for (i in l1.indices) {
            val imgFile = File(l1[i])
            if (imgFile.exists()) {

                var bmp = reducingQuality(BitmapFactory.decodeFile(imgFile.absolutePath))
                val stream=com.itextpdf.io.source.ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG,Quality,stream)
                val byteArary=stream.toByteArray()
                var imageData:ImageData
                if(comp){

                    imageData = ImageDataFactory.create(byteArary)
                }
                else{
                    imageData=ImageDataFactory.create(imgFile.path)
                }
                val image=Image(imageData)


                if(image.imageHeight>image.imageWidth){
                    Log.i("inside","portrait")
                    if(i!=0){
                        document.add(AreaBreak(PageSize.A4))
                    }
                    else{
                        pdfDocument.defaultPageSize=PageSize.A4
                    }
                    image.scaleToFit(575f,822f)
                    document.setMargins(10f,10f,10f,10f)
                    image.setFixedPosition((595-image.imageScaledWidth)/2,(842-image.imageScaledHeight)/2)
                    var wi=pdfDocument.defaultPageSize.width
                    Log.i("p7age Size",wi.toString())
                    Log.i("page Size",pdfDocument.defaultPageSize.height.toString())
                }
                else{
                    Log.i("inside","landscape")
                    if(i!=0){
                        document.add(AreaBreak(PageSize.A4.rotate()))
                    }
                    else{
                        pdfDocument.defaultPageSize=PageSize.A4.rotate()
                    }
                    document.setMargins(10f,10f,10f,10f)
                    image.scaleToFit(822f,575f)
                    image.setFixedPosition((842-image.imageScaledWidth)/2,(595-image.imageScaledHeight)/2)
                    var wi=pdfDocument.defaultPageSize.width
                    Log.i("p7age Size",wi.toString())
                    Log.i("page Size",pdfDocument.defaultPageSize.height.toString())
                }

                document.add(image)
                stream.close()
            }
        }
        document.close()

        try {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Done. Locate your pdf in your Document directory.",
                    Toast.LENGTH_SHORT
                ).show()
                progressDialog.dismiss()
            }
        } catch (e: IOException) {
            Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
        outputStream.close()
    }



    @Throws(IOException::class)
    suspend fun createPDF(l1: LinkedList<String>, name: String,progressDialog: AlertDialog) {
        val pdf = PdfDocument()
        val paint = Paint()
        var scaledbmp:Bitmap?=null
        for (i in l1.indices) {
            val imgFile = File(l1[i])
            if (imgFile.exists()) {
               var bmp = BitmapFactory.decodeFile(imgFile.absolutePath)
                var height: Int
                var width: Int
                if (bmp!!.getWidth() > bmp!!.getHeight()) {
                    width = 612
                    height = bmp!!.getHeight() * width / bmp!!.getWidth()
                     scaledbmp = Bitmap.createScaledBitmap(bmp!!, width, height, false)
                } else {
                    height = 816
                    println(height)
                    Log.i("myHeight",height.toString())
                    width = bmp!!.getWidth() * height / bmp!!.getHeight()
                    scaledbmp = Bitmap.createScaledBitmap(bmp!!, 583, height, false)
                    scaledbmp=compressImage(scaledbmp)
                }
            }
            val info = PdfDocument.PageInfo.Builder(scaledbmp!!.width, scaledbmp!!.height, 1).create()
            val page = pdf.startPage(info)
            val canvas = page.canvas
            canvas.drawBitmap(
                scaledbmp!!,
                0f
                ,
                0f
                ,
                paint
            )

            pdf.finishPage(page)


        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val contentValues = ContentValues()
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.pdf")
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            contentValues.put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOCUMENTS
            )
            val uri =
                contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            try {
                val outputStream = contentResolver.openOutputStream(uri!!)
                pdf.writeTo(outputStream)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Done. Locate your pdf in your Document directory.",
                        Toast.LENGTH_SHORT
                    ).show()
                    progressDialog.dismiss()
                }
            } catch (e: IOException) {
                Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        } else {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "$name.pdf"
            )
            try {
                file.createNewFile()
                pdf.writeTo(FileOutputStream(file))
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Done. Locate your pdf in your Document directory.",
                        Toast.LENGTH_SHORT
                    ).show()
                    progressDialog.dismiss()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        pdf.close()

    }

    @SuppressLint("NotifyDataSetChanged")
    fun getImagePaths() : ArrayList<String>? {
        var imagePaths=ArrayList<String>()
        val columns = arrayOf(MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID)
        val oderby = MediaStore.Images.Media._ID
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            columns,
            null,
            null,
            "$oderby DESC"
        ).use { cursor ->
            while (cursor!!.moveToNext()) {
                val cI = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                imagePaths.add(cursor.getString(cI))
            }
        }
        return imagePaths
    }

    fun compressImage(bitmap: Bitmap): Bitmap {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream) //0=lowest, 100=highest quality

        val byteArray: ByteArray = stream.toByteArray()
        //convert your byteArray into bitmap
        //convert your byteArray into bitmap
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    fun reducingQuality(bitmap: Bitmap):Bitmap{
        val maxDimension=1280
        var bmpWidth:Int
        var bmpHeight:Int
        if(bitmap.height>bitmap.width){
            bmpHeight=maxDimension
            bmpWidth=bitmap.width*maxDimension/bitmap.height
        }
        else{
            bmpWidth=maxDimension
            bmpHeight=bitmap.height*maxDimension/bitmap.width
        }
        return Bitmap.createScaledBitmap(bitmap,bmpWidth,bmpHeight,true)
    }



}