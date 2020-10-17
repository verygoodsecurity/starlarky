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
package proguard.optimize.peephole;

import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.visitor.*;
import proguard.classfile.constant.*;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.editor.*;
import proguard.classfile.instruction.*;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.util.*;
import proguard.classfile.visitor.*;
import proguard.optimize.KeepMarker;
import proguard.optimize.info.*;

import java.util.*;

/**
 * This AttributeVisitor inlines short methods or methods that are only invoked
 * once, in the code attributes that it visits.
 *
 * @author Eric Lafortune
 */
public class MethodInliner
extends      SimplifiedVisitor
implements   AttributeVisitor,
             InstructionVisitor,
             ConstantVisitor,
             MemberVisitor,
             LineNumberInfoVisitor
{
    static final int METHOD_DUMMY_START_LINE_NUMBER = 0;
    static final int INLINED_METHOD_END_LINE_NUMBER = -1;

    private static final int MAXIMUM_INLINED_CODE_LENGTH       = Integer.parseInt(System.getProperty("maximum.inlined.code.length",      "8"));
    private static final int MAXIMUM_RESULTING_CODE_LENGTH_JSE = Integer.parseInt(System.getProperty("maximum.resulting.code.length", "7000"));
    private static final int MAXIMUM_RESULTING_CODE_LENGTH_JME = Integer.parseInt(System.getProperty("maximum.resulting.code.length", "2000"));

    //*
    private static final boolean DEBUG = false;
    /*/
    private static       boolean DEBUG = true;
    //*/


    private final boolean            microEdition;
    private final boolean            allowAccessModification;
    private final boolean            inlineSingleInvocations;
    private final InstructionVisitor extraInlinedInvocationVisitor;

    private final CodeAttributeComposer codeAttributeComposer = new CodeAttributeComposer();
    private final AccessMethodMarker    accessMethodMarker    = new AccessMethodMarker();
    private final CatchExceptionMarker  catchExceptionMarker  = new CatchExceptionMarker();
    private final StackSizeComputer     stackSizeComputer     = new StackSizeComputer();

    private ProgramClass       targetClass;
    private ProgramMethod      targetMethod;
    private ConstantAdder      constantAdder;
    private ExceptionInfoAdder exceptionInfoAdder;
    private int                estimatedResultingCodeLength;
    private boolean            inlining;
    private Stack              inliningMethods              = new Stack();
    private boolean            emptyInvokingStack;
    private int                uninitializedObjectCount;
    private int                variableOffset;
    private boolean            inlined;
    private boolean            inlinedAny;
    private boolean            copiedLineNumbers;
    private String             source;
    private int                minimumLineNumberIndex;


    /**
     * Creates a new MethodInliner.
     * @param microEdition            indicates whether the resulting code is
     *                                targeted at Java Micro Edition.
     * @param allowAccessModification indicates whether the access modifiers of
     *                                classes and class members can be changed
     *                                in order to inline methods.
     * @param inlineSingleInvocations indicates whether the single invocations
     *                                should be inlined, or, alternatively,
     *                                short methods.
     */
    public MethodInliner(boolean microEdition,
                         boolean allowAccessModification,
                         boolean inlineSingleInvocations)
    {
        this(microEdition,
             allowAccessModification,
             inlineSingleInvocations,
             null);
    }


    /**
     * Creates a new MethodInliner.
     * @param microEdition            indicates whether the resulting code is
     *                                targeted at Java Micro Edition.
     * @param allowAccessModification indicates whether the access modifiers of
     *                                classes and class members can be changed
     *                                in order to inline methods.
     * @param inlineSingleInvocations indicates whether the single invocations
     *                                should be inlined, or, alternatively,
     *                                short methods.
     * @param extraInlinedInvocationVisitor an optional extra visitor for all
     *                                      inlined invocation instructions.
     */
    public MethodInliner(boolean            microEdition,
                         boolean            allowAccessModification,
                         boolean            inlineSingleInvocations,
                         InstructionVisitor extraInlinedInvocationVisitor)
    {
        this.microEdition                  = microEdition;
        this.allowAccessModification       = allowAccessModification;
        this.inlineSingleInvocations       = inlineSingleInvocations;
        this.extraInlinedInvocationVisitor = extraInlinedInvocationVisitor;
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        // TODO: Remove this when the method inliner has stabilized.
        // Catch any unexpected exceptions from the actual visiting method.
        try
        {
            // Process the code.
            visitCodeAttribute0(clazz, method, codeAttribute);
        }
        catch (RuntimeException ex)
        {
            System.err.println("Unexpected error while inlining method:");
            System.err.println("  Target class   = ["+targetClass.getName()+"]");
            System.err.println("  Target method  = ["+targetMethod.getName(targetClass)+targetMethod.getDescriptor(targetClass)+"]");
            if (inlining)
            {
                System.err.println("  Inlined class  = ["+clazz.getName()+"]");
                System.err.println("  Inlined method = ["+method.getName(clazz)+method.getDescriptor(clazz)+"]");
            }
            System.err.println("  Exception      = ["+ex.getClass().getName()+"] ("+ex.getMessage()+")");

            ex.printStackTrace();
            System.err.println("Not inlining this method");

            if (DEBUG)
            {
                targetMethod.accept(targetClass, new ClassPrinter());
                if (inlining)
                {
                    method.accept(clazz, new ClassPrinter());
                }

                throw ex;
            }
        }
    }


    public void visitCodeAttribute0(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        if (!inlining)
        {
//            codeAttributeComposer.DEBUG = DEBUG =
//                clazz.getName().equals("abc/Def") &&
//                method.getName(clazz).equals("abc");

            targetClass                  = (ProgramClass)clazz;
            targetMethod                 = (ProgramMethod)method;
            constantAdder                = new ConstantAdder(targetClass);
            exceptionInfoAdder           = new ExceptionInfoAdder(targetClass, codeAttributeComposer);
            estimatedResultingCodeLength = codeAttribute.u4codeLength;
            inliningMethods.clear();
            uninitializedObjectCount     = method.getName(clazz).equals(ClassConstants.METHOD_NAME_INIT) ? 1 : 0;
            inlinedAny                   = false;
            codeAttributeComposer.reset();
            stackSizeComputer.visitCodeAttribute(clazz, method, codeAttribute);

            // Append the body of the code.
            copyCode(clazz, method, codeAttribute);

            targetClass   = null;
            targetMethod  = null;
            constantAdder = null;

            // Update the code attribute if any code has been inlined.
            if (inlinedAny)
            {
                codeAttributeComposer.visitCodeAttribute(clazz, method, codeAttribute);

                // Update the accessing flags.
                codeAttribute.instructionsAccept(clazz, method, accessMethodMarker);

                // Update the exception catching flags.
                catchExceptionMarker.visitCodeAttribute(clazz, method, codeAttribute);
            }
        }

        // Only inline the method if it is invoked once or if it is short.
        else if ((inlineSingleInvocations ?
                      MethodInvocationMarker.getInvocationCount(method) == 1 :
                      codeAttribute.u4codeLength <= MAXIMUM_INLINED_CODE_LENGTH) &&
                 estimatedResultingCodeLength + codeAttribute.u4codeLength <
                 (microEdition ?
                     MAXIMUM_RESULTING_CODE_LENGTH_JME :
                     MAXIMUM_RESULTING_CODE_LENGTH_JSE))
        {
            if (DEBUG)
            {
                System.out.println("MethodInliner: inlining ["+
                                   clazz.getName()+"."+method.getName(clazz)+method.getDescriptor(clazz)+"] in ["+
                                   targetClass.getName()+"."+targetMethod.getName(targetClass)+targetMethod.getDescriptor(targetClass)+"]");
            }

            // Ignore the removal of the original method invocation,
            // the addition of the parameter setup, and
            // the modification of a few inlined instructions.
            estimatedResultingCodeLength += codeAttribute.u4codeLength;

            // Append instructions to store the parameters.
            storeParameters(clazz, method);

            // Inline the body of the code.
            copyCode(clazz, method, codeAttribute);

            inlined    = true;
            inlinedAny = true;
        }
    }


    public void visitLineNumberTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LineNumberTableAttribute lineNumberTableAttribute)
    {
        // Remember the source if we're inlining a method.
        source = inlining ?
            clazz.getName()                                 + '.' +
            method.getName(clazz)                           +
            method.getDescriptor(clazz)                     + ':' +
            lineNumberTableAttribute.getLowestLineNumber()  + ':' +
            lineNumberTableAttribute.getHighestLineNumber() :
            null;

        // Insert all line numbers, possibly partly before previously inserted
        // line numbers.
        lineNumberTableAttribute.lineNumbersAccept(clazz, method, codeAttribute, this);

        copiedLineNumbers = true;
    }


    /**
     * Appends instructions to pop the parameters for the given method, storing
     * them in new local variables.
     */
    private void storeParameters(Clazz clazz, Method method)
    {
        String descriptor = method.getDescriptor(clazz);

        boolean isStatic =
            (method.getAccessFlags() & ClassConstants.ACC_STATIC) != 0;

        // Count the number of parameters, taking into account their categories.
        int parameterCount  = ClassUtil.internalMethodParameterCount(descriptor);
        int parameterSize   = ClassUtil.internalMethodParameterSize(descriptor);
        int parameterOffset = isStatic ? 0 : 1;

        // Store the parameter types.
        String[] parameterTypes = new String[parameterSize];

        InternalTypeEnumeration internalTypeEnumeration =
            new InternalTypeEnumeration(descriptor);

        for (int parameterIndex = 0; parameterIndex < parameterSize; parameterIndex++)
        {
            String parameterType = internalTypeEnumeration.nextType();
            parameterTypes[parameterIndex] = parameterType;
            if (ClassUtil.internalTypeSize(parameterType) == 2)
            {
                parameterIndex++;
            }
        }

        codeAttributeComposer.beginCodeFragment(parameterSize+1);

        // Go over the parameter types backward, storing the stack entries
        // in their corresponding variables.
        for (int parameterIndex = parameterSize-1; parameterIndex >= 0; parameterIndex--)
        {
            String parameterType = parameterTypes[parameterIndex];
            if (parameterType != null)
            {
                byte opcode;
                switch (parameterType.charAt(0))
                {
                    case ClassConstants.TYPE_BOOLEAN:
                    case ClassConstants.TYPE_BYTE:
                    case ClassConstants.TYPE_CHAR:
                    case ClassConstants.TYPE_SHORT:
                    case ClassConstants.TYPE_INT:
                        opcode = InstructionConstants.OP_ISTORE;
                        break;

                    case ClassConstants.TYPE_LONG:
                        opcode = InstructionConstants.OP_LSTORE;
                        break;

                    case ClassConstants.TYPE_FLOAT:
                        opcode = InstructionConstants.OP_FSTORE;
                        break;

                    case ClassConstants.TYPE_DOUBLE:
                        opcode = InstructionConstants.OP_DSTORE;
                        break;

                    default:
                        opcode = InstructionConstants.OP_ASTORE;
                        break;
                }

                codeAttributeComposer.appendInstruction(parameterSize-parameterIndex-1,
                                                        new VariableInstruction(opcode, variableOffset + parameterOffset + parameterIndex));
            }
        }

        // Put the 'this' reference in variable 0 (plus offset).
        if (!isStatic)
        {
            codeAttributeComposer.appendInstruction(parameterSize,
                                                    new VariableInstruction(InstructionConstants.OP_ASTORE, variableOffset));
        }

        codeAttributeComposer.endCodeFragment();
    }


    /**
     * Appends the code of the given code attribute.
     */
    private void copyCode(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        // The code may expand, due to expanding constant and variable
        // instructions.
        codeAttributeComposer.beginCodeFragment(codeAttribute.u4codeLength);

        // Copy the instructions.
        codeAttribute.instructionsAccept(clazz, method, this);

        // Append a label just after the code.
        codeAttributeComposer.appendLabel(codeAttribute.u4codeLength);

        // Copy the exceptions.
        codeAttribute.exceptionsAccept(clazz, method, exceptionInfoAdder);

        // Copy the line numbers.
        copiedLineNumbers = false;

        // The line numbers need to be inserted sequentially.
        minimumLineNumberIndex = 0;

        codeAttribute.attributesAccept(clazz, method, this);

        // Make sure we at least have some entry at the start of the method.
        if (!copiedLineNumbers)
        {
            String source = inlining ?
                clazz.getName()             + '.' +
                method.getName(clazz)       +
                method.getDescriptor(clazz) +
                ":0:0" :
                null;

            minimumLineNumberIndex =
                codeAttributeComposer.insertLineNumber(minimumLineNumberIndex,
                    new ExtendedLineNumberInfo(0,
                                               METHOD_DUMMY_START_LINE_NUMBER,
                                               source)) + 1;
        }

        // Add a marker at the end of an inlined method.
        // The marker will be corrected in LineNumberLinearizer,
        // so it points to the line of the enclosing method.
        if (inlining)
        {
            String source =
                clazz.getName()             + '.' +
                method.getName(clazz)       +
                method.getDescriptor(clazz) +
                ":0:0";

            minimumLineNumberIndex =
                codeAttributeComposer.insertLineNumber(minimumLineNumberIndex,
                    new ExtendedLineNumberInfo(codeAttribute.u4codeLength,
                                               INLINED_METHOD_END_LINE_NUMBER,
                                               source)) + 1;
        }

        codeAttributeComposer.endCodeFragment();
    }


    // Implementations for InstructionVisitor.

    public void visitAnyInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, Instruction instruction)
    {
        codeAttributeComposer.appendInstruction(offset, instruction);
    }


    public void visitSimpleInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SimpleInstruction simpleInstruction)
    {
        // Are we inlining this instruction?
        if (inlining)
        {
            // Replace any return instructions by branches to the end of the code.
            switch (simpleInstruction.opcode)
            {
                case InstructionConstants.OP_IRETURN:
                case InstructionConstants.OP_LRETURN:
                case InstructionConstants.OP_FRETURN:
                case InstructionConstants.OP_DRETURN:
                case InstructionConstants.OP_ARETURN:
                case InstructionConstants.OP_RETURN:
                    // Are we not at the last instruction?
                    if (offset < codeAttribute.u4codeLength-1)
                    {
                        // Replace the return instruction by a branch instruction.
                        Instruction branchInstruction =
                            new BranchInstruction(InstructionConstants.OP_GOTO_W,
                                                  codeAttribute.u4codeLength - offset);

                        codeAttributeComposer.appendInstruction(offset,
                                                                branchInstruction);
                    }
                    else
                    {
                        // Just leave out the instruction, but put in a label,
                        // for the sake of any other branch instructions.
                        codeAttributeComposer.appendLabel(offset);
                    }

                    return;
            }
        }

        codeAttributeComposer.appendInstruction(offset, simpleInstruction);
    }


    public void visitVariableInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, VariableInstruction variableInstruction)
    {
        // Are we inlining this instruction?
        if (inlining)
        {
            // Update the variable index.
            variableInstruction.variableIndex += variableOffset;
        }

        codeAttributeComposer.appendInstruction(offset, variableInstruction);
    }


    public void visitConstantInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, ConstantInstruction constantInstruction)
    {
        // Is it a method invocation?
        switch (constantInstruction.opcode)
        {
            case InstructionConstants.OP_NEW:
                uninitializedObjectCount++;
                break;

            case InstructionConstants.OP_INVOKEVIRTUAL:
            case InstructionConstants.OP_INVOKESPECIAL:
            case InstructionConstants.OP_INVOKESTATIC:
            case InstructionConstants.OP_INVOKEINTERFACE:
                // See if we can inline it.
                inlined = false;

                // Append a label, in case the invocation will be inlined.
                codeAttributeComposer.appendLabel(offset);

                emptyInvokingStack =
                    !inlining &&
                    stackSizeComputer.isReachable(offset) &&
                    stackSizeComputer.getStackSizeAfter(offset) == 0;

                variableOffset += codeAttribute.u2maxLocals;

                clazz.constantPoolEntryAccept(constantInstruction.constantIndex, this);

                variableOffset -= codeAttribute.u2maxLocals;

                // Was the method inlined?
                if (inlined)
                {
                    if (extraInlinedInvocationVisitor != null)
                    {
                        extraInlinedInvocationVisitor.visitConstantInstruction(clazz, method, codeAttribute, offset, constantInstruction);
                    }

                    // The invocation itself is no longer necessary.
                    return;
                }

                break;
        }

        // Are we inlining this instruction?
        if (inlining)
        {
            // Make sure the constant is present in the constant pool of the
            // target class.
            constantInstruction.constantIndex =
                constantAdder.addConstant(clazz, constantInstruction.constantIndex);
        }

        codeAttributeComposer.appendInstruction(offset, constantInstruction);
    }


    // Implementations for ConstantVisitor.

    public void visitAnyMethodrefConstant(Clazz clazz, RefConstant refConstant)
    {
        refConstant.referencedMemberAccept(this);
    }


    // Implementations for MemberVisitor.

    public void visitAnyMember(Clazz Clazz, Member member) {}


    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        int accessFlags = programMethod.getAccessFlags();

        if (// Don't inline methods that must be preserved.
            !KeepMarker.isKept(programMethod)                                                     &&

            // Only inline the method if it is private, static, or final.
            // This currently precludes default interface methods, because
            // they can't be final.
            (accessFlags & (ClassConstants.ACC_PRIVATE |
                            ClassConstants.ACC_STATIC  |
                            ClassConstants.ACC_FINAL)) != 0                                       &&

            // Only inline the method if it is not synchronized, etc.
            (accessFlags & (ClassConstants.ACC_SYNCHRONIZED |
                            ClassConstants.ACC_NATIVE       |
                            ClassConstants.ACC_ABSTRACT)) == 0                                    &&

            // Don't inline an <init> method, except in an <init> method in the
            // same class.
//            (!programMethod.getName(programClass).equals(ClassConstants.METHOD_NAME_INIT) ||
//             (programClass.equals(targetClass) &&
//              targetMethod.getName(targetClass).equals(ClassConstants.METHOD_NAME_INIT))) &&
            !programMethod.getName(programClass).equals(ClassConstants.METHOD_NAME_INIT) &&

            // Don't inline a method into itself.
            (!programMethod.equals(targetMethod) ||
             !programClass.equals(targetClass))                                                   &&

            // Only inline the method if it isn't recursing.
            !inliningMethods.contains(programMethod)                                              &&

            // Only inline the method if its target class has at least the
            // same version number as the source class, in order to avoid
            // introducing incompatible constructs.
            targetClass.u4version >= programClass.u4version                                       &&

            // Only inline the method if it doesn't invoke a super method or a
            // dynamic method, or if it is in the same class.
            (!SuperInvocationMarker.invokesSuperMethods(programMethod) &&
             !DynamicInvocationMarker.invokesDynamically(programMethod) ||
             programClass.equals(targetClass))                                                    &&

            // Only inline the method if it doesn't branch backward while there
            // are uninitialized objects.
            (!BackwardBranchMarker.branchesBackward(programMethod) ||
             uninitializedObjectCount == 0)                                                       &&

            // Only inline if the code access of the inlined method allows it.
            (allowAccessModification ||
             ((!AccessMethodMarker.accessesPrivateCode(programMethod) ||
               programClass.equals(targetClass)) &&

              (!AccessMethodMarker.accessesPackageCode(programMethod) ||
               ClassUtil.internalPackageName(programClass.getName()).equals(
               ClassUtil.internalPackageName(targetClass.getName())))))                           &&

//               (!AccessMethodMarker.accessesProtectedCode(programMethod) ||
//                targetClass.extends_(programClass) ||
//                targetClass.implements_(programClass)) ||
            (!AccessMethodMarker.accessesProtectedCode(programMethod) ||
             programClass.equals(targetClass))                                                    &&

            // Only inline the method if it doesn't catch exceptions, or if it
            // is invoked with an empty stack.
            (!CatchExceptionMarker.catchesExceptions(programMethod) ||
             emptyInvokingStack)                                                                  &&

            // Only inline the method if it always returns with an empty
            // stack.
            !NonEmptyStackReturnMarker.returnsWithNonEmptyStack(programMethod)                    &&

            // a subset of the initialized superclasses.
            ((accessFlags & ClassConstants.ACC_STATIC) == 0 ||
             programClass.equals(targetClass)                        ||
             initializedSuperClasses(targetClass).containsAll(initializedSuperClasses(programClass))))
        {
            boolean oldInlining = inlining;
            inlining = true;
            inliningMethods.push(programMethod);

            // Inline the method body.
            programMethod.attributesAccept(programClass, this);

            // Update the optimization information of the target method.
            MethodOptimizationInfo info =
                MethodOptimizationInfo.getMethodOptimizationInfo(targetMethod);
            if (info != null)
            {
                info.merge(MethodOptimizationInfo.getMethodOptimizationInfo(programMethod));
            }

            inlining = oldInlining;
            inliningMethods.pop();
        }
        else if (programMethod.getName(programClass).equals(ClassConstants.METHOD_NAME_INIT))
        {
            uninitializedObjectCount--;
        }
    }


    // Implementations for LineNumberInfoVisitor.

    public void visitLineNumberInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, LineNumberInfo lineNumberInfo)
    {
        try
        {
            String newSource = lineNumberInfo.getSource() != null ?
                lineNumberInfo.getSource() :
                source;

            LineNumberInfo newLineNumberInfo = newSource != null ?
                new ExtendedLineNumberInfo(lineNumberInfo.u2startPC,
                                           lineNumberInfo.u2lineNumber,
                                           newSource) :
                new LineNumberInfo(lineNumberInfo.u2startPC,
                                   lineNumberInfo.u2lineNumber);

            minimumLineNumberIndex =
                codeAttributeComposer.insertLineNumber(minimumLineNumberIndex, newLineNumberInfo) + 1;
        }
        catch (IllegalArgumentException e)
        {
            if (DEBUG)
            {
                System.err.println("Invalid line number while inlining method:");
                System.err.println("  Target class   = ["+targetClass.getName()+"]");
                System.err.println("  Target method  = ["+targetMethod.getName(targetClass)+targetMethod.getDescriptor(targetClass)+"]");
                if (inlining)
                {
                    System.err.println("  Inlined class  = ["+clazz.getName()+"]");
                    System.err.println("  Inlined method = ["+method.getName(clazz)+method.getDescriptor(clazz)+"]");
                }
                System.err.println("  Exception      = ["+e.getClass().getName()+"] ("+e.getMessage()+")");
            }
        }
    }


    /**
     * Returns the set of superclasses and interfaces that are initialized.
     */
    private Set initializedSuperClasses(Clazz clazz)
    {
        Set set = new HashSet();

        // Visit all superclasses and interfaces, collecting the ones that have
        // static initializers.
        clazz.hierarchyAccept(true, true, true, false,
                              new StaticInitializerContainingClassFilter(
                              new ClassCollector(set)));

        return set;
    }
}
