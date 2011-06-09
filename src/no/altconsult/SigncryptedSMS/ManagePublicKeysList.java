package no.altconsult.SigncryptedSMS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.PhoneLookup;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ManagePublicKeysList extends ListActivity {
	private ArrayList<ModelPublicKeyRow> modelPublicKeyRows;
	private ModelPublicKeyRow contact;
	//public static final Logger Log = Logger.getLogger("SigncryptedSMS");
	private static Signcryption signcryption = MainActivity.signcryption;
	private static final int DIALOG_MORE_INFO = 1;
	private static final int DIALOG_VERIFICATION = 2;
	private static final int QR_SCAN_REQUEST_CODE = 3;
	private TextView txtfinger;
	private Button btnOk;
	private Button btnCancel;
	private Dialog dialog;
	
	public class PublicKeyAdapter extends ArrayAdapter<ModelPublicKeyRow> {
		public PublicKeyAdapter(Context context, int textViewResourceId,
				ArrayList<ModelPublicKeyRow> objects) {
			super(context, textViewResourceId, objects);
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ModelPublicKeyRow m = modelPublicKeyRows.get(position);
			
			LayoutInflater inflater=getLayoutInflater();
			View row=inflater.inflate(R.layout.row_public_key, parent, false);
			ImageView icon=(ImageView)row.findViewById(R.id.icon_public_key_status);
			TextView txtStatus =(TextView)row.findViewById(R.id.textView_public_key_status);
			TextView txtName =(TextView)row.findViewById(R.id.textView_public_key_row_displayname);
			TextView txtNumber =(TextView)row.findViewById(R.id.textView_public_key_display_number);
			Button btn = (Button)row.findViewById(R.id.button_public_key_verify_or_info);
			btn.setTag(m);
			btn.setOnClickListener(new View.OnClickListener(){
	            public void onClick(View v) 
	            { 
	            	onButtonClicked(v);
	            }
	        });
			//Set Icon, Status text and button text
			if(m.isVerified()){
				icon.setImageResource(R.drawable.verified);
				txtStatus.setText(R.string.verified);
				btn.setText(R.string.more_info);
			}else{
				icon.setImageResource(R.drawable.pending);
				txtStatus.setText(R.string.pending);
				btn.setText(R.string.start_verifying);
			}
			//Set name and number
			txtName.setText(m.getName());
			txtNumber.setText(m.getNumber());
			return row;
		}
	}
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent data = getIntent();
        switch (data.getFlags()) {
		case MainActivity.NEW_PUBLIC_KEY_SMS:
			String message = data.getStringExtra("public_key");
			String address = data.getStringExtra("public_key_address");
			newImport(message, address);
			break;
		default:
			break;
		}
        renderRows();
    }
	private void newImport(String message, String address) {
		ContactInfo c = queryContactInfo(queryContactIdByPhoneNumber(address));
		if(c.getId() != 0){
			putPublicKeyToNote(address, message, false);
			Toast.makeText(getBaseContext(), "Public key from:" + address + " received and saved as pending", 
		    		Toast.LENGTH_LONG).show();
		}else{
			Toast.makeText(getBaseContext(), "Only numbers stored in contacts supported", 
		    		Toast.LENGTH_LONG).show();
		}
	}
	public void renderRows() {
		initRows();
        setListAdapter(new PublicKeyAdapter(ManagePublicKeysList.this, R.layout.row_public_key, modelPublicKeyRows));
        queryPublicKeys();
	}
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		String selection = l.getItemAtPosition(position).toString();
		Toast.makeText(this, selection, Toast.LENGTH_LONG).show();
	}
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(Menu.NONE,MainActivity.IMPORT_FROM_TEXT,Menu.NONE, 
    			R.string.import_from_text);
    	menu.add(Menu.NONE,MainActivity.IMPORT_FROM_QR_CODE,Menu.NONE, 
    			R.string.import_from_QR_code);
    	return super.onCreateOptionsMenu(menu);
    }
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
		case MainActivity.IMPORT_FROM_TEXT:
			// TODO implement
			Toast.makeText(getBaseContext(), "not implemented", 
		    		Toast.LENGTH_LONG).show();
			//queryPublicKeysFromSMSInbox();
			//renderRows();
			break;
		case MainActivity.IMPORT_FROM_QR_CODE:
			// TODO implement
			try{
			Intent intent = new Intent("com.google.zxing.client.android.SCAN");
			intent.setPackage("com.google.zxing.client.android");
			intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
			startActivityForResult(intent, QR_SCAN_REQUEST_CODE);
			}catch(Exception e){
				Toast.makeText(this, "Install Barcode Scanner by ZXing Team " +
						"from Android market to use this feature.", Toast.LENGTH_SHORT).show();
			}
			break;
		default:
			break;
		}
    	return super.onOptionsItemSelected(item);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	if (requestCode == QR_SCAN_REQUEST_CODE) {
    		if (resultCode == RESULT_OK) {
    			String str = intent.getStringExtra("SCAN_RESULT");	
    			String address = signcryption.getAddressFromQRCode(str);
    			String key = signcryption.getPublicKeyFromQRCode(str);
    			//Log.info("QR SCANNED: adress:" + address + " Key:" + key);
    			if(signcryption.isPublicKeySMSMessage(key)){
    				newImport(key, address);
    				renderRows();
    			}else{
    				Toast.makeText(getBaseContext(), "Number not in contacts or the QR format is wrong.", 
    			    		Toast.LENGTH_LONG).show();
    			}
    		}
    	}
    }

	private void initRows() {
		modelPublicKeyRows = new ArrayList<ModelPublicKeyRow>();
	}
	private void onButtonClicked(View v){
		contact = (ModelPublicKeyRow)v.getTag();
		if(dialog != null)
			txtfinger.setText("Fingerprint: " + signcryption.getPublicKeyFingerPrint(contact.getPublicKey()));
		if(contact != null){
			if(!contact.isVerified()){
				// TODO impl. verify procedure
				// TODO ImageView icon=(ImageView)v.findViewById(R.id.icon_public_key_status);
				// TODO TextView txtStatus =(TextView)v.findViewById(R.id.textView_public_key_status);
				// TODO Button btn = (Button)v.findViewById(R.id.button_public_key_verify_or_info);
				showDialog(DIALOG_VERIFICATION);
			}
			if(contact.isVerified()){
				showDialog(DIALOG_MORE_INFO);
			}
		}
	}
    public String queryContactIdByPhoneNumber(String phoneNumber){
    	Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
    	try{
    		Cursor cursor = getContentResolver().query(uri, null,null,null,null);
	    	  if(cursor.moveToFirst()){
	    		/*String columns[] = cursor.getColumnNames();
	         	for (String column : columns) {  
	              int index = cursor.getColumnIndex(column);  
	              //Log.info("queryContactIdByPhoneNumber->Column: " + column + " == ["  
	                      + cursor.getString(index) + "]");
	         	}*/
	              return cursor.getString(cursor.getColumnIndex("_ID")); // this is the person ID you need
	          }
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    	return "0";
    }
	private void putPublicKeyToNote(String phonenumber, String note, boolean isVerified){
		if(signcryption.isPublicKeyVerified(note))
			note = signcryption.extractPublicKeyVerified(note);
		if(signcryption.isPublicKeyPending(note))
			note = signcryption.extractPublicKeyPending(note);
		if(isVerified)
			note = Signcryption.PuKeySMS_verified + note;
		else
			note = Signcryption.PuKeySMS_pending + note;
		Uri uri = ContactsContract.Data.CONTENT_URI;
		try{
		ContentValues values = new ContentValues();
		//values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE);
		values.put(ContactsContract.CommonDataKinds.Note.NOTE, note);
		String where = ContactsContract.CommonDataKinds.Note.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
		String[] selectionArgs = new String[]{queryContactIdByPhoneNumber(phonenumber), 
		 		ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE};
		int rowsUpdated = getContentResolver().update(
				uri, 
				values, 
				where, 
				selectionArgs);
		//Log.info("Notes put:" + rowsUpdated);
		}catch(Exception e){
			e.printStackTrace();
			Toast.makeText(getBaseContext(), "Couldn't save public " +
					"key to Contact, check that the number is listed in contacts.", 
		    		Toast.LENGTH_LONG).show();
		}
	}
    public ContactInfo queryContactInfo(String id){
    	ContactInfo aContact = new ContactInfo();
    	try{
    	Cursor cursor = getContentResolver().query(  
    	        Phone.CONTENT_URI, null,  
    	        "raw_contact_linkpriority1" + "=?",  
    	        new String[]{id}, null);
        if(cursor != null){
	    	if(cursor.moveToFirst()){
		    	/*String columns[] = cursor.getColumnNames();
		        for (String column : columns) {  
		            int index = cursor.getColumnIndex(column);  
		            //Log.info(Phone.CONTENT_URI +"queryContactInfo(String id)->Column: " + column + " == ["  
		              //      + cursor.getString(index) + "]");  
		        }*/
	        aContact.setId(cursor.getColumnIndex(id));
	        int i = cursor.getColumnIndex(Phone.DISPLAY_NAME);
	        aContact.setmDisplayName(cursor.getString(i));
	        i = cursor.getColumnIndex(Phone.NUMBER);
	        aContact.setmPhoneNumber(cursor.getString(i));
	        queryContactIdByPhoneNumber(cursor.getString(i));
	        }
    	}
        return aContact;
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    	return aContact;
    }
    
    private void queryPublicKeys(){
		String note="";
		//Pending keys
		String noteWhere = ContactsContract.Data.MIMETYPE + " = ? "//
        		+ "AND " + ContactsContract.CommonDataKinds.Note.NOTE + 
        		" like '%" + Signcryption.PuKeySMS_pending + Signcryption.PuKeySMSid +"%'"; 
        String[] noteWhereParams = new String[]{ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE};
        try{
        Cursor cursor = getContentResolver().query(ContactsContract.Data.CONTENT_URI, null, noteWhere, noteWhereParams, null); 
	 	while(cursor.moveToNext()) { 
	    	/*String columns[] = cursor.getColumnNames();
	        for (String column : columns) {  
	            int index = cursor.getColumnIndex(column);  
	            Log.info("queryPublicKeys->Column: " + column + " == ["  
	                    + cursor.getString(index) + "]");  
	        }*/
	 		try{
	 		note = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE));
	 		modelPublicKeyRows.add(new ModelPublicKeyRow(
	 				cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Note.RAW_CONTACT_ID)),
	 				note,
	 				cursor.getString(cursor.getColumnIndex("display_name")),
	 				queryContactInfo(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Note.RAW_CONTACT_ID))).getmPhoneNumber(),
	 				false));
	 		}catch(Exception e){
	 			e.printStackTrace();
	 		}
	 	}
        
	 	//Verified keys
	 	noteWhere = ContactsContract.Data.MIMETYPE + " = ? "//
		+ "AND " + ContactsContract.CommonDataKinds.Note.NOTE + 
		" like '%" + Signcryption.PuKeySMS_verified + Signcryption.PuKeySMSid +"%'"; 
        cursor = getContentResolver().query(ContactsContract.Data.CONTENT_URI, null, noteWhere, noteWhereParams, null);
	 	while(cursor.moveToNext()) {
	    	/*String columns[] = cursor.getColumnNames();
	        for (String column : columns) {  
	            int index = cursor.getColumnIndex(column);  
	            Log.info("queryPublicKeys->Column: " + column + " == ["  
	                    + cursor.getString(index) + "]");  
	        }*/
	 		try{
	 		note = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE));
	 		modelPublicKeyRows.add(new ModelPublicKeyRow(
	 				cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Note.RAW_CONTACT_ID)),
	 				note,
	 				cursor.getString(cursor.getColumnIndex("display_name")),
	 				queryContactInfo(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Note.RAW_CONTACT_ID))).getmPhoneNumber(),
	 				true));
	 		}catch(Exception e){
	 			e.printStackTrace();
	 		}
	 	}
	 	cursor.close();
        }catch(Exception e){
        	
        }
    }
    private void queryPublicKeysFromSMSInbox(){
    	HashMap<String, ModelPublicKeyRow> hm = new HashMap<String, ModelPublicKeyRow>();
    	Uri uri = Uri.parse("content://sms/inbox/");
    	String where = "body like '%" + Signcryption.PuKeySMSid +"%'";
    	try{
	    	Cursor cursor = getContentResolver().query(
	    			uri, 
	    			null, 
	    			where, 
	    			null, 
	    			"date asc");
	    	if(cursor != null)
	    	while(cursor.moveToNext()){
	    		/*String columns[] = cursor.getColumnNames();
		       	for (String column : columns) {  
		            int index = cursor.getColumnIndex(column);  
		            Log.info("sms/inbox/" +"Column: " + column + " == ["  
		                    + cursor.getString(index) + "]");  
		        }*/
		       	hm.put(
		       	cursor.getString(cursor.getColumnIndex("address")), //Key
		       	new ModelPublicKeyRow(
		 				"",
		 				cursor.getString(cursor.getColumnIndex("body")),
		 				queryContactInfo(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Note.RAW_CONTACT_ID))).getmDisplayName(),
		 				cursor.getString(cursor.getColumnIndex("address")),
		 				false)
		       	);
	    	}
	    	cursor.close();
	    	Set set = hm.entrySet();
	    	Iterator i = set.iterator();
	        while(i.hasNext()){
	          Map.Entry me = (Map.Entry)i.next();
	          ModelPublicKeyRow m = (ModelPublicKeyRow)me.getValue();
	          //Log.info("HashMap:" + me.getKey() + " : " + me.getValue() );
		 		modelPublicKeyRows.add(new ModelPublicKeyRow(
		 				m.getContact_id(),
		 				m.getPublicKey(),
		 				m.getName(),
		 				m.getNumber(),
		 				false));
	        }
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }
    @Override
    protected Dialog onCreateDialog(int id) {
		dialog = new Dialog(this);
		dialog.setCancelable(false);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.dialog_fingerprint);
		txtfinger = (TextView)dialog.findViewById(R.id.textView_manage_public_keys_fingerprint_value);
		btnOk = (Button) dialog.findViewById(R.id.ok_button);
		btnCancel = (Button) dialog.findViewById(R.id.button_cancel);
		txtfinger.setText("Fingerprint: " + signcryption.getPublicKeyFingerPrint(contact.getPublicKey()));
		switch (id) {
		case DIALOG_MORE_INFO:
			btnOk.setText(R.string.ok);
			btnCancel.setVisibility(View.GONE);
			btnOk.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					dismissDialog(DIALOG_MORE_INFO);
				}
			});
			break;
		case DIALOG_VERIFICATION:
			btnOk.setText(R.string.verify);
			btnOk.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					verify();
					renderRows();
					dismissDialog(DIALOG_VERIFICATION);
				}
			});
			btnCancel.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					dismissDialog(DIALOG_VERIFICATION);
				}
			});
			break;
		default:
			dialog = null;
			break;
		}
    	return dialog;
    }
    private void verify(){
    	putPublicKeyToNote(contact.getNumber(),contact.getPublicKey(), true);
    }
}