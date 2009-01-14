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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWIf.java#4 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.Log;
import ariba.util.core.ListUtil;
import ariba.util.core.Fmt;

import java.lang.reflect.Field;
import java.util.Map;

// subclassed by AWWrapperIf
public class AWIf extends AWBindableElement implements AWElementContaining
{
    private AWIfBlock[] _ifBlocks;

    public AWIfBlock[] _conditionBlocks ()
    {
        return _ifBlocks;
    }

    public void setTemplateName (String name)
    {
        super.setTemplateName(name);
        if (_ifBlocks != null) {
            for (int index = _ifBlocks.length - 1; index > -1; index--) {
                AWIfBlock ifBlock = _ifBlocks[index];
                if (ifBlock != null) {
                    ifBlock.setTemplateName(name);
                }
            }
        }
    }

    public void setLineNumber (int lineNumber)
    {
        super.setLineNumber(lineNumber);
        if (_ifBlocks != null) {
            for (int index = _ifBlocks.length - 1; index > -1; index--) {
                AWIfBlock ifBlock = _ifBlocks[index];
                if (ifBlock != null) {
                    ifBlock.setLineNumber(lineNumber);
                }
            }
        }
    }

    public static boolean evaluateConditionInComponent (AWBinding conditionBinding, AWComponent component, boolean negate)
    {
        boolean booleanValue = false;
        Object object = conditionBinding.value(component);
        if (object == null) {
            booleanValue = false;
        }
        else {
            if (object instanceof Boolean) {
                booleanValue = ((Boolean)object).booleanValue();
            }
            else if (object instanceof Number) {
                booleanValue = (((Number)object).intValue() == 0) ? false : true;
            }
            else {
                booleanValue = true;
            }
        }
        if (negate) {
            booleanValue = !booleanValue;
        }
        return booleanValue;
    }

    public void init (String tagName, Map bindingsHashtable)
    {
        _ifBlocks = new AWIfBlock[1];
        _ifBlocks[0] = newConditionBlockWithBindings(bindingsHashtable, this);
        super.init(tagName, null);
    }

    protected AWContainerElement[] conditionBlocks ()
    {
        return _ifBlocks;
    }

    public void add (AWElement element)
    {
        if (element instanceof AWElse) {
            AWElse elseElement = (AWElse)element;
            Map bindingsHashtable = elseElement.bindingsHashtable();
            AWIfBlock newIfBlock = newConditionBlockWithBindings(bindingsHashtable, elseElement);
            _ifBlocks = (AWIfBlock[])AWUtil.realloc(_ifBlocks, _ifBlocks.length + 1);
            _ifBlocks[_ifBlocks.length - 1] = newIfBlock;
        }
        else {
            _ifBlocks[_ifBlocks.length - 1].add(element);
        }
    }

    private AWIfBlock newConditionBlockWithBindings (Map bindingsHashtable, AWBaseElement baseElement)
    {
        AWIfBlock ifBlock = new AWIfBlock();
        ifBlock.init("AWIfBlock", bindingsHashtable);
        ifBlock.setTemplateName(baseElement.templateName());
        ifBlock.setLineNumber(baseElement.lineNumber());
        return ifBlock;
    }

    private boolean conditionValueInPhase (AWIfBlock ifBlock, AWRequestContext requestContext,
                                           AWComponent component, AWElementIdPath targetElementIdPath, String phaseName)
    {
        boolean conditionValue = false;
        final boolean allowsSkipping = requestContext.allowsSkipping();
        if (allowsSkipping) {
            // if allowsdSkipping, ignore the computedConditionValue and
            // determine if the conditionValue based on if the senderId is
            // within the current ifBlock
            conditionValue = requestContext.nextPrefixMatches(targetElementIdPath);
            if (conditionValue) {
                // Go ahead and evaluate the actual conditional so that we simulate normal execution
                boolean actualConditionValue = ifBlock.evaluateConditionInComponent(component);
                // and check it so we can report a mismatch in evaluations across phases.
                if (!actualConditionValue) {
                    Log.aribaweb.debug("Warning: Mismatched AWIf in %s phase: %s:%s",
                            phaseName, templateName(), Integer.toString(lineNumber()));
                }
            }
            else {
                if (Log.aribawebexec_skip.isDebugEnabled()) {
                    // Evaluate the actual conditional
                    boolean actualConditionValue = ifBlock.evaluateConditionInComponent(component);
                    // and check / report a mismatch in evaluations across phases
                    if (actualConditionValue) {
                        Log.aribawebexec_skip.debug(
                            "Skipping condition that we would have otherwise traversed %s %s %s",
                            phaseName, templateName(), Integer.toString(lineNumber()));
                    }
                }
            }
        }
        else {
            conditionValue = ifBlock.evaluateConditionInComponent(component);
            if (AWConcreteApplication.IsDebuggingEnabled && !conditionValue &&
                    requestContext.nextPrefixMatches(targetElementIdPath)) {
                Log.dumpAWStack(component,
                    Fmt.S("Error: Mismatched AWIf in %s phase: %s:%s",
                            phaseName, templateName(), Integer.toString(lineNumber())));
            }
        }
        return conditionValue;
    }

