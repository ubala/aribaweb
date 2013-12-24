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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.DefaultXmlExtension;
import com.intellij.xml.XmlSchemaProvider;

/**
 * A class that retrieves list of available tag. By default this should work only with the
 * DefaultXmlExtension class, but for some reason if we just let the system call the
 * default behavior it does not work.
 * There was we are calling the same code from our  and it works well.
 */
public class AWLXmlExtension extends DefaultXmlExtension
{


    public boolean isAvailable (final PsiFile file)
    {
        return AWPathUtil.isAribaWebFile(file);
    }

    @NotNull
    public List<Pair<String, String>> getAvailableTagNames (@NotNull final XmlFile file,
                                                            @NotNull final XmlTag context)
    {

        final Set<String> namespaces = new HashSet<String>(Arrays.asList(context
                .knownNamespaces()));
        final List<XmlSchemaProvider> providers = XmlSchemaProvider
                .getAvailableProviders(file);
        for (XmlSchemaProvider provider : providers) {
            namespaces.addAll(provider.getAvailableNamespaces(file, null));
        }
        final ArrayList<String> nsInfo = new ArrayList<String>();
        final String[] names = TagNameReferenceExt.getTagNameVariants(context,
                namespaces, nsInfo);
        final List<Pair<String, String>> set = new ArrayList<Pair<String,
                String>>(names.length);
        final Iterator<String> iterator = nsInfo.iterator();
        for (String name : names) {
            final int pos = name.indexOf(':');
            final String s = pos >= 0 ? name.substring(pos + 1): name;
            set.add(Pair.create(s, iterator.next()));
        }
        return set;
    }


}
