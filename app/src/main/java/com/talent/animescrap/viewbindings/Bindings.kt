package com.talent.animescrap.viewbindings

import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.databinding.BindingAdapter
import coil.load
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerDrawable
import com.talent.animescrap.R

private val shimmer =
    Shimmer.AlphaHighlightBuilder()// The attributes for a ShimmerDrawable is set by this builder
        .setDuration(1200) // how long the shimmering animation takes to do one full sweep
        .setBaseAlpha(0.6f) //the alpha of the underlying children
        .setHighlightAlpha(0.9f) // the shimmer alpha amount
        .setDirection(Shimmer.Direction.LEFT_TO_RIGHT)
        .setAutoStart(true)
        .build()
/* A function that is called when the `image` attribute is used in the layout. */
@BindingAdapter("image")
fun ImageView.setImage(url: String?) {
    if (!url.isNullOrEmpty())
        load(url) {
            placeholder(ShimmerDrawable().apply {
                setShimmer(shimmer)
            })
            error(R.drawable.ic_broken_image)
            build()
        }
}