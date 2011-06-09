package no.altconsult.SigncryptedSMS;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class TypePrivateKeyPasswordActivity extends Activity {
	public EditText edit;
	public Button btn;
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.type_private_key_password);
        
        btn = (Button) findViewById(R.id.button_type_private_key_password);
        edit = (EditText) findViewById(R.id.edit_text_input_password);
        btn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) 
            { 
				sendPasswordBackToMain();
            }
        });
    }
    public void sendPasswordBackToMain(){
    	Intent data = new Intent();
    	data.putExtra("private_key_password", edit.getText().toString());
    	setResult(MainActivity.ASK_FOR_PRIVATE_KEY_PASSWORD, data);
    	finish();
    }
}
