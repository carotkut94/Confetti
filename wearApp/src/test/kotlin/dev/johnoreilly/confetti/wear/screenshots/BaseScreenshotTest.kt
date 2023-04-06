package dev.johnoreilly.confetti.wear.screenshots

import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.scrollAway
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import com.google.android.horologist.compose.tools.coil.FakeImageLoader
import dev.johnoreilly.confetti.screenshot.RNGScreenshotTestRule
import dev.johnoreilly.confetti.wear.FixedTimeSource
import dev.johnoreilly.confetti.wear.app.KoinTestApp
import dev.johnoreilly.confetti.wear.preview.TestFixtures
import dev.johnoreilly.confetti.wear.proto.Theme
import dev.johnoreilly.confetti.wear.ui.ConfettiTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okio.FileSystem
import okio.Path
import org.junit.Rule
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.AfterTest

@RunWith(RobolectricTestRunner::class)
@Config(
    application = KoinTestApp::class,
    sdk = [30],
    qualifiers = "w221dp-h221dp-small-notlong-round-watch-xhdpi-keyshidden-nonav"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class BaseScreenshotTest(
    record: Boolean,
    tolerance: Float = 0.01f,
    a11yEnabled: Boolean = false
): KoinTest {

    private var fakeImageLoader = FakeImageLoader.Never
    var mobileTheme: Theme? = null

    private val applicationContext: Context
        get() = ApplicationProvider.getApplicationContext<Application>()

    val resources: Resources
        get() = applicationContext.resources


    fun setImageLoader(imageLoader: FakeImageLoader) {
        this.fakeImageLoader = imageLoader
    }

    fun setTheme(theme: Theme) {
        mobileTheme = theme
    }

    @get:Rule
    val screenshotTestRule = createScreenshotTestRule(
        tolerance = tolerance,
        record = record,
        a11yEnabled = a11yEnabled,
        imageLoader = fakeImageLoader,
        theme = mobileTheme,
        resources = resources
    )


    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "Colors: {0}")
        fun params() = listOf(
            arrayOf("Material", null),
            arrayOf("MobileDefault", TestFixtures.MobileTheme),
            arrayOf("MobileAndroid", TestFixtures.AndroidTheme),
        )

        fun loadTestBitmap(path: Path): Bitmap = FileSystem.RESOURCES.read(path) {
            BitmapFactory.decodeStream(this.inputStream())
        }
    }

    enum class TimeTextMode {
        OnTop,
        Off,
        Scrolling
    }

    @AfterTest
    fun teardown() {
        stopKoin()
    }
}

private fun createScreenshotTestRule(
    record: Boolean,
    tolerance: Float = 0.1f,
    a11yEnabled: Boolean = false,
    imageLoader: FakeImageLoader,
    resources: Resources,
    theme: Theme?,
): ScreenshotTestRule {
    return ScreenshotTestRule(record, tolerance, a11yEnabled, theme, imageLoader, resources)
}

class ScreenshotTestRule(
    record: Boolean,
    tolerance: Float,
    a11yEnabled: Boolean,
    private val mobileTheme: Theme? = null,
    private val fakeImageLoader: FakeImageLoader,
    private val resources: Resources,
) : RNGScreenshotTestRule(record, tolerance, a11yEnabled) {

    @Composable
    fun FakeImageLoader.apply(content: @Composable () -> Unit) {
        // Not sure why this is needed, but Coil has improved
        // test support in next release
        this.override {
            CompositionLocalProvider(coil.compose.LocalImageLoader provides this) {
                content()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun takeWearScreenshot(
        round: Boolean = resources.configuration.isScreenRound,
        timeText: @Composable () -> Unit = {
            TimeText(
                timeSource = FixedTimeSource
            )
        },
        positionIndicator: @Composable () -> Unit = {
        },
        checks: suspend (composeTestRule: ComposeContentTestRule) -> Unit = {},
        content: @Composable () -> Unit
    ) {

        takeScreenshot(checks = checks) {
            fakeImageLoader.apply {
                Box(
                    modifier = Modifier
                        .background(Color.Transparent)
                ) {
                    ConfettiTheme(mobileTheme = mobileTheme) {
                        Scaffold(
                            modifier = Modifier
                                .fillMaxSize()
                                .run {
                                    if (round) {
                                        clip(CircleShape)
                                    } else {
                                        this
                                    }
                                }
                                .background(Color.Black),
                            timeText = {
                                timeText()
                            },
                            positionIndicator = positionIndicator
                        ) {
                            content()
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun takeComponentScreenshot(
        checks: suspend (composeRule: ComposeContentTestRule) -> Unit = {},
        content: @Composable BoxScope.() -> Unit
    ) {
        takeScreenshot(checks) {
            fakeImageLoader.override {
                Box(
                    modifier = Modifier
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    ConfettiTheme(mobileTheme = mobileTheme) {
                        content()
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalHorologistApi::class)
    fun takeScrollableScreenshot(
        round: Boolean = resources.configuration.isScreenRound,
        timeTextMode: BaseScreenshotTest.TimeTextMode,
        columnStateFactory: ScalingLazyColumnState.Factory = ScalingLazyColumnDefaults.belowTimeText(),
        checks: suspend (columnState: ScalingLazyColumnState, composeRule: ComposeContentTestRule) -> Unit = { _, _ -> },
        content: @Composable (columnState: ScalingLazyColumnState) -> Unit
    ) {
        lateinit var columnState: ScalingLazyColumnState

        takeWearScreenshot(
            round,
            timeText = {
                if (timeTextMode != BaseScreenshotTest.TimeTextMode.Off) {
                    TimeText(
                        timeSource = FixedTimeSource,
                        modifier = if (timeTextMode == BaseScreenshotTest.TimeTextMode.Scrolling)
                            Modifier.scrollAway(columnState.state)
                        else
                            Modifier
                    )
                }
            },
            positionIndicator = {
                PositionIndicator(scalingLazyListState = columnState.state)
            },
            checks = {
                checks(columnState, it)
            }
        ) {
            columnState = columnStateFactory.create()

            content(columnState)
        }
    }

}
