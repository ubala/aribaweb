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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/HTMLActionFilter.java#12 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRedirect;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWGenericActionTag;
import ariba.ui.aribaweb.util.AWResource;
import ariba.util.core.Assert;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HTMLActionFilter extends AWComponent
{
    private static final Pattern EmbeddedActionPattern
            = java.util.regex.Pattern.compile("(?:(?:HREF|href)=\"([^#][^\"]+)\")(?: *target=\"([^\"]+)\")?|(?:(?:SRC|src)=\"([^\"]+)\")");
    protected Matcher _matcher;
    protected String _input;
    public String _staticUrl;
    protected int _readPos;

    public interface UrlFilter
    {
        // Should return null if no replacement made
        String replacementForUrl (String url);
    }
        
    public Object nextAction ()
    {
        if (_matcher == null) {
            _input = stringValueForBinding(BindingNames.value);
            if (_input == null) return null;
            _matcher = EmbeddedActionPattern.matcher(_input);
            _readPos = 0;
        }
        return (_matcher.find()) ? Boolean.TRUE : null;
    }

    public String currentPrefix ()
    {
        int oldPos = _readPos;
        _readPos = _matcher.end();
        return _input.substring(oldPos, _matcher.start());
    }

    public boolean matchIsHref ()
    {
        return _matcher.group(1) != null;
    }

    public String currentTarget ()
    {
        String url = _matcher.group(1);
        setValueForBinding(url, "actionUrl");
        String target = stringValueForBinding("actionTarget");
        return target != null ? target : _matcher.group(2);
    }

    public String remainderString ()
    {
        return _input.substring(_readPos);
    }

    public boolean useStaticUrl ()
    {
        String orig = _matcher.group(3);
        setValueForBinding(orig, "resourceUrl");

        _staticUrl = stringValueForBinding("replacementUrl");
        if (_staticUrl == null && !hasBinding("resourceResponse")) {
            if (orig.startsWith("/")) {
                AWResource resource = resourceManager().resourceNamed(orig);
                Assert.that(resource != null, "Can't find resource referenced in markdown: %s", orig);
                _staticUrl = resource.url();
            } else {
                _staticUrl = orig;
            }
        }

        return _staticUrl != null;
    }

    public AWResponseGenerating currentClicked ()
    {
        String url = _matcher.group(1);
        setValueForBinding(url, "actionUrl");
        String target = currentTarget();
        AWResponseGenerating response = (AWResponseGenerating)valueForBinding(AWBindingNames.action);
        if (response == null) {
            response = HTMLActions.handleAction(url, target, this);
        }
        return response;
    }

    public String staticUrlForActionResults ()
    {
        return requestContext().staticUrlForActionResults(currentClicked());
    }

    public AWResponseGenerating resourceRequest ()
    {
        String orig = _matcher.group(3);
        setValueForBinding(orig, "resourceUrl");
        return (AWResponseGenerating)valueForBinding("resourceResponse");
    }

    protected void sleep ()
    {
        super.sleep();
        _matcher = null;
        _input = null;
    }
}