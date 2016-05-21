package models;

import java.sql.Date;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import com.avaje.ebean.Model;

@Entity
public class Child extends Model {
	
	private String firstName;
	
	private String lastName;
	
	private Date birthDate;
	
	private String gender;
	
	@OneToOne
	@JoinColumn(name="child_id")
	private ChildLogin childLogin;

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

	public Date getBirthDate() {
		return birthDate;
	}
	
	/**
	 * 
	 * @return the date in the format yyyy-mm-dd
	 */
	public String getBirthDateFormatted() {
		return birthDate.toString();
	}

	public void setBirthDate(Date birthDate) {
		this.birthDate = birthDate;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	/**
	 * @param childLogin the childLogin to set
	 */
	public void setChildLogin(ChildLogin childLogin) {
		this.childLogin = childLogin;
	}

	/**
	 * @return the childLogin
	 */
	public ChildLogin getChildLogin() {
		return childLogin;
	}	

}
