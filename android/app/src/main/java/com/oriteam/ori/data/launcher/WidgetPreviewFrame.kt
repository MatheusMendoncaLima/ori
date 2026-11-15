package com.oriteam.ori.data.launcher

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.annotation.RequiresApi

private const val TAG = "OriWidgets"

/** A preview is a widget that Android has not given an id: it is never a widget of the home screen. */
private const val NO_WIDGET_ID = -1

/**
 * The drawn preview of the widget [providerId]: the one its app generated (Android 15+, the widget as it really looks,
 * which One UI shows first), or else its preview layout (Android 12+) for a widget whose app gives no picture of it,
 * made the way Android makes a widget, without binding one. Made on the main thread and attached to the list like any
 * view, so a clock in it draws its hands; the icon stays under it, in case it draws nothing here. Null when there is
 * none or it could not be made.
 */
internal fun AndroidWidgetHost.previewViewOf(context: Context, providerId: String): View? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) previewLayoutViewOf(context, providerId) else null

@RequiresApi(Build.VERSION_CODES.S)
private fun AndroidWidgetHost.previewLayoutViewOf(context: Context, providerId: String): View? {
    val info = providerOf(providerId) ?: return null
    val hasGenerated = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
    val generated = if (hasGenerated) generatedPreviewOf(info) else null
    // A picture given by the app comes before its layout: the layout is drawn only for widgets without a picture.
    val layout = generated ?: info.takeIf { it.previewLayout != 0 && it.previewImage == 0 }
        ?.let { RemoteViews(it.provider.packageName, it.previewLayout) }
        ?: return null
    return runCatching { layoutOf(context, info, layout)?.let { WidgetPreviewFrame(context, it, sizeOf(info)) } }
        .onFailure { Log.w(TAG, "A widget preview could not be made: ${it.javaClass.simpleName}") }
        .getOrNull()
}

/** Whether the app of the widget generated a preview of it for the home screen (Android 15+). */
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
internal fun AppWidgetProviderInfo.hasGeneratedPreview(): Boolean =
    generatedPreviewCategories and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN != 0

/** The preview the app generated (Android 15+); null without one, or when Android does not give it. */
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
private fun AndroidWidgetHost.generatedPreviewOf(info: AppWidgetProviderInfo): RemoteViews? {
    if (!info.hasGeneratedPreview()) return null
    return runCatching {
        manager.getWidgetPreview(info.provider, info.profile, AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)
    }.onFailure { Log.w(TAG, "A generated widget preview was not given: ${it.javaClass.simpleName}") }.getOrNull()
}

/**
 * The preview layout made, or null when Android could not make it: some layouts use classes only their own home
 * screen has (the clock of Samsung uses ConstraintLayout), and Android then shows an empty error view.
 */
@RequiresApi(Build.VERSION_CODES.S)
private fun layoutOf(context: Context, info: AppWidgetProviderInfo, layout: RemoteViews): View? {
    val view = PreviewHostView(context)
    view.setAppWidget(NO_WIDGET_ID, info)
    view.updateAppWidget(layout)
    return view.takeUnless { it.hasFailed }
}

/** A widget view that tells when Android could not make its layout (it asks for the error view then). */
private class PreviewHostView(context: Context) : AppWidgetHostView(context) {
    var hasFailed = false
        private set

    override fun getErrorView(): View {
        hasFailed = true
        return super.getErrorView()
    }
}

/** The size the widget asks for, in pixels: what its layout is made for. */
private fun sizeOf(info: AppWidgetProviderInfo) = Size(maxOf(info.minWidth, 1), maxOf(info.minHeight, 1))

/**
 * Shows [preview] at the widget's own [size], shrunk to fit this view and centered, as a picture: touches and the
 * screen reader do not reach it (the row of the list says what the widget is).
 */
@SuppressLint("ViewConstructor")
internal class WidgetPreviewFrame(context: Context, private val preview: View, private val size: Size) :
    FrameLayout(context) {
    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        addView(preview, LayoutParams(size.width, size.height))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        preview.measure(exactly(size.width), exactly(size.height))
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec).coerceAtLeast(1),
            MeasureSpec.getSize(heightMeasureSpec).coerceAtLeast(1)
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val scale = minOf(width.toFloat() / size.width, height.toFloat() / size.height)
        preview.pivotX = 0f
        preview.pivotY = 0f
        preview.scaleX = scale
        preview.scaleY = scale
        preview.translationX = (width - size.width * scale) / 2
        preview.translationY = (height - size.height * scale) / 2
        preview.layout(0, 0, size.width, size.height)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean = false
}

private fun exactly(size: Int) = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)
