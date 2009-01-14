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

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTBatchNavigationBar.java#16 $
*/
package ariba.ui.table;

import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.ui.validation.AWVFormatterFactory;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;

public final class AWTBatchNavigationBar extends AWComponent
{
    private static final String ShowTotalItemsBinding = "showTotalItems";
    private static final String CaptionFormatBinding = "captionFormat";
    private AWTDisplayGroup _displayGroup;
    public boolean _isLastBatchDisplayed;
    public int[] _batchLabels;
    public int _currentBatchLabel;
    public int _selectedBatchLabel;

    public boolean isStateless ()
    {
        // this is made stateful to support backtracking
        return false;
    }


    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        /*
        reset displaygroup . Subsequent get will fetch from binding
        so if top component change display group, the navigation bar
        is changed accordingly
        */
        _displayGroup = null;
        _selectedBatchLabel = displayGroup().currentBatchIndex();

        super.renderResponse(requestContext, component);
    }

    /* -------------
        Bindings
        ------------ */

    public AWTDisplayGroup displayGroup ()
    {
        if (_displayGroup == null) {
            _displayGroup = (AWTDisplayGroup)valueForBinding(BindingNames.displayGroup);
        }
        return _displayGroup;
    }

    /* -------------
        Awl Accessors
        ------------ */

    public int[] batchLabels () {

        int batchCount = displayGroup().batchCount();
        if ((_batchLabels == null) || (_batchLabels.length != batchCount)) {
            _batchLabels = new int[batchCount];

            for (int i=0; i<batchCount; i++) {
                _batchLabels[i] = i+1;
            }
        }
        return _batchLabels;
    }

    public boolean isFirstBatchDisplayed ()
    {
        // this computes all three flags -- if the .awl ever changes, this must be updated if necessary.
        boolean isFirstBatchDisplayed = false;
        _isLastBatchDisplayed = false;
        AWTDisplayGroup displayGroup = displayGroup();
        int currentBatchIndex = displayGroup.currentBatchIndex();
        if (currentBatchIndex == 1) {
            isFirstBatchDisplayed = true;
        }
        else if (currentBatchIndex == displayGroup.batchCount()) {
            _isLastBatchDisplayed = true;
        }

        return isFirstBatchDisplayed;
    }

    public String caption ()
    {
        String captionFormat = (String)valueForBinding(CaptionFormatBinding);
        if (captionFormat == null) {
            return null;
        }
        else {
            String formattedValue = AWVFormatterFactory.formattedValue(AWVFormatterFactory.IntegerFormatterKey,
                Constants.getInteger(displayGroup().allObjects().size()), session());
            String retString = Fmt.S(captionFormat, formattedValue);
            return retString;
        }
    }

    public String itemsString ()
    {
        return (displayGroup().allObjects().size() == 1) ? localizedJavaString(1, "item" /*  */) : localizedJavaString(2, "items" /*  */);

    }

    public boolean showNavigationBar ()
    {
        if (requestContext().isExportMode()) return false;
        boolean showTotalItems = booleanValueForBinding(ShowTotalItemsBinding);
        return (displayGroup().hasMultipleBatches() || showTotalItems)
                    && (displayGroup().useBatching() && !requestContext().isPrintMode());
    }

    /* -------------
        Actions
        ------------ */

    public AWComponent displayPreviousBatchClicked ()
    {
        AWTDisplayGroup displayGroup = displayGroup();
        recordCurrentBatchIndex(displayGroup);
        displayGroup.displayPreviousBatch();
        return null;
    }

    public AWComponent displayNextBatchClicked ()
    {
        AWTDisplayGroup displayGroup = displayGroup();
        recordCurrentBatchIndex(displayGroup);
        displayGroup.displayNextBatch();
        return null;
    }

    public AWComponent batchSelected ()
    {
        AWTDisplayGroup displayGroup = displayGroup();
        recordCurrentBatchIndex(displayGroup);
        displayGroup.setCurrentBatchIndex(_selectedBatchLabel);
        return null;
    }

    /*-----------------------------------------------------------------------
        Backtracking
      -----------------------------------------------------------------------*/

    private void recordCurrentBatchIndex (AWTDisplayGroup displayGroup)
    {
        int currentBatchIndex = displayGroup.currentBatchIndex();
        recordBacktrackState(currentBatchIndex);
    }

    public Object restoreFromBacktrackState (Object backtrackState)
    {
        AWTDisplayGroup displayGroup = displayGroup();
        int existingBatchIndex = displayGroup.currentBatchIndex();
        int backtrackBatchIndex = ((Integer)backtrackState).intValue();
        displayGroup.setCurrentBatchIndex(backtrackBatchIndex);
        return Constants.getInteger(existingBatchIndex);
    }
}
