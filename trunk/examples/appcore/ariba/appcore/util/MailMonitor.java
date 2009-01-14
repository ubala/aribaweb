package ariba.appcore.util;

import ariba.ui.meta.persistence.ObjectContext;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWMimeParsedMessage;
import ariba.util.core.Assert;
import ariba.util.core.ProgressMonitor;
import ariba.util.core.Fmt;
import ariba.appcore.Global;
import ariba.appcore.User;

import com.sun.mail.imap.IMAPFolder;

import javax.mail.Store;
import javax.mail.Folder;
import javax.mail.Session;
import javax.mail.MessagingException;
import javax.mail.AuthenticationFailedException;
import javax.mail.Message;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class MailMonitor
{
    public interface MessageHandler
    {
        void process (MimeMessage message);
    }

    Store _store;
    Folder _inbox;
    Properties _loginParams;

    static final String ProtocolProperty = "mail.store.protocol";
    static final int NewMessagePollIntervalMsec = 10*1000;
    static Session session;

    static void initialize (Properties loginParams)
    {
        if (session == null) {
            session = Session.getDefaultInstance(loginParams, null);
        }
    }

    static Session sharedSession ()
    {
        Assert.that(session != null, "sharedSession called before initialize");
        return session;
    }

    public static MimeMessage mimeForData (byte[] mimeData)
    {
        ByteArrayInputStream is = new ByteArrayInputStream(mimeData);
        try {
            return new MimeMessage(sharedSession(), is);
        } catch (MessagingException e) {
            throw new AWGenericException(e);
        }
    }

    public static byte[] bytesForMime (MimeMessage message)
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            message.writeTo(os);
        } catch (IOException e) {
            throw new AWGenericException(e);
        } catch (MessagingException e) {
            throw new AWGenericException(e);
        }
        return os.toByteArray();        
    }

    public MailMonitor (Properties loginParams)
    {
        _loginParams = loginParams;
    }

    public static Store connect (Properties loginParams) throws AuthenticationFailedException
    {
        initialize(loginParams);
        ProgressMonitor.instance().prepare("Connecting to mail server...", 0);
        String protocol = (String)loginParams.get(ProtocolProperty);
        Store store = null;
        try {
            store = session.getStore(protocol);
            String host = (String)loginParams.get(Fmt.S("mail.%s.host", protocol));
            String userName = (String)loginParams.get("mail.username");
            String password = (String)loginParams.get("mail.password");
            store.connect(host, userName, password);
        } catch (MessagingException e) {
           throw new AWGenericException(e);
        }
        return store;
    }

    void connect ()
    {
        try {
            _store = connect(_loginParams);
            _inbox = _store.getFolder("INBOX");
            ProgressMonitor.instance().prepare("Fetching messages for folder ${inbox.name}...".toString(), 0);
            _inbox.open(Folder.READ_ONLY);
        } catch (AuthenticationFailedException e) {
            throw new AWGenericException("Failed to login: ${e.getMessage()}".toString(), e);
        } catch (MessagingException e) {
            throw new AWGenericException("Failed to login: ${e.getMessage()}".toString(), e);
        }
    }

    static final String _LastProcessedKey = MailMonitor.class.getName();

    static String getMessageID (Message message)
    {
        try {
            return ((MimeMessage)message).getMessageID();
        } catch (MessagingException e) {
            throw new AWGenericException(e);
        }
    }

    // Todo:  add support for getNewMessagesSince...
    public void process (int atMost, MessageHandler handler)
    {
        ObjectContext.bindNewContext();
        connect();

        Message[] messages;
        try {
            messages = _inbox.getMessages();
        } catch (MessagingException e) {
            throw new AWGenericException(e);
        }
        int count = messages.length;
        int start = Math.max(0, count-atMost);

        // we persist the ID of the last message that we processed
        Global proceededRecord = Global.findOrCreate(_LastProcessedKey);
        String lastId = proceededRecord.getStringValue();
        if (lastId != null) {
            // backup until we hit our id (or the start
            for (int i=count-1; i >=start; i--) {
                if (getMessageID(messages[i]).equals(lastId)) {
                    start = i + 1;
                    break;
                }
            }
        }

        // process backlog
        for (int i=start; i < count; i++) {
            MimeMessage message = (MimeMessage)messages[i];
            proceededRecord.setStringValue(getMessageID(message));
            ObjectContext.get().save();
            handler.process(message);
        }

	    processIncomingMessages(_inbox, handler, proceededRecord);
    }

    void processIncomingMessages (Folder folder, final MessageHandler handler, final Global proceededRecord)
    {
        // event handler for new ones
        MessageCountAdapter listener = new MessageCountAdapter() {
            public void messagesAdded (MessageCountEvent ev)
            {
                Message[] messages = ev.getMessages();

                System.out.printf("New messages: %s\n", messages.length);
                ObjectContext.bindNewContext();
                for (Message m : messages) {
                    MimeMessage message = (MimeMessage)m;
                    proceededRecord.setStringValue(getMessageID(message));
                    ObjectContext.get().save();

                    handler.process(message);
                }
            }
        };

        folder.addMessageCountListener(listener);

        try {
            while (true) {
                if (folder instanceof IMAPFolder) {
                    System.out.println("Using IMAP IDLE...");
                    try {
                        ((IMAPFolder)folder).idle();
                    } catch (Exception e) {
                        // Continue / reconnect
                    } finally {
                        folder.removeMessageCountListener(listener);
                        if (!folder.isOpen()) folder.open(Folder.READ_ONLY);
                        folder.addMessageCountListener(listener);
                    }
                } else {
                        int count = _inbox.getMessageCount();
                        System.out.printf("Checked Inbox... count = %s", count);
                        Thread.currentThread().sleep(NewMessagePollIntervalMsec);
                }
            }
        } catch (Exception e) {
            throw new AWGenericException(e);
        }
    }


    private static final Pattern _FromPattern = Pattern.compile(
            "(?m)^From: (?:\"(.+?)\")?\\s?<(.+?)>$");

    public static User findOrCreateSender (AWMimeParsedMessage parsedMessage)
    {
        User sender = null;
        if (parsedMessage.getIsPureForward()) {
            String text = parsedMessage.plainTextForPart(parsedMessage.getTopLevelParts().get(0));
            Matcher m = _FromPattern.matcher(text);
            if (m.find()) {
                sender = User.findOrCreate(m.group(2), m.group(1));
            }
        }

        if (sender == null) {
            MimeMessage message = parsedMessage.getMessage();
            InternetAddress from = null;
            try {
                from = (InternetAddress)message.getFrom()[0];
            } catch (MessagingException e) {
                throw new AWGenericException(e);
            }
            sender = User.findOrCreate(from.getAddress(), from.getPersonal());
        }
        return sender;
    }
}
