package com.dapadz.eteinsets.effect.impl

import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import com.dapadz.eteinsets.effect.core.AnimatedInsetEffect
import com.dapadz.eteinsets.dsl.InsetsSpec

/**
 * Эффект, который удерживает View по центру видимой области экрана,
 * когда появляется клавиатура (IME).
 *
 * Этот эффект плавно изменяет `translationY` для View так, чтобы его центр
 * совпадал с центром пространства между верхом экрана и верхом клавиатуры.
 *
 * @see InsetsSpec.keepCenteredUnderIme
 */
class KeepCenteredUnderImeEffect : AnimatedInsetEffect() {

    /** Кэшированная высота экрана в пикселях. */
    private var screenHeightPx: Int = 0
    /** Кэшированная начальная позиция центра View по оси Y на экране. */
    private var initialViewCenterY: Float = 0f

    /**
     * Перед началом анимации кэширует начальные метрики:
     * высоту экрана и положение центра View.
     */
    override fun onPrepare(animation: WindowInsetsAnimationCompat) {
        val view = hostView ?: return
        val location = IntArray(2).also { view.getLocationOnScreen(it) }
        initialViewCenterY = location[1] + view.measuredHeight / 2f
        screenHeightPx = view.resources.displayMetrics.heightPixels
    }

    /**
     * На каждом кадре анимации пересчитывает и применяет `translationY`
     * для удержания View по центру доступной области.
     */
    override fun onProgress(insets: WindowInsetsCompat, animations: List<WindowInsetsAnimationCompat>) {
        val view = hostView ?: return
        val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom.toFloat()

        // Центр видимой области = (Высота экрана - Высота IME) / 2
        val newCenterY = (screenHeightPx - imeHeight) / 2f

        // Смещаем View на разницу между новым и старым центром
        view.translationY = newCenterY - initialViewCenterY
    }
}