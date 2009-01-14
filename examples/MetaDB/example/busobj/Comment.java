package example.busobj;

import ariba.ui.meta.annotations.Traits;
import ariba.ui.meta.annotations.Trait.*;
import ariba.ui.meta.annotations.Properties;

import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Entity;

@Entity
public class Comment
{
    @Id
    @GeneratedValue
    private Long id;

    @RichText
    protected String text;

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    // Maybe add list of attachments?

    // Maybe add replies (hierarchy)
}
