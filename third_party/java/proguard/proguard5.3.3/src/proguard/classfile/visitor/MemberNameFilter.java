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
import proguard.util.*;


/**
 * This <code>MemberVisitor</code> delegates its visits to another given
 * <code>MemberVisitor</code>, but only when the visited member
 * has a name that matches a given regular expression.
 *
 * @author Eric Lafortune
 */
public class MemberNameFilter implements MemberVisitor
{
    private final StringMatcher regularExpressionMatcher;
    private final MemberVisitor memberVisitor;


    /**
     * Creates a new MemberNameFilter.
     * @param regularExpression the regular expression against which member
     *                          names will be matched.
     * @param memberVisitor     the <code>MemberVisitor</code> to which visits
     *                          will be delegated.
     */
    public MemberNameFilter(String        regularExpression,
                            MemberVisitor memberVisitor)
    {
        this(new ListParser(new NameParser()).parse(regularExpression),
             memberVisitor);
    }


    /**
     * Creates a new MemberNameFilter.
     * @param regularExpressionMatcher the regular expression against which
     *                                 member names will be matched.
     * @param memberVisitor            the <code>MemberVisitor</code> to which
     *                                 visits will be delegated.
     */
    public MemberNameFilter(StringMatcher regularExpressionMatcher,
                            MemberVisitor memberVisitor)
    {
        this.regularExpressionMatcher = regularExpressionMatcher;
        this.memberVisitor            = memberVisitor;
    }


    // Implementations for MemberVisitor.

    public void visitProgramField(ProgramClass programClass, ProgramField programField)
    {
        if (accepted(programField.getName(programClass)))
        {
            memberVisitor.visitProgramField(programClass, programField);
        }
    }


    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        if (accepted(programMethod.getName(programClass)))
        {
            memberVisitor.visitProgramMethod(programClass, programMethod);
        }
    }


    public void visitLibraryField(LibraryClass libraryClass, LibraryField libraryField)
    {
        if (accepted(libraryField.getName(libraryClass)))
        {
            memberVisitor.visitLibraryField(libraryClass, libraryField);
        }
    }


    public void visitLibraryMethod(LibraryClass libraryClass, LibraryMethod libraryMethod)
    {
        if (accepted(libraryMethod.getName(libraryClass)))
        {
            memberVisitor.visitLibraryMethod(libraryClass, libraryMethod);
        }
    }


    // Small utility methods.

    private boolean accepted(String name)
    {
        return regularExpressionMatcher.matches(name);
    }
}
