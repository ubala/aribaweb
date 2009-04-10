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

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTCSVDataSource.java#21 $
*/
package ariba.ui.table;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWFileResource;
import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.util.AWResourceManager;
import ariba.ui.validation.AWVFormatterFactory;
import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.Date;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.Locale;

import ariba.util.io.CSVConsumer;
import ariba.util.io.CSVReader;
import ariba.util.i18n.I18NUtil;
import ariba.util.formatter.DateFormatter;
import ariba.util.formatter.BigDecimalFormatter;
import ariba.util.fieldvalue.FieldValue;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.net.URL;

public final class AWTCSVDataSource extends AWTDataSource implements CSVConsumer
{
    protected List _lines = ListUtil.list();
    protected List _objects = null;
    protected Entity _entity = new Entity();
    ParsePosition _parsePosition = new ParsePosition(0);
    static NumberFormat _NumberFormatter = NumberFormat.getNumberInstance(Locale.US);
    static BigDecimalFormatter _BigDecimalFormatter = new BigDecimalFormatter();
    static DateFormatter _DateFormatter = new DateFormatter();
    protected static final Class _NullMarker = AWTCSVDataSource.class;

    public final class Entity extends AWTEntity
    {
        protected List _propertyKeys;
        protected List _propertyClasses;

/*
protected Entity (List keys, List classes)
        {
            _propertyKeys = keys;
            _propertyClasses = classes;
        }
*/
        public List propertyKeys ()
        {
            return _propertyKeys;
        }

        public String defaultFormatterNameForKey (String key)
        {
            // return based on data type
            int pos = _propertyKeys.indexOf(key);

            if (pos >= 0) {
                Class propClass = (Class)_propertyClasses.get(pos);
                if (AWUtil.classInheritsFromClass(propClass, Date.class)) return AWVFormatterFactory.ShortDateFormatterKey;
                if ((propClass==BigDecimal.class) || (propClass==Double.class)) return AWVFormatterFactory.BigDecimalFormatterKey;
                if ((propClass==Integer.class) || (propClass==Long.class)) return AWVFormatterFactory.IntegerFormatterKey;
            }
            return null;  // default is no formatter
        }

        public String displayStringForKey (String key)
        {
            return key;
        }

        public String defaultAlignmentForKey (String key)
        {
            // Right-align numbers
            int pos = _propertyKeys.indexOf(key);

            if ((pos >=0) && (AWUtil.classInheritsFromClass((Class)_propertyClasses.get(pos), Number.class))) {
                return "right";
            }
            return null;
        }

    }

    public AWTEntity entity ()
    {
        return _entity;
    }

    /** called when done */
    public List objects ()
    {
        if (_objects == null) {
            if (_entity.propertyKeys().contains("outlineLevel")) {
                _objects = computeOutlineList (_lines, "children", "outlineLevel");
            } else {
                _objects = _lines;
            }
        }
        return _objects;
    }

    public List fetchObjects ()
    {
        return objects();
    }

    public boolean hasChanges()
    {
        return false;
    }

    public List headings ()
    {
        return _entity.propertyKeys();
    }

    public static AWTCSVDataSource dataSourceForFile (File file)
    {
        AWTCSVDataSource dataSource = new AWTCSVDataSource();
        CSVReader reader = new CSVReader(dataSource);
        try {
            reader.read(file, I18NUtil.EncodingUTF8);
        } catch (IOException e) {
            throw new AWGenericException(e);
        }
        return dataSource;
    }

    public static AWTCSVDataSource dataSourceForURL (URL url)
    {
        AWTCSVDataSource dataSource = new AWTCSVDataSource();
        CSVReader reader = new CSVReader(dataSource);
        try {
            reader.read(url, I18NUtil.EncodingUTF8);
        } catch (IOException e) {
            throw new AWGenericException(e);
        }
        return dataSource;
    }

    public static AWTCSVDataSource dataSourceForCSVString (String content)
    {
        AWTCSVDataSource dataSource = new AWTCSVDataSource();
        CSVReader reader = new CSVReader(dataSource);
        try {
            reader.read(new StringReader(content), "AWTCSVData");
        } catch (IOException e) {
            throw new AWGenericException(e);
        }
        return dataSource;
    }

    public static AWTCSVDataSource dataSourceForPath (String path, AWComponent parentComponent)
    {
        URL url = ResourceLocator.urlForRelativePath(path, parentComponent);
        if (url != null) {
            return dataSourceForURL(url);
        }

        AWResourceManager resourceManager = parentComponent.resourceManager();
        AWResource resource = resourceManager.resourceNamed(path);
        if (resource instanceof AWFileResource) {
            String filePath = ((AWFileResource)resource)._fullPath();
            File file = new File(filePath);
            return dataSourceForFile(file);
        }
        else {
            throw new RuntimeException("Cannot find file: " + path);
        }
    }

