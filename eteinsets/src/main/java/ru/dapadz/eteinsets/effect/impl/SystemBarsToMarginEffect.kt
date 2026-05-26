package ru.dapadz.eteinsets.effect.impl

import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import ru.dapadz.eteinsets.dsl.InsetsSpec
import ru.dapadz.eteinsets.effect.core.AnimatedInsetEffect

/**
 * Эффект, который добавляет отступы системных панелей (`system bars`)
 * к `margin` у View.
 *
 * Эффект применяется один раз при первом получении [WindowInsetsCompat], чтобы
 * не накапливать одинаковые значения при повторной доставке отступов.
 *
 * Если [View.getLayoutParams] не поддерживают [ViewGroup.MarginLayoutParams],
 * эффект остаётся безопасным no-op.
 *
 * @property addLeft Добавить левый системный отступ к `leftMargin`.
 * @property addTop Добавить верхний системный отступ к `topMargin`.
 * @property addRight Добавить правый системный отступ к `rightMargin`.
 * @property addBottom Добавить нижний системный отступ к `bottomMargin`.
 *
 * @see InsetsSpec.systemBarsMargin
 */
class SystemBarsToMarginEffect(
    private val addLeft: Boolean = false,
    private val addTop: Boolean = false,
    private val addRight: Boolean = false,
    private val addBottom: Boolean = false
) : AnimatedInsetEffect() {

    /** Флаг, предотвращающий повторное добавление одинаковых margin-значений. */
    private var applied: Boolean = false

    /**
     * При первом вызове добавляет отступы системных панелей к `margin` View.
     */
    override fun onApplyWindowInsets(view: View, insets: WindowInsetsCompat) {
        super.onApplyWindowInsets(view, insets)
        if (applied) return

        val marginLayoutParams = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())

        marginLayoutParams.leftMargin += if (addLeft) system.left else 0
        marginLayoutParams.topMargin += if (addTop) system.top else 0
        marginLayoutParams.rightMargin += if (addRight) system.right else 0
        marginLayoutParams.bottomMargin += if (addBottom) system.bottom else 0
        view.layoutParams = marginLayoutParams

        applied = true
    }
}
