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

    $Id: $
*/

package ariba.util.i18n;

import ariba.util.core.StringUtil;
import java.io.UnsupportedEncodingException;
import java.text.Collator;

/**
    Class I18NSupport is the default class for i18n support, it contains the default
    implementation of i18n support. All language specific support should extend this class.

    @aribaapi private
*/

public class I18NSupport
{
    /*-----------------------------------------------------------------------
        Public Methods
      -----------------------------------------------------------------------*/

    /**
       Class constructor.
    */
    public I18NSupport ()
    {
    }

    /**
        Returns the UI encoding string

        @return the encoding string
        @aribaapi private
    */
    public String uiEncoding ()
    {
        return I18NConstants.IANACharset[I18NConstants.UTF8];
    }

    /**
        Returns the file encoding string for the specified locale

        @return the encoding string
        @aribaapi documented
    */
    public String fileEncoding ()
    {
        return I18NConstants.IANACharset[I18NConstants.Cp1252];
    }

    /**
        Returns the MIME header encoding string

        @return the encoding string
        @aribaapi private
    */
    public String mimeHeaderEncoding ()
    {
        return I18NConstants.EncodingQ;
    }

    /**
        Returns the MIME body encoding string

        @return the encoding string
        @aribaapi private
    */
    public String mimeBodyEncoding ()
    {
        return I18NConstants.CharacterEncoding[I18NConstants.Cp1252];
    }

    /**
        Returns the MIME body charset string

        @return the encoding string
        @aribaapi private
    */
    public String mimeBodyCharset ()
    {
        return I18NConstants.IANACharset[I18NConstants.Cp1252];
    }

    /**
        Normalizes characters in the catalog string

        @param string the string to be normalized
        @return the normalized string
        @aribaapi private
    */
    public String normalizeCatalog (String string)
    {
        return string;
    }

    /**
        Normalizes the input date format string

        @param string the string to be normalized
        @return the normalized string
        @aribaapi private
    */
    public String normalizeDate (String string)
    {
        return string;
    }

    /**
        Normalizes the input decimal string

        @param string the string to be normalized
        @return the normalized string
        @aribaapi private
    */
    public String normalizeNumber (String string)
    {
        return string;
    }

    /**
        Normalizes the input money format string

        @param string the string to be normalized
        @return the normalized string
        @aribaapi private
    */
    public String normalizeMoney (String string)
    {
        return string;
    }

    /**
        Normalizes the input read text string

        @param string the string to be normalized
        @return the normalized string
        @aribaapi private
    */
    public String readFileText (String string)
    {
        return string;
    }

    /**
        Normalizes the input write text string

        @param string the string to be normalized
        @return the normalized string
        @aribaapi private
    */
    public String writeFileText (String string, String encoding)
    {
        return string;
    }

    /**
        Normalizes the input mail text string

        @param string the string to be normalized
        @return the normalized string
        @aribaapi private
    */
    public String normalizeMailText (String string)
    {
        return string;
    }

    /**
        Returns the width of the input string using its default encoding

        @param string the string to get the width of
        @return the width of the string
        @aribaapi private
    */
    public int widthInBytes (String string)
    {
        if (StringUtil.nullOrEmptyOrBlankString(string)) {
            return 0;
        }

        try {
            return string.getBytes(uiEncoding()).length;
        }
        catch (UnsupportedEncodingException uee) {
            // log here
            return string.length();
        }
    }

    /**
        Returns the width of the input string

        @param string the string to get the width of
        @return the width of the string
        @aribaapi documented
    */
    public int getStringWidth (String string)
    {
        if (StringUtil.nullOrEmptyOrBlankString(string)) {
            return 0;
        }
        return string.length();
    }


    /**
        Returns the decomposition flag

        @return the decomposition flag
        @aribaapi documented
    */
    public int decompositionFlag ()
    {
        return Collator.CANONICAL_DECOMPOSITION;
    }


    /**
        Check if it is supported by EM

        @return <b>true</b> by default
        @aribaapi private
    */
    public boolean isSupportedInEM ()
    {
        return true;
    }

}
