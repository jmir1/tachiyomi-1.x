/*
 * Copyright (C) 2018 The Tachiyomi Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package tachiyomi.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.onCommit
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AnimationClockAmbient
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.navigate
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.library.model.Category
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.ui.R
import tachiyomi.ui.Route
import tachiyomi.ui.categories.visibleName
import tachiyomi.ui.core.coil.MangaCover
import tachiyomi.ui.core.components.AutofitGrid
import tachiyomi.ui.core.components.Pager
import tachiyomi.ui.core.components.PagerState
import tachiyomi.ui.core.components.Toolbar
import tachiyomi.ui.core.components.manga.MangaGridItem
import tachiyomi.ui.core.theme.CustomColors
import tachiyomi.ui.core.viewmodel.viewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LibraryScreen(navController: NavController) {
  val vm = viewModel<LibraryViewModel>()

  Column {
    Toolbar(title = { Text(stringResource(R.string.library_label)) })
    AnimatedVisibility(
      visible = vm.categories.isNotEmpty(),
      enter = expandVertically(),
      exit = shrinkVertically()
    ) {
      ScrollableTabRow(
        selectedTabIndex = vm.selectedCategoryIndex,
        backgroundColor = CustomColors.current.bars,
        contentColor = CustomColors.current.onBars,
        edgePadding = 0.dp
      ) {
        vm.categories.forEachIndexed { i, category ->
          Tab(
            selected = vm.selectedCategoryIndex == i,
            onClick = { vm.setSelectedCategory(category) },
            text = { Text(category.visibleName) }
          )
        }
      }
    }
    LibraryPager(
      categories = vm.categories,
      selectedIndex = vm.selectedCategoryIndex,
      getLibrary = { vm.getLibraryForCategoryIndex(it) },
      onClickManga = { navController.navigate("${Route.LibraryManga.id}/${it.id}") },
      onPageChanged = { vm.setSelectedPage(it) }
    )
  }
}

@Composable
private fun LibraryPager(
  categories: List<Category>,
  selectedIndex: Int,
  getLibrary: (Int) -> Flow<List<LibraryManga>>,
  onClickManga: (LibraryManga) -> Unit,
  onPageChanged: (Int) -> Unit
) {
  if (categories.isEmpty()) return

  val clock = AnimationClockAmbient.current
  val state = remember(categories.size, selectedIndex) {
    PagerState(
      clock = clock,
      currentPage = selectedIndex,
      minPage = 0,
      maxPage = categories.size
    )
  }
  onCommit(state.currentPage) {
    if (state.currentPage != selectedIndex) {
      onPageChanged(state.currentPage)
    }
  }
  Pager(state = state, offscreenLimit = 1) {
    val library by remember { getLibrary(page) }.collectAsState(initial = emptyList())
    LibraryGrid(
      library = library,
      onClickManga = onClickManga
    )
  }
}

@Composable
private fun LibraryGrid(
  library: List<LibraryManga>,
  onClickManga: (LibraryManga) -> Unit = {}
) {
  AutofitGrid(
    data = library,
    defaultColumnWidth = 160.dp,
    modifier = Modifier.fillMaxSize()
  ) { manga ->
    MangaGridItem(
      title = manga.title,
      cover = MangaCover.from(manga),
      onClick = { onClickManga(manga) }
    )
  }
}
