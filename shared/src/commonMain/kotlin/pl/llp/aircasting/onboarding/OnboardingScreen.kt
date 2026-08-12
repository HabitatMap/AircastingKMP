package pl.llp.aircasting.onboarding

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_aircasting_wordmark
import aircasting.shared.generated.resources.ic_arrow_forward
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import pl.llp.aircasting.i18n.LocalStrings

private val ScreenPadding = 24.dp
private val ImageSize = 288.dp
private val ImageToDots = 48.dp
private val DotSize = 8.dp
private val DotsToText = 24.dp

private val TextBlockHeight = 120.dp

private val PagerHeight = ImageSize + ImageToDots + DotSize + DotsToText + TextBlockHeight

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
  val strings = LocalStrings.current
  val pages = remember(strings) { onboardingPages(strings) }
  val pager = rememberPagerState { pages.size }
  val scope = rememberCoroutineScope()

  BackHandler(enabled = pager.currentPage > 0) {
    scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
  }

  Column(
    Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .statusBarsPadding()
      .navigationBarsPadding()
      .verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
      Icon(
        painterResource(Res.drawable.ic_aircasting_wordmark),
        contentDescription = strings.appLogo,
        tint = Color.Unspecified,
      )
    }
    Spacer(Modifier.height(64.dp))
    OnboardingPager(pages, pager)
    Spacer(Modifier.height(67.dp))
    OnboardingActions(
      onNext = {
        if (pager.currentPage == pages.lastIndex) onFinish()
        else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
      },
      onSkip = onFinish,
    )
    Spacer(Modifier.height(32.dp))
  }
}

@Composable
private fun OnboardingPager(pages: List<OnboardingPage>, pager: PagerState) {
  Box(Modifier.fillMaxWidth().height(PagerHeight)) {
    HorizontalPager(pager) { index ->
      val page = pages[index]
      Column(
        Modifier.fillMaxWidth().padding(horizontal = ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Image(painterResource(page.image), contentDescription = null, Modifier.size(ImageSize))
        Spacer(Modifier.height(ImageToDots + DotSize + DotsToText))
        Text(
          page.title,
          style = MaterialTheme.typography.headlineLargeEmphasized,
          color = MaterialTheme.colorScheme.onBackground,
          textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
          page.body,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }
    }
    PageIndicator(
      count = pages.size,
      selected = pager.currentPage,
      modifier = Modifier.align(Alignment.TopCenter).padding(top = ImageSize + ImageToDots),
    )
  }
}

@Composable
private fun PageIndicator(count: Int, selected: Int, modifier: Modifier = Modifier) {
  Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    repeat(count) { index ->
      val active = index == selected
      Box(
        Modifier
          .size(width = if (active) 24.dp else DotSize, height = DotSize)
          .background(
            if (active) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.outlineVariant,
            CircleShape,
          ),
      )
    }
  }
}

@Composable
private fun OnboardingActions(onNext: () -> Unit, onSkip: () -> Unit) {
  val strings = LocalStrings.current
  Column(
    Modifier.fillMaxWidth().padding(horizontal = ScreenPadding),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    OutlinedButton(
      onClick = onNext,
      modifier = Modifier.fillMaxWidth().height(56.dp),
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primaryContainer),
      colors = ButtonDefaults.outlinedButtonColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
      ),
    ) {
      Text(strings.onboardingNext, style = MaterialTheme.typography.titleMedium)
      Spacer(Modifier.width(8.dp))
      Icon(painterResource(Res.drawable.ic_arrow_forward), contentDescription = null, Modifier.size(24.dp))
    }
    Spacer(Modifier.height(24.dp))
    TextButton(onClick = onSkip) {
      Text(
        strings.onboardingSkip,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
    }
  }
}