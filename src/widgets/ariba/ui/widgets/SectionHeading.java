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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/SectionHeading.java#13 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.util.core.ResourceService;

public final class SectionHeading extends AWComponent
{
    public boolean inSectionWrapper ()
    {
        Boolean inSectionWrapper = (Boolean)env().peek("inSectionWrapper");
        return inSectionWrapper != null ? inSectionWrapper.booleanValue() : false;
    }

    public String getTitle ()
    {
        String title = stringValueForBinding("title");
        if (title != null)
        {
            if (ResourceService.getService().getRestrictedLocale(
                    preferredLocale()).getLanguage().equals("tr"))
            {
                title = title.replace((char)0x131, (char)0x49);
                title = title.replace((char)0x69, (char)0x130);
            }
            else if (ResourceService.getService().getRestrictedLocale(
                    preferredLocale()).getLanguage().equals("el")) {
                /*
                Special handling for certain letters in Greek. Browsers when using
                text-transform:uppercase do not follow the right grammar rules for Greek.

                e.g. If the word "??????" started a phrase or was a name it would change
                to " ?????? " and it's fine for the ? to keep the accent. However, if the
                whole word is in Caps, it should change to "??????".

                With the following, we get rid of the Accented letters all together.
                Confirmed with the i19n team that that's Okay.

                Here's the matrix:

                ? 	(lowercase alpha)               ?   (uppercase alpha)
                ? 	(lowercase alpha with tonos)    ?   (uppercase alpha)
                ? 	(lowercase epsilon)             ?   (uppercase epsilon)
                ? 	(lowercase epsilon with tonos)  ?   (uppercase epsilon)
                ? 	(lowercase iota)                ?   (uppercase iota)
                ? 	(lowercase iota with tonos)     ?   (uppercase iota)
                ? 	(lowercase eta)                 ?   (uppercase eta)
                ? 	(lowercase eta with tonos)      ?   (uppercase eta)
                ? 	(lowercase upsilon)             ?   (uppercase upsilon)
                ? 	(lowercase upsilon with tonos)  ?   (uppercase upsilon)
                ? 	(lowercase omicron)             ?   (uppercase omicron)
                ? 	(lowercase omicron with tonos)  ?   (uppercase omicron)
                ? 	(lowercase omega)               ?   (uppercase omega)
                ? 	(lowercase omega with tonos)    ?   (uppercase omega)

                */

                title = title.replace((char)0x3B1, (char)0x391);
                title = title.replace((char)0x3AC, (char)0x391);

                title = title.replace((char)0x3B5, (char)0x395);
                title = title.replace((char)0x3AD, (char)0x395);

                title = title.replace((char)0x3B9, (char)0x399);
                title = title.replace((char)0x3AF, (char)0x399);

                title = title.replace((char)0x3B7, (char)0x397);
                title = title.replace((char)0x3AE, (char)0x397);

                title = title.replace((char)0x3C5, (char)0x3A5);
                title = title.replace((char)0x3CD, (char)0x3A5);

                title = title.replace((char)0x3BF, (char)0x39F);
                title = title.replace((char)0x3CC, (char)0x39F);

                title = title.replace((char)0x3C9, (char)0x3A9);
                title = title.replace((char)0x3CE, (char)0x3A9);
            }
        }

        return title;
    }
}
