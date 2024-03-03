package ru.curs.celesta.ij

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.ConcurrencyUtil
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

private val globalKeyForProvider: ConcurrentMap<String, Key<CachedValue<*>>> = ConcurrentHashMap()


private fun <T> getKeyForName(
    name: String,
    keyForProvider: ConcurrentMap<String, Key<CachedValue<*>>>
): Key<CachedValue<T>?> {
    var key = keyForProvider[name]
    if (key == null) {
        key = ConcurrencyUtil.cacheOrGet(keyForProvider, name, Key.create(name))
    }
    @Suppress("UNCHECKED_CAST")
    return key as Key<CachedValue<T>?>
}

private fun <T> getKeyForClass(
    providerClass: Class<*>,
    keyForProvider: ConcurrentMap<String, Key<CachedValue<*>>>
): Key<CachedValue<T>?> {
    val name = providerClass.name
        ?: error("$providerClass doesn't have a name; can't be used for cache value provider")
    return getKeyForName(name, keyForProvider)
}

fun <T, P:PsiElement> P.cachedValue(provider: P.() -> T): T = CachedValuesManager.getManager(project)
    .getCachedValue(
        this,
        getKeyForClass(
            provider.javaClass,
            globalKeyForProvider
        ),
        {
            val result = provider(this)
            CachedValueProvider.Result.create(result, this)
        },
        false
    )