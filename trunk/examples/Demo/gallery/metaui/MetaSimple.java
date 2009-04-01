package gallery.metaui;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.meta.annotations.Trait;
import ariba.ui.meta.annotations.Traits;
import ariba.ui.meta.annotations.Action;
import ariba.util.core.Date;

import java.math.BigDecimal;
import java.io.File;

public class MetaSimple extends AWComponent {
    public boolean isStateless() { return false; }

    public SampleModel model = new SampleModel();

    public static class SampleModel {
        public @Trait.Required String title;
        public Choices shirtSize = Choices.Medium;
        public BigDecimal price = new BigDecimal(10.50);
        public int quantity = 1;
        public double discount = 0.0;
        public boolean expediteShipping;
        public Date needBy;
        public @Trait.RichText String description;
        public @Traits("imageData") byte[] logo;
        public File otherInstructions;

        @Action
        public void doSomething () { System.out.println("I did it!"); }
    }
    public static enum Choices { Small, Medium, Large, ExtraLarge }
}
