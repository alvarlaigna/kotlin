/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi.debugText

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

// invoke this instead of getText() when you need debug text to identify some place in PSI without storing the element itself
// this is need to avoid unnecessary file parses
// this defaults to get text if the element is not stubbed
fun KtElement.getDebugText(): String {
    if (this !is KtElementImplStub<*> || this.stub == null) {
        return text
    }
    if (this is KtPackageDirective) {
        val fqName = getFqName()
        if (fqName.isRoot()) {
            return ""
        }
        return "package " + fqName.asString()
    }
    return accept(DebugTextBuildingVisitor, Unit)
}


private object DebugTextBuildingVisitor : KtVisitor<String, Unit>() {

    private val LOG = Logger.getInstance(this.javaClass)

    override fun visitKtFile(file: KtFile, data: Unit?): String? {
        return "STUB file: ${file.name}"
    }

    override fun visitKtElement(element: KtElement, data: Unit?): String? {
        if (element is KtElementImplStub<*>) {
            LOG.error("getDebugText() is not defined for ${element.javaClass}")
        }
        return element.text
    }

    override fun visitImportDirective(importDirective: KtImportDirective, data: Unit?): String? {
        val importPath = importDirective.getImportPath()
        if (importPath == null) {
            return "import <invalid>"
        }
        val aliasStr = if (importPath.hasAlias()) " as " + importPath.getAlias()!!.asString() else ""
        return "import ${importPath.getPathStr()}" + aliasStr
    }

