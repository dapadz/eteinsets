package com.dapadz.eteinsets.effect.core

import android.view.View
import androidx.core.view.WindowInsetsCompat
import com.dapadz.eteinsets.dispatcher.InsetsDispatcher

/**
 * Абстрактная единица поведения для обработки оконных отступов.
 *
 * `InsetEffect` инкапсулирует определённую логику, которая должна быть применена
 * в ответ на изменение отступов. Например, добавление `padding` к View или
 * смещение его положения.
 *
 * Эффекты не управляют своим жизненным циклом самостоятельно. Они получают события
 * от [InsetsDispatcher], к которому прикреплены.
 */
abstract class InsetEffect {

    /**
     * Диспетчер, который доставляет события этому эффекту.
     * Устанавливается при вызове [attach].
     */
    internal var dispatcher: InsetsDispatcher? = null

    /**
     * View, к которому применяется эффект.
     */
    protected var hostView: View? = null
        private set

    /**
     * Прикрепляет эффект к диспетчеру. Вызывается [InsetsDispatcher] при своей инициализации.
     * @param dispatcher Диспетчер, который будет поставлять события.
     */
    internal fun attach(dispatcher: InsetsDispatcher) {
        this.dispatcher = dispatcher
    }

    /**
     * Отсоединяет эффект, очищая все внутренние ссылки.
     * Вызывается, когда [InsetsDispatcher] уничтожается.
     */
    internal fun detach() {
        dispatcher = null
        hostView = null
    }

    /**
     * Основная точка входа для обработки событий изменения отступов.
     *
     * Этот метод вызывается диспетчером каждый раз, когда `onApplyWindowInsets`
     * срабатывает на хост-View.
     *
     * @param view View, получившая событие.
     * @param insets Текущие оконные отступы.
     */
    open fun onApplyWindowInsets(view: View, insets: WindowInsetsCompat) {
        if (hostView !== view) hostView = view
    }
}