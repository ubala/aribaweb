package gallery.html;

import ariba.ui.aribaweb.core.AWComponent
import ariba.ui.aribaweb.core.AWResponseGenerating
import ariba.ui.aribaweb.core.AWActionCallback

class FileUploadPanel extends AWComponent
{
    AWActionCallback _callback
    public long maxsize
    public String fileName
    public def mimeType
    public byte[] bytes
    boolean fileSizeExceeded

    public void setup (AWActionCallback callback, long uploadMaxsize)
    {
        _callback = callback;
        maxsize = uploadMaxsize;
    }

    public boolean isClientPanel ()
    {
        return true;
    }

    AWResponseGenerating done ()
    {
        if (fileSizeExceeded) {
            recordValidationError("file", "Your upload exceeds the maximuma allowable size", null);
            errorManager().checkErrorsAndEnableDisplay();
            return null;
        }
        ariba.ui.widgets.ModalPageWrapper.prepareToExit(this)
        _callback.prepare(requestContext())
        return _callback.doneAction(this);
    }
}
