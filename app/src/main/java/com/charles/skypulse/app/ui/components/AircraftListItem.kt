package com.charles.skypulse.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.charles.skypulse.app.domain.model.Aircraft
import com.charles.skypulse.app.domain.model.AltitudeUnit
import com.charles.skypulse.app.domain.model.DistanceUnit
import com.charles.skypulse.app.domain.model.SpeedUnit
import com.charles.skypulse.app.domain.util.FormatUtils
import com.charles.skypulse.app.ui.theme.SkyColors
import com.charles.skypulse.app.ui.theme.SkyType

@Composable
fun AircraftListItem(
    aircraft: Aircraft,
    distanceUnit: DistanceUnit,
    altitudeUnit: AltitudeUnit,
    speedUnit: SpeedUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    accent: Color = SkyColors.PrimaryFixedDim,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SkyColors.SurfaceContainerLow.copy(alpha = 0.6f), shape)
            .border(1.dp, SkyColors.GlassStroke, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.18f), CircleShape)
                .border(1.dp, accent.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Flight,
                contentDescription = null,
                tint = accent,
                modifier = Modifier
                    .size(20.dp)
                    .rotate((aircraft.headingDegrees?.toFloat() ?: 0f) + 45f),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                aircraft.displayName,
                style = SkyType.TitleMd,
                color = SkyColors.TextHigh,
            )
            Text(
                subtitle ?: aircraft.typeCode ?: aircraft.originCountry ?: "Unknown type",
                style = SkyType.LabelSm,
                color = SkyColors.OnSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                FormatUtils.distanceFromNm(aircraft.distanceNm, distanceUnit),
                style = SkyType.DataLg,
                color = SkyColors.PrimaryFixedDim,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    FormatUtils.altitude(aircraft.altitudeFeet, altitudeUnit),
                    style = SkyType.LabelSm,
                    color = SkyColors.TextMed,
                )
                Text(
                    FormatUtils.speed(aircraft.speedKnots, speedUnit),
                    style = SkyType.LabelSm,
                    color = SkyColors.TextMed,
                )
            }
        }
    }
}
