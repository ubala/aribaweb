package example.busobj;

import ariba.util.core.Date;
import ariba.util.core.Fmt;
import ariba.ui.meta.annotations.NavModuleClass;
import ariba.ui.meta.annotations.Properties;
import ariba.ui.meta.annotations.Traits;
import ariba.ui.meta.annotations.Trait.*;

import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Entity;

@Entity
public class Person
{
    @Id @GeneratedValue
    private Long id;

    String firstName;
    String lastName;
    String email;
    String phoneNumber;
    Date birthDay;

    public Person () {}

    public Person (String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }
    
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @LabelField
    public String getName () {
        return Fmt.S("%s %s", firstName, lastName);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Date getBirthDay() {
        return birthDay;
    }

    public void setBirthDay(Date birthDay) {
        this.birthDay = birthDay;
    }
}