package com.dapadz.eteinsets.dsl

import com.dapadz.eteinsets.dsl.common.SystemBarsPaddingScope
import com.dapadz.eteinsets.effect.core.InsetEffect
import com.dapadz.eteinsets.effect.impl.ImeAvoidOverlapsEffect
import com.dapadz.eteinsets.effect.impl.KeepCenteredUnderImeEffect
import com.dapadz.eteinsets.effect.impl.SystemBarsToPaddingEffect
import com.dapadz.eteinsets.effect.impl.SystemBarsToPaddingWhileImeEffect
import com.dapadz.eteinsets.dsl.insets

/**
 * DSL-конструктор для декларативного описания поведения View при изменении оконных отступов.
 *
 * Позволяет комбинировать готовые эффекты ([InsetEffect]) для реализации
 * сложных сценариев без необходимости управлять флагами или слушать колбэки вручную.
 *
 * Пример использования:
 * ```
 * myView.insets {
 *  // Добавить отступы системных панелей к padding'у View
 *  systemBarsPadding(bottom = true).hideWhenIme()
 *  // Поднимать View над клавиатурой
 *  imeAvoidOverlaps()
 * }
 * ```
 * @see insets
 */
class InsetsSpec internal constructor() {

    private val effects = mutableListOf<InsetEffect>()
    private val systemBarsScopes = mutableListOf<SystemBarsPaddingScope>()

    /**
     * Добавляет отступы системных панелей к `padding` для View.
     * Этот эффект применяется однократно
     *
     * @param left Если `true`, применить левый системный отступ.
     * @param top Если `true`, применить верхний системный отступ.
     * @param right Если `true`, применить правый системный отступ.
     * @param bottom Если `true`, применить нижний системный отступ.
     * @return [SystemBarsPaddingScope] для дальнейшей настройки, например, [SystemBarsPaddingScope.hideWhenIme].
     */
    fun systemBarsPadding(
        left: Boolean = false,
        top: Boolean = false,
        right: Boolean = false,
        bottom: Boolean = false
    ): SystemBarsPaddingScope {
        effects += SystemBarsToPaddingEffect(
            addLeft = left,
            addTop = top,
            addRight = right,
            addBottom = bottom
        )
        return SystemBarsPaddingScope().also { systemBarsScopes += it }
    }

    /**
     * Добавляет эффект, который предотвращает перекрытие View клавиатурой.
     * View будет смещаться вверх синхронно с анимацией IME.
     *
     * @param mode Способ смещения View ([ImeAvoidOverlapsEffect.ApplyMode]).
     *   По умолчанию [ImeAvoidOverlapsEffect.ApplyMode.PADDING_BOTTOM].
     * @param overlapOnly Если `true` (по умолчанию), View будет смещаться только если
     *   клавиатура его действительно перекрывает. Если `false`, смещение будет равно всей высоте IME.
     */
    fun imeAvoidOverlaps(
        mode: ImeAvoidOverlapsEffect.ApplyMode = ImeAvoidOverlapsEffect.ApplyMode.PADDING_BOTTOM,
        overlapOnly: Boolean = true
    ) {
        val strategy = if (overlapOnly) ImeAvoidOverlapsEffect.MoveStrategy.ONLY_IF_OVERLAP
        else ImeAvoidOverlapsEffect.MoveStrategy.ALWAYS
        effects += ImeAvoidOverlapsEffect(mode, strategy)
    }

    /**
     * Добавляет эффект, который удерживает View по центру видимой области экрана
     * при появлении клавиатуры.
     */
    fun keepCenteredUnderIme() {
        effects += KeepCenteredUnderImeEffect()
    }

    /**
     * Позволяет добавить один или несколько кастомных [InsetEffect].
     * @param extra Ваши реализации [InsetEffect].
     */
    fun use(vararg extra: InsetEffect) {
        effects += extra
    }

    /**
     * Собирает и финализирует список эффектов на основе DSL-конфигурации.
     *
     * Этот метод обрабатывает модификаторы (например, `hideWhenIme`) и добавляет
     * соответствующие эффекты в правильном порядке.
     *
     * @return Финальный список [InsetEffect] для передачи в диспетчер.
     */
    internal fun build(): List<InsetEffect> {
        systemBarsScopes.forEach { scope ->
            if (scope.hideBottomWhenIme) {
                // ВАЖНО: добавляем эффект после базового SystemBarsToPaddingEffect,
                // чтобы порядок вызовов onApplyWindowInsets был корректным.
                // Сначала базовый эффект добавляет padding, затем этот эффект его захватывает.
                effects += SystemBarsToPaddingWhileImeEffect()
            }
        }
        return effects.toList()
    }
}