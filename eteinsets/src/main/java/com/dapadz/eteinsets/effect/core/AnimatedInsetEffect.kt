package com.dapadz.eteinsets.effect.core

import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import com.dapadz.eteinsets.dispatcher.ImeInsetsDispatcher

/**
 * Расширение [InsetEffect] для обработки событий анимации отступов.
 *
 * Используйте этот класс, если ваш эффект должен плавно реагировать на изменения
 * отступов во времени, например, при появлении/скрытии клавиатуры (IME)
 * или при использовании жестовой навигации.
 *
 * Класс предоставляет пустые реализации методов [WindowInsetsAnimationCompat.Callback],
 * которые можно переопределить для создания синхронизированных анимаций.
 *
 * @see WindowInsetsAnimationCompat
 * @see ImeInsetsDispatcher
 */
abstract class AnimatedInsetEffect : InsetEffect() {

    /**
     * Вызывается перед началом анимации отступов.
     *
     * Это подходящее место для сохранения начальных состояний View (например,
     * его размеров или положения) перед тем, как они будут изменены анимацией.
     *
     * @param animation Объект анимации, содержащий информацию о типе и продолжительности.
     */
    open fun onPrepare(animation: WindowInsetsAnimationCompat) {}

    /**
     * Вызывается в самом начале анимации отступов.
     *
     * @param animation Объект анимации.
     * @param bounds Границы анимации.
     */
    open fun onStart(
        animation: WindowInsetsAnimationCompat,
        bounds: WindowInsetsAnimationCompat.BoundsCompat
    ) {}

    /**
     * Вызывается на каждом кадре во время выполнения анимации.
     *
     * Этот метод является ключевым для создания плавных, синхронизированных с системой
     * анимаций. Здесь можно обновлять свойства View (например, `translationY` или `padding`)
     * на основе текущих значений отступов.
     *
     * @param insets Текущие отступы на данном кадре анимации.
     * @param animations Список всех активных на данный момент анимаций отступов.
     */
    open fun onProgress(insets: WindowInsetsCompat, animations: List<WindowInsetsAnimationCompat>) {}

    /**
     * Вызывается по завершении анимации отступов.
     *
     * Используйте этот метод, чтобы привести состояние View к его финальному значению,
     * гарантируя, что оно будет корректным после окончания анимации.
     *
     * @param animation Завершившаяся анимация.
     */
    open fun onEnd(animation: WindowInsetsAnimationCompat) {}
}