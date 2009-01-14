package mailclient

import ariba.util.core.*;
import ariba.ui.aribaweb.core.*
import ariba.ui.aribaweb.util.*
import ariba.ui.table.*
import ariba.ui.widgets.*
import javax.mail.*
import java.util.Properties
import ariba.ui.meta.annotations.*

/*
    NOTE: To connect to gmail you'll need to add their SSL cert to your keystore. Nice instructions
    can be found here: http://agileice.blogspot.com/2008/10/using-groovy-to-connect-to-gmail.html
 */
class MailReader extends AWComponent
{
    LoginParams loginParams = new LoginParams()
    Store store
    List<Folder> folders
    Folder folder, selFolder
    Map folderMessages = [:]
    AWTDisplayGroup displayGroup
    AWMimeParsedMessage message

    public void init ()
    {
        Confirmation.showConfirmation(requestContext(), AWEncodedString.sharedEncodedString("LoginPanel"));
    }

    void loginAction ()
    {
        try {
            Properties props = new Properties();
            props.setProperty("mail.store.protocol", "imaps");
            props.setProperty("mail.imaps.host", loginParams.imapServer);
            props.setProperty("mail.imaps.port", loginParams.imapPort.toString());
            def session = Session.getDefaultInstance(props,null)
            ProgressMonitor.instance().prepare("Connecting to mail server...", 0)
            store = session.getStore("imaps")
            store.connect(loginParams.imapServer, loginParams.userName, loginParams.password)
            folders = store.defaultFolder.list().findAll { !it.name.startsWith("[") }
            selFolder = folders.find { it.name == "INBOX" }
        } catch (AuthenticationFailedException e) {
            recordValidationError("password", "Failed to login: ${e.getMessage()}".toString(), null);
        }
        if (errorManager().checkErrorsAndEnableDisplay()) {
            Confirmation.showConfirmation(requestContext(), AWEncodedString.sharedEncodedString("LoginPanel"));
        }
    }

    List messages () {
        if (!selFolder) return null
        List messages = folderMessages[selFolder]
        if (!messages) {
            ProgressMonitor.instance().prepare("Fetching messages for folder ${selFolder}...".toString(), 0)
            selFolder.open(Folder.READ_ONLY)
            folderMessages[selFolder] = messages = selFolder.messages.collect { new AWMimeParsedMessage(it) }
            displayGroup.setObjectArray(messages)
            if (!messages.isEmpty()) displayGroup.setItemToForceVisible(messages[-1])
        }
        return messages
    }

    def inspectMime () {
        def p = pageWithName("MimeTreePanel")
        p.clientPanel = true;
        p.message = displayGroup.selectedObject().message
        return p 
    }
}

@Traits("oneZone")
class LoginParams {
    String  imapServer = "imap.gmail.com"
    int imapPort = 993
    String userName = "aribaweb"
    @Trait.Secret String password
}
