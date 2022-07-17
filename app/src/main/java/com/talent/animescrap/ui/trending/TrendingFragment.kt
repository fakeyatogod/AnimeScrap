package com.talent.animescrap.ui.trending

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

        binding.swipeContainer.setOnRefreshListener {
            binding.recyclerView.visibility = View.GONE
            trendingViewModel.animeTrendingList.observe(viewLifecycleOwner) {
                binding.recyclerView.adapter = RecyclerAdapter(activity as Context, it)
                binding.recyclerView.setHasFixedSize(true)
                binding.swipeContainer.isRefreshing = false
                binding.recyclerView.visibility = View.VISIBLE
            }

        }

        trendingViewModel.animeTrendingList.observe(viewLifecycleOwner) {
            binding.progressbarInMain.visibility = View.GONE
            binding.recyclerView.adapter = RecyclerAdapter(activity as Context, it)
            binding.recyclerView.setHasFixedSize(true)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}