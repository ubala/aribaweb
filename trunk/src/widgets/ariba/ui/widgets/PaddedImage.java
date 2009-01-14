/*
    Copyright 1996-2008 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/PaddedImage.java#5 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.util.AWImageInfo;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;

public final class PaddedImage extends AWComponent
{
    /*-----------------------------------------------------------------------
        Constants
     -----------------------------------------------------------------------*/

    private static final String FileNameBinding = "filename";
    private static final String MinWidthBinding = "minWidth";
    private static final String MinHeightBinding = "minHeight";

    /*-----------------------------------------------------------------------
        Members
     -----------------------------------------------------------------------*/

    private String _filename;
    private String _style;

    /*-----------------------------------------------------------------------
        Init
     -----------------------------------------------------------------------*/

    public void sleep ()
    {
        super.sleep();
        _filename = null;
        _style = null;
    }

    /*-----------------------------------------------------------------------
        Bindings
     -----------------------------------------------------------------------*/

    public String filename ()
    {
        if (_filename == null) {
            _filename = stringValueForBinding(FileNameBinding);
        }
        return _filename;
    }


    public int minWidth ()
    {
        if (hasBinding(MinWidthBinding)) {
            return intValueForBinding(MinWidthBinding);
        }

        return -1;
    }

    public int minHeight ()
    {
        if (hasBinding(MinHeightBinding)) {
            return intValueForBinding(MinHeightBinding);
        }

        return -1;
    }

    public String style ()
    {
        if (_style == null) {
            AWImageInfo awImageInfo = resourceManager().imageInfoForName(filename());
            if (awImageInfo != null) {
                int height = awImageInfo.height;
                int width = awImageInfo.width;

                int minWidth = minWidth();
                int widthPadding = 0;
                if (minWidth > 0) {
                    if (width < minWidth) {
                        widthPadding = (minWidth - width)/2;
                    }
                }

                int minHeight = minHeight();
                int heightPadding = 0;
                if (minHeight > 0) {
                    if (height < minHeight) {
                        heightPadding = (minHeight - height)/2;
                    }
                }

                _style = Fmt.S("margin: %spx %spx %spx %spx",
                                Constants.getInteger(heightPadding),
                                Constants.getInteger(widthPadding),
                                Constants.getInteger(heightPadding),
                                Constants.getInteger(widthPadding));
            }
        }

        return _style;
    }
}
