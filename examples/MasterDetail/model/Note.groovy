/* Note - model class */
package model

import ariba.appcore.*
import ariba.ui.meta.annotations.*
import ariba.ui.meta.annotations.Property.*
import ariba.ui.meta.persistence.*
import ariba.appcore.annotations.*
import org.compass.annotations.*

import javax.persistence.*
import java.util.*

@Entity @Searchable(root=false)
@DefaultAccess @AnonymousAccess([Permission.ClassOperation.view])
class Note
{
    @Id @GeneratedValue
    private Long id

    @SearchableProperty @Trait.LabelField
    String subject

    @ManyToOne User from = User.currentUser();

    Date date = new Date();

    @SearchableProperty @Trait.RichText
    String description
}
