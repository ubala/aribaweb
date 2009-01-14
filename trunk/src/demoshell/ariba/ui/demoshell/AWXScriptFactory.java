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

    $Id: //ariba/platform/ui/demoshell/ariba/ui/demoshell/AWXScriptFactory.java#21 $
*/

package ariba.ui.demoshell;

import org.mozilla.javascript.*;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.util.fieldvalue.OrderedList;
import ariba.ui.aribaweb.util.AWResource;
import ariba.util.core.Assert;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.FormatBuffer;
import ariba.util.core.MultiKeyHashtable;
import ariba.util.fieldvalue.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

public class AWXScriptFactory
{
    static private AWXScriptFactory _SharedInstance = null;

    protected Scriptable _globalScope = null;
    protected JavaScriptErrorReporter _errorReporter = null;

    static {
        // register FieldValue support for JavaScript objects
        FieldValue.registerClassExtension(ScriptableObject.class, new FieldValue_ScriptableObject());

        // register AW support for JavaScript language arrays
        OrderedList.registerClassExtension(ScriptableObject.class, new JSOrderedList());
    }

    public static AWXScriptFactory sharedInstance ()
    {
        if (_SharedInstance == null) {
            _SharedInstance = new AWXScriptFactory();
        }
        return _SharedInstance;
    }

    Context enterContext ()
    {
        Context context = Context.enter();
        context.setCompileFunctionsWithDynamicScope(true);

        // Rhino-1.5: setDebugLevel() not supported
        //context.setDebugLevel(1);
        context.setErrorReporter(errorReporter());
        return context;
    }

    JavaScriptErrorReporter errorReporter ()
    {
        if (_errorReporter == null) {
            _errorReporter = new JavaScriptErrorReporter();
        }
        return _errorReporter;
    }

    Scriptable globalScope ()
    {
        if (_globalScope == null) {
            Context cx = enterContext();
            _globalScope = cx.initStandardObjects(null);

            // initialize context with some helpful globals
            AWResource resource = AWConcreteApplication.defaultApplication().resourceManager().resourceNamed("ariba/ui/demoshell/AWXScript.js");

            try {
                InputStreamReader reader = new InputStreamReader(resource.inputStream());
                cx.evaluateReader(_globalScope, reader, resource.relativePath(), 0, null);
                ariba.ui.demoshell.Log.demoshell.debug("Finished compiling script: %s", reader);
            } catch (IOException e) {
                throw new AWGenericException("Error reading JavaScript init file", e);
            } catch (JavaScriptException e) {
                throw new AWGenericException("Error evaluating JavaScript init file", e);
            }

            cx.exit();
        }
        return _globalScope;
    }

    static Scriptable newObject (Context cx, Scriptable parentScope)
    {
        Scriptable newScope = null;
        try {
            newScope = cx.newObject(parentScope);
        }
        catch (Exception e) { // OK
            throw new AWGenericException(e);
        }
        return newScope;
    }

    Script scriptForString (String source)
    {
        Scriptable global = globalScope();
        Context cx = enterContext();
        try {
            Reader in = new StringReader(source);
            return cx.compileReader(global, in, "ComponentScript", 1, null);
        } catch (IOException e) {
            Assert.that(false, "IOException from StringReader");
            return null;
        }
    }

    Scriptable classScopeForString (String source)
    {
        Context cx = enterContext();
        Scriptable global = globalScope();
        ScriptableObject scope = (ScriptableObject)newObject(cx, global);
        scope.setPrototype(global);

        // scope should be top level so that new variables declared in the script
        // are created here.
        scope.setParentScope(null);

        Script script = null;

        try {
            Reader in = new StringReader(source);
            script = cx.compileReader(scope, in, "ComponentScript", 1, null);
            scope.put("component", scope, null);  // pseduo "this"

            // run script to create functions and variables
            script.exec(cx, scope);
            scope.sealObject();  // no modifications to its properties...
        } catch (IOException e) {
            Assert.that(false, "IOException from StringReader");
            return null;
        } catch (JavaScriptException e) {
            throw new AWGenericException("Exception executing compiled script", e);
        }

        // we should now have an initialized class scope
        return scope;
    }

