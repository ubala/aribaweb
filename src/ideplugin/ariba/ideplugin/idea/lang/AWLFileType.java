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

import javax.swing.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import com.intellij.ide.highlighter.DomSupportEnabled;
import com.intellij.ide.highlighter.XmlLikeFileType;
import com.intellij.lang.html.HTMLLanguage;

public class AWLFileType extends XmlLikeFileType implements DomSupportEnabled
{
    public static final AWLFileType INSTANCE = new AWLFileType();
    @NonNls
    public static final String DEFAULT_EXTENSION = "awl";
    @NonNls
    public static final String DOT_DEFAULT_EXTENSION = "." + DEFAULT_EXTENSION;

    private AWLFileType ()
    {
        super(HTMLLanguage.INSTANCE);
    }

    @NotNull
    public String getName ()
    {
        return "AWL";
    }

    @NotNull
    public String getDescription ()
    {
        return "Ariba Web file";
    }

    @NotNull
    public String getDefaultExtension ()
    {
        return DOT_DEFAULT_EXTENSION;
    }

    public Icon getIcon ()
    {
        return AWIcons.AwlFile;
    }
}
