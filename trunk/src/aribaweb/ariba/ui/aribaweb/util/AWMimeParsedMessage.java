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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWMimeParsedMessage.java#1 $
*/
package ariba.ui.aribaweb.util;

import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.HTML;
import ariba.util.core.StringUtil;
import ariba.util.core.Date;

import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.MessagingException;
import javax.mail.BodyPart;
import javax.activation.DataHandler;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.IOException;

public class AWMimeParsedMessage
{
    MimeMessage _message;
    boolean _didFetchHeader;
    Date _sentDate;
    String _from;
    String _subject;
    String _plainText;
    String _html;
    Map<String, MimePart> _parts;
    List<MimePart> _topLevelParts;
    List<MimePart> _attachmentParts;
    List<MimePart> _nonQuotedParts;
    boolean _isPureForward;

    public AWMimeParsedMessage (MimeMessage message)
    {
        _message = message;
    }

    public MimeMessage getMessage ()
    {
        return _message;
    }

    public String getFrom ()
    {
        ensureHeader();
        return _from;
    }

    public String getSubject ()
    {
        ensureHeader();
        return _subject;
    }

    public Date getSentDate ()
    {
        ensureHeader();
        return _sentDate;
    }

    public List<MimePart> getTopLevelParts ()
    {
        ensureBody();
        return _topLevelParts;
    }

    public List<MimePart> getAttachmentParts ()
    {
        ensureBody();
        return _attachmentParts;
    }

    public String getPlainText ()
    {
        if (_plainText == null) {
            ensureBody();
            StringBuilder buf = new StringBuilder();
            for (MimePart p : _topLevelParts) { buf.append(plainTextForPart(p)); }
            _plainText = buf.toString();
        }
        return _plainText;
    }

    public String getHtml () {
        if (_html == null) {
            ensureBody();
            StringBuilder buf = new StringBuilder();
            for (MimePart p : _topLevelParts) { buf.append(htmlForPart(p)); }
            _html = buf.toString();
        }
        return _html;
    }

    public synchronized void ensureHeader ()
    {
        if (_didFetchHeader) return;
        _didFetchHeader = true;
        try {
            _from = _message.getFrom()[0].toString();
            _subject = _message.getSubject();
            _sentDate = new Date(_message.getSentDate().getTime());
        } catch (Exception e) {
            _subject = "Error fetching message: " + e.getMessage();
        }
    }

    public synchronized void ensureBody ()
    {
        if (_topLevelParts != null) return;
        _topLevelParts = ListUtil.list();
        _attachmentParts = ListUtil.list();
        processPart(_message, true, MapUtil.map());
    }

    public MimePart partForId (String contentId)
    {
        return (_parts != null) ? _parts.get(contentId) : null;
    }

    boolean isInline (MimePart part) throws MessagingException
    {
        String disposition = part.getDisposition();
        if (disposition == null) {
            String type = part.getDataHandler().getContentType().toUpperCase();
            disposition = (type.startsWith("TEXT/")) ? "INLINE" : "ATTACHMENT";
        }
        return disposition.toUpperCase().startsWith("INLINE");
    }

    static final String[] _PartRankList = { "MULTIPART/", "TEXT/HTML", "TEXT/PLAIN" };

    void processPart (MimePart part, boolean topLevel, Map processed)
    {
        if (part == null) return;
        if (processed.get(part) != null) return;
        processed.put(part, true);

        try {
            String type = part.getContentType().toUpperCase();
            DataHandler dh = part.getDataHandler();

            if (type.startsWith("MULTIPART/")) {
                MimeMultipart mm = (MimeMultipart)dh.getContent();
                if (type.startsWith("MULTIPART/ALTERNATIVE")) {
                    processPart((MimePart)findBest(mm, _PartRankList), true, processed);
                    return;
                }
                else if (type.startsWith("MULTIPART/RELATED")) {
                    processPart((MimePart)findBest(mm, _PartRankList), true, processed);
                    // process rest as not top level
                    topLevel = false;
                }
                int i=0, c = mm.getCount();
                for ( ; i < c; i++) {
                    processPart((MimePart)mm.getBodyPart(i), topLevel, processed);
                }
            } else {
                String cid = part.getContentID();
                if (cid != null) {
                    if (_parts == null) _parts = MapUtil.map();
                    _parts.put(cid.replaceAll("^<(.+)>$", "$1"), part);
                }
                if (topLevel && isInline(part)) {
                    // if (type.startsWith("TEXT/HTML")) html += dh.getContent()
                    // else if (type.startsWith("TEXT/PLAIN")) plainText += dh.getContent()
                    _topLevelParts.add(part);
                } else {
                    _attachmentParts.add(part);
                }
            }
        } catch (MessagingException e) {
            throw new AWGenericException(e);
        } catch (IOException e) {
            throw new AWGenericException(e);
        }
    }

