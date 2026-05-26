package ru.dapadz.eteinsets.dsl.common

import ru.dapadz.eteinsets.dsl.InsetsSpec

/**
 * Внутренний режим применения системного нижнего отступа.
 *
 * Используется DSL-слоем для выбора того, какое свойство [android.view.View]
 * должно анимироваться при вызове [SystemBarsInsetScope.hideWhenIme].
 */
internal enum class SystemBarsInsetApplyMode {
    /**
     * Системный отступ применяется через `padding`.
     */
    PADDING,

    /**
     * Системный отступ применяется через `margin`.
     */
    MARGIN
}

/**
 * Область для настройки поведения системных отступов, добавленных через
 * [InsetsSpec.systemBarsPadding] или [InsetsSpec.systemBarsMargin].
 *
 * Экземпляр этого класса возвращается из DSL-методов системных панелей и позволяет
 * декларативно добавить дополнительные модификаторы, не раскрывая внутреннюю
 * реализацию эффектов наружу.
 *
 * @property applyMode Режим, определяющий, какое свойство View должно меняться.
 * @property appliesBottomInset Признак того, что базовый эффект действительно
 * применяет нижний системный отступ. Если `false`, [hideWhenIme] останется no-op.
 *
 * @see InsetsSpec.systemBarsPadding
 * @see InsetsSpec.systemBarsMargin
 */
class SystemBarsInsetScope internal constructor(
    internal val applyMode: SystemBarsInsetApplyMode,
    private val appliesBottomInset: Boolean
) {

    /**
     * Внутренний флаг, указывающий, нужно ли скрывать нижний системный отступ
     * при появлении IME.
     */
    internal var hideBottomWhenIme: Boolean = false

    /**
     * Активирует плавное удаление нижнего отступа системной панели из `padding`
     * или `margin`, когда появляется клавиатура.
     *
     * Такой сценарий устраняет "двойной нижний отступ", который возникает,
     * когда одновременно учитываются и `navigation bar`, и высота IME.
     *
     * Модификатор имеет смысл только для вызовов, где был включён `bottom = true`.
     * Если нижний системный отступ не запрашивался, метод остаётся безопасным no-op.
     *
     * @return Текущий scope для дальнейшей цепочки вызовов.
     */
    fun hideWhenIme(): SystemBarsInsetScope = apply {
        hideBottomWhenIme = appliesBottomInset
    }
}
