package com.dapadz.eteinsets.dsl

import com.dapadz.eteinsets.dispatcher.InsetsDispatcher



/**
 * Дескриптор, возвращаемый функцией [insets].
 * Предоставляет возможность вручную управлять жизненным циклом обработчика отступов,
 * в частности, вызывать его уничтожение через метод [dispose].
 *
 * В большинстве сценариев ручное управление не требуется, так как библиотека
 * автоматически отписывается от событий при отсоединении View от окна.
 *
 * @property dispatcher Внутренний диспетчер, управляющий логикой.
 */
class InsetsHandle internal constructor(
    internal val dispatcher: InsetsDispatcher
) {

    /**
     * Принудительно отписывается от всех системных слушателей и очищает внутренние ссылки.
     * Вызов этого метода немедленно прекратит обработку отступов для связанной View.
     */
    fun dispose() = dispatcher.dispose()
}