    Scriptable instanceForClassScope (Scriptable classScope, Object component)
    {
        // we create a new scope for this instance, with the class scope as
        // its "prototype".
        // "component" is assigned as an instance variable of the scope.
        Context cx = enterContext();

        // we create this object with the class scope as its prototype
        Scriptable instanceScope = newObject(cx, classScope);

        // if all goes well, this means that all function and variable lookups
        // go to our "class" script
        instanceScope.setPrototype(classScope);

        // But any new variables created during execution of the script should
        // be within this instance.
        instanceScope.setParentScope(null);
        instanceScope.put("component", instanceScope, component);

        cx.exit();

        return instanceScope;
    }

    Object call (Scriptable scope, Function f, Object[] args)
    {
        Object result = null;

        Context cx = enterContext();
        try {
            result = f.call(cx, scope, scope, args);
        } catch (JavaScriptException e) {
            throw new AWGenericException("Error calling method: " + f, e);
        } finally {
            cx.exit();
        }

        return result;
    }

    Object call (Context cx, Scriptable scope, String methodName, Object[] args)
    {
        Object o = ScriptRuntime.getProp(scope, methodName, scope);
        Function m1 = (Function)o;
        Object result = null;
        try {
            result = m1.call(cx, scope, scope, args);
        } catch (JavaScriptException e) {
            throw new AWGenericException("Error calling method: " + methodName, e);
        }

        return result;
    }

    /**
     * For objects (like AWXHTMLComponent) which embed JavaScript
     *   -- script enbedders must also support the Extensible interface
     */
    interface Embedder extends Extensible
    {
        Scriptable scriptScope ();
        Scriptable scriptClassScope ();
    }

    /**
        A dummy/marker class used to maintain a negative cache for accessors which are not
        found.
    */
    static class NotFoundFieldValueAccessor extends BaseAccessor
    {
        public NotFoundFieldValueAccessor ()
        {
            super(Object.class, "NotFound");
        }

        public Object getValue (Object target){return null;}
        public void setValue (Object target, Object value){}
    }

    /**
     * FieldValue implementation for objects that have embedded JavaScripts
     * (e.g. AWXHTMLEmbeddedComponent).
     *
     * For properties of the script (as opposed to the root Java object) this
     * forwards the FieldValue call to the implementation on that class
     * (see FieldValue_ScriptableObject below)
     */
    public static class FieldValue_ScriptEmbedder extends FieldValue_Object
    {
        protected static final FieldValueAccessor NotFoundAccessor = new NotFoundFieldValueAccessor();
        // Key is <class, classScope, key>
        protected final MultiKeyHashtable[] _accessorsHashtable = {
            new MultiKeyHashtable(3, 8 , true),
            new MultiKeyHashtable(3, 8, true),
        };

        public FieldValueAccessor getAccessor (Object target, String fieldName, int type)
        {
            Embedder embedder = (Embedder)target;
            Object classScope = embedder.scriptClassScope();
            if (classScope == null) classScope = "";  // avoid NULL key

            FieldValueAccessor accessor = null;
            Class targetObjectClass = target.getClass();
            MultiKeyHashtable accessorsHashtable = _accessorsHashtable[type];
            synchronized (accessorsHashtable) {
                fieldName = fieldName.intern();
                accessor = (FieldValueAccessor)accessorsHashtable.get(targetObjectClass, classScope,
                                                                      fieldName);
                if (accessor == NotFoundAccessor) {
                    accessor = null;
                }
                else if (accessor == null) {
                    accessor = createAccessor(target, fieldName, type);
                    accessorsHashtable.put(targetObjectClass, classScope, fieldName,
                                            (accessor != null) ? accessor : NotFoundAccessor);
                }
            }
            return accessor;
        }

        /**
         * We override createAccessor to create a JavaScript accessor
         * if Object property exists.
         * This implementation is highly bogus for the following reasons:
         * 1) a javascript method cannot override a Java method
         * XXX: Should extend FieldValue_Extensible, rather than Object?
         */
        public FieldValueAccessor createAccessor (Object receiver, String fieldName, int type)
        {
            FieldValueAccessor accessor = super.createAccessor(receiver, fieldName, type);
            if (accessor == null) {
                Embedder embedder = (Embedder)receiver;

                if (type == FieldValue.Setter) {
                    accessor = SetAccessor.createAccessor(embedder, fieldName);
                } else {
                    accessor = GetAccessor.createAccessor(embedder, fieldName);
                }
            }
            if ((accessor == null) && (receiver instanceof Extensible)) {
                accessor = _AWXFieldValueInsider.extensibleFieldValueAccessor(receiver, fieldName);
            }
            return accessor;
        }
    }

