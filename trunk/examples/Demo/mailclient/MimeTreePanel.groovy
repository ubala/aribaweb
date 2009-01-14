package mailclient

import ariba.ui.aribaweb.core.AWComponent
import javax.mail.internet.MimeMultipart
import javax.mail.internet.MimePart
import javax.mail.internet.MimeMessage

class MimeTreePanel extends AWComponent
{
    MimeMessage message
    MimePart part, selectedPart;

    public boolean isStateless () { false; }

    // MIME structure inspect support
    List messageRoot () { message ? [message] : null }

    String partLabel () {
        String s = part.dataHandler.getContentType()
        int i = s.indexOf(';')
        return (i == -1) ? s : s.substring(0, i)
    }

    List partChildren ()
    {
        if (!part.dataHandler.getContentType().startsWith("multipart/")) return null;
        MimeMultipart mm = part.dataHandler.content
        return (0 .. mm.count-1).collect { mm.getBodyPart(it) }
    }
}
