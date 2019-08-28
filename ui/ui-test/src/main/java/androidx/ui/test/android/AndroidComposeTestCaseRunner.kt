/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.ui.test.android

import android.R
import android.annotation.TargetApi
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Picture
import android.graphics.RenderNode
import android.os.Build
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.Compose
import androidx.compose.CompositionContext
import androidx.compose.FrameManager
import androidx.compose.Recomposer
import androidx.compose.frames.currentFrame
import androidx.compose.frames.inFrame
import androidx.ui.core.AndroidComposeView
import androidx.ui.core.ComponentNode
import androidx.ui.core.DrawNode
import androidx.ui.core.setContent
import androidx.ui.test.ComposeBenchmarkScope
import androidx.ui.test.ComposeTestCase

/**
 * Factory method to provide implementation of [ComposeBenchmarkScope].
 */
fun createAndroidComposeBenchmarkRunner(
    testCase: ComposeTestCase,
    activity: Activity
): ComposeBenchmarkScope {
    return AndroidComposeTestCaseRunner(testCase, activity)
}

internal class AndroidComposeTestCaseRunner(
    private val testCase: ComposeTestCase,
    private val activity: Activity
) : ComposeBenchmarkScope {

    override val measuredWidth: Int
        get() = view!!.measuredWidth
    override val measuredHeight: Int
        get() = view!!.measuredHeight

    internal var view: ViewGroup? = null
        private set

    private var compositionContext: CompositionContext? = null

    override var didLastRecomposeHaveChanges = false
        private set

    private val supportsRenderNode = Build.VERSION.SDK_INT >= 29

    private val screenWithSpec: Int
    private val screenHeightSpec: Int
    private val capture = if (supportsRenderNode) RenderNodeCapture() else PictureCapture()
    private var canvas: Canvas? = null

    private var recomposer: Recomposer? = null

    private var simulationState: SimulationState = SimulationState.Initialized

    init {
        val displayMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels

        screenWithSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.AT_MOST)
        screenHeightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST)
    }

    override fun setupContent() {
        require(view == null) { "Content was already set!" }

        recomposer = Recomposer.current()
        compositionContext = activity.setContent { testCase.emitContent() }!!
        FrameManager.nextFrame()
        view = findComposeView(activity)!!
    }

    override fun hasPendingChanges(): Boolean {
        if (Recomposer.hasPendingChanges() || hasPendingChangesInFrame()) {
            FrameManager.nextFrame()
        }

        return Recomposer.hasPendingChanges()
    }

    /**
     * The reason we have this method is that if a model gets changed in the same frame as created
     * it won'd trigger pending frame. So [Recompose#hasPendingChanges] stays false. Committing
     * the current frame does not help either. So we need to check this in order to know if we
     * need to recompose.
     */
    private fun hasPendingChangesInFrame(): Boolean {
        return inFrame && currentFrame().hasPendingChanges()
    }

    override fun measure() {
        getView().measure(screenWithSpec, screenHeightSpec)
        simulationState = SimulationState.MeasureDone
    }

    override fun measureWithSpec(widthSpec: Int, heightSpec: Int) {
        getView().measure(widthSpec, heightSpec)
        simulationState = SimulationState.MeasureDone
    }

    override fun drawPrepare() {
        require(simulationState == SimulationState.LayoutDone ||
                simulationState == SimulationState.DrawDone) {
            "Draw can be only executed after layout or draw, current state is '$simulationState'"
        }
        canvas = capture.beginRecording(getView().width, getView().height)
        simulationState = SimulationState.DrawPrepared
    }

    override fun draw() {
        require(simulationState == SimulationState.DrawPrepared) {
            "You need to call 'drawPrepare' before calling 'draw'."
        }
        getView().draw(canvas)
        simulationState = SimulationState.DrawInProgress
    }

    override fun drawFinish() {
        require(simulationState == SimulationState.DrawInProgress) {
            "You need to call 'draw' before calling 'drawFinish'."
        }
        capture.endRecording()
        simulationState = SimulationState.DrawDone
    }

    override fun drawToBitmap() {
        drawPrepare()
        draw()
        drawFinish()
    }

    override fun requestLayout() {
        getView().requestLayout()
    }

    override fun layout() {
        require(simulationState == SimulationState.MeasureDone) {
            "Layout can be only executed after measure, current state is '$simulationState'"
        }
        val view = getView()
        view.layout(view.left, view.top, view.right, view.bottom)
        simulationState = SimulationState.LayoutDone
    }

    override fun recompose() {
        if (hasPendingChanges()) {
            didLastRecomposeHaveChanges = true
            recomposer!!.recomposeSync()
        } else {
            didLastRecomposeHaveChanges = false
        }
        simulationState = SimulationState.RecomposeDone
    }

    override fun doFrame() {
        if (view == null) {
            setupContent()
        }

        recompose()

        measure()
        layout()
        drawToBitmap()
    }

    override fun invalidateViews() {
        invalidateViews(getView())
    }

    override fun disposeContent() {
        if (view == null) {
            // Already disposed or never created any content
            return
        }

        // TODO(pavlis): replace with activity.disposeComposition() after it gets fixed.
        Compose.disposeComposition((view as AndroidComposeView).root, activity, null)

        // Clear the view
        val rootView = activity.findViewById(R.id.content) as ViewGroup
        rootView.removeAllViews()
        // Important so we can set the content again.
        view = null
        simulationState = SimulationState.Initialized
    }

    override fun capturePreviewPictureToActivity() {
        require(measuredWidth > 0 && measuredHeight > 0) {
            "Preview can't be used on empty view. Did you run measure & layout before calling it?"
        }

        val picture = Picture()
        val canvas = picture.beginRecording(getView().measuredWidth, getView().measuredHeight)
        getView().draw(canvas)
        picture.endRecording()
        val imageView = ImageView(activity)
        val bitmap: Bitmap
        if (Build.VERSION.SDK_INT >= 28) {
            bitmap = Bitmap.createBitmap(picture)
        } else {
            val width = picture.width.coerceAtLeast(1)
            val height = picture.height.coerceAtLeast(1)
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            Canvas(bitmap).drawPicture(picture)
        }
        imageView.setImageBitmap(bitmap)
        activity.setContentView(imageView)
    }

    private fun getView(): ViewGroup {
        require(view != null) { "View was not set! Call setupContent first!" }
        return view!!
    }
}

