/*
 * Copyright 2018 Juliane Lehmann, Alan Dreamer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tcqq.viewbehavior;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.appbar.AppBarLayout;

/**
 * {@link AppBarLayout.OnOffsetChangedListener} implementation
 * that reacts on offset changes by translating the View towards the bottom. The View gets displaced
 * such that when the AppBarLayout is completely collapsed, then the top of the View is at the bottom
 * of the parent view (typically a {@link CoordinatorLayout}). For intermediate states, the fraction of the View
 * displacement respective to this total is relative to the fraction of the AppBarLayout collapse.
 */
public class ViewOffSetter implements AppBarLayout.OnOffsetChangedListener {

    private final View parent;
    private final View view;

    // need to separate translationY on the view that comes from this behavior
    // and one that comes from other sources
    private float viewTranslationYByThis = 0.0f;

    ViewOffSetter(@NonNull View parent, @NonNull View child) {
        this.parent = parent;
        this.view = child;
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        // view should scroll out down in sync with the appBarLayout scrolling out up.
        // let's see how far along the way the appBarLayout is
        // (if displacementFraction == 0.0f then no displacement, appBar is fully expanded;
        //  if displacementFraction == 1.0f then full displacement, appBar is totally collapsed)
        float displacementFraction = -verticalOffset / (float) appBarLayout.getTotalScrollRange();

        // top languagePosition, accounting for translation not coming from this behavior
        float topUntranslatedFromThis = view.getTop() + view.getTranslationY() - viewTranslationYByThis;

        // total length to displace by (from languagePosition uninfluenced by this behavior) for a full appBar collapse
        float fullDisplacement = parent.getBottom() - topUntranslatedFromThis;

        // calculate new value for displacement coming from this behavior
        float newTranslationYFromThis = fullDisplacement * displacementFraction;

        // update translation value by difference found in this step
        view.setTranslationY(newTranslationYFromThis - viewTranslationYByThis + view.getTranslationY());

        // store new value
        viewTranslationYByThis = newTranslationYFromThis;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ViewOffSetter that = (ViewOffSetter) o;

        return parent.equals(that.parent) && view.equals(that.view);

    }

    @Override
    public int hashCode() {
        int result = parent.hashCode();
        result = 31 * result + view.hashCode();
        return result;
    }
}
