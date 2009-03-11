/*
    Issue - model class
*/
package model

import ariba.appcore.*
import ariba.ui.meta.annotations.*
import javax.persistence.*
import java.util.*
import ariba.ui.meta.annotations.Property.Visible
import ariba.ui.meta.persistence.ObjectContext
import ariba.util.core.ListUtil
import javax.mail.internet.InternetAddress
import javax.mail.Message.RecipientType
import org.compass.annotations.*
import javax.mail.internet.MimeMessage
import ariba.appcore.annotations.*

@Entity @NavModuleClass @Searchable
@DefaultAccess @AnonymousAccess([Permission.ClassOperation.view])
class Issue
{
    @Id @GeneratedValue @SearchableId
    private Long id

    @SearchableProperty @Trait.LabelField
    String subject

    @Visible("false")
    String subjectKey

    @ManyToOne User owner
    @ManyToOne Category category
    Priority priority
    Status status
    Date lastModified
    @ManyToOne User submitter
    Date created

    @OneToMany @SearchableComponent
    List <Note> notes = ListUtil.list()

    void init (String subject)
    {
        this.subject = subject.replaceAll("(?i)((^re:)|(^fwd:))\\s*", "")
        this.subjectKey = subjectToKey(subject)
        lastModified = created = new Date()
    }

    @Traits("searchable")
    Long getId () { id }

    List <Note> getNotes() { notes }

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
    
    void addToNotes (Note note)
    {
        notes.add(note)
        if (!submitter) submitter = note.sender
        if (!lastModified || lastModified.time < note.date.time) lastModified = note.date
    }

    // Try to find the default category based on the recepient addresses and assign
    void autoAssign (javax.mail.Address[] recipients)
    {
        if (category) return;
        for (InternetAddress address : recipients) {
            Category cat = Category.findByAddress(address.getPersonal()) ?:
                           Category.findByAddress(address.getAddress())
            if (cat) { setCategory(cat); return; }
        }
    }

    static void processMessage (MimeMessage message)
    {
        ObjectContext context = ObjectContext.get()
        Issue issue = Issue.findOrCreate(message.subject)
        Note note = context.create(Note.class)
        note.init(message)
        issue.addToNotes(note)
        issue.autoAssign(message.getRecipients(RecipientType.TO))
        issue.autoAssign(message.getRecipients(RecipientType.CC))

        context.save()
    }
    
    static String subjectToKey (String subject)
    {
        String key = subject.toLowerCase()
        key = key.replaceAll("\\s+", " ").replaceAll("((^re:)|(^fwd:))\\s*", "")
        return key
    }

    static Issue find (String key)
    {
        return ObjectContext.get().findOne(Issue.class, [subjectKey:key])
    }

    static Issue create (String subject)
    {
        Issue issue = ObjectContext.get().create(Issue.class)
        issue.init(subject)
        return issue
    }

    static Issue findOrCreate (String subject)
    {
        return find(subjectToKey(subject)) ?: create(subject)
    }
}

enum Status {Unassigned, Assigned, Closed}
enum Priority {Low, Medium, High, Critical}
