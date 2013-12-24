/*
    Copyright (c) 1996-2013 Ariba, Inc.
    All rights reserved. Patents pending.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/core/Constants.java#28 $
*/

package ariba.util.core;

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
    Constants for well known values

    @aribaapi documented
*/
public class Constants implements Externalizable
{
    /**
        leave this decl first. Otherwise strange things may happen.

        @aribaapi private
    */
    public static final int StringNullId = 1;

    /**
        @aribaapi private
    */
    private int id;

    /**
        @aribaapi private
    */
    private static List constantsList = ListUtil.list();

    private Constants (int id)
    {
        this.id = id;
            // Do not change the String.valueOf to Constants.getInteger or
            // there will be a bootstrapping problem
        if (constantsList.size() != id) {
            Assert.that(false,
                        "You may not initialize constants out of order: %s != %s",
                        String.valueOf(constantsList.size()),
                        String.valueOf(id));
        }
        if (id == StringNullId) {
                // in the case of StringNull we really store the String
                // value of the constant
            constantsList.add(StringNull);
        }
        else {
                // normally we just store the new Constants object
            constantsList.add(this);
        }
    }

    /**
        @aribaapi private
    */
    public static Object constantForId (int i)
    {
        if (i < 0 || i >= constantsList.size()) {
            Assert.that(false,
                        "Constants: %s is outside of range %s",
                        Constants.getInteger(i),
                        Constants.getInteger(constantsList.size()));
        }
        return constantsList.get(i);
    }

    /**
        @aribaapi private
    */
    public int getId ()
    {
        return id;
    }

        // orientations
    /**
        @aribaapi private
    */
    public static final boolean Horizontally = true;

    /**
        @aribaapi private
    */
    public static final boolean Vertically   = false;

    /**
        @aribaapi private
    */
    public static final Constants NumberNull  = new Constants(0);
    /**
        Null String object.
        It would be nice to use new String to guarantee a unique new object,
        but that gives us a tricky initialization ordering problem.
        So just make the string weird.
        @aribaapi private
    */
    public static final String StringNull  = "<null^^null$#>";
    /**
        @aribaapi private
    */
    public static final Constants StringNullConstant =
        new Constants(StringNullId);
    /**
        @aribaapi private
    */
    public static final Constants DateNull    = new Constants(2);
    /**
        @aribaapi private
    */
    public static final Constants IntegerNull = new Constants(3);
    /**
        @aribaapi private
    */
    public static final Constants DoubleNull  = new Constants(4);
    /**
        @aribaapi private
    */
    public static final Constants BlobNull  = new Constants(5);

    /**
        @aribaapi private
    */
    public static final Constants LongNull  = new Constants(6);
    /**
        String constant for java.lang.Object
        @aribaapi documented
    */
    public static final String ObjectType     = Object.class.getName();
    /**
        String constant for java.lang.String
        @aribaapi documented
    */
    public static final String StringType     = String.class.getName();
    /**
        String constant for java.lang.Boolean
        @aribaapi documented
    */
    public static final String BooleanType    = Boolean.class.getName();
    /**
        String constant for java.lang.Integer
        @aribaapi documented
    */
    public static final String IntegerType    = Integer.class.getName();
    /**
        String constant for java.lang.Long
        @aribaapi documented
    */
    public static final String LongType       = Long.class.getName();
    /**
        String constant for java.lang.Float
        @aribaapi documented
    */
    public static final String FloatType      = Float.class.getName();
    /**
        String constant for java.lang.Double
        @aribaapi documented
    */
    public static final String DoubleType     = Double.class.getName();
    /**
        String constant for java.lang.Number
        @aribaapi documented
    */
    public static final String NumberType     = Number.class.getName();
    /**
        String constant for java.math.BigDecimal
        @aribaapi documented
    */
    public static final String BigDecimalType = BigDecimal.class.getName();
    /**
        String constant for java.util.Date
        @aribaapi private
    */
    public static final String JavaDateType   = java.util.Date.class.getName();

    /**
        String constant for java.sql.Date
        @aribaapi documented
    */
    public static final String SQLDateType    = java.sql.Date.class.getName();
    /**
        String constant for java.sql.Timestamp
        @aribaapi documented
    */
    public static final String TimestampType = Timestamp.class.getName();

