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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/MessagesList.java#5 $
*/

package ariba.ui.widgets;

import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import java.util.List;
import java.util.Locale;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.util.core.Fmt;

/**
    Displays a list of messages using the style appropriate according to its severity.

    <pre>
    <b>Required bindings:</b><p>
       none
    <p>
    <b>Optional bindings:</b><p>
        message: Either a vector of messages or a single message. Each
        message may be a String or an Object, which will be toString'ed for
        display.  If the message is a String it can either be an
        already-localed String or can be an NLSString of the form
        "@resource/key", in which case we'll translate it.

        isError:        indicates whether the message are "errors". By default true.
        isWarning:      indicates whether the message are "alerts". By default false.
                        If isError is true, this parameter is ignored.
        isConfirmation: indicates whether the message are "confirmations". By default false.
                        If isError or isWarning is true, this parameter is ignored.
                        Example of confirmation message: "Your password has been successfully updated."
    </pre>
    @aribaapi ariba
*/
public class MessagesList extends AWComponent
{
    // Binding names
    protected static final String MessageBinding   = "message";
    protected static final String IsErrorBinding   = "isError";
    protected static final String IsWarningBinding = "isWarning";
    protected static final String IsConfirmBinding = "isConfirmation";

    // constants for the severity of the messages
    private static final int ErrorKind   = 0;
    private static final int AlertKind   = 1;
    private static final int ConfirmKind = 2;

    // members
    protected List/*String*/ m_message;
    private int _messageKind = -1;

    /**
        Overridden to clean up cached values.
    */
    public void sleep ()
    {
        super.sleep();

        m_message = null;
        _messageKind = -1;
    }

    /**
        Returns true if we have some error messages.
    */
    public boolean hasMessage ()
    {
        return !ListUtil.nullOrEmptyList(message());
    }

    /**
        Returns a vector of localized message strings.
    */
    public List/*String*/ message ()
    {
        if (m_message == null) {
            Object message = valueForBinding(MessageBinding);
            Locale locale = preferredLocale();
            if (message == null) {
                m_message = ListUtil.list();
            }
            else if (!(message instanceof List)) {
                m_message = ListUtil.list(convertMessage(message, locale));
            }
            else {
                // have a list, convert all items to localized strings
                List rawMessages = (List)message;
                m_message = ListUtil.list();
                int elen = rawMessages.size();
                for (int ii=0; ii<elen; ii++) {
                    m_message.add(convertMessage(rawMessages.get(ii), locale));
                }
            }
        }

        return m_message;
    }

    /**
        Convert any object into a localized message string
        @param messageObj the message object, must not be null
        @param locale the locale we should translate to
        @return a localized string
        @aribaapi private
    */
    private String convertMessage (Object messageObj, Locale locale)
    {
        if (messageObj instanceof String) {
            String smessage = (String)messageObj;
            if (ResourceService.isNlsKey(smessage)) {
                // convert nls string
                smessage = ResourceService.getService().getLocalizedCompositeKey(
                    smessage, locale);
            }
            return addMessageWrapperTo(smessage);
        }
        else {
            return addMessageWrapperTo(messageObj.toString());
        }
    }

    /**
       Processes the several boolean binding to return the message kind
       they match to.
    */
    private int messageKind ()
    {
        if (_messageKind == -1) {
            boolean value = booleanValueForBinding(IsConfirmBinding, false);
            if (value) {
                _messageKind = ConfirmKind;
            }
            value = booleanValueForBinding(IsWarningBinding, false);
            if (value) {
                _messageKind = AlertKind;
            }
            value = booleanValueForBinding(IsErrorBinding, false);
            if (value || _messageKind == -1) {
                _messageKind = ErrorKind;
            }
        }
        return _messageKind;
    }

    /**
       Returns the style to use to display the message.
    */
    public String style ()
    {
        switch (messageKind()) {
        case ConfirmKind:
          return "message messageConfirm";
        case AlertKind:
          return "message messageAlert";
        case ErrorKind:
        default:
          return "message messageError";
        }
    }

    /**
       Returns the localized string to display.
       This string includes the message kind wrapper ("Error: " for instance)
       @param currentMessage the localized string to prepend with the right message wrapper
    */
    private String addMessageWrapperTo (String message)
    {
        return message;
        /* todo: Move wrapper message to imge tooltip
        String messageWrapperKey;
        switch (messageKind()) {
        case ConfirmKind:
          messageWrapperKey = "ConfirmMessage";
          break;
        case AlertKind:
          messageWrapperKey = "AlertMessage";
          break;
        case ErrorKind:
        default:
          messageWrapperKey = "ErrorMessage";
          break;
        }
        return Fmt.Sil(preferredLocale(),
                       "ariba.htmlui.widgets",
                       messageWrapperKey,
                       message);
        */
    }
}
