/**
 * Alternate Ouya Store
 Copyright (C) 2013  ProfessorPopTart

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.ProfessorPopTart.alternateouyastore;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ListView;
import java.util.ArrayList;
import android.graphics.Bitmap;
import java.io.InputStream;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.content.Context;
import android.widget.Toast;
import android.os.AsyncTask;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;
import org.apache.http.HttpEntity;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import android.content.Intent;
import android.net.Uri;

public class MainActivity extends Activity {

    ArrayAdapter<String> GameListAdapter;
    ArrayList<String> GameList;
    private static String TAG_URL = "https://devs.ouya.tv/api/v1/apps";
    private static final String TAG_TOKEN = "f6e0f5a2-aee2-4a05-80f2-66e3ee6ef456";
    String HttpResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GameList = new ArrayList<String>();
        GameListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, GameList);
        final ListView myListView = (ListView) findViewById(R.id.appsLBX);
        final TextView myGameName = (TextView) findViewById(R.id.gameName);
        myListView.setAdapter(GameListAdapter);
        myListView.setTextFilterEnabled(true);

        //assign onclick event to getListBTN
        Button myGetListBTN = (Button) findViewById(R.id.getListBTN);
        myGetListBTN.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                populateList();
            }
        });

        //assign onclick event to downloadGameBTN
        Button myDownloadBTN = (Button) findViewById(R.id.donwloadBTN);
        myDownloadBTN.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                buildDownloadRequest(myGameName.getText().toString());
            }
        });

        //assign onSelected event to appsLBX
        myListView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedFromList = (myListView.getItemAtPosition(i).toString());
                displayImage(getImgUrl(selectedFromList));
                myGameName.setText(selectedFromList);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * This is used to fill gameList with data from the JSON apps object
     * <p>
     * The object is then used as a dataset for the ListView that shows the game titles
     *
     * @param  gameList  an ArrayList of strings
     */
    public void fillList(ArrayList<String> gameList)
    {

        try{
            JSONObject MyResults = new JSONObject(HttpResults);

            for(int i=0;i< MyResults.getJSONArray("apps").length();i++)
            {
                String title = MyResults.getJSONArray("apps").getJSONObject(i).getString("title");
                gameList.add(i,title);
            }
        } catch(Exception e){
            e.printStackTrace();
        }
        GameListAdapter.notifyDataSetChanged();
    }

    /**
     * This is will kick off the task of getting an image from a url
     *
     * @param  imgUrl  String url to the image file
     */
    public void displayImage(String imgUrl)
    {
        new DownloadImageTask((ImageView) findViewById(R.id.gameImage))
                .execute(imgUrl);
    }

    /**
     * This is will kick off the task of refreshing the list of games.
     */
    public void populateList()
    {
        GameList.clear();
        new getJsonTask()
                .execute();
    }

    /**
     * This is will send a toast message.
     *
     * @param  msg  String message to display to the user.
     */
    private void toastMessage(String msg)
    {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        if(context != null)
        {
            Toast toast = Toast.makeText(context, msg, duration);
            toast.show();
        }
    }

    /**
     * This class will start an image download as an Asynchronous task.
     */
    class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urlDisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urlDisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

    /**
     * This class will start the JSON request for the list of games as an Asynchronous task.
     */
    class getJsonTask extends AsyncTask<String, Void, String> {
        String Results;
        private void getJsonTask(String MyResults){
            this.Results = MyResults;
        }
        protected String doInBackground(String... urls) {
            InputStream is = null;
            try {
                HttpClient httpclient = new DefaultHttpClient();
                HttpGet httpPost = new HttpGet(TAG_URL);
                HttpResponse response = httpclient.execute(httpPost);
                HttpEntity entity = response.getEntity();
                is = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null)
                    sb.append(line + "\n");

                String resString = sb.toString();

                is.close();
                return resString;
            }
            catch(Throwable t) {
                t.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(String result) {
            HttpResults = result;
            fillList(GameList);
        }
    }

    /**
     * This class will start the JSON request for the download url of a game as an Asynchronous task.
     */
    class getJsonUrlTask extends AsyncTask<String, Void, String> {
        String downloadUrlResults;
        private void getJsonUrlTask(String MyResults){
            this.downloadUrlResults = MyResults;
        }
        protected String doInBackground(String... urls) {
            InputStream is = null;
            try {
                HttpClient httpclient = new DefaultHttpClient();
                HttpGet httpPost = new HttpGet(urls[0]);
                HttpResponse response = httpclient.execute(httpPost);
                HttpEntity entity = response.getEntity();
                is = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null)
                    sb.append(line + "\n");

                String resString = sb.toString();

                is.close();
                return resString;
            }
            catch(Throwable t) {
                t.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(String result) {
            downloadUrlResults = result;
            downloadApk(downloadUrlResults);
        }
    }

    /**
     * This will get the img url of a game from the JSON apps response
     *
     * @param  title String title of the game.
     * @returns the imgUrl as a string.
     */
    private String getImgUrl(String title){
        String url = "x";
        try{
            JSONObject MyResults = new JSONObject(HttpResults);

            for(int i=0;i< MyResults.getJSONArray("apps").length();i++)
            {
                String searchTitle = MyResults.getJSONArray("apps").getJSONObject(i).getString("title");
                String searchUrl = MyResults.getJSONArray("apps").getJSONObject(i).getString("mainImageFullUrl");
                if(searchTitle.equals(title))
                {
                    url = searchUrl;
                }
            }
        } catch(Exception e){
            e.printStackTrace();
        }
        GameListAdapter.notifyDataSetChanged();
        return url;
    }

    /**
     * This will get the "version" (uid) of a game from the JSON apps response
     *
     * @param title String title of the game.
     * @returns the version as a string.
     */
    private String getUuid(String title){
        String version = "x";
        try{
            JSONObject MyResults = new JSONObject(HttpResults);

            for(int i=0;i< MyResults.getJSONArray("apps").length();i++)
            {
                String searchTitle = MyResults.getJSONArray("apps").getJSONObject(i).getString("title");
                String searchVersion = MyResults.getJSONArray("apps").getJSONObject(i).getString("version");
                if(searchTitle.equals(title))
                {
                    version = searchVersion;
                }
            }
        } catch(Exception e){
            e.printStackTrace();
        }
        GameListAdapter.notifyDataSetChanged();
        return version;
    }

    /**
     * This will build the download url JSON request for a game from the JSON apps response
     *
     * @param title the String title of the game
     */
    private void buildDownloadRequest(String title){
        String downloadUrl = "https://devs.ouya.tv/api/v1/apps/" + getUuid(title) + "/download?auth_token=" + TAG_TOKEN;
        new getJsonUrlTask()
                .execute(downloadUrl);
    }

    /**
     * This will get the img url of a game from the JSON response
     *
     * @param urlResults  String JSON response form the download url request
     * @returns the link to download the apk
     */
    private String getLink(String urlResults){
        try{
            JSONObject MyResults = new JSONObject(urlResults);
            String url = MyResults.getJSONObject("app").getString("downloadLink");
            return url;
        }
        catch(Exception e){
            e.printStackTrace();
            return "";
        }
    }

    /**
     * This will start the download of the apk file
     *
     * @param urlResults  String JSON response form the download url request
     */
    private void downloadApk(String urlResults)
    {
        String link = getLink(urlResults);
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(link));
        startActivity(i);
    }
}