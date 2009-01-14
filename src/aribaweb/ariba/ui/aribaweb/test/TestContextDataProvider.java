package ariba.ui.aribaweb.test;

import ariba.ui.aribaweb.core.AWSession;

/**
 * Implementators should
 * @aribaapi private
 */
public interface TestContextDataProvider {

	public void register(TestContext tc, AWSession session);

	public void unregister(TestContext tc, AWSession session);

	public Object resolve(TestContext tc, Object obj);

}
