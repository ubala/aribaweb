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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/DateField.java#29 $
*/

package ariba.ui.widgets;

import java.util.Date;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWEditableRegion;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWSession;
import ariba.ui.aribaweb.util.AWDateFactory;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWFormatter;
import ariba.ui.validation.AWVDateFormatter;
import ariba.util.core.Fmt;
import ariba.util.core.StringUtil;

public class DateField extends AWComponent
{
    private static final String DefaultFormatterKey = "DateField_defaultFormatter";
    protected Object _date;
    public AWFormatter _formatter;
    public AWDateFactory _dateFactory;
    public AWEncodedString _menuId;
    public AWEncodedString _dateTextFieldId;
    public AWEncodedString _linkId;
    public boolean _showCalendar;
    private String _exampleDate;
    private Object[] _formValueStrings = null;
    
    /**
        Convenience setter method to assist DateField debugging. (Otherwise it
        seems to be impossible to trap the sets of this field.)
        @aribaapi ariba
    */
    public void setDate (Object object)
    {
        _date = object;
    }

    public Object getDate ()
    {
        return _date;
    }
    
    public void setFormValueStrings (Object[] obj)
    {
    	_formValueStrings = obj;
    }
    
    public boolean isStateless()
    {
        // We must make this stateful so that the textDate can be available during the
        // invokeAction phase; it is set during takeValues
        return false;
    }

    public void setMenuId (AWEncodedString menuId)
    {
        _menuId = menuId;
        _dateTextFieldId =
            AWEncodedString.sharedEncodedString(StringUtil.strcat("DF", menuId.string()));
    }

    public AWComponent textFieldAction ()
    {
        // we will have already pushed at the end of takeValues
        // if the action came from the textField
        valueForBinding(BindingNames.action);
        _showCalendar = true;
        return null;
    }

    public AWComponent calendarAction ()
    {
        // if the action comes from the Calendar, we will have already pushed
        // the date (see overrid of applyValues below)
        valueForBinding(BindingNames.action);
        return null;
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        super.applyValues(requestContext, component);
        if (_linkId != null && _linkId.equals(requestContext.requestSenderId())) {
            _date = Calendar.computeCalendarDate(requestContext, dateFactory());
            _formValueStrings = null;
        }
        // displayed in text field, after formatting
        setValueForBinding(_formValueStrings,BindingNames.formValueStrings);
        setValueForBinding(_date, BindingNames.value);
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        _date = valueForBinding(BindingNames.value);
        super.renderResponse(requestContext, component);
        _showCalendar = false;
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
        if (_date == null) {
            _date = dateFactory().createDate();
        }
        return (Date)_date;
    }

    public AWFormatter formatter ()
    {
        if (_formatter == null) {
            _formatter = (AWFormatter)valueForBinding(BindingNames.formatter);
            if (_formatter == null) {
                _formatter = (AWFormatter)session().dict().get(DefaultFormatterKey);
                if (_formatter == null) {
                    // A null format pattern means we will try numerous patterns
                    // as defined at the system install site
                    _formatter = new AWVDateFormatter(null, preferredLocale(), clientTimeZone());
                    session().dict().put(DefaultFormatterKey, _formatter);
                }
            }
        }
        return _formatter;
    }

    public AWDateFactory dateFactory ()
    {
        if (_dateFactory == null) {
            _dateFactory = (AWDateFactory)valueForBinding(BindingNames.dateFactory);
            if (_dateFactory == null) {
                if (booleanValueForBinding(BindingNames.calendarDate)) {
                    _dateFactory = Calendar.CalendarDateFactory;
                }
                else {
                    _dateFactory = Calendar.DefaultDateFactory;
                }
            }
        }
        return _dateFactory;
    }

    ////////////////
    // Sample Date to be used to determine the length of a date field / for example format
    // Jan 31, 2000
    ////////////////
    private static final Object SampleDate = Calendar.DefaultDateFactory.createDate(2000, 0, 31);

    public String exampleDate ()
    {

        Boolean showTip = (Boolean)valueForBinding(BindingNames.showTip);
        if (showTip != null && !showTip.booleanValue()) {
            return null;
        }

        if (booleanValueForBinding(BindingNames.disabled)) {
            return localizedJavaString(3, "Date field is disabled" /* accessibility enabled date field disabled message */);
        }
        if (_exampleDate == null) {
            AWSession session = requestContext().session(false);
            if (session.isAccessibilityEnabled()) {
                return localizedJavaString(2, "Enter date: mm/dd/yyyy" /* accessibility enabled example date format */);
            }
            Object sampleDate = SampleDate;
            AWDateFactory dateFactory = dateFactory();
            if (dateFactory != Calendar.DefaultDateFactory) {
                sampleDate = dateFactory.createDate(2000, 0, 31);
            }
            String formattedDate = formatter().format(sampleDate);
            _exampleDate = Fmt.S(localizedJavaString(1, "Enter date: %s" /* example date format */), formattedDate);
        }
        return _exampleDate;
    }
    
    public boolean isDisabled ()
    {
        return  booleanValueForBinding(BindingNames.disabled) || 
            AWEditableRegion.disabled(requestContext());
    }
}
