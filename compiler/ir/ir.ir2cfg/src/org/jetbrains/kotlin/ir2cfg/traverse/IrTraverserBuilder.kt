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

package org.jetbrains.kotlin.ir2cfg.traverse

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.expressions.IrWhen

fun IrElement.traverser(atStart: Boolean = true): IrTraverser = when(this) {
    is IrFunction -> IrFunctionTraverser(this)
    is IrStatementContainer -> IrContainerTraverser(this, atStart)
    is IrWhen -> IrWhenTraverser(this, atStart)
    is IrStatement -> IrStatementTraverser(this)
    else -> IrSimpleTraverser()
}

fun IrElement.traverserAtStart() = traverser(atStart = true)

fun IrElement.traverserAtEnd() = traverser(atStart = false)

fun IrTraverser?.hasNext(): Boolean = this?.hasNext() ?: false

fun IrTraverser?.hasPrevious(): Boolean = this?.hasPrevious() ?: false

