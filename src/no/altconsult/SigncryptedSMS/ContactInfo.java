package no.altconsult.SigncryptedSMS;

/**
 * A model object containing contact data.
 */
public class ContactInfo {

    private int id;
	private String mDisplayName;
    private String mPhoneNumber;
    private long pictureId;
	public ContactInfo(int id, String mDisplayName, String mPhoneNumber) {
		super();
		this.id = id;
		this.mDisplayName = mDisplayName;
		this.mPhoneNumber = mPhoneNumber;
	}
	public ContactInfo(){
		
	}
	
	public int getId() {
		return id;
	}
	public String getmDisplayName() {
		if(mDisplayName == null)
			return "not in contacts";
		return mDisplayName;
	}
	public String getmPhoneNumber() {
		if(mPhoneNumber == null)
			return "not in contacts";
		return mPhoneNumber;
	}
	public void setId(int id) {
		this.id = id;
	}
	public void setmDisplayName(String mDisplayName) {
		this.mDisplayName = mDisplayName;
	}
	public void setmPhoneNumber(String mPhoneNumber) {
		this.mPhoneNumber = mPhoneNumber;
	}
	public void setPic(long id){
		pictureId = id;
	}
	public long getPicId(){
		return pictureId;
	}
	

}