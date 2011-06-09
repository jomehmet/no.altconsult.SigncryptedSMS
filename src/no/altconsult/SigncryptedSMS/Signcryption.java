package no.altconsult.SigncryptedSMS;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import no.altconsult.signcryption.Ascii85Coder;
import no.altconsult.signcryption.FieldType;
import no.altconsult.signcryption.KeyLength;
import no.altconsult.signcryption.Signcrypt;
import no.altconsult.signcryption.SigncryptionManager;
import no.altconsult.signcryption.SigncryptionSettings;
import no.altconsult.signcryption.Unsigncrypt;

public class Signcryption extends SigncryptionManager{
	
	public static final String PuKeySMSid = "<PuKey>";
	public static final String PuKeySMS_pending= "<pending>";
	public static final String PuKeySMS_verified= "<verified>";
	public static final String SigncryptedSMSid = "<SSMS>";
	public static final String PrKeyid = "<PrKey>";
	public static final String PuKeyNumberDividor = "<~@~>";
	private static final SigncryptionSettings settings = 
		new SigncryptionSettings((byte)0xAA,(byte)1,
				FieldType.P384, KeyLength.key256);
	private String privateKeyPassword;
	private Unsigncrypt us;
	/**
	 * If user dosen't have key pair.
	 */
	public Signcryption() {
		super(settings);
		generateKeyPair();
	}
	/**
	 * User has Ascii85 AES encrypted Private Key
	 * 
	 * @param Pk
	 */
	public Signcryption(String Pk, String password) {
		super(settings);
		setKeyPairFromEncryptedPrivateKey(Pk, password);
		setPrivateKeyPassword(password);
	}
	
	public Signcryption(String PrK) {
		super(settings);
		setKeyPair(PrK);
	}
	public String getPrivateKeyPassword(){
		return privateKeyPassword;
	}
	public void setPrivateKeyPassword(String p){
		privateKeyPassword = p;
	}
	public String getPublicKeyAsSMSMessage(){
		return PuKeySMSid + getPublicKeyAsAscii85();
	}
	public boolean isPublicKeySMSMessage(String msg){
		if(msg.length()< PuKeySMSid.length())
			return false;
		return msg.substring(0, PuKeySMSid.length()).equals(PuKeySMSid);
	}
	public boolean isPrivateKeyAscii85(String msg){
		if(msg.length()< PrKeyid.length())
			return false;
		return msg.substring(0, PrKeyid.length()).equals(PrKeyid);
	}
	public boolean isSigncryptedSMSMessage(String msg){
		if(msg.length()< SigncryptedSMSid.length())
			return false;
		return msg.substring(0, SigncryptedSMSid.length()).equals(SigncryptedSMSid);
	}
	public boolean isPublicKeyPending(String msg){
		if(msg.length()< PuKeySMS_pending.length())
			return false;
		return msg.substring(0, PuKeySMS_pending.length()).equals(PuKeySMS_pending);
	}
	public boolean isPublicKeyVerified(String msg){
		if(msg.length()< PuKeySMS_verified.length())
			return false;
		return msg.substring(0, PuKeySMS_verified.length()).equals(PuKeySMS_verified);
	}
	public String extractSigncryptedAscii85FromSMSMessage(String msg){
		return msg.substring(SigncryptedSMSid.length());
	}
	public String extractPublicKeyAscii85FromSMSMessage(String msg){
		return msg.substring(PuKeySMSid.length());
	}
	public String extractPrivateKeyAscii85FromText(String msg){
		return msg.substring(PrKeyid.length());
	}
	public String extractPublicKeyPending(String msg){
		return msg.substring(PuKeySMS_pending.length());
	}
	public String extractPublicKeyVerified(String msg){
		return msg.substring(PuKeySMS_verified.length());
	}
	/**
	 * Makes a <SSMS><-ascii85 encoded signcrypted message->
	 * 
	 * @param msg
	 * @param ascii85PrivateKey_sender
	 * @param ascii85PublicKey_receiver
	 * @return
	 */
	public String signcryptMessage(String msg, 
			String ascii85PrivateKey_sender, 
			String ascii85PublicKey_receiver){
		Signcrypt sc = new Signcrypt(
				getPrivateKey(),
				getPublicKeyFromAscii85(ascii85PublicKey_receiver),
				msg, 
				settings);
		byte[] bytes = sc.getSignCryptPacket().getPacketAsBytes();
		String ascii85 = Ascii85Coder.encodeBytesToAscii85(bytes);
		return SigncryptedSMSid + ascii85;
	}
	public String unsigncryptMessage(String msg,
			String ascii85PrivateKey_receiver, 
			String ascii85PublicKey_sender){
		msg = extractSigncryptedAscii85FromSMSMessage(msg);
		byte[] bytes = Ascii85Coder.decodeAscii85StringToBytes(msg);
		us = new Unsigncrypt(
				getPublicKeyFromAscii85(ascii85PublicKey_sender), 
				getPrivateKey(), 
				bytes, 
				settings); 
		return us.getStringMessage();
	}
	/**
	 * Depends on unsigncryptMessage(String msg,
			String ascii85PrivateKey_receiver, 
			String ascii85PublicKey_sender)
	 * @return
	 */
	public long getTimeStamp(){
		if(us == null)
			return 0;
		return us.getUnixTimeStamp();
	}
	public String getPublicKeyFingerPrint(String ascii85key){
		if(isPublicKeyPending(ascii85key))
			ascii85key = extractPublicKeyPending(ascii85key);
		if(isPublicKeyVerified(ascii85key))
			ascii85key = extractPublicKeyVerified(ascii85key);
		if(isPublicKeySMSMessage(ascii85key))
			ascii85key = extractPublicKeyAscii85FromSMSMessage(ascii85key);
		MessageDigest digest;
			try {
				digest = MessageDigest.getInstance("SHA-256");
				digest.update(Ascii85Coder.decodeAscii85StringToBytes(ascii85key));
				return new BigInteger(digest.digest()).abs().toString();
			} catch (NoSuchAlgorithmException e) {
				System.out.println(e.getMessage());
			}
		return "error";
	}
    public String fixSnippet(String snippet){
    	if(snippet == null)
    		return "";
    	if(isSigncryptedSMSMessage(snippet))
    		return "SigncryptedSMS";
    	if(isPublicKeySMSMessage(snippet))
    		return "Public Key";
    	if(snippet.length() >20)
    		return snippet.substring(0, 20) + "...";
    	return snippet;
    }
    public String isPublicKeyMessage(String msg){
    	if(isPublicKeySMSMessage(msg))
    		return "Public Key";
    	return msg;
    }
    
    public String getAddressFromQRCode(String str){	
    	//return java.util.Arrays.toString(str.split("<|>"));
    	try{
    	return str.split(PuKeyNumberDividor)[0];
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    	return "0";
    }
    public String getPublicKeyFromQRCode(String str){	
    	try{
        	return str.split(PuKeyNumberDividor)[1];
        	}catch(Exception e){
        		e.printStackTrace();
        	}
        return "";
    }
}









