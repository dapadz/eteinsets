package ru.dapadz.eteinsets.effect.impl

import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import ru.dapadz.eteinsets.dispatcher.ImeInsetsDispatcher
import ru.dapadz.eteinsets.dsl.common.SystemBarsInsetApplyMode
import ru.dapadz.eteinsets.effect.core.AnimatedInsetEffect
import ru.dapadz.eteinsets.dsl.common.SystemBarsInsetScope

/**
 * Эффект, который плавно убирает нижний отступ системной панели (`system navigation bar`)
 * из `padding` или `margin` View, когда появляется клавиатура (IME).
 *
 * Этот эффект предназначен для работы в паре с [SystemBarsToPaddingEffect] или
 * [SystemBarsToMarginEffect].
 * Он решает проблему двойного отступа снизу, когда клавиатура открыта:
 * один отступ от системной панели, второй — от самой клавиатуры.
 *
 * Эффект линейно интерполирует нижний inset у View от базового значения до
 * `baseBottomInset - systemBarHeight` синхронно с анимацией IME.
 *
 * @property applyMode Способ обновления нижнего отступа View.
 *
 * @see SystemBarsInsetScope.hideWhenIme
 */
class SystemBarsToPaddingWhileImeEffect internal constructor(
    private val applyMode: SystemBarsInsetApplyMode = SystemBarsInsetApplyMode.PADDING
) : AnimatedInsetEffect() {

    private val imeDispatcher: ImeInsetsDispatcher?
        get() = dispatcher as? ImeInsetsDispatcher

    /**
     * Базовый нижний inset View, который уже включает отступ системной панели.
     * Захватывается при первом вызове `onApplyWindowInsets`.
     */
    private var baseBottomWithSystemPx: Int? = null

    /**
     * Максимальная высота IME, используемая для нормализации прогресса анимации.
     * Получается из `bounds` в [onStart].
     */
    private var imeMaxHeightPx: Float = 0f

    /**
     * При первом вызове захватывает базовый нижний inset (`paddingBottom` или
     * `bottomMargin` в зависимости от [applyMode]).
     *
     * Важно, что этот колбэк должен вызываться *после* базового эффекта системных
     * панелей, чтобы захватить уже обновлённое значение.
     */
    override fun onApplyWindowInsets(view: View, insets: WindowInsetsCompat) {
        super.onApplyWindowInsets(view, insets)
        if (baseBottomWithSystemPx == null) {
            baseBottomWithSystemPx = view.captureBottomInset()
        }
    }

    /**
     * В начале анимации захватывает максимальную высоту IME для корректной нормализации.
     */
    override fun onStart(
        animation: WindowInsetsAnimationCompat,
        bounds: WindowInsetsAnimationCompat.BoundsCompat
    ) {
        if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
            // Верхняя граница анимации IME даёт стабильную максимальную высоту
            // как при открытии, так и при закрытии.
            imeMaxHeightPx = bounds.upperBound.bottom.toFloat()
        }
    }

    /**
     * На каждом кадре анимации интерполирует нижний inset View,
     * убирая вклад системной панели.
     */
    override fun onProgress(
        insets: WindowInsetsCompat,
        animations: List<WindowInsetsAnimationCompat>
    ) {
        val view = hostView ?: return
        val base = baseBottomWithSystemPx
            ?: view.captureBottomInset()?.also { baseBottomWithSystemPx = it }
            ?: return

        val sysBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
        val imeBottom = (imeDispatcher?.keyboardHeightPx
            ?: insets.getInsets(WindowInsetsCompat.Type.ime()).bottom).toFloat()

        val denominator = when {
            imeMaxHeightPx > 0f -> imeMaxHeightPx
            (imeDispatcher?.keyboardHeightPx ?: 0) > 0 -> (imeDispatcher?.keyboardHeightPx ?: 0).toFloat()
            else -> 1f
        }
        val fraction = (imeBottom / denominator).coerceIn(0f, 1f)

        val targetBottom = (base - sysBottom * fraction).toInt().coerceAtLeast(0)
        view.applyBottomInset(targetBottom)
    }

    /**
     * По окончании анимации устанавливает финальный нижний inset.
     */
    override fun onEnd(animation: WindowInsetsAnimationCompat) {
        val view = hostView ?: return
        val base = baseBottomWithSystemPx ?: view.captureBottomInset() ?: return
        val sys = imeDispatcher?.lastInsets?.getInsets(WindowInsetsCompat.Type.systemBars()) ?: Insets.NONE
        val isImeOpen = (imeDispatcher?.keyboardHeightPx ?: 0) > 0

        val finalBottom = if (isImeOpen) {
            (base - sys.bottom).coerceAtLeast(0)
        } else {
            base
        }
        view.applyBottomInset(finalBottom)
    }

    /**
     * Читает текущее нижнее значение, которое управляется эффектом.
     */
    private fun View.captureBottomInset(): Int? = when (applyMode) {
        SystemBarsInsetApplyMode.PADDING -> paddingBottom
        SystemBarsInsetApplyMode.MARGIN -> (layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin
    }

    /**
     * Применяет рассчитанное нижнее значение к View в зависимости от [applyMode].
     */
    private fun View.applyBottomInset(bottomInsetPx: Int) {
        when (applyMode) {
            SystemBarsInsetApplyMode.PADDING -> updatePadding(bottom = bottomInsetPx)
            SystemBarsInsetApplyMode.MARGIN -> updateBottomMargin(bottomInsetPx)
        }
    }

    /**
     * Безопасно обновляет `bottomMargin`, если layout params поддерживают margin.
     */
    private fun View.updateBottomMargin(bottomInsetPx: Int) {
        val marginLayoutParams = layoutParams as? ViewGroup.MarginLayoutParams ?: return
        if (marginLayoutParams.bottomMargin == bottomInsetPx) return

        marginLayoutParams.bottomMargin = bottomInsetPx
        layoutParams = marginLayoutParams
    }
}
