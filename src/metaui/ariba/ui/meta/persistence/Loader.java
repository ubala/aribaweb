/*
    Copyright 2008 Craig Federighi

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License.
    You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/persistence/Loader.java#3 $
*/
package ariba.ui.meta.persistence;

import ariba.util.core.ListUtil;
import ariba.util.core.SetUtil;
import ariba.util.core.IOUtil;
import ariba.util.core.ClassUtil;
import ariba.util.core.Assert;
import ariba.util.core.MapUtil;
import ariba.util.io.CSVConsumer;
import ariba.util.io.CSVReader;
import ariba.util.i18n.I18NUtil;
import ariba.util.fieldvalue.FieldValue;
import ariba.util.fieldvalue.FieldPath;
import ariba.util.fieldvalue.RelationshipField;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWFormatting;
import ariba.ui.aribaweb.util.AWMultiLocaleResourceManager;
import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.meta.core.Context;
import ariba.ui.meta.core.Meta;
import ariba.ui.meta.core.UIMeta;
import ariba.ui.meta.core.ObjectMeta;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.Collections;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;

/**
    CSV Data loader.
    Enable loading data into a cluster of classes from a group of inner-dependent load files.
    Load file resources identify their classes and fields through naming convention.  E.g.:
        loads/ariba.appcore.User.csv    -- Load for User class
            name, email, manager.name, ...
                -- Column names denote field names.  Relationship field dotted paths identify join key
        loads/ariba.appore.User-memberOf.csv   -- Load of User -> Group many-to-many relationship
                                                  ("-memberOf" denotes toMany field being loaded)
            name, memberOf.name
                -- unprefixed names are lookup keys for the root class, prefixed names
                   are lookup keys for the destination

    Dependent load handling
        - Loads are automatically ordered based on class dependencies
            - in one-to-many, one goes first
        - Upserts are used, so missing objects on foreign key references are created and then filled in later

    Data Formatting
        - parsing of data fields is handled using *formatters* looked up via metaui
  */
public class Loader
{
    List<Load> _loads = ListUtil.list();
    ObjectContext _ctx;
    Meta _meta;
    Context _metaContext;
    Map <Class, ClassCandidateKeyCache> _lookupKeyCaches = MapUtil.map();

    public Loader ()
    {
        _ctx = ObjectContext.get();
        _meta = UIMeta.getInstance();
        _metaContext = _meta.newContext();
    }

    public static List<URL> findLoads (AWMultiLocaleResourceManager resourceManager, String prefix)
    {
        List result = ListUtil.list();
        // Slow implementation: iterates over all known resources...
        List<AWResource> allResources = resourceManager.allResources();
        for (AWResource resource : allResources) {
            String path = resource.relativePath();
            if (path.startsWith(prefix) && path.endsWith(".csv")) {
                URL url;
                try {
                    url = new URL(resource.fullUrl());
                } catch (MalformedURLException e) {
                    throw new AWGenericException(e);
                }
                result.add(url);
            }
        }
        return result;
    }

    public void prepareAllLoads (AWMultiLocaleResourceManager resourceManager, String prefix)
    {
        List<URL> loadUrls = findLoads(resourceManager, prefix);
        for (URL url : loadUrls) {
            prepareLoad(url);
        }
    }

    public Load prepareLoad (URL csvUrl)
    {
        Load load = new Load(csvUrl);
        _loads.add(load);
        return load;
    }

    public void runLoads ()
    {
        List<Load> ordered = orderLoads(_loads);
        for (Load load : ordered) {
            load.run();
        }
    }

    ClassCandidateKeyCache cacheForClass (Class cls)
    {
        ClassCandidateKeyCache cache = _lookupKeyCaches.get(cls);
        if (cache == null) {
            cache = new ClassCandidateKeyCache(cls);
            _lookupKeyCaches.put(cls, cache);
        }
        return cache;
    }

    Object lookupByKey (Class cls, String foreignKey, Object keyVal)
    {
        return cacheForClass(cls).lookup(foreignKey, keyVal);
    }

    List<Load> orderLoads (Collection<Load> loads)
    {
        // Ordering: We make sure loads for classes we depend on get added first
        Map<Class, List<Load>> classToLoads = AWUtil.groupBy(loads, new AWUtil.ValueMapper() {
            public Object valueForObject (Object object)
            {
                return ((Load)object)._cls;
            }
        });
        Set<Load> addedLoads = SetUtil.set();
        List<Load> ordered = ListUtil.list();
        _addLoads(loads, addedLoads, classToLoads, ordered);
        return ordered;
    }

