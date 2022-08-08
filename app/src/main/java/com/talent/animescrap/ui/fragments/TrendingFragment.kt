package com.talent.animescrap.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.talent.animescrap.adapter.RecyclerAdapter
import com.talent.animescrap.databinding.FragmentTrendingBinding
import com.talent.animescrap.ui.viewmodels.TrendingViewModel

class TrendingFragment : Fragment() {

    private var _binding: FragmentTrendingBinding? = null
    private lateinit var trendingViewModel: TrendingViewModel
    private var rvAdapter = RecyclerAdapter()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentTrendingBinding.inflate(inflater, container, false)
        trendingViewModel = ViewModelProvider(this)[TrendingViewModel::class.java]

        binding.progressbarInMain.visibility = View.VISIBLE
        binding.recyclerView.layoutManager = GridLayoutManager(activity as Context, 2)
        binding.recyclerView.adapter = rvAdapter
        trendingViewModel.trendingAnimeList.observe(viewLifecycleOwner) {
            binding.progressbarInMain.visibility = View.GONE
            rvAdapter.submitList(it)
            binding.recyclerView.setHasFixedSize(true)
            if (binding.swipeContainer.isRefreshing) {
                binding.swipeContainer.isRefreshing = false
            }
        }

        binding.swipeContainer.setOnRefreshListener { trendingViewModel.getTrendingAnimeList() }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}