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
package ariba.ideplugin.idea.lang;

import org.jetbrains.annotations.NotNull;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlFileNSInfoProvider;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.util.XmlUtil;
import static ariba.ideplugin.idea.lang.util.AWPathUtil.AW_A_URI;
import static ariba.ideplugin.idea.lang.util.AWPathUtil.AW_T_URI;
import static ariba.ideplugin.idea.lang.util.AWPathUtil.AW_W_URI;
import static ariba.ideplugin.idea.lang.util.AWPathUtil.A_PREFIX;
import static ariba.ideplugin.idea.lang.util.AWPathUtil.T_PREFIX;
import static ariba.ideplugin.idea.lang.util.AWPathUtil.W_PREFIX;

public class AWDefaultNSProvider implements XmlFileNSInfoProvider
{

    private static final String[][] myNamespaces = new String[][]{
            new String[]{"", XmlUtil.HTML_URI},
            new String[]{A_PREFIX, AW_A_URI},
            new String[]{T_PREFIX, AW_T_URI},
            new String[]{W_PREFIX, AW_W_URI}};

    // nsPrefix, namespaceId
    public String[][] getDefaultNamespaces (@NotNull XmlFile xmlFile)
    {
        final XmlDocument document = xmlFile.getDocument();
        if (document != null && isAcceptableFile(xmlFile.getName())) {
            final XmlTag tag = document.getRootTag();
            return myNamespaces;
        }
        return null;
    }

    private boolean isAcceptableFile (String name)
    {
        return name != null && name.toLowerCase().endsWith(".awl");
    }
}