/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2017 Eric Lafortune @ GuardSquare
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package proguard.optimize.info;

import proguard.classfile.*;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.constant.RefConstant;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.instruction.*;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.util.SimplifiedVisitor;

/**
 * This InstructionVisitor marks all methods that invoke super methods (other
 * than initializers) from the instructions that it visits.
 *
 * @author Eric Lafortune
 */
public class SuperInvocationMarker
extends      SimplifiedVisitor
implements   InstructionVisitor,
             ConstantVisitor
{
    private boolean invokesSuperMethods;


    // Implementations for InstructionVisitor.

    public void visitAnyInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, Instruction instruction) {}


    public void visitConstantInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, ConstantInstruction constantInstruction)
    {
        if (constantInstruction.opcode == InstructionConstants.OP_INVOKESPECIAL)
        {
            invokesSuperMethods = false;

            clazz.constantPoolEntryAccept(constantInstruction.constantIndex, this);

            if (invokesSuperMethods)
            {
                setInvokesSuperMethods(method);
            }
        }
    }


    // Implementations for ConstantVisitor.

    public void visitAnyMethodrefConstant(Clazz clazz, RefConstant refConstant)
    {
        invokesSuperMethods =
            !clazz.equals(refConstant.referencedClass) &&
            !refConstant.getName(clazz).equals(ClassConstants.METHOD_NAME_INIT);
    }


    // Small utility methods.

    private static void setInvokesSuperMethods(Method method)
    {
        MethodOptimizationInfo info = MethodOptimizationInfo.getMethodOptimizationInfo(method);
        if (info != null)
        {
            info.setInvokesSuperMethods();
        }
    }


    public static boolean invokesSuperMethods(Method method)
    {
        MethodOptimizationInfo info = MethodOptimizationInfo.getMethodOptimizationInfo(method);
        return info == null || info.invokesSuperMethods();
    }
}
