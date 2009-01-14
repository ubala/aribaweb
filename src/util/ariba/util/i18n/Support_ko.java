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


/**
    Support class for Korean.
    
    @aribaapi private
*/

public class Support_ko extends CJKSupport
{
    /*-----------------------------------------------------------------------
        Public Methods
      -----------------------------------------------------------------------*/

    /**
       Class constructor.
    */    
    public Support_ko ()
    {
    }

    public String fileEncoding ()
    {
        return I18NConstants.IANACharset[I18NConstants.KSC5601];
    }
            
    public String mimeBodyEncoding ()
    {
        return I18NConstants.IANACharset[I18NConstants.KSC5601];
    }
    
    public String mimeBodyCharset ()
    {
        return I18NConstants.IANACharset[I18NConstants.KSC5601];
    }
    
}
