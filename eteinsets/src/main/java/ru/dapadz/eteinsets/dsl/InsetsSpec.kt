package ru.dapadz.eteinsets.dsl

import ru.dapadz.eteinsets.dsl.common.SystemBarsInsetApplyMode
import ru.dapadz.eteinsets.dsl.common.SystemBarsInsetScope
import ru.dapadz.eteinsets.effect.core.InsetEffect
import ru.dapadz.eteinsets.effect.impl.ImeAvoidOverlapsEffect
import ru.dapadz.eteinsets.effect.impl.KeepCenteredUnderImeEffect
import ru.dapadz.eteinsets.effect.impl.SystemBarsToMarginEffect
import ru.dapadz.eteinsets.effect.impl.SystemBarsToPaddingEffect
import ru.dapadz.eteinsets.effect.impl.SystemBarsToPaddingWhileImeEffect

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

    /**
     * Фабрики эффектов, сохраняющие исходный порядок DSL-вызовов.
     *
     * Такой подход позволяет корректно вставлять модификаторы вроде `hideWhenIme`
     * сразу после соответствующего базового эффекта, не нарушая ожидаемую
     * последовательность обработки `WindowInsets`.
     */
    private val effectFactories = mutableListOf<() -> List<InsetEffect>>()

    /**
     * Добавляет отступы системных панелей к `padding` для View.
     * Этот эффект применяется однократно.
     *
     * @param left Если `true`, применить левый системный отступ.
     * @param top Если `true`, применить верхний системный отступ.
     * @param right Если `true`, применить правый системный отступ.
     * @param bottom Если `true`, применить нижний системный отступ.
     * @return [SystemBarsInsetScope] для дальнейшей настройки, например, [SystemBarsInsetScope.hideWhenIme].
     */
    fun systemBarsPadding(
        left: Boolean = false,
        top: Boolean = false,
        right: Boolean = false,
        bottom: Boolean = false
    ): SystemBarsInsetScope {
        val scope = SystemBarsInsetScope(
            applyMode = SystemBarsInsetApplyMode.PADDING,
            appliesBottomInset = bottom
        )

        effectFactories += {
            buildList {
                add(
                    SystemBarsToPaddingEffect(
                        addLeft = left,
                        addTop = top,
                        addRight = right,
                        addBottom = bottom
                    )
                )
                scope.createHideWhenImeEffectOrNull()?.let(::add)
            }
        }

        return scope
    }

    /**
     * Добавляет отступы системных панелей к `margin` для View.
     *
     * Эффект полезен в сценариях, где нужно сместить саму View относительно
     * родителя, а не изменить внутренние отступы её контента.
     *
     * Если layout params конкретной View не поддерживают `margin`, эффект
     * безопасно превращается в no-op.
     *
     * @param left Если `true`, применить левый системный отступ.
     * @param top Если `true`, применить верхний системный отступ.
     * @param right Если `true`, применить правый системный отступ.
     * @param bottom Если `true`, применить нижний системный отступ.
     * @return [SystemBarsInsetScope] для дальнейшей настройки, например, [SystemBarsInsetScope.hideWhenIme].
     */
    fun systemBarsMargin(
        left: Boolean = false,
        top: Boolean = false,
        right: Boolean = false,
        bottom: Boolean = false
    ): SystemBarsInsetScope {
        val scope = SystemBarsInsetScope(
            applyMode = SystemBarsInsetApplyMode.MARGIN,
            appliesBottomInset = bottom
        )

        effectFactories += {
            buildList {
                add(
                    SystemBarsToMarginEffect(
                        addLeft = left,
                        addTop = top,
                        addRight = right,
                        addBottom = bottom
                    )
                )
                scope.createHideWhenImeEffectOrNull()?.let(::add)
            }
        }

        return scope
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
        effectFactories += { listOf(ImeAvoidOverlapsEffect(mode, strategy)) }
    }

    /**
     * Добавляет эффект, который удерживает View по центру видимой области экрана
     * при появлении клавиатуры.
     */
    fun keepCenteredUnderIme() {
        effectFactories += { listOf(KeepCenteredUnderImeEffect()) }
    }

    /**
     * Позволяет добавить один или несколько кастомных [InsetEffect].
     * @param extra Ваши реализации [InsetEffect].
     */
    fun use(vararg extra: InsetEffect) {
        effectFactories += { extra.toList() }
    }

    /**
     * Собирает и финализирует список эффектов на основе DSL-конфигурации.
     */
    internal fun build(): List<InsetEffect> = effectFactories.flatMap { it() }

    /**
     * Создаёт IME-модификатор только для тех сценариев, где действительно был
     * запрошен нижний системный отступ.
     */
    private fun SystemBarsInsetScope.createHideWhenImeEffectOrNull(): InsetEffect? {
        if (!hideBottomWhenIme) return null
        return SystemBarsToPaddingWhileImeEffect(applyMode = applyMode)
    }
}
