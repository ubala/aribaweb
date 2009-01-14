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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWBaseElement.java#15 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.FastStringBuffer;

import java.util.Map;

abstract public class AWBaseElement extends AWBaseObject implements AWElement
{
    private String _templateName;
    private int _lineNumber;

    // ** Thread Safety Considerations: Although elements are shared between threads, since there are no ivars, there are no locking issues..

    public AWElement determineInstance (String elementName, Map bindingsHashtable, String templateName, int lineNumber)
    {
        throw new AWGenericException(getClass().getName() + ": determineInstance(String, Map, String, int) not implemented.");
    }

    public AWElement determineInstance (String tagName, String translatedClassName, Map bindingsHashtable, String templateName, int lineNumber)
    {
        return determineInstance(tagName, bindingsHashtable, templateName, lineNumber);
    }

    public void setTemplateName (String name)
    {
        _templateName = name;
    }

    public String templateName ()
    {
        return _templateName;
    }

    public void setLineNumber (int line)
    {
        _lineNumber = line;
    }

    public int lineNumber ()
    {
        return _lineNumber;
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        return null;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
    }

    public String toString ()
    {
        return super.toString() + "(" + _templateName + ":" + _lineNumber + ")";
    }

    /**
     * This default implementation is not intended to be executed -- just here for backward compatibility
     * to avoid forcing old code to update to new api's
     * (which was required when no default implementation was provided).
     * @param buffer
     */
    public void appendTo (StringBuffer buffer)
    {
        buffer.append(toString());
    }

    /**
        Checks for things like valid binding names and definitions.  Logs all errors and warnings.
        This is the default, which is to do nothing.
    */
    public void validate (AWValidationContext validationContext, AWComponent component)
    {
    }

    public Object clone()
    {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException ex) {
            return null;
        }
    }

    ////////////////////////
    // Visitable Interface
    ///////////////////////
    public void startVisit (AWVisitor visitor)
    {
        visitor.performAction(this);
    }

    public void continueVisit (AWVisitor object)
    {
        // default is to nop
    }

    public String bareStringContent () {
        final FastStringBuffer buf = new FastStringBuffer();

        AWUtil.iterate(this, new AWUtil.ElementIterator() {
            public Object process(AWElement e) {
               if (e instanceof AWBareString ) {
                    buf.append(((AWBareString)e).string());
                }
                return null; // keep looking
            }
        });
        return buf.toString();
    }
}
