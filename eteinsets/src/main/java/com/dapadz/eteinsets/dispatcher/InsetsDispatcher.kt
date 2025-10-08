package com.dapadz.eteinsets.dispatcher

import android.view.View
import android.view.ViewGroup
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.forEach
import com.dapadz.eteinsets.effect.core.AnimatedInsetEffect
import com.dapadz.eteinsets.effect.core.InsetEffect
import java.lang.ref.WeakReference
import kotlin.collections.asSequence
import androidx.core.view.WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE

/**
 * Базовый диспетчер событий оконных отступов ([WindowInsetsCompat]).
 *
 * Этот класс является центральным узлом, который получает события отступов от системы
 * и перенаправляет их списку зарегистрированных эффектов ([InsetEffect]).
 * Он реализует [OnApplyWindowInsetsListener] и [WindowInsetsAnimationCompat.Callback],
 * что позволяет ему обрабатывать как статичные изменения отступов, так и их анимации.
 *
 * Основные обязанности:
 * - Хранить список эффектов и управлять их жизненным циклом.
 * - Получать системные колбэки и передавать их каждому эффекту.
 * - Обеспечивать автоматическую отписку от событий при отсоединении View от окна.
 *
 * @param dispatchMode Режим диспетчеризации для [WindowInsetsAnimationCompat.Callback].
 *   Рекомендуется [DISPATCH_MODE_CONTINUE_ON_SUBTREE], чтобы дочерние View также получали события.
 * @param effects Список эффектов, которые будут управляться этим диспетчером.
 */
open class InsetsDispatcher(
    dispatchMode: Int,
    vararg effects: InsetEffect,
) : WindowInsetsAnimationCompat.Callback(dispatchMode), OnApplyWindowInsetsListener {

    /**
     * Список эффектов, которые будут получать события от этого диспетчера.
     * */
    private val effects: List<InsetEffect> = effects.toList()

    /**
     * Cсылка на View-хост
     * */
    private var hostRef: WeakReference<View>? = null

    /**
     * Предоставляет доступ к View, к которому прикреплен диспетчер.
     * Возвращает `null`, если View было уничтожено или еще не прикреплено.
     */
    val host: View? get() = hostRef?.get()

    init {
        // При инициализации прикрепляем каждый эффект к этому диспетчеру.
        effects.forEach { it.attach(this) }
    }

    /**
     * Включает механизм автоматической очистки ресурсов.
     *
     * Диспетчер будет следить за состоянием [view] и вызовет [dispose] автоматически,
     * когда View будет отсоединено от окна. Это предотвращает утечки памяти.
     *
     * @param view View-хост.
     * @return `InsetsDispatcher` для цепочки вызовов.
     */
    internal fun enableAutoDispose(view: View): InsetsDispatcher {
        if (host == null) hostRef = WeakReference(view)
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewDetachedFromWindow(v: View) {
                dispose()
                v.removeOnAttachStateChangeListener(this)
            }
            override fun onViewAttachedToWindow(v: View) = Unit
        })
        return this
    }

    /**
     * Освобождает ресурсы, используемые диспетчером и его эффектами.
     *
     * Снимает все слушатели с `host` View, отсоединяет эффекты и очищает ссылки.
     * Вызывается автоматически, если включен `auto-dispose`, но может быть вызван и вручную.
     */
    fun dispose() {
        host?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it, null)
            ViewCompat.setWindowInsetsAnimationCallback(it, null)
        }
        effects.forEach { it.detach() }
        hostRef?.clear()
        hostRef = null
    }

    /**
     * Основной метод, получающий события об изменении отступов.
     *
     * Он выполняет следующие действия:
     * 1. Сохраняет ссылку на `view`, если она изменилась.
     * 2. Передает `insets` каждому зарегистрированному эффекту.
     * 3. Передает `insets` дочерним View и проверяет, были ли они потреблены.
     *
     * @return Возвращает [WindowInsetsCompat.CONSUMED], если отступы были использованы дочерними View,
     *   иначе возвращает оригинальные `insets`.
     */
    override fun onApplyWindowInsets(view: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        if (host !== view) hostRef = WeakReference(view)
        effects.forEach { it.onApplyWindowInsets(view, insets) }

        // Позволяем дочерним элементам также обработать отступы.
        var consumedByChildren = false
        (view as? ViewGroup)?.forEach { child ->
            val result = ViewCompat.dispatchApplyWindowInsets(child, insets)
            if (result.isConsumed) consumedByChildren = true
        }
        return if (consumedByChildren) WindowInsetsCompat.CONSUMED else insets
    }

    /**
     * Передает событие `onPrepare` всем [AnimatedInsetEffect].
     */
    override fun onPrepare(animation: WindowInsetsAnimationCompat) {
        effects.asSequence()
            .filterIsInstance<AnimatedInsetEffect>()
            .forEach { it.onPrepare(animation) }
    }

    /**
     * Передает событие `onStart` всем [AnimatedInsetEffect].
     */
    override fun onStart(
        animation: WindowInsetsAnimationCompat,
        bounds: WindowInsetsAnimationCompat.BoundsCompat
    ): WindowInsetsAnimationCompat.BoundsCompat = bounds.also {
        effects.asSequence()
            .filterIsInstance<AnimatedInsetEffect>()
            .forEach { it.onStart(animation, bounds) }
    }

    /**
     * Передает событие `onProgress` всем [AnimatedInsetEffect].
     */
    override fun onProgress(
        insets: WindowInsetsCompat,
        running: List<WindowInsetsAnimationCompat>
    ): WindowInsetsCompat = insets.also {
        effects.asSequence()
            .filterIsInstance<AnimatedInsetEffect>()
            .forEach { it.onProgress(insets, running) }
    }

    /**
     * Передает событие `onEnd` всем [AnimatedInsetEffect].
     */
    override fun onEnd(animation: WindowInsetsAnimationCompat) {
        effects.asSequence()
            .filterIsInstance<AnimatedInsetEffect>()
            .forEach { it.onEnd(animation) }
    }
}