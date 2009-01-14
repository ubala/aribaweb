/*
    Copyright 2008 Craig Federighi

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License.
    You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/core/MetaRules.java#2 $
*/
package ariba.ui.meta.core;

import ariba.ui.aribaweb.core.AWBareString;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWContainerElement;
import ariba.ui.aribaweb.core.AWElement;
import ariba.ui.aribaweb.core.AWHtmlTemplateParser;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWTemplate;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.util.core.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

/*
    Register rules in context of the template in which this tag appears
 */
public class MetaRules extends AWContainerElement implements AWHtmlTemplateParser.LiteralBody
{
    public static final String TemplateId = "awTemplateId";

    /*
        Assumptions / Approach:
            - Our element instance changes when our template is reloaded
            - We only want to re-register rules if we're reloaded *and* our string actually changed
     */

    static class TemplateData {
        AWTemplate template;
        Map<MetaRules, Meta.RuleSet> elementMap = new HashMap();
    }

    public void initWithContext (Context context, AWComponent component)
    {
        Meta meta = context.meta();
        AWTemplate template = component.template();
        String templateId = template.templateName().intern();
        TemplateData data = (TemplateData)meta.identityCache().get(templateId);
        if (data != null && data.template != template) {
            // unload rules
            for (Map.Entry<MetaRules, Meta.RuleSet> entry : data.elementMap.entrySet()) {
                Meta.RuleSet ruleSet = entry.getValue();
                ruleSet.disableRules();
                // FIXME!  Need to support reloading rules with same "natural order"
                // salience as in the original rule set
            }
            data = null;
        }
        if (data == null) {
            data = new TemplateData();
            data.template = template;
            meta.identityCache().put(templateId, data);
        }

        Meta.RuleSet ruleSet= data.elementMap.get(this);
        if (ruleSet == null) {
            meta.beginRuleSet();
            try {
                new Parser(meta, contentString()).addRulesWithPredicate(
                        Arrays.asList(new Meta.Predicate(TemplateId, templateId)));
            } catch (Exception e) {
                meta.endRuleSet().disableRules();
                throw new AWGenericException(e);
            }
            data.elementMap.put(this, meta.endRuleSet());
        }
    }

    public void renderResponse (AWRequestContext requestContext, AWComponent component)
    {
        // nothing (don't emit our body!)
    }

    public String contentString ()
    {
        AWElement content = contentElement();
        Assert.that(content instanceof AWBareString, "MetaRules content must be simple string");
        return ((AWBareString)content).string();
    }

}
