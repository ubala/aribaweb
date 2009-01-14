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

    $Id: //ariba/platform/util/expr/ariba/util/expr/ASTNodeVisitor.java#7 $
*/

package ariba.util.expr;

/**
    @aribaapi private
*/
public class ASTNodeVisitor
{

    public void visit (SimpleNode node)
    {
        doVisit(node);
    }


    public void visit (ExpressionNode node)
    {
        doVisit(node);
    }

    ///////////////////////////////////////////

    public void visit (ASTConst node)
    {
        doVisit(node);
    }

    public void visit (ASTProperty node)
    {
        doVisit(node);
    }

    public void visit (ASTIndex node)
    {
        doVisit(node);
    }

    public void visit (ASTVarRef node)
    {
        doVisit(node);
    }

    public void visit (ASTRootVarRef node)
    {
        doVisit(node);
    }

    public void visit (ASTThisVarRef node)
    {
        doVisit(node);
    }

    public void visit (ASTThisFieldVarRef node)
    {
        doVisit(node);
    }

    public void visit (ASTLoginUserVarRef node)
    {
        doVisit(node);
    }

    public void visit (ASTStaticField node)
    {
        doVisit(node);
    }

    public void visit (ASTCtor node)
    {
        doVisit(node);
    }

    public void visit (ASTMethod node)
    {
        doVisit(node);
    }

    public void visit (ASTStaticMethod node)
    {
        doVisit(node);
    }

    public void visit (ASTCast node)
    {
        doVisit(node);
    }

    ///////////////////////////////////////////

    public void visit (ASTChain node)
    {
        doVisit(node);
    }

    public void visit (ASTEval node)
    {
        doVisit(node);
    }

    ///////////////////////////////////////////

    public void visit (ASTAssign node)
    {
        doVisit(node);
    }

    public void visit (ASTIn node)
    {
        doVisit(node);
    }

    public void visit (ASTNotIn node)
    {
        doVisit(node);
    }

    public void visit (ASTInstanceof node)
    {
        doVisit(node);
    }

    public void visit (ASTSequence node)
    {
        doVisit(node);
    }

    public void visit (ASTKeyValue node)
    {
        doVisit(node);
    }

    public void visit (ASTList node)
    {
        doVisit(node);
    }

    public void visit (ASTMap node)
    {
        doVisit(node);
    }

    public void visit (ASTProject node)
    {
        doVisit(node);
    }

    public void visit (ASTVarDecl node)
    {
        doVisit(node);
    }

    public void visit (ASTTest node)
    {
        doVisit(node);
    }

     ///////////////////////////////////////////

    protected void doVisit (SimpleNode node)
    {}

    protected void doVisit (ExpressionNode node)
    {}

    protected void doVisit (ASTConst node)
    {}

    protected void doVisit (ASTProperty node)
    {}

    protected void doVisit (ASTIndex node)
    {}

    protected void doVisit (ASTRootVarRef node)
    {}

    protected void doVisit (ASTVarRef node)
    {};

    protected void doVisit (ASTThisVarRef node)
    {}

    protected void doVisit (ASTThisFieldVarRef node)
    {}

    protected void doVisit (ASTLoginUserVarRef node)
    {}

    protected void doVisit (ASTStaticField node)
    {}

    protected void doVisit (ASTCtor node)
    {}

    protected void doVisit (ASTChain node)
    {}

    protected void doVisit (ASTEval node)
    {}

    protected void doVisit (ASTAssign node)
    {}

    protected void doVisit (ASTMethod node)
    {}

    protected void doVisit (ASTIn node)
    {}

    protected void doVisit (ASTNotIn node)
    {}

    protected void doVisit (ASTInstanceof node)
    {}

    protected void doVisit (ASTSequence node)
    {}

    protected void doVisit (ASTStaticMethod node)
    {}

    protected void doVisit (ASTList node)
    {}

    protected void doVisit (ASTMap node)
    {}

    protected void doVisit (ASTKeyValue node)
    {}

    protected void doVisit (ASTProject node)
    {}

    protected void doVisit (ASTVarDecl node)
    {}

    protected void doVisit (ASTTest node)
    {}

    protected void doVisit (ASTCast node)
    {}
}
