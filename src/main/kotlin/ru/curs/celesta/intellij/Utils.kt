package ru.curs.celesta.intellij

@Suppress("UNCHECKED_CAST")
fun <T> Any?.castSafelyTo(): T? = this as? T