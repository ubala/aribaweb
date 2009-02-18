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
import ariba.ui.meta.core.PropertyValue;
import ariba.ui.aribaweb.util.AWUtil;

import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
        _ClassExtensionRegistry.registerClassExtension(Number.class,
                                          new AsToString());
        _ClassExtensionRegistry.registerClassExtension(PropertyValue.Expr.class,
                                          new AsToString());
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
            writer.print("null");
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

    static class AsToString extends OSSSerialize
    {
        public void writeOSS (Object value, PrintWriter writer)
        {
            writer.print(value);
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

    static void indent (PrintWriter writer, int level)
    {
        while (level-- > 0) {
            writer.print("    ");
        }
    }

    public static void writeProperties (PrintWriter writer, Map<String, Object> properties, int level, boolean singleLine)
    {
        for (Map.Entry<String,Object> e : properties.entrySet()) {
            if (!singleLine) indent(writer, level);
            Object val = e.getValue();
            if (val == null) {
                writer.printf("%s:null%s", e.getKey(), singleLine ? "; " : ";\n");

            } else {
                OSSSerialize serializer = OSSSerialize.get(val);
                if (serializer.getClass() != OSSSerialize.class) {
                    writer.printf("%s:", e.getKey());
                    serializer.writeOSS(val, writer);
                    writer.print( singleLine ? "; " : ";\n");
                }
            }
        }
    }

    static class RuleNode
    {
        Rule.Selector _selector;
        Map<String, Object> _properties;
        List<RuleNode> _children;

        RuleNode nodeForSelector (Rule.Selector pred)
        {
            if (Meta.isPropertyScopeKey(pred.getKey())) return this;

            if (_children == null) _children = ListUtil.list();
            for (RuleNode node : _children) {
                Rule.Selector nodeP = node._selector;
                if (nodeP.getKey().equals(pred.getKey()) && nodeP.getValue().equals(pred.getValue())) return node;
            }
            RuleNode node = new RuleNode();
            node._selector = pred;
            _children.add(node);
            return node;
        }

        void addRule (Rule rule)
        {
            addRule(rule.getSelectors().listIterator(), rule);
        }

        void addRule (Iterator<Rule.Selector> preds, Rule rule)
        {
            if (preds.hasNext()) {
                nodeForSelector(preds.next()).addRule(preds, rule);
            } else {
                if (_properties == null) _properties = new HashMap();
                Rule.merge(UIMeta.getInstance(), rule.getProperties(), _properties, null, null);
            }
        }

        void write (PrintWriter writer, int level, boolean isRoot)
        {
            int childCount = (_children == null) ? 0 : _children.size();
            boolean hasBlock = !isRoot && ((childCount > 1)
                    || (_properties != null && (!shouldInlineProperties(_properties) || childCount > 0)));

            if (_selector != null) {
                writer.printf("%s", _selector.getKey());
                if (!_selector.getValue().equals(Meta.KeyAny) && !_selector.getValue().equals(true)) {
                    writer.print("=");
                    OSSWriter.write(_selector.getValue(), writer);
                }
                writer.print(" ");
            }

            if (hasBlock) {
                writer.println("{");
                level++;
            }
            if (_children != null) {
                List <RuleNode> children = _children;
                boolean shouldWriteSeparatorNewLine = false;
                if (children.size() > 1) {
                    List<RuleNode>predNodes = ListUtil.list();
                    List<RuleNode>strippedChildren = ListUtil.list();
                    factorPredecessorRules(_children, predNodes, strippedChildren);
                    if (predNodes.size() > 1) {
                        writePredecessorChain(writer, predNodes, level);
                        // write the stripped children
                        children = strippedChildren;
                        shouldWriteSeparatorNewLine = true;
                    }
                }
                for (RuleNode child : children) {
                    if (child._properties != null || child._children != null) {
                        if (shouldWriteSeparatorNewLine) {
                            writer.println();
                            shouldWriteSeparatorNewLine = false;                            
                        }
                        if (hasBlock) indent(writer, level);
                        child.write(writer, level, false);                        
                    }
                }
            }
            if (_properties != null) {
                if (!hasBlock) writer.print(" { ");
                writeProperties (writer, _properties, level, !hasBlock);
                if (!hasBlock) writer.println(" }");
            }
            if (hasBlock) {
                level--;
                indent(writer, level);
                writer.println("}");
            }
        }

        boolean shouldInlineProperties (Map<String, Object> properties) {
            int count = properties.size();
            if (count <= 1) return true;
            if (count == 2) {
                for (Object v : properties.values()) {
                    if (v instanceof Collection) return false;
                }
                return true;
            }
            return false;
        }

        void writePredecessorChain (PrintWriter writer, List<RuleNode>predNodes, int level)
        {
            Map<String, List<RuleNode>> predecessors = AWUtil.groupBy(predNodes,
                    new AWUtil.ValueMapper() {
                        public Object valueForObject(Object o) {
                            return ((RuleNode)o)._properties.get(UIMeta.KeyAfter);
                        }
                    });
            Map<String, List<RuleNode>> nodesByKey = AWUtil.groupBy(predNodes,
                    new AWUtil.ValueMapper() {
                        public Object valueForObject(Object o) {
                            return ((RuleNode)o)._selector.getValue();
                        }
                    });

            for (String key : ListUtil.collectionToList(predecessors.keySet())) {
                if (predecessors.containsKey(key)) {
                    List<RuleNode>newList = ListUtil.list();
                    collapseInto(predecessors, key, newList);
                    if (!newList.isEmpty()) predecessors.put(key, newList);
                }
            }

            List<String>predKeys = new ArrayList(predecessors.keySet());
            Collections.sort(predKeys, new Comparator() {
                public int compare (Object o1, Object o2)
                {
                    int o1Rank = rankForKey(o1);
                    int o2Rank = rankForKey(o2);
                    return (o1Rank < 99 || o2Rank < 99) ? (o1Rank - o2Rank) : ((Comparable)o1).compareTo(o2);
                }
            });

            for (String predKey : predKeys) {
                indent(writer, level);
                RuleNode pred = (RuleNode)nodesByKey.get(predKey);
                String predTrait = (pred != null) ? (String)pred._properties.get(Meta.KeyTrait) : null;
                writer.print(formatKeyAndTrait(predKey, predTrait));
                for (RuleNode node : predecessors.get(predKey)) {
                    writer.printf(" => %s", formatKeyAndTrait((String)node._selector.getValue(),
                            node._properties.get(Meta.KeyTrait)));
                }
                writer.println(";");
            }
        }

        static void factorPredecessorRules (List<RuleNode> children, List<RuleNode>predNodes,
                                            List<RuleNode>otherNodes)
        {
            for (RuleNode node : children) {
                if (node._properties != null && node._properties.containsKey(UIMeta.KeyAfter)) {
                    Map predProps = AWUtil.map(UIMeta.KeyAfter, node._properties.get(UIMeta.KeyAfter));
                    Map otherProps = MapUtil.cloneMap(node._properties);
                    otherProps.remove(UIMeta.KeyAfter);
                    Object trait = node._properties.get(Meta.KeyTrait);
                    if (trait != null) {
                        predProps.put(Meta.KeyTrait, trait);
                        otherProps.remove(Meta.KeyTrait);
                    }

                    RuleNode predNode = new RuleNode();
                    predNode._selector = node._selector;
                    predNode._properties = predProps;
                    predNodes.add(predNode);

                    RuleNode otherNode = new RuleNode();
                    otherNode._selector = node._selector;
                    otherNode._properties = (otherProps.size() > 0) ? otherProps : null;
                    otherNode._children = node._children;
                    otherNodes.add(otherNode);
                } else {
                    otherNodes.add(node);
                }
            }
        }

        static Map _PredKeyRank = AWUtil.map("zNone", 0, "zMain", 1, "zTop", 2,
                              "zLeft", 3, "zRight", 4, "zBottom", 5, "zDetail", 6);
        static int rankForKey (Object k)
        {
            Object r = _PredKeyRank.get(k);
            return (r == null) ? 1000 : ((Number)r).intValue();
        }

        void collapseInto (Map<String, List<RuleNode>> predecessors,  String key, List<RuleNode> result)
        {
            List<RuleNode> followers = predecessors.get(key);
            if (followers != null) {
                predecessors.remove(key);
                for (RuleNode node : followers) {
                    result.add(node);
                    String followerKey = (String)node._selector.getValue();
                    collapseInto(predecessors, followerKey, result);
                }
            }
        }
        static String formatKeyAndTrait (String key, Object traits)
        {
            if (traits == null) return key;
            String traitString = (traits instanceof String) ? (String)traits
                    : StringUtil.join((List)traits,",");
            return Fmt.S("%s#%s", key, traitString);
        }
    }
}
