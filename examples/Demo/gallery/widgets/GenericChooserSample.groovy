package gallery.widgets
import ariba.ui.aribaweb.core.*
import ariba.util.core.*

class GenericChooserSample extends AWComponent
{
    def regions = ["Americas", "Europe", "APAC"]
    def _region = "Europe"
    def statesForSelectedRegion
    def state
    
    // Called when value selected in chooser
    def setRegion (String region)  {
        _region = region;

        // generate state
        statesForSelectedRegion = (1..10).collect { "State of " + region + " " + it }

        // reset chooser
        state = null;
    }

    def region () { _region }
}
