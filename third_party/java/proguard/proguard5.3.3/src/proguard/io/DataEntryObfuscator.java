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
package proguard.io;

import proguard.classfile.*;
import proguard.classfile.util.ClassUtil;

import java.io.IOException;
import java.util.Map;

/**
 * This DataEntryReader delegates to another DataEntryReader, renaming the
 * data entries based on the renamed classes in the given ClassPool.
 *
 * @author Eric Lafortune
 */
public class DataEntryObfuscator implements DataEntryReader
{
    private final ClassPool       classPool;
    private final Map             packagePrefixMap;
    private final DataEntryReader dataEntryReader;


    /**
     * Creates a new DataEntryObfuscator.
     * @param classPool        the class pool that maps from old names to new
     *                         names.
     * @param packagePrefixMap the map from old package prefixes to new package
     *                         prefixes.
     * @param dataEntryReader  the DataEntryReader to which calls will be
     *                         delegated.
     */
    public DataEntryObfuscator(ClassPool       classPool,
                               Map             packagePrefixMap,
                               DataEntryReader dataEntryReader)
    {
        this.classPool        = classPool;
        this.packagePrefixMap = packagePrefixMap;
        this.dataEntryReader  = dataEntryReader;
    }


    // Implementations for DataEntryReader.

    public void read(DataEntry dataEntry) throws IOException
    {
        // Delegate to the actual data entry reader.
        dataEntryReader.read(renamedDataEntry(dataEntry));
    }


    /**
     * Create a renamed data entry, if possible.
     */
    private DataEntry renamedDataEntry(DataEntry dataEntry)
    {
        String dataEntryName = dataEntry.getName();

        // Try to find a corresponding class name by removing increasingly
        // long suffixes.
        for (int suffixIndex = dataEntryName.length() - 1;
             suffixIndex > 0;
             suffixIndex--)
        {
            char c = dataEntryName.charAt(suffixIndex);
            if (!Character.isLetterOrDigit(c))
            {
                // Chop off the suffix.
                String className = dataEntryName.substring(0, suffixIndex);

                // Did we get to the package separator?
                if (c == ClassConstants.PACKAGE_SEPARATOR)
                {
                    break;
                }

                // Is there a class corresponding to the data entry?
                Clazz clazz = classPool.getClass(className);
                if (clazz != null)
                {
                    // Did the class get a new name?
                    String newClassName = clazz.getName();
                    if (!className.equals(newClassName))
                    {
                        // Return a renamed data entry.
                        String newDataEntryName =
                            newClassName + dataEntryName.substring(suffixIndex);

                        return new RenamedDataEntry(dataEntry, newDataEntryName);
                    }
                    else
                    {
                        // Otherwise stop looking.
                        return dataEntry;
                    }
                }
            }
        }

        // Try to find a corresponding package name by increasingly removing
        // more subpackages.
        String packagePrefix = dataEntryName;
        do
        {
            // Chop off the class name or the last subpackage name.
            packagePrefix = ClassUtil.internalPackagePrefix(packagePrefix);

            // Is there a package corresponding to the package prefix?
            String newPackagePrefix = (String)packagePrefixMap.get(packagePrefix);
            if (newPackagePrefix != null)
            {
                // Did the package get a new name?
                if (!packagePrefix.equals(newPackagePrefix))
                {
                    // Return a renamed data entry.
                    String newDataEntryName =
                        newPackagePrefix + dataEntryName.substring(packagePrefix.length());

                    return new RenamedDataEntry(dataEntry, newDataEntryName);
                }
                else
                {
                    // Otherwise stop looking.
                    return dataEntry;
                }
            }
        }
        while (packagePrefix.length() > 0);

        return dataEntry;
    }
}