    void _addLoads (Collection<Load> loads, Set<Load> added, Map<Class, List<Load>> classToLoads, List<Load> ordered)
    {
        if (loads == null) return;
        for (Load load : loads) {
            if (added.contains(load)) continue;
            added.add(load);
            for (Class predClass : load._dependsOn) {
                // todo: Need to order lists for a class soe that root load runs before many-to-many loads
                // (What if a many to many has its depends-on include both classes?)
                List<Load> classLoads = classToLoads.get(predClass);
                _addLoads(classLoads, added, classToLoads, ordered);
            }
            ordered.add(load);
        }
    }

    public class Load
    {
        URL _url;
        Class _cls;
        List<FieldProcessor> _fieldProcessors;
        Map <String, Integer> _fieldIndexByKey;
        List<Class> _dependsOn = ListUtil.list();
        RowSource _rowSource;
        ClassCandidateKeyCache _lookupKeyCache;
        boolean _haveSkippedFirst;

        public Load (URL url)
        {
            _url = url;
            _rowSource = new CSVRowSource(_url);

            // Compute class from file name
            _cls = classFromUrl(url.toString(), _rowSource.fileExtension());
            Assert.that(_cls != null, "Could not find class for url: %s", _url);
            _lookupKeyCache = cacheForClass(_cls);

            // Read fields from CSV header
            List<String> keys = _rowSource.keys();
            _fieldProcessors = ListUtil.list();
            _fieldIndexByKey = MapUtil.map();
            _metaContext.push();
            _metaContext.set(ObjectMeta.KeyClass, _cls.getName());
            _metaContext.set(UIMeta.KeyOperation, "edit");          // todo: use "load"

            for (int i=0, c=keys.size(); i < c; i++) {
                String key = keys.get(i);
                _fieldIndexByKey.put(key, i);
                FieldProcessor processor = new FieldProcessor(key);
                _fieldProcessors.add(processor);
                Class depClass = processor.dependsOn();
                if (depClass != null) ListUtil.addElementIfAbsent(_dependsOn, depClass);
            }

            _metaContext.pop();
        }

        public void run ()
        {
            _rowSource.readAll(new LineConsumer() {
                final int processorCount = _fieldProcessors.size();
                Object[] values = new Object[processorCount];
                public void process (List<String> strings)
                {
                    if (!_haveSkippedFirst) { _haveSkippedFirst = true; return; }
                    
                    Assert.that(processorCount == strings.size(), "Mismatch between header column and row value count (%s != %s): %s",
                            processorCount, strings.size(), strings.toString());
                    for (int i=0; i < processorCount; i++) {
                        values[i] = _fieldProcessors.get(i).prepareValue(strings.get(i));
                    }

                    // Todo: commit after every N rows
                    Object obj = findOrCreate(values);
                    for (int i=0; i < processorCount; i++) {
                        _fieldProcessors.get(i).assignValue(obj, values[i]);
                    }
                }
            });
            _ctx.save();
        }

        Object findOrCreate (Object[] values)
        {
            // lookup in caches
            for (ClassCandidateKeyCache.CandidateKeyCache cache : _lookupKeyCache._haveCreatesCaches) {
                Object val = values[_fieldIndexByKey.get(cache._key)];
                Object obj = cache.lookup(val);
                if (obj != null) return obj;
            }

            // not found.  Create it
            // Todo: support predefined lookup key field(s) and cache on here
            return _ctx.create(_cls);
        }

        class FieldProcessor
        {
            String _propKey;
            String _foreignKey;
            Class _type;
            Class _elementType;
            Class _destType;
            Object _formatter;
            FieldPath _fieldPath;

            FieldProcessor (String key)
            {
                int dot = key.indexOf(".");
                if (dot != -1) {
                    _propKey = key.substring(0, dot);
                    _foreignKey = key.substring(dot+1);
                } else {
                    _propKey = key;
                }

                _fieldPath = new FieldPath(_propKey);

                _metaContext.push();
                _metaContext.set(ObjectMeta.KeyField, _propKey);

                String type = (String)_metaContext.propertyForKey(ObjectMeta.KeyType);
                Assert.that(type != null, "No type for field: %s", key);
                _type = ClassUtil.classForName(type);
                Assert.that(_type != null, "No class found for type for field: %s", key);
                _destType = _type;

                if (_foreignKey != null) {
                    // Get info for destination key (key path)
                    String elementType = (String)_metaContext.propertyForKey(ObjectMeta.KeyElementType);
                    if(elementType != null) {
                        _elementType = ClassUtil.classForName(elementType);
                        Assert.that(_elementType != null, "No class found for elementType '%s' for field: %s", elementType, key);
                        _destType = _elementType;
                    }
                    _metaContext.push();
                    _metaContext.set(ObjectMeta.KeyClass, _destType.getName());
                    _metaContext.set(ObjectMeta.KeyField, _foreignKey);
                    _formatter = _metaContext.resolveValue(FieldValue.getFieldValue(_metaContext.properties(), "bindings.formatter"));

                    _metaContext.pop();
                } else {
                    _formatter = _metaContext.resolveValue(FieldValue.getFieldValue(_metaContext.properties(), "bindings.formatter"));

                }

                // todo:  ownedToMany? manyToMany?
                _metaContext.pop();
            }

