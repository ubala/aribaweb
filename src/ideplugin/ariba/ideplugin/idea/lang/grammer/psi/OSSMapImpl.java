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

    $Id: //ariba/platform/ui/ideplugin/ariba/ideplugin/idea/lang/grammer/psi/OSSMapImpl
    .java $
*/
package ariba.ideplugin.idea.lang.grammer.psi;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;

public class OSSMapImpl extends ASTWrapperPsiElement implements OSSMap
{

    public OSSMapImpl (ASTNode node)
    {
        super(node);
    }

    @Override
    @NotNull
    public List<OSSKey> getKeyList ()
    {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, OSSKey.class);
    }

    @Override
    @NotNull
    public List<OSSValue> getValueList ()
    {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, OSSValue.class);
    }

    public void accept (@NotNull PsiElementVisitor visitor)
    {
        if (visitor instanceof OSSVisitor) {
            ((OSSVisitor)visitor).visitMap(this);
        }
        else {
            super.accept(visitor);
        }
    }

}
