package model

import ariba.ui.meta.annotations.*
import javax.persistence.*
import ariba.util.core.*
import java.util.*
import ariba.ui.meta.annotations.Trait.LabelField
import ariba.ui.meta.persistence.ObjectContext
import ariba.appcore.*
import org.compass.annotations.Searchable
import org.compass.annotations.SearchableId
import org.compass.annotations.SearchableProperty

@Entity @NavModuleClass  @Searchable
class Category {
    @Id @GeneratedValue @SearchableId
    private Long id

    @LabelField @SearchableProperty
    String name

    @ManyToOne
    User defaultOwner

    String addressMatchPattern

    // ToDo: more flexible matching
    static Category findByAddress (String address)
    {
        return StringUtil.nullOrEmptyString(address) ? null :
                    ObjectContext.get().findOne(Category.class, [addressMatchPattern:address])
    }
}