    override fun visitImportList(importList: KtImportList, data: Unit?): String? {
        return renderChildren(importList, separator = "\n")
    }

    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry, data: Unit?): String? {
        return render(annotationEntry, annotationEntry.getCalleeExpression(), annotationEntry.getTypeArgumentList())
    }

    override fun visitTypeReference(typeReference: KtTypeReference, data: Unit?): String? {
        return renderChildren(typeReference, " ")
    }

    override fun visitTypeArgumentList(typeArgumentList: KtTypeArgumentList, data: Unit?): String? {
        return renderChildren(typeArgumentList, ", ", "<", ">")
    }

    override fun visitTypeConstraintList(list: KtTypeConstraintList, data: Unit?): String? {
        return renderChildren(list, ", ", "where ", "")
    }

    override fun visitUserType(userType: KtUserType, data: Unit?): String? {
        return render(userType, userType.getQualifier(), userType.getReferenceExpression(), userType.getTypeArgumentList())
    }

    override fun visitDynamicType(type: KtDynamicType, data: Unit?): String? {
        return "dynamic"
    }

    override fun visitAnnotation(annotation: KtAnnotation, data: Unit?): String? {
        return renderChildren(annotation, " ", "[", "]")
    }

    override fun visitConstructorCalleeExpression(constructorCalleeExpression: KtConstructorCalleeExpression, data: Unit?): String? {
        return render(constructorCalleeExpression, constructorCalleeExpression.getConstructorReferenceExpression())
    }

    override fun visitSuperTypeListEntry(specifier: KtSuperTypeListEntry, data: Unit?): String? {
        return render(specifier, specifier.getTypeReference())
    }

    override fun visitSuperTypeList(list: KtSuperTypeList, data: Unit?): String? {
        return renderChildren(list, ", ")
    }

    override fun visitTypeParameterList(list: KtTypeParameterList, data: Unit?): String? {
        return renderChildren(list, ", ", "<", ">")
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression, data: Unit?): String? {
        return renderChildren(expression, ".")
    }

    override fun visitInitializerList(list: KtInitializerList, data: Unit?): String? {
        return renderChildren(list, ", ")
    }

    override fun visitParameterList(list: KtParameterList, data: Unit?): String? {
        return renderChildren(list, ", ", "(", ")")
    }

    override fun visitEnumEntry(enumEntry: KtEnumEntry, data: Unit?): String? {
        return buildText {
            append("STUB: ")
            appendInn(enumEntry.getModifierList(), suffix = " ")
            append("enum entry ")
            appendInn(enumEntry.getNameAsName())
            appendInn(enumEntry.getInitializerList(), prefix = " : ")
        }
    }

    override fun visitFunctionType(functionType: KtFunctionType, data: Unit?): String? {
        return buildText {
            appendInn(functionType.getReceiverTypeReference(), suffix = ".")
            appendInn(functionType.getParameterList())
            appendInn(functionType.getReturnTypeReference(), prefix = " -> ")
        }
    }

    override fun visitTypeParameter(parameter: KtTypeParameter, data: Unit?): String? {
        return buildText {
            appendInn(parameter.getModifierList(), suffix = " ")
            appendInn(parameter.getNameAsName())
            appendInn(parameter.getExtendsBound(), prefix = " : ")
        }
    }

    override fun visitTypeProjection(typeProjection: KtTypeProjection, data: Unit?): String? {
        return buildText {
            val token = typeProjection.getProjectionKind().getToken()
            appendInn(token?.getValue())
            val typeReference = typeProjection.getTypeReference()
            if (token != null && typeReference != null) {
                append(" ")
            }
            appendInn(typeReference)
        }
    }

    override fun visitModifierList(list: KtModifierList, data: Unit?): String? {
        return buildText {
            var first = true
            for (modifierKeywordToken in KtTokens.MODIFIER_KEYWORDS_ARRAY) {
                if (list.hasModifier(modifierKeywordToken)) {
                    if (!first) {
                        append(" ")
                    }
                    append(modifierKeywordToken.value)
                    first = false
                }
            }
        }
    }

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression, data: Unit?): String? {
        return expression.getReferencedName()
    }

    override fun visitNullableType(nullableType: KtNullableType, data: Unit?): String? {
        return renderChildren(nullableType, "", "", "?")
    }

    override fun visitAnonymousInitializer(initializer: KtAnonymousInitializer, data: Unit?): String? {
        val containingDeclaration = KtStubbedPsiUtil.getContainingDeclaration(initializer)
        return "initializer in " + (containingDeclaration?.getDebugText() ?: "...")
    }

    override fun visitClassBody(classBody: KtClassBody, data: Unit?): String? {
        val containingDeclaration = KtStubbedPsiUtil.getContainingDeclaration(classBody)
        return "class body for " + (containingDeclaration?.getDebugText() ?: "...")
    }

    override fun visitPropertyAccessor(accessor: KtPropertyAccessor, data: Unit?): String? {
        val containingProperty = KtStubbedPsiUtil.getContainingDeclaration(accessor, KtProperty::class.java)
        val what = (if (accessor.isGetter()) "getter" else "setter")
        return what + " for " + (if (containingProperty != null) containingProperty.getDebugText() else "...")
    }

    override fun visitClass(klass: KtClass, data: Unit?): String? {
        return buildText {
            append("STUB: ")
            appendInn(klass.getModifierList(), suffix = " ")
            append("class ")
            appendInn(klass.getNameAsName())
            appendInn(klass.getTypeParameterList())
            appendInn(klass.getPrimaryConstructorModifierList(), prefix = " ", suffix = " ")
            appendInn(klass.getPrimaryConstructorParameterList())
            appendInn(klass.getSuperTypeList(), prefix = " : ")
        }
    }

    override fun visitNamedFunction(function: KtNamedFunction, data: Unit?): String? {
        return buildText {
            append("STUB: ")
            appendInn(function.getModifierList(), suffix = " ")
            append("fun ")

            val typeParameterList = function.getTypeParameterList()
            if (function.hasTypeParameterListBeforeFunctionName()) {
                appendInn(typeParameterList, suffix = " ")
            }
            appendInn(function.getReceiverTypeReference(), suffix = ".")
            appendInn(function.getNameAsName())
            if (!function.hasTypeParameterListBeforeFunctionName()) {
                appendInn(typeParameterList)
            }
            appendInn(function.getValueParameterList())
            appendInn(function.getTypeReference(), prefix = ": ")
            appendInn(function.getTypeConstraintList(), prefix = " ")
        }
    }

    override fun visitObjectDeclaration(declaration: KtObjectDeclaration, data: Unit?): String? {
        return buildText {
            append("STUB: ")
            appendInn(declaration.getModifierList(), suffix = " ")
            append("object ")
            appendInn(declaration.getNameAsName())
            appendInn(declaration.getSuperTypeList(), prefix = " : ")
        }
    }

    override fun visitParameter(parameter: KtParameter, data: Unit?): String? {
        return buildText {
            if (parameter.hasValOrVar()) {
                if (parameter.isMutable()) append("var ") else append("val ")
            }
            val name = parameter.getNameAsName()
            appendInn(name)
            val typeReference = parameter.getTypeReference()
            if (typeReference != null && name != null) {
                append(": ")
            }
            appendInn(typeReference)
        }
    }

    override fun visitProperty(property: KtProperty, data: Unit?): String? {
        return buildText {
            append("STUB: ")
            appendInn(property.getModifierList(), suffix = " ")
            append(if (property.isVar()) "var " else "val ")
            appendInn(property.getNameAsName())
            appendInn(property.getTypeReference(), prefix = ": ")
        }
    }

    override fun visitTypeConstraint(constraint: KtTypeConstraint, data: Unit?): String? {
        return buildText {
            appendInn(constraint.getSubjectTypeParameterName())
            appendInn(constraint.getBoundTypeReference(), prefix = " : ")
        }
    }

    fun buildText(body: StringBuilder.() -> Unit): String? {
        val sb = StringBuilder()
        sb.body()
        return sb.toString()
    }

    fun renderChildren(element: KtElementImplStub<*>, separator: String, prefix: String = "", postfix: String = ""): String? {
        val childrenTexts = element.stub?.childrenStubs?.mapNotNull { (it?.psi as? KtElement)?.getDebugText() }
        return childrenTexts?.joinToString(separator, prefix, postfix) ?: element.text
    }

    fun render(element: KtElementImplStub<*>, vararg relevantChildren: KtElement?): String? {
        if (element.stub == null) return element.text
        return relevantChildren.filterNotNull().map { it.getDebugText() }.joinToString("", "", "")
    }
}

private fun StringBuilder.appendInn(target: Any?, prefix: String = "", suffix: String = "") {
    if (target == null) return
    append(prefix)
    append(when (target) {
               is KtElement -> target.getDebugText()
               else -> target.toString()
           })
    append(suffix)
}
