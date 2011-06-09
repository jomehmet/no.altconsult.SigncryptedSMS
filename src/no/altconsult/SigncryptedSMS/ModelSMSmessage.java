package no.altconsult.SigncryptedSMS;

import java.sql.Date;

public class ModelSMSmessage {
	public ModelSMSmessage(long signcryptionTimeStamp, 
			long localTimeStamp,String senderName, String body, 
			boolean isSignCrypted, boolean isInCommingMessage) {
		
		this.signcryptionTimeStamp =  signcryptionTimeStamp;
		this.localTimeStamp = localTimeStamp;
		this.senderName = senderName;
		this.body = body;
		this.isSignCrypted = isSignCrypted;
		this.isInCommingMessage = isInCommingMessage;
	}
	private String senderName;
	private long signcryptionTimeStamp;
	private long localTimeStamp;
	private String body;
	private boolean isSignCrypted;
	private boolean isInCommingMessage;
	
	public String getSigncryptionStatusMessage() {
		String res ="";
		if(isSignCrypted()){
			res += "Signcrypted by " + senderName + " ";
			res += getSigncryptionTimeStamp();
		}else{
			res += "Message is not signcrypted";
		}
		return res;
	}
	private String getSigncryptionTimeStamp(){
		return new Date(signcryptionTimeStamp).toLocaleString();
	}
	public String getlocalTimeStamp() {
		return new Date(localTimeStamp).toLocaleString();
	}
	public String getSenderName() {
		return senderName;
	}
	public String getBody() {
		return body;
	}
	public boolean isSignCrypted() {
		return isSignCrypted;
	}
	public boolean isInCommingMessage() {
		return isInCommingMessage;
	}
}
