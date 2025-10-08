package com.dapadz.eteinsets.dispatcher

import android.view.View
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import com.dapadz.eteinsets.effect.core.InsetEffect
import com.dapadz.eteinsets.utils.imeHeight
import com.dapadz.eteinsets.dsl.insets

/**
 * Расширенный [InsetsDispatcher], который отслеживает состояние клавиатуры (IME).
 *
 * Этот диспетчер предоставляет высокоуровневую информацию о состоянии клавиатуры,
 * такую как её текущая высота и находится ли она в процессе открытия/закрытия.
 * Это основной компонент для сценариев, где поведение View зависит от клавиатуры:
 * - Подъём контента над клавиатурой.
 * - Плавное изменение отступов синхронно с анимацией IME.
 * - Центрирование элементов в доступной области экрана.
 *
 * Он автоматически регистрируется как [WindowInsetsAnimationCompat.Callback]
 * для точного отслеживания анимаций.
 *
 * @param effects Набор [InsetEffect], которые будут получать события от этого диспетчера.
 * @see InsetsDispatcher
 * @see insets
 */
open class ImeInsetsDispatcher(
    vararg effects: InsetEffect
) : InsetsDispatcher(
    dispatchMode = DISPATCH_MODE_CONTINUE_ON_SUBTREE,
    effects = effects
) {

    /**
     * Определяет высокоуровневые состояния клавиатуры (IME).
     */
    enum class KeyboardState {

        /**
         * Клавиатура открывается или меняет свою высоту.
         * Это состояние активно во время анимации.
         * */
        OPENING_OR_CHANGING,

        /**
         * Клавиатура полностью открыта и её высота стабильна.
         * */
        OPEN,

        /**
         * Клавиатура закрывается.
         * Это состояние активно во время анимации закрытия.
         * */
        CLOSING,

        /**
         * Клавиатура полностью закрыта.
         * */
        CLOSED,

        /**
         * Начальное или неопределённое состояние до первого события отступов.
         * */
        UNKNOWN
    }

    /**
     * Текущее высокоуровневое состояние клавиатуры.
     * Обновляется автоматически на основе событий [WindowInsetsCompat] и [WindowInsetsAnimationCompat].
     * @see KeyboardState
     */
    var keyboardState: KeyboardState = KeyboardState.UNKNOWN
        private set

    /**
     * Последний полученный объект [WindowInsetsCompat]. Может быть `null` до первого вызова `onApplyWindowInsets`.
     */
    var lastInsets: WindowInsetsCompat? = null
        private set

    /**
     * Текущая или последняя известная высота клавиатуры (IME) в пикселях.
     * Это значение обновляется на каждом кадре анимации.
     */
    var keyboardHeightPx: Int = 0
        private set

    /**
     * Флаг, указывающий, выполняется ли в данный момент анимация клавиатуры.
     */
    var isImeAnimationRunning: Boolean = false
        private set

    /**
     * Применяет оконные отступы, обновляя состояние клавиатуры на основе изменений.
     *
     * Этот метод является основной точкой для определения состояния IME, когда анимация неактивна.
     * Он анализирует высоту IME и сравнивает её с предыдущим значением для определения
     * состояний [KeyboardState.OPEN] и [KeyboardState.CLOSED].
     *
     * Во время анимации управление состоянием передаётся колбэкам `onProgress`.
     */
    override fun onApplyWindowInsets(view: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        lastInsets = insets
        val imeHeight = insets.imeHeight()

        // Логика определения состояния разделена:
        // 1. Если анимация НЕ идёт, мы находимся в статичном состоянии (открыто/закрыто).
        // 2. Если анимация идёт, состояние определяется в onProgress/onStart.
        if (!isImeAnimationRunning) {
            keyboardState = when {
                imeHeight > 0 && keyboardHeightPx == imeHeight -> KeyboardState.OPEN
                imeHeight == 0 -> KeyboardState.CLOSED
                else -> keyboardState // Сохраняем текущее состояние, если нет изменений
            }
        } else {
            // Во время анимации определяем направление движения
            keyboardState = when {
                imeHeight > keyboardHeightPx -> KeyboardState.OPENING_OR_CHANGING
                imeHeight < keyboardHeightPx -> KeyboardState.CLOSING
                else -> keyboardState
            }
        }
        keyboardHeightPx = imeHeight
        return super.onApplyWindowInsets(view, insets)
    }

    /**
     * Вызывается перед началом анимации отступов.
     * Используется для установки флага [isImeAnimationRunning].
     */
    override fun onPrepare(animation: WindowInsetsAnimationCompat) {
        if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
            isImeAnimationRunning = true
        }
        super.onPrepare(animation)
    }

    /**
     * Вызывается в начале анимации отступов.
     * Устанавливает флаг [isImeAnimationRunning]
     */
    override fun onStart(
        animation: WindowInsetsAnimationCompat,
        bounds: WindowInsetsAnimationCompat.BoundsCompat
    ): WindowInsetsAnimationCompat.BoundsCompat {
        isImeAnimationRunning = true // Явное указание на старт
        return super.onStart(animation, bounds)
    }

    /**
     * Вызывается по завершении анимации отступов.
     * Сбрасывает флаг [isImeAnimationRunning] и устанавливает финальное состояние клавиатуры.
     */
    override fun onEnd(animation: WindowInsetsAnimationCompat) {
        if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
            isImeAnimationRunning = false
            // Устанавливаем конечное состояние после завершения анимации
            keyboardState = if (keyboardHeightPx > 0) KeyboardState.OPEN else KeyboardState.CLOSED
        }
        super.onEnd(animation)
    }
}