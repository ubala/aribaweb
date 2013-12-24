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
package ariba.ideplugin.idea.lang.grammer.psi;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;

public class OSSValueOrListImpl extends ASTWrapperPsiElement implements OSSValueOrList
{

    public OSSValueOrListImpl (ASTNode node)
    {
        super(node);
    }

    @Override
    @NotNull
    public List<OSSLocalizedString> getLocalizedStringList ()
    {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, OSSLocalizedString.class);
    }

    @Override
    @NotNull
    public List<OSSMap> getMapList ()
    {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, OSSMap.class);
    }

    @Override
    @NotNull
    public List<OSSSimpleValue> getSimpleValueList ()
    {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, OSSSimpleValue.class);
    }

    @Override
    @NotNull
    public List<OSSWrappedList> getWrappedListList ()
    {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, OSSWrappedList.class);
    }

    public void accept (@NotNull PsiElementVisitor visitor)
    {
        if (visitor instanceof OSSVisitor) {
            ((OSSVisitor)visitor).visitValueOrList(this);
        }
        else {
            super.accept(visitor);
        }
    }

}
