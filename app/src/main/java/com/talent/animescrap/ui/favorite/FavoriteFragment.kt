package com.talent.animescrap.ui.favorite

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.talent.animescrap.adapter.RecyclerAdapter
import com.talent.animescrap.databinding.FragmentFavoriteBinding
import com.talent.animescrap.ui.viewmodels.FavoriteViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoriteFragment : Fragment() {

    private var _binding: FragmentFavoriteBinding? = null
    private val favoriteViewModel: FavoriteViewModel by viewModels()
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFavoriteBinding.inflate(inflater, container, false)

        binding.progressbarInMain.visibility = View.VISIBLE
        binding.recyclerView.layoutManager = GridLayoutManager(activity as Context, 2)

        favoriteViewModel.animeFavoriteList.observe(viewLifecycleOwner) {
            binding.progressbarInMain.visibility = View.GONE
            binding.recyclerView.adapter = RecyclerAdapter(activity as Context, it)
            binding.recyclerView.setHasFixedSize(true)
        }

        binding.swipeContainer.setOnRefreshListener {
            CoroutineScope(Dispatchers.IO).launch {
                favoriteViewModel.getLatestAnime(requireContext())
                withContext(Dispatchers.Main) {
                    binding.swipeContainer.isRefreshing = false
                }
            }

        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        CoroutineScope(Dispatchers.IO).launch {
            favoriteViewModel.getLatestAnime(requireContext())
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}