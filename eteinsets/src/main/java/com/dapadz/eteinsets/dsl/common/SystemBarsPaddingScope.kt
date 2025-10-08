package com.dapadz.eteinsets.dsl.common

import com.dapadz.eteinsets.dsl.InsetsSpec

/**
 * Область для настройки поведения эффекта [InsetsSpec.systemBarsPadding].
 *
 * Этот класс используется как возвращаемое значение из `systemBarsPadding`,
 * предоставляя цепочку вызовов для дальнейшей конфигурации.
 *
 * @see InsetsSpec.systemBarsPadding
 */
class SystemBarsPaddingScope internal constructor() {

    /**
     * Внутренний флаг, указывающий, нужно ли скрывать нижний отступ системной панели при появлении IME.
     */
    internal var hideBottomWhenIme: Boolean = false

    /**
     * Модификатор, который активирует плавное удаление нижнего отступа системной
     * панели (`navigation bar`) из `padding` View, когда появляется клавиатура.
     *
     * Это решает проблему "двойного отступа" снизу, когда одновременно видны
     * и отступ от системной панели, и отступ от клавиатуры.
     *
     * @return `this` для дальнейшей цепочки вызовов.
     */
    fun hideWhenIme(): SystemBarsPaddingScope = apply { hideBottomWhenIme = true }

}