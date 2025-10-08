package com.dapadz.eteinsets.dsl

import android.view.View
import androidx.core.view.ViewCompat
import com.dapadz.eteinsets.dispatcher.ImeInsetsDispatcher
import com.dapadz.eteinsets.dispatcher.InsetsDispatcher
import com.dapadz.eteinsets.effect.core.InsetEffect
/**
 * Эта функция-расширение для [View] позволяет декларативно настроить поведение
 * View в ответ на изменение системных оконных отступов (`WindowInsets`).
 *
 * ### Принцип работы:
 * 1. Создает [InsetsSpec] и выполняет на нем предоставленный DSL-блок `build`.
 * 2. Собирает сконфигурированные эффекты ([InsetEffect]).
 * 3. Создает и настраивает [ImeInsetsDispatcher] — единый диспетчер, который
 *    умеет работать с анимациями клавиатуры.
 * 4. Устанавливает этот диспетчер в качестве слушателя `OnApplyWindowInsetsListener`
 *    и `WindowInsetsAnimationCallback` на View, заменяя любые предыдущие.
 * 5. Включает механизм автоматической очистки ресурсов при отсоединении View от окна.
 *
 * @param build Лямбда-выражение с ресивером [InsetsSpec], в котором вы описываете
 *   необходимое поведение (например, `systemBarsPadding(bottom = true)`).
 * @return [InsetsHandle], который можно использовать для ручного управления (например, `dispose()`),
 *   хотя в большинстве случаев это не требуется.
 */
fun View.insets(build: InsetsSpec.() -> Unit): InsetsHandle {
    val spec = InsetsSpec().apply(build)
    val effects = spec.build()
    // Всегда используется ImeInsetsDispatcher, т.к. он обратно совместим
    // и готов к сценариям с клавиатурой "из коробки".
    val dispatcher = ImeInsetsDispatcher(*effects.toTypedArray())
    replaceInsetsCallbacks(dispatcher)
    dispatcher.enableAutoDispose(this)
    return InsetsHandle(dispatcher)
}

/**
 * Заменяет существующие слушатели отступов на новый диспетчер.
 * Сначала удаляются старые слушатели, чтобы избежать двойной обработки,
 * затем устанавливается новый.
 */
private fun View.replaceInsetsCallbacks(dispatcher: InsetsDispatcher) {
    // Сначала обнуляем, чтобы быть уверенными, что старых слушателей нет.
    ViewCompat.setOnApplyWindowInsetsListener(this, null)
    ViewCompat.setWindowInsetsAnimationCallback(this, null)
    // Затем устанавливаем наш диспетчер.
    ViewCompat.setOnApplyWindowInsetsListener(this, dispatcher)
    ViewCompat.setWindowInsetsAnimationCallback(this, dispatcher)
}