private enum class SimulationState {
    Initialized,
    MeasureDone,
    LayoutDone,
    DrawPrepared,
    DrawInProgress,
    DrawDone,
    RecomposeDone
}

private fun findComposeView(activity: Activity): AndroidComposeView? {
    return findComposeView(activity.findViewById(android.R.id.content) as ViewGroup)
}

private fun findComposeView(view: View): AndroidComposeView? {
    if (view is AndroidComposeView) {
        return view
    }

    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            val composeView = findComposeView(view.getChildAt(i))
            if (composeView != null) {
                return composeView
            }
        }
    }
    return null
}

private fun invalidateViews(view: View) {
    view.invalidate()
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            invalidateViews(child)
        }
    }
    if (view is AndroidComposeView) {
        invalidateComponentNodes(view.root)
    }
}

private fun invalidateComponentNodes(node: ComponentNode) {
    if (node is DrawNode) {
        node.invalidate()
    }
    node.visitChildren { child ->
        invalidateComponentNodes(child)
    }
}

// We must separate the use of RenderNode so that it isn't referenced in any
// way on platforms that don't have it. This extracts RenderNode use to a
// potentially unloaded class, RenderNodeCapture.
private interface DrawCapture {
    fun beginRecording(width: Int, height: Int): Canvas
    fun endRecording()
}

@TargetApi(Build.VERSION_CODES.Q)
private class RenderNodeCapture : DrawCapture {
    private val renderNode = RenderNode("Test")

    override fun beginRecording(width: Int, height: Int): Canvas {
        renderNode.setPosition(0, 0, width, height)
        return renderNode.beginRecording()
    }

    override fun endRecording() {
        renderNode.endRecording()
    }
}

private class PictureCapture : DrawCapture {
    private val picture = Picture()

    override fun beginRecording(width: Int, height: Int): Canvas {
        return picture.beginRecording(width, height)
    }

    override fun endRecording() {
        picture.endRecording()
    }
}
