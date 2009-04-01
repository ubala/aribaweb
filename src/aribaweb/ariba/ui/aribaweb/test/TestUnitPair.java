package ariba.ui.aribaweb.test;

/**
 * Created by IntelliJ IDEA.
 * User: jherzog
 * Date: Mar 2, 2009
 * Time: 12:59:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestUnitPair {
    private TestUnit _leftUnit;
    private TestUnit _rightUnit;

    TestUnitPair (TestUnit leftUnit, TestUnit rightUnit)
    {
        _leftUnit = leftUnit;
        _rightUnit = rightUnit;
    }

    public TestUnit getLeftUnit ()
    {
        return _leftUnit;
    }

    public TestUnit getRightUnit ()
    {
        return _rightUnit;
    }

    public boolean hasRightUnit ()
    {
        return _rightUnit != null;
    }    
}
