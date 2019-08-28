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

package androidx.ui.benchmark

import android.view.View
import androidx.ui.test.ComposeTestCase
import androidx.ui.test.ToggleableTestCase
import androidx.ui.test.assertNoPendingChanges
import androidx.ui.test.benchmark.android.AndroidTestCase
import androidx.ui.test.doFramesUntilNoChangesPending
import androidx.ui.test.recomposeAssertHadChanges

/**
 * Measures measure and layout performance of the given test case by toggling measure constraints.
 */
fun ComposeBenchmarkRule.benchmarkLayoutPerf(testCase: ComposeTestCase) {
    runBenchmarkFor(testCase) {
        doFramesUntilNoChangesPending()

        val width = measuredWidth
        val height = measuredHeight
        var widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        var heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)

        requestLayout()
        measureWithSpec(widthSpec, heightSpec)
        layout()

        var lastWidth = measuredWidth
        var lastHeight: Int
        measureRepeated {
            runWithTimingDisabled {
                if (lastWidth == width) {
                    lastWidth = width - 10
                    lastHeight = height - 10
                } else {

                    lastWidth = width
                    lastHeight = height
                }
                widthSpec =
                    View.MeasureSpec.makeMeasureSpec(lastWidth, View.MeasureSpec.EXACTLY)
                heightSpec =
                    View.MeasureSpec.makeMeasureSpec(lastHeight, View.MeasureSpec.EXACTLY)
                requestLayout()
            }
            measureWithSpec(widthSpec, heightSpec)
            layout()
        }
    }
}

fun ComposeBenchmarkRule.benchmarkLayoutPerf(testCase: AndroidTestCase) {
    runBenchmarkFor(testCase) {
        doFrame()

        val width = measuredWidth
        val height = measuredHeight
        var widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        var heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)

        requestLayout()
        measureWithSpec(widthSpec, heightSpec)
        layout()

        var lastWidth = measuredWidth
        var lastHeight: Int
        measureRepeated {
            runWithTimingDisabled {
                if (lastWidth == width) {
                    lastWidth = width - 10
                    lastHeight = height - 10
                } else {

                    lastWidth = width
                    lastHeight = height
                }
                widthSpec =
                    View.MeasureSpec.makeMeasureSpec(lastWidth, View.MeasureSpec.EXACTLY)
                heightSpec =
                    View.MeasureSpec.makeMeasureSpec(lastHeight, View.MeasureSpec.EXACTLY)
                requestLayout()
            }
            measureWithSpec(widthSpec, heightSpec)
            layout()
        }
    }
}

/**
 * Measures draw performance of the given test case by invalidating the view hierarchy.
 */
fun ComposeBenchmarkRule.benchmarkDrawPerf(testCase: AndroidTestCase) {
    runBenchmarkFor(testCase) {
        doFrame()

        measureRepeated {
            runWithTimingDisabled {
                invalidateViews()
                drawPrepare()
            }
            draw()
            runWithTimingDisabled {
                drawFinish()
            }
        }
    }
}

/**
 * Measures draw performance of the given test case by invalidating the view hierarchy.
 */
fun ComposeBenchmarkRule.benchmarkDrawPerf(testCase: ComposeTestCase) {
    runBenchmarkFor(testCase) {
        doFramesUntilNoChangesPending()

        measureRepeated {
            runWithTimingDisabled {
                invalidateViews()
                drawPrepare()
            }
            draw()
            runWithTimingDisabled {
                drawFinish()
            }
        }
    }
}

/**
 * Measures the time of the first composition of the given compose test case.
 */
fun ComposeBenchmarkRule.benchmarkFirstCompose(testCase: ComposeTestCase) {
    runBenchmarkFor(testCase) {
        measureRepeated {
            setupContent()
            runWithTimingDisabled {
                assertNoPendingChanges()
                disposeContent()
            }
        }
    }
}

/**
 * Measures the time of the first set content of the given Android test case.
 */
fun ComposeBenchmarkRule.benchmarkFirstSetContent(testCase: AndroidTestCase) {
    runBenchmarkFor(testCase) {
        measureRepeated {
            setupContent()
            runWithTimingDisabled {
                disposeContent()
            }
        }
    }
}

/**
 * Measures the time of the first measure of the given test case.
 */
fun ComposeBenchmarkRule.benchmarkFirstMeasure(testCase: ComposeTestCase) {
    runBenchmarkFor(testCase) {
        measureRepeated {
            runWithTimingDisabled {
                setupContent()
                requestLayout()
            }

            measure()

            runWithTimingDisabled {
                disposeContent()
            }
        }
    }
}

/**
 * Measures the time of the first measure of the given test case.
 */
fun ComposeBenchmarkRule.benchmarkFirstMeasure(testCase: AndroidTestCase) {
    runBenchmarkFor(testCase) {
        measureRepeated {
            runWithTimingDisabled {
                setupContent()
                requestLayout()
            }

            measure()

            runWithTimingDisabled {
                disposeContent()
            }
        }
    }
}

/**
 * Measures the time of the first layout of the given test case.
 */
fun ComposeBenchmarkRule.benchmarkFirstLayout(testCase: ComposeTestCase) {
    runBenchmarkFor(testCase) {
        measureRepeated {
            runWithTimingDisabled {
                setupContent()
                requestLayout()
                measure()
            }

            layout()

            runWithTimingDisabled {
                disposeContent()
            }
        }
    }
}

