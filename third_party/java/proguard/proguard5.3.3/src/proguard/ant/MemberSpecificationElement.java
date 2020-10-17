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
package proguard.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;
import proguard.MemberSpecification;
import proguard.classfile.*;
import proguard.classfile.util.ClassUtil;
import proguard.util.ListUtil;

import java.util.*;

/**
 * This DataType represents a class member specification in Ant.
 *
 * @author Eric Lafortune
 */
public class MemberSpecificationElement extends DataType
{
    private String access;
    private String annotation;
    private String type;
    private String name;
    private String parameters;


    /**
     * Adds the contents of this class member specification element to the given
     * list.
     * @param memberSpecifications the class member specifications to be
     *                                  extended.
     * @param isMethod                  specifies whether this specification
     *                                  refers to a method.
     * @param isConstructor             specifies whether this specification
     *                                  refers to a constructor.
     */
    public void appendTo(List    memberSpecifications,
                         boolean isMethod,
                         boolean isConstructor)
    {
        // Get the referenced file set, or else this one.
        MemberSpecificationElement memberSpecificationElement = isReference() ?
            (MemberSpecificationElement)getCheckedRef(this.getClass(),
                                                      this.getClass().getName()) :
            this;

        // Create a new class member specification.
        String access     = memberSpecificationElement.access;
        String type       = memberSpecificationElement.type;
        String annotation = memberSpecificationElement.annotation;
        String name       = memberSpecificationElement.name;
        String parameters = memberSpecificationElement.parameters;

        // Perform some basic conversions and checks on the attributes.
        if (annotation != null)
        {
            annotation = ClassUtil.internalType(annotation);
        }

        if (isMethod)
        {
            if (isConstructor)
            {
                if (type != null)
                {
                    throw new BuildException("Type attribute not allowed in constructor specification ["+type+"]");
                }

                if (parameters != null)
                {
                    type = JavaConstants.TYPE_VOID;
                }

                name = ClassConstants.METHOD_NAME_INIT;
            }
            else if ((type != null) ^ (parameters != null))
            {
                throw new BuildException("Type and parameters attributes must always be present in combination in method specification");
            }
        }
        else
        {
            if (parameters != null)
            {
                throw new BuildException("Parameters attribute not allowed in field specification ["+parameters+"]");
            }
        }

        List parameterList = ListUtil.commaSeparatedList(parameters);

        String descriptor =
            parameters != null ? ClassUtil.internalMethodDescriptor(type, parameterList) :
            type       != null ? ClassUtil.internalType(type)                            :
                                 null;

        MemberSpecification memberSpecification =
            new MemberSpecification(requiredAccessFlags(true,  access),
                                    requiredAccessFlags(false, access),
                                    annotation,
                                    name,
                                    descriptor);

        // Add it to the list.
        memberSpecifications.add(memberSpecification);
    }


    // Ant task attributes.

    public void setAccess(String access)
    {
        this.access = access;
    }


    public void setAnnotation(String annotation)
    {
        this.annotation = annotation;
    }


    public void setType(String type)
    {
        this.type = type;
    }


    public void setName(String name)
    {
        this.name = name;
    }


    public void setParameters(String parameters)
    {
        this.parameters = parameters;
    }


    /**
     * @deprecated Use {@link #setParameters(String)} instead.
     */
    public void setParam(String parameters)
    {
        this.parameters = parameters;
    }


    // Small utility methods.

    private int requiredAccessFlags(boolean set,
                                    String  access)
    throws BuildException
    {
        int accessFlags = 0;

        if (access != null)
        {
            StringTokenizer tokenizer = new StringTokenizer(access, " ,");
            while (tokenizer.hasMoreTokens())
            {
                String token = tokenizer.nextToken();

                if (token.startsWith("!") ^ set)
                {
                    String strippedToken = token.startsWith("!") ?
                        token.substring(1) :
                        token;

                    int accessFlag =
                        strippedToken.equals(JavaConstants.ACC_PUBLIC)       ? ClassConstants.ACC_PUBLIC       :
                        strippedToken.equals(JavaConstants.ACC_PRIVATE)      ? ClassConstants.ACC_PRIVATE      :
                        strippedToken.equals(JavaConstants.ACC_PROTECTED)    ? ClassConstants.ACC_PROTECTED    :
                        strippedToken.equals(JavaConstants.ACC_STATIC)       ? ClassConstants.ACC_STATIC       :
                        strippedToken.equals(JavaConstants.ACC_FINAL)        ? ClassConstants.ACC_FINAL        :
                        strippedToken.equals(JavaConstants.ACC_SYNCHRONIZED) ? ClassConstants.ACC_SYNCHRONIZED :
                        strippedToken.equals(JavaConstants.ACC_VOLATILE)     ? ClassConstants.ACC_VOLATILE     :
                        strippedToken.equals(JavaConstants.ACC_TRANSIENT)    ? ClassConstants.ACC_TRANSIENT    :
                        strippedToken.equals(JavaConstants.ACC_BRIDGE)       ? ClassConstants.ACC_BRIDGE       :
                        strippedToken.equals(JavaConstants.ACC_VARARGS)      ? ClassConstants.ACC_VARARGS      :
                        strippedToken.equals(JavaConstants.ACC_NATIVE)       ? ClassConstants.ACC_NATIVE       :
                        strippedToken.equals(JavaConstants.ACC_ABSTRACT)     ? ClassConstants.ACC_ABSTRACT     :
                        strippedToken.equals(JavaConstants.ACC_STRICT)       ? ClassConstants.ACC_STRICT       :
                        strippedToken.equals(JavaConstants.ACC_SYNTHETIC)    ? ClassConstants.ACC_SYNTHETIC    :
                                                                               0;

                    if (accessFlag == 0)
                    {
                        throw new BuildException("Incorrect class member access modifier ["+strippedToken+"]");
                    }

                    accessFlags |= accessFlag;
                }
            }
        }

        return accessFlags;
    }
}
