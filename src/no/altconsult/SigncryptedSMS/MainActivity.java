package no.altconsult.SigncryptedSMS;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.logging.Logger;


import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts.People;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ListActivity {
    
	public static final String PRIVATE_KEY_FILE = "SigncryptedSMS6";
	public static final String FIRST_LAUNCH_FILE = "EXIST";
	
	public static final int SHARE_PUBLIC_KEY = 1;
	public static final int MANAGE_PUBLIC_KEYS = 2;
	public static final int CHANGE_PRIVATE_KEY_PASSWORD = 3;
	public static final int MORE_OPTIONS = 4;
	public static final int NEW_PRIVATE_KEY = 5;
	public static final int ASK_FOR_PRIVATE_KEY_PASSWORD = 6;
	public static final int IMPORT_FROM_TEXT = 7;
	public static final int IMPORT_FROM_QR_CODE = 8;
	public static final int NEW_PUBLIC_KEY_SMS = 10;
	public static final int DIALOG_SHOW_MY_FINGERPRINT = 11;
	
	public static Signcryption signcryption;
	private long tmpDate = 0;
	
	public static final boolean signcryptionIsActive = true;
	//public static final Logger Log = Logger.getLogger("SigncryptedSMS");
	//private static final String TAG = "MainActivity";
	private TextView txtfinger;
	
	private ArrayList<ModelSMSThreadRow> modelSMSThreadRows;
	public class InboxAdapter extends ArrayAdapter<ModelSMSThreadRow> {

		public InboxAdapter(Context context, int textViewResourceId,
				ArrayList<ModelSMSThreadRow> objects) {
			super(context, textViewResourceId, objects);
			// TODO Auto-generated constructor stub
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ModelSMSThreadRow m = modelSMSThreadRows.get(position);
			
			LayoutInflater inflater=getLayoutInflater();
			View row=inflater.inflate(R.layout.row_message_thread, parent, false);
			row.setTag(m);
			if(position != 0){
				ImageView icon=(ImageView)row.findViewById(R.id.icon_thread_contact);
				TextView txtName =(TextView)row.findViewById(R.id.textView_thread_row_displayname);
				TextView txtNumber =(TextView)row.findViewById(R.id.textView_thread_display_number);
				TextView txtMessageTeaser =(TextView)row.findViewById(R.id.textView_thread_message_teaser);
				TextView txtTime =(TextView)row.findViewById(R.id.textView_thread_time);
				
				//Set the fields from the model
				txtName.setText(m.getDisplayname());
				txtNumber.setText(m.getNumber());
				txtTime.setText(m.getDate());
				txtMessageTeaser.setText(m.getMessageTeaser());
				
				//Set picture 
				Bitmap bm = loadContactPhoto(getContentResolver(), Long.parseLong(m.getContactId()));
				if(bm != null){
					icon.setImageBitmap(bm);
				}
			// TODO get icons from contacts
			}else{
				ImageView icon=(ImageView)row.findViewById(R.id.icon_thread_contact);
				TextView txtName =(TextView)row.findViewById(R.id.textView_thread_row_displayname);
				TextView txtNumber =(TextView)row.findViewById(R.id.textView_thread_display_number);
				// Quickfix to add New message button
				txtName.setText("New Message");
				txtName.setPadding(10,8,0,5);
				txtNumber.setText("Compose new message");
				txtNumber.setPadding(12,0,0,0);
				icon.setVisibility(8);
				
			}
	       row.setOnClickListener(new View.OnClickListener(){
	            public void onClick(View v) 
	            { 
	        		goToConversation(v);
	            }
	        });
			
			return row;
		}

	}
	public void goToConversation(View v){
		Intent intent = new Intent(this, ConversationListActivity.class);
		intent.putExtra("thread_number", ((ModelSMSThreadRow)v.getTag()).getId());
		intent.putExtra("contact_id", ((ModelSMSThreadRow)v.getTag()).getContactId());
		intent.putExtra("address", ((ModelSMSThreadRow)v.getTag()).getNumber());
		startActivity(intent);
	}
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SmsReceiver.pointer = this;
        initSigncryption();
        renderAdapter();
    }
    @Override
    protected void onResume() {
    	super.onResume();
    	renderAdapter();
    }
    
	public void renderAdapter() {
		initInboxRows();
        setListAdapter(new InboxAdapter(MainActivity.this, R.layout.row_message_thread, modelSMSThreadRows));
	}
    private void initInboxRows() {
		modelSMSThreadRows = new ArrayList<ModelSMSThreadRow>();
		//First row empty for New Message
		modelSMSThreadRows.add(new ModelSMSThreadRow("-1","","","","","",0,0,""));
		//Add threads
		queryForSMSmessages();
	}
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(Menu.NONE,SHARE_PUBLIC_KEY,Menu.NONE, 
    			R.string.activity_share_public_key);
    	menu.add(Menu.NONE,MANAGE_PUBLIC_KEYS,Menu.NONE, 
    			R.string.activity_manage_public_keys);
    	//menu.add(Menu.NONE,CHANGE_PRIVATE_KEY_PASSWORD,	Menu.NONE,
    	//		R.string.activity_change_private_key_password);
    	//menu.add(Menu.NONE,MORE_OPTIONS,Menu.NONE, 
    	//		R.string.more_options);
    	menu.add(Menu.NONE,DIALOG_SHOW_MY_FINGERPRINT,Menu.NONE, 
    			R.string.my_fingerprint);
    	return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	Intent intent;
    	switch (item.getItemId()) {
		case SHARE_PUBLIC_KEY:
            intent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
            startActivityForResult(intent, SHARE_PUBLIC_KEY);
			break;
		case MANAGE_PUBLIC_KEYS:
			intent = new Intent(this, ManagePublicKeysList.class);
			startActivity(intent);
			break;
		case CHANGE_PRIVATE_KEY_PASSWORD:
			intent = new Intent(this, ChangePrivateKeyPasswordActivity.class);
			intent.putExtra("private_key_password", signcryption.getPrivateKeyPassword());
			startActivityForResult(intent, CHANGE_PRIVATE_KEY_PASSWORD);
			break;
		case MORE_OPTIONS:
			renderAdapter();
			break;
		case DIALOG_SHOW_MY_FINGERPRINT:
			setFingerPrint();
			showDialog(DIALOG_SHOW_MY_FINGERPRINT);
			break;
		default:
			break;
		}
    	return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	switch (requestCode) {
		case NEW_PRIVATE_KEY:
			if(signcryptionIsActive){
				new_private_key(data);
			}else{
				Toast.makeText(this, "Signcryption is not activated.", Toast.LENGTH_SHORT).show();
			}
			break;
		case CHANGE_PRIVATE_KEY_PASSWORD:
			if(signcryptionIsActive){
				signcryption.setPrivateKeyPassword(data.getStringExtra("private_key_password"));
				Toast.makeText(this, "Private key has been stored with new encryption password.", Toast.LENGTH_SHORT).show();
			}else{
				Toast.makeText(this, "Signcryption is not activated.", Toast.LENGTH_SHORT).show();
			}
			break;
		case ASK_FOR_PRIVATE_KEY_PASSWORD:
			if(signcryptionIsActive){
				signcryption = new Signcryption(readPrivateKey(), data.getStringExtra("private_key_password"));
				signcryption.setPrivateKeyPassword(data.getStringExtra("private_key_password"));
			}else{
				Toast.makeText(this, "Signcryption is not activated.", Toast.LENGTH_SHORT).show();
			}
			break;
		case SHARE_PUBLIC_KEY:
			if(resultCode != Activity.RESULT_CANCELED){
				Uri uri = data.getData();
				//Log.info(TAG + "contact uri:" + uri.toString());
				if(uri != null){
					ContactInfo c = queryContactInfo(uri, null);
					sendPublicKeyAsSMS(c.getmPhoneNumber());
				}
			}
			break;
		default:
			break;
		}
    }
	public void new_private_key(Intent data) {
		signcryption = new Signcryption();
		if(data != null){// If we support encryption
			signcryption.setPrivateKeyPassword(data.getStringExtra("private_key_password"));
			savePrivateKey(signcryption.getEncryptedAscii85PrivateKey(signcryption.getPrivateKeyPassword()));
			Toast.makeText(this, "A key-pair has been generated and stored securely.", Toast.LENGTH_SHORT).show();
		}else{ //no encryption of private key
			savePrivateKey(signcryption.getPrivateKey().toString());
			Toast.makeText(this, "A key-pair has been generated and stored.", Toast.LENGTH_SHORT).show();
		}
	}
    private void initSigncryption(){
    	if(signcryption == null){
    		if(hasPrivateKey()){
    			signcryption = new Signcryption(readPrivateKey());	
    			// TODO implement encryption of the private key
    			//Intent intent = new Intent(this, TypePrivateKeyPasswordActivity.class);
    			//startActivity(intent);
    			//startActivityForResult(intent, ASK_FOR_PRIVATE_KEY_PASSWORD);
    		}
    		else{//Have to generate and encrypt Pk
    			new_private_key(null);
    			// TODO implement encryption of the private key
    			//Intent intent = new Intent(this, TypePrivateKeyPasswordActivity.class);
    			//intent.addFlags(NEW_PRIVATE_KEY);
    			//startActivityForResult(intent, NEW_PRIVATE_KEY);
    		}
    	}
    }

    private void sendPublicKeyAsSMS(String phoneNumber){
    	if(signcryptionIsActive){
	    	sendSMS(phoneNumber, signcryption.getPublicKeyAsSMSMessage());
	    	Toast.makeText(this, "Public key to:\n" + phoneNumber, 
	    			Toast.LENGTH_SHORT).show();
    	}else{
        	Toast.makeText(this, "Signcryption not active, nothing sent to:\n" + phoneNumber, 
        			Toast.LENGTH_SHORT).show();
    		
    	}
    }
    
    private void queryForSMSmessages(){
    	Uri uri = Uri.parse("content://sms/conversations/");
    	
    	String TAG = "SMS Query:";
    	try{
	    	Cursor cursor = getContentResolver().query(  
	    	        uri, //uri
	    	        new String[]{"thread_id", "msg_count", "snippet"}, //projection-> bestemte felter som navn 
	    	        null,  //selection
	    	        null,//new String[]{"10"}, //Selection args
	    	        "date DESC limit 100");//Sort order
	        uri = Phone.CONTENT_URI;
	        while(cursor.moveToNext()){  
		       /* String columns[] = cursor.getColumnNames();
		        for (String column : columns) {  
		            int index = cursor.getColumnIndex(column);  
		            Log.info(TAG +"queryForSMS:Column: " + column + " == ["  
		                    + cursor.getString(index) + "]");  
		        }
		        Log.info("move to next");*/
	        	String[] res = queryContactIdByThreadId(cursor.getString(cursor.getColumnIndex("thread_id")));
	        	ContactInfo aContactInfo = new ContactInfo();
	        	if(res[0] != null && !res[0].equals("0"))
	        		aContactInfo = queryContactInfo(null, res[0]);
	        	else
	        		aContactInfo.setmPhoneNumber(res[1]);
	
		        modelSMSThreadRows.add(new ModelSMSThreadRow(
		        		cursor.getString(cursor.getColumnIndex("thread_id")),
		        		String.valueOf(aContactInfo.getId()),
		        		aContactInfo.getmDisplayName(), 
		        		aContactInfo.getmPhoneNumber(), 
		        		"0", 
		        		cursor.getString(cursor.getColumnIndex("msg_count")), 
		        		tmpDate,// TODO get from queryContactIdByThreadId 
		        		aContactInfo.getPicId(), 
		        		signcryption.fixSnippet(cursor.getString(cursor.getColumnIndex("snippet")))));
	        }
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    } 
    private String[] queryContactIdByThreadId(String id){
    	Uri uri = Uri.parse("content://sms/conversations/" + id);
    	try{
    	Cursor cursor = getContentResolver().query(  
    	        uri, //uri
    	        new String[]{"date", "address"}, //projection-> bestemte felter som navn 
    	        null,  //selection
    	        null,//new String[]{id}, //Selection args
    	        "date DESC LIMIT 1");//Sort order
    	if(cursor != null)
    	cursor.moveToFirst();
    	/*String columns[] = cursor.getColumnNames();
       	for (String column : columns) {  
            int index = cursor.getColumnIndex(column);  
            Log.info(TAG +"Column: " + column + " == ["  
                    + cursor.getString(index) + "]");  
        }*/
    	//Quick fix
        tmpDate = cursor.getLong(cursor.getColumnIndex("date"));
        String contact_id = queryContactIdByPhoneNumber(cursor.getString(cursor.getColumnIndex("address")));
    	//Log.info(TAG + "Contact ID:" + contact_id);
    	return new String[]{contact_id, cursor.getString(cursor.getColumnIndex("address"))};
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    	return new String[]{"0","0"};
    }
   
    public void savePrivateKey(String privateKey){
    	FileOutputStream out = null;
    	byte[] data = null;
		try {
			data = privateKey.getBytes("ascii");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
    	try{
            out = openFileOutput(PRIVATE_KEY_FILE, MODE_APPEND);
    	}catch(FileNotFoundException e){
            
        } catch (Exception fileError){
            // don't report exception
        }
        try {
			out.write(data);
			out.close();
			//Log.info("Private key saved to internal memory.");
		} catch (IOException e) {
			e.printStackTrace();
		}
        
    }
    /**
     * Use uri or id to get a ContactInfo object.
     * 
     * If String id is set, uri is not used.
     * 
     * @param uri
     * @param id
     * @return
     */
    public ContactInfo queryContactInfo(Uri uri, String id){
    	ContactInfo aContact = new ContactInfo();
    	if(uri == null && id == null)
    		return aContact;
    	if(id == null)
    		id = uri.getLastPathSegment();
    	try{
	    	Cursor cursor = getContentResolver().query(  
	    	        Phone.CONTENT_URI, 
	    	        new String[]{Phone.DISPLAY_NAME, Phone.NUMBER, "photo_id"},  
	    	        Phone.CONTACT_ID + "=?",  
	    	        new String[]{id}, 
	    	        null);
	        if(cursor != null)
	    	if(cursor.moveToFirst()){
	        aContact.setId(Integer.parseInt(id));
	        int i = cursor.getColumnIndex(Phone.DISPLAY_NAME);
	        aContact.setmDisplayName(cursor.getString(i));
	        i = cursor.getColumnIndex(Phone.NUMBER);
	        aContact.setmPhoneNumber(cursor.getString(i));
	        aContact.setPic(cursor.getLong(cursor.getColumnIndex("photo_id")));
	        }
    	}catch(Exception e){
    		e.printStackTrace();
    	}
        return aContact;
    }
    public String queryContactIdByPhoneNumber(String phoneNumber){
    	try{
    	Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
    	Cursor cursor = null;
    	if (uri != null) {
    	  
    		cursor = getContentResolver().query(
    			  uri, 
    			  null,//new String[]{"_ID"},
    			  null,
    			  null,
    			  null);
    	  if(cursor != null)
    	  if(cursor.moveToFirst()){
    		 /* String columns[] = cursor.getColumnNames();
    	       	for (String column : columns) {  
    	            int index = cursor.getColumnIndex(column);  
    	            Log.info(TAG +"Column: " + column + " == ["  
    	                    + cursor.getString(index) + "]");  
    	        }*/
    	    return cursor.getString(cursor.getColumnIndex("_ID")); // this is the person ID we need
    	  }
    	}
    	}catch(Exception e){}
    	return "0";
    }
   
   public boolean firstLaunch(){
        try{
            openFileInput(FIRST_LAUNCH_FILE);
        }catch(Exception e){
            e.printStackTrace();
            // if this is the first launch, we touch the file
            try {
                FileOutputStream out = openFileOutput(FIRST_LAUNCH_FILE, MODE_PRIVATE);
                out.write("".getBytes());
                out.flush();
                out.close();
            } catch (Exception fileError){
                // don't report exception
            }
            return true;
        }
        return false;
    }
    public boolean hasPrivateKey(){
        try{
            openFileInput(PRIVATE_KEY_FILE);
        }catch(Exception e){
            return false;
        }
        return true;
    }
    public String readPrivateKey(){
        String result;
        try {
            FileInputStream in = openFileInput(PRIVATE_KEY_FILE);
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
        //Log.info("PrivateKey saved to:" + getFilesDir() + "with value:" + result);
        return result;
    }

    private void sendSMS(String phoneNumber, String message)
    {      
    	try{
    	String SENT = "SMS_SENT";
    	String DELIVERED = "SMS_DELIVERED";
    		
        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver(){
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				switch (getResultCode())
				{
				    case Activity.RESULT_OK:
					    Toast.makeText(getBaseContext(), "SMS sent", 
					    		Toast.LENGTH_SHORT).show();
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
    		Toast.makeText(this, "A problem occured with sending SMS, please try again.", Toast.LENGTH_SHORT).show();
    	}
    } 
    public void notifyReceivedMessage(String msg, String address){
	    //Log.info(TAG + " Received message from:" + address + ":" + msg);
    	if(signcryption.isPublicKeySMSMessage(msg)){
	        try{
    		Intent intent = new Intent(this, ManagePublicKeysList.class);
	        intent.setFlags(MainActivity.NEW_PUBLIC_KEY_SMS);
			intent.putExtra("public_key", msg);
			intent.putExtra("public_key_address", address);
			startActivity(intent);
	        }catch(Exception e){
	        	e.printStackTrace();
	        }
	    }
    	renderAdapter();
    }
    protected Dialog onCreateDialog(int id) {
		Dialog dialog = new Dialog(this);
		dialog.setCancelable(false);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.dialog_fingerprint);
		txtfinger = (TextView)dialog.findViewById(R.id.textView_manage_public_keys_fingerprint_value);
		Button btnOk = (Button) dialog.findViewById(R.id.ok_button);
		Button btnCancel = (Button) dialog.findViewById(R.id.button_cancel);
		setFingerPrint();
    	switch (id) {
		case DIALOG_SHOW_MY_FINGERPRINT:
			btnOk.setText(R.string.ok);
			btnCancel.setVisibility(View.GONE);
			btnOk.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					dismissDialog(DIALOG_SHOW_MY_FINGERPRINT);
				}
			});
			break;
		default:
			dialog = null;
			break;
		}
    	return dialog;
    }
    private void setFingerPrint(){
    	if(txtfinger != null)
    		txtfinger.setText("Fingerprint: " + signcryption.getPublicKeyFingerPrint(signcryption.getPublicKeyAsAscii85()));
    }
    public static Bitmap loadContactPhoto(ContentResolver cr, long  id) {
        Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
        InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(cr, uri);
        if (input == null) {
            return null;
        }
        return BitmapFactory.decodeStream(input);
    }

}






