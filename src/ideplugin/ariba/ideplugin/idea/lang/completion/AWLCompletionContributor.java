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

    $Id:$
*/
package ariba.ideplugin.idea.lang.completion;

import ariba.ideplugin.idea.lang.util.AWPathUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.XmlCompletionContributor;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlExtension;
import static com.intellij.psi.xml.XmlElementType.HTML_TAG;

public class AWLCompletionContributor extends XmlCompletionContributor
{

    private Set<LookupElement> lookupElementsCache = new HashSet<LookupElement>();

    public AWLCompletionContributor ()
    {
        super();
    }

    @Override
    public void fillCompletionVariants (final CompletionParameters parameters,
                                        final CompletionResultSet result)
    {
        super.fillCompletionVariants(parameters, result);
        if (result.isStopped()) {
            return;
        }
        final PsiElement element = parameters.getPosition();
        PsiElement parent = element.getParent();

        if (parent != null && parent.getNode().getElementType() == HTML_TAG) {
            completeTagName(parameters, result);
        }
    }

    private void completeTagName (CompletionParameters parameters,
                                  CompletionResultSet result)
    {
        PsiElement element = parameters.getPosition();
        if (!isXmlNameCompletion(parameters)) {
            return;
        }
        result.stopHere();

        PsiElement parent = element.getParent();
        if (!(parent instanceof XmlTag) ||
                !(parameters.getOriginalFile() instanceof XmlFile)) {
            return;
        }
        final XmlTag tag = (XmlTag)parent;
        final String namespace = tag.getNamespace();
        final String prefix = result.getPrefixMatcher().getPrefix();
        final int pos = prefix.indexOf(':');

        final PsiReference reference = tag.getReference();
        String namespacePrefix = tag.getNamespacePrefix();

        if (reference != null && !namespace.isEmpty() && !namespacePrefix.isEmpty()) {
            // fallback to simple completion
            if (lookupElementsCache.size() > 0) {
                Set<LookupElement> lookupElements = filterLookupElements
                        (namespacePrefix, prefix);
                result.addAllElements(lookupElements);
            }
        }
        else {
            final CompletionResultSet newResult = result.withPrefixMatcher(pos >= 0 ?
                    prefix.substring(pos + 1):
                    prefix);

            final XmlFile file = (XmlFile)parameters.getOriginalFile();
            final List<Pair<String, String>> names = XmlExtension.getExtension(file)
                    .getAvailableTagNames(file, tag);
            List<Pair<String, String>> prefixedNames = prefixWithNamespace(names);
            if (prefixedNames.size() == lookupElementsCache.size()) {
                newResult.addAllElements(lookupElementsCache);
            }
            else {
                lookupElementsCache.clear();
                for (Pair<String, String> pair : prefixedNames) {
                    final String name = pair.getFirst();
                    final String ns = pair.getSecond();
                    final LookupElement item = createLookupElement(name, ns, ns,
                            namespacePrefix);
                    lookupElementsCache.add(item);
                }
                newResult.addAllElements(lookupElementsCache);

            }
        }
    }

    private Set<LookupElement> filterLookupElements (String namespacePrefix,
                                                     String prefix)
    {
        Set<LookupElement> newResult = new HashSet<LookupElement>();
        Iterator<LookupElement> iterator = lookupElementsCache.iterator();


        while (iterator.hasNext()) {
            LookupElement next = iterator.next();
            Pair pair = (Pair)next.getObject();
            if (next.getLookupString().startsWith(namespacePrefix + ":" + prefix)) {
                String name = (String)pair.getFirst();
                String nameSpace = (String)pair.getSecond();
                LookupElement lookupElement = createLookupElement(name.substring(2),
                        nameSpace, nameSpace, "");
                newResult.add(lookupElement);
            }
        }
        return newResult;

    }

    private List<Pair<String, String>> prefixWithNamespace (List<Pair<String,
            String>> names)
    {
        final List<Pair<String, String>> newNames = new ArrayList<Pair<String, String>>();

        for (Pair<String, String> pair : names) {
            final String name = pair.getFirst();
            final String ns = pair.getSecond();
            String prefix = extractPrefixFromNS(ns);
            newNames.add(Pair.create(prefix + name, ns));
        }
        return newNames;
    }

    private String extractPrefixFromNS (String ns)
    {
        if (isAWTag(ns)) {
            return AWPathUtil.getPrefixForNameSpace(ns) + ":";
        }
        return "";
    }

    private boolean isAWTag (String ns)
    {
        return AWPathUtil.AW_A_URI.equals(ns) ||
                AWPathUtil.AW_T_URI.equals(ns) ||
                AWPathUtil.AW_W_URI.equals(ns);
    }


}