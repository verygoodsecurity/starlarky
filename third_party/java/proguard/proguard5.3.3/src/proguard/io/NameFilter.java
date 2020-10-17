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

import proguard.util.*;

import java.util.List;

/**
 * This DataEntryReader delegates to one of two other DataEntryReader instances,
 * depending on the name of the data entry.
 *
 * @author Eric Lafortune
 */
public class NameFilter extends FilteredDataEntryReader
{
    /**
     * Creates a new NameFilter that delegates to the given reader, depending
     * on the given list of filters.
     */
    public NameFilter(String          regularExpression,
                      DataEntryReader acceptedDataEntryReader)
    {
        this(regularExpression, acceptedDataEntryReader, null);
    }


    /**
     * Creates a new NameFilter that delegates to either of the two given
     * readers, depending on the given list of filters.
     */
    public NameFilter(String          regularExpression,
                      DataEntryReader acceptedDataEntryReader,
                      DataEntryReader rejectedDataEntryReader)
    {
        super(new DataEntryNameFilter(new ListParser(new FileNameParser()).parse(regularExpression)),
              acceptedDataEntryReader,
              rejectedDataEntryReader);
    }


    /**
     * Creates a new NameFilter that delegates to the given reader, depending
     * on the given list of filters.
     */
    public NameFilter(List            regularExpressions,
                      DataEntryReader acceptedDataEntryReader)
    {
        this(regularExpressions, acceptedDataEntryReader, null);
    }


    /**
     * Creates a new NameFilter that delegates to either of the two given
     * readers, depending on the given list of filters.
     */
    public NameFilter(List            regularExpressions,
                      DataEntryReader acceptedDataEntryReader,
                      DataEntryReader rejectedDataEntryReader)
    {
        super(new DataEntryNameFilter(new ListParser(new FileNameParser()).parse(regularExpressions)),
              acceptedDataEntryReader,
              rejectedDataEntryReader);
    }
}