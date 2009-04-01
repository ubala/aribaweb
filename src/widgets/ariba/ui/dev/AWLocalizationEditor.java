/*
    Copyright 1996-2009 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/widgets/ariba/ui/dev/AWLocalizationEditor.java#1 $
*/
package ariba.ui.dev;

import ariba.ui.aribaweb.core.AWConcreteServerApplication;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.table.AWTDisplayGroup;
import ariba.ui.widgets.MessageBanner;
import ariba.ui.widgets.Confirmation;
import ariba.util.core.URLUtil;
import ariba.util.core.StringUtil;
import ariba.util.core.IOUtil;
import ariba.util.core.Fmt;
import ariba.util.core.SystemUtil;
import ariba.util.core.StringCSVConsumer;
import ariba.util.core.ListUtil;
import ariba.util.core.Assert;
import ariba.util.io.CSVReader;
import ariba.util.io.CSVConsumer;
import ariba.util.i18n.MergedStringLocalizer;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.HttpURLConnection;

/**
    Simplistic localization tool
 */
public class AWLocalizationEditor extends AWComponent
{
    public StringManager _stringManager;
    TranslationService _translationService = new TranslationService();
    public String _currentLocale;
    public AWTDisplayGroup _localeDisplayGroup;
    public AWTDisplayGroup _fileDisplayGroup;
    public LString _currentString;
    public String baseLocale = "en_US";
    public File _selectedProjectDir;
    
    public boolean isStateless ()
    {
        return false;
    }

    public void init ()
    {
        super.init();

        resetStringManager();
        // default initial file selection (to app, if possible)
        _selectedProjectDir = _stringManager._projectDirs.get(0);
        String appName = AWUtil.lastComponent(application().adaptorUrl(), "/");
        for (File dir : _stringManager._projectDirs) {
            if (appName.equals(dir.getName())) _selectedProjectDir = dir;
        }
    }

    public ProjectFiles projectFiles ()
    {
        return _stringManager.projectFilesForProjectDir(_selectedProjectDir);
    }

    void resetStringManager ()
    {
        _stringManager = new StringManager();
    }

    public StringFile currentStringFile ()
    {
        return projectFiles().stringFileForFile((File)_fileDisplayGroup.selectedObject());
    }

    public void saveFileChanges ()
    {
        StringFile stringFile = currentStringFile();
        stringFile.save();
        MessageBanner.setMessage(Fmt.S("Saved %s", stringFile._file), session());
    }

    public String selectedLocale ()
    {
        return (String)_localeDisplayGroup.selectedObject();
    }

    public String selectedFileTitle ()
    {
        return "..." +
                ((File)_fileDisplayGroup.selectedObject()).getPath()
                    .substring(_selectedProjectDir.getParentFile().getPath().length());
    }
    
    public List<File> filesForProjectAndLocale (File projectDir, String locale)
    {
        return (projectDir == null || locale == null) ? null : projectFiles().filesForLocale.get(locale);
    }

    public int rowsForCurrentTrans ()
    {
        return (_currentString.trans != null) ? _currentString.trans.length() / 40 + 1 : 2;
    }

    public boolean antAvailable ()
    {
        return (_selectedProjectDir != null) && (new File(_selectedProjectDir, "build.xml")).exists();
    }

    public void antLocalize ()
    {
        antRun(_selectedProjectDir, "localize");
        resetStringManager();
        MessageBanner.setMessage("Done running `ant localize`.  See console log for more info...", session());
    }

    public void antLocalizeMeta ()
    {
        antRun(_selectedProjectDir, "localize-meta");
        MessageBanner.setMessage("Done running `ant localize-meta`.  See console log for more info...", session());
        resetStringManager();
    }

    public void showAddLocale ()
    {
        Confirmation.showConfirmation(requestContext(), AWEncodedString.sharedEncodedString("AddLocalePanel"));
    }

    public void antLocalizeLocales ()
    {
        if (errorManager().checkErrorsAndEnableDisplay()) {
            Confirmation.showConfirmation(requestContext(), AWEncodedString.sharedEncodedString("AddLocalePanel"));
        }
        antRun(_selectedProjectDir, "localize-locales");
        resetStringManager();
        MessageBanner.setMessage("Done running `ant localize-locales`.  See console log for more info...", session());
    }

    public void antAddLocale ()
    {
        if (errorManager().checkErrorsAndEnableDisplay()) {
            Confirmation.showConfirmation(requestContext(), AWEncodedString.sharedEncodedString("AddLocalePanel"));
        }
        String arg = "-Dlocale.list="+_currentLocale;
        antRun(_selectedProjectDir, arg, "localize-locales");
        resetStringManager();
        MessageBanner.setMessage(Fmt.S("Done running `ant %s localize-locales`.  See console log for more info...", arg), session());
    }