    BodyPart findBest (MimeMultipart mm, String[] types) throws MessagingException
    {
        BodyPart best = null;
        int rank = -1;

        int i=0, c = mm.getCount();
        for (; i < c; i++) {
            BodyPart part = mm.getBodyPart(i);
            String type = part.getContentType().toUpperCase();
            for (int r=0; r < types.length; r++) {
                if (type.startsWith(types[r]) && (best == null || r < rank)) {
                    best = part;
                    rank = r;
                }
            }
        }
        return best;
    }

    public boolean hasQuoteCut ()
    {
        return getNonQuotedParts() != _topLevelParts;
    }

    public String plainTextForPart (MimePart part)
    {
        try {
            DataHandler dh = part.getDataHandler();
            String type = dh.getContentType().toUpperCase();
            boolean isHtml = type.startsWith("TEXT/HTML");
            if (!isHtml && !type.startsWith("TEXT/PLAIN")) return null;
            String contents = (String)dh.getContent();
            return isHtml ? HTML.fullyConvertToPlainText(contents) : contents;
        } catch (MessagingException e) {
            throw new AWGenericException(e);
        } catch (IOException e) {
            throw new AWGenericException(e);
        }
    }

    public String htmlForPart (MimePart part)
    {
        try {
            DataHandler dh = part.getDataHandler();
            String type = dh.getContentType().toUpperCase();
            boolean isHtml = type.startsWith("TEXT/HTML");
            if (!isHtml && !type.startsWith("TEXT/PLAIN")) return null;
            String contents = (String)dh.getContent();
            return isHtml ? contents : HTML.fullyEscape(contents);
        } catch (MessagingException e) {
            throw new AWGenericException(e);
        } catch (IOException e) {
            throw new AWGenericException(e);
        }
    }

    public List<MimePart> getNonQuotedParts ()
    {
        if (_nonQuotedParts == null) {
            ensureBody();
            List newParts = ListUtil.list();
            for (MimePart orig : _topLevelParts) {
                MimePart newPart = checkReplyCut(orig);
                if (newPart != orig) {
                    // if our first part is blank, pre-cut, then don't cut
                    if (newParts.size() == 0 && StringUtil.nullOrEmptyOrBlankString(plainTextForPart(newPart))) {
                        newPart = orig;
                        _isPureForward = true;
                    } else {
                        newParts.add(newPart);
                        _nonQuotedParts = newParts;
                        break;
                    }
                }
                newParts.add(newPart);
            }
            if (_nonQuotedParts == null) _nonQuotedParts = _topLevelParts;
        }
        return _nonQuotedParts;
    }

    public boolean getIsPureForward ()
    {
        getNonQuotedParts();
        return _isPureForward;
    }

    // ToDo: other cut patterns
    static List<String> QuotePatterns = Arrays.asList(
            "On ((Sun|Mon|Tue|Wed|Thu|Fri|Sat), )?(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) \\d{1,2}, \\d{4},? at \\d{1,2}:\\d\\d(:\\d\\d)?\\s?(AM|PM)( -?\\d{4})?, [\\w ]+ wrote:",
            "-----Original",
            "----- Begin",
            "Begin forwarded message:",
            "----- Forwarded message",
            "-- Note added",
            "--\\s*$",
            "In message <"
        );

    static Pattern TextQuoteCutPattern = Pattern.compile(
            "(?m)" + StringUtil.fastJoin(AWUtil.collect(QuotePatterns, new AWUtil.ValueMapper() {
                public Object valueForObject (Object object) { return "(?:^" + object + ")"; }
            }), "|"));

    static Pattern HtmlQuoteCutPattern = Pattern.compile(
            StringUtil.fastJoin(AWUtil.collect(QuotePatterns, new AWUtil.ValueMapper() {
                public Object valueForObject (Object object) { return "(?:>" + object + ")"; }
            }), "|"));

    MimePart checkReplyCut (MimePart part)
    {
        try {
            String type = part.getDataHandler().getContentType().toUpperCase();
            boolean isHtml = type.startsWith("TEXT/HTML");
            if (!isHtml && !type.startsWith("TEXT/PLAIN")) return part;

            String contents = (String)part.getDataHandler().getContent();
            Matcher m = (isHtml ? HtmlQuoteCutPattern : TextQuoteCutPattern).matcher(contents);
            if (!m.find()) return part;

            String newContent = contents.substring(0, m.start() + (isHtml ? 1 : 0));
            // HACK: really need to parse HTML DOM to gracefully close off elements...
            MimeBodyPart newPart = new MimeBodyPart();
            if (isHtml) {
                // HACK: really need to parse HTML DOM to gracefully close off elements...
                newContent += "</div>";
                newPart.setContent(newContent, "text/html");

            } else {
                newPart.setText(newContent);
            }
            return newPart;
        } catch (MessagingException e) {
            throw new AWGenericException(e);
        } catch (IOException e) {
            throw new AWGenericException(e);
        }
    }
}
