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
package proguard.classfile.attribute;

import proguard.classfile.*;
import proguard.classfile.attribute.visitor.AttributeVisitor;

/**
 * This Attribute represents a constant value attribute.
 *
 * @author Eric Lafortune
 */
public class ConstantValueAttribute extends Attribute
{
    public int u2constantValueIndex;


    /**
     * Creates an uninitialized ConstantValueAttribute.
     */
    public ConstantValueAttribute()
    {
    }


    /**
     * Creates an initialized ConstantValueAttribute.
     */
    public ConstantValueAttribute(int u2attributeNameIndex,
                                  int u2constantValueIndex)
    {
        super(u2attributeNameIndex);

        this.u2constantValueIndex = u2constantValueIndex;
    }

    
    // Implementations for Attribute.

    public void accept(Clazz clazz, Field field, AttributeVisitor attributeVisitor)
    {
        attributeVisitor.visitConstantValueAttribute(clazz, field, this);
    }
}
