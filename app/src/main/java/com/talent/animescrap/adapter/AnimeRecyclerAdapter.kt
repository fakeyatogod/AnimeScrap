package com.talent.animescrap.adapter

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.talent.animescrap.R
import com.talent.animescrap.databinding.LandscapeCoverCardviewItemBinding
import com.talent.animescrap.databinding.PortraitCoverCardviewItemBinding
import com.talent.animescrap_common.model.SimpleAnime
import com.talent.animescrap.ui.fragments.FavoriteFragmentDirections
import com.talent.animescrap.ui.fragments.LatestFragmentDirections
import com.talent.animescrap.ui.fragments.SearchFragmentDirections
import com.talent.animescrap.ui.fragments.TrendingFragmentDirections
import dagger.hilt.android.internal.managers.ViewComponentManager

class AnimeRecyclerAdapter(private val cardType: String = "portrait card") :
    ListAdapter<SimpleAnime, AnimeRecyclerAdapter.ViewHolder>(AnimeDiffUtil) {

    inner class ViewHolder(private val binding: ViewDataBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            if (binding is LandscapeCoverCardviewItemBinding) {
                binding.setClickListener { view ->
                    binding.animeInfo?.let { anime -> navigate(view, anime) }
                }
            } else if (binding is PortraitCoverCardviewItemBinding) {
                binding.setClickListener { view ->
                    binding.animeInfo?.let { anime -> navigate(view, anime) }
                }
            }
        }

        private fun navigate(view: View, anime: SimpleAnime) {
            val mContext = if (view.context is ViewComponentManager.FragmentContextWrapper)
                (view.context as ViewComponentManager.FragmentContextWrapper).baseContext
            else view.context

            val navController =
                (mContext as Activity).findNavController(R.id.nav_host_fragment_activity_main_bottom_nav)
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
            if (binding is LandscapeCoverCardviewItemBinding) {
                binding.animeInfo = item
            } else if (binding is PortraitCoverCardviewItemBinding) {
                binding.animeInfo = item
            }
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ViewDataBinding = if (cardType == "portrait card") {
            PortraitCoverCardviewItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        } else {
            LandscapeCoverCardviewItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        }
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
