package com.talent.animescrap.adapter

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.talent.animescrap.R
import com.talent.animescrap.model.Photos
import com.talent.animescrap.ui.fragments.FavoriteFragmentDirections
import com.talent.animescrap.ui.fragments.LatestFragmentDirections
import com.talent.animescrap.ui.fragments.SearchFragmentDirections
import com.talent.animescrap.ui.fragments.TrendingFragmentDirections

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

        holder.itemImage.load(pic.resImage) {
            error(R.drawable.ic_broken_image)
            listener(
                onSuccess = { _, _ ->
                    holder.progressInCard.visibility = View.GONE
                }
            )
            build()
        }



        holder.cView.setOnClickListener {

            val navController =
                (context as Activity).findNavController(R.id.nav_host_fragment_activity_main_bottom_nav)
            when (navController.currentDestination?.id) {
                R.id.navigation_favorite -> {
                    val action =
                        FavoriteFragmentDirections.actionNavigationFavoriteToNavigationAnime()
                            .setAnimeLink(pic.resLink)
                    navController.navigate(action)
                }
                R.id.navigation_latest -> {
                    val action = LatestFragmentDirections.actionNavigationLatestToNavigationAnime()
                        .setAnimeLink(pic.resLink)
                    navController.navigate(action)
                }
                R.id.navigation_trending -> {
                    val action =
                        TrendingFragmentDirections.actionNavigationTrendingToNavigationAnime()
                            .setAnimeLink(pic.resLink)
                    navController.navigate(action)
                }
                R.id.navigation_search -> {
                    val action =
                        SearchFragmentDirections.actionNavigationSearchToNavigationAnime()
                            .setAnimeLink(pic.resLink)
                    navController.navigate(action)
                }
            }
        }

    }

    override fun getItemCount(): Int {
        return itemList.size

    }

}
