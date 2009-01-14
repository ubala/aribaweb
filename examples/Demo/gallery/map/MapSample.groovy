package gallery.map

import ariba.ui.aribaweb.core.AWComponent

class MapSample extends AWComponent
{
    def displayGroup, item, message;

    public boolean isStateless() { return false; }

    void itemClicked () {
        message = "You clicked " + item.Company;
    }
}