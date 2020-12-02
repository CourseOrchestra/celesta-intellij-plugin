package ru.curs.celesta.intellij

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.nextLeaf
import com.intellij.psi.util.prevLeaf

/**
 * Prev meaningful leaf
 */
fun PsiElement?.prev(): PsiElement? = this?.prevLeaf { it !is PsiWhiteSpace && it !is PsiComment }

/**
 * Next meaningful leaf
 */
fun PsiElement?.next(): PsiElement? = this?.nextLeaf { it !is PsiWhiteSpace && it !is PsiComment }