/**
 * Measures the time of the first layout of the given test case.
 */
fun ComposeBenchmarkRule.benchmarkFirstLayout(testCase: AndroidTestCase) {
    runBenchmarkFor(testCase) {
        measureRepeated {
            runWithTimingDisabled {
                setupContent()
                requestLayout()
                measure()
            }

            layout()

            runWithTimingDisabled {
                disposeContent()
            }
        }
    }
}

/**
 * Measures the time of the first draw of the given test case.
 */
fun ComposeBenchmarkRule.benchmarkFirstDraw(testCase: ComposeTestCase) {
    runBenchmarkFor(testCase) {
        measureRepeated {
            runWithTimingDisabled {
                setupContent()
                requestLayout()
                measure()
                layout()
                drawPrepare()
            }

            draw()

            runWithTimingDisabled {
                drawFinish()
                disposeContent()
            }
        }
    }
}

/**
 * Measures the time of the first draw of the given test case.
 */
fun ComposeBenchmarkRule.benchmarkFirstDraw(testCase: AndroidTestCase) {
    runBenchmarkFor(testCase) {
        measureRepeated {
            runWithTimingDisabled {
                setupContent()
                requestLayout()
                measure()
                layout()
                drawPrepare()
            }

            draw()

            runWithTimingDisabled {
                drawFinish()
                disposeContent()
            }
        }
    }
}

/**
 *  Measures recomposition time of the hierarchy after changing a state.
 */
fun <T> ComposeBenchmarkRule.toggleStateBenchmarkRecompose(
    testCase: T
) where T : ComposeTestCase, T : ToggleableTestCase {
    runBenchmarkFor(testCase) {
        doFramesUntilNoChangesPending()

        measureRepeated {
            runWithTimingDisabled {
                testCase.toggleState()
            }
            recomposeAssertHadChanges()
            assertNoPendingChanges()
        }
    }
}

/**
 *  Measures measure time of the hierarchy after changing a state.
 */
fun <T> ComposeBenchmarkRule.toggleStateBenchmarkMeasure(
    testCase: T,
    toggleCausesRecompose: Boolean = true
) where T : ComposeTestCase, T : ToggleableTestCase {
    runBenchmarkFor(testCase) {
        doFramesUntilNoChangesPending()

        measureRepeated {
            runWithTimingDisabled {
                testCase.toggleState()
                if (toggleCausesRecompose) {
                    recomposeAssertHadChanges()
                }
                requestLayout()
                assertNoPendingChanges()
            }
            measure()
            assertNoPendingChanges()
        }
    }
}

/**
 *  Measures layout time of the hierarchy after changing a state.
 */
fun <T> ComposeBenchmarkRule.toggleStateBenchmarkLayout(
    testCase: T,
    toggleCausesRecompose: Boolean = true
) where T : ComposeTestCase, T : ToggleableTestCase {
    runBenchmarkFor(testCase) {
        doFramesUntilNoChangesPending()

        measureRepeated {
            runWithTimingDisabled {
                testCase.toggleState()
                if (toggleCausesRecompose) {
                    recomposeAssertHadChanges()
                }
                requestLayout()
                measure()
                assertNoPendingChanges()
            }
            layout()
            assertNoPendingChanges()
        }
    }
}

/**
 *  Measures draw time of the hierarchy after changing a state.
 */
fun <T> ComposeBenchmarkRule.toggleStateBenchmarkDraw(
    testCase: T,
    toggleCausesRecompose: Boolean = true
) where T : ComposeTestCase, T : ToggleableTestCase {
    runBenchmarkFor(testCase) {
        doFramesUntilNoChangesPending()

        measureRepeated {
            runWithTimingDisabled {
                testCase.toggleState()
                if (toggleCausesRecompose) {
                    recomposeAssertHadChanges()
                }
                assertNoPendingChanges()
                requestLayout()
                measure()
                layout()
                drawPrepare()
            }
            draw()
            runWithTimingDisabled {
                drawFinish()
            }
        }
    }
}

/**
 *  Measures measure time of the hierarchy after changing a state.
 */
fun <T> ComposeBenchmarkRule.toggleStateBenchmarkMeasure(
    testCase: T
) where T : AndroidTestCase, T : ToggleableTestCase {
    runBenchmarkFor(testCase) {
        doFrame()

        measureRepeated {
            runWithTimingDisabled {
                testCase.toggleState()
            }
            measure()
        }
    }
}

/**
 *  Measures layout time of the hierarchy after changing a state.
 */
fun <T> ComposeBenchmarkRule.toggleStateBenchmarkLayout(
    testCase: T
) where T : AndroidTestCase, T : ToggleableTestCase {
    runBenchmarkFor(testCase) {
        doFrame()

        measureRepeated {
            runWithTimingDisabled {
                testCase.toggleState()
                measure()
            }
            layout()
        }
    }
}

/**
 *  Measures draw time of the hierarchy after changing a state.
 */
fun <T> ComposeBenchmarkRule.toggleStateBenchmarkDraw(
    testCase: T
) where T : AndroidTestCase, T : ToggleableTestCase {
    runBenchmarkFor(testCase) {
        doFrame()

        measureRepeated {
            runWithTimingDisabled {
                testCase.toggleState()
                measure()
                layout()
                drawPrepare()
            }
            draw()
            runWithTimingDisabled {
                drawFinish()
            }
        }
    }
}
