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

    $Id: //ariba/platform/ui/demoshell/ariba/ui/demoshell/AWXSJSHTMLComponent.java#2 $
*/

// This component is used for any included HTML template.
//
// We implement a couple of convenience for such java-less components:
// 1) dynamic fields (i.e. "$myField" is auto-created)
// 2) action generation: ("$goto.somePage" does an HTML goto that page)

package ariba.ui.demoshell;

import ariba.util.fieldvalue.FieldValue;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Function;

public class AWXSJSHTMLComponent extends AWXHTMLComponent implements AWXScriptFactory.Embedder
{
    static {
        Class dummy = AWXScriptFactory.class; // Force initialization
        FieldValue.registerClassExtension(AWXSJSHTMLComponent.class,
                               new AWXScriptFactory.FieldValue_ScriptEmbedder());
    }

    Object _scriptObject;  // our script state
    static String ScriptObjNullMarker = "NULL";

    protected Object callFunction (String name)
    {
        Scriptable scope = scriptScope();
        Object result = null;
        if (scope != null) {
            // Call init()
            Object prop = ScriptRuntime.getProp(scope, name, scope);
            if ((prop != null) && (prop instanceof Function)) {
                result = AWXScriptFactory.sharedInstance().call(scope, (Function)prop, null);
            }
        }
        return result;
    }

    public void init ()
    {
        Log.demoshell.debug("*** Initializing instance %s (%s)", componentDefinition().componentName(), this);
        super.init();
        callFunction("init");  // force initialization of our script instance immediately
    }

    /////////////
    // Awake
    /////////////
    protected void awake ()
    {
        super.awake();
        callFunction("awake");  // force initialization of our script instance immediately
    }

    protected void sleep ()
    {
        super.sleep();
        callFunction("sleep");  // force initialization of our script instance immediately
    }

    /**
     * Implementation of AWXScriptFactory.Embedder: returns instance (if any) of
     * our script instance.
     */
    public Scriptable scriptScope ()
    {
        if (_scriptObject == ScriptObjNullMarker) return null;

        AWXServerScript scriptTag = AWXServerScript.instanceInComponent(this);
        if ((_scriptObject == null) || scriptTag.isStale((Scriptable)_scriptObject, this)) {
            if (scriptTag != null) {
                _scriptObject = scriptTag.createInstance(this);
            }
            if (_scriptObject == null) {
                _scriptObject = ScriptObjNullMarker;
            }
        }
        return (Scriptable)((_scriptObject == ScriptObjNullMarker) ? null : _scriptObject);
    }

    public Scriptable scriptClassScope ()
    {
        Scriptable instanceScope = scriptScope();
        return (instanceScope == null) ? null : instanceScope.getPrototype();
    }

    /**
     * Silly debugging method
     */
    public String javaScriptString ()
    {
        AWXServerScript tag = AWXServerScript.instanceInComponent(this);
        if (tag != null) {
            return tag.scriptString();
        }
        return null;
    }
}
