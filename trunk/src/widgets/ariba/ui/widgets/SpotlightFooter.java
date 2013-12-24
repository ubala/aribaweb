/*
    Copyright 1996-2012 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/SpotlightFooter.java#1 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;

import java.util.List;
import java.util.Map;

public final class SpotlightFooter extends AWComponent
{

    private static final String SpotLightsKey = "Spotlights";
    private static final String DefaultStepKey = "DefaultStep";

    public AWEncodedString _currentSpotlight;
    public int _currentIndex;

    public static void register (AWRequestContext requestContext, AWEncodedString id, String step)
    {
        if (step == null) {
            step = DefaultStepKey;
        }
        Map<String, List<AWEncodedString>> spotlightsForStep =
            (Map<String, List<AWEncodedString>>)requestContext.get(SpotLightsKey);
        if (spotlightsForStep == null) {
            spotlightsForStep = MapUtil.map();
            requestContext.put(SpotLightsKey, spotlightsForStep);
        }
        List<AWEncodedString> spotlights = spotlightsForStep.get(step);
        if (spotlights == null) {
            spotlights = ListUtil.list();
            spotlightsForStep.put(step, spotlights);
        }
        spotlights.add(id);
    }

    @Override
    protected void sleep ()
    {
        _currentSpotlight = null;
        super.sleep();
    }

    public List<AWEncodedString> spotlights ()
    {
        Map<String, List<AWEncodedString>> spotlightsForStep =
            (Map<String, List<AWEncodedString>>)requestContext().get(SpotLightsKey);
        if (spotlightsForStep != null) {
            String step = null;
            if (pageComponent() instanceof SpotlightState) {
                step = ((SpotlightState)pageComponent()).step();
            }
            if (step == null) {
                step = DefaultStepKey;
            }
            return spotlightsForStep.get(step);
        }
        return null;
    }


    public boolean notFirst ()
    {
        return _currentIndex != 0;
    }

}
