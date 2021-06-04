package com.talent.animescrap.ui.trending

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.talent.animescrap.adapter.RecyclerAdapter
import com.talent.animescrap.databinding.FragmentTrendingBinding
import com.talent.animescrap.model.Photos
import org.jsoup.Jsoup

class TrendingFragment : Fragment() {

    private var _binding: FragmentTrendingBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentTrendingBinding.inflate(inflater, container, false)

        _binding!!.progressbarInMain.visibility = View.VISIBLE
        _binding!!.recyclerView.layoutManager = GridLayoutManager(activity as Context, 2)

        Thread {

            val picInfo = arrayListOf<Photos>()
            val url = "https://yugenani.me/trending/"

            val doc = Jsoup.connect(url).get()
            val allInfo = doc.getElementsByClass("anime-meta")
            for (item in allInfo) {
                val itemImage = item.getElementsByTag("img").attr("data-src")
                val itemName = item.getElementsByClass("anime-name").attr("title")
                val itemLink = item.attr("href")
                val picObject = Photos(itemName, itemImage, itemLink)
                picInfo.add(picObject)
            }

            activity?.runOnUiThread {
                _binding!!.progressbarInMain.visibility = View.GONE
                _binding!!.recyclerView.adapter = RecyclerAdapter(activity as Context, picInfo)
                _binding!!.recyclerView.setHasFixedSize(true)
            }
        }.start()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}