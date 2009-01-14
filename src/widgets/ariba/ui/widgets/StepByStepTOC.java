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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/StepByStepTOC.java#25 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.util.fieldvalue.OrderedList;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.util.core.Fmt;
import ariba.util.core.Constants;

public final class StepByStepTOC extends AWComponent
{
    /*-----------------------------------------------------------------------
        Constants
      -----------------------------------------------------------------------*/

        // styles
    protected static final AWEncodedString WizStep =
        new AWEncodedString("tocItem");
    protected static final AWEncodedString WizStepRollover =
        new AWEncodedString("tocItemRollover");
    protected static final AWEncodedString WizStepCurrent =
        new AWEncodedString("tocItemCurrent");

        // JavaScript
    protected static final AWEncodedString MouseOver =
        new AWEncodedString(Fmt.S("this.className='%s';", WizStepRollover.string()));
    protected static final AWEncodedString MouseOut =
        new AWEncodedString(Fmt.S("this.className='%s';", WizStep.string()));

    private static String gif (String name)
    {
        return Fmt.S("widg/%s.gif", name).intern();
    }

        // image names
    private static final String VerticalLineFirst = gif("toc_vertline1");
    private static final String VerticalLine      = gif("toc_vertline");
    private static final String VerticalLineLast  = gif("toc_vertlineLast");

        // step image names
    private static final String[] StepNumberImageNames = {
        gif("s01"), gif("s02"), gif("s03"), gif("s04"), gif("s05"),
        gif("s06"), gif("s07"), gif("s08"), gif("s09"), gif("s10"),
        gif("s11"), gif("s12"), gif("s13"), gif("s14"), gif("s15"),
    };

        // step image names
    private static final String[] SelectedStepNumberImageNames = {
        gif("ss01"), gif("ss02"), gif("ss03"), gif("ss04"), gif("ss05"),
        gif("ss06"), gif("ss07"), gif("ss08"), gif("ss09"), gif("ss10"),
        gif("ss11"), gif("ss12"), gif("ss13"), gif("ss14"), gif("ss15"),
    };

        // image for non-numbered step
    private static final String NonNumberedStepImageName = gif("s00");

    /*-----------------------------------------------------------------------
        Fields
      -----------------------------------------------------------------------*/

    private AWBinding _stepBinding;
    private AWBinding _stepIndexBinding;
    private AWBinding _stepLabelBinding;
    private AWBinding _stepIsNumberedBinding;
    private AWBinding _stepIsVisibleBinding;

    private Object _currentStep;
    private boolean _currentStepIsNumbered;
    private int _currentStepIndex;
    private Object _selectedStep;
    private int _visibleStepCount;
    private int _navigateEnd;
    public Object _showSelections;
    public boolean _isClickable;

    protected void awake ()
    {
        // These bindings required all phases
        _stepBinding = bindingForName(BindingNames.step);
        _stepIsVisibleBinding = bindingForName(BindingNames.stepIsVisible);

        _selectedStep = valueForBinding(BindingNames.selectedStep);
        if (_selectedStep == null) {
            Object steps = valueForBinding(BindingNames.steps);
            _selectedStep = OrderedList.get(steps).elementAt(steps, 0);
        }
        _showSelections = valueForBinding(BindingNames.showSelections);
        AWBinding isClickableBinding = bindingForName(BindingNames.isClickable);
        _isClickable = isClickableBinding == null ? true : booleanValueForBinding(isClickableBinding);
        AWBinding navigateEndBinding = bindingForName("navigateStepIndexMax");        
        _navigateEnd = navigateEndBinding == null ? -1 : intValueForBinding(navigateEndBinding);
    }

    protected void sleep ()
    {
        _stepBinding = null;
        _stepIsVisibleBinding = null;

        _currentStep = null;
        _selectedStep = null;
        _showSelections = null;

        _stepIndexBinding = null;
        _stepLabelBinding = null;
        _stepIsNumberedBinding = null;
        _currentStepIsNumbered = false;
        _currentStepIndex = 0;
        _visibleStepCount = 0;
        _isClickable = false;
        _navigateEnd = -1;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        // These bindings only required for renderResponse phase
        _stepLabelBinding = bindingForName(BindingNames.stepLabel);
        _stepIndexBinding = bindingForName(BindingNames.stepIndex);
        _stepIsNumberedBinding = bindingForName(BindingNames.stepIsNumbered);
        _visibleStepCount = intValueForBinding(BindingNames.visibleStepCount);

        super.renderResponse(requestContext, component);

        _visibleStepCount = -1;
    }

    public void setCurrentStep (Object step)
    {
        _currentStep = step;
        setValueForBinding(step, _stepBinding);
        _currentStepIsNumbered = booleanValueForBinding(_stepIsNumberedBinding);
        _currentStepIndex = intValueForBinding(_stepIndexBinding);
    }

    public AWEncodedString currentStyle ()
    {
        return (_currentStep == _selectedStep) ? WizStepCurrent : WizStep;
    }

    public AWEncodedString currentMouseOver ()
    {
        return (_currentStep == _selectedStep) ? null : MouseOver;
    }

    public AWEncodedString currentMouseOut ()
    {
        return (_currentStep == _selectedStep) ? null : MouseOut;
    }

    public String verticalLineImageName ()
     {
        String verticalLineImageName = null;
        if (_currentStepIndex == (_visibleStepCount - 1)) {
            if (_currentStepIndex == 0) {
                verticalLineImageName = null;
            }
            else {
                verticalLineImageName = VerticalLineLast;
            }
        }
        else if (_currentStepIndex == 0) {
            verticalLineImageName = VerticalLineFirst;
        }
        else {
            verticalLineImageName = VerticalLine;
        }
        return verticalLineImageName;
    }

    //
    // The following methods implemented in java to avoid always
    // looking up their bindings as these appear within a repetition.
    //

    public AWEncodedString currentStepLabel ()
    {
        return encodedStringValueForBinding(_stepLabelBinding);
    }

    public String currentStepNumberImageName ()
    {
        if (_currentStepIsNumbered) {
            if (_currentStep == _selectedStep) {
                return SelectedStepNumberImageNames[_currentStepIndex];
            }
            else {
                return StepNumberImageNames[_currentStepIndex];
            }
        }
        else {
            return NonNumberedStepImageName;
        }
    }

    public String currentStepNumberHint ()
    {
        if (_currentStepIsNumbered) {
            String fmt = localizedJavaString(1, "Step {0}" /*  */);
            return Fmt.Si(fmt, Constants.getInteger(_currentStepIndex + 1));
        }
        else {
            return localizedJavaString(2, "Non-numbered step" /*  */);
        }
    }

    public Boolean currentStepIsVisible ()
    {
        return (Boolean)valueForBinding(_stepIsVisibleBinding);
    }

    // Action Handling

    public AWResponseGenerating stepClicked ()
    {
        setValueForBinding(_currentStep, BindingNames.selectedStep);
        return (AWResponseGenerating)valueForBinding(BindingNames.stepAction);
    }
    
    public boolean curIsClickable ()
    {
        return (_isClickable ||
               _currentStepIndex < _navigateEnd) &&
               _currentStep != _selectedStep;
    }

    public boolean currentStepSelected ()
    {
        return _currentStep == _selectedStep;
    }

    public String awname ()
    {
        return "StepByStepTOC";
    }

}
