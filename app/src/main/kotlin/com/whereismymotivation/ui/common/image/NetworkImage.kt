package com.whereismymotivation.ui.common.image

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.whereismymotivation.R

@Composable
fun NetworkImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    @DrawableRes placeholderRes: Int = R.drawable.photo_architecture
) {
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        placeholder = painterResource(placeholderRes),
        modifier = modifier,
        contentScale = contentScale
    )
}