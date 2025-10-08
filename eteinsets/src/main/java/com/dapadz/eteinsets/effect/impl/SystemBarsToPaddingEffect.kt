package com.dapadz.eteinsets.effect.impl

import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.dapadz.eteinsets.effect.core.AnimatedInsetEffect
import com.dapadz.eteinsets.dsl.InsetsSpec

/**
 * Эффект, который добавляет отступы системных панелей (`system bars`)
 * к `padding` для View.
 *
 * Этот эффект применяется однократно при первом получении отступов, чтобы избежать
 * многократного добавления `padding`. Он позволяет View "растянуться" под
 * системные панели (status bar, navigation bar), но при этом размещать свой
 * контент внутри безопасной области.
 *
 * @property addLeft Добавить левый отступ системной панели к `paddingLeft`.
 * @property addTop Добавить верхний отступ системной панели к `paddingTop`.
 * @property addRight Добавить правый отступ системной панели к `paddingRight`.
 * @property addBottom Добавить нижний отступ системной панели к `paddingBottom`.
 *
 * @see InsetsSpec.systemBarsPadding
 */
class SystemBarsToPaddingEffect(
    private val addLeft: Boolean = false,
    private val addTop: Boolean = false,
    private val addRight: Boolean = false,
    private val addBottom: Boolean = false
) : AnimatedInsetEffect() {

    /** Флаг, чтобы применить эффект только один раз. */
    private var applied: Boolean = false

    /**
     * При первом вызове добавляет отступы системных панелей к `padding` View.
     * Последующие вызовы игнорируются.
     */
    override fun onApplyWindowInsets(view: View, insets: WindowInsetsCompat) {
        super.onApplyWindowInsets(view, insets)
        if (applied) return

        val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(
            left = view.paddingLeft + if (addLeft) system.left else 0,
            top = view.paddingTop + if (addTop) system.top else 0,
            right = view.paddingRight + if (addRight) system.right else 0,
            bottom = view.paddingBottom + if (addBottom) system.bottom else 0
        )
        applied = true
    }
}