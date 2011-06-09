package no.altconsult.SigncryptedSMS;

public class ModelSMSThreadRow {
	private String id;
	private String contactId;
	private String displayname;
	private String number;
	private String readMessages;
	private String totalMessages;
	private long date;
	private long picture;
	private String messageTeaser;
	public ModelSMSThreadRow(String id,String contactId, String displayname, String number,
			String readNumbersRead, String totalMessages, long date, long picture, String messageTeaser) {
		super();
		this.id = id;
		this.contactId = contactId;
		this.displayname = displayname;
		this.number = number;
		this.readMessages = readNumbersRead;
		this.totalMessages = totalMessages;
		this.date = date;
		this.picture = picture;
		this.messageTeaser = messageTeaser;
	}
	public String getId(){
		return id;
	}
	public void setContactId(String contactId){
		this.contactId = contactId;
	}
	public String getContactId(){
		return contactId;
	}
	
	
	public String getDisplayname() {
		return displayname;
	}
	public String getNumber() {
		return number;
	}
	public String getReadStatus() {
		if(readMessages == null)
			return "(" + totalMessages + ")";
		
		return "(" + readMessages + 
		"/" + totalMessages + ")";
	}
	public String getDate() {
		return (String) android.text.format.DateFormat.format("dd/MM-yy", new java.util.Date(date));
	}
	public long getPicture() {
		return picture;
	}
	public String getMessageTeaser() {
		return messageTeaser;
	}
}
