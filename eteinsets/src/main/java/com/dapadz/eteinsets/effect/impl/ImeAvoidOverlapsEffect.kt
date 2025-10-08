package com.dapadz.eteinsets.effect.impl

import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.dapadz.eteinsets.dispatcher.ImeInsetsDispatcher
import com.dapadz.eteinsets.effect.core.AnimatedInsetEffect
import com.dapadz.eteinsets.utils.imeHeight
import kotlin.math.max
import com.dapadz.eteinsets.dsl.InsetsSpec

/**
 * Эффект, который предотвращает перекрытие View клавиатурой (IME).
 *
 * Этот эффект смещает View вверх, чтобы оно оставалось видимым при появлении клавиатуры.
 * Смещение может применяться через `padding`, `margin` или `translationY`.
 * Эффект является анимированным и синхронизируется с анимацией появления/скрытия IME.
 *
 * @param applyMode Способ применения смещения ([ApplyMode]).
 * @param moveStrategy Стратегия, определяющая, когда следует смещать View ([MoveStrategy]).
 *
 * @see InsetsSpec.imeAvoidOverlaps
 */
class ImeAvoidOverlapsEffect(
    private val applyMode: ApplyMode,
    private val moveStrategy: MoveStrategy = MoveStrategy.ONLY_IF_OVERLAP
) : AnimatedInsetEffect() {

    /**
     * Способ применения смещения к View.
     * */
    enum class ApplyMode {
        /**
         * Смещение применяется через `paddingBottom`.
         * */
        PADDING_BOTTOM,

        /**
         * Смещение применяется через `marginBottom`.
         * */
        MARGIN_BOTTOM,

        /**
         * Смещение применяется через `translationY` (отрицательное значение).
         * */
        TRANSLATION_Y
    }

    /**
     * Стратегия, определяющая, когда следует смещать View.
     * */
    enum class MoveStrategy {
        /**
         * Смещать View всегда, когда клавиатура видима.
         * */
        ALWAYS,
        /**
         * Смещать View, только если клавиатура его перекрывает.
         * */
        ONLY_IF_OVERLAP
    }

    private val imeDispatcher: ImeInsetsDispatcher?
        get() = dispatcher as? ImeInsetsDispatcher

    /** Кэшированное расстояние от низа View до низа экрана в пикселях. */
    private var distanceFromScreenBottomPx: Int = 0

    /**
     * Сохраняет метрики View и применяет смещение, если клавиатура уже открыта.
     */
    override fun onApplyWindowInsets(view: View, insets: WindowInsetsCompat) {
        super.onApplyWindowInsets(view, insets)
        distanceFromScreenBottomPx = view.distanceFromScreenBottom()
        // Применяем смещение для статичных состояний (когда анимация не идет)
        when (imeDispatcher?.keyboardState) {
            ImeInsetsDispatcher.KeyboardState.OPEN -> applyOffset(insets.imeHeight())
            ImeInsetsDispatcher.KeyboardState.CLOSED -> applyOffset(0)
            else -> Unit // Во время анимации обработка идет в onProgress
        }
    }

    /**
     * Применяет смещение на каждом кадре анимации IME для плавного движения.
     */
    override fun onProgress(insets: WindowInsetsCompat, animations: List<WindowInsetsAnimationCompat>) {
        applyOffset(insets.imeHeight())
    }

    /**
     * Применяет финальное смещение по окончании анимации.
     */
    override fun onEnd(animation: WindowInsetsAnimationCompat) {
        // Устанавливаем конечное смещение на основе финальной высоты клавиатуры
        applyOffset(imeDispatcher?.keyboardHeightPx ?: 0)
    }

    /**
     * Рассчитывает и применяет необходимое смещение к [hostView].
     * @param rawImeHeightPx Текущая высота IME на данном кадре.
     */
    private fun applyOffset(rawImeHeightPx: Int) {
        val view = hostView ?: return
        val keyboardMaxPx = imeDispatcher?.keyboardHeightPx ?: 0

        val offsetPx = when (moveStrategy) {
            MoveStrategy.ALWAYS -> rawImeHeightPx
            MoveStrategy.ONLY_IF_OVERLAP -> {
                // Смещаем только на величину перекрытия
                (rawImeHeightPx - distanceFromScreenBottomPx).coerceIn(0, keyboardMaxPx)
            }
        }

        when (applyMode) {
            ApplyMode.PADDING_BOTTOM -> view.updatePadding(bottom = offsetPx)
            ApplyMode.MARGIN_BOTTOM -> view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                setMargins(marginStart, topMargin, marginEnd, offsetPx)
            }
            ApplyMode.TRANSLATION_Y -> view.translationY = -offsetPx.toFloat()
        }
    }

    /**
     * Рассчитывает расстояние от нижней границы View до нижней границы экрана.
     */
    private fun View.distanceFromScreenBottom(): Int {
        val screenHeight = resources.displayMetrics.heightPixels
        val location = IntArray(2).also { getLocationOnScreen(it) }
        val viewBottomOnScreen = location[1] + measuredHeight
        return max(0, screenHeight - viewBottomOnScreen)
    }
}