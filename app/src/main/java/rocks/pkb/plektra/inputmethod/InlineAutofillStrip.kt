package rocks.pkb.plektra.inputmethod

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import rocks.pkb.plektra.inputmethod.suggestions.ui.FullSuggestionsBar

/**
 * The password manager's chips in the middle of the suggestion bar, in place of the
 * suggestions. Each chip sits on a suggestion button's background; the strip scrolls when the
 * chips don't fit.
 */
class InlineAutofillStrip(private val context: Context) {
    var views: List<View> = emptyList()
        private set
    private var strip: HorizontalScrollView? = null

    val isShown: Boolean
        get() = strip?.parent != null

    fun set(chips: List<View>) {
        views = chips
    }

    /** Puts the chips in [bar] when [active], or takes them out. */
    fun render(bar: FullSuggestionsBar?, active: Boolean, chipColour: Int, buttonHeight: Int?) {
        val host = bar?.centerAccessoryHost() ?: return
        val scroller = strip
        if (!active || views.isEmpty()) {
            if (scroller?.parent != null) {
                (scroller.parent as? ViewGroup)?.removeView(scroller)
                bar.setCenterAccessoryActive(false)
            }
            return
        }
        val current = scroller ?: HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            // Chips centred in the bar while they fit, scrolling when they don't
            isFillViewport = true
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                },
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
            )
        }.also { strip = it }
        val row = current.getChildAt(0) as LinearLayout
        if (row.childCount != views.size ||
            views.withIndex().any { (i, v) -> (row.getChildAt(i) as? ViewGroup)?.getChildAt(0) !== v }
        ) {
            row.removeAllViews()
            val density = context.resources.displayMetrics.density
            val gap = (4 * density).toInt()
            views.forEach { chip ->
                (chip.parent as? ViewGroup)?.removeView(chip)
                // The size the chip was made at (wrap content measures it at 0 wide)
                val made = chip.layoutParams
                val width = made?.width?.takeIf { it > 0 } ?: ViewGroup.LayoutParams.WRAP_CONTENT
                val button = FrameLayout(context).apply {
                    background = GradientDrawable().apply {
                        cornerRadius = 7 * density
                        setColor(chipColour)
                    }
                    addView(chip, FrameLayout.LayoutParams(
                        width,
                        made?.height ?: ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER
                    ))
                }
                row.addView(button, LinearLayout.LayoutParams(
                    width, buttonHeight ?: made?.height?.takeIf { it > 0 } ?: ViewGroup.LayoutParams.MATCH_PARENT
                ).apply { marginStart = gap; marginEnd = gap; gravity = Gravity.CENTER_VERTICAL })
            }
        }
        if (current.parent !== host) {
            (current.parent as? ViewGroup)?.removeView(current)
            host.removeAllViews()
            host.addView(current, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        bar.setCenterAccessoryActive(true)
    }

    /** Empties the strip and takes it out of [bar]. */
    fun clear(bar: FullSuggestionsBar?) {
        views = emptyList()
        strip?.let { s ->
            (s.getChildAt(0) as? ViewGroup)?.removeAllViews()
            if (s.parent != null) {
                (s.parent as? ViewGroup)?.removeView(s)
                bar?.setCenterAccessoryActive(false)
            }
        }
    }
}
