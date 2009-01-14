/* Client code for GMaps components */
ariba.GMaps = function() {
    // imports
    var Dom = ariba.Dom;
    var Util = ariba.Util;

    // Module statics
    var maps = {};
    var currentMapData;
    
    function notNull (arg) {
        return (arg && arg != "" && arg != "null");
    }

    function invoke (actionId /* key, value, key, value, ...*/ ) {
        var formElm;
        for (var i = 1; i < arguments.length; i += 2) {
            Dom.setElementValue(arguments[i], arguments[i+1]);
            formElm = Dom.getElementById(arguments[i]);
        }
        var formId = (formElm) ? Dom.lookupFormId(formElm) : null;
        ariba.Request.senderClicked(actionId, formId);
    }
    
    // map object classes
    var types = {
        AddressMarker : function () {
            var marker, map;

            return {
                // our args are what gets passed to registerObj, except with the first arg
                // swapped to be the mapDAta instance
                initialize : function (mapData, address, latitude, longitude, autoCenterMap, clickId, dragId, infoHtml) {
                    function addMarker (point)
                    {
                        if (autoCenterMap) map.panTo(point);
                        var opts = {};
                        if (notNull(dragId)) opts["draggable"] = true;
                        marker = new GMarker(point, opts);
                        map.addOverlay(marker);
                        if (notNull(clickId)) {
                            GEvent.addListener(marker,"click", function() {
                              ariba.Request.invoke(null, clickId);
                            });
                        }
                        if (notNull(dragId)) {
                            GEvent.addListener(marker,"dragend", function(latlng) {
                                var latlngIds = mapData.latlngIds();
                                invoke(dragId, latlngIds[0], latlng.lat(), latlngIds[1], latlng.lng());
                            });
                        }

                        if (infoHtml && infoHtml != "") {
                            map.openInfoWindowHtml(point, infoHtml);
                        }
                    }

                    map = mapData.map();
                    if (notNull(address)) {
                        var geocoder = new GClientGeocoder();
                        geocoder.getLatLng(
                          address,
                          function(point) {
                              if (point) {
                                  addMarker(point)
                              } else {
                                  alert(address + " not found");
                              }
                          });
                    } else {
                        addMarker(new GLatLng(latitude, longitude));
                    }
                },
                destroy : function () {
                    map.removeOverlay(marker);
                }
            }
        },
        Polyline : function () {
            var polyline, map;

            return {
                // our args are what gets passed to registerObj, except with the first arg
                // swapped to be the map instance
                initialize : function (mapData, points, color, size) {
                    map = mapData.map();
                    polyline = new GPolyline(points, color, size);
                    map.addOverlay(polyline);
                },
                destroy : function () {
                    map.removeOverlay(polyline);
                }
            }
        }
    }

    // Per Map instance
    function MapData (divId, latitude, longitude, zoomLevel,
                        clickId, latId, lngId) {
        var self = this;
        var map;
        var curObjs = {};  // contructor args string -> contruced Object
        var newObjs = {};  // contructor args string -> contructor args *array*

        var div = Dom.getElementById(divId);
        map = new GMap2(div);
        map.addControl(new GSmallMapControl());
        map.addControl(new GMapTypeControl());
        map.setCenter(new GLatLng(latitude, longitude), zoomLevel);

        if (notNull(clickId)) {
            GEvent.addListener(map,"click", function(overlay,latlng) {     
              if (latlng) {
                  invoke(clickId, latId, latlng.lat(), lngId, latlng.lng());
              }
            });
        }

        return {
            /*
                Being way to tricky for anyones good...
                We use the varags array of arguments to registerObj as the *key* into
                the curObjs and newObjs maps.  This way, if the args change (type, or args
                to that type's constructor) then we will consider the old definition as
                missing and will destroy and recreate.
             */
            registerObj : function (/* type, args */) {
                var a = Util.toArray(arguments);
                newObjs[a] = a;
            },

            processObjs : function () {
                // we diff newObjs and curObjs, creating or deleting as necessary
                for (var b in curObjs) {
                    if (!newObjs[b]) {  // gone --> delete
                        curObjs[b].destroy();
                        delete curObjs[b];
                    }
                }
                for (var a in newObjs) {
                    if (!curObjs[a]) { // new --> create
                        var args = Util.toArray(newObjs[a]); // copy
                        var type = args.shift()
                        args.unshift(this); // add mapData as arg
                        var obj = new types[type];
                        obj.initialize.apply(obj, args);
                        curObjs[a] = obj;
                    }
                }
                newObjs = {};  // reset
            },

            map : function () { return map; },

            latlngIds : function () { return [latId, lngId]; }
        };
    }

    // Module Methods
    return {
        create : function (divId, latitude, longitude, zoomLevel, clickId, latId, lngId) {
            currentMapData = maps[divId];
            if (!currentMapData || !Dom.getElementById(divId).getAttribute("awdidInit")) {
                Dom.getElementById(divId).setAttribute("awdidInit", true);
                maps[divId] = currentMapData = new MapData(divId, latitude, longitude,
                        zoomLevel, clickId, latId, lngId);
            }
            return currentMapData;
        },

        current : function () { return currentMapData; }
    }
}();