    /*
        allowsSkipping: if true, allows If and For to skip the entire body or iterations up to and
                        beyond the iteration in question.  By default, this is false except for ariba.ui.
                        All packages should strive to use allowsSkipping true as there is negligible cost and
                        potentially huge savings.
    */
    public void applyValues (AWRequestContext requestContext, AWComponent component)
    {
        boolean conditionValue = false;
        for (int index = 0, conditionBlocksLength = _ifBlocks.length; index < conditionBlocksLength; index++) {
            AWIfBlock currentIfBlock = _ifBlocks[index];
            AWBaseElement prev = requestContext.pushCurrentElement(currentIfBlock);
            if (!conditionValue) {

                AWElementIdPath targetFormIdPath = requestContext.targetFormIdPath();
                conditionValue = conditionValueInPhase(currentIfBlock, requestContext, component,
                        targetFormIdPath, "applyValues");
                requestContext.pushElementIdLevel();
                if (conditionValue) {
                    currentIfBlock.applyValues(requestContext, component);
                }
                requestContext.popElementIdLevel();
            }
            else {
                requestContext.incrementElementId();
            }
            requestContext.popCurrentElement(prev);
        }
    }

    public AWResponseGenerating invokeAction (AWRequestContext requestContext, AWComponent component)
    {
        AWResponseGenerating actionResults = null;
        boolean conditionValue = false;
        for (int index = 0, conditionBlocksLength = _ifBlocks.length; index < conditionBlocksLength; index++) {
            AWIfBlock currentIfBlock = _ifBlocks[index];
            AWBaseElement prev = requestContext.pushCurrentElement(currentIfBlock);
            if (!conditionValue) {
                AWElementIdPath requestSenderIdPath = requestContext.requestSenderIdPath();
                conditionValue = conditionValueInPhase(currentIfBlock, requestContext, component,
                        requestSenderIdPath, "invokeAction");
                requestContext.pushElementIdLevel();
                if (conditionValue) {
                    actionResults = currentIfBlock.invokeAction(requestContext, component);
                    if (actionResults != null) {
                        // if we have action results, then the invokeActionPhase is complete
                        // and we do not need to continue incrementing the elementId.
                        break;
                    }
                }
                requestContext.popElementIdLevel();
            }
            else {
                requestContext.incrementElementId();
            }
            requestContext.popCurrentElement(prev);
        }
        return actionResults;
    }


    public void renderResponse (AWRequestContext requestContext, AWComponent component)
    {
        int conditionBlocksLength = _ifBlocks.length;
        boolean conditionValue = false;
        for (int index = 0; index < conditionBlocksLength; index++) {
            AWIfBlock currentIfBlock = _ifBlocks[index];
            AWBaseElement prev = requestContext.pushCurrentElement(currentIfBlock);
            if (!conditionValue) {
                conditionValue = currentIfBlock.evaluateConditionInComponent(component);
                requestContext.pushElementIdLevel();
                if (conditionValue) {
                    currentIfBlock.renderResponse(requestContext, component);
                }
                requestContext.popElementIdLevel();
            }
            else {
                requestContext.incrementElementId();
            }
            requestContext.popCurrentElement(prev);
        }
    }

    public void validate (AWValidationContext validationContext, AWComponent component)
    {
        for (int index = 0, conditionBlocksLength = _ifBlocks.length; index < conditionBlocksLength; index++) {
            _ifBlocks[index].validate(validationContext, component);
        }
    }

    protected Object getFieldValue (Field field)
      throws IllegalArgumentException, IllegalAccessException
    {
        try {
            return field.get(this);
        }
        catch (IllegalAccessException ex) {
            return super.getFieldValue(field);
        }
    }

    public String toString ()
    {
        String conditionBlocksString = ListUtil.arrayToList(conditionBlocks()).toString();
        return super.toString() + conditionBlocksString;
    }

    public static final class AWIfBlock extends AWContainerElement
    {
        private static final int IfTrue = 0;
        private static final int IfFalse = 1;
        private static final int None = 2;
        private static final int IsEqual = 3;
        private static final int IsNotEqual = 4;
        private static final int IsLessThan = 5;
        private static final int IsGreaterThan = 6;
        private static final int IsLessOrEqual = 7;
        private static final int IsGreaterOrEqual = 8;

        private AWBinding _value;
        private AWBinding _operand;
        private int _operator;

