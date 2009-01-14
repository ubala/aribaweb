package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.util.AWContentType;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWMimeParsedMessage;

import javax.mail.internet.MimePart;
import javax.mail.internet.MimeMessage;
import javax.mail.MessagingException;
import javax.activation.DataHandler;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class MimeMessageView extends AWComponent
{
    public String _resourceUrl;
    public MimePart _part;
    public DataHandler _partDataHandler;
    public int _partType;
    public AWMimeParsedMessage _parsedMessage;
    public boolean _showingQuoted;

    public boolean isStateless ()
    {
        return false;
    }

    public void renderResponse (AWRequestContext requestContext, AWComponent component)
    {
        AWMimeParsedMessage parsedMessage = (AWMimeParsedMessage)valueForBinding("parsedMessage");
        if (parsedMessage == null) {
            MimeMessage message = (MimeMessage)valueForBinding("mimeMessage");
            if (message != null) {
                parsedMessage = (_parsedMessage != null && _parsedMessage.getMessage() == message)
                    ? _parsedMessage : new AWMimeParsedMessage(message);
            }
        }
        if (_parsedMessage != parsedMessage) {
            _parsedMessage = parsedMessage;
            _showingQuoted = !booleanValueForBinding("collapseQuoted", false);
        }
        super.renderResponse(requestContext, component);
    }

    public List parts ()
    {
        if (_parsedMessage == null) return null;
        return (_showingQuoted) ? _parsedMessage.getTopLevelParts() : _parsedMessage.getNonQuotedParts();
    }

    public String replacementUrl ()
    {
        return _resourceUrl.startsWith("cid:") ? null : _resourceUrl;
    }

    public void setPart (MimePart part)
    {
        _part = part;
        try {
            _partDataHandler = part.getDataHandler();
            String type = _partDataHandler.getContentType().toUpperCase();
            _partType = (type.startsWith("TEXT/PLAIN")) ? 0
                    : (type.startsWith("TEXT/HTML") ? 1 : 2);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public byte[] partBytes ()
    {
        return dataHandlerBytes(_partDataHandler);
    }

    static byte[] dataHandlerBytes (DataHandler dh)
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            dh.writeTo(os);
        } catch (IOException e) {
            throw new AWGenericException(e);
        }
        return os.toByteArray();
    }

    public AWResponseGenerating downloadPart (MimePart part, boolean asFile)
    {
        AWResponse response = application().createResponse();

        try {
            DataHandler dh = part.getDataHandler();
            AWContentType contentType = AWContentType.contentTypeNamed(dh.getContentType());
            String filename = dh.getName();
            if (filename != null && asFile) {
                Util.setHeadersForDownload (requestContext(), response, filename);
            }
            response.setContentType(contentType);
            response.setContent(dataHandlerBytes(dh));
        } catch (MessagingException e) {
            throw new AWGenericException(e);
        }
        return response;
    }

    public AWResponseGenerating downloadPart ()
    {
        return downloadPart(_part, true);
    }

    public AWResponseGenerating resourceResponse ()
    {
        String contentId = _resourceUrl.substring(4);
        MimePart part = _parsedMessage.partForId(contentId);
        return downloadPart(part, false);
    }
}
