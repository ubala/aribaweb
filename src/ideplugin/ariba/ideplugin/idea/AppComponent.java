package ariba.ideplugin.idea;

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.impl.TemplateImpl;
import com.intellij.codeInsight.template.impl.TemplateSettings;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.codeInsight.template.impl.TemplateContext;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import java.io.InputStream;

public class AppComponent implements ApplicationComponent
{
    public AppComponent ()
    {
    }

    public void initComponent()
    {
        // auto-associate .awl files as xml if not otherwise mapped
        FileType awlType = FileTypeManager.getInstance().getFileTypeByExtension("awl");
        if (awlType.getName().endsWith("UnknownFileType") || awlType.getName().equals("UNKNOWN")) {
            FileType htmType = FileTypeManager.getInstance().getFileTypeByExtension("xml");
            FileTypeManager.getInstance().registerFileType(htmType, new String[] {"awl"});
        }

        // auto-associate .oss files as text if not otherwise mapped
        FileType ossType = FileTypeManager.getInstance().getFileTypeByExtension("oss");
        if (ossType.getName().endsWith("UnknownFileType") || ossType.getName().equals("UNKNOWN")) {
            FileType txtType = FileTypeManager.getInstance().getFileTypeByExtension("txt");
            FileTypeManager.getInstance().registerFileType(txtType, new String[] {"oss"});
        }

        // Load our bundled live templates
        InputStream is = getClass().getResourceAsStream("/ariba/ideplugin/idea/livetemplates.xml");
        if (is != null) {
            loadTemplates(is, "AribaWeb");
        }
    }

    public void disposeComponent()
    {

    }

    public String getComponentName() {
        return "AribaWebApplicationComponent";
    }

    public static final String TemplateGroupName = "AribaWeb";

    void loadTemplates (InputStream inputStream, final String templateName) {
        final SAXBuilder parser = new SAXBuilder();
        try {
            
            TemplateSettings templateSettings = TemplateSettings.getInstance();
            Document doc = parser.build(inputStream);
            Element root = doc.getRootElement();
            for (Object element : root.getChildren()) {
                if (element instanceof Element) {
                    final Template template = readExternal((Element) element, templateName);
                    final String key = template.getKey();                  
                    if (key != null) {
                        TemplateImpl existingTemplate =
                            templateSettings.getTemplate(key, TemplateGroupName);
                        if (existingTemplate == null) {
                            templateSettings.addTemplate(template);
                        }
                        else if (TemplateGroupName.equals(existingTemplate.getGroupName())) {
                            // Update only add if template is in the AribaWeb group
                            templateSettings.removeTemplate(existingTemplate);
                            templateSettings.addTemplate(template);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected Template readExternal (Element element, final String templateName) {
        TemplateImpl template = new TemplateImpl(element.getAttributeValue("name"),
                                                 element.getAttributeValue("value"),
                                                 templateName);
        template.setDescription(element.getAttributeValue("description"));
        template.setToReformat(Boolean.valueOf(element.getAttributeValue("toReformat")));
        template.setToShortenLongNames(Boolean.valueOf(element.getAttributeValue("toShortenFQNames")));
        TemplateContext context = template.getTemplateContext();
        for (Object o : element.getChildren("variable")) {
            Element e = (Element) o;
            template.addVariable(e.getAttributeValue("name"),
                                 e.getAttributeValue("expression"),
                                 e.getAttributeValue("defaultValue"),
                                 Boolean.valueOf(e.getAttributeValue("alwaysStopAt")));
        }
        Element contextElement = element.getChild("context");
        if (contextElement != null) {
            try {
                context.readExternal(contextElement);
            } catch (InvalidDataException e) {
                e.printStackTrace();
            }
        }
        return template;
    }
}