    public void consumeLineOfTokens (String path,
                                     int lineNumber,
                                     List line)
    {
        int count = line.size();

        if (_entity._propertyKeys == null) {
            _entity._propertyKeys = line;

                // data type vector
            _entity._propertyClasses = ListUtil.list(count);
            for (int i=0; i<count; i++) {
                _entity._propertyClasses.add(_NullMarker);
            }
        }
        else {
            Assert.that(_entity._propertyKeys.size() >= count,
                        "CSV line %s field count (%s) doesn't match header: %s",
                        Constants.getInteger(_entity._propertyKeys.size()),
                        Constants.getInteger(count),
                        _entity._propertyKeys);
            Map row = MapUtil.map();
            for (int i=0; i<count; i++) {
                Object val = process((String)line.get(i));

                if (val != null) {
                    row.put(_entity._propertyKeys.get(i), val);

                        // keep track of class of items
                    Class recordedType = (Class)_entity._propertyClasses.get(i);
                    Class valType = val.getClass();
                    if (recordedType != valType) {
                        if (recordedType != _NullMarker) {
                            valType = commonSupertype(valType, recordedType); // if conflict, demote to String
                        }
                        _entity._propertyClasses.set(i, valType);
                    }
                }
            }
            _lines.add(row);
        }
    }

    protected void resetParsePosition ()
    {
        _parsePosition.setErrorIndex(-1);
        _parsePosition.setIndex(0);
    }

    protected boolean incompleteParse (String string)
    {
        return (_parsePosition.getErrorIndex() != -1) || (_parsePosition.getIndex() < string.length());
    }

    /** parse canonical numbers and dates strings into objects */
    protected Object process (String stringVal)
    {
        Object obj = null;

        stringVal = stringVal.trim();

        if (stringVal.length() == 0) return null;

        // try number
        resetParsePosition();
        obj = _NumberFormatter.parse(stringVal, _parsePosition);
        if (incompleteParse(stringVal)) {
            obj = null;
        }
        else if (obj instanceof Double || obj instanceof Float) {
            try {
                obj = _BigDecimalFormatter.parse(stringVal);
            } catch (ParseException e) {
                // swallow
            }
        }
        else if (!(obj instanceof Integer)) {
            obj = Constants.getInteger(((Number)obj).intValue());
        }

        if (obj == null) {
            // try date
            try {
                obj = _DateFormatter.parseDate(stringVal, "MM/dd/yy");
            } catch (ParseException e) {
                ; // swallow
            }
        }

        if (obj == null) {
            // try boolean
            if (stringVal.equals("TRUE")) {
                obj = Boolean.TRUE;
            } else if (stringVal.equals("FALSE")) {
                obj = Boolean.FALSE;
            }
        }

        // return a successfully parsed number or the String
        if (obj == null) {
            obj = stringVal;
        }
        // System.out.println("--- parse \"" + stringVal + " --> (" + obj.getClass().getName() + "): " + obj.toString());
        return obj;
    }

    Class commonSupertype (Class a, Class b)
    {
        if (a == b) return a;

        if (AWUtil.commonSuperclass(a, b) != Number.class) return String.class;

        // if either is fractional, then promote to BigDecimal
        if ((a == BigDecimal.class) || (a == Double.class)
            || (b == BigDecimal.class) || (a == Double.class)) {
            return BigDecimal.class;
        }
        return Integer.class;
    }

    /*
        Create a nested list out of a flat list.
          levelKey -- the field that indicates child nesting
               -- row with same level are considered peers, > -- children, < --> parent
          childrenKey -- field to which to assign the children
    */
    protected static int _NestingLevel_None = -999999;
    public static List computeOutlineList (List allItems, String childrenKey, String levelKey)
    {
        List result = ListUtil.list();
        if (allItems.size() > 0) {
            PeekIterator iter = new PeekIterator(allItems.iterator());
            addItemsForLevel(iter, result, _NestingLevel_None, childrenKey, levelKey);
        }
        return result;
    }

    protected static void addItemsForLevel (PeekIterator iter, List list, int curLevel, String childrenKey, String levelKey)
    {
        Object lastObject = null;
        Object obj;
        while ((obj = iter.peek()) != null) {
            Object objLevel = FieldValue.getFieldValue(obj, levelKey);
            int level = (objLevel != null) ? ((Number)objLevel).intValue() : 0;
            if (curLevel == _NestingLevel_None) curLevel = level;

            // if next object is a parent, we're done
            if (level < curLevel) return;

            // if lower, this is our child -- recurse
            if (level > curLevel) {
                List children = ListUtil.list();
                addItemsForLevel(iter, children, level, childrenKey, levelKey);
                FieldValue.setFieldValue(lastObject, childrenKey, children);
            } else {
                list.add(obj);
                lastObject = obj;
                iter.next();
            }
        }
    }

    // I can't believe that I have to create this...
    public static class PeekIterator
    {
        Iterator _iter;
        Object _next = null;

        public PeekIterator (Iterator iter)
        {
            _iter = iter;
            _next = next();
        }

        public Object peek ()
        {
            return _next;
        }

        public Object next ()
        {
            _next = _iter.hasNext() ? _iter.next() : null;
            return _next;
        }
    }
}
