package gallery.table

import ariba.ui.aribaweb.core.*
import ariba.util.fieldvalue.FieldValue
import ariba.ui.table.AWTDisplayGroup

class MetaContentTable extends AWComponent
{
    public boolean isStateless() { false }

    AWTDisplayGroup displayGroup
    def data, layout
    def sourceURL

    def fetch () {
        def xml = ariba.ui.demoshell.XMLFactory.readDocumentFromUrlString(sourceURL);
        println "XML: ${xml}"
        data = FieldValue.getFieldValue(xml, "Data.children")
        layout = FieldValue.getFieldValue(xml, "Layout")
        return null
    }

    def calcURL () {
        def url = AWXDebugResourceActions.urlForResourceNamed(requestContext(), "gallery/table/RFXList1.xml");
        println "URL: ${url}"
        return url
    }
}
