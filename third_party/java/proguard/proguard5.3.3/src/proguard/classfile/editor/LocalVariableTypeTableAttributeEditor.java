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
package proguard.classfile.editor;

import proguard.classfile.attribute.*;
import proguard.util.ArrayUtil;

/**
 * This class can add local variables to a given local variable type table
 * attribute.
 * Local variable types to be added must have been filled out beforehand.
 *
 * @author Eric Lafortune
 */
public class LocalVariableTypeTableAttributeEditor
{
    private final LocalVariableTypeTableAttribute targetLocalVariableTypeTableAttribute;


    /**
     * Creates a new LocalVariableTypeTableAttributeEditor that will edit local
     * variable types in the given local variable type table attribute.
     */
    public LocalVariableTypeTableAttributeEditor(LocalVariableTypeTableAttribute targetLocalVariableTypeTableAttribute)
    {
        this.targetLocalVariableTypeTableAttribute = targetLocalVariableTypeTableAttribute;
    }


    /**
     * Adds a given local variable type to the local variable type table attribute.
     */
    public void addLocalVariableTypeInfo(LocalVariableTypeInfo localVariableTypeInfo)
    {
        targetLocalVariableTypeTableAttribute.localVariableTypeTable =
            (LocalVariableTypeInfo[])ArrayUtil.add(targetLocalVariableTypeTableAttribute.localVariableTypeTable,
                                                   targetLocalVariableTypeTableAttribute.u2localVariableTypeTableLength++,
                                                   localVariableTypeInfo);
    }
}