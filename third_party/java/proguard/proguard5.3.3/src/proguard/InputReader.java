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
package proguard;

import proguard.classfile.ClassPool;
import proguard.classfile.util.WarningPrinter;
import proguard.classfile.visitor.*;
import proguard.io.*;

import java.io.IOException;

/**
 * This class reads the input class files.
 *
 * @author Eric Lafortune
 */
public class InputReader
{
    private final Configuration configuration;


    /**
     * Creates a new InputReader to read input class files as specified by the
     * given configuration.
     */
    public InputReader(Configuration configuration)
    {
        this.configuration = configuration;
    }


    /**
     * Fills the given program class pool and library class pool by reading
     * class files, based on the current configuration.
     */
    public void execute(ClassPool programClassPool,
                        ClassPool libraryClassPool) throws IOException
    {
        WarningPrinter warningPrinter = new WarningPrinter(System.err, configuration.warn);
        WarningPrinter notePrinter    = new WarningPrinter(System.out, configuration.note);

        DuplicateClassPrinter duplicateClassPrinter = new DuplicateClassPrinter(notePrinter);

        // Read the program class files.
        // Prepare a data entry reader to filter all classes,
        // which are then decoded to classes by a class reader,
        // which are then put in the class pool by a class pool filler.
        readInput("Reading program ",
                  configuration.programJars,
                  new ClassFilter(
                  new ClassReader(false,
                                  configuration.skipNonPublicLibraryClasses,
                                  configuration.skipNonPublicLibraryClassMembers,
                                  warningPrinter,
                  new ClassPresenceFilter(programClassPool, duplicateClassPrinter,
                  new ClassPoolFiller(programClassPool)))));

        // Check if we have at least some input classes.
        if (programClassPool.size() == 0)
        {
            throw new IOException("The input doesn't contain any classes. Did you specify the proper '-injars' options?");
        }

        // Read the library class files, if any.
        if (configuration.libraryJars != null)
        {
            // Prepare a data entry reader to filter all classes,
            // which are then decoded to classes by a class reader,
            // which are then put in the class pool by a class pool filler.
            readInput("Reading library ",
                      configuration.libraryJars,
                      new ClassFilter(
                      new ClassReader(true,
                                      configuration.skipNonPublicLibraryClasses,
                                      configuration.skipNonPublicLibraryClassMembers,
                                      warningPrinter,
                      new ClassPresenceFilter(programClassPool, duplicateClassPrinter,
                      new ClassPresenceFilter(libraryClassPool, duplicateClassPrinter,
                      new ClassPoolFiller(libraryClassPool))))));
        }

        // Print out a summary of the notes, if necessary.
        int noteCount = notePrinter.getWarningCount();
        if (noteCount > 0)
        {
            System.err.println("Note: there were " + noteCount +
                               " duplicate class definitions.");
            System.err.println("      (http://proguard.sourceforge.net/manual/troubleshooting.html#duplicateclass)");
        }

        // Print out a summary of the warnings, if necessary.
        int warningCount = warningPrinter.getWarningCount();
        if (warningCount > 0)
        {
            System.err.println("Warning: there were " + warningCount +
                               " classes in incorrectly named files.");
            System.err.println("         You should make sure all file names correspond to their class names.");
            System.err.println("         The directory hierarchies must correspond to the package hierarchies.");
            System.err.println("         (http://proguard.sourceforge.net/manual/troubleshooting.html#unexpectedclass)");

            if (!configuration.ignoreWarnings)
            {
                System.err.println("         If you don't mind the mentioned classes not being written out,");
                System.err.println("         you could try your luck using the '-ignorewarnings' option.");
                throw new IOException("Please correct the above warnings first.");
            }
        }
    }


    /**
     * Reads all input entries from the given class path.
     */
    private void readInput(String          messagePrefix,
                           ClassPath       classPath,
                           DataEntryReader reader) throws IOException
    {
        readInput(messagePrefix,
                  classPath,
                  0,
                  classPath.size(),
                  reader);
    }


    /**
     * Reads all input entries from the given section of the given class path.
     */
    public void readInput(String          messagePrefix,
                          ClassPath       classPath,
                          int             fromIndex,
                          int             toIndex,
                          DataEntryReader reader) throws IOException
    {
        for (int index = fromIndex; index < toIndex; index++)
        {
            ClassPathEntry entry = classPath.get(index);
            if (!entry.isOutput())
            {
                readInput(messagePrefix, entry, reader);
            }
        }
    }


    /**
     * Reads the given input class path entry.
     */
    private void readInput(String          messagePrefix,
                           ClassPathEntry  classPathEntry,
                           DataEntryReader dataEntryReader) throws IOException
    {
        try
        {
            // Create a reader that can unwrap jars, wars, ears, and zips.
            DataEntryReader reader =
                DataEntryReaderFactory.createDataEntryReader(messagePrefix,
                                                             classPathEntry,
                                                             dataEntryReader);

            // Create the data entry pump.
            DirectoryPump directoryPump =
                new DirectoryPump(classPathEntry.getFile());

            // Pump the data entries into the reader.
            directoryPump.pumpDataEntries(reader);
        }
        catch (IOException ex)
        {
            throw (IOException)new IOException("Can't read [" + classPathEntry + "] (" + ex.getMessage() + ")").initCause(ex);
        }
    }
}
