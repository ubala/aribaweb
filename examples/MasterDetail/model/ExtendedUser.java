package model;

import ariba.appcore.User;
import ariba.ui.meta.annotations.NavModuleClass;
import ariba.ui.meta.annotations.SupercedesSuperclass;

import javax.persistence.Entity;

import org.compass.annotations.Searchable;
import org.compass.annotations.SearchableProperty;

/**
    Example of a class that extends and replaces ("poses as") a parent entity
 */
@Entity
@NavModuleClass
@SupercedesSuperclass
@Searchable
public class ExtendedUser extends User
{
    @SearchableProperty
    String additionalField;

    public String getAdditionalField ()
    {
        return additionalField;
    }

    public void setAdditionalField (String additionalField)
    {
        this.additionalField = additionalField;
    }
}
