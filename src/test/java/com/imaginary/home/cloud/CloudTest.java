/*
 * Copyright (C) 2013 George Reese
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.imaginary.home.cloud;

import com.imaginary.home.controller.CloudService;
import com.imaginary.home.controller.CommunicationException;
import com.imaginary.home.controller.HomeController;
import com.imaginary.home.device.hue.Hue;
import com.imaginary.home.lighting.ColorMode;
import junit.framework.Assert;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

/**
 * Executes tests to verify the functionality of the cloud service API.
 * <p>Created by George Reese: 1/27/13 5:53 PM</p>
 * @author George Reese
 */
public class CloudTest {
    static private final Logger logger = Logger.getLogger("com.imaginary.home.cloud.test");

    static private final Random random  = new Random();
    static private final String VERSION = "2013-01";

    static private String apiKeyId;
    static private String apiKeySecret;
    static private String locationId;
    static private String pairingCode;
    static private String relayKeyId;
    static private String relayKeySecret;
    static private String token;

    private String cloudAPI;

    @Rule public final TestName name = new TestName();

    public CloudTest() { }

    @Before
    public void setUp() throws Exception {
        cloudAPI = System.getProperty("cloudAPI", "http://15.185.177.82:8080");
        File f = new File("target/iha");

        if( !f.exists() ) {
            //noinspection ResultOfMethodCallIgnored
            f.mkdir();
        }
        f = new File(HomeController.CONFIG_FILE);
        if( !f.exists() ) {
            HashMap<String,Object> cfg = new HashMap<String, Object>();

            cfg.put("name", "Relay Test");

            ArrayList<Map<String,Object>> systems = new ArrayList<Map<String, Object>>();
            HashMap<String,Object> hue = new HashMap<String, Object>();
            HashMap<String,Object> auth = new HashMap<String, Object>();

            String hueIp = System.getProperty("ip");
            String hueAccessKey = System.getProperty("accessKey");

            auth.put("ipAddress", hueIp);
            auth.put("accessKey", hueAccessKey);

            hue.put("cname", Hue.class.getName());
            hue.put("id", "1");
            hue.put("authenticationProperties", auth);
            hue.put("customProperties", new HashMap<String,Object>());
            systems.add(hue);
            cfg.put("systems", systems);

            cfg.put("services", new ArrayList<Map<String,Object>>());

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)));

            writer.write((new JSONObject(cfg)).toString());
            writer.newLine();
            writer.flush();
            writer.close();
        }
        if( name.getMethodName().equals("createLocation") && apiKeyId == null ) {
            setupUser();
        }
        if( name.getMethodName().equals("initializePairing") && locationId == null ) {
            setupLocation();
        }
        if( name.getMethodName().equals("pair") && pairingCode == null ) {
            setupPairing();
        }
        if( name.getMethodName().equals("token") && relayKeyId == null ) {
            setupRelay();
        }
        if( name.getMethodName().equals("pushState") && token == null ) {
            setupToken();
        }
        if( name.getMethodName().equals("listDevices") ) {
            setupDevices();
        }
    }

    private void setupUser() throws Exception {
        HashMap<String,Object> user = new HashMap<String, Object>();
        long key = (System.currentTimeMillis()%100000);

        user.put("firstName", "Test " + key);
        user.put("lastName", "User");
        user.put("email", "test" + key + "@example.com");
        user.put("password", "ABC" + random.nextInt(1000000));

        HttpClient client = getClient();

        HttpPost method = new HttpPost(cloudAPI + "/user");
        long timestamp = System.currentTimeMillis();

        method.addHeader("Content-Type", "application/json");
        method.addHeader("x-imaginary-version", VERSION);
        method.addHeader("x-imaginary-timestamp", String.valueOf(timestamp));

        //noinspection deprecation
        method.setEntity(new StringEntity((new JSONObject(user)).toString(), "application/json", "UTF-8"));

        HttpResponse response;
        StatusLine status;

        try {
            response = client.execute(method);
            status = response.getStatusLine();
        }
        catch( IOException e ) {
            e.printStackTrace();
            throw new CommunicationException(e);
        }
        if( status.getStatusCode() == HttpServletResponse.SC_CREATED ) {
            String json = EntityUtils.toString(response.getEntity());
            JSONObject u = new JSONObject(json);

            JSONObject keys = (u.has("apiKeys") && !u.isNull("apiKeys")) ? u.getJSONObject("apiKeys") : null;

            if( keys != null ) {
                CloudTest.apiKeyId = (keys.has("apiKeyId") && !keys.isNull("apiKeyId")) ? keys.getString("apiKeyId") : null;
                CloudTest.apiKeySecret = (keys.has("apiKeySecret") && !keys.isNull("apiKeySecret")) ? keys.getString("apiKeySecret") : null;
            }
        }
        else {
            Assert.fail("Failed to create user (" + status.getStatusCode() + ": " + EntityUtils.toString(response.getEntity()));
        }
    }

    private void setupLocation() throws Exception {
        if( apiKeyId == null ) {
            setupUser();
        }
        HashMap<String,Object> user = new HashMap<String, Object>();
        long key = (System.currentTimeMillis()%100000);

        user.put("name", "My Home " + key);
        user.put("description", "Integration test location");
        user.put("timeZone", TimeZone.getDefault().getID());

        HttpClient client = getClient();

        HttpPost method = new HttpPost(cloudAPI + "/location");
        long timestamp = System.currentTimeMillis();

        method.addHeader("Content-Type", "application/json");
        method.addHeader("x-imaginary-version", VERSION);
        method.addHeader("x-imaginary-timestamp", String.valueOf(timestamp));
        method.addHeader("x-imaginary-api-key", apiKeyId);
        method.addHeader("x-imaginary-signature", CloudService.sign(apiKeySecret.getBytes("utf-8"), "post:/location:" + apiKeyId + ":" + timestamp + ":" + VERSION));

        //noinspection deprecation
        method.setEntity(new StringEntity((new JSONObject(user)).toString(), "application/json", "UTF-8"));

        HttpResponse response;
        StatusLine status;

        try {
            response = client.execute(method);
            status = response.getStatusLine();
        }
        catch( IOException e ) {
            e.printStackTrace();
            throw new CommunicationException(e);
        }
        if( status.getStatusCode() == HttpServletResponse.SC_CREATED ) {
            String json = EntityUtils.toString(response.getEntity());
            JSONObject u = new JSONObject(json);

            locationId = (u.has("locationId") && !u.isNull("locationId")) ? u.getString("locationId") : null;
        }
        else {
            Assert.fail("Failed to create location (" + status.getStatusCode() + ": " + EntityUtils.toString(response.getEntity()));
        }
    }

    private void setupPairing() throws Exception {
        if( locationId == null ) {
            setupLocation();
        }
        HashMap<String,Object> action = new HashMap<String, Object>();

        action.put("action", "initializePairing");

        HttpClient client = getClient();

        HttpPut method = new HttpPut(cloudAPI + "/location/" + locationId);
        long timestamp = System.currentTimeMillis();

        method.addHeader("Content-Type", "application/json");
        method.addHeader("x-imaginary-version", VERSION);
        method.addHeader("x-imaginary-timestamp", String.valueOf(timestamp));
        method.addHeader("x-imaginary-api-key", apiKeyId);
        method.addHeader("x-imaginary-signature", CloudService.sign(apiKeySecret.getBytes("utf-8"), "put:/location/" + locationId + ":" + apiKeyId + ":" + timestamp + ":" + VERSION));

        //noinspection deprecation
        method.setEntity(new StringEntity((new JSONObject(action)).toString(), "application/json", "UTF-8"));

        HttpResponse response;
        StatusLine status;

        try {
            response = client.execute(method);
            status = response.getStatusLine();
        }
        catch( IOException e ) {
            e.printStackTrace();
            throw new CommunicationException(e);
        }
        if( status.getStatusCode() == HttpServletResponse.SC_OK ) {
            String json = EntityUtils.toString(response.getEntity());
            JSONObject u = new JSONObject(json);

            pairingCode = (u.has("pairingCode") && !u.isNull("pairingCode")) ? u.getString("pairingCode") : null;
        }
        else {
            Assert.fail("Failed to initialize pairing  (" + status.getStatusCode() + ": " + EntityUtils.toString(response.getEntity()));
        }
    }

    private void setupRelay() throws Exception {
        if( pairingCode == null ) {
            setupPairing();
        }
        HashMap<String,Object> map = new HashMap<String, Object>();
        long key = (System.currentTimeMillis()%100000);

        map.put("pairingCode", pairingCode);
        map.put("name", "Test Controller " + key);

        HttpClient client = getClient();

        HttpPost method = new HttpPost(cloudAPI + "/relay");
        long timestamp = System.currentTimeMillis();

        method.addHeader("Content-Type", "application/json");
        method.addHeader("x-imaginary-version", VERSION);
        method.addHeader("x-imaginary-timestamp", String.valueOf(timestamp));

        //noinspection deprecation
        method.setEntity(new StringEntity((new JSONObject(map)).toString(), "application/json", "UTF-8"));

        HttpResponse response;
        StatusLine status;

        try {
            response = client.execute(method);
            status = response.getStatusLine();
        }
        catch( IOException e ) {
            e.printStackTrace();
            throw new CommunicationException(e);
        }
        if( status.getStatusCode() == HttpServletResponse.SC_CREATED ) {
            String json = EntityUtils.toString(response.getEntity());
            JSONObject keys = new JSONObject(json);

            relayKeyId = (keys.has("apiKeyId") && !keys.isNull("apiKeyId")) ? keys.getString("apiKeyId") : null;
            relayKeySecret = (keys.has("apiKeySecret") && !keys.isNull("apiKeySecret")) ? keys.getString("apiKeySecret") : null;
            pairingCode = null;
        }
        else {
            Assert.fail("Failed to setup pairing for relay keys (" + status.getStatusCode() + ": " + EntityUtils.toString(response.getEntity()));
        }
    }

    private void setupToken() throws Exception {
        if( relayKeyId == null ) {
            setupRelay();
        }
        HttpClient client = getClient();

        HttpPost method = new HttpPost(cloudAPI + "/token");
        long timestamp = System.currentTimeMillis();

        method.addHeader("Content-Type", "application/json");
        method.addHeader("x-imaginary-version", VERSION);
        method.addHeader("x-imaginary-timestamp", String.valueOf(timestamp));
        method.addHeader("x-imaginary-api-key", relayKeyId);
        method.addHeader("x-imaginary-signature", CloudService.sign(relayKeyId.getBytes("utf-8"), "post:/token:" + relayKeySecret + ":" + timestamp + ":" + VERSION));

        HttpResponse response;
        StatusLine status;

        try {
            response = client.execute(method);
            status = response.getStatusLine();
        }
        catch( IOException e ) {
            e.printStackTrace();
            throw new CommunicationException(e);
        }
        if( status.getStatusCode() == HttpServletResponse.SC_CREATED ) {
            String json = EntityUtils.toString(response.getEntity());
            JSONObject t = new JSONObject(json);

            token = (t.has("token") && !t.isNull("token")) ? t.getString("token") : null;
        }
        else {
            Assert.fail("Failed to generate token (" + status.getStatusCode() + ": " + EntityUtils.toString(response.getEntity()));
        }
    }

    private void setupDevices() throws Exception {
        if( token == null ) {
            setupToken();
        }
        HashMap<String,Object> action = new HashMap<String, Object>();

        action.put("action", "update");

        HashMap<String,Object> relay = new HashMap<String, Object>();

        ArrayList<Map<String,Object>> devices = new ArrayList<Map<String, Object>>();

        {
            HashMap<String,Object> device = new HashMap<String, Object>();

            device.put("systemId", "2");
            device.put("id", "999");
            device.put("model", "XYZ900");
            device.put("on", false);
            device.put("deviceType", "powered");
            device.put("name", "Manipulation Test");
            device.put("description", "A test thing that turns off and on and will be manipulated by tests");
            devices.add(device);
        }
        relay.put("devices", devices);
        action.put("relay", relay);

        HttpClient client = getClient();

        HttpPut method = new HttpPut(cloudAPI + "/relay/" + relayKeyId);
        long timestamp = System.currentTimeMillis();

        method.addHeader("Content-Type", "application/json");
        method.addHeader("x-imaginary-version", VERSION);
        method.addHeader("x-imaginary-timestamp", String.valueOf(timestamp));
        method.addHeader("x-imaginary-api-key", relayKeyId);
        method.addHeader("x-imaginary-signature", CloudService.sign(relayKeySecret.getBytes("utf-8"), "put:/relay/" + relayKeyId + ":" + relayKeyId + ":" + token + ":" + timestamp + ":" + VERSION));

        //noinspection deprecation
        method.setEntity(new StringEntity((new JSONObject(action)).toString(), "application/json", "UTF-8"));

        HttpResponse response;
        StatusLine status;

        try {
            response = client.execute(method);
            status = response.getStatusLine();
        }
        catch( IOException e ) {
            e.printStackTrace();
            throw new CommunicationException(e);
        }
        if( status.getStatusCode() != HttpServletResponse.SC_NO_CONTENT ) {
            Assert.fail("Failed to update state for relay  (" + status.getStatusCode() + ": " + EntityUtils.toString(response.getEntity()));
        }
    }

    static private @Nonnull HttpClient getClient() {
        HttpParams params = new BasicHttpParams();

        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        //noinspection deprecation
        HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
        HttpProtocolParams.setUserAgent(params, "Imaginary Home");
        return new DefaultHttpClient(params);
    }

    @Test
    public void createUser() throws Exception {
        HashMap<String,Object> user = new HashMap<String, Object>();
        long key = (System.currentTimeMillis()%100000);

        user.put("firstName", "Test " + key);
        user.put("lastName", "User");
        user.put("email", "test" + key + "@example.com");
        user.put("password", "ABC" + random.nextInt(1000000));

        HttpClient client = getClient();

        HttpPost method = new HttpPost(cloudAPI + "/user");
        long timestamp = System.currentTimeMillis();

        method.addHeader("Content-Type", "application/json");
        method.addHeader("x-imaginary-version", VERSION);
        method.addHeader("x-imaginary-timestamp", String.valueOf(timestamp));

        //noinspection deprecation
        method.setEntity(new StringEntity((new JSONObject(user)).toString(), "application/json", "UTF-8"));

        HttpResponse response;
        StatusLine status;

        try {
            response = client.execute(method);
            status = response.getStatusLine();
        }
        catch( IOException e ) {
            e.printStackTrace();
            throw new CommunicationException(e);
        }
        if( status.getStatusCode() == HttpServletResponse.SC_CREATED ) {
            String json = EntityUtils.toString(response.getEntity());
            JSONObject u = new JSONObject(json);

            String userId = (u.has("userId") && !u.isNull("userId")) ? u.getString("userId") : null;
            String email = (u.has("email") && !u.isNull("email")) ? u.getString("email") : null;
            String firstName = (u.has("firstName") && !u.isNull("firstName")) ? u.getString("firstName") : null;
            String lastName = (u.has("lastName") && !u.isNull("lastName")) ? u.getString("lastName") : null;

            JSONObject keys = (u.has("apiKeys") && !u.isNull("apiKeys")) ? u.getJSONObject("apiKeys") : null;
            String apiKeyId = null, apiKeySecret = null;

            if( keys != null ) {
                apiKeyId = (keys.has("apiKeyId") && !keys.isNull("apiKeyId")) ? keys.getString("apiKeyId") : null;
                apiKeySecret = (keys.has("apiKeySecret") && !keys.isNull("apiKeySecret")) ? keys.getString("apiKeySecret") : null;
            }
            out("ID:             " + userId);
            out("Email:          " + email);
            out("First Name:     " + firstName);
            out("Last Name:      " + lastName);
            out("API Key ID:     " + apiKeyId);
            out("API Key Secret: " + apiKeySecret);

            Assert.assertNotNull("User ID may not be null", userId);
            Assert.assertNotNull("Email may not be null", email);
            Assert.assertNotNull("First name may not be null", firstName);
            Assert.assertNotNull("Last name may not be null", lastName);
            Assert.assertNotNull("API key ID may not be null", apiKeyId);
            Assert.assertNotNull("API key secret may not be null", apiKeySecret);
            if( CloudTest.apiKeyId == null ) {
                CloudTest.apiKeyId = apiKeyId;
                CloudTest.apiKeySecret = apiKeySecret;
            }
        }
        else {
            Assert.fail("Failed to create user (" + status.getStatusCode() + ": " + EntityUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void createLocation() throws Exception {
        HashMap<String,Object> user = new HashMap<String, Object>();
        long key = (System.currentTimeMillis()%100000);

        user.put("name", "My Home " + key);
        user.put("description", "Integration test location");
        user.put("timeZone", TimeZone.getDefault().getID());

        HttpClient client = getClient();

        HttpPost method = new HttpPost(cloudAPI + "/location");
        long timestamp = System.currentTimeMillis();

        method.addHeader("Content-Type", "application/json");
        method.addHeader("x-imaginary-version", VERSION);
        method.addHeader("x-imaginary-timestamp", String.valueOf(timestamp));
        method.addHeader("x-imaginary-api-key", apiKeyId);
        method.addHeader("x-imaginary-signature", CloudService.sign(apiKeySecret.getBytes("utf-8"), "post:/location:" + apiKeyId + ":" + timestamp + ":" + VERSION));

        //noinspection deprecation
        method.setEntity(new StringEntity((new JSONObject(user)).toString(), "application/json", "UTF-8"));

        HttpResponse response;
        StatusLine status;

        try {
            response = client.execute(method);
            status = response.getStatusLine();
        }
        catch( IOException e ) {
            e.printStackTrace();
            throw new CommunicationException(e);
        }
        if( status.getStatusCode() == HttpServletResponse.SC_CREATED ) {
            String json = EntityUtils.toString(response.getEntity());
            JSONObject u = new JSONObject(json);

            String locationId = (u.has("locationId") && !u.isNull("locationId")) ? u.getString("locationId") : null;

            out("ID:             " + locationId);

            Assert.assertNotNull("Location ID may not be null", locationId);
            if( CloudTest.locationId == null ) {
                CloudTest.locationId = locationId;
            }
        }
        else {
            Assert.fail("Failed to create location (" + status.getStatusCode() + ": " + EntityUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void initializePairing() throws Exception {
        HashMap<String,Object> action = new HashMap<String, Object>();

        action.put("action", "initializePairing");

        HttpClient client = getClient();

        HttpPut method = new HttpPut(cloudAPI + "/location/" + locationId);
        long timestamp = System.currentTimeMillis();

        method.addHeader("Content-Type", "application/json");
        method.addHeader("x-imaginary-version", VERSION);
        method.addHeader("x-imaginary-timestamp", String.valueOf(timestamp));
        method.addHeader("x-imaginary-api-key", apiKeyId);
        method.addHeader("x-imaginary-signature", CloudService.sign(apiKeySecret.getBytes("utf-8"), "put:/location/" + locationId + ":" + apiKeyId + ":" + timestamp + ":" + VERSION));

        //noinspection deprecation
        method.setEntity(new StringEntity((new JSONObject(action)).toString(), "application/json", "UTF-8"));

        HttpResponse response;
        StatusLine status;

        try {
            response = client.execute(method);
            status = response.getStatusLine();
        }
        catch( IOException e ) {
            e.printStackTrace();
            throw new CommunicationException(e);
        }
        if( status.getStatusCode() == HttpServletResponse.SC_OK ) {
            String json = EntityUtils.toString(response.getEntity());
            JSONObject u = new JSONObject(json);

            String pairingCode = (u.has("pairingCode") && !u.isNull("pairingCode")) ? u.getString("pairingCode") : null;

            out("Pairing code: " + pairingCode);

            Assert.assertNotNull("Pairing code may not be null", pairingCode);
            if( CloudTest.pairingCode == null ) {
                CloudTest.pairingCode = pairingCode;
            }
        }
        else {
            Assert.fail("Failed to initialize pairing  (" + status.getStatusCode() + ": " + EntityUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void pair() throws Exception {
        HashMap<String,Object> map = new HashMap<String, Object>();
        long key = (System.currentTimeMillis()%100000);

        map.put("pairingCode", pairingCode);
        map.put("name", "Test Controller " + key);

        HttpClient client = getClient();

        HttpPost method = new HttpPost(cloudAPI + "/relay");
        long timestamp = System.currentTimeMillis();

        method.addHeader("Content-Type", "application/json");
        method.addHeader("x-imaginary-version", VERSION);
        method.addHeader("x-imaginary-timestamp", String.valueOf(timestamp));

        //noinspection deprecation
        method.setEntity(new StringEntity((new JSONObject(map)).toString(), "application/json", "UTF-8"));

        HttpResponse response;
        StatusLine status;

        try {
            response = client.execute(method);
            status = response.getStatusLine();
        }
        catch( IOException e ) {
            e.printStackTrace();
            throw new CommunicationException(e);
        }
        if( status.getStatusCode() == HttpServletResponse.SC_CREATED ) {
            String json = EntityUtils.toString(response.getEntity());
            JSONObject keys = new JSONObject(json);


            String relayKeyId = (keys.has("apiKeyId") && !keys.isNull("apiKeyId")) ? keys.getString("apiKeyId") : null;
            String relayKeySecret = (keys.has("apiKeySecret") && !keys.isNull("apiKeySecret")) ? keys.getString("apiKeySecret") : null;

            out("Key ID:         " + relayKeyId);
            out("Key secret:     " + relayKeySecret);

            Assert.assertNotNull("Relay key ID may not be null", relayKeyId);
            Assert.assertNotNull("Relay key secret may not be null", relayKeySecret);
            if( CloudTest.relayKeyId == null ) {
                CloudTest.relayKeyId = relayKeyId;
                CloudTest.relayKeySecret = relayKeySecret;
            }
            CloudTest.pairingCode = null;
        }
        else {
            Assert.fail("Failed to finish pairing (" + status.getStatusCode() + ": " + EntityUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void token() throws Exception {
        HttpClient client = getClient();

        HttpPost method = new HttpPost(cloudAPI + "/token");
        long timestamp = System.currentTimeMillis();

        method.addHeader("Content-Type", "application/json");
        method.addHeader("x-imaginary-version", VERSION);
        method.addHeader("x-imaginary-timestamp", String.valueOf(timestamp));
        method.addHeader("x-imaginary-api-key", relayKeyId);
        method.addHeader("x-imaginary-signature", CloudService.sign(relayKeySecret.getBytes("utf-8"), "post:/token:" + relayKeyId + ":" + timestamp + ":" + VERSION));

        HttpResponse response;
        StatusLine status;

        try {
            response = client.execute(method);
            status = response.getStatusLine();
        }
        catch( IOException e ) {
            e.printStackTrace();
            throw new CommunicationException(e);
        }
        if( status.getStatusCode() == HttpServletResponse.SC_CREATED ) {
            String json = EntityUtils.toString(response.getEntity());
            JSONObject t = new JSONObject(json);

            String token = (t.has("token") && !t.isNull("token")) ? t.getString("token") : null;

            out("Token:         " + token);
            Assert.assertNotNull("Token may not be null", token);
            CloudTest.token = token;
        }
        else {
            Assert.fail("Failed to generate token (" + status.getStatusCode() + ": " + EntityUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void pushState() throws Exception {
        HashMap<String,Object> action = new HashMap<String, Object>();

        action.put("action", "update");

        HashMap<String,Object> relay = new HashMap<String, Object>();

        ArrayList<Map<String,Object>> devices = new ArrayList<Map<String, Object>>();

        {
            HashMap<String,Object> device = new HashMap<String, Object>();

            device.put("systemId", "1");
            device.put("id", "1");
            device.put("model", "A1234");
            device.put("on", true);
            device.put("deviceType", "light");
            device.put("name", "Test Light Bulb");
            device.put("description", "A test light bulb for integration tests");
            device.put("supportsColorChanges", true);
            device.put("supportsBrightnessChanges", true);
            device.put("colorModes", new ColorMode[] { ColorMode.RGB });

            HashMap<String,Object> color = new HashMap<String, Object>();

            color.put("colorMode", ColorMode.RGB.name());
            color.put("components", new float[] { 100f, 0f, 0f });
            device.put("color", color);
            devices.add(device);

            device = new HashMap<String, Object>();
            device.put("systemId", "2");
            device.put("id", "1");
            device.put("model", "XYZ999");
            device.put("on", false);
            device.put("deviceType", "powered");
            device.put("name", "Test Thing");
            device.put("description", "A test thing that turns off and on");
            devices.add(device);

        }
        relay.put("devices", devices);
        action.put("relay", relay);

        HttpClient client = getClient();

        HttpPut method = new HttpPut(cloudAPI + "/relay/" + relayKeyId);
        long timestamp = System.currentTimeMillis();

        method.addHeader("Content-Type", "application/json");
        method.addHeader("x-imaginary-version", VERSION);
        method.addHeader("x-imaginary-timestamp", String.valueOf(timestamp));
        method.addHeader("x-imaginary-api-key", relayKeyId);
        method.addHeader("x-imaginary-signature", CloudService.sign(relayKeySecret.getBytes("utf-8"), "put:/relay/" + relayKeyId + ":" + relayKeyId + ":" + token + ":" + timestamp + ":" + VERSION));

        //noinspection deprecation
        method.setEntity(new StringEntity((new JSONObject(action)).toString(), "application/json", "UTF-8"));

        HttpResponse response;
        StatusLine status;

        try {
            response = client.execute(method);
            status = response.getStatusLine();
        }
        catch( IOException e ) {
            e.printStackTrace();
            throw new CommunicationException(e);
        }
        if( status.getStatusCode() == HttpServletResponse.SC_NO_CONTENT ) {
            Header h = response.getFirstHeader("x-imaginary-has-commands");
            boolean commands = false;

            if( h != null ) {
                String val = h.getValue();

                commands = val != null && val.equalsIgnoreCase("true");
            }
            out("Commands waiting: " + commands);
        }
        else {
            Assert.fail("Failed to update state for relay  (" + status.getStatusCode() + ": " + EntityUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void listDevices() throws Exception {
        HttpClient client = getClient();

        HttpGet method = new HttpGet(cloudAPI + "/device?locationId=" + URLEncoder.encode(locationId, "utf-8"));

        long timestamp = System.currentTimeMillis();

        method.addHeader("Content-Type", "application/json");
        method.addHeader("x-imaginary-version", VERSION);
        method.addHeader("x-imaginary-timestamp", String.valueOf(timestamp));
        method.addHeader("x-imaginary-api-key", apiKeyId);
        method.addHeader("x-imaginary-signature", CloudService.sign(apiKeySecret.getBytes("utf-8"), "get:/device:" + apiKeyId + ":" + timestamp + ":" + VERSION));

        HttpResponse response;
        StatusLine status;

        try {
            response = client.execute(method);
            status = response.getStatusLine();
        }
        catch( IOException e ) {
            e.printStackTrace();
            throw new CommunicationException(e);
        }
        if( status.getStatusCode() == HttpServletResponse.SC_OK ) {
            String json = EntityUtils.toString(response.getEntity());
            JSONArray list = new JSONArray(json);
            String match = null;

            for( int i=0; i<list.length(); i++ ) {
                JSONObject device = list.getJSONObject(i);

                out("device -> " + device.toString());
                if( device.has("systemId") && device.getString("systemId").equals("2") ) {
                    if( device.has("vendorDeviceId") && device.getString("vendorDeviceId").equals("999") ) {
                        match = device.getString("deviceId");
                    }
                }
            }
            out("MATCH: " + match);
            Assert.assertNotNull("Unable to find the test device among the returned devices", match);
        }
        else {
            Assert.fail("Failed to load devices (" + status.getStatusCode() + ": " + EntityUtils.toString(response.getEntity()));
        }
    }

    private void out(String msg) {
        if( logger.isDebugEnabled() ) {
            logger.debug(name.getMethodName() + "> " + msg);
        }
    }
}