    public boolean translationSupported ()
    {
        return _translationService.translationSupported(baseLocale, selectedLocale());
    }

    public void autoTranslate ()
    {
        StringFile stringFile = currentStringFile();
        List<String> toTrans = new ArrayList();
        for (LString lstring : stringFile._strings) {
            if (lstring.needsTranslation()) toTrans.add(lstring.orig);
        }
        List<String> translations = _translationService.translateStrings(toTrans, baseLocale, selectedLocale());

        int i = 0;
        for (LString lstring : stringFile._strings) {
            if (lstring.needsTranslation()) lstring.trans = checkForBrokenTags(translations.get(i++), lstring.orig);
        }
    }

    static final Pattern _TagPattern = Pattern.compile("(\\{/?\\d\\})");
    static final Pattern _BrokenTagPattern = Pattern.compile("(\\{\\d)");

    String checkForBrokenTags (String trans, String orig)
    {
        // Sometimes translation can break {0} {0/} type tags.  If so, try to clean up (and flag error)
        Matcher m = _TagPattern.matcher(orig);
        boolean broken = false;
        while (m.find()) {
            String tag = m.group(1);
            if (!trans.contains(tag)) broken = true;
        }
        if (broken) {
            trans = trans.replaceAll("\\{/?\\d", "").replace("{", "").replace("}", "");
            trans = "**BROKEN ARG TAGS**: " + trans;
        }
        return trans;
    }

    public static class LString {
        public String file;
        public String key;
        public String orig;
        public String trans;
        public String comment;

        public LString (String file, String key, String orig, String trans, String comment)
        {
            this.file = file;
            this.key = key;
            this.orig = orig;
            this.trans = trans;
            this.comment = comment;
        }

        public boolean hasComment ()
        {
            return !StringUtil.nullOrEmptyOrBlankString(comment) && !comment.equals("@");
        }

        public boolean isStale ()
        {
            return comment != null && comment.endsWith("@");
        }

        public boolean needsTranslation ()
        {
            return StringUtil.nullOrEmptyOrBlankString(trans) || orig.equals(trans);
        }
    }

    public static class StringFile
    {
        public File _file;
        public List<LString> _strings;
        String _encoding;

        public StringFile (File file, List<LString> strings, String encoding)
        {
            _file = file;
            _strings = strings;
            _encoding = encoding;
        }

        String csvQuoted (String s)
        {
            s = s.replace("\n", "\\n");
            s = s.replace("\"", "\"\"");
            return "\"" + s +"\"";
        }

