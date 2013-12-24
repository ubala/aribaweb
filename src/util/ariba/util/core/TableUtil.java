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

    $Id: //ariba/platform/util/core/ariba/util/core/TableUtil.java#13 $
*/

package ariba.util.core;

import ariba.util.i18n.I18NUtil;
import ariba.util.io.DeserializationException;
import ariba.util.io.Deserializer;
import ariba.util.io.FormattingSerializer;
import ariba.util.log.Log;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.Map;

/**
    Table file Utilities. These are helper functions for dealing with
    table files.

    @aribaapi documented
*/
public final class TableUtil
{

    /**
        prevent people from creating this class
    */
    private TableUtil ()
    {
    }

    /**
        Load a serialized Map from the given file. The format of
        the file should be that of calling a Map's toString()
        method.

        @param file the File to load the serialized Map from

        @return the Map loaded from the file
        @aribaapi documented
    */
    public static Map loadMap (File file)
    {
        return loadMap(file, true);
    }

    /**
        Load a serialized Map from the given url. The format of
        the contents should be that of calling a Map's
        toString() method.

        @param file the File to load the serialized Map from
        @param displayError if <b>true</b> and there are errors
        deserializing the Map, an error will be printed.

        @return the Map loaded from the url, <b>null</b> if the map cannot be loaded.
        @aribaapi documented
    */
    public static Map loadMap (File file, boolean displayError)
    {
        if (!file.exists()) {
            if (displayError) {
                Log.util.error(6609, file.getPath());
            }
            return null;
        }
        return loadMap(URLUtil.url(file), displayError);
    }

    /**
        Load a serialized Map from the given url. The format of
        the contents should be that of calling a Map's
        toString() method.

        @param url the URL to load the serialized Map from

        @return the Map loaded from the url
        @aribaapi documented
    */
    public static Map loadMap (URL url)
    {
        return loadMap(url, true);
    }

    /**
        Load a serialized Map from the given url. The format of
        the contents should be that of calling a Map's
        toString() method.

        @param url the URL to load the serialized Map from
        @param displayError if <b>true</b> and there are errors
        deserializing the Map, an error will be printed.

        @return the Map loaded from the url
        @aribaapi documented
    */
    public static Map loadMap (URL url, boolean displayError)
    {
        Object object = loadObject(url);
        if (!(object instanceof Map)) {
            if (displayError) {
                Log.util.error(2920, url, object);
            }
            return null;
        }
        return (Map)object;
    }

    /**
        Load a serialized Map from the given String. The format
        of the contents should be that of calling a Map's
        toString() method.

        @param s the String to load the serialized Map from
        @param displayError if <b>true</b> and there are errors
        deserializing the Map, an error will be printed.

        @return the Map loaded from the url
        @aribaapi documented
    */
    public static Map loadMap (String s, boolean displayError)
    {
        Reader r = new StringReader(s);
        Object object = null;
        try {
            object = loadObject(r);
        }
        catch (IOException e) {
            return null;
        }

        if (!(object instanceof Map)) {
            if (displayError) {
                Log.util.error(2920, s, object);
            }
            return null;
        }
        return (Map)object;
    }

    /**
        Load an object from a URL using loadObject(Reader)

        @param url the URL to load the object from

        @return the deserialized object, or <b>null</b> if there was
        an IOException.

        @see #loadObject(Reader)
        @aribaapi documented
    */
    public static Object loadObject (URL url)
    {
        if (!URLUtil.maybeURLExists(url)) {
            return null;
        }
        try {
            //add for fix :1-BTXAQH
            return loadObject(IOUtil.bufferedReader(url.openStream(),
                                                    I18NUtil.EncodingUTF_8));
        }
        catch (IOException e) {
            return null;
        }
    }

    /**
        Load an object from a file using loadObject(Reader)

        @param file the path for the file to load the object from

        @return the deserialized object, or <b>null</b> if there was
        an IOException.

        @see #loadObject(Reader)
        @aribaapi documented
    */
    public static Object loadObject (File file)
    {
        return loadObject(file, I18NUtil.EncodingISO8859_1);
    }

