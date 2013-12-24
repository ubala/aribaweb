/*
    Calendar.js     -- calendar control and date utilities

    (see ariba.ui.widgets.Calendar.awl)        
*/

ariba.Calendar = function() {
    // imports
    var Util = ariba.Util;
    var Menu = ariba.Menu;
    var Handlers = ariba.Handlers;
    var Event = ariba.Event;
    var Request = ariba.Request;
    var Dom = ariba.Dom;


    // private vars
    /**
        Defined by (localized) dateformat.js
        Temporary checks until these are localized.
     */
    if (typeof AWPreviousYearTitle == "undefined") {
        AWPreviousYearTitle = 'Need to localize AWPreviousYearTitle in dateformat.js under ariba/resource/en_US/widg/';
    }
    if (typeof AWPreviousMonthTitle == "undefined") {
        AWPreviousMonthTitle = 'Need to localize AWPreviousMonthTitle in dateformat.js under ariba/resource/en_US/widg/';
    }
    if (typeof AWNextMonthTitle == "undefined") {
        AWNextMonthTitle = 'Need to localize AWNextMonthTitle in dateformat.js under ariba/resource/en_US/widg/';
    }
    if (typeof AWNextYearTitle == "undefined") {
        AWNextYearTitle = 'Need to localize AWNextYearTitle in dateformat.js under ariba/resource/en_US/widg/';
    }

    var AWTodayStyle = "today";
    var AWSelectedDayStyle = "selectedDay";
    var AWFocusStyle = "calendar_focus";
    var AWDisabledStyle = "calendar_disabled";
    var AWPreviousYearClass = "calendarPreviousYear";
    var AWPreviousMonthClass = "calendarPreviousMonth";
    var AWNextMonthClass = "calendarNextMonth";
    var AWNextYearClass = "calendarNextYear";

    
    // Must keep these strings in sync with ariba.ui.widgets.Calendar.java
    var AWCalendarYearKey = "awcaly";
    var AWCalendarMonthKey = "awcalm";
    var AWCalendarDateKey = "awcald";

    
    /////////////////////////////
    // Month / Year Label Format
    /////////////////////////////
    var AWShortMonthPattern = /\{AWShortMonth\}/;
    var AWMonthPattern = /\{AWMonth\}/;
    var AWShortYearPattern = /\{AWShortYear\}/;
    var AWYearPattern = /\{AWYear\}/;

    
    ////////////////
    // Date Format
    ////////////////
    // Note: 'yMEdhHKkmsSazFDwG' is list of all reserved chars supported by the Date parsing system.
    // See docs for java.text.SimpleDateFormat for more
    var SpecialDateChars = 'yMEdhHKkmsSazFDwG';
    var NumeralSet = '0123456789';


    var Calendar = {

        Control : function (containerId, selectedDate, enabledDays, reuse, dateTextFieldId)
        {
            this.getTable = function ()
            {
                if(!this._reuse) {
                    return Dom.getElementById(this._containerId);
                }
                else if(!this._calTable) {
                    var menu = Dom.getElementById("calendar_menu");
                    this._calTable = Dom.findChild(menu,"TABLE",false);
                }
                return this._calTable;
            }

            this._reuse = reuse;
            this._dateTextFieldId = dateTextFieldId;
            this._containerId = containerId;
            if(reuse){
                var link = Dom.getElementById(this._containerId);
                link._awcalendar = this;
            }
            else {
                this.getTable()._awcalendar = this;
            }

            this._calendarDate = null;
            this._selectedDate = selectedDate;

            this.prepCalendar = function (cdata)
            {
                this._enabledDays = [true, true, true, true, true, true, true];
                if(cdata.length == 0) {
                    return;
                }
                var darr = cdata.split(",");
                for(i = 0; i < darr.length; i++) {
                    if(darr[i] == "-*") {
                        for(j = 0; j < 7; j++) {
                            this._enabledDays[j] = false;
                        }
                        continue;
                    }
                    var d = Math.abs(darr[i]);
                    if(d > 0 && d < 8) {
                        this._enabledDays[d - 1] = darr[i] > 0;
                    }
                }
            }

            this.prepCalendar(enabledDays);

            this.setCalendarDate = function (date)
            {
                var year = date.getFullYear();
                var month = date.getMonth();
                // always set to last day of month so
                // we can easily get days in month
                this._calendarDate = new Date(year, (month + 1), 0);
            }

            this.showDay = function (dayOfWeek){
                return this._enabledDays[dayOfWeek];
            }

            /////////////
            // Rendering
            /////////////

            this.renderMonthYear = function ()
            {
                var tbody = this.getTable().tBodies[0];
                var row = tbody.rows[0];
                var monthYearLabel = row.cells[2];
                Dom.setInnerText(monthYearLabel, Calendar.formatMonthYear(AWCalendarLabelPattern, this._calendarDate));
                this.renderNavigationTitle(row, AWPreviousYearClass, AWPreviousYearTitle);
                this.renderNavigationTitle(row, AWPreviousMonthClass, AWPreviousMonthTitle);
                this.renderNavigationTitle(row, AWNextMonthClass, AWNextMonthTitle);
                this.renderNavigationTitle(row, AWNextYearClass, AWNextYearTitle);
            }

            this.renderNavigationTitle = function (row, imageClass, imageTitle)
            {
                var navigationImage = Dom.findChildUsingPredicate(row, function (e) {
                    return e.tagName == "IMG" && e.className == imageClass;
                });
                if (navigationImage) {
                    navigationImage.alt = imageTitle;
                    navigationImage.title = imageTitle;
                }
            }

            this.renderDayNames = function () {
                var tbody = this.getTable().tBodies[1];
                var rows = tbody.rows;
                var cells = Dom.getRowCells(rows[0]);
                for (var i = 0; i < cells.length; i++) {
                    var index = i + AWDayOfWeekStart;
                    if (index > 6) {
                        index -= 7;
                    }
                    cells[i].innerHTML = AWShortWeekdayNames[index];
                }
            }

            this.renderDays = function ()
            {
                var selectedDay = this._selectedDate.getDate();
                var selectedMonth = this._selectedDate.getMonth();
                var selectedYear = this._selectedDate.getFullYear();

                var calendarMonth = this._calendarDate.getMonth();
                var calendarYear = this._calendarDate.getFullYear();

                var today = new Date();
                var todaysYear = today.getFullYear();
                var todaysMonth = today.getMonth();
                var todaysDate = today.getDate();

                var daysInMonth = this._calendarDate.getDate();
                var lastDayOfMonth = this._calendarDate.getDay();
                var firstDayOfMonth = lastDayOfMonth - (daysInMonth % 7) + 1 - AWDayOfWeekStart;
                if (0 > firstDayOfMonth) {
                    firstDayOfMonth += 7;
                }
                var currentDay = 1 - firstDayOfMonth;
                var tbody = this.getTable().tBodies[2];
                var rows = tbody.rows;
                for (var rowIndex = 0; 6 > rowIndex; rowIndex++) {
                    var row = rows[rowIndex];
                    var cells = row.cells;
                    for (var cellIndex = 0; 7 > cellIndex; cellIndex++) {
                        var cell = cells[cellIndex];
                        Dom.setInnerText(cell, "");
                        if (currentDay > 0 && currentDay <= daysInMonth) {
                            if(this.showDay(cellIndex)){
                                var anchor = document.createElement("A");
                                anchor.href = "#";
                                cell.appendChild(anchor);
                                Dom.setInnerText(anchor, currentDay);
                            }
                            else {
                                Dom.setInnerText(cell, currentDay);
                            }
                        }
                        if (currentDay == todaysDate && calendarMonth == todaysMonth && calendarYear == todaysYear) {
                            Dom.addClass(cell, AWTodayStyle);
                        }
                        else {
                            Dom.removeClass(cell, AWTodayStyle);
                            Dom.removeClass(cell, AWTodayStyle);
                        }
                        if (!this.showDay(cellIndex)) {
                            Dom.addClass(cell, AWDisabledStyle);
                        }
                        else {
                            Dom.removeClass(cell, AWDisabledStyle);
                            Dom.removeClass(cell, AWDisabledStyle);
                        }
                        if ((selectedDay == currentDay) && (selectedMonth == calendarMonth) && (selectedYear == calendarYear)) {
                            Dom.addClass(cell, AWSelectedDayStyle);
                        }
                        else {
                            // must call "remove" because we reuse the cells once rendered
                            Dom.removeClass(cell, AWSelectedDayStyle); 
                            // This second remove was added in 1-B8NJ91/1845273 because 
                            // it was considered to risky to change removeClass to remove all class strings.
                            // There is likely a defect in Dom.addClass that allows for multiple class strings to be added. 
                            Dom.removeClass(cell, AWSelectedDayStyle);
                            Dom.removeClass(cell, AWFocusStyle);
                            Dom.removeClass(cell, AWFocusStyle);
                        }
                        currentDay += 1;
                    }
                }
            }

            this.renderCalendar = function (date)
            {
                this.setCalendarDate(date);
                this.getTable()._awcalendar = this;
                this.renderMonthYear();
                this.renderDayNames();
                this.renderDays();
            }

            ///////////////////
            // Event Handling
            ///////////////////
            this.incrementMonth = function (increment)
            {
                var date = this._calendarDate;
                date = new Date(date.getFullYear(), date.getMonth() + increment, 1);
                this.renderCalendar(date);
            }

            /////////////
            // Init
            //////////////
            if(!this._reuse) {
                this.renderCalendar(this._selectedDate);
            }
        },

        calMouseOver : function (mevent)
        {
            mevent = mevent ? mevent : event;
            var srcElement = Event.eventSourceElement(mevent);
            if (srcElement.tagName == "A" && Dom.getInnerText(srcElement) != "") {
                var cell = Dom.findParent(srcElement, "TD", true);
                Dom.addClass(cell, AWFocusStyle);
            }
        },

        calMouseOut : function (mevent)
        {
            mevent = mevent ? mevent : event;
            var srcElement = Event.eventSourceElement(mevent);
            if (srcElement.tagName == "A") {
                var cell = Dom.findParent(srcElement, "TD", true);
                Dom.removeClass(cell, AWFocusStyle);
            }
        },

        calMouseDown : function (mevent)
        {
            mevent = mevent ? mevent : event;
            var srcElement = Event.eventSourceElement(mevent);
            var srcElementInnerText = Dom.getInnerText(srcElement);
            if (srcElement.tagName == "A" && srcElementInnerText != "") {
                var table = Dom.findParent(srcElement, "TABLE", true);
                var calendar = table._awcalendar;
                var date = calendar._calendarDate;
                var senderId = table.id;
                var formObj = table;
                if(table._awcalendar._reuse) {
                    senderId = table._awcalendar._containerId;
                    formObj = Dom.getElementById(senderId);
                }
                var formId = Dom.lookupFormId(formObj);
                var formObject = Dom.getElementById(formId);
                if (formObject != null) {
                    var yearFormField = Dom.addFormField(formObject, AWCalendarYearKey, date.getFullYear());
                    Dom.addFormField(formObject, AWCalendarMonthKey, date.getMonth());
                    Dom.addFormField(formObject, AWCalendarDateKey, srcElementInnerText);

                    Request.submitFormForElementName(formId, senderId);
                    // must remove this form field since its the one used
                    // to detect if the submission was from the this.Calendar, vs
                    // the textfield (in DateField).  If refresh regions are
                    // in use, the same form is used over and over, and we
                    // are not getting cleaned up automatically by a new page.
                    if (Dom.IsIE) {
                        // Note: in IE, we get an exception when using formObject.removeChild(...)
                        // on Buyer's Payments page.  It is beleived this is due to multiple forms
                        // on that page, but never proven.  We use removeNode to avoid the issue.
                        yearFormField.removeNode();
                    }
                    else {
                        formObject.removeChild(yearFormField);
                    }
                }
                else {
                    var urlString = Request.formatUrl(senderId);
                    urlString = urlString +
                                "&" + AWCalendarYearKey + "=" + date.getFullYear() +
                                "&" + AWCalendarMonthKey + "=" + date.getMonth() +
                                "&" + AWCalendarDateKey + "=" + srcElementInnerText;
                    Request.setDocumentLocation(urlString, null);
                }
            }
        },

        CalendarForElement : function (element)
        {
            var table = Dom.findParent(element, "TABLE", true);
            return table._awcalendar;
        },

        /////////////////////////////////
        // Prev/Next Month/Year Handling
        /////////////////////////////////

        previousMonthClicked : function (mevent, element)
        {
            var calendar = this.CalendarForElement(element);
            calendar.incrementMonth(-1);
            mevent = mevent ? mevent : event;
            Event.cancelBubble(mevent);
        },

        nextMonthClicked : function (mevent, element)
        {
            var calendar = this.CalendarForElement(element);
            calendar.incrementMonth(1);
            mevent = mevent ? mevent : event;
            Event.cancelBubble(mevent);
        },

        previousYearClicked : function (mevent, element)
        {
            var calendar = this.CalendarForElement(element);
            calendar.incrementMonth(-12);
            mevent = mevent ? mevent : event;
            Event.cancelBubble(mevent);
        },

        nextYearClicked : function (mevent, element)
        {
            var calendar = this.CalendarForElement(element);
            calendar.incrementMonth(12);
            mevent = mevent ? mevent : event;
            Event.cancelBubble(mevent);
        },

        /////////////////////
        // DateField Support
        /////////////////////
        /*
            Note: DateField has a special case PopupMenu in it and, as such, cannot use the standard
            PopupMenuLink.  The issue is that we need to be able to detect a change to the TextField
            within the DateField component.  This detection is handled by the functions in this section
            (namely this.dateFieldOnClick(..) and this.dateTextChanged(..)).  In addition, we need to control
            the visibility of the Calendar's PopupMenu upon return from the server -- that is we need
            to indicate that the Calendar should be visible when the page is returned.  So we simulate
            a link click via the this.showCalendar(..) function.  This not only displays the Calendar's
            popupmenu, but positions it accordingly.
        */

        dateFieldOnClick : function (linkObj, mevent)
        {
            var nobrObj = Dom.findParent(linkObj, "NOBR", false);
            var textfieldObj = Dom.findChild(nobrObj, "INPUT", false);
            var dateFieldChanged = textfieldObj.getAttribute("awdidChange");
            var menuId = linkObj.getAttribute("awmenuId");
            var menu = Dom.getElementById(menuId);

            if (dateFieldChanged == "1") {
                var formId = textfieldObj.form.id;
                var textFieldName = textfieldObj.name;
                Handlers.textFieldRefresh(formId, textFieldName);
            } else {
                if (linkObj._awcalendar) {
                    this.showCalendar(linkObj.id);
                }
            }
            return false;
        },

        timeFieldOnClick : function (linkObj, mevent)
        {
            var nobrObj = Dom.findParent(linkObj, "NOBR", false);
            var textfieldObj = Dom.findChild(nobrObj, "INPUT", false);
            var textFieldChanged = textfieldObj.getAttribute("awdidChange");
            var menuId = linkObj.getAttribute("awmenuId");
            
            Menu.menuLinkOnClick(linkObj, menuId, null, mevent);
            
            if (textFieldChanged == "1") {
                var formId = textfieldObj.form.id;
                var textFieldName = textfieldObj.name;
                Handlers.textFieldRefresh(formId, textFieldName);
            }
            return false;
        },
        
        dateTextChanged : function (textfieldObj)
        {
            textfieldObj.setAttribute("awdidChange", "1");
        },

        timeTextChanged : function (textfieldObj)
        {
            textfieldObj.setAttribute("awdidChange", "1");
        },

        /*
            In some date fields, if the text field value is invalid, it prevents the text field value
            from being updating when using the calendar popup until a valid text field value is entered.
            The fix is to override the default calendar body behavior for date field to clear out
            the text input field before submitting.
        */
        dateFieldMouseDown : function (evt)
        {
            // Set this flag, to be (eventually) passed on to 'menuOnMouseDown' in Menu.js.
            // That reads this flag on event and calls hideActiveMenu api that un-displays
            // the div containing the Calendar pop-up.
            evt.hideActiveMenu = true;
            var srcElement = Event.eventSourceElement(evt);
            var srcElementInnerText = Dom.getInnerText(srcElement);
            var table = Dom.findParent(srcElement, "TABLE", false);
            var textfieldObj = Dom.getElementById( table._awcalendar._dateTextFieldId);
            if (srcElement.tagName == "A" && srcElementInnerText != "") {
                // clear out the date field text in case it is an invalid value
                textfieldObj.value = '';
            }
            return this.calMouseDown(evt);
        },

        showCalendar : function (linkId)
        {
            var linkObj = Dom.getElementById(linkId);
            var menuId = linkObj.getAttribute("awmenuId");
            Menu.hideActiveMenu();
            if (linkObj._awcalendar) {
                linkObj._awcalendar.renderCalendar(linkObj._awcalendar._selectedDate);
                Menu.menuLinkOnClick(linkObj, menuId, null, null);
            }
        },

        onTimeChange : function (selectObj, mevent)
        {
            if (Menu.AWActiveMenu == null)
                return;

            var menuid = Menu.AWActiveMenu.id;
            Menu.hideActiveMenu();
            var senderId = selectObj.name;
            var formId = Dom.lookupFormId(selectObj);
            var formObject = Dom.getElementById(formId);
            if (formObject != null) {
                var field = Dom.addFormField(formObject, "awtimeChooserSelected", menuid);
                Request.submitFormForElementName(formId, senderId);

                // must remove this form field since its the one used
                // to detect if the submission was from the this.Calendar, vs
                // the textfield (in DateField).  If refresh regions are
                // in use, the same form is used over and over, and we
                // are not getting cleaned up automatically by a new page.
                if (Dom.IsIE) {
                    // Note: in IE, we get an exception when using formObject.removeChild(...)
                    // on Buyer's Payments page.  It is beleived this is due to multiple forms
                    // on that page, but never proven.  We use removeNode to avoid the issue.
                    field.removeNode();
                }
                else {
                    formObject.removeChild(field);
                }

            }
        },

        ////////////////////
        // Date format util
        ////////////////////

        computeDaysInMonth : function (date)
        {
            var temp = new Date(date.getFullYear(), date.getMonth(), date.getDate());
            temp.setDate(32);
            return 32 - temp.getDate();
        },

        getMonthName : function (date)
        {
            var month = date.getMonth();
            return AWMonthNames[month];
        },

        formatMonthYear : function (monthYearLabelPattern, date)
        {
            var month = date.getMonth();
            var fullYear = new String(date.getFullYear());

            var monthYearLabel =
                    monthYearLabelPattern.replace(AWShortMonthPattern, AWShortMonthNames[month]);
            monthYearLabel = monthYearLabel.replace(AWMonthPattern, AWMonthNames[month]);
            monthYearLabel = monthYearLabel.replace(AWShortYearPattern, fullYear.substr(2));
            monthYearLabel = monthYearLabel.replace(AWYearPattern, fullYear);
            return monthYearLabel;
        },

        parseDateWithPattern : function (dateString, pattern)
        {
            var patternArray = this.parseDateFormatPattern(pattern);
            this.parseDateWithPatternArray(dateString, patternArray);
        },

        parseDateWithPatternArray : function (dateString, patternArray)
        {
            if (dateString.length == 0) {
                return new Date();
            }
            var day = -1;
            var month = -1;
            var year = -1;
            var dateStringIndex = 0;

            for (var patternIndex = 0; patternIndex < patternArray.length; patternIndex++) {
                var pattern = patternArray[patternIndex];
                var patternLength = pattern.length;
                var patternCh = pattern.charAt(0);
                if (SpecialDateChars.indexOf(patternCh) == -1) {
                    // skip to location of current interstitial pattern (note: may already be there)
                    // this serves to ignore unsupported special chars (if present) and re-sync the dateStringIndex.
                    dateStringIndex = dateString.indexOf(pattern, dateStringIndex);
                    // scann off interstitial chars
                    dateStringIndex += patternLength;
                }
                else {
                    // in here, scanning special chars
                    if (patternCh == 'y') {
                        var yearString = dateString.substr(dateStringIndex, patternLength);
                        year = Util.parseInt(yearString);
                        if (patternLength == 2) {
                            // todo:  need to check this assumption.  Do we need to sometimes add 1900?
                            year += 2000;
                        }
                        dateStringIndex += patternLength;
                    }
                    else if (patternCh == 'M') {
                        if (patternLength == 3) {
                            var monthName = dateString.substr(dateStringIndex, patternLength);
                            month = this.lookupShortMonthNameIndex(monthName);
                            dateStringIndex += patternLength;
                        }
                            // Note: too much code duplication with next section -- factor better
                        else if (patternLength == 2) {
                            var monthNumber = dateString.substr(dateStringIndex, patternLength);
                            month = Util.parseInt(monthNumber);
                            dateStringIndex += patternLength;
                        }
                        else if (patternLength == 1) {
                            var nonnumeralIndex = Util.indexOfCharNotInSet(dateString, dateStringIndex + 1, NumeralSet);
                            if (nonnumeralIndex == -1) {
                                nonnumeralIndex = dateString.length;
                            }
                            if ((nonnumeralIndex - dateStringIndex) > 2) {
                                nonnumeralIndex = dateStringIndex + 2;
                            }
                            var monthNumber = dateString.substring(dateStringIndex, nonnumeralIndex);
                            month = Util.parseInt(monthNumber);
                            dateStringIndex += monthNumber.length;
                        }
                    }
                    else if (patternCh == 'd') {
                        if (patternLength == 2) {
                            var dayNumber = dateString.substr(dateStringIndex, patternLength);
                            day = Util.parseInt(dayNumber);
                            dateStringIndex += patternLength;
                        }
                        else if (patternLength == 1) {
                            var nonnumeralIndex = Util.indexOfCharNotInSet(dateString, dateStringIndex + 1, NumeralSet);
                            if (nonnumeralIndex == -1) {
                                nonnumeralIndex = dateString.length;
                            }
                            if ((nonnumeralIndex - dateStringIndex) > 2) {
                                nonnumeralIndex = dateStringIndex + 2;
                            }
                            var dayNumber = dateString.substring(dateStringIndex, nonnumeralIndex);
                            day = Util.parseInt(dayNumber);
                            dateStringIndex += dayNumber.length;
                        }
                    }
                }
            }
            var date = new Date();
            if (year == -1) {
                year = date.getFullYear();
            }
            if (month == -1) {
                month = date.getMonth();
            }
            if (day == -1) {
                day = date.getDate();
            }
            return new Date(year, month - 1, day);
        },

        parseDateFormatPattern : function (pattern)
        {
            var patternArray = new Array();
            var currentPattern = null;
            for (var index = 0; index < pattern.length;) {
                var ch = pattern.charAt(index);
                if (SpecialDateChars.indexOf(ch) != -1) {
                    // In here, we're scanning special date format chars
                    var indexOfNextRegularChar = Util.indexOfNotChar(pattern, index + 1, ch);
                    if (indexOfNextRegularChar == -1) {
                        indexOfNextRegularChar = pattern.length;
                    }
                    currentPattern = pattern.substring(index, indexOfNextRegularChar);
                }
                else {
                    // In here, we're scanning interstitial chars (not special chars)
                    var indexOfNextSpecialChar = Util.indexOfCharInSet(pattern, index + 1, SpecialDateChars);
                    if (indexOfNextSpecialChar == -1) {
                        indexOfNextSpecialChar = pattern.length;
                    }
                    currentPattern = pattern.substring(index, indexOfNextSpecialChar);
                }
                patternArray[patternArray.length] = currentPattern;
                index += currentPattern.length;
            }
            return patternArray;
        },

        lookupShortMonthNameIndex : function (shortMonthName)
        {
            for (var index = 0; index < AWShortMonthNames.length; index++) {
                if (shortMonthName == AWShortMonthNames[index]) {
                    return index + 1;
                }
            }
            return -1;
        },

        formatDate : function (date, pattern)
        {
            if (null == pattern) {
                return "";
            }

            var toAppendTo = new String();
            // inQuote set true when hits 1st single quote
            var inQuote = new Boolean(false);
            var prevCh = "";
            // number of time pattern characters repeated
            var count = 0;
            // Number of characters between quotes
            var interQuoteCount = 1;

            for (var i = 0; i < pattern.length; i++) {
                var ch = pattern.charAt(i);
                if (true == inQuote) {
                    if (ch == '\'') {
                        // ends with 2nd single quote
                        inQuote = false;
                        if (0 == count) {
                            // two consecutive quotes outside a quote: ''
                            toAppendTo = toAppendTo + ch;
                        }
                        else {
                            count = 0;
                        }
                        interQuoteCount = 0;
                    }
                    else {
                        toAppendTo = toAppendTo + ch;
                        count++;
                    }
                }
                    // !inQuote
                else {
                    if ('\'' == ch) {
                        inQuote = true;
                        // handle cases like: yyyy'....
                        if (count > 0) {
                            toAppendTo = toAppendTo + this.subFormat(prevCh, count, date);
                            count = 0;
                            prevCh = "";
                        }

                        // We count characters between quotes so we can recognize
                        // two single quotes inside a quote.  Example: 'o''clock'.
                        if (0 == interQuoteCount) {
                            toAppendTo = toAppendTo + ch;
                        // Make it look like we never left.
                            count = 1;
                        }
                    }
                    else if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                        // ch is a date-time pattern
                        // handle cases: eg, yyyyMMdd
                        if (ch != prevCh && count > 0) {
                            toAppendTo = toAppendTo + this.subFormat(prevCh, count, date);
                            prevCh = ch;
                            count = 1;
                        }
                        else {
                            if (ch != prevCh) {
                                prevCh = ch;
                            }
                            count++;
                        }
                    }
                        // handle cases like: MM-dd-yy or HH:mm:ss
                    else if (count > 0) {
                        toAppendTo = toAppendTo + this.subFormat(prevCh, count, date);
                        toAppendTo = toAppendTo + ch;
                        prevCh = "";
                        count = 0;
                    }
                        // any other unquoted characters
                    else {
                        toAppendTo = toAppendTo + ch;
                    }
                    interQuoteCount++;
                }
            }
            // Format the last item in the pattern
            if (count > 0) {
                toAppendTo = toAppendTo + this.subFormat(prevCh, count, date);
            }
            return toAppendTo;
        },

        // Private member function that does the real date/time formatting.
        // returns String
        subFormat : function (ch, count, date)
        {
            var current = "";

            switch (ch) {
                case 'y':
                    if (count >= 4) {
                        current = date.getFullYear();
                    }
                    else { // count < 4
                        // does getYear() ever return a 2-char value?
                        current = date.getYear();
                    }
                    break;
                case 'M':
                    if (count >= 4) {
                        current = AWMonthNames[date.getMonth()];
                    }
                    else if (count == 3) {
                        current = AWShortMonthNames[date.getMonth()];
                    }
                    else {
                        current = date.getMonth() + 1;
                        if (count > 1) {
                            if (current < 10) {
                                current = "0" + current;
                            }
                        }
                    }
                    break;
                // DAY_OF_WEEK
                case 'E':
                    if (count >= 4) {
                        current = AWWeekdayNames[date.getDay()];
                    }
                        // count < 4, use abbreviated form if exists
                    else {
                        current = AWShortWeekdayNames[date.getDay()];
                    }
                    break;
                case 'K': // HOUR:1-based.  eg, 11PM + 1 hour =>> 12 AM (no idea why there is both 'K' and 'h'!)
                case 'h': // HOUR:1-based.  eg, 11PM + 1 hour =>> 12 AM
                    current = d.getHours();
                    if (current > 12) {
                        current -= 12;
                    }
                    if (count > 1) {
                        if (current < 10) {
                            current = "0" + current;
                        }
                    }
                    break;
                case 'k': // HOUR_OF_DAY:0-based.  eg, 23:59 + 1 hour =>> 00:59 (no idea why there is both 'k' and 'H'!)
                case 'H': // HOUR_OF_DAY:0-based.  eg, 23:59 + 1 hour =>> 00:59
                    current = date.getHours();
                    if (count > 1) {
                        if (current < 10) {
                            current = "0" + current;
                        }
                    }
                    break;
                case 'd':
                    current = date.getDate();
                    if (count > 1) {
                        if (current < 10) {
                            current = "0" + current;
                        }
                    }
                    break;
                case 'm':
                    current = date.getMinutes();
                    if (count > 1) {
                        if (current < 10) {
                            current = "0" + current;
                        }
                    }
                    break;
                case 's':
                    current = date.getSeconds();
                    if (count > 1) {
                        if (current < 10) {
                            current = "0" + current;
                        }
                    }
                    break;
                case 'a':
                    var hours = date.getHours();
                    if (hours < 12) {
                        current = AWAMString;
                    }
                    else {
                        current = AWPMString;
                    }
                    break;
                default:
                    break;
            }
            return current;
        },

        /**
         * This function is part of the "DateField" component
         */
        dateFieldClicked : function (date, clientFormatString, targetName)
        {
            var textField = Dom.getElementById(targetName);
            var dateString = null;
            if (clientFormatString != null && clientFormatString.length > 0) {
                dateString = this.formatDate(date, clientFormatString)
            }
            else {
                dateString = date.toString();
            }
            textField.value = dateString;
        },

        /**
         * This function is part of the "DateField" component -- used to initialize the Calendar from the TextField
         */
        dateFieldInit : function (targetName, pattern)
        {
            var textField = Dom.getElementById(targetName);
            var dateString = Util.strTrim(textField.value);
            return this.parseDate(dateString, pattern);
        },
        EOF:0};

    //
    // iPad - specific methods
    //
    if (Dom.isIPad) Util.extend(Calendar, function () {
        return {
            showCalendar : function (linkId)
            {
                var linkObj = Dom.getElementById(linkId);
                var menuId = linkObj.getAttribute("awmenuId");
                if (linkObj._awcalendar) {
                    linkObj._awcalendar.renderCalendar(linkObj._awcalendar._selectedDate);
                    Menu.menuLinkOnClick(linkObj, menuId, null, null);
                }
            },

        EOF:0};

    }());


    Event.registerBehaviors({
        // CPY - Calender previous year
        CPY : {
            mousedown : function (elm, evt) {
                return Event.cancelBubble(evt);
            },

            mouseup : function (elm, evt) {
                return Calendar.previousYearClicked(evt, elm);
            }
        },

        // CPM - Calender previous month
        CPM : {
            mousedown : function (elm, evt) {
                return Event.cancelBubble(evt);
            },

            mouseup : function (elm, evt) {
                return Calendar.previousMonthClicked(evt, elm);
            }
        },

        // CNM - Calender next month
        CNM : {
            mousedown : function (elm, evt) {
                return Event.cancelBubble(evt);
            },

            mouseup : function (elm, evt) {
                return Calendar.nextMonthClicked(evt, elm);
            }
        },

        // CNY - Calender next year
        CNY : {
            mousedown : function (elm, evt) {
                return Event.cancelBubble(evt);
            },

            mouseup : function (elm, evt) {
                return Calendar.nextYearClicked(evt, elm);
            }
        },

        // CB - Calendar body
        CB : {
            mouseover : function (elm, evt) {
                return Calendar.calMouseOver(evt);
            },

            mouseout : function  (elm, evt) {
                return Calendar.calMouseOut(evt);
            },

            mousedown : function  (elm, evt) {
                return Calendar.calMouseDown(evt);
            }
        }
    });

    ariba.Event.registerBehaviors({

        // DFB - Date Field calendar body override
        DFB : {
            prototype : Event.behaviors.CB,
            mousedown : function  (elm, evt) {
                return Calendar.dateFieldMouseDown(evt);
            }
        }
    });

    return Calendar;
}();
