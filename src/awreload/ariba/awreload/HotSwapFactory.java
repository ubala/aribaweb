package ariba.awreload;

import java.io.File;

import ariba.util.core.Assert;
import ariba.util.core.ClassUtil;
import ariba.ui.aribaweb.util.AWResource;

import java.util.List;
import java.lang.instrument.ClassDefinition;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public class HotSwapFactory
{
    // hotswap (ClassDefinition[] defns, Boolean[]succeeded)
    public static final String HotSwapMethodName = "hotswap";

    public static final Class[] HotSwapMethodParams = new Class[]{ClassDefinition[].class, Boolean[].class};

    public static class ClassSpec {
        public Class _cls;
        public byte[] _bytecode;
        public File _file;

        public ClassSpec (Class cls, byte[] bytecode, File f)
        {
            _cls = cls;
            _bytecode = bytecode;
            _file = f;
        }
    }

    private static final String JVMTI_RELOADER_CLASS_NAME =
            "ariba.awreload.jmti.JVMTIClient";

    /**
     * Try first with JDK 5 and failover on Java 1.4 HotSwap (requires native module)
     */
    private static Object _Instance = null;
    private static Method _Method = null;

    public static Object instance()
    {
        if (_Instance == null) {
            try {
                Class redefinerClass = Class.forName(JVMTI_RELOADER_CLASS_NAME);
                _Instance = redefinerClass.newInstance();
                _Method = redefinerClass.getMethod(HotSwapMethodName, HotSwapMethodParams);
                Assert.that(_Method != null, "Can't find hotswap method on class %s", redefinerClass);
            } catch (Throwable t) {
                System.out.println("Exception initializing AWRelaod: " + t);
            }
        }
        return _Instance;
    }

    public static void hotswap (List<ClassSpec> classSpecs)
    {
        if (_Method == null) return;

        ClassDefinition[] changes = new ClassDefinition[classSpecs.size()];
        for (int i=0; i<classSpecs.size(); i++) {
            ClassSpec spec = classSpecs.get(i);
            changes[i] = new ClassDefinition(spec._cls, spec._bytecode);
        }
        Boolean[] succeeded = new Boolean[classSpecs.size()];
        try {
            _Method.invoke(_Instance, changes, succeeded);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return;
        }

        JavaReloadClassLoader loader = (JavaReloadClassLoader)ClassUtil.getClassFactory();
        for (int i=0; i<classSpecs.size(); i++) {
            ClassSpec spec = classSpecs.get(i);
            if (succeeded[i]) {
                loader.loadSucceeded(spec);
            } else {
                loader.loadFailed(spec);
            }
        }
    }
}
