/*
    Copyright (c) 1996-2001 Ariba, Inc.
    All rights reserved. Patents pending.

    Responsible: craigf
*/

/*
    Create a variable ariba for the ariba.* package so we can later
    access our classes like ariba.util.log.Log instead of
    Packages.ariba.util.log.Log
*/
var ariba = Packages.ariba;
var aw = Packages;  // top level AW components

/*
    Import some classes as variables so we can reference them by just
    the class name without the package such as Log versus
    ariba.util.log.Log
*/
var URL = java.net.URL;

var FieldValue = ariba.util.fieldvalue.FieldValue;
var CSVConsumer = ariba.util.io.CSVConsumer;
var CSVReader   = ariba.util.io.CSVReader;
var Fmt         = ariba.util.core.Fmt;
var Map   = java.util.Map;
var Util        = ariba.util.core.Util;
var List      = java.util.List;
var FirstTokenKeyCSVConsumer = ariba.util.io.FirstTokenKeyCSVConsumer;

var Log = ariba.ui.demoshell.Log;  // used Log.demoshell.debug("foo: %s", x);

// function for allocating a component from an html path or AWComponent name
function page (path) {
    var result = ariba.ui.demoshell.AWXHTMLComponentFactory.sharedInstance().createComponentForRelativePath(path, this.component);
    return result || this.component.pageWithName(path);
}

// FieldValue conveniences
function get (target, key) {
    return FieldValue.getFieldValue(target, key);
}

function set (target, key, val) {
    return FieldValue.setFieldValue(target, key, val);
}

