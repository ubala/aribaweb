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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWBindingApi.java#11 $
*/

package ariba.ui.aribaweb.core;

import java.util.Map;
import ariba.util.core.MapUtil;
import ariba.util.core.StringUtil;
import ariba.util.fieldvalue.FieldValue;
import ariba.ui.aribaweb.util.AWUtil;

public final class AWBindingApi extends AWApiDeclaration
{
    protected static final int Both = 2;
    protected static final int Either = 3;
    private Map _bindings;

    // required bindings
    public static final String Key          = "key";
    public static final String Direction    = "direction";
    public static final String Type         = "type";

    // optional bindings
    public static final String Required     = "required";
    public static final String Alternates   = "alternates";
    public static final String Default      = "default";

    // for debugging
    public String toString ()
    {
        AWBinding binding = getBinding(Type, true);
        String className = binding.stringValue(null);
        binding = getBinding(Direction, true);
        String directionString = (String)binding.value(null);

        binding = getBinding(Default, false);
        String defaultValue = (binding != null) ? binding.stringValue(null) : "";

        return ("key: "+key()+ " type: " + className + " direction: " + directionString +
                " required: " + isRequired() + " alternates: " + alternates() + " default: " + defaultValue);
    }
    
    public void init (String tagName, Map bindingsHashtable)
    {
        _bindings = MapUtil.cloneMap(bindingsHashtable);
        super.init();
    }

    private AWBinding getBinding (String key, boolean required)
    {
        AWBinding binding = (AWBinding)_bindings.get(key);
        if (required && binding == null) {
            throw new RuntimeException("AWBindingApi missing required binding specification \"" + key + "\"");
        }
        return binding;
    }

    private AWBinding getBinding (String key)
    {
        return getBinding(key, false);
    }

    public String key ()
    {
        AWBinding binding = getBinding(Key, true);
        return binding.stringValue(null);
    }

    public int direction ()
    {
        AWBinding binding = getBinding(Direction, true);
        String directionString = (String)binding.value(null);
        int direction = -1;
        if (directionString.equals("get")) {
            direction = FieldValue.Getter;
        }
        else if (directionString.equals("set")) {
            direction = FieldValue.Setter;
        }
        else if (directionString.equals("both")) {
            direction = AWBindingApi.Both;
        }
        else if (directionString.equals("either")) {
            direction = AWBindingApi.Either;
        }
        else {
            throw new RuntimeException("unrecognized direction: " + directionString);
        }
        return direction;
    }

    // defaults to false;
    public boolean isRequired ()
    {
        AWBinding binding = getBinding(Required);
        return binding == null ? false : binding.booleanValue(null);
    }

    public Object defaultValue ()
    {
        Object defaultValue = null;
        AWBinding binding = getBinding(Default, false);
        if (binding != null) {
            defaultValue = binding.value(null);
        }

        return defaultValue;
    }

    public Class type ()
    {
        Class typeClass = null;
        AWBinding binding = getBinding(Type, true);
        String className = binding.stringValue(null);
        if (className.equals("int")) {
            typeClass = Integer.TYPE;
        }
        else if (className.equals("boolean")) {
            typeClass = Boolean.TYPE;
        }
        else if (className.equals("String")) {
            typeClass = String.class;
        }
        else {
            typeClass = AWConcreteApplication.SharedInstance.resourceManager().classForName(className);
        }
        if (typeClass == null) {
            throw new RuntimeException("AWBindingApi: Unrecognized type specified \"" + className + "\"");
        }
        return typeClass;
    }

    public String alternates ()
    {
        AWBinding binding = getBinding(Alternates);
        return (binding != null) ? binding.stringValue(null) : "";
    }

    public String[] alternatesArray ()
    {
        String[] alternates = null;
        String sAlternates = alternates();
        if (StringUtil.nullOrEmptyOrBlankString(sAlternates)) {
            return alternates;
        }

        alternates = AWUtil.parseComponentsString(sAlternates,",");
        return alternates;
    }

    public String directionString ()
    {
        AWBinding binding = getBinding(Direction, true);
        return (String)binding.value(null);
    }

    public String typeString ()
    {
        AWBinding binding = getBinding(Type, true);
        String className = binding.stringValue(null);
        return className;
    }

    public void validate (AWValidationContext validationContext, AWComponent component)
    {
        // todo: should use DTD / schema to validate BindingApi tags
        // todo: validate class types
    }
}
