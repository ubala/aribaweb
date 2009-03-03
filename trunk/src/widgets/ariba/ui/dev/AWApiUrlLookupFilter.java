package ariba.ui.dev;

import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.widgets.HTMLActionFilter;
import ariba.util.core.MapUtil;
import ariba.util.core.Assert;

import java.io.File;
import java.io.FileFilter;
import java.util.Map;

public class AWApiUrlLookupFilter implements HTMLActionFilter.UrlFilter
{
    String _javaDocResourceUrl;
    File _rootDirectory;
    String _translationPrefix;
    Map<String, File> _classNameToFile;

    static AWApiUrlLookupFilter _DefaultInstance = null;

    public static AWApiUrlLookupFilter defaultInstance ()
    {
        if (_DefaultInstance == null) {
            // E.g. "http://aribaweb.org/api"
            String urlPrefix = (String)System.getProperties().get("AWAPI.URL");
            // E.g. "/roots-mainline/install-s4/internal/opensource/docs/api"
            String sourcePath = (String)System.getProperties().get("AWAPI.RootDirectory");
            Assert.that(urlPrefix != null, "Missing parameter: AWAPI.URL");
            Assert.that(sourcePath != null, "Missing parameter: AWAPI.RootDirectory");
            _DefaultInstance = new AWApiUrlLookupFilter(urlPrefix, new File(sourcePath), "/api/");
        }
        return _DefaultInstance;
    }

    public AWApiUrlLookupFilter (String javaDocResourceUrl, File rootDirectory, String translationPrefix)
    {
        _rootDirectory = rootDirectory;
        _translationPrefix = translationPrefix;
        _javaDocResourceUrl = javaDocResourceUrl;
        if (!_javaDocResourceUrl.endsWith("/")) _javaDocResourceUrl += "/";

        _classNameToFile = filesToPath(_rootDirectory);
    }

    public String replacementForUrl (String url)
    {
        if (!url.startsWith(_translationPrefix)) return null;
        String key = url.substring(_translationPrefix.length());
        return urlForName(key + ".html");
    }
        
    String urlForName (String name)
    {
        File file = _classNameToFile.get(name);
        if (file == null) return null;
        String relativePath = relativePath(_rootDirectory, file);
        return _javaDocResourceUrl.concat(relativePath);
    }

    static String relativePath (File parentDir, File file)
    {
        String parentPath = parentDir.getAbsolutePath().replace("\\", "/");
        String filePath = file.getAbsolutePath().replace("\\", "/");
        int offset = parentPath.length() + (parentPath.endsWith("/") ? 0 : 1);
        return filePath.substring(parentPath.length());
    }

    static Map<String, File> filesToPath (File rootDir) {
        final Map<String, File> result = MapUtil.map();
        AWUtil.eachFile(rootDir,
            new FileFilter(){
                public boolean accept(File file)
                {
                    return file.isDirectory() || file.getName().endsWith(".html");
                }
            },
            new AWUtil.FileProcessor () {
                public void process(File file)
                {
                    if (!file.isDirectory()) result.put(file.getName(), file);
                }
            });
        return result;
    }
}
