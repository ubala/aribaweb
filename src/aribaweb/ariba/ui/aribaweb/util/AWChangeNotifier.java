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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWChangeNotifier.java#3 $
*/

package ariba.ui.aribaweb.util;

import ariba.ui.aribaweb.core.AWPage;

public class AWChangeNotifier
{
    private AWPage _page = null;

    public AWChangeNotifier (AWPage page)
    {
        _page = page;
    }

    /**
     * Use to notify the framework that data on the page associated with this
     * AWChangeNotifier has been modified.
     *
     * @return boolean indicating whether or not this AWChangeNotifier is active.  If
     *         method returns false, the application code is expected to release the
     *         reference to this AWChangeNotifier and allow it to be garbage collected.
     * @aribaapi private
     */
    public boolean notifyChange ()
    {
        // Note we're working with a local copy of the page here to avoid requiring
        // synchronization on the notifyChange.  We're willing to have a bit of sloppiness
        // in the deactivation -- ie, a page may receive an extra notification even though
        // it has already deactivated its AWChangeNotifier to avoid the synchronization.
        AWPage page = _page;
        boolean isActive = false;
        if (page != null) {
            isActive = page.notifyChange();
        }

        return isActive;
    }

    /**
     * Note should only be called by the AWPage that owns this AWChangeNotifier.
     * 
     * @aribaapi private
     */
    public void deactivate ()
    {
        _page = null;
    }

    /**
     *
     * @return boolean indicating whether or not this AWChangeNotifier is active
     * @aribaapi private
     */
    public boolean isActive ()
    {
        return _page != null;
    }
}
