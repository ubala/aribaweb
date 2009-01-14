package gallery.table

import ariba.ui.aribaweb.core.AWComponent
import ariba.ui.demoshell.XMLFactory

class XMLDataTable extends AWComponent
{
    def po, item = null, address = null
    def pos = null;

    public boolean isStateless() { false }
    
    void init () {
        pos = [XMLFactory.xmlNamed("POs/PO1.xml", this),
               XMLFactory.xmlNamed("POs/PO2.xml", this),
               XMLFactory.xmlNamed("POs/PO3.xml", this)]
        po = pos[0]
    }
}
