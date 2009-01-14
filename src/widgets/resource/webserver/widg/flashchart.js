function awInsertFlashGraph (divId, dataId, flashURL, width, height)
{
    var objId = divId + "_cht";
    var chart = null;

    var strXML = ariba.Dom.getElementById(dataId).innerHTML.replace(/"/g, "'");
    strXML = strXML.replace(/<!--/g, "").replace(/-->/g, "").replace(/\n/g, "")

    // temporary: compatibility with old FC_MSScatter chart - force re-init if old chart
    if (!ariba.Dom.getElementById(objId) || (flashURL.indexOf("FCF_") == -1)) {
        var div = ariba.Dom.getElementById(divId);
        div.innerHTML = '<div id="' + objId + '"></div>';

        // For compatibility with old Scatter chart
        ariba.Dom.getElementById(objId).SetReturnValue = FC_Loaded;

        chart = new FusionCharts(flashURL, objId, width, height, "0", "1");
        chart.setDataXML(strXML);
        chart.addParam("WMode","Transparent");
        chart.render(objId);
    } else {
        updateChartXML(objId, strXML);
    }

}

// For compatibility with old Scatter chart
function FC_Loaded() { return null; }
