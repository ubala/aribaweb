package ariba.ui.aribaweb.test;

import ariba.ui.aribaweb.core.AWSession;

public class TestContextDataProviderFactory {

	private static final String DEFAULT_TEST_CONTEXT_DATA_PROVIDER = 
					"ariba.test.base.testlistener.BaseTestContextDataProvider";

	private static TestContextDataProviderFactory singleton;

	private TestContextDataProvider provider;

	public static TestContextDataProviderFactory getInstance() {
		if (singleton == null) {
			synchronized (TestContextDataProviderFactory.class) {
				if (singleton == null) {
					singleton = new TestContextDataProviderFactory();
				}
			}
		}
		return singleton;
	}

	public TestContextDataProvider getProvider() {
	    if (provider == null) {
	        synchronized (TestContextDataProviderFactory.class) {
	            if (provider == null) {
            		try {
            			//TODO implement the configuration of this property
            			Class clazz = this.getClass().getClassLoader().loadClass(
            				DEFAULT_TEST_CONTEXT_DATA_PROVIDER);
            			provider = (TestContextDataProvider)clazz.newInstance();
            		} catch (ClassNotFoundException e) {
            			//TODO
            			e.printStackTrace();
            			throw new RuntimeException(e);
            		} catch (InstantiationException e) {
            			e.printStackTrace();
            			throw new RuntimeException(e);
            		} catch (IllegalAccessException e) {
            			e.printStackTrace();
            			throw new RuntimeException(e);
            		}
	            }
	        }
	    }
	    return provider;
	}
}
