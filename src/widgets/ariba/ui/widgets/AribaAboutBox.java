/*
    Copyright (c) 1996-2012 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 
    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/AribaAboutBox.java#1 $
   
*/


package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.util.core.Fmt;
import ariba.util.core.Version;

public class AribaAboutBox extends AWComponent 
{
    private static final int YearLocalKey = 9;
    private static final String Copyright = "&copy; 1996 - {0} Ariba Inc. All Rights Reserved"; 
       
    /**
     * This method returns localized copyright message. 
     * @return String - copyright message.
     */
    public String copyright ()
    {
        String copyright = localizedJavaString(YearLocalKey, Copyright);
        return Fmt.Si(copyright, Integer.toString(Version.year));
    }
}
