package no.altconsult.SigncryptedSMS;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ChangePrivateKeyPasswordActivity extends Activity {
	private EditText edit_old;
	private EditText edit_new;
	private EditText edit_confirm;
	private Button btn;
	private String currentPassword;
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_private_key_password);
        currentPassword = getIntent().getStringExtra("private_key_password");
        btn = (Button) findViewById(R.id.button_change_private_key_password);
        edit_old = (EditText) findViewById(R.id.editText_old_password);
        edit_new = (EditText) findViewById(R.id.editText_new_password);
        edit_confirm = (EditText) findViewById(R.id.editText_confirm_password);
        btn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) 
            { 
            	sendPasswordBackToMain();
            }
        });
    }
    public void sendPasswordBackToMain(){
		if(fieldBlank()){
			Toast.makeText(this, "Fill out all the fields please.",Toast.LENGTH_SHORT).show();
			clearFields();
			return;
		}
    	if(!oldPasswordIsCorrect()){
			Toast.makeText(this, "Old password is wrong.", Toast.LENGTH_SHORT).show();
			clearFields();
			return;
		}
		if(!confirmIsCorrect()){
			Toast.makeText(this, "New and Confirmed password differ.", Toast.LENGTH_SHORT).show();
			clearFields();
			return;
		}
    	Intent data = new Intent();
    	data.putExtra("private_key_password", edit_new.getText().toString());
    	setResult(MainActivity.CHANGE_PRIVATE_KEY_PASSWORD, data);
    	finish();
    }
    private boolean oldPasswordIsCorrect(){
    	return edit_old.getText().toString().equals(currentPassword);
    }
    private boolean confirmIsCorrect(){
    	return edit_confirm.getText().toString().equals(edit_new.getText().toString());
    }
    private void clearFields(){
    	edit_old.setText("");
    	edit_confirm.setText("");
    	edit_new.setText("");
    }
    private boolean fieldBlank(){
    	return (edit_old.getText().toString().equals("")||
    	edit_new.getText().toString().equals("")||
    	edit_confirm.getText().toString().equals(""));
    }
}
