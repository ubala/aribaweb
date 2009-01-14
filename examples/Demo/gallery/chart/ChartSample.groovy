package gallery.chart

import ariba.ui.aribaweb.core.AWComponent

class ChartSample extends AWComponent
{
    def chartTypes = ariba.ui.chart.ChartData.ChartTypes;
    def chosenType = chartTypes[0];

    public boolean isStateless() { return false; }
    
    def itemSets = [
            [name: "Set1",
             items: [[name:"AA", val:10],
                    [name:"BB", val:13],
                    [name:"CC", val:3]]],
            [name: "Set2",
             items: [[name:"AA", val:4],
                    [name:"BB", val:11],
                    [name:"CC", val:15]]]];
    def chosenSet = itemSets[0];
}
