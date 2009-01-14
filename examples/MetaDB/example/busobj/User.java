package example.busobj;

import ariba.ui.validation.ChoiceSourceRegistry;
import ariba.ui.meta.annotations.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Transient;
import java.math.BigDecimal;
import java.util.List;
import java.util.Arrays;
import java.util.Map;

@Entity
@NavModuleClass
public class User extends Person
{
    public enum AccountStatus { PendingReview, Active, Suspended }

    @Traits("secret") String password;
    BigDecimal accountBalance;
    @Properties("traits:enumeration; choices: [0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100];")
    int rating;
    @Traits("richtext") String bio;
    AccountStatus accountStatus = AccountStatus.Active;
    @Transient
    List <Permission> permissions;

    public User () {}

    public User (String firstName, String lastName, int rating) {
        super(firstName, lastName);
        this.rating = rating;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public BigDecimal getAccountBalance() {
        return accountBalance;
    }

    public void setAccountBalance(BigDecimal acoountBalance) {
        this.accountBalance = acoountBalance;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public AccountStatus getAccountStatus()
    {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus)
    {
        this.accountStatus = accountStatus;
    }

    public List<Permission> getPermissions()
    {
        return permissions;
    }

    public void setPermissions(List<Permission> permissions)
    {
        this.permissions = permissions;
    }
    
    public boolean isSuspended () { return accountStatus == AccountStatus.Suspended; }

    @Action(message="Account Suspended")
    @Properties("visible:${!object.isSuspended}")
    public void suspendAccount ()
    {
        accountStatus = AccountStatus.Suspended;
    }

    @Action(message="Account Re-activated")
    @Properties("visible:${object.isSuspended}")
    public void reinstateAccount ()
    {
        accountStatus = AccountStatus.Active;
    }

    public boolean hasPermissions (List<String> permissions)
    {
        // N^2
        for (String p : permissions) {
            for (Permission hasPerm : this.permissions) {
                if (hasPerm.name().equals(p)) return true;
            }
        }
        return false;
    }

    /* Bogus test example of a chooser source */
    static List<User> _AllUsers = Arrays.asList(
                new User("Fred", "Flintstone", 98),
                new User("Barney", "Rubble", 58),
                new User("Betty", "Rubble", 88),
                new User("Wilma", "Flintstone", 78),
                new User("Bam Bam", "Rubble", 32),
                new User("Dino", "Flintstone", 13)
        );

    static {
        ChoiceSourceRegistry.registerProvider(User.class.getName(),
                new ChoiceSourceRegistry.Provider() {
                    public Object choiceSourceForParams (String className, Map context) {
                        return _AllUsers;
                    }
                });
    }

    public enum Permission {
        UserAdmin,
        SourcingCreate,
        SourcingSearch
    }

    // Would normally be thread local...
    static User _EffectiveUser;

    static {
        _EffectiveUser = new User("Test", "User", 100);
        _EffectiveUser.setPermissions(Arrays.asList(Permission.UserAdmin, Permission.SourcingSearch));
    }

    public static User getEffectiveUser()
    {
        return _EffectiveUser;
    }

}
 