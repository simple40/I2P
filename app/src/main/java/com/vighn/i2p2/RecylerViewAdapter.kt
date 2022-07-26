package com.vighn.i2p2

import android.content.Context
import android.net.Uri
import androidx.recyclerview.widget.RecyclerView
import com.vighn.i2p2.RecylerViewAdapter.MyViewHolder
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import com.vighn.i2p2.R
import com.bumptech.glide.Glide
import java.io.File
import java.util.*

class RecylerViewAdapter(var c: Context, var imgPaths: ArrayList<String>) :
    RecyclerView.Adapter<MyViewHolder>() {
    var list = LinkedList<String>()
    var sel: BooleanArray
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val minflator = LayoutInflater.from(c)
        val view = minflator.inflate(R.layout.image2, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val imgfile = File(imgPaths[position])
        val imageUri = Uri.fromFile(imgfile)
        Glide.with(c)
            .load(imageUri).centerCrop()
            .into(holder.imageView)
        if (sel[holder.absoluteAdapterPosition]) {
            holder.imageView.alpha = .3f
        } else {
            holder.imageView.alpha = 1f
        }
    }

    override fun getItemCount(): Int {
        return imgPaths.size
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageView: ImageView

        init {
            imageView = itemView.findViewById(R.id.imageView2)
            itemView.setOnClickListener {
                if (sel[absoluteAdapterPosition]) {
                    imageView.alpha = 1f
                    list.remove(imgPaths[absoluteAdapterPosition])
                    setIsRecyclable(true)
                    sel[absoluteAdapterPosition] = false
                } else {
                    imageView.alpha = 0.3f
                    list.add(imgPaths[absoluteAdapterPosition])
                    setIsRecyclable(false)
                    sel[absoluteAdapterPosition] = true
                }
            }
        }
    }

    @JvmName("getList1")
    fun getList(): LinkedList<String> {
        return list
    }

    init {
        sel = BooleanArray(imgPaths.size)
    }
}