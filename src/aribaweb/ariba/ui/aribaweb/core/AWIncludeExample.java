package ariba.ui.aribaweb.core;

import ariba.util.core.MapUtil;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: pdanella
 * Date: Jul 12, 2011
 * Time: 4:43:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class AWIncludeExample extends AWContainerElement
{
    public AWComponent _component;
    public String Component = "component";
    private Map _examples;
    public AWApi _componentApi;

    public void init (String tagName, Map bindingsHashtable)
    {
        _examples = MapUtil.cloneMap(bindingsHashtable);
        super.init();
    }

    public String componentName ()
    {
       AWBinding binding = getBinding(Component, false);
       return binding == null ? null : binding.stringValue(null);
    }

    public AWComponent component ()
    {
        if (_component == null) {
            // get the example component name
           String exampleComponentName = componentName();
           // create an instance of the component if specified
           if (exampleComponentName != null) {
                // find the component definition (factory pattern)
                AWComponentDefinition definition =
                   ((AWConcreteApplication)AWConcreteApplication.SharedInstance).componentDefinitionForName(exampleComponentName);
               // create the component
               _component = definition.createComponent(new AWComponentReference(definition), null, null);
           }
        }
        return _component;
    }

    //messed up, _componentApi == null -> loadTemplate() blows up
    //don't have any single clue...
    public AWApi exampleComponentApi ()
    {
        // lazily create the example component
        if (_componentApi == null) {
            AWComponent component = component();
            if (component != null) {
                // get the component API object via component
                // (need to trigger parsing of AWL)
                _componentApi = component.componentApi();
           }
       }
       return _componentApi;
    }

    private AWBinding getBinding(String key, boolean required){
        AWBinding binding = (AWBinding)_examples.get(key);
        if (required && binding == null) {
            throw new RuntimeException("AWIncludeExample missing required binding specification \"" + key + "\"");
        }
        return binding;
    }
}