    /**
        Load an object from a file using loadObject(Reader)

        @param file the path for the file to load the object from
        @param encoding the encoding used for file reader
        
        @return the deserialized object, or <b>null</b> if there was
        an IOException.

        @see #loadObject(Reader)
        @aribaapi documented
    */
    public static Object loadObject (File file, String encoding)
    {
        if (!file.exists()) {
            return null;
        }
        try {
            return loadObject(IOUtil.bufferedReader(file, encoding));
        }
        catch (IOException e) {
            return null;
        }
    }

    /**
        Load an object from a stream.  Uses the Deserializer class
        from ariba.util.io to decode the data.

        @param reader the source for the Object's data

        @return the deserialized Object.

        @exception IOException if there was an error deserializing the
        object.
        @aribaapi documented
    */
    public static Object loadObject (Reader reader) throws IOException
    {
        try {
            return new Deserializer(reader).readObject();
        }
        catch (DeserializationException e) {
            Log.util.error(2769, e);
            return null;
        }
        finally {
            reader.close();
        }
    }

    /** Writes an object to a file in safe way.
        This method is similar to {@link #writeObject(java.io.File, java.lang.Object) writeObject}
        but it actually uses the {@link FileReplacer FileReplacer} to be more robust.

        @param file the path for a file to write the object to
        @param object the Object to serialize into the file

        @return <b>true</b> if the object was written successfully
        <b>false</b> otherwise
        @aribaapi ariba
    */
    public static boolean safeWriteObject (File file, Object object)
    {
        if (! file.exists()) {
                // if the destination file doesn't exist, why bother
                // with the safer way
        	Log.util.warning(10405, file);
            return writeObject(file, object);
        }
        if (! file.canWrite()) {
                // if the file is not writable, we won't be able to make the rename
                // so don't even start.
            Log.util.warning(8418, file);
            return false;
        }
        return safeWriteObject(new FileReplacer(file), object);
    }

    private static boolean safeWriteObject (FileReplacer fileReplacer, Object object)
    {
        try {
            boolean success = false;
            OutputStream out = fileReplacer.getOutputStream();
            if (writeObject(out, object)) {
                fileReplacer.applyChanges();
                success = true;
            }
            fileReplacer.commit();
            return success;
        }
        catch (IOException e) {
            Log.util.warning(8896,
                             fileReplacer.getBaseFile(),
                             SystemUtil.stackTrace(e));
            return false;
        }
    }
    
    /**
        Write an object to a file.

        @param file the path for a file to write the object to
        @param object the Object to serialize into the file

        @return <b>true</b> if the object was written successfully
        <b>false</b> otherwise
        @aribaapi documented
    */
    public static boolean writeObject (File file, Object object)
    {
        OutputStream output = null;
        try {
            output = IOUtil.bufferedOutputStream(file);
            return writeObject(output, object);
        }
        catch (IOException e) {
            return false;
        }
        finally {
            if (output != null) {
                try {
                    output.close();
                }
                catch (IOException e) {
                    return false;
                }
            }
        }
    }

    /**
        Write an object to an OutputStream.

        @param outputStream the OutputStream to write the object to
        @param object the Object to serialize into the file

        @return <b>true</b> if the object was written successfully
        <b>false</b> otherwise
        @aribaapi documented
    */
    public static boolean writeObject (OutputStream outputStream,
                                       Object       object)
    {
        try {  
            //add for fix :1-BTXAQH
            PrintWriter printWriter = IOUtil.printWriter(outputStream,
                                                         I18NUtil.EncodingUTF_8);
            boolean result = writeObject(printWriter, object);
            printWriter.flush();
            return result;
        }
        catch (IOException e) {
            return false;
        }
    }


    /**
        Write an object to a printWriter.

        @param printWriter the PrintWriter to write the object to
        @param object the Object to serialize into the file

        @return <b>true</b> if the object was written successfully
        <b>false</b> otherwise
        @aribaapi documented
    */
    public static boolean writeObject (PrintWriter printWriter, Object object)
    {
        boolean result = FormattingSerializer.writeObject(printWriter,
                                                          object);
        printWriter.print("\n");
        return result;
    }

    /**
        @aribaapi private
    */
    public static void main (String args[])
    {
        String fileName = ArrayUtil.nullOrEmptyArray(args) ? 
            "config/Parameters.table" : (String)args[0];
        Assert.that(loadMap(new File(fileName)) != null,
                    "loadMap failed for %s", fileName);
    }
}
    
