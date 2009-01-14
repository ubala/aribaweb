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

    $Id: //ariba/platform/ui/demoshell/ariba/ui/demoshell/AWXServerScript.java#9 $
*/

/**
 * AWXServerScript tag delimits JavaScript meant to extend AW components on the
 * server.
 */

package ariba.ui.demoshell;

import org.mozilla.javascript.*;
import ariba.ui.aribaweb.core.AWContainerElement;
import ariba.ui.aribaweb.core.AWElement;
import ariba.ui.aribaweb.core.AWTemplate;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWBareString;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWHtmlTemplateParser;
import ariba.util.core.FastStringBuffer;

import java.io.File;

public class AWXServerScript extends AWContainerElement implements AWXHTMLComponentFactory.ScriptClassProvider, AWHtmlTemplateParser.LiteralBody
{
    private static final String SelfKey = "__AWXServerScript";

    public AWXServerScript ()
    {
        Log.demoshell.debug("--- creating a new AWXServerScript --- ");
    }
    // we cache an evaluated scope for our script (i.e. a class defn for all our instances)

    Scriptable _classScope;

    // If a template contains this, use JavaScript
    public Class componentSubclass (File file, AWTemplate template)
    {
        return AWXSJSHTMLComponent.class;
    }
    
    public static AWXServerScript instanceInComponent (AWComponent component)
    {
        return (AWXServerScript) AWUtil.elementOfClass(component.template(), AWXServerScript.class);
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        // swallow our content
    }

    /**
     * Called by enclosing components to assign their script instance.
     */
    public Scriptable createInstance (AWComponent component)
    {
        Scriptable instance = AWXScriptFactory.sharedInstance().instanceForClassScope(classScope(component.template()), component);
        instance.put(SelfKey, instance, this);  // store this for our freshness check
        return instance;
    }

    /**
     * Checks whether this instance is out of date compared to the template.
     */
    public boolean isStale (Scriptable instance, AWComponent component)
    {
        return _classScope == null; // (instance.get(SelfKey, instance) == this);
    }

    public Scriptable classScope (AWTemplate template)
    {
        // XXX: thread safety?!?
        if (_classScope == null) {
            final FastStringBuffer buf = new FastStringBuffer();
            AWUtil.iterate(template, new AWUtil.ElementIterator() {
                public Object process(AWElement e) {
                    if (e instanceof AWXServerScript) {
                        buf.append(((AWXServerScript)e).scriptString());
                    }
                    return null; // keep looking
                }
            });

            String string = buf.toString();
            // Log.demoshell.debug("*** Script String: %s", string);
            _classScope = AWXScriptFactory.sharedInstance().classScopeForString(string);
        }
        return _classScope;
    }

    public String scriptString ()
    {
        AWElement content = contentElement();
        if (content instanceof AWBareString) {
            return ((AWBareString)content).string();
        }
        throw new AWGenericException("AWXServerScript with content that is not a BareString!");
    }
}

