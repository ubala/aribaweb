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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/Calendar.java#14 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequest;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWComponentReference;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWSession;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWDateFactory;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Calendar extends AWComponent
{
    protected static final class AWDefaultDateFactory implements AWDateFactory
    {
        private boolean _calendarDate;

        protected AWDefaultDateFactory (boolean calendarDate)
        {
            _calendarDate = calendarDate;
        }

        public Date createDate ()
        {
            return new ariba.util.core.Date();
        }

        public Date createDate (int year, int month, int day)
        {
            return new ariba.util.core.Date(year, month, day);
        }

        public Date createDate (int year, int month, int day, TimeZone timezone, Locale locale)
        {
            return new ariba.util.core.Date(year, month, day, _calendarDate, timezone, locale);
        }

        public int getYear (java.util.Date date, TimeZone timezone, Locale locale)
        {
            return ariba.util.core.Date.getYear(date, timezone, locale);
        }

        public int getMonth (java.util.Date date, TimeZone timezone, Locale locale)
        {
            return ariba.util.core.Date.getMonth(date, timezone, locale);
        }

        public int getDayOfMonth (java.util.Date date, TimeZone timezone, Locale locale)
        {
            return ariba.util.core.Date.getDayOfMonth(date, timezone, locale);
        }

    };

    protected static final AWDateFactory DefaultDateFactory = new AWDefaultDateFactory(false);
    protected static final AWDateFactory CalendarDateFactory = new AWDefaultDateFactory(true);

    private static final String AWCalendarYearKey = "awcaly";
    private static final String AWCalendarMonthKey = "awcalm";
    private static final String AWCalendarDateKey = "awcald";

    public AWEncodedString _elementId;
    public java.util.Date _selectedDate;
    // This is only here to trick takeValuesPahse into visiting this component
    // if we're in a form (also see subcomponent in Calendar.awl)
    public Object _formValueDummy;
    private AWDateFactory _dateFactory;

    protected static boolean isCalendarAction (AWRequest request)
    {
        return request.formValueForKey(AWCalendarYearKey) != null;
    }

    protected void sleep ()
    {
        _elementId = null;
        _selectedDate = null;
        _formValueDummy = null;
        _dateFactory = null;
    }

    static Date computeCalendarDate (AWRequestContext requestContext, AWDateFactory dateFactory)
    {
        AWRequest request = requestContext.request();
        AWSession session = requestContext.session();
        String yearString = request.formValueForKey(AWCalendarYearKey);
        String monthString = request.formValueForKey(AWCalendarMonthKey);
        String dayString = request.formValueForKey(AWCalendarDateKey);
        Locale clientLocale = request.preferredLocale();
        TimeZone clientTimeZone = session.clientTimeZone();
        int year = Integer.parseInt(yearString);
        int month = Integer.parseInt(monthString);
        int day = Integer.parseInt(dayString);
        return dateFactory.createDate(year, month, day, clientTimeZone, clientLocale);
    }

    public AWComponent dateClicked ()
    {
        if ("GET".equalsIgnoreCase(request().method())) {
            // if request is a form POST, then we'll push in take.
            Date calendarDate = Calendar.computeCalendarDate(requestContext(), dateFactory());
            setValueForBinding(calendarDate, BindingNames.selection);
        }
        return (AWComponent)valueForBinding(BindingNames.action);
    }

    protected AWDateFactory dateFactory ()
    {
        if (_dateFactory == null) {
            _dateFactory = (AWDateFactory)valueForBinding(BindingNames.dateFactory);
            if (_dateFactory == null) {
                if (booleanValueForBinding(BindingNames.calendarDate)) {
                    _dateFactory = CalendarDateFactory;
                }
                else {
                    _dateFactory = DefaultDateFactory;
                }
            }
        }
        return _dateFactory;
    }

    public int selectedYear ()
    {
        Date selectedDate = selectedDate();
        return dateFactory().getYear(selectedDate, clientTimeZone(), preferredLocale());
    }

    public int selectedMonth ()
    {
        Date selectedDate = selectedDate();
        return dateFactory().getMonth(selectedDate, clientTimeZone(), preferredLocale());
    }

    public int selectedDayOfMonth ()
    {
        Date selectedDate = selectedDate();
        return dateFactory().getDayOfMonth(selectedDate, clientTimeZone(), preferredLocale());
    }

    private Date selectedDate ()
    {
        if (_selectedDate == null) {
            _selectedDate = (Date)valueForBinding(BindingNames.selection);
            if (_selectedDate == null) {
                _selectedDate = dateFactory().createDate();
            }
        }
        return _selectedDate;
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        super.applyValues(requestContext, component);
        // must push in take so we can be consistent with the TextField used in DateField
        if (_elementId.equals(requestContext.requestSenderId())) {
            Date calendarDate = computeCalendarDate(requestContext, dateFactory());
            setValueForBinding(calendarDate, BindingNames.selection);
        }
    }

    protected boolean _debugSemanticKeyInteresting ()
    {
        return true;
    }

    protected AWBinding _debugPrimaryBinding ()
    {
        AWBinding primaryBinding = null;
        AWComponentReference componentRef = componentReference();

        if (componentRef != null) {
            primaryBinding = bindingForName(AWBindingNames.selection, false);
            if (primaryBinding == null) {
                primaryBinding = bindingForName(AWBindingNames.action, false);
            }
        }
        return primaryBinding;
    }

}
