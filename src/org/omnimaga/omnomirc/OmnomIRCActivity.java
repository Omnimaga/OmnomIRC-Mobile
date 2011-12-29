package org.omnimaga.omnomirc;

import com.loopj.android.http.*;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

public class OmnomIRCActivity extends Activity {
    /** Called when the activity is first created. */
	
	String signature;
	String username = "Guest";
	String channel;
	String curLine;
	Boolean connected = false;
	AsyncHttpClient httpclient = new AsyncHttpClient();
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        TextView text = (TextView)findViewById(R.id.textOutput);

        text.setMovementMethod(new ScrollingMovementMethod());

        connect();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.reconnect:
        	connected = false;
        	connect();
            return true;
        case R.id.prefs:
        	Intent myIntent = new Intent(getBaseContext(), Prefs.class);
            startActivityForResult(myIntent, 0);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    public void connect()
    {
        TextView text = (TextView)findViewById(R.id.textOutput);
        
        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        String user = app_preferences.getString("username", "");
        String pass = app_preferences.getString("password", "");
        channel = app_preferences.getString("channel", "#omnimaga");
        
        Auth(user, pass);
        
        if(signature.equals("") && username.equals("Guest"))
        {
        	text.append("Login failed. Wrong username or password.\n");
        }
        else
        {
        	text.append("Successfully logged in. Welcome "+username+"!\n");
        	load();
        }
    }
    