    /**
        String constant for ariba.util.core.Date
        @aribaapi private
    */
    public static final String AribaDateType   = ariba.util.core.Date.class.getName();


    /**
        String constant for <code>ariba.util.Blob</code>
        @aribaapi ariba
    */
    public static final String BlobType = Blob.class.getName();

    /**
        String constant for <code>java.io.File</code>
        @aribaapi ariba
    */
    public static final String FileType = File.class.getName();

    /**
        String constant for <code>java.net.URL</code>
        @aribaapi ariba
    */
    public static final String URLType = URL.class.getName();

    /**
        String constant for <code>java.util.List</code>
        @aribaapi ariba
    */
    public static final String ListType = List.class.getName();

    /**
        String constant for <code>java.util.Map</code>
        @aribaapi ariba
    */
    public static final String MapType = Map.class.getName();

    /**
        String constant for <code>java.lang.StringBuffer</code>
        @aribaapi ariba
    */
    public static final String StringBufferType = StringBuffer.class.getName();


    /**
        String constant for char
        @aribaapi documented
    */
    public static final String CharPrimitiveType    = Character.TYPE.getName();
    public static final String JavaCharAbbreviation = "C";
    /**
        String constant for byte
        @aribaapi documented
    */
    public static final String BytePrimitiveType    = Byte.TYPE.getName();
    public static final String JavaByteAbbreviation = "B";

    /**
        String constant for int
        @aribaapi documented
    */
    public static final String ShortPrimitiveType   = Short.TYPE.getName();
    public static final String JavaShortAbbreviation= "S";
    /**
        String constant for int
        @aribaapi documented
    */
    public static final String IntPrimitiveType     = Integer.TYPE.getName();
    public static final String JavaIntAbbreviation  = "I";
    /**
        String constant for long
        @aribaapi documented
    */
    public static final String LongPrimitiveType    = Long.TYPE.getName();
    public static final String JavaLongAbbreviation = "J";
    /**
        String constant for float
        @aribaapi documented
    */
    public static final String FloatPrimitiveType   = Float.TYPE.getName();
    public static final String JavaFloatAbbreviation= "F";
    /**
        String constant for double
        @aribaapi documented
    */
    public static final String DoublePrimitiveType  = Double.TYPE.getName();
    public static final String JavaDoubleAbbreviation = "D";
    /**
        String constant for char
        @aribaapi documented
    */
    public static final String BooleanPrimitiveType = Boolean.TYPE.getName();
    public static final String JavaBooleanAbbreviation = "Z";

    public static final String JavaArrayAbbreviation = "[";
    public static final String JavaObjectAbbreviation = "L";

    /**
        String constant for [Ljava.lang.Integer (jni)
        @aribaapi documented
    */
    public static final String IntegerArrayType =
        StringUtil.strcat(JavaArrayAbbreviation,
                          JavaObjectAbbreviation,
                          IntegerType);
    /**
        String constant for [I (jni)
        @aribaapi documented
    */
    public static final String IntArrayType =
        StringUtil.strcat(JavaArrayAbbreviation,
                          JavaObjectAbbreviation,
                          IntPrimitiveType);

    /**
        Public constant for Integer(-1) to avoid object allocation
        @aribaapi documented
    */
    public static final Integer MinusOneInteger = new Integer(-1); // OK
    /**
        Public constant for Integer(0 to avoid object allocation
        @aribaapi documented
    */
    public static final Integer ZeroInteger     = new Integer(0);  // OK
    /**
        Public constant for Integer(1) to avoid object allocation
        @aribaapi documented
    */
    public static final Integer OneInteger      = new Integer(1);  // OK

    /**
        Public constant for Long(-1) to avoid object allocation
        @aribaapi documented
    */
    public static final Long   MinusOneLong = new Long(-1); // OK
    /**
        Public constant for Long(0) to avoid object allocation
        @aribaapi documented
    */
    public static final Long   ZeroLong     = new Long(0); // OK
    /**
        Public constant for Long(1) to avoid object allocation
        @aribaapi documented
    */
    public static final Long   OneLong      = new Long(1); // OK

