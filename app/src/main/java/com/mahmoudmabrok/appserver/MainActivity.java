package com.mahmoudmabrok.appserver;

import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.mahmoudmabrok.appserver.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String SERVER_IP = "127.0.0.1";
    private final int PORT = 7000;
    Thread serverThread;
    ServerSocket serverSocket;
    TextView textView;
    private String IP;
    private Socket socket;
    private PrintWriter output;
    private BufferedReader input;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.tvContented);

        startServer();
    }

    private void startServer() {
        try {
            IP = getLocalIpAddress();
            Log.d(TAG, "startServer: " + IP);
            serverThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        serverSocket = new ServerSocket(PORT);
                        Log.d(TAG, "run: servered connected");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText("NA");
                            }
                        });
                        // start listening
                        socket = serverSocket.accept();
                        // writer to client
                        output = new PrintWriter(socket.getOutputStream());
                        // input from clients
                        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        Log.d(TAG, "run: client connected");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText("Connected to client " +
                                        socket.getInetAddress().getHostAddress());
                            }
                        });

                        new Thread(new ClientInput()).start();


                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });

            serverThread.start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void accept(View view) {
        new Thread(new ClientOutput("accept")).start();
    }

    public void reject(View view) {
        new Thread(new ClientOutput("reject")).start();
    }


    private String getLocalIpAddress() throws UnknownHostException {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager != null;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
    }

    private class ClientInput implements Runnable {
        @Override
        public void run() {
            String message = "";
            while (message != null) {
                try {
                    message = input.readLine();
                    Log.d(TAG, "ClientInput: " + message);
                    if (message != null) {
                        Log.d(TAG, "get correct client: " + message);

                        if (message.equals("reject")) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this,
                                            "SERVER:: get Reject from client",
                                            Toast.LENGTH_SHORT)
                                            .show();
                                }
                            });
                        } else {
                            Intent intent = new Intent(MainActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                            startActivity(intent);
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    class ClientOutput implements Runnable {
        private String message;

        ClientOutput(String message) {
            this.message = message;
        }

        @Override
        public void run() {
            output.write(message + '\n');
            output.flush();
            //  Log.d(TAG, "sent from server : " + message);
        }
    }
}
