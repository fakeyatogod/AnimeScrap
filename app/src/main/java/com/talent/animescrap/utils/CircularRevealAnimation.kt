package com.talent.animescrap.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.transition.TransitionValues
import android.transition.Visibility
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import kotlin.math.hypot

class CircularRevealTransition : Visibility() {
    override fun onAppear(
        sceneRoot: ViewGroup?,
        view: View,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator {
        val startRadius = 0
        val endRadius = hypot(view.width.toDouble(), view.height.toDouble()).toInt()
        val reveal = ViewAnimationUtils.createCircularReveal(
            view,
            view.width / 2,
            view.height / 2,
            startRadius.toFloat(),
            endRadius.toFloat()
        )
        //make view invisible until animation actually starts
        view.alpha = 0f
        reveal.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                view.alpha = 1f
            }
        })
        return reveal
    }

    override fun onDisappear(
        sceneRoot: ViewGroup?,
        view: View,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator {
        val endRadius = 0
        val startRadius =
            hypot(view.width.toDouble(), view.height.toDouble()).toInt()
        return ViewAnimationUtils.createCircularReveal(
            view,
            view.width / 2,
            view.height / 2,
            startRadius.toFloat(),
            endRadius.toFloat()
        )
    }
}