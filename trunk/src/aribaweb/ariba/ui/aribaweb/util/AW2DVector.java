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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AW2DVector.java#6 $
*/

package ariba.ui.aribaweb.util;
import ariba.util.core.ListUtil;

public final class AW2DVector extends AWBaseObject
{
    private static final int ReallocationCushion = 4;
    private Object[][] _slots = new Object[ReallocationCushion][];

    public void setElementAt (Object newElement, int rowNumber, int columnNumber)
    {
        Object[] targetRow = null;
        if (rowNumber >= _slots.length) {
            _slots = (Object[][])AWUtil.realloc(_slots, rowNumber + ReallocationCushion);
        }
        targetRow = _slots[rowNumber];
        if (targetRow == null) {
            targetRow = new Object[columnNumber + ReallocationCushion];
            _slots[rowNumber] = targetRow;
        }
        if (columnNumber >= targetRow.length) {
            targetRow = (Object[])AWUtil.realloc(targetRow, columnNumber + ReallocationCushion);
            _slots[rowNumber] = targetRow;            
        }
        targetRow[columnNumber] = newElement;
    }

    public Object elementAt (int rowNumber, int columnNumber)
    {
        Object targetElement = null;
        if (rowNumber < _slots.length) {
            Object[] targetRow = _slots[rowNumber];
            if ((targetRow != null) && (columnNumber < targetRow.length)) {
                targetElement = targetRow[columnNumber];
            }
        }
        return targetElement;
    }

    public Object clone ()
    {
        AW2DVector newVector = new AW2DVector();
        newVector._slots = (Object[][])_slots.clone();
        return newVector;
    }

    public String toString ()
    {
        Object[] array = _slots[0];
        return super.toString() +
            ((array != null) ? ListUtil.arrayToList(array).toString() : "null") +
            "...rest left out...";
    }
}
