/*
    Copyright 1996-2008 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/io/SerializeUtil.java#6 $
*/

package ariba.util.io;
import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.i18n.I18NUtil;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Locale;

/**
    Utilities useful for Serialization/Externalization.

    @aribaapi documented
*/
public class SerializeUtil
{
    /**
        constant for calling the writeUTF method, used by rpc stubgenerator.
        @aribaapi ariba
    */
    public static final String WriteUTF     = "writeUTF";
    /**
        constant for calling the writeObject method, used by rpc stubgenerator.
        @aribaapi ariba
    */
    public static final String WriteObject  = "writeObject";
    /**
        constant used by rpc stubgenerator to generate code to call the writeInt method.
        @aribaapi ariba
    */
    public static final String WriteInt     = "writeInt";
    /**
        constant used by rpc stubgenerator to generate code to call the writeBoolean method.
        @aribaapi ariba
    */
    public static final String WriteBoolean = "writeBoolean";
    /**
        constant used by rpc stubgenerator to generate code to call the writeLong method.
        @aribaapi ariba
    */
    public static final String WriteLong    = "writeLong";
    /**
        constant used by rpc stubgenerator to generate code to call the writeFloat method.
        @aribaapi ariba
    */
    public static final String WriteFloat   = "writeFloat";
    /**
        constant used by rpc stubgenerator to generate code to call the writeChar method.
        @aribaapi ariba
    */
    public static final String WriteChar    = "writeChar";
    /**
        constant used by rpc stubgenerator to generate code to call the writeLocale method.
        @aribaapi ariba
    */
    public static final String WriteLocale    = "writeLocale";
    /**
        constant used by rpc stubgenerator to generate code to call the resetObjectTable method.
        @deprecated
        @aribaapi ariba
    */
    public static final String ResetObjectTable = "resetObjectTable";

    /**
        constant for calling the readObject method, used by rpc stubgenerator.
        @aribaapi ariba
    */
    public static final String ReadObject  = "readObject";
    /**
        constant for calling the readUTF method, used by rpc stubgenerator.
        @aribaapi ariba
    */
    public static final String ReadUTF     = "readUTF";
    /**
        constant used by rpc stubgenerator to generate code to call the readInt method.
        @aribaapi ariba
    */
    public static final String ReadInt     = "readInt";
    /**
        constant used by rpc stubgenerator to generate code to call the readBoolean method.
        @aribaapi ariba
    */
    public static final String ReadBoolean = "readBoolean";
    /**
        constant used by rpc stubgenerator to generate code to call the readChar method.
        @aribaapi ariba
    */
    public static final String ReadChar    = "readChar";
    /**
        constant used by rpc stubgenerator to generate code to call the readLong method.
        @aribaapi ariba
    */
    public static final String ReadLong    = "readLong";
    /**
        constant used by rpc stubgenerator to generate code to call the readFloat method.
        @aribaapi ariba
    */
    public static final String ReadFloat   = "readFloat";
    /**
        constant used by rpc stubgenerator to generate code to call the readLocale method.
        @aribaapi ariba
    */
    public static final String ReadLocale   = "readLocale";

    /**
        prevent people from creating instances of this class
    */
    private SerializeUtil ()
    {
    }

    /**
        helper function to write a String during serialization. We need this because
        java.io.writeUTF does not accept a null object. So this is a wrapper to handle
        the null object. Used in conjuction with SerializeUtil.readUTF

        @param output  the object output stream to write the String. Cannot be null.
        @param str     the String object to serialize.
        @exception     IOException I/O error occurred while writing to output

        @see #readUTF

        @aribaapi documented
    */
    public static void writeUTF (ObjectOutput output, String str) throws IOException
    {
        if (str == null) {
            output.write(0);
            return;
        }
        if (str.length() == 0) {
            output.write(1);
            return;
        }

        output.write(2);
        output.writeUTF(str);
    }


    /**
        helper function to read a String during serialization. We need this because
        java.io.writeUTF does not accept a null object. So this is a wrapper to handle
        the null object. Used in conjuction with SerializeUtil.writeUTF

        @param input  the object input stream to read the String. Cannot be null.

        @return the String read.

        @exception IOException I/O error occurred while reading from input

        @aribaapi documented
    */
    public static String readUTF (ObjectInput input) throws IOException
    {
        int code = input.read();
        if (code == 0) {
            return null;
        }
        if (code == 1) {
            return "";
        }

            // check for EOF
        if (code == -1) {
            throw new EOFException();
        }
        Assert.that(code == 2,
                    "Unexpected code in inputStream of %s", Constants.getInteger(code));
        return input.readUTF();
    }

    /**
        helper function to write the Locale object during serialization. Actually, only
        the String returned by the Locale.toString method is serialized.

        @param output  the ObjectOutput stream to write out the Locale object. Cannot be
        null.
        @param locale  the Locale object to write, okay to be null.
        @exception     IOException throws IOException if the write fails.

        @see #readLocale

        @aribaapi documented
    */
    public static void writeLocale (ObjectOutput output, Locale locale) throws IOException
    {
        writeUTF(output, locale == null ? null : locale.toString());
    }

    /**
        helper function to read the Locale object during serialization, read back the
        String from the given input stream.

        @param input   the ObjectInput stream to read from, cannot be null.
        @return        the Locale object corresponding to the String read.
        @exception     IOException throws IOException if the read fails.

        @see #writeLocale

        @aribaapi documented
    */
    public static Locale readLocale (ObjectInput input) throws IOException
    {
        String localeStr = readUTF(input);
        if (localeStr == null) {
            return null;
        }
        return I18NUtil.getLocaleFromString(localeStr);
    }
}

