package ru.curs.celesta.ij

@Suppress("UNCHECKED_CAST")
fun <T> Any?.castSafelyTo(): T? = this as? T