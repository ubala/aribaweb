package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.*;

/**
 * Created by IntelliJ IDEA.
 * User: pdanella
 * Date: Jul 21, 2011
 * Time: 12:38:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class AWExampleComponent extends AWComponent {

    // override super to get template based on templateName binding value
    // todo: move this into ExampleComponent.java
    
    @Override
    public AWTemplate template ()
    {
        // get the AWExampleAPI
        String exampleName = stringValueForBinding("templateName");

        if (exampleName == null) {
            return super.template();
        }

        AWExampleApi exampleElement = findExampleApi(exampleName);

        AWConcreteTemplate template = new AWConcreteTemplate();

        template.add(exampleElement);

        return template;
    }

    private AWExampleApi findExampleApi (String exampleName)
    {
        AWApi currentApi = componentDefinition().componentApi();

        for(AWExampleApi currentExample : currentApi.exampleApis()) {
            if(currentExample.name() == exampleName) {
                return currentExample;
            }
        }
        return null;
    }
}
