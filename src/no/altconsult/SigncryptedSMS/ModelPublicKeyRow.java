package no.altconsult.SigncryptedSMS;

public class ModelPublicKeyRow {
	private String contact_id;
	private String name;
	private String number;
	private boolean isVerified;
	private String publicKey;
	public ModelPublicKeyRow(String contact_id, String publicKey, String name, String number,
			boolean isVerified) {
		super();
		this.publicKey = publicKey;
		this.contact_id = contact_id;
		this.name = name;
		this.number = number;
		this.isVerified = isVerified;
	}
	public String getContact_id() {
		return contact_id;
	}
	public String getName() {
		return name;
	}
	public String getPublicKey() {
		return publicKey;
	}
	public String getNumber() {
		return number;
	}
	public boolean isVerified() {
		return isVerified;
	}
	public void setVerified(boolean isVerified){
		this.isVerified = isVerified;
	}
}
