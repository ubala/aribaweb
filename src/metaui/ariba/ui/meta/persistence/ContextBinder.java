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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/persistence/ContextBinder.java#3 $
*/
package ariba.ui.meta.persistence;

import ariba.ui.aribaweb.core.AWPage;
import ariba.ui.aribaweb.core.AWSession;
import ariba.util.core.StringUtil;

/*
    Binds ObjectContext to current thread via association with page.

    By default a new page gets the context from the previous page (or a new one
    if none)
 */
public class ContextBinder implements AWPage.LifecycleListener
{
    private static final String PageDictKey = "ContextBinder_CTX";

    public static void initialize ()
    {
        AWPage.registerLifecycleListener(new ContextBinder());
    }
    
    protected ObjectContext getPageContext (AWPage page)
    {
        if (page.topPanel() != null) {
            return (ObjectContext)page.topPanel().dict().get(PageDictKey);
        }
        return (ObjectContext)page.get(PageDictKey);
    }

    protected void setPageContext (AWPage page, ObjectContext ctx)
    {
        if (page.topPanel() != null) {
            page.topPanel().dict().put(PageDictKey, ctx);
            return;
        }
        page.put(PageDictKey, ctx);
    }

    public void pageWillAwake (AWPage page)
    {
        ObjectContext ctx = getPageContext(page);
        if (ctx != null) {
            // restore page's context to the thread
            ObjectContext.bind(ctx);
        }
        else {
            // pick up previous context and bind it to this page
            if (ObjectContext.peek() == null || StringUtil.nullOrEmptyString(ObjectContext.get().groupName_debug())) {
                AWSession session = page.requestContext().session(false);
                String id = (session != null) ? session.sessionId() : null;
                ObjectContext.bindNewContext(id);
            }
            ctx = ObjectContext.get();

            setPageContext(page, ctx);
        }
    }

    public void pageWillRender(AWPage page)
    {
    }
    
    public void pageWillSleep (AWPage page)
    {
        ObjectContext.unbind();
    }
}
