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

    $Id: //ariba/platform/ui/ideplugin/ariba/ideplugin/idea/lang/grammer/psi/OSSVisitor
    .java$
*/
package ariba.ideplugin.idea.lang.grammer.psi;

import org.jetbrains.annotations.NotNull;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;

public class OSSVisitor extends PsiElementVisitor
{

    public void visitKey (@NotNull OSSKey o)
    {
        visitPsiElement(o);
    }

    public void visitLocalizedString (@NotNull OSSLocalizedString o)
    {
        visitPsiElement(o);
    }

    public void visitMap (@NotNull OSSMap o)
    {
        visitPsiElement(o);
    }

    public void visitPrecedenceChain (@NotNull OSSPrecedenceChain o)
    {
        visitPsiElement(o);
    }

    public void visitRule (@NotNull OSSRule o)
    {
        visitPsiElement(o);
    }

    public void visitRuleBody (@NotNull OSSRuleBody o)
    {
        visitPsiElement(o);
    }

    public void visitRuleBodyKeyValue (@NotNull OSSRuleBodyKeyValue o)
    {
        visitPsiElement(o);
    }

    public void visitSelector (@NotNull OSSSelector o)
    {
        visitPsiElement(o);
    }

    public void visitSelectorDef (@NotNull OSSSelectorDef o)
    {
        visitPsiElement(o);
    }

    public void visitSelectorValue (@NotNull OSSSelectorValue o)
    {
        visitPsiElement(o);
    }

    public void visitSimpleValue (@NotNull OSSSimpleValue o)
    {
        visitPsiElement(o);
    }

    public void visitTraitList (@NotNull OSSTraitList o)
    {
        visitPsiElement(o);
    }

    public void visitValue (@NotNull OSSValue o)
    {
        visitPsiElement(o);
    }

    public void visitValueOrList (@NotNull OSSValueOrList o)
    {
        visitPsiElement(o);
    }

    public void visitWrappedList (@NotNull OSSWrappedList o)
    {
        visitPsiElement(o);
    }

    public void visitPsiElement (@NotNull PsiElement o)
    {
        visitElement(o);
    }

}
