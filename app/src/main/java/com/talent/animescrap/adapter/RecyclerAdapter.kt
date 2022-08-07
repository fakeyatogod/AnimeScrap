package com.talent.animescrap.adapter

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.talent.animescrap.R
import com.talent.animescrap.databinding.MainCardviewItemBinding
import com.talent.animescrap.model.SimpleAnime
import com.talent.animescrap.ui.fragments.FavoriteFragmentDirections
import com.talent.animescrap.ui.fragments.LatestFragmentDirections
import com.talent.animescrap.ui.fragments.SearchFragmentDirections
import com.talent.animescrap.ui.fragments.TrendingFragmentDirections

class RecyclerAdapter : ListAdapter<SimpleAnime, RecyclerAdapter.ViewHolder>(AnimeDiffUtil) {
    inner class ViewHolder(private val binding: MainCardviewItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.setClickListener { view ->
                binding.animeInfo?.let { anime ->
                    navigate(view, anime)
                }
            }
        }

        private fun navigate(view: View, anime: SimpleAnime) {
            val navController =
                (view.context as Activity).findNavController(R.id.nav_host_fragment_activity_main_bottom_nav)
            when (navController.currentDestination?.id) {
                R.id.navigation_favorite -> {
                    val action =
                        FavoriteFragmentDirections.actionNavigationFavoriteToNavigationAnime()
                            .setAnimeLink(anime.animeLink)
                    navController.navigate(action)
                }

                R.id.navigation_latest -> {
                    val action = LatestFragmentDirections.actionNavigationLatestToNavigationAnime()
                        .setAnimeLink(anime.animeLink)
                    navController.navigate(action)
                }

                R.id.navigation_trending -> {
                    val action =
                        TrendingFragmentDirections.actionNavigationTrendingToNavigationAnime()
                            .setAnimeLink(anime.animeLink)
                    navController.navigate(action)
                }

                R.id.navigation_search -> {
                    val action =
                        SearchFragmentDirections.actionNavigationSearchToNavigationAnime()
                            .setAnimeLink(anime.animeLink)
                    navController.navigate(action)
                }
            }
        }

        fun bind(item: SimpleAnime?) {
            binding.animeInfo = item
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            MainCardviewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    object AnimeDiffUtil : DiffUtil.ItemCallback<SimpleAnime>() {
        override fun areItemsTheSame(oldItem: SimpleAnime, newItem: SimpleAnime): Boolean {
            return oldItem.animeLink == newItem.animeLink
        }

        override fun areContentsTheSame(oldItem: SimpleAnime, newItem: SimpleAnime): Boolean {
            return oldItem == newItem
        }
    }

}
