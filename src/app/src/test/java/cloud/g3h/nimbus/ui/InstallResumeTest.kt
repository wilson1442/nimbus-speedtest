package cloud.g3h.nimbus.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the rule behind "prompt, then install" — the user must not have to
 * force-close the app for an update to take.
 *
 * The bug: installUpdate() sent the user to the "install unknown apps" toggle and
 * returned. Nothing re-ran the install when they came back, so the workflow only
 * completed after a force-close restarted the app and the launch check re-prompted.
 * The fix is that onAppResumed() finishes the job, gated by this predicate.
 */
class InstallResumeTest {

    @Test
    fun `install completes when the grant was requested and is now on`() {
        assertTrue(
            "returning from the settings toggle after granting must install immediately",
            NimbusViewModel.shouldCompleteInstall(awaitingPermission = true, stillNeedsPermission = false)
        )
    }

    @Test
    fun `install does not fire while the grant is still missing`() {
        assertFalse(
            "the prompt must stay up so the user can retry, not silently do nothing",
            NimbusViewModel.shouldCompleteInstall(awaitingPermission = true, stillNeedsPermission = true)
        )
    }

    @Test
    fun `a plain resume never installs anything`() {
        // Ordinary app resumes (every onResume) must not trigger an install.
        assertFalse(NimbusViewModel.shouldCompleteInstall(awaitingPermission = false, stillNeedsPermission = false))
        assertFalse(NimbusViewModel.shouldCompleteInstall(awaitingPermission = false, stillNeedsPermission = true))
    }

    @Test
    fun `pre-O devices with no permission model still install`() {
        // Below API 26 there is no unknown-sources toggle, so needsInstallPermission()
        // reports false and the pending install must complete on resume.
        assertTrue(NimbusViewModel.shouldCompleteInstall(awaitingPermission = true, stillNeedsPermission = false))
    }
}
