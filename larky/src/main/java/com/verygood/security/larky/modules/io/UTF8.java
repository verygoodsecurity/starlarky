/*
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

*/
package com.verygood.security.larky.modules.io;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Utilities for working with UTF8 encoding and decoding.
 */
public final class UTF8
{
    public static final int MINIMUM_SERIALISED_LENGTH_BYTES = Integer.BYTES;

    public static byte[] encode( String string )
    {
        return string.getBytes( StandardCharsets.UTF_8 );
    }

    public static String decode( byte[] bytes )
    {
        return new String( bytes, StandardCharsets.UTF_8 );
    }

    public static String decode( byte[] bytes, int offset, int length )
    {
        return new String( bytes, offset, length, StandardCharsets.UTF_8 );
    }

    public static String getDecodedStringFrom( ByteBuffer source )
    {
        // Currently only one key is supported although the data format supports multiple
        int count = source.getInt();
        int remaining = source.remaining();
        if ( count > remaining )
        {
            throw badStringFormatException( count, remaining );
        }
        byte[] data = new byte[count];
        source.get( data );
        return UTF8.decode( data );
    }

    private static IllegalArgumentException badStringFormatException( int count, int remaining )
    {
        return new IllegalArgumentException(
                "Bad string format; claims string is " + count + " bytes long, " +
                "but only " + remaining + " bytes remain in buffer" );
    }

    public static void putEncodedStringInto( String text, ByteBuffer target )
    {
        byte[] data = encode( text );
        target.putInt( data.length );
        target.put( data );
    }

    public static int computeRequiredByteBufferSize( String text )
    {
        return encode( text ).length + 4;
    }

    private UTF8()
    {
        throw new AssertionError( "no instance" );
    }
}