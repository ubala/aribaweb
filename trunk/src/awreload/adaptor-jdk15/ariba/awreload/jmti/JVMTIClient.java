package ariba.awreload.jmti;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;

public class JVMTIClient
{
    public JVMTIClient() {
        if (!Agent.getInstrumentation().isRedefineClassesSupported()) {
            throw new UnsupportedOperationException("This Java 5 does not support JVMTI redefine()");
        }
    }

    // hotswap (ClassDefinition[] defns, Boolean[]succeeded)

    public void hotswap (ClassDefinition[] changes, Boolean[] succeeded)
    {
        Instrumentation instrumentation = Agent.getInstrumentation();
        if (!instrumentation.isRedefineClassesSupported()) {
            //TODO - should we fail ?
            return;
        }

        // try to reload them all
        try {
            instrumentation.redefineClasses(changes);
            for (int i=0; i<changes.length; i++) succeeded[i] = true;
        } catch (Exception e) {
            // throw new AWGenericException(e);
            if (changes.length == 1) {
                succeeded[0] = false;
            } else {
                // we failed to load one or more, so try to load them individually
                for (int i=0; i<changes.length; i++) {
                    ClassDefinition[] change = new ClassDefinition[] {changes[i]};
                    try {
                        instrumentation.redefineClasses(change);
                        succeeded[i] = true;
                    } catch (Exception e2) {
                        succeeded[i] = false;
                    }
                }
            }
        }
    }
}
