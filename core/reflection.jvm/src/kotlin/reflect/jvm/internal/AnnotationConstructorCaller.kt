/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.load.java.structure.reflect.wrapperByPrimitive
import java.lang.reflect.Proxy
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KClass
import kotlin.reflect.KotlinReflectionInternalError
import java.lang.reflect.Method as ReflectMethod

internal class AnnotationConstructorCaller(
        private val jClass: Class<*>,
        private val parameterNames: List<String>,
        private val areOptionalArgumentsAllowed: Boolean,
        methods: List<ReflectMethod> = parameterNames.map { name -> jClass.getDeclaredMethod(name) }
) : FunctionCaller<Nothing?>(
        null, jClass, null, methods.map { it.genericReturnType }.toTypedArray() // TODO: test javaType for annotation constructor parameter
) {
    // Transform primitive int to java.lang.Integer because actual arguments passed here will be boxed and Class#isInstance should succeed
    private val erasedParameterTypes: List<Class<*>> = methods.map { method -> method.returnType.let { it.wrapperByPrimitive ?: it } }

    // TODO: get rid of transformation Class -> KClass and Array<Class> -> Array<KClass> here and back in call
    @Suppress("UNCHECKED_CAST")
    private val defaultValues: List<Any?> = methods.map { method ->
        val value = method.defaultValue
        when {
            value is Class<*> -> Reflection.getOrCreateKotlinClass(value)
            value is Array<*> && value.isArrayOf<Class<*>>() -> Reflection.getOrCreateKotlinClasses(value as Array<Class<*>>)
            else -> value
        }
    }

    override fun call(args: Array<*>): Any? {
        // TODO: test?
        checkArguments(args)

        val values = args.mapIndexed { index, arg ->
            val value = if (arg == null && areOptionalArgumentsAllowed) defaultValues[index] else arg
            val transformed = value.transformKotlinToJvm(erasedParameterTypes[index])
            transformed ?: throwIllegalArgumentType(index, parameterNames[index], erasedParameterTypes[index])
        }

        return createAnnotationInstance(jClass, parameterNames.zip(values).toMap())
    }
}

/**
 * Transforms a Kotlin value to the one required by the JVM, e.g. KClass<*> -> Class<*> or Array<KClass<*>> -> Array<Class<*>>.
 * Returns `null` in case when no transformation is possible (an argument of an incorrect type was passed).
 */
private fun Any?.transformKotlinToJvm(expectedType: Class<*>): Any? {
    @Suppress("UNCHECKED_CAST")
    val result = when (this) {
        is Class<*> -> return null
        is KClass<*> -> this.java
        is Array<*> -> when {
            this.isArrayOf<Class<*>>() -> return null
            this.isArrayOf<KClass<*>>() -> (this as Array<KClass<*>>).map(KClass<*>::java).toTypedArray()
            else -> this
        }
        else -> this
    }

    return if (expectedType.isInstance(result)) result else null
}

private fun throwIllegalArgumentType(index: Int, name: String, expectedJvmType: Class<*>): Nothing {
    // TODO: message should read "... of the required type kotlin.reflect.KClass<...>" when erased type = Class<*>
    throw IllegalArgumentException("Argument #$index $name is not of the required type ${expectedJvmType.kotlin.qualifiedName}")
}

private fun createAnnotationInstance(annotationClass: Class<*>, values: Map<String, Any>): Any {
    return Proxy.newProxyInstance(annotationClass.classLoader /* TODO: test */, arrayOf(annotationClass)) { proxy, method, args ->
        // TODO: support equals, hashCode, toString, annotationType
        values[method.name] ?: throw KotlinReflectionInternalError("Method is not supported: $method (args: ${args.orEmpty().toList()})")
    }
}