    /**
        Public constant for Float(0.0) to avoid object allocation
        @aribaapi documented
    */
    public static final Float  ZeroFloat  = new Float(0.0);
    /**
        Public constant for Double(0.0) to avoid object allocation
        @aribaapi documented
    */
    public static final Double ZeroDouble = new Double(0.0);

    /**
        Public constant for BigDecimal(0.0) to avoid object allocation
        @aribaapi documented
    */
    public static final BigDecimal ZeroBigDecimal       = new BigDecimal(0.0); // OK
    /**
        Public constant for BigDecimal(1.0) to avoid object allocation
        @aribaapi documented
    */
    public static final BigDecimal OneBigDecimal        = new BigDecimal(1.0); // OK
    /**
        Public constant for BigDecimal(10.0) to avoid object
        allocation
        @aribaapi documented
    */
    public static final BigDecimal TenBigDecimal        = new BigDecimal(10.0); // OK
    /**
        Public constant for BigDecimal(100.0) to avoid object
        allocation
        @aribaapi documented
    */
    public static final BigDecimal OneHundredBigDecimal = new BigDecimal(100.0); // OK
    /**
        Public constant for Object()
        @aribaapi documented
    */
    public static final Object NullObject = new Object();
    /**
        Public constant for an empty string
        @aribaapi documented
    */
    public static final String EmptyString = "";

    /**
        Public constant for an empty array. <p/>
        @aribaapi documented
    */
    public static final Object[] EmptyArray = new Object[0];

    public static final String KeySystemDirectoryProperty = "ariba.systemDirectory";

    public static final String SQL_ERROR = "SQL Error";

    /**
        The only other class that should this method is ariba.util.parameters.Parameters
        Everyone else should use SystemUtil.getSystemDirectoryString()
        @aribaapi private
    */
    public static String getDefaultSystemDir ()
    {
        String systemDir = null;
        try {
            systemDir = System.getProperty(KeySystemDirectoryProperty);
        }
        catch (SecurityException e) {
        }
        if (systemDir == null) {
            systemDir = "ariba";
        }
        return systemDir;
    }

    public static final String KeyConfigDirectoryProperty = "ariba.configDirectory";

    /**
        The only other class that should this method is ariba.util.parameters.Parameters
        Everyone else should use SystemUtil.getConfigDirectoryString()
        @aribaapi private
    */
    public static String getDefaultConfigDir ()
    {
        String configDir = null;
        try {
            configDir = System.getProperty(KeyConfigDirectoryProperty);
        }
        catch (SecurityException e) {
        }
        if (configDir == null) {
            configDir = "config";
        }
        return configDir;
    }

    public static final String KeyTempDirectoryProperty = "ariba.tempDirectory";

    /**
        The only other class that should use this method is ariba.util.parameters.Parameters
        Everyone else should use SystemUtil.getLocalTempDirectoryString()
        @aribaapi private
    */
    public static String getDefaultTempDirectory ()
    {
        String tempDir = null;
        try {
            tempDir = System.getProperty(KeyTempDirectoryProperty);
        }
        catch (SecurityException e) {
        }
        if (tempDir == null) {
            tempDir = "temp";
        }
        return tempDir;
    }

    /**
        Gets the directory for storing temp files.
        @aribaapi documented
        @return directory for storing temp files
        @deprecated use SystemUtil.getLocalTempDirectory
    */
    public static File getTempDirectory ()
    {
        return SystemUtil.getLocalTempDirectory();
    }

        // package private
    static final int    MaxSavedInt  = 8192;
    static final int    MinSavedInt  = -1024;
    static final Integer posInts[]   = new Integer[MaxSavedInt+1];
    static final Integer negInts[]   = new Integer[(-MinSavedInt)+1];
    static {
        int i;

            // start with the negative numbers
        for (i = 1; i <= (-MinSavedInt); i++) {
            negInts[i] = new Integer(-i); // OK
        }
        negInts[0] = ZeroInteger;

            // then the positive
        posInts[0] = ZeroInteger;
        posInts[1] = OneInteger;
        for (i = 2; i <= (MaxSavedInt); i++) {
            posInts[i] = new Integer(i); // OK
        }
    }

        // package private
    static final long MaxSavedLong  = 64;
    static final long MinSavedLong  = -16;
    static final Long posLongs[]   = new Long[(int)MaxSavedLong+1];
    static final Long negLongs[]   = new Long[(-(int)MinSavedLong)+1];

