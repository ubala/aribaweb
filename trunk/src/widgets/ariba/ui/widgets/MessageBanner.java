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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/MessageBanner.java#1 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWSession;

public class MessageBanner extends AWComponent
{
    public static final String MessageKey = "AribaPageContentMessage";
    public static final String StickyMessageKey = "AribaPageContentStickyMessage";
    
    private Object _oneTimeMessage = null;
    private AWComponent _stickyMessage = null;

    public void sleep ()
    {
        super.sleep();
        _oneTimeMessage = null;
        _stickyMessage = null;
    }

    public void updateMessage ()
    {
        AWSession session = requestContext().session(false);
        if (session != null) {
            _oneTimeMessage = session.dict().get(MessageKey);
            _stickyMessage = (AWComponent)session.dict().get(StickyMessageKey);
            session.dict().remove(MessageKey);
        }
    }

    public Object getMessage ()
    {
        if (_oneTimeMessage != null) {
            return _oneTimeMessage;
        }
        return _stickyMessage;
    }

    public boolean isTextMessage ()
    {
        return (getMessage() instanceof String);
    }

    public static void setMessage (Object message, AWSession session)
    {
        setMessage(message, false, session);
    }

    public static void setMessage (Object message, boolean sticky, AWSession session)
    {
        if (sticky) {
            session.dict().put(StickyMessageKey, message);
        }
        else {
            session.dict().put(MessageKey, message);
        }
    }

    public static void clearMessage (AWSession session)
    {
        clearMessage(false, session);
    }

    public static void clearMessage (boolean sticky, AWSession session)
    {
        if (sticky) {
            session.dict().remove(StickyMessageKey);
        }
        else {
            session.dict().remove(MessageKey);
        }
    }

}
