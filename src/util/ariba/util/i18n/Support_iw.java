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

    $Id: //ariba/platform/util/core/ariba/util/i18n/Support_iw.java#5 $
*/

package ariba.util.i18n;


/**
    default i18n for Hebrew
    
    @aribaapi private
*/

public class Support_iw extends I18NSupport
{
    /*-----------------------------------------------------------------------
        Public Methods
      -----------------------------------------------------------------------*/
    
    /**
       Class constructor.
    */    
    public Support_iw ()
    {
    }
    
    /**
        Returns the file encoding string for the specified locale

        @return the encoding string
        @aribaapi documented    
    */    
    public String fileEncoding ()
    {
        return I18NConstants.IANACharset[I18NConstants.ISO8859_8];
    }
               
    /**
        Returns the MIME header encoding string

        @return the encoding string
        @aribaapi private    
    */        
    public String mimeHeaderEncoding ()
    {
        return I18NConstants.EncodingB;
    }
    
    /**
        Returns the MIME body encoding string

        @return the encoding string
        @aribaapi private    
    */            
    public String mimeBodyEncoding ()
    {
        return I18NConstants.CharacterEncoding[I18NConstants.ISO8859_8];
    }
    
    /**
        Returns the MIME body charset string

        @return the encoding string
        @aribaapi private    
    */            
    public String mimeBodyCharset ()
    {
        return I18NConstants.IANACharset[I18NConstants.ISO8859_8];
    }
}
