package app;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.widgets.ModalPageWrapper;
import ariba.ui.widgets.ChooserState;
import ariba.util.core.MapUtil;

import java.util.Map;

public class ContinentChooser extends AWComponent
{
    public static Continent[] Continents = Continent.values();
    private static Map<Continent, String> Coordinates;
    static {
        Coordinates = MapUtil.map();
        Coordinates.put(Continent.NorthAmerica, "200,100,50");
        Coordinates.put(Continent.SouthAmerica, "250,250,50");
        Coordinates.put(Continent.Europe, "450,75,30");
        Coordinates.put(Continent.Asia, "550,100,50");
        Coordinates.put(Continent.Africa, "400,200,50");
        Coordinates.put(Continent.Australia, "650,250,30");
        Coordinates.put(Continent.Antarctica, "400,400,20");
    }
    private ChooserState _chooserState;
    public Continent _currentContinent;

    public boolean isClientPanel()
    {
        return true;
    }

    public void setup (ChooserState chooserState)
    {
        _chooserState = chooserState;
    }

    public String currentCoords ()
    {
        return Coordinates.get(_currentContinent);
    }

    public AWComponent continentClicked ()
    {
        _chooserState.setSelectionState(_currentContinent, true);
        ModalPageWrapper.prepareToExit(this);
        return ModalPageWrapper.returnPage(this);
    }
}
