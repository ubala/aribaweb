/*
    Master - model class
*/
package model

import ariba.appcore.*
import ariba.ui.meta.annotations.*
import ariba.util.core.*
import ariba.ui.meta.annotations.Property.*
import ariba.ui.meta.persistence.*
import ariba.appcore.annotations.*
import org.compass.annotations.*

import javax.persistence.*
import java.util.*

@Entity @NavModuleClass @Searchable
@DefaultAccess @AnonymousAccess([Permission.ClassOperation.view])
class Master
{
    @Id @GeneratedValue @SearchableId
    private Long id

    @SearchableProperty @Trait.LabelField
    String title

    @ManyToOne User owner = User.currentUser();

    @OneToMany @SearchableComponent
    List <Detail> details = ListUtil.list()
}
