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

package androidx.ui.test

import androidx.compose.Model
import androidx.compose.onPreCommit
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.test.filters.SmallTest
import androidx.ui.core.Text
import androidx.ui.layout.Container
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Model
private class TestModel {
    var i = 0
}

@SmallTest
@RunWith(JUnit4::class)
class AndroidComposeTestCaseRunnerTest {

    @get:Rule
    val composeTestRule = createComposeRule(
        disableTransitions = true
    )

    @Test
    fun foreverRecomposing_viaModel_shouldFail() {
        val model = TestModel()
        composeTestRule.forGivenContent {
            Text("Hello ${model.i}")
            model.i++
        }.performTestWithEventsControl {
            assertFailsWith<AssertionError>(
                "Changes are still pending after '10' frames.") {
                doFramesAssertAllHadChangesExceptLastOne(10)
            }
        }
    }

    // @Test //- TODO: Does not work, performs only 1 frame until stable
    fun foreverRecomposing_viaState_shouldFail() {
        composeTestRule.forGivenContent {
            val state = +state { 0 }
            Text("Hello ${state.value}")
            state.value++
        }.performTestWithEventsControl {
            assertFailsWith<AssertionError>(
                "Changes are still pending after '10' frames.") {
                doFramesAssertAllHadChangesExceptLastOne(10)
            }
        }
    }

    // @Test //- TODO: Does not work, performs only 1 frame until stable
    fun foreverRecomposing_viaStatePreCommit_shouldFail() {
        composeTestRule.forGivenContent {
            val state = +state { 0 }
            Text("Hello ${state.value}")
            onPreCommit {
                state.value++
            }
        }.performTestWithEventsControl {
            assertFailsWith<AssertionError>(
                "Changes are still pending after '10' frames.") {
                doFramesAssertAllHadChangesExceptLastOne(10)
            }
        }
    }

    @Test
    fun recomposeZeroTime() {
        composeTestRule.forGivenContent {
            // Just empty composable
        }.performTestWithEventsControl {
            doFrame()
            assertNoPendingChanges()
        }
    }

    @Test
    fun recomposeZeroTime2() {
        composeTestRule.forGivenContent {
            Text("Hello")
        }.performTestWithEventsControl {
            doFrame()
            assertNoPendingChanges()
        }
    }

    @Test
    fun recomposeOnce() {
        composeTestRule.forGivenContent {
            val state = +state { 0 }
            if (state.value < 1) {
                state.value++
            }
        }.performTestWithEventsControl {
            doFrame()
            assertNoPendingChanges()
        }
    }

    // @Test //- TODO: Does not work, performs only 1 frame until stable
    fun recomposeTwice() {
        composeTestRule.forGivenContent {
            val state = +state { 0 }
            if (state.value < 2) {
                state.value++
            }
        }.performTestWithEventsControl {
            doFramesAssertAllHadChangesExceptLastOne(2)
        }
    }

    @Test
    fun recomposeTwice2() {
        val model = TestModel()
        composeTestRule.forGivenContent {
            Text("Hello ${model.i}")
            if (model.i < 2) {
                model.i++
            }
        }.performTestWithEventsControl {
            doFramesAssertAllHadChangesExceptLastOne(2)
        }
    }

    @Test
    fun measurePositiveOnEmptyShouldFail() {
        composeTestRule.forGivenContent {
            // Just empty composable
        }.performTestWithEventsControl {
            doFrame()
            assertFailsWith<AssertionError> {
                assertMeasureSizeIsPositive()
            }
        }
    }

    @Test
    fun measurePositive() {
        composeTestRule.forGivenContent {
            Container {
                Text("Hello")
            }
        }.performTestWithEventsControl {
            doFrame()
            assertMeasureSizeIsPositive()
        }
    }

    private inline fun <reified T : Throwable> assertFailsWith(
        expectedErrorMessage: String? = null,
        noinline block: () -> Any
    ) {
        try {
            block()
        } catch (e: Throwable) {
            if (e !is T) {
                throw AssertionError("Expected exception not thrown, received: $e")
            }
            if (expectedErrorMessage != null && e.localizedMessage != expectedErrorMessage) {
                throw AssertionError("Expected error message not found, received: '" +
                        "${e.localizedMessage}'")
            }
            return
        }

        throw AssertionError("Expected exception not thrown")
    }
}