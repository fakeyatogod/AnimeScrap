package com.talent.animescrap.ui.favorite

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.talent.animescrap.adapter.RecyclerAdapter
import com.talent.animescrap.databinding.FragmentFavoriteBinding
import com.talent.animescrap.ui.viewmodels.FavoriteViewModel

class FavoriteFragment : Fragment() {

    private var _binding: FragmentFavoriteBinding? = null
    private lateinit var favoriteViewModel: FavoriteViewModel

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFavoriteBinding.inflate(inflater, container, false)

        favoriteViewModel = ViewModelProvider(this).get(FavoriteViewModel::class.java)

        binding.progressbarInMain.visibility = View.VISIBLE
        binding.recyclerView.layoutManager = GridLayoutManager(activity as Context, 2)


        favoriteViewModel.animeFavoriteList.observe(viewLifecycleOwner, {
            binding.progressbarInMain.visibility = View.GONE
            binding.recyclerView.adapter = RecyclerAdapter(activity as Context, it)
            binding.recyclerView.setHasFixedSize(true)
        })

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}