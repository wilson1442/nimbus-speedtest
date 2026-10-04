package cloud.g3h.nimbus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pins the framework semantics the TV focus ring depends on.
 *
 * The ring is only drawn when the state watched by `onFocusChanged` becomes true,
 * so the ORDER of `onFocusChanged` relative to `focusable()` is load-bearing.
 * Android's docs are explicit — "the `onFocusChanged()` modifier refers to the
 * first focusable element that appears after the `focusable()` or `focusTarget()`
 * modifiers" — and these tests hold the library to it, because getting it wrong
 * fails silently: the ring simply never draws.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FocusSemanticsTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `onFocusChanged before focusable observes focus`() {
        var isFocused = false
        var hasFocus = false
        lateinit var requester: FocusRequester
        rule.setContent {
            requester = remember { FocusRequester() }
            Box(
                Modifier
                    .size(40.dp)
                    .focusRequester(requester)
                    .onFocusChanged { isFocused = it.isFocused; hasFocus = it.hasFocus }
                    .focusable()
            )
        }
        rule.runOnIdle { requester.requestFocus() }
        rule.waitForIdle()
        assertTrue("onFocusChanged placed BEFORE focusable must observe focus", isFocused || hasFocus)
    }

    @Test
    fun `onFocusChanged after focusable observes nothing`() {
        var isFocused = false
        var hasFocus = false
        lateinit var requester: FocusRequester
        rule.setContent {
            requester = remember { FocusRequester() }
            Box(
                Modifier
                    .size(40.dp)
                    .focusRequester(requester)
                    // the order the shipping v1.7.2 code used
                    .focusable()
                    .onFocusChanged { isFocused = it.isFocused; hasFocus = it.hasFocus }
            )
        }
        rule.runOnIdle { requester.requestFocus() }
        rule.waitForIdle()
        // If this ever starts failing, Compose changed and the ring could come back.
        assertTrue(
            "EXPECTED the old order to observe nothing. isFocused=$isFocused hasFocus=$hasFocus",
            !(isFocused || hasFocus)
        )
    }

    @Test
    fun `which node wins focus when clickable is also in the chain`() {
        var isFocused = false
        var hasFocus = false
        var clicks = 0
        lateinit var requester: FocusRequester
        rule.setContent {
            requester = remember { FocusRequester() }
            Box(
                Modifier
                    .size(40.dp)
                    .focusRequester(requester)
                    .onFocusChanged { isFocused = it.isFocused; hasFocus = it.hasFocus }
                    .focusable()
                    .clickable { clicks++ }
            )
        }
        rule.runOnIdle { requester.requestFocus() }
        rule.waitForIdle()
        println("FOCUS-PROBE isFocused=$isFocused hasFocus=$hasFocus clicks=$clicks")
        assertTrue("something in the chain must hold focus", isFocused || hasFocus)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `D-pad OK still activates clickable when an extra focusable is present`() {
        // nimbusFocus adds focusable(); the call sites then add clickable(). If the
        // outer node takes the focus, clickable's own key handler might never run —
        // which would mean pressing OK does nothing. Verify rather than assume.
        var clicks = 0
        lateinit var requester: FocusRequester
        rule.setContent {
            requester = remember { FocusRequester() }
            Box(
                Modifier
                    .size(40.dp)
                    .testTag("box")
                    .focusRequester(requester)
                    .onFocusChanged { }
                    .focusable()
                    .clickable { clicks++ }
            )
        }
        rule.runOnIdle { requester.requestFocus() }
        rule.waitForIdle()
        rule.onNodeWithTag("box").performKeyInput { pressKey(Key.DirectionCenter) }
        rule.waitForIdle()
        println("FOCUS-OK-PROBE clicks=$clicks")
        assertTrue("D-pad OK must still activate the clickable (clicks=$clicks)", clicks > 0)
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun `the real nimbusFocus element takes focus and responds to OK`() {
        // Exercises the shipping modifier rather than a synthetic chain: this is the
        // exact shape the app builds (focusRequester -> nimbusFocus -> clickable).
        var clicks = 0
        lateinit var requester: FocusRequester
        rule.setContent {
            requester = remember { FocusRequester() }
            Box(
                Modifier
                    .size(80.dp)
                    .testTag("start")
                    .focusRequester(requester)
                    .nimbusFocus(cornerRadius = 1000.dp, ringThickness = 3.dp, gap = 7.dp)
                    .clickable { clicks++ }
            )
        }
        rule.runOnIdle { requester.requestFocus() }
        rule.waitForIdle()
        rule.onNodeWithTag("start").assertIsFocused()
        rule.onNodeWithTag("start").performKeyInput { pressKey(Key.DirectionCenter) }
        rule.waitForIdle()
        assertTrue("nimbusFocus element must be focusable and clickable (clicks=$clicks)", clicks > 0)
    }

    // A pixel-level assertion ("the ring colour appears once focused") would be the
    // strongest guard here, but Robolectric cannot support it in this project:
    // captureToImage() needs @GraphicsMode(NATIVE) to rasterise a real window, and in
    // NATIVE mode key injection stops reaching clickable (verified — the key tests
    // above go from clicks>0 to clicks=0). So the mechanism is pinned instead: the
    // two ordering tests below/above hold Compose to the documented rule, and the
    // real-modifier test proves the shipping chain is focusable and activatable.
}
