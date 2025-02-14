/*
 * Copyright (C) 2018 The Tachiyomi Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package tachiyomi.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import tachiyomi.domain.library.model.CategoryWithCount
import tachiyomi.domain.library.model.DisplayMode
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.ui.R
import tachiyomi.ui.categories.visibleName
import tachiyomi.ui.core.components.Toolbar
import tachiyomi.ui.core.theme.CustomColors
import tachiyomi.ui.core.viewmodel.viewModel
import tachiyomi.ui.main.Route

@OptIn(ExperimentalMaterialApi::class, ExperimentalPagerApi::class)
@Composable
fun LibraryScreen(
  navController: NavController,
  requestHideBottomNav: (Boolean) -> Unit
) {
  val vm = viewModel<LibraryViewModel>()
  val scope = rememberCoroutineScope()
  val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
  val pagerState = rememberPagerState(vm.categories.size, vm.selectedCategoryIndex)

  LaunchedEffect(pagerState) {
    snapshotFlow { pagerState.currentPage }.collect {
      vm.setSelectedPage(it)
    }
  }
  LaunchedEffect(vm.selectedManga.size, sheetState.targetValue) {
    requestHideBottomNav(
      vm.selectedManga.isNotEmpty() || sheetState.targetValue != ModalBottomSheetValue.Hidden
    )
  }

  // TODO(inorichi): a modal bottom sheet does not work very well with bottom navigation. We'll
  //  probably need a custom implementation that draws over the whole screen
  ModalBottomSheetLayout(
    sheetState = sheetState,
    sheetContent = { LibrarySheet() }
  ) {
    Column {
      LibraryToolbar(
        selectedCategory = vm.selectedCategory,
        selectedManga = vm.selectedManga,
        showCategoryTabs = vm.showCategoryTabs,
        showCountInCategory = vm.showCountInCategory,
        searchMode = vm.searchMode,
        searchQuery = vm.searchQuery,
        onClickSearch = { vm.openSearch() },
        onClickFilter = { scope.launch { sheetState.show() } },
        onClickRefresh = { vm.updateLibrary() },
        onClickCloseSelection = { vm.unselectAll() },
        onClickCloseSearch = { vm.closeSearch() },
        onClickSelectAll = { vm.selectAllInCurrentCategory() },
        onClickUnselectAll = { vm.flipAllInCurrentCategory() },
        onChangeSearchQuery = { vm.updateQuery(it) }
      )
      LibraryTabs(
        state = pagerState,
        visible = vm.showCategoryTabs,
        categories = vm.categories,
        showCount = vm.showCountInCategory,
        onClickTab = { scope.launch { pagerState.animateScrollToPage(it) } }
      )
      LibraryPager(
        state = pagerState,
        categories = vm.categories,
        displayMode = vm.displayMode,
        selectedManga = vm.selectedManga,
        getLibraryForPage = { vm.getLibraryForCategoryIndex(it) },
        onClickManga = { manga ->
          if (vm.selectedManga.isEmpty()) {
            navController.navigate("${Route.LibraryManga.id}/${manga.id}")
          } else {
            vm.toggleManga(manga)
          }
        },
        onLongClickManga = { vm.toggleManga(it) }
      )
    }
  }
}

@Composable
private fun LibraryToolbar(
  selectedCategory: CategoryWithCount?,
  selectedManga: List<Long>,
  showCategoryTabs: Boolean,
  showCountInCategory: Boolean,
  searchMode: Boolean,
  searchQuery: String,
  onClickSearch: () -> Unit,
  onClickFilter: () -> Unit,
  onClickRefresh: () -> Unit,
  onClickCloseSelection: () -> Unit,
  onClickCloseSearch: () -> Unit,
  onClickSelectAll: () -> Unit,
  onClickUnselectAll: () -> Unit,
  onChangeSearchQuery: (String) -> Unit
) {
  if (searchMode) {
    // Search toolbar
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Toolbar(
      title = {
        BasicTextField(
          searchQuery,
          onChangeSearchQuery,
          modifier = Modifier.focusRequester(focusRequester),
          textStyle = LocalTextStyle.current.copy(color = LocalContentColor.current),
          cursorBrush = SolidColor(LocalContentColor.current),
          singleLine = true,
          keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
      },
      navigationIcon = {
        IconButton(onClick = onClickCloseSearch) {
          Icon(Icons.Default.ArrowBack, contentDescription = null)
        }
      },
      actions = {
        IconButton(onClick = { onChangeSearchQuery("") }) {
          Icon(Icons.Default.Close, contentDescription = null)
        }
        IconButton(onClick = {
          onClickFilter()
          focusManager.clearFocus()
        }) {
          Icon(Icons.Default.FilterList, contentDescription = null)
        }
      }
    )
    LaunchedEffect(focusRequester) {
      focusRequester.requestFocus()
    }
    BackHandler(onBack = onClickCloseSearch)
  } else if (selectedManga.isEmpty()) {
    // Regular toolbar
    Toolbar(
      title = {
        val text = when {
          showCategoryTabs -> stringResource(R.string.library_label)
          selectedCategory != null -> selectedCategory.visibleName + if (!showCountInCategory) {
            ""
          } else {
            " (${selectedCategory.mangaCount})"
          }
          else -> ""
        }
        Text(text)
      },
      actions = {
        IconButton(onClick = onClickSearch) {
          Icon(Icons.Default.Search, contentDescription = null)
        }
        IconButton(onClick = onClickFilter) {
          Icon(Icons.Default.FilterList, contentDescription = null)
        }
        IconButton(onClick = onClickRefresh) {
          Icon(Icons.Default.Refresh, contentDescription = null)
        }
      }
    )
  } else {
    // Selection toolbar
    Toolbar(
      title = { Text("${selectedManga.size}") },
      navigationIcon = {
        IconButton(onClick = onClickCloseSelection) {
          Icon(Icons.Default.Close, contentDescription = null)
        }
      },
      actions = {
        IconButton(onClick = onClickSelectAll) {
          Icon(Icons.Default.SelectAll, contentDescription = null)
        }
        IconButton(onClick = onClickUnselectAll) {
          Icon(Icons.Default.FlipToBack, contentDescription = null)
        }
      }
    )
    BackHandler(onBack = onClickCloseSelection)
  }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalPagerApi::class)
@Composable
private fun LibraryTabs(
  state: PagerState,
  visible: Boolean,
  categories: List<CategoryWithCount>,
  showCount: Boolean,
  onClickTab: (Int) -> Unit
) {
  if (categories.isEmpty()) return

  AnimatedVisibility(
    visible = visible,
    enter = expandVertically(),
    exit = shrinkVertically()
  ) {
    ScrollableTabRow(
      selectedTabIndex = state.currentPage,
      backgroundColor = CustomColors.current.bars,
      contentColor = CustomColors.current.onBars,
      edgePadding = 0.dp,
      indicator = { TabRowDefaults.Indicator(Modifier.pagerTabIndicatorOffset(state, it)) }
    ) {
      categories.forEachIndexed { i, category ->
        Tab(
          selected = state.currentPage == i,
          onClick = { onClickTab(i) },
          text = {
            Text(
              category.visibleName + if (!showCount) {
                ""
              } else {
                " (${category.mangaCount})"
              }
            )
          }
        )
      }
    }
  }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun LibraryPager(
  state: PagerState,
  categories: List<CategoryWithCount>,
  displayMode: DisplayMode,
  selectedManga: List<Long>,
  getLibraryForPage: @Composable (Int) -> State<List<LibraryManga>>,
  onClickManga: (LibraryManga) -> Unit,
  onLongClickManga: (LibraryManga) -> Unit
) {
  if (categories.isEmpty()) return

  HorizontalPager(state = state) { page ->
    val library by getLibraryForPage(page)
    when (displayMode) {
      DisplayMode.CompactGrid -> LibraryMangaCompactGrid(
        library = library,
        selectedManga = selectedManga,
        onClickManga = onClickManga,
        onLongClickManga = onLongClickManga
      )
      DisplayMode.ComfortableGrid -> LibraryMangaComfortableGrid(
        library = library,
        selectedManga = selectedManga,
        onClickManga = onClickManga,
        onLongClickManga = onLongClickManga
      )
      DisplayMode.List -> LibraryMangaList(
        library = library,
        selectedManga = selectedManga,
        onClickManga = onClickManga,
        onLongClickManga = onLongClickManga
      )
    }
  }
}
