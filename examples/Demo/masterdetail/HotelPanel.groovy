package masterdetail

import ariba.ui.aribaweb.core.AWComponent

class HotelPanel extends AWComponent
{
    public boolean isStateless() { false }

    def hotelProp;
    def item;

    // fake items (as maps...)
    def expenseTypes = [
        [Type:"Hotel", Amount:0.0, RoomTax:0.0, OtherTax:0.0],
        [Type:"Breakfast", Amount:0.0],
        [Type:"Lunch", Amount:0.0],
        [Type:"Laundry", Amount:0.0]
    ];

    def isHotel () { return item.Type == "Hotel" }

    // Fake row objects that we can feed the nested table.  Enough info to tell us what to get from the parent hotel item
    def hotelTaxProperites = [
        [label:"Room Tax", key:"RoomTax"],
        [label:"Other Tax", key:"OtherTax"]
    ];

    // Accessor for the Amount field -- forward to the parent hotel item to get the right value
    // you'd use FieldValue.get() here... (not item[])
    def hotelTaxAmount () { return item[hotelProp.key]; }
    def setHotelTaxAmount (amt) { item[hotelProp.key] = amt; }

    def okAction ()
    {
        // show some page -- just returning current page in this example
        return null;
    }
}
