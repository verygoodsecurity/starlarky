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
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.visitor.*;
import proguard.classfile.util.SimplifiedVisitor;
import proguard.evaluation.ConstantValueFactory;
import proguard.evaluation.value.*;

/**
 * This class stores some optimization information that can be attached to
 * a field.
 *
 * @author Eric Lafortune
 */
public class FieldOptimizationInfo
extends      SimplifiedVisitor
implements   AttributeVisitor
{
    private static final ParticularValueFactory VALUE_FACTORY          = new ParticularValueFactory();
    private static final ConstantValueFactory   CONSTANT_VALUE_FACTORY = new ConstantValueFactory(VALUE_FACTORY);
    private static final InitialValueFactory    INITIAL_VALUE_FACTORY  = new InitialValueFactory(VALUE_FACTORY);

    private boolean        isWritten;
    private boolean        isRead;
    private boolean        canBeMadePrivate = true;
    private ReferenceValue referencedClass;
    private Value          value;


    public FieldOptimizationInfo(Clazz clazz, Field field)
    {
        int accessFlags = field.getAccessFlags();

        isWritten =
        isRead    = (accessFlags & ClassConstants.ACC_VOLATILE) != 0;

        resetValue(clazz, field);
    }


    public FieldOptimizationInfo(FieldOptimizationInfo FieldOptimizationInfo)
    {
        this.isWritten        = FieldOptimizationInfo.isWritten;
        this.isRead           = FieldOptimizationInfo.isRead;
        this.canBeMadePrivate = FieldOptimizationInfo.canBeMadePrivate;
        this.referencedClass  = FieldOptimizationInfo.referencedClass;
        this.value            = FieldOptimizationInfo.value;
    }


    public void setWritten()
    {
        isWritten = true;
    }


    public boolean isWritten()
    {
        return isWritten;
    }


    public void setRead()
    {
        isRead = true;
    }


    public boolean isRead()
    {
        return isRead;
    }


    public void setCanNotBeMadePrivate()
    {
        canBeMadePrivate = false;
    }


    public boolean canBeMadePrivate()
    {
        return canBeMadePrivate;
    }


    public void generalizeReferencedClass(ReferenceValue referencedClass)
    {
        this.referencedClass = this.referencedClass != null ?
            this.referencedClass.generalize(referencedClass) :
            referencedClass;
    }


    public ReferenceValue getReferencedClass()
    {
        return referencedClass;
    }


    public void resetValue(Clazz clazz, Field field)
    {
        int accessFlags = field.getAccessFlags();

        value = null;

        // See if we can initialize a static field with a constant value.
        if ((accessFlags & ClassConstants.ACC_STATIC) != 0)
        {
            field.accept(clazz, new AllAttributeVisitor(this));
        }

        // Otherwise initialize a non-final field with the default value.
        // Conservatively, even a final field needs to be initialized with the
        // default value, because it may be accessed before it is set.
        if (value == null &&
            (SideEffectInstructionChecker.OPTIMIZE_CONSERVATIVELY ||
             (accessFlags & ClassConstants.ACC_FINAL) == 0))
        {
            value = INITIAL_VALUE_FACTORY.createValue(field.getDescriptor(clazz));
        }
    }


    public void generalizeValue(Value value)
    {
        this.value = this.value != null ?
            this.value.generalize(value) :
            value;
    }


    public Value getValue()
    {
        return value;
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    public void visitConstantValueAttribute(Clazz clazz, Field field, ConstantValueAttribute constantValueAttribute)
    {
        // Retrieve the initial static field value.
        value = CONSTANT_VALUE_FACTORY.constantValue(clazz, constantValueAttribute.u2constantValueIndex);
    }


    // Small utility methods.

    public static void setFieldOptimizationInfo(Clazz clazz, Field field)
    {
        field.setVisitorInfo(new FieldOptimizationInfo(clazz, field));
    }


    public static FieldOptimizationInfo getFieldOptimizationInfo(Field field)
    {
        Object visitorInfo = field.getVisitorInfo();

        return visitorInfo instanceof FieldOptimizationInfo ?
            (FieldOptimizationInfo)visitorInfo :
            null;
    }
}
