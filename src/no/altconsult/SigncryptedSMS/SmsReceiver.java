package no.altconsult.SigncryptedSMS;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class SmsReceiver extends BroadcastReceiver {
	public static MainActivity pointer;
	@Override
	public void onReceive(Context context, Intent intent) 
	{
        //---get the SMS message passed in---
        Bundle bundle = intent.getExtras();
        SmsMessage[] msgs = null;
        String address = "";
        if (bundle != null)
        {
            //---retrieve the whole SMS message received---
            Object[] pdus = (Object[]) bundle.get("pdus");
            msgs = new SmsMessage[pdus.length];
            String message = "";
            for (int i=0; i<msgs.length; i++){
                msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                address = msgs[i].getOriginatingAddress();         
                message += msgs[i].getMessageBody().toString();
            }
            //---pass it over to MainActivity---
            if(pointer != null)
            	pointer.notifyReceivedMessage(message, address);
        }                 		
	}
}
