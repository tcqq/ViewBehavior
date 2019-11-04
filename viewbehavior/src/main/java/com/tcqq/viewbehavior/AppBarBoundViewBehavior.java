/*
 * Copyright 2018 Juliane Lehmann, Alan Perry
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

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/**
 * Behavior for Views that does not support anchoring to AppBarLayout, but instead translates the View
 * out of the bottom in sync with the AppBarLayout collapsing towards the top.
 * <p/>
 * Extends CoordinatorLayout.Behavior to keep using the pre-Lollipop shadow padding offset.
 * <p/>
 * Replaces inbuilt SnackBar displacement by a relative version that does not interfere with other
 * sources of translation for the View; in particular not translation from the sync to the scrolling AppBarLayout.
 */
public class AppBarBoundViewBehavior extends CoordinatorLayout.Behavior<View> {

    private static final String TAG = AppBarBoundViewBehavior.class.getSimpleName();

    // Whether we already registered our OnOffsetChangedListener with the AppBarLayout
    // Does not get saved in instance state, because AppBarLayout does not save its listeners either
    private boolean listenerRegistered = false;


    private ValueAnimator snackBarViewTranslationYAnimator;
    // respect that other code may also change y translation; keep track of the part coming from us
    private float snackBarViewTranslationYByThis;

    public AppBarBoundViewBehavior(Context context, AttributeSet attrs) {
        super();
    }

    @Override
    public boolean layoutDependsOn(@NonNull CoordinatorLayout parent, @NonNull View child, @NonNull View dependency) {
        if (dependency instanceof AppBarLayout && !listenerRegistered) {
            ((AppBarLayout) dependency).addOnOffsetChangedListener(new ViewOffSetter(parent, child));
            listenerRegistered = true;
        }
        return dependency instanceof AppBarLayout || super.layoutDependsOn(parent, child, dependency);
    }

    @Override
    public boolean onDependentViewChanged(@NonNull CoordinatorLayout parent, @NonNull View child, @NonNull View dependency) {
        //noinspection SimplifiableIfStatement
        if (dependency instanceof AppBarLayout) {
            // if the dependency is an AppBarLayout, do not allow super to react on that
            // we don't want that behavior
            return true;
        } else if (dependency instanceof Snackbar.SnackbarLayout) {
            updateViewTranslationForSnackBar(parent, child);
            return true;
        }
        return super.onDependentViewChanged(parent, child, dependency);
    }

    @Override
    public void onDependentViewRemoved(@NonNull CoordinatorLayout parent, @NonNull View child, @NonNull View dependency) {
        if (dependency instanceof Snackbar.SnackbarLayout) {
            updateViewTranslationForSnackBar(parent, child);
        }
    }

    private void updateViewTranslationForSnackBar(CoordinatorLayout parent,
                                                  final View view) {

        // We want to introduce additional y-translation (with respect to what's already there),
        // by the current visible height of any SnackBar
        final float targetTransYByThis = getVisibleHeightOfOverlappingSnackBar(parent, view);

        if (snackBarViewTranslationYByThis == targetTransYByThis) {
            // We're already at (or currently animating to) the target value, return...
            return;
        }

        final float currentTransY = view.getTranslationY();

        // Calculate difference between what we want now and what we wanted earlier
        final float stepTransYDelta = targetTransYByThis - snackBarViewTranslationYByThis;

        // ... and we're going to change the current state just by the difference
        final float targetTransY = currentTransY + stepTransYDelta;

        // Make sure that any current animation is cancelled
        if (snackBarViewTranslationYAnimator != null && snackBarViewTranslationYAnimator.isRunning()) {
            snackBarViewTranslationYAnimator.cancel();
        }

        if (view.isShown()
                && Math.abs(currentTransY - targetTransY) > (view.getHeight() * 0.667f)) {
            // If the view will be travelling by more than 2/3 of it's height, let's animate
            // it instead
            if (snackBarViewTranslationYAnimator == null) {
                snackBarViewTranslationYAnimator = ValueAnimator.ofFloat(currentTransY, targetTransY);
                snackBarViewTranslationYAnimator.setInterpolator(new FastOutSlowInInterpolator());
                snackBarViewTranslationYAnimator.addUpdateListener(
                        animator -> view.setTranslationY((Float) animator.getAnimatedValue()));
            }
            snackBarViewTranslationYAnimator.start();
        } else {
            // Now update the translation Y
            view.setTranslationY(targetTransY);
        }

        snackBarViewTranslationYByThis = targetTransYByThis;
    }

    /**
     * returns visible height of SnackBar, if SnackBar is overlapping view
     * 0 otherwise
     */
    private float getVisibleHeightOfOverlappingSnackBar(CoordinatorLayout parent,
                                                        View view) {
        float minOffset = 0;
        final List<View> dependencies = parent.getDependencies(view);
        for (int i = 0, z = dependencies.size(); i < z; i++) {
            final View child = dependencies.get(i);
            if (child instanceof Snackbar.SnackbarLayout && parent.doViewsOverlap(view, child)) {
                minOffset = Math.min(minOffset,
                        child.getTranslationY() - child.getHeight());
            }
        }
        return minOffset;
    }
}
