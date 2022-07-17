package com.talent.animescrap.adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityOptionsCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.talent.animescrap.ui.activities.PageActivity
import com.talent.animescrap.R
import com.talent.animescrap.model.Photos

class RecyclerAdapter(val context: Context, private val itemList: ArrayList<Photos>) :
    RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val itemName: TextView = itemView.findViewById(R.id.name)
        val itemImage: ImageView = itemView.findViewById(R.id.imageView)
        val cView: com.google.android.material.card.MaterialCardView =
            itemView.findViewById(R.id.cView)
        val progressInCard: CircularProgressIndicator = itemView.findViewById(R.id.progressInCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v =
            LayoutInflater.from(parent.context).inflate(R.layout.main_cardview_item, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pic = itemList[position]
        holder.itemName.text = pic.resName

        Picasso.get().load(pic.resImage).error(R.drawable.ic_broken_image)
            .into(
                holder.itemImage, object : Callback {
                    override fun onSuccess() {
                        holder.progressInCard.visibility = View.GONE
                    }

                    override fun onError(e: Exception?) {
                    }

                }
            )


        holder.cView.setOnClickListener {
            val intent = Intent(context, PageActivity::class.java)
            intent.putExtra("content_link", pic.resLink)

            val options = ActivityOptionsCompat.makeSceneTransitionAnimation((context as Activity),holder.itemImage, "pageImageT")
            context.startActivity(intent, options.toBundle())
        }

    }

    override fun getItemCount(): Int {
        return itemList.size

    }

}
