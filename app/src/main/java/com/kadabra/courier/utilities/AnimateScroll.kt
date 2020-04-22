package com.kadabra.courier.utilities

import android.graphics.Point
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.ScrollView

/**
 * Created by Mokhtar on 1/19/2020.
 */

object AnimateScroll {

     fun scrollToView(scrollViewParent: ScrollView, view: View) {
        // Get deepChild Offset
        val childOffset = Point()
         getDeepChildOffset(scrollViewParent, view.parent, view, childOffset)
        // Scroll to child.
        scrollViewParent.smoothScrollTo(0, childOffset.y)
    }

     fun getDeepChildOffset(mainParent: ViewGroup, parent: ViewParent, child: View, accumulatedOffset: Point) {
        val parentGroup = parent as ViewGroup
        accumulatedOffset.x += child.left
        accumulatedOffset.y += child.top
        if (parentGroup == mainParent) {
            return
        }
         getDeepChildOffset(mainParent, parentGroup.parent, parentGroup, accumulatedOffset)
    }
}
