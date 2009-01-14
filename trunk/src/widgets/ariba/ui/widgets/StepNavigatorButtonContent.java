package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.util.core.MapUtil;
import ariba.util.core.StringUtil;
import java.util.Map;

public class StepNavigatorButtonContent extends AWComponent
{
    private static Map ImagesForString = MapUtil.map(2);

    static {
        ImagesForString.put("<", "navPrevious.gif");
        ImagesForString.put(">", "navNext.gif");
    }

    public String[] _tokens;
    public String _currentToken;
    public int _index;

    protected void awake ()
    {
        super.awake();
        String value  = stringValueForBinding(BindingNames.value);
        if (value.indexOf(' ') > -1) {
            _tokens = StringUtil.delimitedStringToArray(value, ' ');
        }
        else {
            _tokens = new String[1];
            _tokens[0] = value;
        }
    }

    protected void sleep ()
    {
        _tokens = null;
        _currentToken = null;
        super.sleep();
    }

    public String currentImage ()
    {
        return (String)ImagesForString.get(_currentToken);   
    }

    public String spaceIfAny ()
    {
        return (_index < _tokens.length-1) ? " ": null;
    }
}
