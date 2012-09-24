package search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;

import java.io.Reader;
import java.io.IOException;

/** An Analyzer that filters tokenizes sequences matching Character.isJavaIdentifierPart() and then toLowers them. */
public final class SourceCodeAnalyzer extends Analyzer
{

    private static final String[] STOP_WORDS = {
      // Java
        "public","private","protected","interface",
        "abstract","implements","extends","null", "new",
        "switch","case", "default" ,"synchronized" ,
        "do", "if", "else", "break","continue","this",
        "assert" ,"for","instanceof", "transient",
        "final", "static" ,"void","catch","try",
        "throws","throw","class", "finally","return",
        "const" , "native", "super","while", "import",
        "package" ,"true", "false",
      // English
        "a", "an", "and", "are","as","at","be", "but",
        "by", "for", "if", "in", "into", "is", "it",
        "no", "not", "of", "on", "or", "s", "such",
        "that", "the", "their", "then", "there","these",
        "they", "this", "to", "was", "will", "with"
    };

    static PerFieldAnalyzerWrapper analyzerForField (String fieldName, Analyzer defaultAnalyzer)
    {
        if (defaultAnalyzer == null) defaultAnalyzer = new StandardAnalyzer(Version.LUCENE_36);
        PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(defaultAnalyzer);
        analyzer.addAnalyzer(fieldName, new SourceCodeAnalyzer());
        return analyzer;
    }


    public TokenStream tokenStream (String fieldName, Reader reader)
    {
        // return new StopFilter(new CodeTokenizer(reader), STOP_WORDS);
        return new CodeTokenizer(reader);
    }

    static class CodeTokenizer extends CharTokenizer
    {
        public CodeTokenizer (Reader in)
        {
            super(in);
        }

        protected boolean isTokenChar (char c)
        {
            return Character.isJavaIdentifierPart(c);
        }

        protected char normalize (char c)
        {
            return Character.toLowerCase(c);
        }
    }
}
