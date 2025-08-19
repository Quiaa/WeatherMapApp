package com.example.weathermapapp.util

/**
 * LiveData ile tek seferlik olayları (single live events) yönetmek için kullanılan bir sarmalayıcı sınıf.
 * Örneğin, bir Snackbar göstermek, bir navigasyon başlatmak gibi işlemler için kullanılır.
 * Bu sınıf, içeriğin daha önce işlenip işlenmediğini takip eder.
 */
open class Event<out T>(private val content: T) {

    private var hasBeenHandled = false

    /**
     * İçeriği döndürür ve daha önce işlenmediyse işlendi olarak işaretler.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }
}