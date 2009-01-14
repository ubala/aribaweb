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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/Parameters.java#3 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.Assert;
import ariba.util.core.DebugState;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
    Servlet style parameters from URL or CGI or RFC822 and friends

    The parameter names are case insensitive.

    We use the java.util classes because the Servlet API requires it.

    @aribaapi private
*/
public class Parameters implements Externalizable, DebugState
{
    public static final String ClassName = "ariba.ui.aribaweb.util.Parameters";

    /**
        List of original case names
    */
    private java.util.Vector names = new java.util.Vector();

    /**
        Mapping from case-insensitive names to values
    */
    private Hashtable hashtable = new Hashtable();

    /**
        Any class that implements Externalizable must have
        a constructor with no arguments.
    */
    public Parameters()
    {
    }

    public Parameters(Map hashtable)
    {
        Iterator k = hashtable.keySet().iterator();
        Iterator e = hashtable.values().iterator();
        while (k.hasNext()) {
            putParameter((String)k.next(), (String)e.next());
        }
    }

    private Parameters(Hashtable hashtable)
    {
    }

    public void putParameter (String name, String value)
    {
        String original = name;
        name = name.toUpperCase();
        if (hashtable.containsKey(name)) {
            List vector = (List)hashtable.get(name);
            vector.add(value);
            return;
        }

        List vector = ListUtil.list(value);
        hashtable.put(name, vector);
        names.add(original);
    }

    public String getParameter (String name)
    {
        name = name.toUpperCase();
        List vector = (List)hashtable.get(name);
        if (vector == null) {
            return null;
        }
        Assert.that(vector.size() == 1,
                    "asked for a multi-valued parameter %s singularly, %s",
                    name, vector.toString());
        return (String)ListUtil.firstElement(vector);
    }

    public int getParameterCount ()
    {
        return hashtable.size();
    }

    public String[] getParameterValues (String name)
    {
        name = name.toUpperCase();
        List vector = (List)hashtable.get(name);
        return VectorToStringArray(vector);
    }

    public Iterator getParameterValuesIterator (String name)
    {
        name = name.toUpperCase();
        List vector = (List)hashtable.get(name);
        if (vector == null) {
            return null;
        }
        return vector.iterator();
    }

    private String[] VectorToStringArray (List vector)
    {
        if (vector == null) {
            return null;
        }
        int      length  = vector.size();
        String[] strings = new String[length];
        vector.toArray(strings);
        return strings;
    }

    public Iterator getParameterNames ()
    {
        return names.iterator();
    }

    public String removeParameter (String name)
    {
        name = name.toUpperCase();
        List vector = (List)hashtable.remove(name);
        if (vector == null) {
            return null;
        }
        removeName(name);
        return (String)ListUtil.firstElement(vector);
    }

    public String[] removeParameterValues (String name)
    {
        name = name.toUpperCase();
        List vector = (List)hashtable.remove(name);
        removeName(name);
        return VectorToStringArray(vector);
    }

    private void removeName (String name)
    {
        for (int i = 0, s = names.size(); i < s ; i++) {
            String string = (String)names.get(i);
            if (string.equalsIgnoreCase(name)) {
                names.remove(i);
                return;
            }
        }
    }

    /**
        Convenience method which removes the given parameter if it exists,
        then puts the new parameter value.  Returns the previous parameter
        value.
    */
    public String replaceParameter (String name, String value)
    {
        String result = removeParameter(name);
        putParameter(name, value);
        return result;
    }

    /**
        Implementation of the Externalizable interface

        @aribaapi private
    */
    public void writeExternal (ObjectOutput output) throws IOException
    {
        output.writeObject(hashtable);
    }

    /**
        Implementation of the Externalizable interface

        @aribaapi private
    */
    public void readExternal (ObjectInput input)
      throws IOException, ClassNotFoundException
    {
        hashtable = (Hashtable)input.readObject();
    }

    public String toString ()
    {
        return hashtable.toString();
    }

    public Object debugState ()
    {
        return MapUtil.map(hashtable);
    }

    public void clear ()
    {
        hashtable.clear();
        names.clear();
    }
}