    static {
        int i;

            // start with the negative numbers
        for (i = 1; i <= (-MinSavedLong); i++) {
            negLongs[i] = new Long(-i); // OK
        }
        negLongs[0] = ZeroLong;

            // then the positive
        posLongs[0] = ZeroLong;
        posLongs[1] = OneLong;
        for (i = 2; i <= (MaxSavedLong); i++) {
            posLongs[i] = new Long(i); // OK
        }
    }
    /**
        Where applicable some functions use the Success int constant return
        value.
        @aribaapi documented
    */
    public static final int Success = 0;

    /**
        Where applicable some functions use the Success Integer constant
        return value.
        @aribaapi documented
    */
    public static final Integer SuccessInteger = Constants.ZeroInteger;

    /**
        Helper function to generate Booleans without excess memory
        allocation.

        @param b the boolean value requested

        @return the constant Boolean for true or false as requested.
        @aribaapi documented
    */
    public static Boolean getBoolean (boolean b)
    {
        return b ? Boolean.TRUE : Boolean.FALSE;
    }
    /**
        Parse a string into a Boolean. This assumes that the
        Boolean.toBoolean() method was used to create the String.

        @param s a String representation of a boolean

        @return <b>Boolean.TRUE</b> if the string was for the true
        boolean, <b>Boolean.FALSE</b> otherwise
        @aribaapi documented
    */
    public static Boolean getBoolean (String s)
    {
        return getBoolean((s != null) && s.equalsIgnoreCase("true"));
    }

    /**
        Helper function to generate Integers without excess memory
        allocation.

        @param i the integer value requested

        @return the corresponding Integer as requested.
        @aribaapi documented
    */
    public static Integer getInteger (int i)
    {
        /*
            This is code to provide a histogram of the uses of
            Constants.getInteger() it looks like the histogram changes
            every release and I don't want to have to keep rewriting
            the code...so I'm just leaving it commented out so there
            is no footprint

            int index = i/1000;
            index += 50;
            synchronized(histogram) {
            if (index >= 0 && index < 100) {
            histogram[index]++;
            }
            count ++;
            if (count % 10000 == 0) {
            FormatBuffer fsb = new FormatBuffer(1024);
            int tmpsum = 0;
            for (int j=0;j<100;j++) {
            int range = (j-50)*1000;
            Fmt.B(fsb, "%s:%s;", Integer.toString(range),
            Integer.toString(histogram[j]));
            tmpsum += histogram[j];
            }
            Log.misc.debug("Total of %s objects, %s not in histogram; %s",
            Integer.toString(count),
            Integer.toString(count - tmpsum),
            fsb.toString());
            }
            }
        */
        if (i >= 0 && i <= Constants.MaxSavedInt) {
            return (Constants.posInts[i]);
        }
        if (i < 0 && i >= Constants.MinSavedInt) {
            return (Constants.negInts[-i]);
        }
        return new Integer(i); // OK
    }
    /**
        Helper function to create Long objects. This will try to
        avoid allocating new Long objects on every invocation.

        @param l a long to convert into a Long

        @return a constant Long, occasionally from a pre-allocated
        set.
        @aribaapi documented
    */
    public static Long getLong (long l)
    {
        if (l >= 0 && l <= Constants.MaxSavedLong) {
            return (Constants.posLongs[(int)l]);
        }
        if (l < 0 && l >= Constants.MinSavedLong) {
            return (Constants.negLongs[(int)-l]);
        }
        return new Long(l); // OK
    }

    /**
        Implementation of Externalizable interface.

        @aribaapi private
    */
    public Constants ()
    {
    }

    /**
        Implementation of Externalizable interface.

        @aribaapi private
    */

    public void writeExternal (ObjectOutput output) throws IOException
    {
        output.writeInt(id);
    }

    /**
        Implementation of Externalizable interface.

        @aribaapi private
    */
    public void readExternal (ObjectInput input)
      throws IOException, ClassNotFoundException
    {
        id = input.readInt();
    }

    private Object readResolve () throws ObjectStreamException
    {
        Object constants = constantForId(id);
        return constants;
    }
}








