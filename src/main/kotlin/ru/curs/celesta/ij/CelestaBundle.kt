package ru.curs.celesta.ij

import com.intellij.AbstractBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.*

object CelestaBundle {
    @NonNls
    private const val BUNDLE = "messages.Celesta"

    private val bundle: ResourceBundle by lazy {
        ResourceBundle.getBundle(BUNDLE)
    }

    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
        return AbstractBundle.message(bundle, key, *params)
    }
}