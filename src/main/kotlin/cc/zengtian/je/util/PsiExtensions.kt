package cc.zengtian.je.util

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.impl.source.PsiClassReferenceType

fun PsiElement.getTypeHierarchyList(): List<Class<in PsiElement>> {
    val list = ArrayList<Class<in PsiElement>>()
    var temp: PsiElement? = this
    while (temp != null) {
        list.add(temp.javaClass)
        temp = temp.parent
    }
    list.reverse()
    return list
}

@Suppress("UNCHECKED_CAST")
fun <T : PsiElement> PsiElement?.findFirstParentOfType(clazz: Class<out T>): T? {
    if (this == null) {
        return null
    }
    var temp: PsiElement? = this.parent
    while (temp != null) {
        if (clazz.isInstance(temp)) {
            return temp as T
        }
        temp = temp.parent
    }
    return null
}

@Suppress("UNCHECKED_CAST")
fun <T : PsiElement> PsiElement?.findFirstDirectChildOfType(clazz: Class<out T>): T? {
    if (this == null) {
        return null
    }
    for (child in this.children) {
        if (clazz.isInstance(child)) {
            return child as T
        }
    }
    return null
}

fun PsiField.getFieldFullReferenceType(): String? {
    val type = this.findFirstDirectChildOfType(PsiTypeElement::class.java)?.type ?: return null
    if (type is PsiClassReferenceType) {
        val classType: PsiClassReferenceType = type
        if (classType.canonicalText == classType.presentableText) {
            return null
        }
        return classType.canonicalText
    }
    return null
}

fun PsiField.getFieldType(): String? {
    val type = this.findFirstDirectChildOfType(PsiTypeElement::class.java)?.type ?: return null
    if (type is PsiClassReferenceType) {
        val classType: PsiClassReferenceType = type
        return classType.presentableText
    }
    return null
}

fun PsiField.typeIsImported(): Boolean {
    TODO()
}