        public void save ()
        {
            String encoding = (_encoding != null) ? _encoding : "Cp1252";
            try {
                PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(_file), encoding));
                out.println(_encoding);
                for (LString lstring : _strings) {
                    out.print(csvQuoted(lstring.file)); out.print(",");
                    out.print(csvQuoted(lstring.key)); out.print(",");
                    out.print(csvQuoted(lstring.orig)); out.print(",");
                    out.print(csvQuoted(lstring.trans)); out.print(",");
                    out.print(csvQuoted(lstring.comment));
                    out.println();
                }
                out.close();
            } catch (UnsupportedEncodingException e) {
                throw new AWGenericException(e);
            } catch (FileNotFoundException e) {
                throw new AWGenericException(e);
            }
        }
    }

    public static class ProjectFiles
    {
        public File dir;
        public Set<String> locales = new HashSet();
        public Map<String, List<File>> filesForLocale = new HashMap();
        public Map<File, StringFile> stringsForFile = new HashMap();

        public ProjectFiles (File dir)
        {
            this.dir = dir;
        }

        void init(File resourceDir)
        {
            for (File localeDir : resourceDir.listFiles()) {
                File stringsDir = new File(localeDir, "strings");
                if (stringsDir.isDirectory()) {
                    for (File stringsFile : stringsDir.listFiles()) {
                        if (stringsFile.getName().endsWith(".csv")) {
                            registerStringsFile(dir, stringsFile, localeDir.getName());
                        }
                    }
                }
            }
        }

        void registerStringsFile (File projectDir, File stringsFile, String localeName)
        {
            // _allFileNames.add(AWUtil.pathToLastComponent(stringsFile.getName(), "."));
            locales.add(localeName);
            List<File> files = filesForLocale.get(localeName);
            if (files == null) {
                files = new ArrayList();
                filesForLocale.put(localeName, files);
            }
            files.add(stringsFile);
        }

        public StringFile stringFileForFile (File file)
        {
            StringFile stringFile = stringsForFile.get(file);
            if (stringFile == null) {
                URL url = URLUtil.url(file);
                CSVStringConsumer csvConsumer = new CSVStringConsumer(url, true);
                CSVReader reader = loadStringsFromCSV(url, csvConsumer);
                List<LString> strings = new ArrayList();
                for (List<String> line : csvConsumer.getLines()) {
                    while (line.size() < 5) line.add("");
                    strings.add(new LString(line.get(0),line.get(1),line.get(2),line.get(3),line.get(4)));
                }
                stringFile = new StringFile(file, strings,
                        (reader.isEncodingExplicitlySet()) ? reader.getEncoding() : null);
                stringsForFile.put(file, stringFile);
            }
            return stringFile;
        }
    }

    public static class StringManager
    {
        public List<File> _projectDirs = new ArrayList();
        Map<File, ProjectFiles> _projectFilesForProjectDir = new HashMap();

        public StringManager ()
        {
            init(AWConcreteServerApplication._debugSearchPaths());
        }

        List<String> ResourcePaths = AWUtil.list(
                "resource/en_US/strings",
                "ariba/resource/en_US/strings",
                "resource/ariba/resource/en_US/strings"
        );

        void init (List<String> projectPaths)
        {
            for (String projectPath : projectPaths) {
                File projDir = new File(projectPath);
                ProjectFiles proj = null;
                for (String resourcePath : ResourcePaths) {
                    File dir = new File(projectPath, resourcePath);
                    if (dir.isDirectory()) {
                        proj = new ProjectFiles(projDir);
                        proj.init(dir.getParentFile().getParentFile());
                    }
                }
                _projectDirs.add(projDir);
                _projectFilesForProjectDir.put(projDir, proj);
            }
        }

        ProjectFiles projectFilesForProjectDir (File projDir)
        {
            return _projectFilesForProjectDir.get(projDir);
        }
    }
    
    public static CSVReader loadStringsFromCSV (URL url, CSVConsumer consumer)
    {
        InputStream in = null;
        try {
            if (!URLUtil.maybeURLExists(url)) {
                return null;
            }

            in = url.openStream();
            CSVReader reader = new CSVReader(consumer);
            reader.readForSpecifiedEncoding(in, url.toString());
            return reader;
        }
        catch (IOException e) {
            throw new AWGenericException(e);
        }
        finally {
            IOUtil.close(in);
        }
    }

    // Cover service for invoking Babel Fish
    static class TranslationService
    {
        static final Set<String> SupportedTranslationsSet = new HashSet(AWUtil.list(
                "zh_en", "zh_zt", "zt_en", "zt_zh",
                "en_zh", "en_zt", "en_nl", "en_fr",
                "en_de", "en_el", "en_it", "en_ja",
                "en_ko", "en_pt", "en_ru", "en_es",
                "nl_en", "nl_fr",
                "fr_nl", "fr_en", "fr_de", "fr_el",
                "fr_it", "fr_pt", "fr_es",
                "de_en", "de_fr",
                "el_en", "el_fr",
                "it_en", "it_fr",
                "ja_en", "ko_en",
                "pt_en", "pt_fr",
                "ru_en",
                "es_en", "es_fr"
                ));
        static final String _LtEsc = "@";
        static final String _GtEsc = "^";
        static final String _SlashEsc = "~";
        static final String _StrSep = " --- ";
        static final String _StrSepRegEx = " --- ";
        static final int MaxTransSize = 150;

        List<String> translateStrings (List<String> strings, String sourceLocale, String destLocale)
        {
            String transToken = transToken(sourceLocale, destLocale);

            // Batch strings into 150 char groups, escaping html
            List<String>result = new ArrayList();
            StringBuffer buf = new StringBuffer();
            for (String string : strings) {
                if (string.length() > MaxTransSize) {
                    processBuf(buf, result, transToken);
                    result.add(string);
                }
                else {
                    if (buf.length() + string.length() > MaxTransSize) {
                        processBuf(buf, result, transToken);
                    }
                    if (buf.length() > 0) {
                        buf.append(_StrSep);
                    }
                    buf.append(_escapeForTrans(string));
                    // processBuf(buf, result, transToken);
                }
            }
            processBuf(buf, result, transToken);
            return result;
        }

        // Process one buffer full of delimited strings, adding (descaped) results to result list
        void processBuf (StringBuffer buf, List<String> result, String transToken)
        {
            if (buf.length() ==0) return;

            String trans = requestTranslation(buf.toString(), transToken);
            for (String str : trans.split(_StrSepRegEx)) {
                result.add(_deescapeTrans(str));
            }
            buf.setLength(0);
        }

        final static String TransServiceBaseUrl = "http://babelfish.yahoo.com/translate_txt?doit=done&ei=UTF-8&fr=bf-res&intl=1&tt=urltext";
        // &lp=en_es&trtext=Hello
        final static Pattern TranlationResponsePattern = Pattern.compile("<div id=\"result\">(?:<div .*?>)?(.+?)</div>");

        String shortLocale (String javaLocale)
        {
            if ("zh_CN".equals(javaLocale)) javaLocale = "zh";
            else if ("zh_TW".equals(javaLocale)) javaLocale = "zt";
            
            return javaLocale.replaceFirst("_.+$", "");
        }

        String transToken (String sourceLocale, String destLocale)
        {
            // ToDo: check against available trans, return null if not available
            return shortLocale(sourceLocale) + "_" + shortLocale(destLocale);
        }

        public boolean translationSupported (String sourceLocale, String destLocale)
        {
            return SupportedTranslationsSet.contains(transToken(sourceLocale, destLocale));
        }

        // Call to Bablefish for translation
        String requestTranslation (String str, String transToken)
        {
            String urlStr = AWUtil.urlAddingQueryValue(TransServiceBaseUrl, "lp", transToken);
            urlStr = AWUtil.urlAddingQueryValue(urlStr, "trtext", str);
            try {
                URL url = new URL(urlStr);

                String response = stringFromUrl(url);
                Matcher m = TranlationResponsePattern.matcher(response);
                if (m.find()) return m.group(1);
                return str;
            } catch (IOException e) {
                throw new AWGenericException(e);
            }
        }

        static String _escapeForTrans (String s)
        {
            return s.replace("<", _LtEsc).replace(">", _GtEsc).replace("/", _SlashEsc);
        }

        static String _deescapeTrans (String s)
        {
            return s.replace(_LtEsc, "<").replace(_GtEsc, ">").replace(_SlashEsc, "/");
        }
    }

    static String stringFromUrl (URL url)
    {

        try {
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.0; de-DE; rv:1.7.5) Gecko/20041122 Firefox/1.0");
            conn.setRequestProperty("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
            conn.setRequestProperty("Accept-Language", "en-us;q=0.8,en;q=0.5");
            conn.setRequestProperty("Accept-Encoding", "");
            conn.setRequestProperty("Accept-Charset", "utf-8;q=0.7,*;q=0.7");
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(url.getQuery());
            wr.flush();
            wr.close();
            return AWUtil.stringWithContentsOfInputStream(conn.getInputStream(), false, "UTF8");
            /*
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF8"));
            // return AWUtil.getString(in);
            String line;
            StringBuilder result = new StringBuilder();
            while ((line = in.readLine()) != null) {
                result.append(line);
                result.append("\n");
            }
            return result.toString();
            */
        } catch (IOException e) {
            throw new AWGenericException(e);
        }
    }

    private static void antRun (File projDir, String ...args)
    {
        String antExe = SystemUtil.isWin32() ? "ant.bat" : "ant";
        String antHome = AWUtil.getenv("ANT_HOME");
        if (!StringUtil.nullOrEmptyString(antHome)) antExe = (new File(new File(antHome, "bin"), antExe)).toString();

        List list = AWUtil.list (antExe,
                "-emacs", "-logger", "org.apache.tools.ant.NoBannerLogger",
                "-f", new File(projDir, "build.xml").toString());
        for (String arg : args) list.add(arg);

        String[] cmdArray = new String[list.size()];
        list.toArray(cmdArray);
        runCmd(cmdArray);
    }

    private static void runCmd (String[] cmdArray)
    {
        try {
            // String cmd = Fmt.S("sh internal/gnumake.sh \"%s\" %s",dir,fileName);
            // Process p = Runtime.getRuntime().exec(cmd);
            Process p = Runtime.getRuntime().exec(cmdArray);

                // getInputStream gives an Input stream connected to
                // the process p's standard output (and vice versa). We use
                // that to construct a BufferedReader so we can readLine() it.
            BufferedReader is =
                new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = null;
            while ((line = is.readLine()) != null) {
                // show the programmer what's going on on the console.
                System.out.println(line);
            }
        }
        catch (IOException ioe) {
            System.out.println(ioe.toString());
        }
        catch (Throwable t) {
            System.out.println("Exception thrown running build command: " + t.toString());
        }
    }

    public static class CSVStringConsumer implements StringCSVConsumer
    {
        private List _lines = ListUtil.list();
        private URL _url;
        private boolean _displayWarning;

        public CSVStringConsumer (URL url,
                                    boolean displayWarning)
        {
            _url = url;
            _displayWarning = displayWarning;
        }

        // Returns a hashtable that contains the localized strings for a component.
        public Map getStrings ()
        {
            Assert.that(false,"Should not be called!");
            return null;
        }

        public void consumeLineOfTokens (String filepath, int lineNumber, List line)
        {
            for (int index = line.size() - 1; index >= 0; index--) {
                String currentString = (String)line.get(index);
                String unescapedString = MergedStringLocalizer.unescapeCsvString(currentString);
                line.set(index, unescapedString);
            }
            _lines.add(line);
        }

        public List<List<String>> getLines ()
        {
            return _lines;
        }
    }
}
