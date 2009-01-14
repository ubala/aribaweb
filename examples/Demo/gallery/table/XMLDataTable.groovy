package gallery.table

import ariba.ui.aribaweb.core.AWComponent

class XMLDataTable extends AWComponent
{
    def po, item = null, address = null
    def pos = null;

    public boolean isStateless() { false }
    
    void init () {
        pos = ["POs/PO1.xml", "POs/PO2.xml", "POs/PO3.xml"].collect {
            URL url = ariba.ui.table.ResourceLocator.urlForRelativePath(it, this);
            new XmlSlurper().parseText(url.text)
        }
        po = pos[0]
    }
}
