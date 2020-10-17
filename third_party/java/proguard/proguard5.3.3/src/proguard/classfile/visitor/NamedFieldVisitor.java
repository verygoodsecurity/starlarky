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
package proguard.classfile.visitor;

import proguard.classfile.*;


/**
 * This class visits ProgramMember objects referring to fields, identified by
 * a name and descriptor pair.
 *
 * @author Eric Lafortune
 */
public class NamedFieldVisitor implements ClassVisitor
{
    private final String        name;
    private final String        descriptor;
    private final MemberVisitor memberVisitor;


    public NamedFieldVisitor(String        name,
                             String        descriptor,
                             MemberVisitor memberVisitor)
    {
        this.name          = name;
        this.descriptor    = descriptor;
        this.memberVisitor = memberVisitor;
    }


    // Implementations for ClassVisitor.

    public void visitProgramClass(ProgramClass programClass)
    {
        programClass.fieldAccept(name, descriptor, memberVisitor);
    }


    public void visitLibraryClass(LibraryClass libraryClass)
    {
        libraryClass.fieldAccept(name, descriptor, memberVisitor);
    }
}
