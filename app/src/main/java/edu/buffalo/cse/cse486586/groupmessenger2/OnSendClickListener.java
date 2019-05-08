package edu.buffalo.cse.cse486586.groupmessenger2;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;

class OnSendClickListener implements View.OnClickListener{
    private TextView _tv;
    private String myport;
    private EditText eText;
    private static final String REMOTE_PORT0 = "11108";
    private static final String REMOTE_PORT1 = "11112";
    private static final String REMOTE_PORT2 = "11116";
    private static final String REMOTE_PORT3 = "11120";
    private static final String REMOTE_PORT4 = "11124";
    public OnSendClickListener(TextView tv, String myPort, EditText editText) {
        this.myport = myPort;
        this._tv = tv;
        this.eText = editText;
    }

    @Override
    public void onClick(View v) {
        String msg = eText.getText().toString()+"\n";
        eText.setText("");
        new sendMessage().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg,this.myport);
    }

    private static class sendMessage extends AsyncTask<String,Void,Void> {
        @Override
        protected Void doInBackground(String... messages) {
            try {
                Log.d("Starting ","sendMessage background");
                String msgToSend = messages[0];
                String mport = messages[1];
                Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(REMOTE_PORT0));
                Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(REMOTE_PORT1));
                Socket socket2 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(REMOTE_PORT2));
                Socket socket3 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(REMOTE_PORT3));
                Socket socket4 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(REMOTE_PORT4));
                ArrayList<Socket> socketArrayList = new ArrayList<Socket>();
                socketArrayList.add(socket0);
                socketArrayList.add(socket1);
                socketArrayList.add(socket2);
                socketArrayList.add(socket3);
                socketArrayList.add(socket4);

                int remote = 0;
                int count = 0;
                ArrayList<Integer> proposals = new ArrayList<Integer>();
                ArrayList<String> pros = new ArrayList<String>();
                for (Socket s:socketArrayList) {
                    if(s.isConnected() && msgToSend.length()>0){
                        s.setTcpNoDelay(true);
                        Log.d("Connected","True");
                        Log.d("Sending to","avd"+ Integer.toString(count));
                        DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                        DataInputStream disAck = new DataInputStream((s.getInputStream()));

                        dos.writeUTF(msgToSend+":"+mport);
                        dos.flush();
                        Log.d("Ack","Received ACK ");

                        String proposalReceived = null;
                        try {
                            proposalReceived = disAck.readUTF();
                        }catch(EOFException eof){
                            Log.d("Client","Catcing EOF Exception in proposalReceive");
                            remote = s.getPort();
                            count++;
                            continue;
                        }
                        if(!proposalReceived.isEmpty()){
                            Log.d("Proposal is",proposalReceived);
                            proposals.add(Integer.parseInt(proposalReceived));
                            pros.add(proposalReceived.substring(0,1));
                        }
                        else{
                            Log.d("Waiting ","for Proposal");
                            Thread.sleep(500);
                        }

                        //dos.close();
                        //disAck.close();
                        Thread.sleep(200);
                        s.close();
                    }
                    count++;
                }

                String finalSeqNo = Integer.toString(Collections.max(proposals));
                Log.d("Seq no for ",finalSeqNo);
                Log.d("Sending","final sequence no to everyone");
                Log.d("Client","Failed node is: "+Integer.toString(remote));
                for (int item :proposals){
                    String temp = Integer.toString(item);
                    String port = temp.substring(temp.length()-5);
                    Log.d("Client","port is"+port);
                    Socket sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(port));
                    sock.setTcpNoDelay(true);
                    DataOutputStream seqDos = new DataOutputStream((sock.getOutputStream()));
                    String seqString = "SEQ:"+temp+":"+ finalSeqNo+":"+Integer.toString(remote);
                    seqDos.writeUTF(seqString);
                    seqDos.flush();

                    Log.d("Client","Sent final seq to: "+port);
                    Thread.sleep(500);
                    Log.d("Sequence ACK","Received");
                    //seqDis.close();
                    //seqDos.close();
                    sock.close();
                }
                Log.d("Client Task","Finished");

            }
            catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

}
