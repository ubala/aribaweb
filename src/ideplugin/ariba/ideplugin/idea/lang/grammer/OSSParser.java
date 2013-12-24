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
package ariba.ideplugin.idea.lang.grammer;

import org.jetbrains.annotations.NotNull;
import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import com.intellij.lang.PsiParser;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.tree.IElementType;
import static ariba.ideplugin.idea.lang.grammer.psi.OSSTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.TOKEN_ADVANCER;
import static com.intellij.lang.parser.GeneratedParserUtilBase._SECTION_GENERAL_;
import static com.intellij.lang.parser.GeneratedParserUtilBase._SECTION_RECOVER_;
import static com.intellij.lang.parser.GeneratedParserUtilBase.adapt_builder_;
import static com.intellij.lang.parser.GeneratedParserUtilBase.consumeToken;
import static com.intellij.lang.parser.GeneratedParserUtilBase
        .empty_element_parsed_guard_;
import static com.intellij.lang.parser.GeneratedParserUtilBase.enterErrorRecordingSection;
import static com.intellij.lang.parser.GeneratedParserUtilBase.exitErrorRecordingSection;
import static com.intellij.lang.parser.GeneratedParserUtilBase.nextTokenIs;
import static com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_;
import static com.intellij.lang.parser.GeneratedParserUtilBase.report_error_;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class OSSParser implements PsiParser
{

    public static Logger LOG_ = Logger.getInstance("ariba.ideplugin.idea.lang.grammer" +
            ".OSSParser");

    /* ********************************************************** */
    // KW_CLASS
    //                     | KW_DISPLAYKEY
    //                     | KW_SEARCHOPERATION
    //                     | KW_TRAIT
    //                     | KW_OPERATION
    //                     | KW_FIELD
    //                     | KW_BINDINGS
    //                     | KW_COMPONENT
    //                     | KW_OBJECT
    //                     | KW_VALUEREDIRECTOR
    //                     | KW_ACTION
    //                     | KW_ACTIONRESULTS
    //                     | KW_VISIBLE
    //                     | KW_PAGENAME
    //                     | KW_PAGEBINDINGS
    //                     | KW_AFTER
    //                     | KW_ZLEFT
    //                     | KW_ZRIGHT
    //                     | KW_ZNONE
    //                     | KW_LAYOUT
    //                     | KW_HOMEPAGE
    //                     | KW_MODULE_TRAIT
    //                     | KW_WRAPPERCOMPONENT
    //                     | KW_WRAPPERBINDINGS
    //                     | KW_PORTLETWRAPPER
    //                     | KW_DISPLAYGROUP
    //                     | KW_NEEDSFORM
    //                     | KW_BEFORE
    //                     | KW_TEXTSEARCHSUPPORTED
    //                     | KW_USETEXTINDEX
    //                     | KW_LABEL
    //                     | KW_MODULE
    //                     | IDENTIFIER
    static boolean IDENTIFIER_KEY (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "IDENTIFIER_KEY")) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        result_ = consumeToken(builder_, KW_CLASS);
        if (!result_) {
            result_ = consumeToken(builder_, KW_DISPLAYKEY);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_SEARCHOPERATION);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_TRAIT);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_OPERATION);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_FIELD);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_BINDINGS);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_COMPONENT);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_OBJECT);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_VALUEREDIRECTOR);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_ACTION);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_ACTIONRESULTS);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_VISIBLE);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_PAGENAME);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_PAGEBINDINGS);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_AFTER);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_ZLEFT);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_ZRIGHT);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_ZNONE);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_LAYOUT);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_HOMEPAGE);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_MODULE_TRAIT);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_WRAPPERCOMPONENT);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_WRAPPERBINDINGS);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_PORTLETWRAPPER);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_DISPLAYGROUP);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_NEEDSFORM);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_BEFORE);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_TEXTSEARCHSUPPORTED);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_USETEXTINDEX);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_LABEL);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KW_MODULE);
        }
        if (!result_) {
            result_ = consumeToken(builder_, IDENTIFIER);
        }
        if (!result_) {
            marker_.rollbackTo();
        }
        else {
            marker_.drop();
        }
        return result_;
    }

    /* ********************************************************** */
    // STRING_LITERAL | IDENTIFIER_KEY
    public static boolean key (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "key")) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<key>");
        result_ = consumeToken(builder_, STRING_LITERAL);
        if (!result_) {
            result_ = IDENTIFIER_KEY(builder_, level_ + 1);
        }
        if (result_) {
            marker_.done(KEY);
        }
        else {
            marker_.rollbackTo();
        }
        result_ = exitErrorRecordingSection(builder_, level_, result_, false,
                _SECTION_GENERAL_,
                null);
        return result_;
    }

    /* ********************************************************** */
    // simpleValue
    //                     | wrappedList
    //                     | map
    //                     | DYN_FIELDPATHBINDING
    //                     | localizedString
    //                     | EXPR_LITERAL
    static boolean listValue (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "listValue")) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        result_ = simpleValue(builder_, level_ + 1);
        if (!result_) {
            result_ = wrappedList(builder_, level_ + 1);
        }
        if (!result_) {
            result_ = map(builder_, level_ + 1);
        }
        if (!result_) {
            result_ = consumeToken(builder_, DYN_FIELDPATHBINDING);
        }
        if (!result_) {
            result_ = localizedString(builder_, level_ + 1);
        }
        if (!result_) {
            result_ = consumeToken(builder_, EXPR_LITERAL);
        }
        if (!result_) {
            marker_.rollbackTo();
        }
        else {
            marker_.drop();
        }
        return result_;
    }

    /* ********************************************************** */
    // LOCALIZATION_KEY key
    public static boolean localizedString (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "localizedString")) {
            return false;
        }
        if (!nextTokenIs(builder_, LOCALIZATION_KEY)) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        result_ = consumeToken(builder_, LOCALIZATION_KEY);
        result_ = result_ && key(builder_, level_ + 1);
        if (result_) {
            marker_.done(LOCALIZED_STRING);
        }
        else {
            marker_.rollbackTo();
        }
        return result_;
    }

    /* ********************************************************** */
    // '{' [  mapEntry  (';' mapEntry)*  ';'?  ] '}'
    public static boolean map (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "map")) {
            return false;
        }
        if (!nextTokenIs(builder_, LEFT_BRACE)) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        result_ = consumeToken(builder_, LEFT_BRACE);
        result_ = result_ && map_1(builder_, level_ + 1);
        result_ = result_ && consumeToken(builder_, RIGHT_BRACE);
        if (result_) {
            marker_.done(MAP);
        }
        else {
            marker_.rollbackTo();
        }
        return result_;
    }

    // [  mapEntry  (';' mapEntry)*  ';'?  ]
    private static boolean map_1 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "map_1")) {
            return false;
        }
        map_1_0(builder_, level_ + 1);
        return true;
    }

    // mapEntry  (';' mapEntry)*  ';'?
    private static boolean map_1_0 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "map_1_0")) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        result_ = mapEntry(builder_, level_ + 1);
        result_ = result_ && map_1_0_1(builder_, level_ + 1);
        result_ = result_ && map_1_0_2(builder_, level_ + 1);
        if (!result_) {
            marker_.rollbackTo();
        }
        else {
            marker_.drop();
        }
        return result_;
    }

    // (';' mapEntry)*
    private static boolean map_1_0_1 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "map_1_0_1")) {
            return false;
        }
        int offset_ = builder_.getCurrentOffset();
        while (true) {
            if (!map_1_0_1_0(builder_, level_ + 1)) {
                break;
            }
            int next_offset_ = builder_.getCurrentOffset();
            if (offset_ == next_offset_) {
                empty_element_parsed_guard_(builder_, offset_, "map_1_0_1");
                break;
            }
            offset_ = next_offset_;
        }
        return true;
    }

    // ';' mapEntry
    private static boolean map_1_0_1_0 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "map_1_0_1_0")) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        result_ = consumeToken(builder_, SEMI);
        result_ = result_ && mapEntry(builder_, level_ + 1);
        if (!result_) {
            marker_.rollbackTo();
        }
        else {
            marker_.drop();
        }
        return result_;
    }

    // ';'?
    private static boolean map_1_0_2 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "map_1_0_2")) {
            return false;
        }
        consumeToken(builder_, SEMI);
        return true;
    }

    /* ********************************************************** */
    // key ':' value
    static boolean mapEntry (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "mapEntry")) {
            return false;
        }
        boolean result_ = false;
        boolean pinned_ = false;
        Marker marker_ = builder_.mark();
        enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
        result_ = key(builder_, level_ + 1);
        result_ = result_ && consumeToken(builder_, COLON);
        pinned_ = result_; // pin = 2
        result_ = result_ && value(builder_, level_ + 1);
        if (!result_ && !pinned_) {
            marker_.rollbackTo();
        }
        else {
            marker_.drop();
        }
        result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_,
                _SECTION_GENERAL_, null);
        return result_ || pinned_;
    }

    /* ********************************************************** */
    // precedenceChainNode ('=>' precedenceChainNode)+ ';'
    public static boolean precedenceChain (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "precedenceChain")) {
            return false;
        }
        boolean result_ = false;
        boolean pinned_ = false;
        Marker marker_ = builder_.mark();
        enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_,
                "<precedence chain>");
        result_ = precedenceChainNode(builder_, level_ + 1);
        result_ = result_ && precedenceChain_1(builder_, level_ + 1);
        pinned_ = result_; // pin = 2
        result_ = result_ && consumeToken(builder_, SEMI);
        if (result_ || pinned_) {
            marker_.done(PRECEDENCE_CHAIN);
        }
        else {
            marker_.rollbackTo();
        }
        result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_,
                _SECTION_GENERAL_, null);
        return result_ || pinned_;
    }

    // ('=>' precedenceChainNode)+
    private static boolean precedenceChain_1 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "precedenceChain_1")) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        result_ = precedenceChain_1_0(builder_, level_ + 1);
        int offset_ = builder_.getCurrentOffset();
        while (result_) {
            if (!precedenceChain_1_0(builder_, level_ + 1)) {
                break;
            }
            int next_offset_ = builder_.getCurrentOffset();
            if (offset_ == next_offset_) {
                empty_element_parsed_guard_(builder_, offset_, "precedenceChain_1");
                break;
            }
            offset_ = next_offset_;
        }
        if (!result_) {
            marker_.rollbackTo();
        }
        else {
            marker_.drop();
        }
        return result_;
    }

    // '=>' precedenceChainNode
    private static boolean precedenceChain_1_0 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "precedenceChain_1_0")) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        result_ = consumeToken(builder_, NEXT);
        result_ = result_ && precedenceChainNode(builder_, level_ + 1);
        if (!result_) {
            marker_.rollbackTo();
        }
        else {
            marker_.drop();
        }
        return result_;
    }

    /* ********************************************************** */
    // DYN_FIELDPATHBINDING | IDENTIFIER_KEY  | '*'
    static boolean precedenceChainNode (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "precedenceChainNode")) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        result_ = consumeToken(builder_, DYN_FIELDPATHBINDING);
        if (!result_) {
            result_ = IDENTIFIER_KEY(builder_, level_ + 1);
        }
        if (!result_) {
            result_ = consumeToken(builder_, STAR);
        }
        if (!result_) {
            marker_.rollbackTo();
        }
        else {
            marker_.drop();
        }
        return result_;
    }

    /* ********************************************************** */
    // key ':' value ';'
    static boolean property (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "property")) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        result_ = key(builder_, level_ + 1);
        result_ = result_ && consumeToken(builder_, COLON);
        result_ = result_ && value(builder_, level_ + 1);
        result_ = result_ && consumeToken(builder_, SEMI);
        if (!result_) {
            marker_.rollbackTo();
        }
        else {
            marker_.drop();
        }
        return result_;
    }

    /* ********************************************************** */
    // selector+  traitList? ('{' ruleBody  '}' | ';')
    public static boolean rule (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "rule")) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<rule>");
        result_ = rule_0(builder_, level_ + 1);
        result_ = result_ && rule_1(builder_, level_ + 1);
        result_ = result_ && rule_2(builder_, level_ + 1);
        if (result_) {
            marker_.done(RULE);
        }
        else {
            marker_.rollbackTo();
        }
        result_ = exitErrorRecordingSection(builder_, level_, result_, false,
                _SECTION_GENERAL_,
                null);
        return result_;
    }

    // selector+
    private static boolean rule_0 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "rule_0")) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        result_ = selector(builder_, level_ + 1);
        int offset_ = builder_.getCurrentOffset();
        while (result_) {
            if (!selector(builder_, level_ + 1)) {
                break;
            }
            int next_offset_ = builder_.getCurrentOffset();
            if (offset_ == next_offset_) {
                empty_element_parsed_guard_(builder_, offset_, "rule_0");
                break;
            }
            offset_ = next_offset_;
        }
        if (!result_) {
            marker_.rollbackTo();
        }
        else {
            marker_.drop();
        }
        return result_;
    }

    // traitList?
    private static boolean rule_1 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "rule_1")) {
            return false;
        }
        traitList(builder_, level_ + 1);
        return true;
    }

    // '{' ruleBody  '}' | ';'
    private static boolean rule_2 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "rule_2")) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        result_ = rule_2_0(builder_, level_ + 1);
        if (!result_) {
            result_ = consumeToken(builder_, SEMI);
        }
        if (!result_) {
            marker_.rollbackTo();
        }
        else {
            marker_.drop();
        }
        return result_;
    }

    // '{' ruleBody  '}'
    private static boolean rule_2_0 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "rule_2_0")) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        result_ = consumeToken(builder_, LEFT_BRACE);
        result_ = result_ && ruleBody(builder_, level_ + 1);
        result_ = result_ && consumeToken(builder_, RIGHT_BRACE);
        if (!result_) {
            marker_.rollbackTo();
        }
        else {
            marker_.drop();
        }
        return result_;
    }

    /* ********************************************************** */
    // ruleBodyKeyValue*  rule* ruleBodyKeyValue* rule* precedenceChain*
    public static boolean ruleBody (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "ruleBody")) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<rule body>");
        result_ = ruleBody_0(builder_, level_ + 1);
        result_ = result_ && ruleBody_1(builder_, level_ + 1);
        result_ = result_ && ruleBody_2(builder_, level_ + 1);
        result_ = result_ && ruleBody_3(builder_, level_ + 1);
        result_ = result_ && ruleBody_4(builder_, level_ + 1);
        if (result_) {
            marker_.done(RULE_BODY);
        }
        else {
            marker_.rollbackTo();
        }
        result_ = exitErrorRecordingSection(builder_, level_, result_, false,
                _SECTION_GENERAL_,
                null);
        return result_;
    }

    // ruleBodyKeyValue*
    private static boolean ruleBody_0 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "ruleBody_0")) {
            return false;
        }
        int offset_ = builder_.getCurrentOffset();
        while (true) {
            if (!ruleBodyKeyValue(builder_, level_ + 1)) {
                break;
            }
            int next_offset_ = builder_.getCurrentOffset();
            if (offset_ == next_offset_) {
                empty_element_parsed_guard_(builder_, offset_, "ruleBody_0");
                break;
            }
            offset_ = next_offset_;
        }
        return true;
    }

    // rule*
    private static boolean ruleBody_1 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "ruleBody_1")) {
            return false;
        }
        int offset_ = builder_.getCurrentOffset();
        while (true) {
            if (!rule(builder_, level_ + 1)) {
                break;
            }
            int next_offset_ = builder_.getCurrentOffset();
            if (offset_ == next_offset_) {
                empty_element_parsed_guard_(builder_, offset_, "ruleBody_1");
                break;
            }
            offset_ = next_offset_;
        }
        return true;
    }

    // ruleBodyKeyValue*
    private static boolean ruleBody_2 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "ruleBody_2")) {
            return false;
        }
        int offset_ = builder_.getCurrentOffset();
        while (true) {
            if (!ruleBodyKeyValue(builder_, level_ + 1)) {
                break;
            }
            int next_offset_ = builder_.getCurrentOffset();
            if (offset_ == next_offset_) {
                empty_element_parsed_guard_(builder_, offset_, "ruleBody_2");
                break;
            }
            offset_ = next_offset_;
        }
        return true;
    }

    // rule*
    private static boolean ruleBody_3 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "ruleBody_3")) {
            return false;
        }
        int offset_ = builder_.getCurrentOffset();
        while (true) {
            if (!rule(builder_, level_ + 1)) {
                break;
            }
            int next_offset_ = builder_.getCurrentOffset();
            if (offset_ == next_offset_) {
                empty_element_parsed_guard_(builder_, offset_, "ruleBody_3");
                break;
            }
            offset_ = next_offset_;
        }
        return true;
    }

    // precedenceChain*
    private static boolean ruleBody_4 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "ruleBody_4")) {
            return false;
        }
        int offset_ = builder_.getCurrentOffset();
        while (true) {
            if (!precedenceChain(builder_, level_ + 1)) {
                break;
            }
            int next_offset_ = builder_.getCurrentOffset();
            if (offset_ == next_offset_) {
                empty_element_parsed_guard_(builder_, offset_, "ruleBody_4");
                break;
            }
            offset_ = next_offset_;
        }
        return true;
    }

    /* ********************************************************** */
    // key ':' value '!'? ';'?
    public static boolean ruleBodyKeyValue (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "ruleBodyKeyValue")) {
            return false;
        }
        boolean result_ = false;
        boolean pinned_ = false;
        Marker marker_ = builder_.mark();
        enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_,
                "<rule body key value>");
        result_ = key(builder_, level_ + 1);
        result_ = result_ && consumeToken(builder_, COLON);
        pinned_ = result_; // pin = 2
        result_ = result_ && report_error_(builder_, value(builder_, level_ + 1));
        result_ = pinned_ && report_error_(builder_, ruleBodyKeyValue_3(builder_,
                level_ + 1)) && result_;
        result_ = pinned_ && ruleBodyKeyValue_4(builder_, level_ + 1) && result_;
        if (result_ || pinned_) {
            marker_.done(RULE_BODY_KEY_VALUE);
        }
        else {
            marker_.rollbackTo();
        }
        result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_,
                _SECTION_GENERAL_, null);
        return result_ || pinned_;
    }

    // '!'?
    private static boolean ruleBodyKeyValue_3 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "ruleBodyKeyValue_3")) {
            return false;
        }
        consumeToken(builder_, EXCL_MARK);
        return true;
    }

    // ';'?
    private static boolean ruleBodyKeyValue_4 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "ruleBodyKeyValue_4")) {
            return false;
        }
        consumeToken(builder_, SEMI);
        return true;
    }

    /* ********************************************************** */
    // rule *
    static boolean rules (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "rules")) {
            return false;
        }
        int offset_ = builder_.getCurrentOffset();
        while (true) {
            if (!rule(builder_, level_ + 1)) {
                break;
            }
            int next_offset_ = builder_.getCurrentOffset();
            if (offset_ == next_offset_) {
                empty_element_parsed_guard_(builder_, offset_, "rules");
                break;
            }
            offset_ = next_offset_;
        }
        return true;
    }

    /* ********************************************************** */
    // '@'? (selectorDef |  '~' IDENTIFIER_KEY)
    public static boolean selector (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "selector")) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<selector>");
        result_ = selector_0(builder_, level_ + 1);
        result_ = result_ && selector_1(builder_, level_ + 1);
        if (result_) {
            marker_.done(SELECTOR);
        }
        else {
            marker_.rollbackTo();
        }
        result_ = exitErrorRecordingSection(builder_, level_, result_, false,
                _SECTION_GENERAL_,
                null);
        return result_;
    }

    // '@'?
    private static boolean selector_0 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "selector_0")) {
            return false;
        }
        consumeToken(builder_, AT);
        return true;
    }

    // selectorDef |  '~' IDENTIFIER_KEY
    private static boolean selector_1 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "selector_1")) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        result_ = selectorDef(builder_, level_ + 1);
        if (!result_) {
            result_ = selector_1_1(builder_, level_ + 1);
        }
        if (!result_) {
            marker_.rollbackTo();
        }
        else {
            marker_.drop();
        }
        return result_;
    }

    // '~' IDENTIFIER_KEY
    private static boolean selector_1_1 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "selector_1_1")) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        result_ = consumeToken(builder_, NEGATE);
        result_ = result_ && IDENTIFIER_KEY(builder_, level_ + 1);
        if (!result_) {
            marker_.rollbackTo();
        }
        else {
            marker_.drop();
        }
        return result_;
    }

    /* ********************************************************** */
    // IDENTIFIER_KEY   selectorValue?
    public static boolean selectorDef (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "selectorDef")) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<selector def>");
        result_ = IDENTIFIER_KEY(builder_, level_ + 1);
        result_ = result_ && selectorDef_1(builder_, level_ + 1);
        if (result_) {
            marker_.done(SELECTOR_DEF);
        }
        else {
            marker_.rollbackTo();
        }
        result_ = exitErrorRecordingSection(builder_, level_, result_, false,
                _SECTION_GENERAL_,
                null);
        return result_;
    }

    // selectorValue?
    private static boolean selectorDef_1 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "selectorDef_1")) {
            return false;
        }
        selectorValue(builder_, level_ + 1);
        return true;
    }

    /* ********************************************************** */
    // '=' (simpleValue | '(' valueOrList ')' )
    public static boolean selectorValue (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "selectorValue")) {
            return false;
        }
        if (!nextTokenIs(builder_, OP_EQ)) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        result_ = consumeToken(builder_, OP_EQ);
        result_ = result_ && selectorValue_1(builder_, level_ + 1);
        if (result_) {
            marker_.done(SELECTOR_VALUE);
        }
        else {
            marker_.rollbackTo();
        }
        return result_;
    }

    // simpleValue | '(' valueOrList ')'
    private static boolean selectorValue_1 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "selectorValue_1")) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        result_ = simpleValue(builder_, level_ + 1);
        if (!result_) {
            result_ = selectorValue_1_1(builder_, level_ + 1);
        }
        if (!result_) {
            marker_.rollbackTo();
        }
        else {
            marker_.drop();
        }
        return result_;
    }

    // '(' valueOrList ')'
    private static boolean selectorValue_1_1 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "selectorValue_1_1")) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        result_ = consumeToken(builder_, LEFT_PARENTH);
        result_ = result_ && valueOrList(builder_, level_ + 1);
        result_ = result_ && consumeToken(builder_, RIGHT_PARENTH);
        if (!result_) {
            marker_.rollbackTo();
        }
        else {
            marker_.drop();
        }
        return result_;
    }

    /* ********************************************************** */
    // STRING_LITERAL
    //                     |  SQ_STRING_LITERAL
    //                     | INT_LITERAL
    //                     | FLT_LITERAL
    static boolean simpleVal1 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "simpleVal1")) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        result_ = consumeToken(builder_, STRING_LITERAL);
        if (!result_) {
            result_ = consumeToken(builder_, SQ_STRING_LITERAL);
        }
        if (!result_) {
            result_ = consumeToken(builder_, INT_LITERAL);
        }
        if (!result_) {
            result_ = consumeToken(builder_, FLT_LITERAL);
        }
        if (!result_) {
            marker_.rollbackTo();
        }
        else {
            marker_.drop();
        }
        return result_;
    }

    /* ********************************************************** */
    // simpleVal1
    //                     | IDENTIFIER_KEY
    //                     | KEY_PATH
    //                     | "true"
    //                     | "false"
    //                     | "null"
    public static boolean simpleValue (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "simpleValue")) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<simple value>");
        result_ = simpleVal1(builder_, level_ + 1);
        if (!result_) {
            result_ = IDENTIFIER_KEY(builder_, level_ + 1);
        }
        if (!result_) {
            result_ = consumeToken(builder_, KEY_PATH);
        }
        if (!result_) {
            result_ = consumeToken(builder_, "true");
        }
        if (!result_) {
            result_ = consumeToken(builder_, "false");
        }
        if (!result_) {
            result_ = consumeToken(builder_, "null");
        }
        if (result_) {
            marker_.done(SIMPLE_VALUE);
        }
        else {
            marker_.rollbackTo();
        }
        result_ = exitErrorRecordingSection(builder_, level_, result_, false,
                _SECTION_GENERAL_,
                null);
        return result_;
    }

    /* ********************************************************** */
    // '#' IDENTIFIER  (',' IDENTIFIER)*
    public static boolean traitList (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "traitList")) {
            return false;
        }
        if (!nextTokenIs(builder_, HASH)) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        result_ = consumeToken(builder_, HASH);
        result_ = result_ && consumeToken(builder_, IDENTIFIER);
        result_ = result_ && traitList_2(builder_, level_ + 1);
        if (result_) {
            marker_.done(TRAIT_LIST);
        }
        else {
            marker_.rollbackTo();
        }
        return result_;
    }

    // (',' IDENTIFIER)*
    private static boolean traitList_2 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "traitList_2")) {
            return false;
        }
        int offset_ = builder_.getCurrentOffset();
        while (true) {
            if (!traitList_2_0(builder_, level_ + 1)) {
                break;
            }
            int next_offset_ = builder_.getCurrentOffset();
            if (offset_ == next_offset_) {
                empty_element_parsed_guard_(builder_, offset_, "traitList_2");
                break;
            }
            offset_ = next_offset_;
        }
        return true;
    }

    // ',' IDENTIFIER
    private static boolean traitList_2_0 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "traitList_2_0")) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        result_ = consumeToken(builder_, COMA);
        result_ = result_ && consumeToken(builder_, IDENTIFIER);
        if (!result_) {
            marker_.rollbackTo();
        }
        else {
            marker_.drop();
        }
        return result_;
    }

    /* ********************************************************** */
    // valueOrList
    //                 | wrappedList
    //                 | map
    //                 | DYN_FIELDPATHBINDING
    //                 | localizedString
    //                 | EXPR_LITERAL
    public static boolean value (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "value")) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<value>");
        result_ = valueOrList(builder_, level_ + 1);
        if (!result_) {
            result_ = wrappedList(builder_, level_ + 1);
        }
        if (!result_) {
            result_ = map(builder_, level_ + 1);
        }
        if (!result_) {
            result_ = consumeToken(builder_, DYN_FIELDPATHBINDING);
        }
        if (!result_) {
            result_ = localizedString(builder_, level_ + 1);
        }
        if (!result_) {
            result_ = consumeToken(builder_, EXPR_LITERAL);
        }
        if (result_) {
            marker_.done(VALUE);
        }
        else {
            marker_.rollbackTo();
        }
        result_ = exitErrorRecordingSection(builder_, level_, result_, false,
                _SECTION_GENERAL_,
                null);
        return result_;
    }

    /* ********************************************************** */
    // listValue (','  listValue)*
    public static boolean valueOrList (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "valueOrList")) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_,
                "<value or list>");
        result_ = listValue(builder_, level_ + 1);
        result_ = result_ && valueOrList_1(builder_, level_ + 1);
        if (result_) {
            marker_.done(VALUE_OR_LIST);
        }
        else {
            marker_.rollbackTo();
        }
        result_ = exitErrorRecordingSection(builder_, level_, result_, false,
                _SECTION_GENERAL_,
                null);
        return result_;
    }

    // (','  listValue)*
    private static boolean valueOrList_1 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "valueOrList_1")) {
            return false;
        }
        int offset_ = builder_.getCurrentOffset();
        while (true) {
            if (!valueOrList_1_0(builder_, level_ + 1)) {
                break;
            }
            int next_offset_ = builder_.getCurrentOffset();
            if (offset_ == next_offset_) {
                empty_element_parsed_guard_(builder_, offset_, "valueOrList_1");
                break;
            }
            offset_ = next_offset_;
        }
        return true;
    }

    // ','  listValue
    private static boolean valueOrList_1_0 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "valueOrList_1_0")) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        result_ = consumeToken(builder_, COMA);
        result_ = result_ && listValue(builder_, level_ + 1);
        if (!result_) {
            marker_.rollbackTo();
        }
        else {
            marker_.drop();
        }
        return result_;
    }

    /* ********************************************************** */
    // '[' listValue  (',' listValue) * ']'
    public static boolean wrappedList (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "wrappedList")) {
            return false;
        }
        if (!nextTokenIs(builder_, LEFT_BRACKET)) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        result_ = consumeToken(builder_, LEFT_BRACKET);
        result_ = result_ && listValue(builder_, level_ + 1);
        result_ = result_ && wrappedList_2(builder_, level_ + 1);
        result_ = result_ && consumeToken(builder_, RIGHT_BRACKET);
        if (result_) {
            marker_.done(WRAPPED_LIST);
        }
        else {
            marker_.rollbackTo();
        }
        return result_;
    }

    // (',' listValue) *
    private static boolean wrappedList_2 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "wrappedList_2")) {
            return false;
        }
        int offset_ = builder_.getCurrentOffset();
        while (true) {
            if (!wrappedList_2_0(builder_, level_ + 1)) {
                break;
            }
            int next_offset_ = builder_.getCurrentOffset();
            if (offset_ == next_offset_) {
                empty_element_parsed_guard_(builder_, offset_, "wrappedList_2");
                break;
            }
            offset_ = next_offset_;
        }
        return true;
    }

    // ',' listValue
    private static boolean wrappedList_2_0 (PsiBuilder builder_, int level_)
    {
        if (!recursion_guard_(builder_, level_, "wrappedList_2_0")) {
            return false;
        }
        boolean result_ = false;
        Marker marker_ = builder_.mark();
        result_ = consumeToken(builder_, COMA);
        result_ = result_ && listValue(builder_, level_ + 1);
        if (!result_) {
            marker_.rollbackTo();
        }
        else {
            marker_.drop();
        }
        return result_;
    }

    @NotNull
    public ASTNode parse (IElementType root_, PsiBuilder builder_)
    {
        int level_ = 0;
        boolean result_;
        builder_ = adapt_builder_(root_, builder_, this);
        if (root_ == KEY) {
            result_ = key(builder_, level_ + 1);
        }
        else if (root_ == LOCALIZED_STRING) {
            result_ = localizedString(builder_, level_ + 1);
        }
        else if (root_ == MAP) {
            result_ = map(builder_, level_ + 1);
        }
        else if (root_ == PRECEDENCE_CHAIN) {
            result_ = precedenceChain(builder_, level_ + 1);
        }
        else if (root_ == RULE) {
            result_ = rule(builder_, level_ + 1);
        }
        else if (root_ == RULE_BODY) {
            result_ = ruleBody(builder_, level_ + 1);
        }
        else if (root_ == RULE_BODY_KEY_VALUE) {
            result_ = ruleBodyKeyValue(builder_, level_ + 1);
        }
        else if (root_ == SELECTOR) {
            result_ = selector(builder_, level_ + 1);
        }
        else if (root_ == SELECTOR_DEF) {
            result_ = selectorDef(builder_, level_ + 1);
        }
        else if (root_ == SELECTOR_VALUE) {
            result_ = selectorValue(builder_, level_ + 1);
        }
        else if (root_ == SIMPLE_VALUE) {
            result_ = simpleValue(builder_, level_ + 1);
        }
        else if (root_ == TRAIT_LIST) {
            result_ = traitList(builder_, level_ + 1);
        }
        else if (root_ == VALUE) {
            result_ = value(builder_, level_ + 1);
        }
        else if (root_ == VALUE_OR_LIST) {
            result_ = valueOrList(builder_, level_ + 1);
        }
        else if (root_ == WRAPPED_LIST) {
            result_ = wrappedList(builder_, level_ + 1);
        }
        else {
            Marker marker_ = builder_.mark();
            enterErrorRecordingSection(builder_, level_, _SECTION_RECOVER_, null);
            result_ = parse_root_(root_, builder_, level_);
            exitErrorRecordingSection(builder_, level_, result_, true, _SECTION_RECOVER_,
                    TOKEN_ADVANCER);
            marker_.done(root_);
        }
        return builder_.getTreeBuilt();
    }

    protected boolean parse_root_ (final IElementType root_, final PsiBuilder builder_,
                                   final int level_)
    {
        return rules(builder_, level_ + 1);
    }

}
