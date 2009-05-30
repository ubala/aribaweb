/* Issue - model class */
package model

import ariba.appcore.*
import ariba.ui.meta.annotations.*
import ariba.ui.meta.annotations.Property.*
import ariba.ui.meta.persistence.*
import ariba.appcore.annotations.*
import org.compass.annotations.*
import ariba.ui.meta.annotations.Property.Visible

import javax.persistence.*
import java.util.*

@Entity @NavModuleClass @Searchable
@DefaultAccess @AnonymousAccess([Permission.ClassOperation.view])
class Issue
{
   @Id @GeneratedValue @SearchableId
   private Long id

   @SearchableProperty @Trait.LabelField
   String title

   @ManyToOne User owner

   Priority priority
   Status status

   @ManyToOne Category category

   @OneToMany(cascade = CascadeType.ALL) @SearchableComponent
   Set <Note> notes

   void addToNotes (Note note)
   {
       if (!notes) notes = []
       notes += note
   }
  
   void setCategory (Category cat)
   {
       category = cat
       if (!this.owner) this.owner = cat.defaultOwner
   }

   @Action(message="Owner updated") @Visible('${properties.editing}')
   void assignToMe ()
   {
       owner = User.currentUser()
       status = Status.Assigned
   }
}
enum Status {Unassigned, Assigned, Closed}
enum Priority {Low, Medium, High, Critical}