    protected static class SetAccessor extends BaseAccessor
            implements FieldValueSetter
    {
        protected Function _function;

        public static SetAccessor createAccessor (Embedder target, String fieldName)
        {
            Scriptable classScope = target.scriptClassScope();
            if (classScope != null) {
                Function function = FieldValue_ScriptableObject.functionWithPrefix (classScope, fieldName, "set");
                if ((function != null) || (ScriptRuntime.getProp(classScope, fieldName, classScope) != Context.getUndefinedValue()))
                {
                    return new SetAccessor (target.getClass(), fieldName, function);
                }
            }
            return null;
        }

        protected SetAccessor (Class cls, String fieldName, Function f)
        {
            super(cls, fieldName);
            _function = f;
        }

        public void setValue (Object target, Object value)
        {
            // forward to implementation on the scope
            Scriptable scope = ((Embedder)target).scriptScope();
            if (_function != null) {
                Object[] args = new Object[1];
                args[0] = value;
                AWXScriptFactory.sharedInstance().call(scope, _function, args);
            } else {
                ScriptRuntime.setProp(scope, getFieldName(), value, scope);  
            }
        }
    }

    protected static class GetAccessor extends BaseAccessor
            implements FieldValueGetter
    {
        protected Function _function;

        public static GetAccessor createAccessor (Embedder target, String fieldName)
        {
            Scriptable classScope = target.scriptClassScope();
            if (classScope != null) {
                Function function = FieldValue_ScriptableObject.functionWithPrefix (classScope, fieldName, "get");
                Object prop = null;
                boolean propDefined = false;

                if (function == null) {
                    prop = ScriptRuntime.getProp(classScope, fieldName, classScope);
                    propDefined = classScope.has(fieldName, classScope);
                    if (prop == Context.getUndefinedValue())  {
                        prop = null;
                    }
                    else if (prop instanceof Function) {
                        function = (Function)prop;
                    }
                }
                if (function != null || prop != null || propDefined) {
                    return new GetAccessor (target.getClass(), fieldName, function);
                }
            }
            return null;
        }

        protected GetAccessor (Class cls, String fieldName, Function f)
        {
            super(cls, fieldName);
            _function = f;
        }

        public Object getValue (Object target)
        {
            // forward to implementation on the scope
            Scriptable scope = ((Embedder)target).scriptScope();
            String fieldName = getFieldName();
            Object result = null;

            if (_function != null) {
                result = AWXScriptFactory.sharedInstance().call(scope, _function, null);
            } else {
                result = ScriptRuntime.getProp(scope, fieldName, scope);  // unwrap?
            }

            if ((result != null) && (result instanceof Wrapper)) {
                result = ((Wrapper)result).unwrap();
            }

            return result;
        }
    }

    /**
     * FieldValue implementation for JavaScript object.
     *
     * We model this after the implementation on Map (i.e. no accessors).
     * Sadly, we extend FiledValue_Object to inherit the implementation of
     * the field-path based accessors (which are implemented in terms of
     * the primitives that we override here).
     */
    public static class FieldValue_ScriptableObject extends FieldValue_Object
    {
        public Object getFieldValuePrimitive (Object receiver, FieldPath fieldPath)
        {
            Scriptable scope = (Scriptable)receiver;
            String fieldName = fieldPath.car();
            Function getter = functionWithPrefix(scope, fieldName, "get");
            Object result = null;

            if (getter == null) {
                result = ScriptRuntime.getProp(scope, fieldName, scope);  // unwrap?
                if (result == Context.getUndefinedValue()) {
                    throw new FieldValueException ("" + receiver.getClass() +": Attempt to get unknown property, '" + fieldName +"'");
                }
                if (result instanceof Function) {
                    getter = (Function)result;
                }
            }

            if (getter != null) {
                result = AWXScriptFactory.sharedInstance().call(scope, getter, null);
            }

            if ((result != null) && (result instanceof Wrapper)) {
                result = ((Wrapper)result).unwrap();
            }

            return result;
         }

        public void setFieldValuePrimitive (Object receiver, FieldPath fieldPath, Object value)
        {
            Scriptable scope = (Scriptable)receiver;
            String fieldName = fieldPath.car();

            Function setter = functionWithPrefix(scope, fieldName, "set");
            if (setter != null) {
                Object[] args = new Object[1];
                args[0] = value;
                AWXScriptFactory.sharedInstance().call(scope, setter, args);
            } else if (scope.getPrototype().has(fieldName, scope)) {
                scope.put(fieldPath.car(), scope, value);  // wrap?
            } else {
                throw new FieldValueException(receiver.getClass().getName() +
                                          " -- Attempt to set unknown property: '" + fieldName + "'");
            }
        }

