/*
 * Copyright (C) 2018 The Tachiyomi Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package tachiyomi.ui.core.components.manga

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AmbientTextStyle
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradient
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.font
import androidx.compose.ui.text.font.fontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tachiyomi.ui.R
import tachiyomi.ui.core.coil.CoilImage
import tachiyomi.ui.core.coil.MangaCover

private val ptSansFont = fontFamily(font(R.font.ptsans_bold))

@Composable
fun MangaGridItem(
  title: String,
  cover: MangaCover,
  onClick: () -> Unit = {},
) {
  val fontStyle = AmbientTextStyle.current.merge(
    TextStyle(letterSpacing = 0.sp, fontFamily = ptSansFont, fontSize = 14.sp)
  )

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .aspectRatio(3f / 4f)
      .padding(4.dp)
      .clickable(onClick = onClick),
    elevation = 4.dp,
    shape = RoundedCornerShape(4.dp)
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      CoilImage(model = cover)
      Box(modifier = Modifier.fillMaxSize().then(shadowGradient))
      Text(
        text = title,
        color = Color.White,
        style = fontStyle,
        modifier = Modifier.wrapContentHeight(Alignment.CenterVertically)
          .align(Alignment.BottomStart)
          .padding(8.dp)
      )
    }
  }
}

private val shadowGradient = Modifier.drawWithCache {
  val gradient = LinearGradient(
    0.75f to Color.Transparent,
    1.0f to Color(0xAA000000),
    startX = 0f,
    startY = 0f,
    endX = 0f,
    endY = size.height
  )
  onDrawBehind {
    drawRect(gradient)
  }
}
