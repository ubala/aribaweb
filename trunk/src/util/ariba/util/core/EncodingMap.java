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

    $Id: //ariba/platform/util/core/ariba/util/core/EncodingMap.java#7 $
*/

package ariba.util.core;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

/**
    @aribaapi ariba
*/
public class EncodingMap
{
    /*-----------------------------------------------------------------------
        Private Constants
    -----------------------------------------------------------------------*/

    private static final int EncodingTypes = 4;
    private static final int UIEncodingIndex = 0;
    private static final int HTMLEncodingIndex = 1;
    public static final int EmailEncodingIndex = 2;
    private static final int ClientSideEncodingIndex = 3;
    private static final Locale DefaultLocale = new Locale("defaultLocale", "");

    public static final String[] encodingNames = {"UI encoding",
                                                   "HTML encoding",
                                                   "Email encoding",
                                                   "Client-Side encoding"};

    private static final EncodingMap self = new EncodingMap();

    private EncodingMapStrategy strategy = null;

    private final GrowOnlyHashtable cachedEncodings;

    private EncodingMap ()
    {
        cachedEncodings = new GrowOnlyHashtable();
    }

    /*-----------------------------------------------------------------------
        Public Methods
    -----------------------------------------------------------------------*/

    /**
        return the global encoding map
    */

    public static EncodingMap getEncodingMap ()
    {
        return self;
    }

    public void setStrategy (EncodingMapStrategy ems)
    {
        strategy = ems;
    }

    /**
        encoding used in the html ui
    */
    public String getUIEncoding (Locale locale)
    {
        String[] encodings =  getEncodings(locale);
        if (encodings != null) {
            return encodings[UIEncodingIndex];
        }
        return null;
    }

    public String getDefaultUIEncoding ()
    {
        return getUIEncoding(DefaultLocale);
    }

    /**
        encoding used for HTML attachments, printing, etc.
    */
    public String getHTMLEncoding (Locale locale)
    {
        String[] encodings =  getEncodings(locale);
        if (encodings != null) {
            return (String)encodings[HTMLEncodingIndex];
        }
        return null;
    }

    /**
        encoding used for email, both charset tag and
        body
    */

    public String getEmailEncoding (Locale locale)
    {
        if (strategy != null) {
	    return strategy.getEmailEncoding(locale);
        }
        else {
            return getSysEmailEncoding(locale);
        }
    }

    public String getSysEmailEncoding (Locale locale)
    {
        String[] encodings =  getEncodings(locale);
        if (encodings != null) {
            return encodings[EmailEncodingIndex];
        }
        return null;
    }

    /**
        encoding used on the client machine. If for a given
        locale the mapping is empty we return null

        @return the encoding associated with the given locale,
         null if the encoding map is empty
    */
    public String getClientSideEncoding (Locale locale)
    {
        String[] encodings =  getEncodings(locale);
        if (encodings != null) {
            return encodings[ClientSideEncodingIndex];
        }
        return null;
    }


    public void setUIEncoding (Locale locale, String encoding)
    {
        setEncodings(locale, encoding, UIEncodingIndex);
    }

    public void setHTMLEncoding (Locale locale, String encoding)
    {
        setEncodings(locale, encoding, HTMLEncodingIndex);
    }

    public void setEMailEncoding (Locale locale, String encoding)
    {
        if (strategy != null) {
            strategy.setEmailEncoding(locale, encoding);
        }
        else {
            setSysEmailEncoding(locale, encoding);
        }
    }

    public void setSysEmailEncoding (Locale locale, String encoding)
    {
         setEncodings(locale, encoding, EmailEncodingIndex);
    }

    public void setClientSideEncoding (Locale locale, String encoding)
    {
        setEncodings(locale, encoding, ClientSideEncodingIndex);
    }

    /*-----------------------------------------------------------------------
        Private Methods
    -----------------------------------------------------------------------*/

    private String[] getEncodings (Locale locale)
    {
        String[] returnInfo = null;

        if (locale != null) {
            returnInfo = (String[])cachedEncodings.get(locale);
        }
        if (returnInfo == null) {
            // check to see whether a universal
            // encoding has been defined
            returnInfo = (String[])cachedEncodings.get(DefaultLocale);
        }
        return returnInfo;
    }

    private void setEncodings (Locale locale, String encodingName, int index)
    {
        String[] encodings = null;

        encodings =  (String[])cachedEncodings.get(locale);

        if (encodings == null) {
            encodings = new String[EncodingTypes];
            cachedEncodings.put(locale, encodings);
        }

        boolean valid = !StringUtil.nullOrEmptyOrBlankString(encodingName);
        if (valid) {
            try {
                "".getBytes(encodingName);
            }
            catch (UnsupportedEncodingException uee) {
                valid = false;
            }
        }
        Assert.that(valid, "%s for locale %s is set to %s which is invalid.",
                    encodingNames[index], locale.toString(), encodingName);

           //make sure we only set once, no runtime change for detault system
           //encodings, it is needed now as before there is the encoding value
           //never change
        if (StringUtil.nullOrEmptyString(encodings[index]))  {
            encodings[index] = encodingName;
        }
    }


}


