/*
    Copyright 2008 Craig Federighi

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License.
    You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $  $
*/
package ariba.ui.meta.editor;

import ariba.util.core.ClassExtension;
import ariba.util.core.ClassExtensionRegistry;
import ariba.util.core.StringUtil;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.fieldvalue.FieldPath;
import ariba.ui.meta.core.Meta;
import ariba.ui.meta.core.Rule;
import ariba.ui.meta.core.UIMeta;
import ariba.ui.aribaweb.util.AWUtil;

import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.HashMap;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.File;

public class OSSWriter
{
    protected static final ClassExtensionRegistry
        _ClassExtensionRegistry = new ClassExtensionRegistry();

    static {
        _ClassExtensionRegistry.registerClassExtension(Object.class,
                                          new OSSSerialize());
        _ClassExtensionRegistry.registerClassExtension(String.class,
                                          new FromString());
        _ClassExtensionRegistry.registerClassExtension(Map.class,
                                          new FromMap());
        _ClassExtensionRegistry.registerClassExtension(List.class,
                                          new FromList());
        _ClassExtensionRegistry.registerClassExtension(Meta.OverrideValue.class,
                                          new FromOverride());
        _ClassExtensionRegistry.registerClassExtension(FieldPath.class,
                                          new FromFieldPath());
    }

    public static void updateEditorRules (File file, List<Rule> rules)
    {
        StringWriter swriter = new StringWriter();
        PrintWriter writer = new PrintWriter(swriter);

        OSSWriter.RuleNode root = new OSSWriter.RuleNode();
        // Lame version for now.  In future, create tree by common properties
        for (Rule rule : rules) {
            root.addRule(rule);
        }
        root.write(writer, 0, true);

        writer.flush();
        updateEditorRules(file, swriter.toString());
    }

    static void updateEditorRules (File file, String updatedRuleText)
    {
        String ruleFileString = AWUtil.stringWithContentsOfFile(file);
        int sepStart = ruleFileString.indexOf(Meta.RuleFileDelimeterStart);

        String userRules = (sepStart == -1) ? ruleFileString : ruleFileString.substring(0, sepStart);
        if (!userRules.endsWith("\n\n\n")) userRules += "\n\n\n";

        String newRules = userRules + Meta.RuleFileDelimeter + updatedRuleText;
        AWUtil.writeToFile(newRules, file);
    }
    
    public static void write (Object obj, PrintWriter writer)
    {
        if (obj != null) {
            OSSSerialize.get(obj).writeOSS(obj, writer);
        } else {
            writer.write("null");
        }
    }

    public static String toOSS (Object obj)
    {
        StringWriter swriter = new StringWriter();
        PrintWriter writer = new PrintWriter(swriter);
        write(obj, writer);
        writer.flush();
        return swriter.toString();
    }

    /*
        Escape user-entered string for parser
     */
    public static String escapeString (String str)
    {
        // Auto-quote casual strings, while not quoting expressions, etc
        if (StringUtil.nullOrEmptyString(str)) return "\"\"";
        if (isKeyPathIdentifier(str)) return str;

        // Don't quote expressions / collections, or already quoted strings
        if ("${[\"\'".indexOf(str.charAt(0)) != -1) return str;

        // don't quote Override values
        if (str.charAt(str.length()-1) == '!' && isKeyPathIdentifier(str.substring(0, str.length()-1))) {
            return str;
        }

        return Fmt.S("\"%s\"", str);
    }

    public static boolean isKeyPathIdentifier (String s) {
        int c = s.length();
        if (c == 0 || !Character.isJavaIdentifierStart(s.charAt(0))) return false;

        for (int i=1; i<c; i++) {
            char ch = s.charAt(i);
            if (!Character.isJavaIdentifierPart(ch) && (ch != '.')) return false;
        }
        return true;
    }

    public static class OSSSerialize extends ClassExtension
    {
        public static OSSSerialize get (Object target)
        {
            return (OSSSerialize)_ClassExtensionRegistry.get(target);
        }

        /*
            Class extension method to format object in OSS
         */
        public void writeOSS (Object value, PrintWriter writer)
        {
            writer.print(value);
        }
    }

