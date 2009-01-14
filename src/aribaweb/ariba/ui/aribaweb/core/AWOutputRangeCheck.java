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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWOutputRangeCheck.java#4 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWPagedVector;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWCharacterEncoding;

import java.util.Map;

/*
    Wacky container used to perform a post-append scan of all data emitted to a AWBaseResponse in the content
    of the tag.
    Used in AWTColumn to render an &nbsp; if its body ends up evaluating to nothing (or nothing but empty
    tags).

    Will set its contentIterator binding with the AWPagedVectorIterator for its content.
    (or nothing if response is not an AWBaseResponse with AWResponseBuffer).
 */
public class AWOutputRangeCheck extends AWContainerElement
{

    public AWBinding _contentIterator;

/*
    private static final String[] SupportedBindingNames = {"contentIterator"};
    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }
*/
    
    public void init (String tagName, Map bindingsHashtable)
    {
        _contentIterator = (AWBinding)bindingsHashtable.remove("contentIterator");
        super.init(tagName, bindingsHashtable);
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component) {
        int start = -1;
        AWPagedVector vector = null;
        if (requestContext.response() instanceof AWBaseResponse) {
            vector = ((AWBaseResponse)requestContext.response()).globalContents();
            if (vector != null) start = vector.size();
        }

        super.renderResponse(requestContext, component);

        if (start != -1 && _contentIterator != null) {
            AWPagedVector.AWPagedVectorIterator iter = vector.elements(start, vector.size());
            _contentIterator.setValue(iter, component);
            iter.release();
        }
    }

    private static final String imageTag = "img";
    private static final String inputTag = "input";

    public static boolean hasVisbibleContent (AWPagedVector.AWPagedVectorIterator elements)
    {
        int nesting = 0;
        while (elements.hasNext()) {
            Object element = elements.next();
            if (element instanceof AWEncodedString) {
                AWEncodedString encodedString = (AWEncodedString)element;
                String currString = encodedString.string();
                if (currString.startsWith(imageTag) ||
                    currString.startsWith(inputTag)) {
                    return true;
                }
                byte[] bytes = encodedString.bytes(AWCharacterEncoding.UTF8);
                for (int i=0, count=bytes.length; i < count; i++) {
                    char c = (char)bytes[i];
                    if (c == '<') {
                        nesting++;
                    } else if (c == '>') {
                        nesting--;
                    } else if (nesting == 0 && !Character.isWhitespace(c)) {
                        // not empty!
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
