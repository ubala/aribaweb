package ariba.awreload.jmti;

import java.lang.instrument.Instrumentation;

/**
 * Java 1.5 preMain agent
 * Can be used with -javaagent:awreload.jar
 */
public class Agent {

    /**
     * The instrumentation instance
     */
    private static Instrumentation _instrumentation;

    /**
     * JSR-163 preMain Agent entry method
     */
    public static void premain(String options, Instrumentation instrumentation) {
        _instrumentation = instrumentation;
    }

    /**
     * Returns the Instrumentation system level instance
     */
    public static Instrumentation getInstrumentation() {
        if (_instrumentation == null) {
            throw new UnsupportedOperationException("Java 5 was not started with preMain -javaagent");
        }
        return _instrumentation;
    }
}
