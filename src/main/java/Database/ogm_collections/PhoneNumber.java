package Database.ogm_collections;

import javax.persistence.Embeddable;

@Embeddable
public class PhoneNumber {

    private String number;

    public PhoneNumber() { }
    public PhoneNumber(String number){
        this.number = number;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}
