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

package com.imaginary.home.device.hue;

import com.imaginary.home.controller.CloudService;
import com.imaginary.home.controller.CommunicationException;
import com.imaginary.home.controller.HomeController;
import junit.framework.Assert;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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

    private String cloudAPI;

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
            System.out.println("JSON=" + json);
            JSONObject u = new JSONObject(json);

            String locationId = (u.has("locationId") && !u.isNull("locationId")) ? u.getString("locationId") : null;

            out("ID:             " + locationId);

            Assert.assertNotNull("Location ID may not be null", locationId);
        }
        else {
            Assert.fail("Failed to create location (" + status.getStatusCode() + ": " + EntityUtils.toString(response.getEntity()));
        }
    }

    private void out(String msg) {
        if( logger.isDebugEnabled() ) {
            logger.debug("> " + msg);
        }
    }
}
