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

import com.imaginary.home.controller.CommunicationException;
import com.imaginary.home.controller.ControllerException;
import com.imaginary.home.controller.HomeController;
import com.imaginary.home.controller.ManagedResource;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 1/26/13 5:49 PM</p>
 *
 * @author George Reese
 */
public class RelayTest {
    static private final Logger logger = Logger.getLogger("com.imaginary.home.controller.test");

    @Before
    public void setUp() throws IOException {
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

    @Test
    public void stateToJSON() throws CommunicationException, ControllerException {
        ArrayList<Map<String,Object>> devices = new ArrayList<Map<String, Object>>();
        HashMap<String,Object> state = new HashMap<String,Object>();

        state.put("action", "update");
        for( ManagedResource resource : HomeController.getInstance().listResources() ) {
            HashMap<String,Object> json = new HashMap<String, Object>();

            resource.toMap(json);
            devices.add(json);
        }
        state.put("devices", devices);
        JSONObject json = new JSONObject(state);

        out("JSON:\n" + json.toString());
    }

    private void out(String msg) {
        if( logger.isDebugEnabled() ) {
            logger.debug("> " + msg);
        }
    }

}
