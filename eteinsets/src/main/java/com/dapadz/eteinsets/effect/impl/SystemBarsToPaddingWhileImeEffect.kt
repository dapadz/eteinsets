package com.dapadz.eteinsets.effect.impl

import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.dapadz.eteinsets.dispatcher.ImeInsetsDispatcher
import com.dapadz.eteinsets.effect.core.AnimatedInsetEffect
import com.dapadz.eteinsets.dsl.common.SystemBarsPaddingScope

/**
 * Эффект, который плавно убирает нижний отступ системной панели (`system navigation bar`)
 * из `padding` View, когда появляется клавиатура (IME).
 *
 * Этот эффект предназначен для работы в паре с [SystemBarsToPaddingEffect].
 * Он решает проблему двойного отступа снизу, когда клавиатура открыта:
 * один отступ от системной панели, второй — от самой клавиатуры.
 *
 * Эффект линейно интерполирует `paddingBottom` от `basePadding` до `basePadding - systemBarHeight`
 * синхронно с анимацией IME.
 *
 * @see SystemBarsPaddingScope.hideWhenIme
 */
class SystemBarsToPaddingWhileImeEffect : AnimatedInsetEffect() {

    private val imeDispatcher: ImeInsetsDispatcher?
        get() = dispatcher as? ImeInsetsDispatcher

    /**
     * Базовый нижний `padding` View, который уже включает отступ системной панели.
     * Захватывается при первом вызове `onApplyWindowInsets`.
     */
    private var baseBottomWithSystemPx: Int? = null

    /**
     * Максимальная высота IME, используемая для нормализации прогресса анимации.
     * Получается из `bounds` в [onStart].
     */
    private var imeMaxHeightPx: Float = 0f

    /**
     * При первом вызове захватывает базовый `paddingBottom`.
     * Важно, что этот колбэк должен вызываться *после* [SystemBarsToPaddingEffect],
     * чтобы захватить `padding` уже с добавленным системным отступом.
     */
    override fun onApplyWindowInsets(view: View, insets: WindowInsetsCompat) {
        super.onApplyWindowInsets(view, insets)
        // Захватываем базовый padding один раз.
        if (baseBottomWithSystemPx == null) {
            baseBottomWithSystemPx = view.paddingBottom
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
     * На каждом кадре анимации интерполирует `paddingBottom`, убирая вклад системной панели.
     */
    override fun onProgress(
        insets: WindowInsetsCompat,
        animations: List<WindowInsetsAnimationCompat>
    ) {
        val view = hostView ?: return
        val base = baseBottomWithSystemPx ?: view.paddingBottom.also { baseBottomWithSystemPx = it }

        val sysBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
        val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom.toFloat()

        // Знаменатель для нормализации прогресса анимации
        val denominator = when {
            imeMaxHeightPx > 0f -> imeMaxHeightPx
            (imeDispatcher?.keyboardHeightPx ?: 0) > 0 -> (imeDispatcher?.keyboardHeightPx ?: 0).toFloat()
            else -> 1f
        }
        val fraction = (imeBottom / denominator).coerceIn(0f, 1f)

        // Линейно интерполируем padding: от `base` до `base - sysBottom`
        val targetBottom = (base - sysBottom * fraction).toInt()
        view.updatePadding(bottom = targetBottom)
    }

    /**
     * По окончании анимации устанавливает финальный `paddingBottom`.
     */
    override fun onEnd(animation: WindowInsetsAnimationCompat) {
        val view = hostView ?: return
        val base = baseBottomWithSystemPx ?: view.paddingBottom
        val sys = imeDispatcher?.lastInsets?.getInsets(WindowInsetsCompat.Type.systemBars()) ?: Insets.NONE
        val isImeOpen = (imeDispatcher?.keyboardHeightPx ?: 0) > 0

        val finalBottom = if (isImeOpen) {
            base - sys.bottom // IME открыт, убираем системный отступ
        } else {
            base              // IME закрыт, возвращаем базовый отступ
        }
        view.updatePadding(bottom = finalBottom)
    }
}