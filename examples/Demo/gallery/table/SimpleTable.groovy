package gallery.table

import ariba.ui.table.AWTDisplayGroup
import ariba.ui.aribaweb.core.AWComponent

class SimpleTable extends AWComponent
{
    AWTDisplayGroup displayGroup = null;

    public boolean isStateless() { false }

    def isBig () {
        return displayGroup.currentItem().Amount.intValue() > 1000000;
    }

    def hasValidationError () {
        String message = displayGroup.currentItem().ValidationErrorMessage
        return message && message.length() > 0
    }
}
