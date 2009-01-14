package gallery.chart

import ariba.ui.aribaweb.core.AWComponent
import ariba.ui.table.AWTDisplayGroup

class ChartPivot extends AWComponent
{
    def chartTypes = ariba.ui.chart.ChartPivotData.ChartTypes
    def chosenType = chartTypes[0];
    def valueColumnName = "Revenue";
    AWTDisplayGroup displayGroup

    public boolean isStateless() { return false; }
}
