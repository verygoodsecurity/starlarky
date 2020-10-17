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
package proguard.shrink;

import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.util.*;
import proguard.classfile.visitor.*;

import java.io.PrintStream;


/**
 * This ClassVisitor     and MemberVisitor prints out the reasons why
 * classes and class members have been marked as being used.
 *
 * @see UsageMarker
 *
 * @author Eric Lafortune
 */
public class ShortestUsagePrinter
extends      SimplifiedVisitor
implements   ClassVisitor,
             MemberVisitor,
             AttributeVisitor
{
    private final ShortestUsageMarker shortestUsageMarker;
    private final boolean             verbose;
    private final PrintStream         ps;


    /**
     * Creates a new UsagePrinter that prints verbosely to <code>System.out</code>.
     * @param shortestUsageMarker the usage marker that was used to mark the
     *                            classes and class members.
     */
    public ShortestUsagePrinter(ShortestUsageMarker shortestUsageMarker)
    {
        this(shortestUsageMarker, true);
    }


    /**
     * Creates a new UsagePrinter that prints to the given stream.
     * @param shortestUsageMarker the usage marker that was used to mark the
     *                            classes and class members.
     * @param verbose             specifies whether the output should be verbose.
     */
    public ShortestUsagePrinter(ShortestUsageMarker shortestUsageMarker,
                                boolean             verbose)
    {
        this(shortestUsageMarker, verbose, System.out);
    }

    /**
     * Creates a new UsagePrinter that prints to the given stream.
     * @param shortestUsageMarker the usage marker that was used to mark the
     *                            classes and class members.
     * @param verbose             specifies whether the output should be verbose.
     * @param printStream         the stream to which to print.
     */
    public ShortestUsagePrinter(ShortestUsageMarker shortestUsageMarker,
                                boolean             verbose,
                                PrintStream         printStream)
    {
        this.shortestUsageMarker = shortestUsageMarker;
        this.verbose             = verbose;
        this.ps                  = printStream;
    }


    // Implementations for ClassVisitor.

    public void visitProgramClass(ProgramClass programClass)
    {
        // Print the name of this class.
        ps.println(ClassUtil.externalClassName(programClass.getName()));

        // Print the reason for keeping this class.
        printReason(programClass);
    }


    public void visitLibraryClass(LibraryClass libraryClass)
    {
        // Print the name of this class.
        ps.println(ClassUtil.externalClassName(libraryClass.getName()));

        // Print the reason for keeping this class.
        ps.println("  is a library class.\n");
    }


    // Implementations for MemberVisitor.

    public void visitProgramField(ProgramClass programClass, ProgramField programField)
    {
        // Print the name of this field.
        String name = programField.getName(programClass);
        String type = programField.getDescriptor(programClass);

        ps.println(ClassUtil.externalClassName(programClass.getName()) +
                   (verbose ?
                        ": " + ClassUtil.externalFullFieldDescription(0, name, type):
                        "."  + name));

        // Print the reason for keeping this method.
        printReason(programField);
    }


    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        // Print the name of this method.
        String name = programMethod.getName(programClass);
        String type = programMethod.getDescriptor(programClass);

        ps.print(ClassUtil.externalClassName(programClass.getName()) +
                 (verbose ?
                      ": " + ClassUtil.externalFullMethodDescription(programClass.getName(), 0, name, type):
                      "."  + name));
        programMethod.attributesAccept(programClass, this);
        ps.println();

        // Print the reason for keeping this method.
        printReason(programMethod);
    }


    public void visitLibraryField(LibraryClass libraryClass, LibraryField libraryField)
    {
        // Print the name of this field.
        String name = libraryField.getName(libraryClass);
        String type = libraryField.getDescriptor(libraryClass);

        ps.println(ClassUtil.externalClassName(libraryClass.getName()) +
                   (verbose ?
                        ": " + ClassUtil.externalFullFieldDescription(0, name, type):
                        "."  + name));

        // Print the reason for keeping this field.
        ps.println("  is a library field.\n");
    }


    public void visitLibraryMethod(LibraryClass libraryClass, LibraryMethod libraryMethod)
    {
        // Print the name of this method.
        String name = libraryMethod.getName(libraryClass);
        String type = libraryMethod.getDescriptor(libraryClass);

        ps.println(ClassUtil.externalClassName(libraryClass.getName()) +
                   (verbose ?
                        ": " + ClassUtil.externalFullMethodDescription(libraryClass.getName(), 0, name, type):
                        "."  + name));

        // Print the reason for keeping this method.
        ps.println("  is a library method.\n");
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        codeAttribute.attributesAccept(clazz, method, this);
    }


    public void visitLineNumberTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LineNumberTableAttribute lineNumberTableAttribute)
    {
        ps.print(" (" +
                 lineNumberTableAttribute.getLowestLineNumber() + ":" +
                 lineNumberTableAttribute.getHighestLineNumber() + ")");
    }


    // Small utility methods.

    private void printReason(VisitorAccepter visitorAccepter)
    {
        if (shortestUsageMarker.isUsed(visitorAccepter))
        {
            ShortestUsageMark shortestUsageMark = shortestUsageMarker.getShortestUsageMark(visitorAccepter);

            // Print the reason for keeping this class.
            ps.print("  " + shortestUsageMark.getReason());

            // Print the class or method that is responsible, with its reasons.
            shortestUsageMark.acceptClassVisitor(this);
            shortestUsageMark.acceptMemberVisitor(this);
        }
        else
        {
            ps.println("  is not being kept.\n");
        }
    }
}
