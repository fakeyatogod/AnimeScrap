package com.talent.animescrap.ui.favorite

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.room.Room
import com.talent.animescrap.adapter.RecyclerAdapter
import com.talent.animescrap.databinding.FragmentFavoriteBinding
import com.talent.animescrap.model.Photos
import com.talent.animescrap.room.LinksRoomDatabase
import org.jsoup.Jsoup

class FavoriteFragment : Fragment() {

    private var _binding: FragmentFavoriteBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFavoriteBinding.inflate(inflater, container, false)

        _binding!!.progressbarInMain.visibility = View.VISIBLE
        _binding!!.recyclerView.layoutManager = GridLayoutManager(activity as Context, 2)

        val db = Room.databaseBuilder(
            activity as Context, LinksRoomDatabase::class.java, "link-db"
        ).build()

        Thread {
            val picInfo = arrayListOf<Photos>()
            val linkDao = db.linkDao()
            val favLinks = linkDao.getLinks()
            for (i in favLinks) {
                val url = "https://yugenani.me${i.linkString}watch/?sort=episode"
                val doc = Jsoup.connect(url).get()
                val animeContent = doc.getElementsByClass("p-10-t")
                val animeCover = doc.getElementsByClass("cover").attr("src")
                val animeDetails = arrayListOf<String>()
                for (element in animeContent) {
                    animeDetails.add(element.text())
                }
                val picObject = Photos(animeDetails[0], animeCover, i.linkString)
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