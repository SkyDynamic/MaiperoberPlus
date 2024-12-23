package io.github.skydynamic.maiproberplus.ui.compose.scores.chuni

import android.icu.math.BigDecimal
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.skydynamic.maiproberplus.core.data.chuni.ChuniData
import io.github.skydynamic.maiproberplus.core.database.entity.ChuniScoreEntity
import io.github.skydynamic.maiproberplus.core.utils.NetworkImageRequestUtil
import io.github.skydynamic.maiproberplus.ui.compose.scores.common.LevelBox
import java.text.NumberFormat

@Composable
fun ChuniScoreDetailCard(
    modifier: Modifier,
    scoreDetail: ChuniScoreEntity,
    onClick: () -> Unit
) {
    val title = scoreDetail.title
    val level = scoreDetail.level
    val color = scoreDetail.diff.color
    val rating = scoreDetail.rating
    val id = if (scoreDetail.songId == -1) {
        ChuniData.getSongIdFromTitle(title)
    } else {
        scoreDetail.songId
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxSize(),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            AsyncImage(
                model = NetworkImageRequestUtil.getImageRequest(
                    "https://assets2.lxns.net/chunithm/jacket/$id.png"
                ),
                contentDescription = null,
                onError = { error ->
                    Log.e("Image", "Error loading image", error.result.throwable)
                },
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color.copy(alpha = 0.4f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        .height(25.dp)
                        .fillMaxWidth()
                        .background(color.copy(alpha = 0.8f))
                ) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 5.dp, end = 40.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 8.dp)
                    ,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = NumberFormat.getNumberInstance().format(scoreDetail.score),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Rating: ${
                                BigDecimal(rating.toDouble()).setScale(2, BigDecimal.ROUND_UP).toDouble()
                            }",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                    LevelBox(
                        level,
                        Modifier
                            .padding(end = 8.dp)
                    )
                }
            }
        }
    }
}