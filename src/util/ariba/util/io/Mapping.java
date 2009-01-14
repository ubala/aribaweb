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

    $Id: //ariba/platform/util/core/ariba/util/io/Mapping.java#9 $
*/

package ariba.util.io;

import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
    Read in mapping file (value, mappedValue) and provide mapping.
    The mapping file is a csv file, with the first 2 columns of each
    row representing the key and its corresponding value. Any rows
    with less than 2 columns are skipped, and extra columns in exess
    of 2 of each row are also skipped.

    <p> The file can optionally include in its first row the encoding
    scheme to use. For exmaple, the line "8859_1,," specifies 8859_1
    encoding is to be used. For such cases, this class should be
    instantiated with the encoding parameter being null.</p>

    <p> Most files also contain in its first line (if no encoding is
    specified) or 2nd line (if encoding is specified) header
    information (such as the names of the columns). This header will
    not be read. However, some files does not contain the header. For
    these files, the class should be instantiated with the hasHeader
    parameter being false, otherwise, the first key-value pair will be
    skipped.</p>

    <p> Comments are indicated by a leading '#' character. <b>Note that
    the keys (that is the first column of each row) must not begin with
    '#'.</b> Currently even wrapping quotes around '#' will be treated as
    comments. Any comments if any, must appear after the encoding line (if
    any) and the header (if any).</p>

    <p>
    <b>Note that this class is not synchronized. </b> It is expected that the
    mapping be first generated when the mapping file is read. Only after
    that should the key-value pairs (or the entire map) be accessed.
    </p>

    @aribaapi documented
*/
public final class Mapping implements CSVConsumer
{
    private static final int ColumnFrom = 0;
    private static final int ColumnTo   = 1;

    private final CSVReader csvReader = new CSVReader(this);
    private CommentChecker commentchecker = new CommentChecker(null);
    private final Map map = MapUtil.map();
    /**
        The reverse map is used to lookup the key for the value.
        Since multiple keys could be mapped to a <code>value</code>,
        the keys are stored in a list.
        @aribaapi private
    */
    private final Map/*<String,List>*/ reverseMap = MapUtil.map();

    private URL    fileURL;
    private String encoding;
    private String defaultValue;
    private boolean hasHeader = true;
    /**
        Instantiates an instance of this class.
        @param fileURL the URL of the mapping file. Must be a valid URL.
        @param defaultValue the value to be used as the value of any
        key that does not have any mapped value.
        @param hasHeader if true, specifies that the mapping file has
        a header (which is the first line of the file if no encoding
        is specified, or the second line in the file if encoding is
        specified). The header, if present, will not be read.

        @aribaapi documented
    */
    public Mapping (URL fileURL, String defaultValue, boolean hasHeader)
    {
        this(fileURL, null, defaultValue, hasHeader);
    }
                    
    /**
        Instantiates an instance of this class.
        @param fileURL the URL of the mapping file. Must be a valid URL.
        @param encoding the encoding to be used. If null and if the
        encoding specified in the file is a supported encoding, it
        will be used. If it is not a supported encoding, the system
        default encoding will be used.
        @param defaultValue the value to be used as the value of any
        key that does not have any mapped value.

        @aribaapi documented
    */
    public Mapping (URL    fileURL,
                    String encoding,
                    String defaultValue)
    {
        this(fileURL, encoding, defaultValue, true);
    }

    /**
        Instantiates an instance of this class.
        @param fileURL the URL of the mapping file. Must be a valid URL.
        @param encoding the encoding to be used. If null and if the
        encoding specified in the file is a supported encoding, it
        will be used. If it is not a supported encoding, the system
        default encoding will be used.
        @param defaultValue the value to be used as the value of any
        key that does not have any mapped value.
        @param hasHeader if true, specifies that the mapping file has
        a header (which is the first line of the file if no encoding
        is specified, or the second line in the file if encoding is
        specified). The header, if present, will not be read.
        @aribaapi documented
    */
    public Mapping (URL fileURL,
                    String encoding,
                    String defaultValue,
                    boolean hasHeader)
    {
        this.fileURL      = fileURL;
        this.encoding     = encoding;
        this.defaultValue = defaultValue;
        this.hasHeader = hasHeader;
    }

    /**
        Reads the contents of the mapping file.
        @exception IOException when I/O errors occurs.
        @see #map
        @aribaapi documented
    */
    public void read () throws IOException
    {
        if (encoding == null) {
            csvReader.readForSpecifiedEncoding(fileURL);
        }
        else {
            csvReader.read(fileURL, encoding);
        }
        immutate();
    }

    /**
        Returns the value of the given key. Note that read should have
        been called before calling this method.
        @param key the key to map.
        @see #read
        @see #mapCopy
        @return the mapped value
        @aribaapi documented
    */
    public String map (String key)
    {
        if (key == null) {
            return defaultValue(null);
        }

        String mappedValue = (String)map.get(key);
        if (mappedValue == null) {
            return defaultValue(key);
        }
        return mappedValue;
    }

    /**
        Returns the list of keys for the given value.
        It could return null or empty list on a not matched case.
        The returned list is an immutable list.
        Note that read should have been called before calling this method.
        @param value the value to map.
        @see #read
        @see #mapCopy
        @return the mapped list of keys
        @aribaapi ariba
    */
    public List/*<String>*/ reverseMap (String value)
    {
        if (value == null) {
            return null;
        }

        return (List)reverseMap.get(value);
    }

    /**
        Returns a copy of the map.
        @see #map
        @return a copy of the map.
        @aribaapi documented
    */
    public Map mapCopy ()
    {
        return MapUtil.copyMap(map);
    }

    private String defaultValue (String value)
    {
        return (defaultValue == null) ? value : defaultValue;
    }

    public void consumeLineOfTokens (String filePath,
                                     int    lineNumber,
                                     List line)
    {
            // ignore the first line if the file has a header
        if (lineNumber == 1 && hasHeader) {
            return;
        }
        if (commentchecker.isComment(line)) {
            return;
        }
        if (ColumnTo < line.size()) {
            Object key   = line.get(ColumnFrom);
            Object value = line.get(ColumnTo);
            map.put(key, value);
            List keyList = (List)reverseMap.get(value);
            if (keyList == null) {
                keyList = ListUtil.list();
                reverseMap.put(value, keyList);
            }
            ListUtil.addElementIfAbsent(keyList, key);
        }
    }

    /**
        immutable the internal data structure.
    */
    private void immutate ()
    {
            // make the list in reverseMap immutable.
        Iterator iter = reverseMap.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String)iter.next();
            List list = (List)reverseMap.get(key);
            reverseMap.put(key, ListUtil.immutableList(list));
        }
    }

}