            Object prepareValue (String fieldValue)
            {
                Object val = (_formatter != null) ? AWFormatting.get(_formatter).parseObject(_formatter, fieldValue)
                                                  : fieldValue;

                if (_foreignKey != null) {
                    val = lookupByKey(_destType, _foreignKey, val);
                }
                return val;
            }

            void assignValue (Object obj, Object val)
            {
                if (_elementType != null) {
                    RelationshipField.addTo(obj, _fieldPath, val);
                } else {
                    _fieldPath.setFieldValue(obj, val);
                }
            }

            Class dependsOn ()
            {
                return (_foreignKey != null) ? _destType : null;
            }

        }
    }

    static Class classFromUrl (String url, String extension)
    {
        String name = AWUtil.lastComponent(url, '/').replaceAll("\\."+extension+"$", "").replaceAll("-.+$", "");
        return ClassUtil.classForName(name);
    }

    interface LineConsumer
    {
        void process (List<String> values);
    }

    abstract static class RowSource
    {
        public abstract String fileExtension ();
        public abstract List<String> keys ();
        public abstract void readAll (LineConsumer consumer);
    }

    static class AbortedRead extends RuntimeException {}

    static class CSVRowSource extends RowSource implements CSVConsumer
    {
        URL _url;
        List<String> _lastRow;
        LineConsumer _consumer;

        public CSVRowSource (URL url)
        {
            _url = url;
        }

        public String fileExtension ()
        {
            return "csv";
        }

        public List<String> keys ()
        {
            // read first line (we have to launch a read and then bail out upon the first callback)
            CSVReader reader = new CSVReader(this);
            InputStream is = null;
            try {
                is = _url.openStream();
                Reader in = IOUtil.bufferedReader(is, I18NUtil.EncodingUTF8);
                reader.read(in, _url.toString());
            } catch (IOException e) {
                throw new AWGenericException(e);
            } catch (AbortedRead e) {
                // expected... ignore
                try {
                    if (is != null) is.close();
                } catch (IOException e2) {
                    throw new AWGenericException(e2);
                }
            }
            return _lastRow;
        }

        public void readAll (LineConsumer consumer)
        {
            _consumer = consumer;
            CSVReader reader = new CSVReader(this);
            try {
                reader.read(_url, I18NUtil.EncodingUTF8);
            } catch (IOException e) {
                throw new AWGenericException(e);
            } catch (AbortedRead e) {
                // expected... ignore
            }
        }

        public void consumeLineOfTokens (String path, int lineNumber, List line)
        {
            _lastRow = line;
            if (_consumer == null) throw new AbortedRead();
            _consumer.process(line);
        }
    }

    class ClassCandidateKeyCache
    {
        Class _cls;
        Map <String, CandidateKeyCache> _caches = MapUtil.map();   // LRU HashTable?
        List<CandidateKeyCache> _haveCreatesCaches = ListUtil.list();

        ClassCandidateKeyCache (Class cls)
        {
            _cls = cls;
        }

        Object lookup (String key, Object val)
        {
            CandidateKeyCache cache = _caches.get(key);
            if (cache == null) {
                cache = new CandidateKeyCache(key);
                _caches.put(key, cache);
            }
            return cache.lookup(val);
        }

        class CandidateKeyCache
        {
            String _key;
            boolean _didCreate;

            CandidateKeyCache (String key)
            {
                _key = key;
            }

            Map <Object, Object> _objectsByKey = MapUtil.map();

            Object lookup (Object val)
            {
                Object obj = _objectsByKey.get(val);
                if (obj == null) {
                    obj = _ctx.findOne(_cls, Collections.singletonMap(_key, val));
                    if (obj == null) {
                        obj = _ctx.create(_cls);
                        FieldValue.setFieldValue(obj, _key, val);
                        if (!_didCreate) {
                            _didCreate = true;
                            _haveCreatesCaches.add(this);
                        }
                    }
                }
                return obj;
            }
        }
    }

}
