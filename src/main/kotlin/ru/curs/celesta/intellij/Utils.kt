package ru.curs.celesta.intellij

fun <T> Any?.castSafelyTo(): T? = this as? T