    public void Auth(String user, String pass)
    {
		DefaultHttpClient httpclient = new DefaultHttpClient();
        
        try {
            HttpGet httpget = new HttpGet("http://www.omnimaga.org/index.php?action=login");

            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();

            Log.d("OmnomAuth", "Login form get: " + response.getStatusLine());
            //EntityUtils.consume(entity);

            Log.d("OmnomAuth", "Initial set of cookies:");
            List<Cookie> cookies = httpclient.getCookieStore().getCookies();
            if (cookies.isEmpty()) {
                Log.d("OmnomAuth", "None");
            } else {
                for (int i = 0; i < cookies.size(); i++) {
                	Log.d("OmnomAuth", "- " + cookies.get(i).toString());
                }
            }

            HttpPost httpost = new HttpPost("http://www.omnimaga.org/index.php?action=login2");

            List <NameValuePair> nvps = new ArrayList <NameValuePair>();
            nvps.add(new BasicNameValuePair("user", user));
            nvps.add(new BasicNameValuePair("passwrd", pass));
            nvps.add(new BasicNameValuePair("cookieneverexp", "true"));

            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

            response = httpclient.execute(httpost);
            entity = response.getEntity();

            Log.d("OmnomAuth", "Login form get: " + response.getStatusLine());
            //EntityUtils.consume(entity);

            Log.d("OmnomAuth", "Post logon cookies:");
            cookies = httpclient.getCookieStore().getCookies();
            if (cookies.isEmpty()) {
            	Log.d("OmnomAuth", "None");
            } else {
                for (int i = 0; i < cookies.size(); i++) {
                	Log.d("OmnomAuth", "- " + cookies.get(i).toString());
                }
            }
            
            HttpGet httpget2 = new HttpGet("http://www.omnimaga.org/checkLogin.php?txt");

            response = httpclient.execute(httpget2);
            entity = response.getEntity();
            
            String page = EntityUtils.toString(entity);

            signature = page.split("\n")[0];
            username = page.split("\n")[1];
            
            Log.d("OmnomAuth", "Signature: "+signature+"\n");
            Log.d("OmnomAuth", "Username: "+username+"\n");

        } catch (Exception e) {
			Log.e("OmnomAuth", e.getMessage());
		} finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();
        }
    }
    
    public void load()
    {
    	SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String linesNum = "50"; //app_preferences.getString("scrollback", "50");
		
		RequestParams params = new RequestParams();
		params.put("count", linesNum);
		params.put("channel", new String(Base64.encodeBytes(channel.getBytes())));
		params.put("nick", new String(Base64.encodeBytes(username.getBytes())));
		params.put("signature", new String(Base64.encodeBytes(signature.getBytes())));
		
		httpclient.get("http://omnomirc.www.omnimaga.org/load.php", params, new AsyncHttpResponseHandler()
		{
			@Override
			public void onSuccess(String response)
			{
	            String lines[] = response.split(";");
	            for(int i=0; i<lines.length; i++)
	            {
	            	if(lines[i].startsWith("addLine('"))
	            	{
	            		addLine(lines[i].substring(9, lines[i].length()-2));
	            	}
	            }
	            connected = true;
	            update();
			}
		});
		/*
		try {
			HttpGet httpget = new HttpGet("http://omnomirc.www.omnimaga.org/load.php?"+"&signature="+);
			HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            
            String page = EntityUtils.toString(entity);
            String lines[] = page.split(";");
            for(int i=0; i<lines.length; i++)
            {
            	if(lines[i].startsWith("addLine('"))
            	{
            		addLine(lines[i].substring(9, lines[i].length()-2));
            	}
            }
        } catch (Exception e) {
			Log.e("OmnomLoad", "Error: "+e.getMessage());
		} finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();
        }*/
    }
    
    AsyncHttpResponseHandler updateHandler = new AsyncHttpResponseHandler()
	{
		@Override
		public void onSuccess(String response)
		{
            String lines[] = response.split("\n");
            for(int i=0; i<lines.length; i++)
            {
            	addLine(lines[i]);
            }/*
            try {
				//wait(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
            update();
		}
		
		public void onFailure(Throwable t)
		{
	        TextView text = (TextView)findViewById(R.id.textOutput);
	        
			connected = false;
			text.append(Html.fromHtml("<font color=\"#C73232\">Connection lost. Please reconnect.</font><br/>"));
			Log.e("update", t.getMessage(), t);
		}
	};
    
    public void update()
    {
		Log.d("update", "Updating");
		
		RequestParams params = new RequestParams();
		params.put("lineNum", curLine);
		params.put("channel", new String(Base64.encodeBytes(channel.getBytes())));
		params.put("nick", new String(Base64.encodeBytes(username.getBytes())));
		params.put("signature", new String(Base64.encodeBytes(signature.getBytes())));
		
		httpclient.get("http://omnomirc.www.omnimaga.org/update.php", params, updateHandler);
		
		Log.d("update", "Update complete");
		/*
		try {
			HttpGet httpget = new HttpGet("http://omnomirc.www.omnimaga.org/update.php?"+"&channel="+new String(Base64.encodeBytes(channel.getBytes()))+"&nick="+new String(Base64.encodeBytes(username.getBytes()))+"&signature="+new String(Base64.encodeBytes(signature.getBytes())));
			HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            
            String page = EntityUtils.toString(entity);
            String lines[] = page.split("\n");
            for(int i=0; i<lines.length; i++)
            {
            	addLine(lines[i]);
            }
        } catch (Exception e) {
			Log.e("OmnomLoad", "Error: "+e.getMessage());
		} finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();
        }
        */
    }
    
    public void addLine(String message)
    {
        TextView text = (TextView)findViewById(R.id.textOutput);
        
    	String[] messageParts = message.split(":");
    	if(messageParts.length >= 4)
    	{
	    	for (int i=4;i<messageParts.length;i++)
	    	{
				try {
					messageParts[i] = new String(Base64.decode(messageParts[i].replace(',', '='), Base64.URL_SAFE));
				} catch (IOException e) {
					Log.e("Base64Decode", e.getMessage());
				}
	    	}
	    	curLine = messageParts[0]; //This is a global var for a reason
	    	String type = messageParts[1];
	    	String online = messageParts[2];
	    	Date time = new Date(Long.parseLong(messageParts[3])*1000);
	    	Log.d("addLine", message);
	    	if(!type.equals("curline"))
	    		text.append("[" + DateFormat.getTimeInstance(DateFormat.MEDIUM).format(time) + "] ");
	    	if(type.equals("pm"))
	    	{
	    		text.append("[PM]Ê");
	    		//TODO: got ping'd, do something
	    	}
	    	if(type.equals("message") || type.equals("pm"))
	    	{
	    		text.append(Html.fromHtml("&lt;<font color=\"" + color_of(messageParts[4]) + "\">" + TextUtils.htmlEncode(messageParts[4]) + "</font>&gt; " + TextUtils.htmlEncode(messageParts[5])));
	    	}
	    	if(type.equals("message") || type.equals("action"))
	    	{
	    		if(messageParts[5].contains(username))
	    		{
	    			//TODO: got ping'd, do something
	    		}
	    	}
	    	if(type.equals("action"))
	    	{
	    		text.append(Html.fromHtml("* <font color=\"" + color_of(messageParts[4]) + "\">" + TextUtils.htmlEncode(messageParts[4]) + "</font> " + TextUtils.htmlEncode(messageParts[5])));
	    	}
	    	if(type.equals("join"))
	    	{
	    		text.append(Html.fromHtml("* <font color=\"" + color_of(messageParts[4]) + "\">" + TextUtils.htmlEncode(messageParts[4]) + "</font> <font color=\"#2A8C2A\">has joined "+channel+"</font>"));
	    	}
	    	if(type.equals("quit"))
	    	{
	    		if(messageParts.length >= 6)
	    			text.append(Html.fromHtml("* <font color=\"" + color_of(messageParts[4]) + "\">" + TextUtils.htmlEncode(messageParts[4]) + "</font> <font color=\"#66361F\">has quit IRC ("+ TextUtils.htmlEncode(messageParts[5]) +")</font>"));
	    		else
	    			text.append(Html.fromHtml("* <font color=\"" + color_of(messageParts[4]) + "\">" + TextUtils.htmlEncode(messageParts[4]) + "</font> <font color=\"#66361F\">has quit IRC</font>"));
	    	}
	    	if(type.equals("part"))
	    	{
	    		if(messageParts.length >= 6)
	    			text.append(Html.fromHtml("* <font color=\"" + color_of(messageParts[4]) + "\">" + TextUtils.htmlEncode(messageParts[4]) + "</font> <font color=\"#66361F\">has left "+channel+" ("+ TextUtils.htmlEncode(messageParts[5]) +")</font>"));
	    		else
	    			text.append(Html.fromHtml("* <font color=\"" + color_of(messageParts[4]) + "\">" + TextUtils.htmlEncode(messageParts[4]) + "</font> <font color=\"#66361F\">has left "+channel+"</font>"));
	    	}
	    	if(type.equals("mode"))
	    	{
	    		text.append(Html.fromHtml("* <font color=\"" + color_of(messageParts[4]) + "\">" + TextUtils.htmlEncode(messageParts[4]) + "</font> <font color=\"#2A8C2A\">set "+channel+" mode "+ TextUtils.htmlEncode(messageParts[5]) +"</font>"));
	    	}
	    	if(type.equals("nick"))
	    	{
	    		text.append(Html.fromHtml("* <font color=\"" + color_of(messageParts[4]) + "\">" + TextUtils.htmlEncode(messageParts[4]) + "</font> <font color=\"#2A8C2A\">has changed his nick to</font> <font color=\"" + color_of(messageParts[5]) + "\">"+ TextUtils.htmlEncode(messageParts[5]) +"</font>"));
	    	}
	    	if(type.equals("kick"))
	    	{
	    		text.append(Html.fromHtml("* <font color=\"" + color_of(messageParts[4]) + "\">" + TextUtils.htmlEncode(messageParts[4]) + "</font> <font color=\"#C73232\">has kicked</font> <font color=\"" + color_of(messageParts[5]) + "\">" + TextUtils.htmlEncode(messageParts[5]) + "</font> <font color=\"#C73232\">from "+channel+" ("+ TextUtils.htmlEncode(messageParts[6]) +")</font>"));	
	        }
	    	if(type.equals("topic"))
	    	{
	    		text.append(Html.fromHtml("* <font color=\"" + color_of(messageParts[4]) + "\">" + TextUtils.htmlEncode(messageParts[4]) + "</font> set topic to " + TextUtils.htmlEncode(messageParts[5])));
	    	}
	    	text.append("\n");
    	}
    }
    
	public String color_of(String name)
	{
    	String rcolors[] = { "#2A8C2A", "#C33B3B", "#80267F", "#D9A641", "#3DCC3D", "#1A5555", "#2F8C74", "#4545E6", "#B037B0" };
    	int sum = 0;

    	for (int i = 0; i < name.length(); i++)
    		sum += name.getBytes()[i];
    	sum %= rcolors.length;
    	return rcolors[sum];
    }
}