        public void init (String tagName, Map bindingsHashtable)
        {
            _value = (AWBinding)bindingsHashtable.remove(AWBindingNames.value);
            if (_value != null) {
                if ((_operand = (AWBinding)bindingsHashtable.remove(AWBindingNames.isEqual)) != null) {
                    _operator = IsEqual;
                }
                else if ((_operand = (AWBinding)bindingsHashtable.remove(AWBindingNames.isNotEqual)) != null) {
                    _operator = IsNotEqual;
                }
                else if ((_operand = (AWBinding)bindingsHashtable.remove(AWBindingNames.isLessThan)) != null) {
                    _operator = IsLessThan;
                }
                else if ((_operand = (AWBinding)bindingsHashtable.remove(AWBindingNames.isGreaterThan)) != null) {
                    _operator = IsGreaterThan;
                }
                else if ((_operand = (AWBinding)bindingsHashtable.remove(AWBindingNames.isLessOrEqual)) != null) {
                    _operator = IsLessOrEqual;
                }
                else if ((_operand = (AWBinding)bindingsHashtable.remove(AWBindingNames.isGreaterOrEqual)) != null) {
                    _operator = IsGreaterOrEqual;
                }
                else {
                    throw new AWGenericException(
                        "AWIf: Missing binding.  If you specify the 'value' binding, " +
                        "you must also specify a binding to which this value will be compared.");
                }
            }
            else {
                AWBinding negateBinding = (AWBinding)bindingsHashtable.remove(AWBindingNames.negate);
                boolean negate = (negateBinding != null) ? negateBinding.booleanValue(null): false;
                if ((_operand = (AWBinding)bindingsHashtable.remove(AWBindingNames.ifTrue)) != null) {
                    _operator = negate ? IfFalse : IfTrue;
                }
                else if ((_operand = (AWBinding)bindingsHashtable.remove(AWBindingNames.notEqualNull)) != null) {
                    _operator = negate ? IfFalse : IfTrue;
                }
                else if ((_operand = (AWBinding)bindingsHashtable.remove(AWBindingNames.condition)) != null) {
                    _operator = negate ? IfFalse : IfTrue;
                }
                else if ((_operand = (AWBinding)bindingsHashtable.remove(AWBindingNames.ifFalse)) != null) {
                    _operator = negate ? IfTrue : IfFalse;
                }
                else if ((_operand = (AWBinding)bindingsHashtable.remove(AWBindingNames.equalNull)) != null) {
                    _operator = negate ? IfTrue : IfFalse;
                }
                else {
                    _operator = None;
                }
            }
            super.init(tagName, bindingsHashtable);
        }

        public boolean evaluateConditionInComponent (AWComponent component)
        {
            switch (_operator) {
                    // IfTrue
                case 0: {
                    return AWIf.evaluateConditionInComponent(_operand, component, false);
                }
                    // IfFalse
                case 1: {
                    return AWIf.evaluateConditionInComponent(_operand, component, true);
                }
                    // None
                case 2: {
                    return true;
                }
                    // IsEqual
                case 3: {
                    return isEqual(_value, _operand, component);
                }
                    // IsNotEqual
                case 4: {
                    return !isEqual(_value, _operand, component);
                }
                    // IsLessThan
                case 5: {
                    return doubleValue(_value, component) < doubleValue(_operand, component);
                }
                    // IsGreaterThan
                case 6: {
                    return doubleValue(_value, component) > doubleValue(_operand, component);
                }
                    // IsLessOrEqual
                case 7: {
                    return doubleValue(_value, component) <= doubleValue(_operand, component);
                }
                    // IsGreaterOrEqual
                case 8: {
                    return doubleValue(_value, component) >= doubleValue(_operand, component);
                }
            }
            return true;
        }

        private boolean isEqual (AWBinding valueBinding, AWBinding operandBinding, AWComponent component)
        {
            Object value = (valueBinding == null) ? null: valueBinding.value(component);
            Object operand = (operandBinding == null) ? null: operandBinding.value(component);
            boolean isEqual = (value == operand);
            if (!isEqual && (value != null)) {
                isEqual = value.equals(operand);
            }
            return isEqual;
        }

        private double doubleValue (AWBinding binding, AWComponent component)
        {
            double doubleValue = 0.0;
            if (binding != null) {
                Number doubleNumber = (Number)binding.value(component);
                if (doubleNumber != null) {
                    doubleValue = doubleNumber.doubleValue();
                }
            }
            return doubleValue;
        }

        protected Object getFieldValue (Field field)
        throws IllegalArgumentException, IllegalAccessException
        {
            try {
                return field.get(this);
            }
            catch (IllegalAccessException ex) {
                return super.getFieldValue(field);
            }
        }

        public String toString ()
        {
            String string = super.toString();
            string += _operand == null ? "No Operand" : _operand.toString();
            if (_value != null) {
                string += " " + _value.toString();
            }
            return string;
        }

        public AWResponseGenerating invokeAction (AWRequestContext requestContext, AWComponent component)
        {
            if (AWConcreteApplication.IsDebuggingEnabled && component.actionTracingEnabled()) {
                Log.aribaweb.debug("AWIfBlock: %s:%s", templateName(), lineNumber());
            }
            return super.invokeAction(requestContext, component);
        }
    }
}
