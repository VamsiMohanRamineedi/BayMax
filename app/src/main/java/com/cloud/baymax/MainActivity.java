package com.cloud.baymax;

import android.*;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.ibm.mobilefirstplatform.clientsdk.android.analytics.api.Analytics;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneInputStream;
import com.ibm.watson.developer_cloud.android.library.audio.StreamPlayer;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.Voice;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneInputStream;
import com.ibm.watson.developer_cloud.android.library.audio.StreamPlayer;
import com.ibm.watson.developer_cloud.android.library.audio.utils.ContentType;
import com.ibm.watson.developer_cloud.conversation.v1.ConversationService;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageRequest;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageResponse;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechResults;
import com.ibm.watson.developer_cloud.speech_to_text.v1.websocket.RecognizeCallback;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.Voice;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.*;
import com.ibm.mobilefirstplatform.clientsdk.android.analytics.api.*;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.*;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final TextToSpeech service = new TextToSpeech();
    private FloatingActionButton fab;
    private EditText editText = null;
    private RecyclerView recyclerView;
    private ChatAdapter mAdapter;
    private ArrayList messageArrayList;
    private EditText inputMessage;
    private ImageButton btnSend;
    private ImageButton btnRecord;
    private Map<String,Object> context = new HashMap<>();
    StreamPlayer streamPlayer;
    private boolean initialRequest;
    private boolean permissionToRecordAccepted = false;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String TAG = "MainActivity";
    private static final int RECORD_REQUEST_CODE = 101;
    private boolean listening = false;
    private SpeechToText speechService;
    private MicrophoneInputStream capture;
    private Logger myLogger;
    private Context mContext;
    private String workspace_id;
    private String conversation_username;
    private String conversation_password;
    private String STT_username;
    private String STT_password;
    private String TTS_username;
    private String TTS_password;
    private String analytics_APIKEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //Button mapButton = (Button) findViewById(R.id.button1);
        editText = (EditText) findViewById(R.id.editText);
        //mapButton.setOnClickListener(this);
        setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        /*fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/

        mContext = getApplicationContext();
        conversation_username = mContext.getString(R.string.conversation_username);
        conversation_password = mContext.getString(R.string.conversation_password);
        workspace_id = mContext.getString(R.string.workspace_id);
        STT_username = mContext.getString(R.string.STT_username);
        STT_password = mContext.getString(R.string.STT_password);
        TTS_username = mContext.getString(R.string.TTS_username);
        TTS_password = mContext.getString(R.string.TTS_password);
        analytics_APIKEY = mContext.getString(R.string.mobileanalytics_apikey);


        //Bluemix Mobile Analytics
        BMSClient.getInstance().initialize(getApplicationContext(), BMSClient.REGION_US_SOUTH);
        //Analytics is configured to record lifecycle events.
        Analytics.init(getApplication(), "WatBot", analytics_APIKEY, false, Analytics.DeviceEvent.ALL);
        //Analytics.send();
        myLogger = Logger.getLogger("myLogger");
        // Send recorded usage analytics to the Mobile Analytics Service
        Analytics.send(new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                // Handle Analytics send success here.
            }

            @Override
            public void onFailure(Response response, Throwable throwable, JSONObject jsonObject) {
                // Handle Analytics send failure here.
            }
        });

        // Send logs to the Mobile Analytics Service
        Logger.send(new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                // Handle Logger send success here.
            }

            @Override
            public void onFailure(Response response, Throwable throwable, JSONObject jsonObject) {
                // Handle Logger send failure here.
            }
        });

        inputMessage = (EditText) findViewById(R.id.message);
        btnSend = (ImageButton) findViewById(R.id.btn_send);
        //btnRecord= (ImageButton) findViewById(R.id.btn_record);
        String customFont = "Montserrat-Regular.ttf";
        Typeface typeface = Typeface.createFromAsset(getAssets(), customFont);
        inputMessage.setTypeface(typeface);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        messageArrayList = new ArrayList<>();
        mAdapter = new ChatAdapter(messageArrayList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);
        this.inputMessage.setText("");
        this.initialRequest = true;
        sendMessage();

        //Watson Text-to-Speech Service on Bluemix
        service.setUsernameAndPassword(TTS_username, TTS_password);

        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission to record denied");
            makeRequest();
        }


        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new ClickListener() {
            @Override
            public void onClick(View view, final int position) {
                Thread thread = new Thread(new Runnable() {
                    public void run() {
                        Message audioMessage;
                        try {

                            audioMessage =(Message) messageArrayList.get(position);
                            streamPlayer = new StreamPlayer();
                            if(audioMessage != null && !audioMessage.getMessage().isEmpty())
                                //Change the Voice format and choose from the available choices
                                streamPlayer.playStream(service.synthesize(audioMessage.getMessage(), Voice.EN_LISA).execute());
                            else
                                streamPlayer.playStream(service.synthesize("No Text Specified", Voice.EN_LISA).execute());

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
            }

            @Override
            public void onLongClick(View view, int position) {
                recordMessage();

            }
        }));

        btnSend.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(checkInternetConnection()) {
                    sendMessage();
                }
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                recordMessage();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.button1 :
                Intent mapIntent = new Intent(this,MapsActivity.class);
                mapIntent.putExtra("DestinationName","Times Square");
                startActivity(mapIntent);
                break;
            case R.id.button2 :
                if(editText.getText() != null){
                    String url= makeUrl(editText.getText().toString());
                    Log.d("UrlMade", url);
                    FetchUrl FetchUrl = new FetchUrl(this);

                    // Start downloading json data from Google Directions API
                    FetchUrl.execute(url);
                }
                break;
        }

    }

    private class FetchUrl extends AsyncTask<String, Void, String> {
        private Context context;

        public FetchUrl(Context context){
            this.context=context;
        }
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
                Log.d("MainActivityLog","Data fetched from ibm service = "+data);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask(context);

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);


        }

    }

    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, String> {

        private Context context;

        public ParserTask(Context context){
            this.context=context;
        }
        // Parsing the data in non-ui thread
        @Override
        protected String doInBackground(String... jsonData) {

            JSONObject jObject;
            String destination= null;

            try {
                jObject = new JSONObject(jsonData[0]);

                if(!(jObject.getJSONArray("entities").isNull(0))){
                    JSONObject entity =((JSONObject)jObject.getJSONArray("entities").get(0));
                    if(entity.get("type").toString().equalsIgnoreCase("Location")  ||
                            entity.get("type").toString().equalsIgnoreCase("Facility")){
                            destination=entity.get("text").toString();
                    }
                    else {
                        destination= ((JSONObject)jObject.getJSONArray("keywords").get(0)).get("text").toString();
                    }
                }
                else{
                    destination= ((JSONObject)jObject.getJSONArray("keywords").get(0)).get("text").toString();
                }


                Log.d("MainActivityLog","destination extracted = "+destination);


            } catch (Exception e) {
                Log.d("ParserTask",e.toString());
                e.printStackTrace();
            }
            return destination;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(String destination) {
            Intent mapIntent = new Intent(context,MapsActivity.class);
            Toast.makeText(context,"Destination Extracted :"+destination,Toast.LENGTH_LONG).show();
            mapIntent.putExtra("DestinationName",destination);
            startActivity(mapIntent);
        }
    }

    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            Log.d("downloadUrl", data.toString());
            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    private String makeUrl(String inputText){
        String firstPart="https://watson-api-explorer.mybluemix.net/natural-language-understanding/api/v1/analyze?version=2017-02-27&";

        String[] words = inputText.split(" ");
        StringBuilder sentence = new StringBuilder(words[0]);

        for (int i = 1; i < words.length; ++i) {
            sentence.append("%20");
            sentence.append(words[i]);
        }
        String secondPart="text="+sentence.toString();

        String thirdPart="&features=entities%2Ckeywords&return_analyzed_text=false&clean=true&fallback_to_raw=true&concepts.limit=8&emotion.document=true&entities.limit=50&entities.emotion=false&entities.sentiment=false&keywords.limit=50&keywords.emotion=false&keywords.sentiment=false&relations.model=en-news&semantic_roles.limit=50&semantic_roles.entities=true&semantic_roles.keywords=true&sentiment.document=true";

        return (firstPart+secondPart+thirdPart);
    }

    // Speech-to-Text Record Audio permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
            case RECORD_REQUEST_CODE: {

                if (grantResults.length == 0
                        || grantResults[0] !=
                        PackageManager.PERMISSION_GRANTED) {

                    Log.i(TAG, "Permission has been denied by user");
                } else {
                    Log.i(TAG, "Permission has been granted by user");
                }
                return;
            }
        }
        if (!permissionToRecordAccepted ) finish();

    }

    protected void makeRequest() {
        ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.RECORD_AUDIO},
                RECORD_REQUEST_CODE);
    }

    // Sending a message to Watson Conversation Service
    private void sendMessage() {

        final String inputmessage = this.inputMessage.getText().toString().trim();
        if(!this.initialRequest) {
            Message inputMessage = new Message();
            inputMessage.setMessage(inputmessage);
            inputMessage.setId("1");
            messageArrayList.add(inputMessage);
            myLogger.info("Sending a message to Watson Conversation Service");

        }
        else
        {
            Message inputMessage = new Message();
            inputMessage.setMessage(inputmessage);
            inputMessage.setId("100");
            this.initialRequest = false;
            Toast.makeText(getApplicationContext(),"Tap on the message for Voice",Toast.LENGTH_LONG).show();

        }

        this.inputMessage.setText("");
        mAdapter.notifyDataSetChanged();

        Thread thread = new Thread(new Runnable(){
            public void run() {
                try {

                    ConversationService service = new ConversationService(ConversationService.VERSION_DATE_2016_07_11);
                    service.setUsernameAndPassword(conversation_username, conversation_password);
                    MessageRequest newMessage = new MessageRequest.Builder().inputText(inputmessage).context(context).build();
                    MessageResponse response = service.message(workspace_id, newMessage).execute();

                    //Passing Context of last conversation
                    if(response.getContext() !=null)
                    {
                        context.clear();
                        context = response.getContext();
                        Log.i("MainActivity","Context = "+context);

                    }
                    Message outMessage=new Message();
                    if(response!=null)
                    {
                        Log.i("MainActivity","Response from converation service = "+response);
                        if(response.getOutput()!=null && response.getOutput().containsKey("text"))
                        {

                            ArrayList responseList = (ArrayList) response.getOutput().get("text");
                            if(null !=responseList && responseList.size()>0){
                                outMessage.setMessage((String)responseList.get(0));
                                outMessage.setId("2");
                            }
                            messageArrayList.add(outMessage);

                            speakOutMessage(outMessage);
                            analyzeIntent(response);
                        }

                        runOnUiThread(new Runnable() {
                            public void run() {
                                mAdapter.notifyDataSetChanged();
                                if (mAdapter.getItemCount() > 1) {
                                    recyclerView.getLayoutManager().smoothScrollToPosition(recyclerView, null, mAdapter.getItemCount()-1);

                                }

                            }
                        });


                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();

    }
    private void speakOutMessage(final Message outMessage){
        Thread thread = new Thread(new Runnable() {
            public void run() {
                Message audioMessage;
                try {

                    audioMessage =outMessage;
                    streamPlayer = new StreamPlayer();
                    if(audioMessage != null && !audioMessage.getMessage().isEmpty())
                        //Change the Voice format and choose from the available choices
                        streamPlayer.playStream(service.synthesize(audioMessage.getMessage(), Voice.EN_LISA).execute());
                    else
                        streamPlayer.playStream(service.synthesize("No Respone from BayMax", Voice.EN_LISA).execute());

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }
    private void analyzeIntent(MessageResponse response){
        ArrayList intentList = (ArrayList) response.getIntents();
        if(intentList!=null) {
            //Extracting Intent
            com.ibm.watson.developer_cloud.conversation.v1.model.Intent conversationIntent =
                    (com.ibm.watson.developer_cloud.conversation.v1.model.Intent) intentList.get(0);

            //Extracting InputText
            Map<String, Object> input = response.getInput();
            String inputText = (String) input.get("text");

            if (conversationIntent.getIntent().equalsIgnoreCase("give_directions") && conversationIntent.getConfidence()>0.85) {
                String url= makeUrl(inputText);
                Log.d("UrlMade", url);
                FetchUrl FetchUrl = new FetchUrl(this);

                // Start downloading json data from Google Directions API
                FetchUrl.execute(url);
            }

            Log.i("MainActivity", "Intent : " + conversationIntent.getIntent() + "InputText : " + inputText);
        }
    }

    //Record a message via Watson Speech to Text
    private void recordMessage() {
        //mic.setEnabled(false);
        speechService = new SpeechToText();
        speechService.setUsernameAndPassword(STT_username, STT_password);

        if(listening != true) {
            capture = new MicrophoneInputStream(true);
            new Thread(new Runnable() {
                @Override public void run() {
                    try {
                        speechService.recognizeUsingWebSocket(capture, getRecognizeOptions(), new MicrophoneRecognizeDelegate());
                    } catch (Exception e) {
                        showError(e);
                    }
                }
            }).start();
            listening = true;
            Toast.makeText(MainActivity.this,"Listening....Click to Stop", Toast.LENGTH_LONG).show();
            fab.setImageResource(R.mipmap.mic_on);

        } else {
            try {
                capture.close();
                listening = false;
                Toast.makeText(MainActivity.this,"Stopped Listening....Click to Start", Toast.LENGTH_LONG).show();
                fab.setImageResource(R.mipmap.ic_mic);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * Check Internet Connection
     * @return
     */
    private boolean checkInternetConnection() {
        // get Connectivity Manager object to check connection
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        // Check for network connections
        if (isConnected){
            return true;
        }
        else {
            Toast.makeText(this, " No Internet Connection available ", Toast.LENGTH_LONG).show();
            return false;
        }

    }

    //Private Methods - Speech to Text
    private RecognizeOptions getRecognizeOptions() {
        return new RecognizeOptions.Builder()
                .continuous(true)
                .contentType(ContentType.OPUS.toString())
                //.model("en-UK_NarrowbandModel")
                .interimResults(true)
                .inactivityTimeout(2000)
                .build();
    }

    //Watson Speech to Text Methods.
    private class MicrophoneRecognizeDelegate implements RecognizeCallback {

        @Override
        public void onTranscription(SpeechResults speechResults) {
            System.out.println(speechResults);
            if(speechResults.getResults() != null && !speechResults.getResults().isEmpty()) {
                String text = speechResults.getResults().get(0).getAlternatives().get(0).getTranscript();
                showMicText(text);
            }
        }

        @Override public void onConnected() {

        }

        @Override public void onError(Exception e) {
            showError(e);
            enableMicButton();
        }

        @Override public void onDisconnected() {
            enableMicButton();
        }

        @Override
        public void onInactivityTimeout(RuntimeException runtimeException) {

        }

        @Override
        public void onListening() {

        }
    }

    private void showMicText(final String text) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                inputMessage.setText(text);
            }
        });
    }

    private void enableMicButton() {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                //btnRecord.setEnabled(true);
                fab.setEnabled(true);
            }
        });
    }

    private void showError(final Exception e) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
    }
}