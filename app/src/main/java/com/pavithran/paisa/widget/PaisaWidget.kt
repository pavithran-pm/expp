package com.pavithran.paisa.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import com.pavithran.paisa.data.Dates
import com.pavithran.paisa.data.ExpenseRepository
import com.pavithran.paisa.data.Money
import com.pavithran.paisa.voice.VoiceCaptureActivity
import java.time.LocalDate

/** 2x1 widget: today's total, and one tap straight into listening. */
class PaisaWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val range = Dates.dayRange(LocalDate.now(Dates.zone))
        val total = runCatching {
            ExpenseRepository.from(context).totalBetween(range.first, range.last)
        }.getOrDefault(0.0)

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .cornerRadius(16.dp)
                        .background(GlanceTheme.colors.primaryContainer)
                        .clickable(actionStartActivity<VoiceCaptureActivity>()),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                ) {
                    Text(
                        text = "🎙  Log expense",
                        style = TextStyle(
                            fontWeight = FontWeight.Medium,
                            color = ColorProvider(Color.Black, Color.White)
                        )
                    )
                    Text(
                        text = "Today ${Money.format(total)}",
                        style = TextStyle(color = ColorProvider(Color.Black, Color.White))
                    )
                }
            }
        }
    }

    companion object {
        suspend fun refresh(context: Context) {
            PaisaWidget().updateAll(context)
        }
    }
}

class PaisaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PaisaWidget()
}
