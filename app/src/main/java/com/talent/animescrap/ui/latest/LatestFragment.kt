package com.talent.animescrap.ui.latest

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.talent.animescrap.adapter.RecyclerAdapter
import com.talent.animescrap.databinding.FragmentLatestBinding
import com.talent.animescrap.ui.viewmodels.LatestViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LatestFragment : Fragment() {

    private var _binding: FragmentLatestBinding? = null
    private lateinit var latestViewModel: LatestViewModel

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        latestViewModel = ViewModelProvider(this)[LatestViewModel::class.java]

        _binding = FragmentLatestBinding.inflate(inflater, container, false)

        binding.progressbarInMain.visibility = View.VISIBLE
        binding.recyclerView.layoutManager = GridLayoutManager(activity as Context, 2)

        latestViewModel.animeLatestList.observe(viewLifecycleOwner) {
            binding.progressbarInMain.visibility = View.GONE
            binding.recyclerView.adapter = RecyclerAdapter(activity as Context, it)
            binding.recyclerView.setHasFixedSize(true)
        }

        binding.swipeContainer.setOnRefreshListener {
            CoroutineScope(Dispatchers.IO).launch {
                latestViewModel.getLatestAnime()
                withContext(Dispatchers.Main) {
                    binding.swipeContainer.isRefreshing = false
                }
            }

        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}