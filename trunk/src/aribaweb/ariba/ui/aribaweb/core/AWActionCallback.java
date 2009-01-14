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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWActionCallback.java#2 $
*/
package ariba.ui.aribaweb.core;

/*
    Used for pages with callback actions.  E.g.:

    public AWComponent someAction ()
    {
        SomeModalPage page = (SomeModalPage)pageWithName(SomeModalPage.class.getName());
        page.init(someArgs,
                new AWActionCallback(this) {
                    public AWResponseGenerating doneAction (AWComponent sender) {
                        data = ((SomeModalPage)sender).getSelectedItem();
                        // do something with selected item and return self...
                        return pageComponent();
                    }
                });
        return page;
    }

    The Called Page does this:

    AWActionCallback _callback;
    
    public AWResponseGenerating itemClicked ()
    {
        // make sure our callback page is awake to process callback
        _callback.prepare(requestContext());
        return _callback.doneAction(this);
    }

 */
public class AWActionCallback
{
    AWPage _page;
    AWComponent _component;

    public AWActionCallback (AWComponent returnComponent)
    {
        _page = returnComponent.page();
        _component = returnComponent;
    }

    // action should call this first
    public void prepare (AWRequestContext requestContext)
    {
        _page.ensureAwake(requestContext);
        _component.ensureAwake(_page);
    }

    // default action
    public AWResponseGenerating doneAction (AWComponent sender)
    {
        return _component;
    }
}
