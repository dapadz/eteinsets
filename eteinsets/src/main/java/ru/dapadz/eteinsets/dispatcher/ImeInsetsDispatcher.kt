package ru.dapadz.eteinsets.dispatcher

import android.view.View
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import ru.dapadz.eteinsets.effect.core.InsetEffect
import ru.dapadz.eteinsets.dsl.insets
import ru.dapadz.eteinsets.utils.imeHeight
import kotlin.math.roundToInt

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
     * Последняя гарантированно ненулевая высота клавиатуры.
     *
     * Нужна как fallback для системных анимаций, где платформа временно может
     * отдавать `ime = 0`, хотя жест закрытия клавиатуры ещё продолжается.
     */
    private var lastVisibleKeyboardHeightPx: Int = 0

    /**
     * Нижняя граница текущей IME-анимации.
     */
    private var imeAnimationLowerBoundPx: Int = 0

    /**
     * Верхняя граница текущей IME-анимации.
     */
    private var imeAnimationUpperBoundPx: Int = 0

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
        val previousKeyboardHeightPx = keyboardHeightPx

        if (imeHeight > 0) {
            lastVisibleKeyboardHeightPx = imeHeight
        }

        // Логика определения состояния разделена:
        // 1. Если анимация НЕ идёт, мы находимся в статичном состоянии (открыто/закрыто).
        // 2. Если анимация идёт, состояние определяется в onProgress/onStart.
        if (!isImeAnimationRunning) {
            keyboardHeightPx = imeHeight
            keyboardState = if (imeHeight > 0) KeyboardState.OPEN else KeyboardState.CLOSED
        } else {
            // Во время анимации определяем направление движения
            keyboardState = when {
                imeHeight > previousKeyboardHeightPx -> KeyboardState.OPENING_OR_CHANGING
                imeHeight < previousKeyboardHeightPx -> KeyboardState.CLOSING
                imeHeight == 0 && previousKeyboardHeightPx > 0 -> KeyboardState.CLOSING
                else -> keyboardState
            }

            // Во время predictive back система может сразу отдать imeHeight = 0.
            // В таком случае сохраняем последнее корректное значение до onProgress,
            // где восстановим эффективную высоту по progress анимации.
            if (imeHeight > 0) {
                keyboardHeightPx = imeHeight
            }
        }
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
        if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
            isImeAnimationRunning = true
            imeAnimationLowerBoundPx = bounds.lowerBound.bottom
            imeAnimationUpperBoundPx = bounds.upperBound.bottom
        }
        return super.onStart(animation, bounds)
    }

    /**
     * Во время анимации вычисляет эффективную высоту IME.
     *
     * На Android 16+ при жесте predictive back для клавиатуры платформа может
     * начать присылать `ime = 0` уже с первых кадров анимации закрытия. В таком
     * случае восстанавливаем текущую высоту по `WindowInsetsAnimationCompat`
     * и его `interpolatedFraction`, которая для системных анимаций всегда валидна.
     */
    override fun onProgress(
        insets: WindowInsetsCompat,
        running: List<WindowInsetsAnimationCompat>
    ): WindowInsetsCompat {
        keyboardHeightPx = resolveEffectiveImeHeight(insets, running)
        return super.onProgress(insets, running)
    }

    /**
     * Вызывается по завершении анимации отступов.
     * Сбрасывает флаг [isImeAnimationRunning] и устанавливает финальное состояние клавиатуры.
     */
    override fun onEnd(animation: WindowInsetsAnimationCompat) {
        if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
            isImeAnimationRunning = false
            keyboardHeightPx = lastInsets?.imeHeight() ?: 0
            // Устанавливаем конечное состояние после завершения анимации
            keyboardState = if (keyboardHeightPx > 0) KeyboardState.OPEN else KeyboardState.CLOSED
            imeAnimationLowerBoundPx = 0
            imeAnimationUpperBoundPx = 0
        }
        super.onEnd(animation)
    }

    /**
     * Возвращает текущую эффективную высоту IME для эффектов.
     *
     * Предпочитает фактическую высоту из `WindowInsets`, но при системных
     * анимациях с нулевым значением использует bounds + progress анимации.
     */
    private fun resolveEffectiveImeHeight(
        insets: WindowInsetsCompat,
        running: List<WindowInsetsAnimationCompat>
    ): Int {
        val reportedImeHeight = insets.imeHeight()
        if (reportedImeHeight > 0 || !isImeAnimationRunning) {
            return reportedImeHeight
        }

        val imeAnimation = running.lastOrNull { animation ->
            animation.typeMask and WindowInsetsCompat.Type.ime() != 0
        } ?: return reportedImeHeight

        val lowerBound = imeAnimationLowerBoundPx
        val upperBound = maxOf(imeAnimationUpperBoundPx, lastVisibleKeyboardHeightPx)
        if (upperBound <= lowerBound) {
            return reportedImeHeight
        }

        val fraction = imeAnimation.interpolatedFraction.coerceIn(0f, 1f)
        return when (keyboardState) {
            KeyboardState.CLOSING -> lerp(
                start = upperBound,
                end = lowerBound,
                fraction = fraction
            )

            KeyboardState.OPENING_OR_CHANGING -> lerp(
                start = lowerBound,
                end = upperBound,
                fraction = fraction
            )

            KeyboardState.OPEN -> upperBound
            KeyboardState.CLOSED -> lowerBound
            KeyboardState.UNKNOWN -> reportedImeHeight
        }
    }

    /**
     * Линейная интерполяция между двумя значениями.
     */
    private fun lerp(start: Int, end: Int, fraction: Float): Int {
        return (start + (end - start) * fraction).roundToInt()
    }
}