        protected static Function functionWithPrefix (Scriptable scope,
                                         String fieldName, String prefix)
        {
            FastStringBuffer buf = new FastStringBuffer();
            buf.append(prefix);
            buf.append(Character.toUpperCase(fieldName.charAt(0)));
            int len = fieldName.length();
            if (len > 1) buf.appendStringRange(fieldName, 1, len);
            String accessorName = buf.toString();
            Object prop = ScriptRuntime.getProp(scope, accessorName, scope);
            return ((prop != null) && (prop instanceof Function))
                    ? (Function)prop : null;
        }

        /**
         * Overridden to disable.  This throws an exception if called.
         */
        public FieldValueAccessor createAccessor (Object receiver, String fieldName, int type)
        {
            throw new FieldValueException(receiver.getClass().getName() +
                                          ": createAccessor() not suported");
        }

        /**
         *Overridden to disable.  This throws an exception if called.
         */
        public FieldValueAccessor getAccessor (Object receiver, String fieldName, int type)
        {
            throw new FieldValueException(receiver.getClass().getName() +
                                          ": getAccessor() not suported");
        }
    }


    public static class JSOrderedList extends OrderedList {
        // ** Thread Safety Considerations: no state here -- no locking required.
        public int size (Object receiver)
        {
            // xxx-Rhino Version 1.5: jsGet_length()
            return (int)((NativeArray)receiver).jsGet_length();
        }

        public Object elementAt (Object receiver, int elementIndex)
        {
            ScriptableObject obj = (ScriptableObject)receiver;
            Object val = obj.get(elementIndex, obj);
            if ((val != null) && (val instanceof Wrapper)) {
                val = ((Wrapper)val).unwrap();
            }
            return val;
        }

        public void setElementAt (Object receiver, Object element, int elementIndex)
        {
            ScriptableObject obj = (ScriptableObject)receiver;
            obj.put(elementIndex, obj, element);
        }

        public void addElement (Object receiver, Object element)
        {
            throwUnsupportedApiException("addElement(Object receiver, Object element)");
        }

        public void insertElementAt (Object receiver, Object element, int elementIndex)
        {
            throwUnsupportedApiException("insertElementAt (Object receiver, Object element, int elementIndex)");
        }

        public Object mutableInstance (Object receiver)
        {
            throwUnsupportedApiException("mutableInstance (Object receiver)");
            return null;
        }

        public Object sublist (Object receiver, int beginIndex, int endIndex)
        {
            throwUnsupportedApiException("sublist (Object receiver, beginIndex, endIndex)");
            return null;
        }

        private void throwUnsupportedApiException (String methodName)
        {
            throw new AWGenericException("Error: Java arrays do not support: \"" + methodName + "\"");
        }
    }

    /**
        A custom ErrorReporter that uses our Logging.
    */
    class JavaScriptErrorReporter implements ErrorReporter
    {
        public void warning (String message,
                             String sourceName,
                             int    line,
                             String lineSource,
                             int    lineOffset)
        {
            Log.demoshell.debug(
                "--- JavaScript Warning: %s",
                string(message, sourceName, line, lineSource, lineOffset));
        }

        public void error (String message,
                           String sourceName,
                           int    line,
                           String lineSource,
                           int    lineOffset)
        {
            throw new AWGenericException("JavaScript Error -- " + sourceName + ":" + line + "("+lineSource+"): " + message);
        }

        public EvaluatorException runtimeError (String message,
                                                String sourceName,
                                                int    line,
                                                String lineSource,
                                                int    lineOffset)
        {
            return new EvaluatorException(string(message, sourceName, line, lineSource, lineOffset));
        }

        String string;

        public String string (String message,
                              String sourceName,
                              int    line,
                              String lineSource,
                              int    lineOffset)
        {
            FormatBuffer buffer = new FormatBuffer();
            if (sourceName != null) {
                buffer.append(sourceName);
                buffer.append('(');
                buffer.append(line);
                if (lineOffset > 0) {
                    buffer.append(',');
                    buffer.append(lineOffset);
                }
                buffer.append(')');
                buffer.append(':');
                buffer.append(':');
                if (lineSource != null) {
                    buffer.append(lineSource);
                    buffer.append('\n');
                }
            }
            buffer.append(message);
            string = buffer.toString();
            return string;
        }
    }
}

