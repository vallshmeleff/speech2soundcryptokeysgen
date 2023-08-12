package com.example.text2speechnn;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Logger;

//--------------------------------------------------
//
// Synthesize speech from text New Version TTS. Synthesizes and writes to a file in /data/data/...
// Generates encryption keys from an audio file
//
// The application synthesizes the vernam.wav sound file from the text specified in the textToSay String variable.
// Next, read from the audio 60 bytes, convert them to positive integers and get the key for the Vernam cipher.
// The key is shown in the Log.e and in the EditText
// If you run the application multiple times, you can see that the "created" 60 numbers are the same. This allows you to create an encryption system at the sender and recipient without synchronizing the exchange process or generating encryption keys
// Algorithm: An EditText is added to the application, in which the user enters the source text - for example, the first phrase from the first news of a predetermined site posted at 12:00 (Or from a newspaper, magazine, announcement system)
// Based on this phrase, the application creates N numbers that are used for XOR operations of message encryption (Vernam cipher).
// Here N is the length of the message.
// The receiving party only needs to read the news on the same site at the same time and enter the same phrase into the application. Then exactly the same encryption key will be generated as that of the transmitting side.
// PS. The algorithm for selecting bytes for a key from an audio file can be made much more complicated than shown here. The sender and recipient must have the same smartphones with the same OS versions.
// Another interesting option for independent synchronous generation of encryption keys is the use of asymmetric huge programming objects.
// Developing such a demo project without using a team of programmers will require significantly more time and technical effort.
//
// Email:
// 3f9d45e41863fa14dac44196ff1401a4c6aa230951f0924db95925409c2ac23b30bd22bcb39be1adca6074f3b315939075272c6093d5f70fb8
// Key:
// 6fef2a836a029779bfb66db69a7a66cda8cf467b7dd0fb23cf3c4b34f358e20610cb43d0dfee8cdea20d119fd673f5d0124a4d09fffb9460d5
//
// Levice Crypto Freeware
//
// http://vernamcode.orgfree.com/
//
// --------------------------------------------------
public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private TextToSpeech repeatTTS;
    Button btnspeakout;
    EditText edtTexttoSpeak;
    // public static Context context;
    public File file = null;
    public File path = null;
    public String destFileName = "vernam.wav";
    public int[] vernamkey = new int[60]; // An array of integers to write the encryption key from the sound file
    public int[] vernamkey1 = new int[60]; // An array of integers to write the encryption key from the sound file
    public int[] vernamkey2 = new int[60]; // An array of integers to write the encryption key from the sound file
    public int content; // FFile content
    public int x = 0; // Encryption key1 write counter 0-19
    public int n = 0; // Encryption key1 write counter 0-19
    public int m = 0; // Encryption key2 write counter 0-19
    public int g = 0; // Encryption key2 write counter 0-19


    public static String ie = ""; // Vernam Key in INT
    public static String hexie = ""; // Vernam Key in HEX
    public static String sysmsg = "";

    public static final String KEY_PARAM_UTTERANCE_ID = "utteranceId";
    public static long length = 0; // Audio file size
    public static int cnter = 0; // File position counter
    public static int nonzerocounter = 0; // Zero Bytes Count in file

    HashMap<String, String> myHashRender = new HashMap();
    public static String wakeUpText = "Hello world, this is a test message! Find the best answer to your technical question, help others answer theirs";
    public TextToSpeech textToSpeechSystem;
    public static String gh = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        TextView Guide = (TextView) findViewById(R.id.Guide);
        Guide.setMovementMethod(new ScrollingMovementMethod());

        Context context = getApplicationContext();
        path = context.getApplicationContext().getFilesDir();
        file = new File(path, "/" + "vernam.wav");
        file.setWritable(true);

        destFileName = file.toString();
        Log.e("FILES Path", "== Audio Files path == " + destFileName);

        //////////ontheStart(); // Text to Speech to file

    } // OnCreate



    // @Override necessary if you use onStart instead of ontheStart
    // @SuppressWarnings("deprecation")
    protected void ontheStart() {
        final String utteranceId = "myTestingId"; // Just a unique identifier
        Bundle bundleTts = new Bundle();
        super.onStart();
        textToSpeechSystem = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                textToSpeechSystem.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {

                        Log.i("TTS onInit", "==== TextToSpeech Started ====");
                    }
                    @Override
                    public void onDone(String utteranceId) {
                        Log.i("TTS onInit", "==== TextToSpeech Done ====");
                        ReadFile(); // Read file and show 20 bytes of recorded sound file
                        sysmsg = "Vernam Key generated in INT and in HEX (copied to clipboard)";
                        handler.sendEmptyMessage(0); // Write to Edit Text
                    }
                    @Override
                    public void onError(String utteranceId) {
                        Log.i("TTS onInit", "==== TextToSpeech Error ====");
                    }
                        });
                // == Не произносить == textToSpeechSystem.speak(wakeUpText, TextToSpeech.QUEUE_FLUSH, null, "doesn't matter yet");
                myHashRender.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "12345"); // HashMap myHashRender
                textToSpeechSystem.synthesizeToFile(wakeUpText,bundleTts, new File(destFileName), "utteranceId");
                sysmsg = "Text to File started";
                handler.sendEmptyMessage(0); // Write to Edit Text

            } // onInit
        });
    }


    public void ReadFile(){ // Read a file with synthesized speech
        NoneZeroByteCount(); // Count None Zero bytes in file
        FileInputStream fis = null;
        length = file.length(); // Audio file size
        try {
            fis = new FileInputStream(path +"/" + "vernam.wav");
            Log.i("== ReadFile =", " == == Total file size to read (in bytes) == == "+ fis.available());

            //x = 0; // Encryption key1 write counter 0-59
            g = 0; // vernamkey[g]
            n = 0; // vernamkey1[n]
            m = 0; // vernamkey2[m]

            while ((content = fis.read()) != -1) {
                if (content > 0) {
                    KeyBuild3(); // Key Build 3
                    cnter++; // File position counter
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null)
                    fis.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

    } // ReadFile


    //----------------------------------------------
    //
    // Count the number of bytes in the file that are not equal to zero
    //
    //----------------------------------------------
    public void NoneZeroByteCount() { // Zero Bytes Count in file
        FileInputStream fis = null;
        length = file.length(); // Audio file size
        try {
            fis = new FileInputStream(path +"/" + "vernam.wav");
            while ((content = fis.read()) != -1) {

                if (content > 0) {
                    nonzerocounter++;
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null)
                    fis.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        Log.i("== ReadFile =", " == == None Zero Bytes in File == == " + String.valueOf(nonzerocounter));




    } // Zero Bytes Count in file




    //----------------------------------------------
    //
    // Make Vernam Key (ver 3.0)
    //
    //----------------------------------------------
    public void KeyBuild3() { // Key Build 3
        double numblocks = Math.floor(nonzerocounter/61); // Block length WITHOUT NULL BYTES to search for the Encryption Key byte
        int i = (int) numblocks; // INT block length without nulls
        //Log.i("== KeyBuild3 =", " == == Index n == == " + String.valueOf(n));

            // comtent - int
        if (g <= 59) {
            if (cnter == 511 + g * i) {
                vernamkey[g] = content; // Write to vernamkey[]
                ////== ie = ie + String.valueOf(vernamkey[g]) + " "; // Vernam Kei in INT
                ////==hexie = hexie + Integer.valueOf(String.valueOf(vernamkey[g]), 16) + " "; // Vernam Kei in HEX
                g++;
                gh = gh + "*";
                sysmsg = gh;
                handler.sendEmptyMessage(0); // Write to Edit Text
            }
        }

            if (n <= 59) {
            if (cnter == 512 + n * i) {
                vernamkey1[n] = content; // Write to vernamkey[]
                vernamkey2[n] = vernamkey[n] ^ vernamkey1[n];

                ie = ie + String.valueOf(vernamkey1[n]) + " "; // Vernam Key 2 in INT
                hexie = hexie + Integer.valueOf(String.valueOf(vernamkey1[n]), 16) + " "; // Vernam Key 2 in HEX
                n++;
            }

        }


    } // KeyBuild



    public void copyToClipboardHEX(View v) { // Copy Vernam Key in HEX to Clipboard
        EditText cxtv = findViewById(R.id.VernamKeyHex); // Generated Vernam Key in HEX

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("label", cxtv.getText());
        clipboard.setPrimaryClip(clip);
    }

    public void copyToClipboardINT(View v) { // Copy Vernam Key in INT to Clipboard
        EditText cxtv = findViewById(R.id.VernamKeyInt); // Generated Vernam Key in INT

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("label", cxtv.getText());
        clipboard.setPrimaryClip(clip);
    }




    @Override
    public void onInit(int status) {

    }

    @Override
    public void onPause() {
        if (textToSpeechSystem != null) {
            textToSpeechSystem.stop();
            textToSpeechSystem.shutdown();
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (textToSpeechSystem != null) {
            textToSpeechSystem.stop();
            textToSpeechSystem.shutdown();
        }
        super.onDestroy();
    }






    //--------------------------------------------
    //
    // Output on display
    //
    //--------------------------------------------
    Handler handler = new Handler(Looper.getMainLooper()) { // Print Text on screen
        @Override
        public void handleMessage(Message msg) {

            EditText smsg = findViewById(R.id.SysMessage); // Generated Vernam Key in INT
            smsg.setText(MainActivity.sysmsg);
            EditText sxtv = findViewById(R.id.VernamKeyInt); // Generated Vernam Key in INT
            sxtv.setText(MainActivity.ie);
            EditText cxtv = findViewById(R.id.VernamKeyHex); // Generated Vernam Key in HEX
            cxtv.setText(MainActivity.hexie);

        }
    };


    //-----------------------------------------------------
    // Exit
    //-----------------------------------------------------
    public void onButtonExit(View arg0) {
        moveTaskToBack(true);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);

    }

    //-----------------------------------------------------
    // Generate
    //-----------------------------------------------------
    public void onButtonGenerate(View arg0) {

        ontheStart(); // Text to Speech to file

    }






} // MainActivity