    static class FromString extends OSSSerialize
    {
        public void writeOSS (Object value, PrintWriter writer)
        {
            if (!isKeyPathIdentifier((String)value)) {
                writer.print("\"");
                writer.print((String)value);
                writer.print("\"");
            } else {
                writer.print(value);
            }
        }
    }

    static class FromMap extends OSSSerialize
    {
        public void writeOSS (Object value, PrintWriter writer)
        {
            writer.print("{");
            boolean first = true;
            for (Map.Entry e : ((Map<String, Object>)value).entrySet()) {
                if (first) {
                    first = false;
                } else {
                    writer.print("; ");
                }
                writer.print(e.getKey());
                writer.print(":");
                write(e.getValue(), writer);
            }
            writer.print("}");
        }
    }

    static class FromList extends OSSSerialize
    {
        public void writeOSS (Object value, PrintWriter writer)
        {
            writer.print("[");
            boolean first = true;
            for (Object v : ((List<Object>)value)) {
                if (first) {
                    first = false;
                } else {
                    writer.print(", ");
                }
                write(v, writer);
            }
            writer.print("]");
        }
    }

    static class FromOverride extends OSSSerialize
    {
        public void writeOSS (Object value, PrintWriter writer)
        {
            write(((Meta.OverrideValue)value).value(), writer);
            writer.print("!");
        }
    }

    static class FromFieldPath extends OSSSerialize
    {
        public void writeOSS (Object value, PrintWriter writer)
        {
            writer.print("$");
            writer.print(((FieldPath)value).toString());
        }
    }

    static class RuleNode
    {
        Rule.Predicate _predicate;
        Map<String, Object> _properties;
        List<RuleNode> _children;

        RuleNode nodeForPredicate (Rule.Predicate pred)
        {
            if (Meta.isPropertyScopeKey(pred.getKey())) return this;

            if (_children == null) _children = ListUtil.list();
            for (RuleNode node : _children) {
                Rule.Predicate nodeP = node._predicate;
                if (nodeP.getKey().equals(pred.getKey()) && nodeP.getValue().equals(pred.getValue())) return node;
            }
            RuleNode node = new RuleNode();
            node._predicate = pred;
            _children.add(node);
            return node;
        }

        void addRule (Rule rule)
        {
            addRule(rule.getPredicates().listIterator(), rule);
        }

        void addRule (Iterator<Rule.Predicate> preds, Rule rule)
        {
            if (preds.hasNext()) {
                nodeForPredicate(preds.next()).addRule(preds, rule);
            } else {
                if (_properties == null) _properties = new HashMap();
                Rule.merge(UIMeta.getInstance(), rule.getProperties(), _properties, false, null);
            }
        }

        void write (PrintWriter writer, int level, boolean isRoot)
        {
            if (_predicate != null) {
                if (_predicate.getValue().equals(Meta.KeyAny) || _predicate.getValue().equals(true)) {
                    writer.printf("%s ", _predicate.getKey());
                } else {
                    writer.printf("%s=%s ", _predicate.getKey(), _predicate.getValue());
                }
            }

            boolean hasBlock = !isRoot && (((_children != null && _children.size() > 1) || _properties != null));
            if (hasBlock) {
                writer.println("{");
                level++;
            }
            if (_children != null) {
                for (RuleNode child : _children) {
                    if (hasBlock) indent(writer, level);
                    child.write(writer, level, false);
                }
            }
            if (_properties != null) {
                writeProperties (writer, _properties, level);
            }
            if (hasBlock) {
                level--;
                indent(writer, level);
                writer.println("}");
            }
        }

        void indent (PrintWriter writer, int level) {
            while (level-- > 0) {
                writer.print("    ");
            }
        }

        void writeProperties (PrintWriter writer, Map<String, Object> properties, int level) {
            for (Map.Entry<String,Object> e : properties.entrySet()) {
                indent(writer, level);
                writer.printf("%s:", e.getKey());
                OSSWriter.write(e.getValue(), writer);
                writer.print(";\n");
            }
        }
    }
}
