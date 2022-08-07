package com.talent.animescrap.viewbindings

import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.databinding.BindingAdapter
import coil.load
import com.talent.animescrap.R


/* A function that is called when the `image` attribute is used in the layout. */
@BindingAdapter("image", "loaderId")
fun ImageView.setImage(url: String?, progressBar: ProgressBar) {
    if (!url.isNullOrEmpty())
        load(url) {
            error(R.drawable.ic_broken_image)
            listener(
                onSuccess = { _, _ ->
                    progressBar.visibility = View.GONE
                }
            )
            build()
        }
}