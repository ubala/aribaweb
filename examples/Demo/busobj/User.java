package busobj;

import ariba.ui.validation.ChoiceSourceRegistry;
import ariba.ui.meta.annotations.*;
import ariba.ui.meta.annotations.Trait.*;
import ariba.ui.meta.annotations.Property.*;
import ariba.util.core.Date;

import java.math.BigDecimal;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.ArrayList;
import java.io.File;

@Localized
public class User extends Person
{
    public enum AccountStatus { PendingReview, Active, Suspended }

    @Secret String password;
    BigDecimal accountBalance;
    @Properties("trait:enumeration; choices: [10, 20, 30, 40, 50, 60, 70, 80, 90, 100];")
    int rating = 50;
    @RichText String bio;
    AccountStatus _accountStatus = AccountStatus.Active;
    List <Permission> _permissions;
    public File fullProfile;
    @Traits("ownedToMany") List<Project>_projects;

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
        return _accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus)
    {
        _accountStatus = accountStatus;
    }

    public List<Permission> getPermissions()
    {
        return _permissions;
    }

    public void setPermissions(List<Permission> permissions)
    {
        _permissions = permissions;
    }

    public List<Project> getProjects()
    {
        return _projects;
    }

    public void setProjects(List<Project> projects)
    {
        _projects = projects;
    }

    public boolean isSuspended () { return _accountStatus == AccountStatus.Suspended; }

    @Action(message="Account Suspended")
    @Visible("${!object.isSuspended}")
    public void suspendAccount ()
    {
        _accountStatus = AccountStatus.Suspended;
    }

    @Action(message="Account Re-activated")
    @Visible("${object.isSuspended}")
    public void reinstateAccount ()
    {
        _accountStatus = AccountStatus.Active;
    }

    @Action public void doIt () {}

    public boolean hasPermissions (List<String> permissions)
    {
        // N^2
        for (String p : permissions) {
            for (Permission hasPerm : _permissions) {
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
        // prefab a user with some interesting data
        _EffectiveUser = new User("Test", "User", 60);
        _EffectiveUser.setPermissions(Arrays.asList(Permission.UserAdmin, Permission.SourcingSearch));
        List<Project> projects = new ArrayList();
        projects.add(new Project("Project X", "A mysterious project",
                        new Date(2008, 4, 12), new BigDecimal(1234)));
        projects.add(new Project("Magnum", "A BIG project",
                        new Date(2006, 7, 23), new BigDecimal(1234567)));
        _EffectiveUser.setProjects(projects);
    }

    public static User getEffectiveUser()
    {
        return _EffectiveUser;
    }

}
 