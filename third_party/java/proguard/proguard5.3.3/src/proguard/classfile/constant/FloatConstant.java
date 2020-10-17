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
package proguard.classfile.constant;

import proguard.classfile.*;
import proguard.classfile.constant.visitor.ConstantVisitor;

/**
 * This Constant represents a float constant in the constant pool.
 *
 * @author Eric Lafortune
 */
public class FloatConstant extends Constant
{
    public float f4value;


    /**
     * Creates an uninitialized FloatConstant.
     */
    public FloatConstant()
    {
    }


    /**
     * Creates a new FloatConstant with the given float value.
     */
    public FloatConstant(float value)
    {
        f4value = value;
    }


    /**
     * Returns the float value of this FloatConstant.
     */
    public float getValue()
    {
        return f4value;
    }


    /**
     * Sets the float value of this FloatConstant.
     */
    public void setValue(float value)
    {
        f4value = value;
    }


    // Implementations for Constant.

    public int getTag()
    {
        return ClassConstants.CONSTANT_Float;
    }

    public void accept(Clazz clazz, ConstantVisitor constantVisitor)
    {
        constantVisitor.visitFloatConstant(clazz, this);
    }
}
