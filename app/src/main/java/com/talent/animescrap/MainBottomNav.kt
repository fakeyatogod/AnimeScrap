package com.talent.animescrap

import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.talent.animescrap.adapter.RecyclerAdapter
import com.talent.animescrap.databinding.ActivityMainBottomNavBinding
import com.talent.animescrap.model.Photos
import org.jsoup.Jsoup
import java.util.*


class MainBottomNav : AppCompatActivity() {

    private lateinit var binding: ActivityMainBottomNavBinding
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBottomNavBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main_bottom_nav)

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_favorite, R.id.navigation_latest, R.id.navigation_trending
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.topbar_menu, menu)
        val search = menu?.findItem(R.id.search_nav)
        searchView = search?.actionView as SearchView
        searchView.queryHint = "Search Anime"

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {

                val newText2 = newText.lowercase(Locale.ENGLISH)
                if (newText.length >= 3) {
                    supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main_bottom_nav)
                        ?.let {
                            supportFragmentManager.beginTransaction()
                                .hide(it).commit()
                        }
                    findViewById<BottomNavigationView>(R.id.nav_view).visibility = View.GONE
                    val recyclerView = binding.recyclerView2
                    recyclerView.visibility = View.VISIBLE
                    recyclerView.layoutManager = GridLayoutManager(this@MainBottomNav, 2)
                    val searchUrl = "https://yugenani.me/search/?q=${newText2.replace(" ", "+")}"
                    println(searchUrl)
                    Thread {
                        val picInfo = arrayListOf<Photos>()
                        val doc = Jsoup.connect(searchUrl).get()
                        val allInfo = doc.getElementsByClass("anime-meta")
                        for (item in allInfo) {
                            val itemImage = item.getElementsByTag("img").attr("data-src")
                            val itemName = item.getElementsByClass("anime-name").text()
                            val itemLink = item.attr("href")
                            val picObject = Photos(itemName, itemImage, itemLink)
                            picInfo.add(picObject)
                        }

                        runOnUiThread {
                            recyclerView.adapter = RecyclerAdapter(this@MainBottomNav, picInfo)
                            recyclerView.setHasFixedSize(true)
                        }
                    }.start()

                } else {
                    val recyclerView = binding.recyclerView2
                    recyclerView.visibility = View.GONE
                }
                return false
            }
        })

        return super.onCreateOptionsMenu(menu)

    }

    override fun onBackPressed() {
        if (!searchView.isIconified) {
            searchView.isIconified = true
            searchView.onActionViewCollapsed()
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main_bottom_nav)
                ?.let {
                    supportFragmentManager.beginTransaction()
                        .show(it).commit()
                }
            findViewById<BottomNavigationView>(R.id.nav_view).visibility = View.VISIBLE
        } else {
            super.onBackPressed()
        }
    }


}