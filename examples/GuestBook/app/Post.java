package app;

import ariba.ui.meta.annotations.Trait;
import ariba.ui.meta.annotations.Property;
import ariba.ui.meta.annotations.Action;
import ariba.ui.meta.annotations.NavModuleClass;
import java.util.Date;

@NavModuleClass
public class Post {

    @Trait.Required
    @Property.Label("Name")
    @Property.Editable("${properties.editing && value!='admin'}")    
    public String userName;

    @Property.Valid("${object.isValidBirthday}")
    public Date birthday;

    @Trait.LabelField
    public String title;

    @Trait.RichText
    @Property.Visible("${properties.editing || !object.isPrivate}")
    public String comment;

    public int rating = 3;

    public boolean isPrivate;

    @Trait.Required
    public Continent continent;

    public Object isValidBirthday () {
        if (birthday == null || birthday.before(new java.util.Date())) {
            return true;
        }
        return "Birthday cannot be in the future";        
    }

    @Action(message="isPrivate set to %s")
    public boolean toggleIsPrivate () {
        isPrivate = !isPrivate;
        return isPrivate;        
    }

}
