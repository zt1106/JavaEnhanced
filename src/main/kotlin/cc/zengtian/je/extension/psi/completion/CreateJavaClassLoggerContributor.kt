package cc.zengtian.je.extension.psi.completion

import cc.zengtian.je.completion.logger.LoggerLibrary
import cc.zengtian.je.util.IDEA_JAVA_COMPLETION_DUMMY_IDENTIFIER_TRIMMED
import cc.zengtian.je.util.findFirstParentOfType
import cc.zengtian.je.util.getFieldFullReferenceType
import cc.zengtian.je.util.getFieldType
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.JavaCompletionContributor.isInJavaContext
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.*
import com.intellij.util.ProcessingContext

@Suppress("UnstableApiUsage")
class CreateJavaClassLoggerContributor : CompletionContributor(), DumbAware {

    private val fieldDeclarationAfterEqualsBeforeMethodCall = psiElement(PsiIdentifier::class.java)
            .withParents(PsiReferenceExpression::class.java, PsiField::class.java, PsiClass::class.java)

    private val fieldDeclarationAfterMethodCall = psiElement(PsiIdentifier::class.java)
            .withParents(PsiReferenceExpression::class.java, PsiExpressionList::class.java, PsiMethodCallExpression::class.java, PsiField::class.java, PsiClass::class.java)


    private val inJavaContextPredicate: (PsiElement) -> Boolean = { isInJavaContext(it) }

    private val isCompletionPlaceHolder: (PsiElement) -> Boolean = { it.text == IDEA_JAVA_COMPLETION_DUMMY_IDENTIFIER_TRIMMED }

    private val inNonAnonymousClass: (PsiElement) -> Boolean = { it.findFirstParentOfType(PsiClass::class.java)?.name != null }

    private val isEqualsToken: (PsiElement) -> Boolean = { it is PsiJavaToken && it.text == "=" }

    private val inLoggerField: (PsiElement) -> Boolean = lambda@{ it.findFirstParentOfType(PsiField::class.java) != null }

    init {
        extend(CompletionType.BASIC, fieldDeclarationAfterMethodCall, LoggerClassParameterCompletionProvider())
        extend(CompletionType.BASIC, fieldDeclarationAfterEqualsBeforeMethodCall, LoggerCreationCompletionProvider())
    }

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        super.fillCompletionVariants(parameters, result)
    }

    inner class LoggerClassParameterCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
            val element = parameters.position
            val clazz = element.findFirstParentOfType(PsiClass::class.java) ?: return
            if (element.parent.text.startsWith(clazz.name ?: return)) {
                return
            }
            val classStr = (clazz.name ?: return) + ".class"
            result.addElement(LookupElementBuilder
                    .create(classStr)
                    .withIcon(AllIcons.Nodes.Class)
                    .withBoldness(true)
                    .withInsertHandler { c, _ ->
                        val range = element.findFirstParentOfType(PsiField::class.java)?.textRange ?: return@withInsertHandler
                        val reformatCodeProcessor = ReformatCodeProcessor(c.project, c.file, range, false)
                        reformatCodeProcessor.run()
                    }
            )
        }
    }

    inner class LoggerCreationCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
            val element = parameters.position
            if (!inJavaContextPredicate(element) || !inNonAnonymousClass(element)) {
                return
            }
            val field = element.findFirstParentOfType(PsiField::class.java) ?: return
            val qualifiedName = field.getFieldFullReferenceType() ?: return
            // field type is a supported Logger type (if imported)
            val library = LoggerLibrary.values().find { qualifiedName == it.loggerClass }
            if (library != null) {
                val clazz = element.findFirstParentOfType(PsiClass::class.java) ?: return
                val classStr = (clazz.name ?: return) + ".class"
                var invocation = library.invocation(classStr)
                val prefix = element.findFirstParentOfType(PsiReferenceExpression::class.java)?.text?.replace(IDEA_JAVA_COMPLETION_DUMMY_IDENTIFIER_TRIMMED, "")
                if (prefix != null && invocation.startsWith(prefix)) {
                    // trim before "."
                    if (prefix.contains(".")) {
                        val lastDotIdx = prefix.lastIndexOf(".")
                        invocation = invocation.substring(lastDotIdx + 1)
                    }
                    result.addElement(LookupElementBuilder
                            .create(invocation)
                            .withIcon(AllIcons.Nodes.Method)
                            .withBoldness(true)
                            .withInsertHandler { c, _ ->
                                val range = element.findFirstParentOfType(PsiField::class.java)?.textRange ?: return@withInsertHandler
                                val reformatCodeProcessor = ReformatCodeProcessor(c.project, c.file, range, false)
                                reformatCodeProcessor.run()
                            }
                    )
                }
                return
            }
            // or a supported type in user's dependencies (when not imported) (red "Logger")
        }
    }


    override fun invokeAutoPopup(position: PsiElement, typeChar: Char): Boolean {
        if (typeChar == '=' || typeChar == '(') {
            if (inJavaContextPredicate(position) && inNonAnonymousClass(position)) {
                // fix white space before type char
                val field: PsiField? = if (position is PsiIdentifier) {
                    position.findFirstParentOfType(PsiField::class.java)
                } else if (position is PsiWhiteSpace && position.prevSibling is PsiField) {
                    position.prevSibling as PsiField
                } else {
                    null
                }
                val fieldFullReferenceType = field?.getFieldFullReferenceType()
                if (LoggerLibrary.values().any { it.loggerClass == fieldFullReferenceType }) {
                    return true
                }
                if (fieldFullReferenceType == null) {
                    val fieldType = field?.getFieldType() ?: "WHATEVER...."
                    return LoggerLibrary.values().any { it.loggerClass.endsWith(fieldType) }
                }
            }
        }
        return super.invokeAutoPopup(position, typeChar)
    }
}