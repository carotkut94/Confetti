@file:OptIn(ExperimentalHorologistApi::class)
@file:Suppress("UnstableApiUsage")

package dev.johnoreilly.confetti.wear

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.core.graphics.drawable.toDrawable
import coil.decode.DataSource
import coil.request.SuccessResult
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.auth.data.common.model.AuthUser
import com.google.android.horologist.compose.tools.coil.FakeImageLoader
import dev.johnoreilly.confetti.wear.preview.TestFixtures.JohnUrl
import dev.johnoreilly.confetti.wear.screenshots.BaseScreenshotTest
import dev.johnoreilly.confetti.wear.settings.SettingsListView
import dev.johnoreilly.confetti.wear.settings.SettingsUiState
import okio.Path.Companion.toPath
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

class SettingsScreenTest : BaseScreenshotTest(
    tolerance = 0.05f,
    record = false,
    a11yEnabled = true
) {

    @Before
    fun loadImages() {
        val johnBitmap = loadTestBitmap("john.jpg".toPath())


        val fakeImageLoader = FakeImageLoader {
            SuccessResult(
                drawable = johnBitmap.toDrawable(resources),
                dataSource = DataSource.MEMORY,
                request = it
            )
        }

        setImageLoader(fakeImageLoader)
    }

    @OptIn(ExperimentalHorologistApi::class)
    @Test
    fun loggedOutSettings() = screenshotTestRule.takeScrollableScreenshot(
        timeTextMode = TimeTextMode.OnTop,
        checks = {_, rule ->
            rule.onNodeWithText("Sign In").assertIsDisplayed()
        }
    ) { columnState ->
        SettingsListView(
            uiState = SettingsUiState.Success(null),
            conferenceCleared = { },
            navigateToGoogleSignIn = { },
            navigateToGoogleSignOut = { },
            columnState = columnState,
            onRefreshClick = {},
            onRefreshToken = {},
            onEnableDeveloperMode = {}
        )
    }

    @Test
    fun loggedInSettings() = screenshotTestRule.takeScrollableScreenshot(
        timeTextMode = TimeTextMode.OnTop,
        checks = { _, rule ->
            rule.onNodeWithContentDescription("Logged in as John O'Reilly")
                .assertHasClickAction()
                .assertIsDisplayed()
        }
    ) { columnState ->
        SettingsListView(
            uiState = SettingsUiState.Success(AuthUser("John O'Reilly", avatarUri = JohnUrl)),
            conferenceCleared = { },
            navigateToGoogleSignIn = { },
            navigateToGoogleSignOut = { },
            columnState = columnState,
            onRefreshClick = {},
            onRefreshToken = {},
            onEnableDeveloperMode = {}
        )
    }

    @OptIn(ExperimentalHorologistApi::class)
    @Test
    fun loggedInSettingsA11y() {
        assumeTrue(mobileTheme == null)

        // allow more tolerance as A11y tests are mainly for illustrating the
        // current observable behaviour
        //tolerance = 0.10f

        //snapshotTransformer = A11ySnapshotTransformer()

        screenshotTestRule.takeScrollableScreenshot(
            timeTextMode = TimeTextMode.OnTop,
            checks = { _, rule ->
                rule.onNodeWithContentDescription("Logged in as John O'Reilly")
                    .assertHasClickAction()
                    .assertIsDisplayed()
            }
        ) { columnState ->
            SettingsListView(
                uiState = SettingsUiState.Success(AuthUser("John O'Reilly", avatarUri = JohnUrl)),
                conferenceCleared = { },
                navigateToGoogleSignIn = { },
                navigateToGoogleSignOut = { },
                columnState = columnState,
                onRefreshClick = {},
                onRefreshToken = {},
                onEnableDeveloperMode = {}
            )
        }
    }
}