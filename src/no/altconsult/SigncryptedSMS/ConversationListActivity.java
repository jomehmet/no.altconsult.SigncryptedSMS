package no.altconsult.SigncryptedSMS;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.logging.Logger;

import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.TextView;

public class ConversationListActivity extends ListActivity {
    
	//public static final Logger Log = Logger.getLogger("SigncryptedSMS");
	//public static final String TAG = "Conversation";
	private ArrayList<ModelSMSmessage> modelSMSmessage;
	private ConversationAdapter adapter;
	private int threadNumber;
	private int currentContactId;
	private String currentAddress;
	private ContactInfo currentContact;
	private TextView txtDisplayName;
	private TextView txtNumber;
	private EditText edit_new_msg;
	private Button btn_current_contact;
	private Button btn_send;
	private static Signcryption signcryption = MainActivity.signcryption;
	public BroadcastReceiver smsReceiver;
	private String currentMessage;
	private String currentMessageClear;
	private ImageView icon;
	private final static int DIALOG_SHOW_FINGERPRINT = 1;
	private final static int SELECT_CONTACT = 2;
	private final static int ADD_CONTACT = 3;
	private String note;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        try {
        	threadNumber = Integer.parseInt(getIntent().getStringExtra("thread_number"));
        	currentContactId = Integer.parseInt(getIntent().getStringExtra("contact_id"));
        	currentAddress = getIntent().getStringExtra("address");
		} catch (Exception e) {
			e.printStackTrace();
			threadNumber = -1;
		}
    	initCurrentContact();
    	renderAdapter();
        setUpBroadcastReceiver();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	unregisterReceiver(smsReceiver);
    }
    protected void onResume() {
    	super.onResume();
    	renderAdapter();
    	registerReceiver(smsReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
    }
    
    
	public void renderAdapter() {
		setContentView(R.layout.conversation);
		initViews();
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		setCurrentContact();
        btn_current_contact.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) 
            { 
            	onButtonInfoContactClick();
            	
            }
        });
        btn_send.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) 
            { 
            	sendNewMessage();
            }
        });
		initCoversationRows();
		queryConversationRows();
        setUpListBehavior();
	}
	private void onButtonInfoContactClick() {
		if(currentContact != null && currentContact.getId()!= 0){//More Info
    		showDialog(DIALOG_SHOW_FINGERPRINT);
    	}else if(currentAddress != null){
    		addNewContact();
    	}else{
    		selectContact();
    	}
	}
	public void initViews() {
		txtDisplayName = (TextView)findViewById(R.id.textView_conversation_display_name);
    	txtNumber = (TextView)findViewById(R.id.textView_conversation_number);
    	edit_new_msg = (EditText) findViewById(R.id.editText_conversation_new_message);
    	btn_current_contact =(Button)findViewById(R.id.button_conversation_more_info);
    	btn_send = (Button)findViewById(R.id.button_conversation_send);
    	icon=(ImageView)findViewById(R.id.imageView_conversation_contact_icon);
	}
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	switch (requestCode) {
		case SELECT_CONTACT:
			if(resultCode != Activity.RESULT_CANCELED){
				Uri uri = data.getData();
				if(uri != null){
					queryContactInfo(uri, false);
					setCurrentContact();
				}else{
					Toast.makeText(this, R.string.contact_has_no_phone_number, Toast.LENGTH_SHORT).show();
				}
			}
			break;
		case ADD_CONTACT:
			if(resultCode != Activity.RESULT_CANCELED){
				Uri uri = data.getData();
				if(uri != null){
					queryContactInfo(uri, false);
					setCurrentContact();
				}else{
					Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
				}
			}
			break;
		default:
			break;
		}
    }
    public void queryContactInfo(Uri uri, boolean isDirectUri){
    	currentContact = new ContactInfo();
    	String id = uri.getLastPathSegment();
    	try{
    	Cursor cursor = getContentResolver().query(  
    			isDirectUri? uri : Phone.CONTENT_URI, 
    	        null,  
    	        isDirectUri? null:Phone.CONTACT_ID + "=?",  
    	        isDirectUri? null:new String[]{id}, 
    	        null);
        
    	if(cursor.moveToFirst()){
    		/*String columns[] = cursor.getColumnNames();
           	for (String column : columns) {  
                int index = cursor.getColumnIndex(column);  
                //Log.info("QueryContactInfo:Column: " + column + " == ["  
                  //      + cursor.getString(index) + "]");  
            }*/
	        currentContact.setId(Integer.parseInt(id));
	        currentContactId = currentContact.getId();
	        currentContact.setmDisplayName(cursor.getString(cursor.getColumnIndex(Phone.DISPLAY_NAME)));
	        if(isDirectUri){
	        	currentContact.setmPhoneNumber(currentAddress);
	        }else{
	        	currentContact.setmPhoneNumber(cursor.getString(cursor.getColumnIndex(Phone.NUMBER)));
	        	currentAddress = currentContact.getmPhoneNumber();
	        } 
    	}
        cursor.close();
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }
    
    private void setUpBroadcastReceiver() {
    	 smsReceiver = new BroadcastReceiver() {
    	    	public void onReceive(Context context, Intent intent) {
    	    		renderAdapter();
    	    	}
    	    };
    	    registerReceiver(smsReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
    }
 	private void setUpListBehavior() {
		adapter = new ConversationAdapter(
				this, R.layout.row_conversation_item,
				modelSMSmessage);
		setListAdapter(adapter);
		ListView lv = getListView();
		//lv.setTextFilterEnabled(true);
		//registerForContextMenu(lv);
		lv.setSelection(modelSMSmessage.size()-1);
	}
 	private void initCurrentContact(){
 		currentContact = queryContactInfo(null , String.valueOf(currentContactId));
 	}
    public ContactInfo queryContactInfo(Uri uri, String id){
    	ContactInfo aContact = new ContactInfo();
    	if(uri == null && id == null)
    		return aContact;
    	if(id == null)
    		id = uri.getLastPathSegment();
    	try{
    	Cursor cursor = getContentResolver().query(  
    	        Phone.CONTENT_URI, null,  
    	        Phone.CONTACT_ID + "=?",  
    	        new String[]{id}, null);
        if(cursor != null)
    	if(cursor.moveToFirst()){
	        aContact.setId(Integer.parseInt(id));
	        int i = cursor.getColumnIndex(Phone.DISPLAY_NAME);
	        aContact.setmDisplayName(cursor.getString(i));
	        i = cursor.getColumnIndex(Phone.NUMBER);
	        aContact.setmPhoneNumber(cursor.getString(i));
        }
	        cursor.close();
		}catch(Exception e){
			e.printStackTrace();
		}
        
        return aContact;
    }
 	/**
 	 * Sets data to GUI and depends on currentContact
 	 * 
 	 */
 	private void setCurrentContact(){
 		if(txtDisplayName == null){
 			initViews();
 		}
 		if(currentAddress == null){//new message
 			txtDisplayName.setText("");
 			txtNumber.setText("");
 			btn_current_contact.setText(R.string.button_pick_contact);
 			btn_send.setText(R.string.send_normal);
 			btn_send.setClickable(false);
 			return;
 		}
 		if(currentContact.getId() == 0 && currentAddress != null){
 			txtDisplayName.setText("");
 			txtNumber.setText(currentAddress);
 			btn_current_contact.setText(R.string.button_add_contact);
 			btn_send.setText(R.string.send_normal);
 			btn_send.setClickable(false);
 			return;
 		}
		txtDisplayName.setText(currentContact.getmDisplayName());
		txtNumber.setText(currentAddress);
		btn_current_contact.setText(R.string.more_info);
		btn_send.setClickable(true);
		if(hasPublicKey()){
			btn_send.setText(R.string.send_signcrypted);
		}else{
			btn_send.setText(R.string.send_normal);
		}
		
		Bitmap bm = MainActivity.loadContactPhoto(getContentResolver(), (long)currentContact.getId());
		if(bm != null){
			icon.setImageBitmap(bm);
		}
 	}
 	public class ConversationAdapter extends ArrayAdapter<ModelSMSmessage> {

		public ConversationAdapter(Context context, int textViewResourceId,
				ArrayList<ModelSMSmessage> objects) {
			super(context, textViewResourceId, objects);
			// TODO Auto-generated constructor stub
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ModelSMSmessage m = (ModelSMSmessage) modelSMSmessage.get(position);
			
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.row_conversation_item, null);
			}
			LinearLayout layoutBottom = (LinearLayout)v.findViewById(R.id.linearLayout_conversation_bottom);
			LinearLayout layoutTop = (LinearLayout)v.findViewById(R.id.linearLayout_conversation_top);
			TextView txtTop =(TextView)v.findViewById(R.id.textView_conversation_item_top);
			TextView txtBody =(TextView)v.findViewById(R.id.textView_conversation_item_body);
			TextView txtBottom =(TextView)v.findViewById(R.id.textView_conversation_item_bottom);
			
			//Set graphichs
			if(m.isSignCrypted()){
				txtTop.setBackgroundDrawable(getResources().getDrawable(R.drawable.conversation_green_top));
			}else{
				txtTop.setBackgroundDrawable(getResources().getDrawable(R.drawable.conversation_red_top));
			}
			if(m.isInCommingMessage()){
				txtBody.setBackgroundDrawable(getResources().getDrawable(R.drawable.conversation_in_body));
				txtBottom.setBackgroundDrawable(getResources().getDrawable(R.drawable.conversation_in_bottom));
				layoutBottom.setPadding(0, 0, layoutTop.getPaddingLeft(), 0);
			}else{//OutGoing
				txtBody.setBackgroundDrawable(getResources().getDrawable(R.drawable.conversation_out_body));
				txtBottom.setBackgroundDrawable(getResources().getDrawable(R.drawable.conversation_out_bottom));
				txtBottom.setPadding(0, 0, txtBottom.getPaddingRight()+ layoutBottom.getPaddingRight(), 0);
				layoutBottom.setPadding(layoutTop.getPaddingRight(), 0, 0, 0);
			}
			//Set textvalues
			txtTop.setText(m.getSigncryptionStatusMessage());
			txtBody.setText(m.getBody());
			txtBottom.setText(m.getlocalTimeStamp());
			return v;
		}
	}
	private void selectContact(){
        try{
		Intent intent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
        startActivityForResult(intent, SELECT_CONTACT);
    	}catch(Exception e){
    		e.printStackTrace();
    	}
	}
	private void addNewContact(){
		Intent intent = new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI);
		intent.putExtra(ContactsContract.Intents.Insert.PHONE, currentAddress);
		intent.putExtra(ContactsContract.Intents.Insert.PHONE_ISPRIMARY, new Integer(1));
		startActivityForResult(intent, ADD_CONTACT);
	}
    private void initCoversationRows() {
		modelSMSmessage = new ArrayList<ModelSMSmessage>();
		if(threadNumber < 0){
			//new currentMessage
			return;
		}
	}
    private void queryConversationRows(){
    	if(threadNumber == 0){
    		//Log.info(TAG + "threadNumber is null");
    		return;
    	}
    	Uri uri = Uri.parse("content://sms/conversations/" + threadNumber);
    	try{
	    	Cursor cursor = getContentResolver().query(  
	    	        uri, //uri
	    	        null,//new String[]{"thread_id", "body", "read", "adress", "person","date"}, //projection-> bestemte felter som navn 
	    	        null,  //selection
	    	        null,//new String[]{id}, //Selection args
	    	        "date ASC");//Sort order
	        while(cursor.moveToNext()){  
		       /* String columns[] = cursor.getColumnNames();
		        for (String column : columns) {  
		            int index = cursor.getColumnIndex(column);  
		            Log.info(TAG +"Column: " + column + " == ["  
		                    + cursor.getString(index) + "]");  
		        }
		        Log.info("move to next");
		        */
		        String message = cursor.getString(cursor.getColumnIndex("body"));
		        boolean isIncomming = isIncommingMessage(cursor.getString(cursor.getColumnIndex("type")));
		        long timestamp = 0;
		        if(signcryption.isSigncryptedSMSMessage(cursor.getString(cursor.getColumnIndex("body")))){
		        	String PuKey = getPublicKeyFromContact();
		        	if(isIncomming){
		        		if(PuKey != null){
		        		message = signcryption.unsigncryptMessage(message, 
		        				readPrivateKey(), 
		        				PuKey);
		        		timestamp = signcryption.getTimeStamp();
		        		}else{
		        			message = "No Public Key! Message can not be shown.";
		        		}
		        	}else{
		        		timestamp = cursor.getLong(cursor.getColumnIndex("date"))/1000L;
		        		message = signcryption.extractSigncryptedAscii85FromSMSMessage(message);
		        	}
		        	modelSMSmessage.add(new ModelSMSmessage(
		        			timestamp*1000L,//signcryption timestamp
		        			cursor.getLong(cursor.getColumnIndex("date")),//received timestamp
		        			isIncomming? currentContact.getmDisplayName(): "me",//sender name
			        		message,//body
			        		true,//isSignCrypted
			        		isIncomming//isIncomming currentMessage
			        		));
		        }else{
		        	modelSMSmessage.add(new ModelSMSmessage(
		        			timestamp*1000L,//signcryption timestamp
		        			cursor.getLong(cursor.getColumnIndex("date")),//received timestamp
			        		currentContact.getmDisplayName(),//sender name
			        		signcryption.isPublicKeyMessage(message),//body
			        		false,//isSignCrypted
			        		isIncomming//isIncomming currentMessage
			        		));
		        }
	        }
	        cursor.close();
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }
    
    private boolean isIncommingMessage(String i){
    	if(Integer.valueOf(i) == 1)
    		return true;
    	return false;
    }
    private void sendNewMessage(){
    	currentMessage  = edit_new_msg.getText().toString();
    	currentMessageClear =  new String(currentMessage);
    	if(currentContact == null){
    		Toast.makeText(getBaseContext(), "Please select a contact", 
		    		Toast.LENGTH_SHORT).show();
    		return;
    	}
    	if(currentAddress == null){
    		Toast.makeText(getBaseContext(), "No phonenumber selected", 
		    		Toast.LENGTH_SHORT).show();
    		return;
    	}
    	if(hasPublicKey()){
    		currentMessage = signcryption.signcryptMessage(currentMessage, 
    				readPrivateKey(), 
    				getPublicKeyFromContact());
    		//Log.info(TAG + " Send Signcrypted SMS:" + currentContact.getmPhoneNumber() +  currentMessage);
    	}else{
    		//Log.info(TAG + " Send normal SMS:" + currentContact.getmPhoneNumber() + "->" +  currentMessage);
    	}
    	sendSMS(currentAddress, currentMessage);
    }
    private void sendSMS(String phoneNumber, String message)
    {      
    	try{
    	String SENT = "SMS_SENT";
    	String DELIVERED = "SMS_DELIVERED";
    	InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    	mgr.hideSoftInputFromWindow(edit_new_msg.getWindowToken(), 0);
    	edit_new_msg.setText("");
    	Toast.makeText(getBaseContext(), "Sending SMS..", 
	    		Toast.LENGTH_SHORT).show();
    		
        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver(){
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				
				switch (getResultCode())
				{
				    case Activity.RESULT_OK:
				    	if(currentMessage != null){
				    		addMessageToSent(currentAddress, currentMessageClear);
				    		currentMessage = null;
					    	renderAdapter();
					    	Toast.makeText(getBaseContext(), "SMS sent", 
						    		Toast.LENGTH_SHORT).show();
				    	}
					    break;
				    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					    Toast.makeText(getBaseContext(), "Generic failure", 
					    		Toast.LENGTH_SHORT).show();
					    break;
				    case SmsManager.RESULT_ERROR_NO_SERVICE:
					    Toast.makeText(getBaseContext(), "No service", 
					    		Toast.LENGTH_SHORT).show();
					    break;
				    case SmsManager.RESULT_ERROR_NULL_PDU:
					    Toast.makeText(getBaseContext(), "Null PDU", 
					    		Toast.LENGTH_SHORT).show();
					    break;
				    case SmsManager.RESULT_ERROR_RADIO_OFF:
					    Toast.makeText(getBaseContext(), "Radio off", 
					    		Toast.LENGTH_SHORT).show();
					    break;
				}
			}
        }, new IntentFilter(SENT));
        
        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver(){
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				switch (getResultCode())
				{
				    case Activity.RESULT_OK:
					    Toast.makeText(getBaseContext(), "SMS delivered", 
					    		Toast.LENGTH_SHORT).show();
					    break;
				    case Activity.RESULT_CANCELED:
					    Toast.makeText(getBaseContext(), "SMS not delivered", 
					    		Toast.LENGTH_SHORT).show();
					    break;					    
				}
			}
        }, new IntentFilter(DELIVERED));        

        SmsManager sms = SmsManager.getDefault();
        ArrayList<String> messages = sms.divideMessage(message);
        int messageCount = messages.size();
        ArrayList<PendingIntent> deliveryIntents = new ArrayList<PendingIntent>(messageCount);
        ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>(messageCount);

        for (int j = 0; j < messageCount; j++) {
           sentIntents.add(PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0));
           deliveryIntents.add(PendingIntent.getBroadcast(this, 0, new Intent(DELIVERED), 0));
        }
        sms.sendMultipartTextMessage(phoneNumber, null, messages,sentIntents,deliveryIntents);
    	}catch(Exception e){
    		e.printStackTrace();
		    Toast.makeText(getBaseContext(), "Problems with the SMS service, please try again.", 
		    		Toast.LENGTH_SHORT).show();
    	}
    }
    private boolean hasPublicKey(){
    	if(currentContact == null)
    		return false;
    	String note = getNote(currentContact.getmPhoneNumber());
    	if(note == null)
    		return false;
		if(signcryption.isPublicKeyVerified(note))
			note = signcryption.extractPublicKeyVerified(note);
		else 
			return false;
		return signcryption.isPublicKeySMSMessage(note);
    }
    private String getPublicKeyFromContact(){
    	String note = getNote(currentAddress);
    	if(signcryption.isPublicKeyVerified(note)){
    		note = signcryption.extractPublicKeyVerified(note);
    		if(signcryption.isPublicKeySMSMessage(note))
    			return  signcryption.extractPublicKeyAscii85FromSMSMessage(note);
    	}
    	return null;
    }
    private String getNote(String phonenumber){
		if(note != null)
			return note;
        String noteWhere = ContactsContract.CommonDataKinds.Note.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?"; 
        String[] noteWhereParams = new String[]{queryContactIdByPhoneNumber(phonenumber), 
 		ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE}; 
        try{
        Cursor cursor = getContentResolver().query(ContactsContract.Data.CONTENT_URI, null, noteWhere, noteWhereParams, null); 
	 	if (cursor.moveToFirst()) { 
    		/*String columns[] = cursor.getColumnNames();
         	for (String column : columns) {  
              int index = cursor.getColumnIndex(column);  
              //Log.info("getNote->Column: " + column + " == ["  
                //      + cursor.getString(index) + "]");
         	}*/
	 		note = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE));
	 	    //Log.info("Note read:" + note);
	 	} 
	 	cursor.close();
	 	return note;
        }catch(Exception e){
        	e.printStackTrace();
        }
        return "";
    }
    public String queryContactIdByPhoneNumber(String phoneNumber){
    	try{
    	Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
    	Cursor cursor = null;
    	if (uri != null) {
    	  cursor = getContentResolver().query(uri, new String[]{PhoneLookup._ID},null,null,null);
    	  if(cursor.moveToFirst())
    	    return cursor.getString(cursor.getColumnIndex(PhoneLookup._ID)); // this is the person ID you need
    	}
        }catch(Exception e){
        	e.printStackTrace();
        }
        return "0";
    }
    public String readPrivateKey(){
        String result;
        try {
            FileInputStream in = openFileInput(MainActivity.PRIVATE_KEY_FILE);
            byte[] buffer = new byte[1024];
            int length = in.read(buffer);
            in.close();
            byte[]res = new byte[length];
            System.arraycopy(buffer, 0, res, 0, length);
            result = new String(res,"ascii");
        } catch (Exception fileError){
            fileError.printStackTrace();
        	return "";
        }
        //Log.info("PrivateKey saved to:" + getFilesDir());
        return result;
    }
    private void addMessageToSent(String address, String message){
    	if(hasPublicKey())
    		message = Signcryption.SigncryptedSMSid + message;
    	Uri uri = Uri.parse("content://sms/sent/");
    	try{
    	ContentValues values = new ContentValues();
    	values.put("address", address);
    	values.put("body", message);
    	getContentResolver().insert(uri, values);
        }catch(Exception e){
        	e.printStackTrace();
		    Toast.makeText(getBaseContext(), "Could not save outgoing message.", 
		    		Toast.LENGTH_SHORT).show();
        }
    }
    protected Dialog onCreateDialog(int id) {
		Dialog dialog = new Dialog(this);
		dialog.setCancelable(false);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.dialog_fingerprint);
		TextView txtHead = (TextView)dialog.findViewById(R.id.textView_manage_public_keys_heading);
		TextView txtfinger = (TextView)dialog.findViewById(R.id.textView_manage_public_keys_fingerprint_value);
		Button btnOk = (Button) dialog.findViewById(R.id.ok_button);
		Button btnCancel = (Button) dialog.findViewById(R.id.button_cancel);
		if(hasPublicKey()){
			txtHead.setText(R.string.more_info_description_have_public_key);
			txtfinger.setText("Fingerprint: " + signcryption.getPublicKeyFingerPrint(getPublicKeyFromContact()));
		}else{
			txtHead.setText("                 ");
			txtfinger.setText(R.string.more_info_description_no_public_key);
		}
		switch (id) {
		case DIALOG_SHOW_FINGERPRINT:
			btnOk.setText(R.string.ok);
			btnCancel.setVisibility(View.GONE);
			btnOk.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					dismissDialog(DIALOG_SHOW_FINGERPRINT);
				}
			});
			break;
		default:
			dialog = null;
			break;
		}
    	return dialog;
    }
}
















