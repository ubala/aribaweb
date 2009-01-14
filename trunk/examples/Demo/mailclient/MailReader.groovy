package mailclient

import ariba.util.core.*;
import ariba.ui.aribaweb.core.*
import ariba.ui.aribaweb.util.*
import ariba.ui.table.*
import ariba.ui.widgets.*
import javax.mail.*
import java.util.Properties
import javax.mail.internet.MimeMultipart
import ariba.ui.meta.annotations.Trait
import javax.activation.DataHandler
import ariba.ui.meta.annotations.Traits

/*
    NOTE: To connect to gmail you'll need to add their SSL cert to your keystore. Nice instructions
    can be found here: http://agileice.blogspot.com/2008/10/using-groovy-to-connect-to-gmail.html
 */
class MailReader extends AWComponent
{
    LoginParams loginParams = new LoginParams()
    def store
    List folderNames
    Map folderMessages = [:]
    AWTDisplayGroup displayGroup
    def folderName, selFolder
    Msg message

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
            folderNames = store.defaultFolder.list().findAll { !it.name.startsWith("[") }
            selFolder = folderNames.find { it.name == "INBOX" }
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
            def folder = store.getFolder(selFolder.name)
            folder.open(Folder.READ_ONLY)
            folderMessages[selFolder] = messages = folder.messages.collect { new Msg(message:it) }
            displayGroup.setObjectArray(messages)
            if (!messages.isEmpty()) displayGroup.setItemToForceVisible(messages[-1])
            // prefetch(displayGroup.displayedObjects())
        }
        return messages
    }

    void prefetch (List messages) {
        Thread.start {
            // messages.reverseEach { it.ensureHeader() }
            messages.reverseEach { println "Prefetching ${it.sentDate} ${it.subject}"; it.ensureBody() }
        }
    }
}

class Msg {
    Message message
    boolean didFetchHeader, didFetchBody
    Date sentDate
    String from, subject
    String plainText, html

    String getFrom ()       { ensureHeader(); from }
    String getSubject ()    { ensureHeader(); subject }
    Date getSentDate ()     { ensureHeader(); sentDate }
    String getPlainText ()  { ensureBody(); plainText }
    String getHtml ()       { ensureBody(); html }
    
    synchronized void ensureHeader () {
        if (didFetchHeader) return
        didFetchHeader = true;
        try {
            from = message.from[0].toString()
            subject = message.subject
            sentDate = new Date(message.sentDate.getTime())
        } catch (Exception e) {
            subject = "Error fetching message: ${e.message}".toString()
        }
    }

    synchronized void ensureBody () {
        if (didFetchBody) return
        didFetchBody = true;
        processDataHandler(message.getDataHandler())
        // println "Fetched body for: ${getSentDate()} ${getSubject()}"
    }

    void processDataHandler (DataHandler dh) {
        String type = dh.getContentType()
        if (type.startsWith("TEXT/PLAIN") && !plainText) {
            plainText = (String)dh.getContent()
        }
        else if (type.startsWith("TEXT/HTML") && !html) {
            html = (String)dh.getContent()
        }
        else if (type.startsWith("multipart/")) {
            MimeMultipart mm = dh.getContent()
            int i=0, c = mm.count
            for ( ; i < c; i++) {
                processDataHandler(mm.getBodyPart(i).getDataHandler())
            }
        }
    }
}
@Traits("oneZone")
class LoginParams {
    String  imapServer = "imap.gmail.com"
    int imapPort = 993
    String userName = "aribaweb"
    @Trait.Secret String password
}
