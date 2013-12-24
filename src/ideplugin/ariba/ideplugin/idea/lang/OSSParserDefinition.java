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

import ariba.ideplugin.idea.lang.grammer.OSSLexer;
import ariba.ideplugin.idea.lang.grammer.OSSParser;
import ariba.ideplugin.idea.lang.grammer.psi.OSSFile;
import ariba.ideplugin.idea.lang.grammer.psi.OSSTypes;
import org.jetbrains.annotations.NotNull;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageUtil;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.BLOCK_COMMENT;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.EXPR_LITERAL;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.FLT_LITERAL;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.INT_LITERAL;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_ACTION;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_ACTIONRESULTS;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_AFTER;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_BEFORE;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_BINDINGS;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_CLASS;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_COMPONENT;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_DISPLAYGROUP;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_DISPLAYKEY;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_FIELD;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_HOMEPAGE;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_LABEL;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_LAYOUT;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_MODULE;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_MODULE_TRAIT;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_NEEDSFORM;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_OBJECT;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_OPERATION;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_PAGEBINDINGS;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_PAGENAME;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_PORTLETWRAPPER;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_SEARCHOPERATION;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_TEXTSEARCHSUPPORTED;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_TRAIT;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_USETEXTINDEX;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_VALUEREDIRECTOR;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_VISIBLE;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_WRAPPERBINDINGS;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_WRAPPERCOMPONENT;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_ZLEFT;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_ZNONE;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.KW_ZRIGHT;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.LINE_COMMENT;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.SQ_STRING_LITERAL;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.STRING_LITERAL;

public class OSSParserDefinition implements ParserDefinition
{
    public static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE);
    public static final IFileElementType FILE = new IFileElementType(Language
            .<OSSLanguage>findInstance(OSSLanguage.class));
    public static final TokenSet KEYWORD_BIT_SET = TokenSet.create(
            KW_ACTION,
            KW_ACTIONRESULTS,
            KW_AFTER,
            KW_BEFORE,
            KW_BINDINGS,
            KW_CLASS,
            KW_COMPONENT,
            KW_DISPLAYGROUP,
            KW_DISPLAYKEY,
            KW_FIELD,
            KW_HOMEPAGE,
            KW_LABEL,
            KW_LAYOUT,
            KW_MODULE,
            KW_MODULE_TRAIT,
            KW_NEEDSFORM,
            KW_OBJECT,
            KW_OPERATION,
            KW_PAGEBINDINGS,
            KW_PAGENAME,
            KW_PORTLETWRAPPER,
            KW_SEARCHOPERATION,
            KW_TEXTSEARCHSUPPORTED,
            KW_TRAIT,
            KW_USETEXTINDEX,
            KW_VALUEREDIRECTOR,
            KW_VISIBLE,
            KW_WRAPPERBINDINGS,
            KW_WRAPPERCOMPONENT,
            KW_ZLEFT,
            KW_ZNONE,
            KW_ZRIGHT
    );
    public static final TokenSet COMMENT_BIT_SET = TokenSet.create(
            LINE_COMMENT,
            BLOCK_COMMENT
    );
    public static final TokenSet LITERALS = TokenSet.create(
            FLT_LITERAL,
            EXPR_LITERAL,
            INT_LITERAL,
            FLT_LITERAL,
            STRING_LITERAL,
            SQ_STRING_LITERAL
    );

    @NotNull
    @Override
    public Lexer createLexer (Project project)
    {
        return new OSSLexer();
    }

    @NotNull
    public TokenSet getWhitespaceTokens ()
    {
        return WHITE_SPACES;
    }

    @NotNull
    public TokenSet getCommentTokens ()
    {
        return COMMENT_BIT_SET;
    }

    @NotNull
    public TokenSet getStringLiteralElements ()
    {
        return LITERALS;
    }

    @NotNull
    public PsiParser createParser (final Project project)
    {
        return new OSSParser();
    }

    @Override
    public IFileElementType getFileNodeType ()
    {
        return FILE;
    }

    public PsiFile createFile (FileViewProvider viewProvider)
    {
        return new OSSFile(viewProvider);
    }

    @NotNull
    @Override
    public PsiElement createElement (com.intellij.lang.ASTNode node)
    {
        return OSSTypes.Factory.createElement(node);
    }

    @Override
    public SpaceRequirements spaceExistanceTypeBetweenTokens (com.intellij.lang.ASTNode
                                                                          left,
                                                              com.intellij.lang.ASTNode
                                                                      right)
    {
        final Lexer lexer = createLexer(left.getPsi().getProject());
        return LanguageUtil.canStickTokensTogetherByLexer(left, right, lexer);
    }
}
