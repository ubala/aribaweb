/*
    Issue - model class
*/
package model

import ariba.appcore.*
import ariba.appcore.util.*
import ariba.appcore.annotations.*
import javax.persistence.*
import javax.mail.internet.*
import ariba.ui.meta.annotations.Property.Visible
import org.compass.annotations.*
import ariba.ui.aribaweb.util.*
import java.util.regex.Matcher

@Entity @Searchable(root=false)
@DefaultAccess @AnonymousAccess([Permission.ClassOperation.view])
class Note
{
    @Id @GeneratedValue
    private Long id

    @ManyToOne User sender
    String subject
    Date date
    byte[] mimeData

    @SearchableProperty @Visible("false")
    String indexText

    @Transient
    protected AWMimeParsedMessage _parsedMessage

    void init (MimeMessage message)
    {
        subject = message.subject
        date = new Date(message.getSentDate().getTime())
        mimeData = MailMonitor.bytesForMime(message)
        AWMimeParsedMessage parsedMessage = getParsedMessage()
        indexText = parsedMessage.getPlainText()
        sender = MailMonitor.findOrCreateSender(parsedMessage);
    }

    AWMimeParsedMessage getParsedMessage ()
    {
        if (!_parsedMessage) _parsedMessage = new AWMimeParsedMessage(MailMonitor.mimeForData(mimeData))
        return _parsedMessage
    }
}
