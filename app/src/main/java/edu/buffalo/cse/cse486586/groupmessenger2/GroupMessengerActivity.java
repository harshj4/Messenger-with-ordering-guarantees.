package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final int SERVER_PORT = 10000;
    boolean flag = false;
    static String myPort = null;
    static int msgCount = 0;
    static int delieveredCount = 0;
    static int keyCount = 0;
    int crashedNode = 2;
    public static Uri contentUri;
    public static ContentResolver cr;
    public static ContentValues contentValues;
    public static TextView remoteTextView;
    public static HashMap<Integer,String> msgBuffer = new HashMap<Integer, String>();
    public static HashMap<String,String> proposalMap = new HashMap<String, String>();
    public static HashMap<Integer,Boolean> statusMap = new HashMap<Integer, Boolean>();
    public static HashMap<Integer,Integer> destMap = new HashMap<Integer, Integer>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority("edu.buffalo.cse.cse486586.groupmessenger2.provider");
        uriBuilder.scheme("content");
        contentUri = uriBuilder.build();
        remoteTextView = (TextView) findViewById(R.id.textView1);
        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        EditText editText = (EditText) findViewById(R.id.editText1);
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            e.printStackTrace();
        }

        findViewById(R.id.button4).setOnClickListener(
                new OnSendClickListener(tv,myPort,editText));

    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            Log.d("Starting: ", "ServerTask");
            ServerSocket serverSocket = sockets[0];
            Socket listeningSocket = null;
            cr = getContentResolver();
            String receivedString = null;
            try {

                while (true) {
                    Log.d("Before", "accept");
                    listeningSocket = serverSocket.accept();
                    listeningSocket.setTcpNoDelay(true);
                    int destPort;
                    Log.d("After", "accept");
                    DataInputStream dis = new DataInputStream(listeningSocket.getInputStream());
                    DataOutputStream dosAck = new DataOutputStream((listeningSocket.getOutputStream()));
                    try {
                        receivedString = dis.readUTF();
                    }catch(EOFException eof){
                        Log.d("Server","Catching EOF Exception in read");
                        continue;
                    }
                    Log.d("Received String is:",receivedString);

                    StringTokenizer st = new StringTokenizer(receivedString,":");
                    String header = st.nextToken();
                    if(!header.equals("SEQ")){
                        destPort = Integer.parseInt(st.nextToken());
                        String proposalToSend = Integer.toString(msgCount+1) + myPort;
                        dosAck.writeUTF(proposalToSend);
                        dosAck.flush();
                        proposalMap.put(proposalToSend,header);
                        msgBuffer.put(Integer.parseInt(proposalToSend),header);
                        statusMap.put(Integer.parseInt(proposalToSend),false);
                        destMap.put(Integer.parseInt(proposalToSend),destPort);
                        Log.d("Proposal: ","Sent"+Integer.toString(msgCount+1));

                        //listeningSocket.close();
                        //publishProgress(receivedString);

                    }
                    else{//SEQ:123456:123456:12345

                        String origProposal = st.nextToken();
                        String seqNumber = st.nextToken();
                        crashedNode = Integer.parseInt(st.nextToken());
                        String origMsg = proposalMap.get(origProposal);
                        Log.d("Final seq no is "+ receivedString,"For "+origMsg);
                        msgBuffer.remove(Integer.parseInt(origProposal));
                        msgBuffer.put(Integer.parseInt(seqNumber),origMsg);
                        statusMap.remove(Integer.parseInt(origProposal));
                        statusMap.put(Integer.parseInt(seqNumber),true);
                    }

                    Set<Integer> bufferKeys = msgBuffer.keySet();
                    int minSeq = Collections.min(bufferKeys);
                    Set<Integer> destMapKeys = destMap.keySet();
                    Log.d("Server","Failed node is: "+ Integer.toString(crashedNode));
                    Log.d("Server","Dest map is: "+ destMap.toString());
                    if(destMapKeys.contains(minSeq)){
                        if(destMap.get(minSeq).equals(crashedNode)){
                            msgBuffer.remove(minSeq);
                            bufferKeys.remove(minSeq);
                            if(!bufferKeys.isEmpty()){
                                minSeq = Collections.min(bufferKeys);
                            }
                        }
                    }
                    Log.d("Server","Msg buffer is: "+msgBuffer.toString());
                    Log.d("Server","Status map is: "+statusMap.toString());
                    Log.d("Server","Min Seq is: "+Integer.toString(minSeq));
                    while(statusMap.get(minSeq) == true){
                        String msgToDeliver = msgBuffer.get(minSeq);
                        publishProgress(msgToDeliver);
                        contentValues = new ContentValues();
                        contentValues.put("key", Integer.toString(keyCount));
                        contentValues.put("value", msgToDeliver);
                        cr.insert(contentUri, contentValues);
                        Cursor c = cr.query(contentUri, null, Integer.toString(keyCount), null, null);
                        c.moveToFirst();
                        Log.d("Read key", c.getString(0));
                        Log.d("Read value", c.getString(1));
                        c.close();
                        msgBuffer.remove(minSeq);
                        statusMap.remove(minSeq);
                        bufferKeys.remove(minSeq);
                        keyCount++;
                        if(!bufferKeys.isEmpty()){
                            minSeq = Collections.min(bufferKeys);
                        }
                        else{
                            break;
                        }

                    }

                    //dis.close();
                    //dosAck.close();
                    msgCount++;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            Log.d("Server sockets closed", "Successfully");
            return null;
        }

        protected void onProgressUpdate(String...strings) {

            Log.d("Inside:","serverProgressUpdate");
            String strReceived = strings[0].trim();
            //remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
            return;
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}
