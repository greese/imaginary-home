/**
 * Copyright (C) 2013 George Reese
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.imaginary.home.controller;

import com.imaginary.home.lighting.Light;
import com.imaginary.home.lighting.LightingService;
import org.dasein.util.CalendarWrapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class HomeController {
    static public final ExecutorService executorService = Executors.newCachedThreadPool();

    static public final String COMMAND_FILE   = "/etc/imaginary/iha/command.cfg";
    static public final String CONFIG_FILE    = "/etc/imaginary/iha/iha.cfg";
    static public final String SCHEDULER_FILE = "/etc/imaginary/iha/schedule.cfg";

    static private HomeController homeController;

    static public @Nonnull String formatDate(long timestamp) {
        return formatDate(new Date(timestamp));
    }

    static public @Nonnull String formatDate(@Nonnull Date when) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        cal.setTime(when);
        return fmt.format(cal.getTime());
    }

    static public @Nonnull HomeController getInstance() throws ControllerException {
        if( homeController == null ) {
            try {
                homeController = new HomeController();
            }
            catch( Exception e ) {
                throw new ControllerException("Unable to load system: " + e.getMessage());
            }
        }
        return homeController;
    }

    static public @Nonnull String now() {
        return formatDate(new Date());
    }

    static public long parseDate(@Nonnull String timestring) throws ParseException {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        return (fmt.parse(timestring)).getTime();
    }

    private Map<String,HomeAutomationSystem>       automationSystems;
    private List<CloudService>                     cloudServices;
    private final LinkedList<CommandList>          commandQueue      = new LinkedList<CommandList>();
    private long                                   lastLoad          = 0L;
    private String                                 name;
    private boolean                                running           = false;
    private TreeSet<ScheduledCommandList>          scheduler;

    private HomeController() throws JSONException, ClassNotFoundException, IllegalAccessException, InstantiationException, IOException {
        automationSystems = new HashMap<String,HomeAutomationSystem>();
        scheduler = new TreeSet<ScheduledCommandList>();
        cloudServices = new ArrayList<CloudService>();
        loadConfiguration();
        loadCommands();
        loadSchedule();
        executorService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                processCommands();
                return true;
            }
        });
        executorService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                checkScheduler();
                return true;
            }
        });
        executorService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                while( true ) {
                    synchronized( commandQueue ) {
                        if( !running ) {
                            return true;
                        }
                        try { commandQueue.wait(CalendarWrapper.MINUTE); }
                        catch( InterruptedException ignore ) { }
                        File f = new File(CONFIG_FILE);

                        if( f.lastModified() > lastLoad ) {
                            try {
                                loadConfiguration();
                            }
                            catch( Throwable t ) {
                                t.printStackTrace();
                            }
                        }
                    }
                }
            }
        });
        for( final CloudService service : cloudServices ) {
            executorService.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    poll(service);
                    return true;
                }
            });
        }
    }

    public void cancelScheduledCommands(@Nonnull String ... havingCommandIds) throws ControllerException {
        synchronized( commandQueue ) {
            if( !running ) {
                return;
            }
            for( ScheduledCommandList list : scheduler ) {
                boolean matches = false;

                for( JSONObject cmd : list ) {
                    for( String id : havingCommandIds ) {
                        try {
                            if( cmd.getString("id").equals(id) ) {
                                matches = true;
                                break;
                            }
                        }
                        catch( JSONException e ) {
                            throw new ControllerException(e);
                        }
                    }
                    if( matches ) {
                        break;
                    }
                }
                if( matches ) {
                    scheduler.remove(list);
                    saveSchedule();
                    return;
                }
            }
        }
    }

    private void checkScheduler() {
        while( true ) {
            synchronized( commandQueue ) {
                if( !running ) {
                    return;
                }
            }
            ScheduledCommandList cmdList;

            do {
                synchronized( commandQueue ) {
                    if( !scheduler.isEmpty() ) {
                        cmdList = scheduler.iterator().next();
                        scheduler.remove(cmdList);
                        try {
                            saveSchedule();
                        }
                        catch( Throwable t ) {
                            t.printStackTrace();
                        }
                    }
                    else {
                        cmdList = null;
                    }
                }
                if( cmdList != null && cmdList.getExecuteAfter() <= System.currentTimeMillis() ) {
                    ArrayList<Future<Boolean>> results = new ArrayList<Future<Boolean>>();
                    boolean[] completions = new boolean[results.size()];
                    CloudService service = getService(cmdList.getServiceId());

                    if( service == null ) {
                        continue;
                    }
                    for( JSONObject cmd : cmdList ) {
                        try {
                            Command c = new Command(cmd);

                            results.add(c.start());
                        }
                        catch( JSONException e ) {
                            results.add(null);
                        }
                    }
                    boolean done;

                    do {
                        done = true;
                        for( int i=0; i<completions.length; i++ ) {
                            if( !completions[i] ) {
                                Future<Boolean> f = results.get(i);
                                JSONObject cmd = cmdList.get(i);

                                if( f == null ) {
                                    try {
                                        service.postResult(cmd.getString("id"), false, null, new JSONException("Invalid JSON in command"));
                                    }
                                    catch( Throwable t ) {
                                        t.printStackTrace();
                                    }
                                    finally {
                                        completions[i] = true;
                                    }
                                }
                                if( f.isDone() ) {
                                    Throwable failure = null;
                                    boolean result = false;

                                    try {
                                        result = f.get();
                                    }
                                    catch( Throwable t ) {
                                        failure = t;
                                    }
                                    try {
                                        service.postResult(cmd.getString("id"), result, null, failure);
                                    }
                                    catch( Throwable t ) {
                                        t.printStackTrace();
                                    }
                                    finally {
                                        completions[i] = true;
                                    }
                                }
                                else {
                                    done = false;
                                }
                            }
                        }
                    } while( !done );
                }
            } while( cmdList != null );
            synchronized( commandQueue ) {
                try { commandQueue.wait(60000L); }
                catch( InterruptedException ignore ) { }
            }
        }
    }

    public @Nonnull String getName() {
        return name;
    }

    public @Nullable CloudService getService(@Nonnull String id) {
        for( CloudService svc : cloudServices ) {
            if( svc.getServiceId().equals(id) ) {
                return svc;
            }
        }
        return null;
    }

    public @Nullable HomeAutomationSystem getSystem(@Nonnull String id) {
        return automationSystems.get(id);
    }

    private void loadCommands() throws IOException, JSONException {
        synchronized( commandQueue ) {
            BufferedReader reader;

            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(COMMAND_FILE)));
            }
            catch( IOException e ) {
                e.printStackTrace();
                return;
            }
            StringBuilder json = new StringBuilder();
            String line;

            while( (line = reader.readLine()) != null ) {
                json.append(line);
                json.append(" ");
            }
            JSONObject cfg = new JSONObject(json.toString());
            if( cfg.has("commands") ) {
                JSONArray list = cfg.getJSONArray("commands");

                for( int i=0; i<list.length(); i++ ) {
                    JSONObject cmd = list.getJSONObject(i);
                    JSONObject[] commands;
                    String serviceId;

                    if( cmd.has("serviceId") ) {
                        serviceId = cmd.getString("serviceId");
                    }
                    else {
                        continue;
                    }
                    if( cmd.has("commands") ) {
                        JSONArray cmds = cmd.getJSONArray("commands");

                        commands = new JSONObject[cmds.length()];
                        for( int j=0; j<cmds.length(); j++ ) {
                            commands[j] = cmds.getJSONObject(j);
                        }
                    }
                    else {
                        continue;
                    }
                    commandQueue.push(new CommandList(serviceId, commands));
                }
            }
            try {
                saveCommands(new ArrayList<CommandList>());
            }
            catch( Throwable t ) {
                if( commandQueue.isEmpty() ) {
                    return;
                }
                throw new IOException("Failed to save empty commands: " + t.getMessage());
            }
        }
    }

    private void loadConfiguration() throws JSONException, ClassNotFoundException, IllegalAccessException, InstantiationException, IOException {
        synchronized( commandQueue ) {
            BufferedReader reader;

            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(CONFIG_FILE)));
            }
            catch( IOException e ) {
                e.printStackTrace();
                return;
            }
            HashMap<String,HomeAutomationSystem> automationSystems = new HashMap<String, HomeAutomationSystem>();
            ArrayList<CloudService> services = new ArrayList<CloudService>();
            StringBuilder json = new StringBuilder();
            String line;

            while( (line = reader.readLine()) != null ) {
                json.append(line);
                json.append(" ");
            }
            JSONObject cfg = new JSONObject(json.toString());

            if( cfg.has("name") && !cfg.isNull("name") ) {
                name = cfg.getString("name");
            }
            else {
                name = "Imaginary Home Controller Relay";
            }
            if( cfg.has("systems") ) {
                JSONArray list = cfg.getJSONArray("systems");

                for( int i=0; i<list.length(); i++ ) {
                    JSONObject sys = list.getJSONObject(i);

                    String cname = sys.getString("cname");
                    String id = sys.getString("id");

                    HomeAutomationSystem system = (HomeAutomationSystem)Class.forName(cname).newInstance();
                    Properties auth = new Properties();
                    Properties custom = new Properties();

                    JSONObject p;

                    if( sys.has("authenticationProperties") ) {
                        p = sys.getJSONObject("authenticationProperties");
                        for( String key : JSONObject.getNames(p) ) {
                            auth.put(key, p.getString(key));
                        }
                    }
                    if( sys.has("customProperties") ) {
                        p = sys.getJSONObject("customProperties");
                        for( String key : JSONObject.getNames(p) ) {
                            auth.put(key, p.getString(key));
                        }
                    }
                    system.init(id, auth, custom);
                    automationSystems.put(id, system);
                }
            }
            if( cfg.has("services") ) {
                JSONArray list = cfg.getJSONArray("services");

                for( int i=0; i<list.length(); i++ ) {
                    JSONObject svc = list.getJSONObject(i);
                    String name, endpoint, id, secret, proxyHost = null;
                    int proxyPort = 0;

                    if( svc.has("endpoint") ) {
                        endpoint = svc.getString("endpoint");
                    }
                    else {
                        continue;
                    }
                    if( svc.has("id") ) {
                        id = svc.getString("id");
                    }
                    else {
                        continue;
                    }
                    if( svc.has("apiKeySecret") ) {
                        secret = svc.getString("apiKeySecret");
                    }
                    else {
                        continue;
                    }
                    if( svc.has("name") ) {
                        name = svc.getString("name");
                    }
                    else {
                        name = endpoint;
                    }
                    if( svc.has("proxyHost") && !svc.isNull("proxyHost") ) {
                        proxyHost = svc.getString("proxyHost");
                    }
                    if( svc.has("proxyPort") && !svc.isNull("proxyPort") ) {
                        proxyPort = svc.getInt("proxyPort");
                    }
                    services.add(new CloudService(id, secret, name, endpoint, proxyHost, proxyPort));
                }
            }
            this.cloudServices = services;
            this.automationSystems = automationSystems;
            lastLoad = System.currentTimeMillis();
        }
    }

    private void loadSchedule() throws IOException, JSONException {
        synchronized( commandQueue ) {
            BufferedReader reader;

            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(SCHEDULER_FILE)));
            }
            catch( IOException e ) {
                // probably does not exist, which is OK
                return;
            }
            StringBuilder json = new StringBuilder();
            String line;

            while( (line = reader.readLine()) != null ) {
                json.append(line);
                json.append(" ");
            }
            JSONObject cfg = new JSONObject(json.toString());
            if( cfg.has("schedule") ) {
                JSONArray list = cfg.getJSONArray("schedule");

                for( int i=0; i<list.length(); i++ ) {
                    JSONObject scheduledList = list.getJSONObject(i);
                    JSONObject[] commands = null;
                    long executeAfter = 0L;
                    String serviceId;
                    String scheduleId;

                    if( scheduledList.has("serviceId") ) {
                        serviceId = scheduledList.getString("serviceId");
                    }
                    else {
                        continue;
                    }
                    if( scheduledList.has("scheduleId") ) {
                        scheduleId = scheduledList.getString("scheduleId");
                    }
                    else {
                        continue;
                    }
                    if( scheduledList.has("executeAfter") ) {
                        try {
                            executeAfter = parseDate(scheduledList.getString("executeAfter") );
                        }
                        catch( ParseException e ) {
                            // this should not happen
                            e.printStackTrace();
                            continue;
                        }
                    }
                    if( scheduledList.has("commands") ) {
                        JSONArray cmds = scheduledList.getJSONArray("commands");
                        if( cmds.length() < 1 ) {
                            continue;
                        }
                        commands = new JSONObject[cmds.length()];

                        for( int j=0; j<cmds.length(); j++ ) {
                            commands[j] = cmds.getJSONObject(j);
                        }
                    }
                    if( commands == null || executeAfter < System.currentTimeMillis() ) {
                        continue;
                    }
                    scheduler.add(new ScheduledCommandList(serviceId, scheduleId, executeAfter, commands));
                }
            }
        }
    }

    public @Nonnull Iterable<ManagedResource> listResources() throws CommunicationException {
        ArrayList<ManagedResource> resources = new ArrayList<ManagedResource>();

        for( HomeAutomationSystem system : listSystems() ) {
            if( system instanceof LightingService ) {
                for( Light light : ((LightingService)system).listLights() ) {
                    resources.add(light);
                }
            }
        }
        return resources;
    }

    public @Nonnull Collection<HomeAutomationSystem> listSystems() {
        return automationSystems.values();
    }

    // TODO: identify a mechanism for initiating this pairing call
    public @Nonnull String pairService(@Nonnull String name, @Nonnull String endpoint, @Nullable String proxyHost, int proxyPort, @Nonnull String pairingToken) throws ControllerException, CommunicationException {
        CloudService service = CloudService.pair(name, endpoint, proxyHost, proxyPort, pairingToken);

        cloudServices.add(service);
        saveConfiguration();
        return service.getServiceId();
    }

    public @Nonnull String pairSystem(@Nonnull String cname, @Nonnull Properties authProperties, @Nonnull Properties customProperties) throws ControllerException, CommunicationException {
        try {
            HomeAutomationSystem system = (HomeAutomationSystem)Class.forName(cname).newInstance();
            String id = UUID.randomUUID().toString();

            system.init(id, authProperties, customProperties);
            system.pair("IHA");
            automationSystems.put(system.getId(), system);
            saveConfiguration();
            return id;
        }
        catch( ClassNotFoundException e ) {
            e.printStackTrace();
            throw new CommunicationException("No such automation system: " + cname);
        }
        catch( InstantiationException e ) {
            e.printStackTrace();
            throw new CommunicationException("Invalid automation system: " + cname);
        }
        catch( IllegalAccessException e ) {
            e.printStackTrace();
            throw new CommunicationException("Invalid automation system: " + cname);
        }
    }

    private void poll(@Nonnull CloudService service) {
        long pollWait = CalendarWrapper.MINUTE;
        long nextState = 0L;

        while( true ) {
            synchronized( commandQueue ) {
                try { commandQueue.wait(pollWait); }
                catch( InterruptedException ignore ) { }
                try {
                    if( !running ) {
                        return;
                    }
                    if( System.currentTimeMillis() > nextState ) {
                        boolean hasCommands = service.postState();

                        nextState = System.currentTimeMillis() + CalendarWrapper.MINUTE;
                        if( hasCommands ) {
                            service.fetchCommands();
                            pollWait = (10L * CalendarWrapper.SECOND);
                        }
                        else {
                            pollWait = CalendarWrapper.MINUTE;
                        }
                    }
                    else {
                        if( service.hasCommands() ) {
                            service.fetchCommands();
                            pollWait = (10L * CalendarWrapper.SECOND);
                        }
                        else {
                            pollWait = pollWait*2;
                            if( pollWait > CalendarWrapper.MINUTE ) {
                                pollWait = CalendarWrapper.MINUTE;
                            }
                        }
                    }
                }
                catch( Throwable t ) {
                    t.printStackTrace();
                }
            }
        }
    }

    private void processCommands() {
        synchronized( commandQueue ) {
            running = true;
        }
        while( true ) {
            synchronized( commandQueue ) {
                if( !running ) {
                    return;
                }
            }
            CommandList cmdList;

            do {
                synchronized( commandQueue ) {
                    if( !commandQueue.isEmpty() ) {
                        cmdList = commandQueue.poll();
                    }
                    else {
                        cmdList = null;
                    }
                }
                if( cmdList != null ) {
                    CloudService service = getService(cmdList.getServiceId());

                    if( service == null ) {
                        continue;
                    }
                    ArrayList<Future<Boolean>> results = new ArrayList<Future<Boolean>>();
                    boolean[] completions = new boolean[results.size()];

                    for( JSONObject cmd : cmdList ) {
                        try {
                            Command c = new Command(cmd);

                            results.add(c.start());
                        }
                        catch( JSONException e ) {
                            results.add(null);
                        }
                    }
                    boolean done;

                    do {
                        done = true;
                        for( int i=0; i<completions.length; i++ ) {
                            if( !completions[i] ) {
                                Future<Boolean> f = results.get(i);
                                JSONObject cmd = cmdList.get(i);

                                if( f == null ) {
                                    try {
                                        service.postResult(cmd.getString("id"), false, null, new JSONException("Invalid JSON in command"));
                                    }
                                    catch( Throwable t ) {
                                        t.printStackTrace();
                                    }
                                    finally {
                                        completions[i] = true;
                                    }
                                }
                                else if( f.isDone() ) {
                                    Throwable failure = null;
                                    boolean result = false;

                                    try {
                                        result = f.get();
                                    }
                                    catch( Throwable t ) {
                                        failure = t;
                                    }
                                    try {
                                        service.postResult(cmd.getString("id"), result, null, failure);
                                    }
                                    catch( Throwable t ) {
                                        t.printStackTrace();
                                    }
                                    finally {
                                        completions[i] = true;
                                    }
                                }
                                else {
                                    done = false;
                                }
                            }
                        }
                    } while( !done );
                }
            } while( cmdList != null );
            synchronized( commandQueue ) {
                try { commandQueue.wait(30000L); }
                catch( InterruptedException ignore ) { }
            }
        }
    }

    public void queueCommands(@Nonnull CloudService service, @Nonnull JSONObject ... commands) throws ControllerException {
        synchronized( commandQueue ) {
            if( !running ) {
                throw new ControllerException("Not currently accepting new commands");
            }
            commandQueue.push(new CommandList(service.getServiceId(), commands));
            commandQueue.notifyAll();
        }
    }

    private void saveCommands(List<CommandList> toSave) throws ControllerException {
        synchronized( commandQueue ) {
            ArrayList<Map<String,Object>> all = new ArrayList<Map<String, Object>>();
            HashMap<String,Object> cfg = new HashMap<String, Object>();

            for( CommandList cmdList : toSave ) {
                ArrayList<Map<String,Object>> commands = new ArrayList<Map<String, Object>>();
                HashMap<String,Object> map = new HashMap<String, Object>();

                for( JSONObject cmd : cmdList ) {
                    try {
                        commands.add(toMap(cmd));
                    }
                    catch( JSONException e ) {
                        throw new ControllerException(e);
                    }
                }
                map.put("commands", commands);
                map.put("serviceId", cmdList.getServiceId());
                all.add(map);
            }
            cfg.put("commands", all);
            try {
                File f = new File(COMMAND_FILE);
                File backup = null;

                if( f.exists() ) {
                    backup = new File(COMMAND_FILE + "." + System.currentTimeMillis());
                    if( !f.renameTo(backup) ) {
                        throw new ControllerException("Unable to make backup of configuration file");
                    }
                    f = new File(COMMAND_FILE);
                }
                boolean success = false;
                try {
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)));

                    writer.write((new JSONObject(cfg)).toString());
                    writer.newLine();
                    writer.flush();
                    writer.close();
                    success = true;
                }
                finally {
                    if( !success && backup != null ) {
                        //noinspection ResultOfMethodCallIgnored
                        backup.renameTo(f);
                    }
                }
            }
            catch( IOException e ) {
                throw new ControllerException("Unable to save command file: " + e.getMessage());
            }
        }
    }

    private void saveConfiguration() throws ControllerException {
        synchronized( commandQueue ) {
            try {
                loadConfiguration();
            }
            catch( Exception e ) {
                throw new ControllerException("Failed to load current system state: " + e.getMessage());
            }
            ArrayList<Map<String,Object>> all = new ArrayList<Map<String, Object>>();
            HashMap<String,Object> cfg = new HashMap<String, Object>();

            cfg.put("name", name);
            for( HomeAutomationSystem sys : listSystems() ) {
                HashMap<String,Object> json = new HashMap<String, Object>();

                json.put("cname", sys.getClass().getName());
                json.put("id", sys.getId());
                json.put("authenticationProperties", sys.getAuthenticationProperties());
                json.put("customProperties", sys.getCustomProperties());
                all.add(json);
            }
            cfg.put("systems", all);
            all = new ArrayList<Map<String, Object>>();
            for( CloudService service : cloudServices ) {
                HashMap<String,Object> json = new HashMap<String, Object>();

                json.put("id", service.getServiceId());
                json.put("name", service.getName());
                json.put("endpoint", service.getEndpoint());
                json.put("apiKeySecret", service.getApiKeySecret());
                if( service.getProxyHost() != null ) {
                    json.put("proxyHost", service.getProxyHost());
                    json.put("proxyPort", service.getProxyPort());
                }
                all.add(json);
            }
            cfg.put("services", all);
            try {
                File f = new File(CONFIG_FILE);
                File backup = null;

                if( f.exists() ) {
                    backup = new File(CONFIG_FILE + "." + System.currentTimeMillis());
                    if( !f.renameTo(backup) ) {
                        throw new ControllerException("Unable to make backup of configuration file");
                    }
                    f = new File(CONFIG_FILE);
                }
                boolean success = false;
                try {
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)));

                    writer.write((new JSONObject(cfg)).toString());
                    writer.newLine();
                    writer.flush();
                    writer.close();
                    success = true;
                }
                finally {
                    if( !success && backup != null ) {
                        //noinspection ResultOfMethodCallIgnored
                        backup.renameTo(f);
                    }
                }
            }
            catch( IOException e ) {
                throw new ControllerException("Unable to save configuration: " + e.getMessage());
            }
        }
    }

    private void saveSchedule() throws ControllerException {
        synchronized( commandQueue ) {
            ArrayList<Map<String,Object>> all = new ArrayList<Map<String,Object>>();
            HashMap<String,Object> cfg = new HashMap<String, Object>();

            for( ScheduledCommandList sList : scheduler ) {
                ArrayList<Map<String,Object>> commands = new ArrayList<Map<String, Object>>();
                HashMap<String,Object> schedule = new HashMap<String, Object>();

                for( JSONObject cmd : sList ) {
                    try {
                        commands.add(toMap(cmd));
                    }
                    catch( JSONException e ) {
                        throw new ControllerException(e);
                    }
                }
                schedule.put("commands", commands);
                schedule.put("executeAfter", sList.getExecuteAfter());
                schedule.put("scheduleId", sList.getScheduleId());
                schedule.put("serviceId", sList.getServiceId());
                all.add(schedule);
            }
            cfg.put("schedule", all);
            try {
                File f = new File(SCHEDULER_FILE);
                File backup = null;

                if( f.exists() ) {
                    backup = new File(SCHEDULER_FILE + "." + System.currentTimeMillis());
                    if( !f.renameTo(backup) ) {
                        throw new ControllerException("Unable to make backup of configuration file");
                    }
                    f = new File(SCHEDULER_FILE);
                }
                boolean success = false;
                try {
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)));

                    writer.write((new JSONObject(cfg)).toString());
                    writer.newLine();
                    writer.flush();
                    writer.close();
                    success = true;
                }
                finally {
                    if( !success && backup != null ) {
                        //noinspection ResultOfMethodCallIgnored
                        backup.renameTo(f);
                    }
                }
            }
            catch( IOException e ) {
                throw new ControllerException("Unable to save command file: " + e.getMessage());
            }
        }
    }

    public void scheduleCommands(@Nonnull CloudService service, @Nonnull String scheduleId, @Nonnegative long executeAfter, @Nonnull JSONObject ... commands) throws ControllerException {
        synchronized( commandQueue ) {
            if( !running ) {
                throw new ControllerException("Not currently accepting new commands");
            }
            if( executeAfter < System.currentTimeMillis() ) {
                throw new ControllerException("Invalid execution time: " + formatDate(executeAfter));
            }
            scheduler.add(new ScheduledCommandList(service.getServiceId(), scheduleId, executeAfter, commands));
            saveSchedule();
            commandQueue.notifyAll();
        }
    }

    public void shutdown() {
        synchronized( commandQueue ) {
            running = false;
            commandQueue.notifyAll();
            try {
                saveCommands(commandQueue);
            }
            catch( Throwable t ) {
                t.printStackTrace();
            }
            executorService.shutdown();
            try {
                if( !executorService.awaitTermination(2, TimeUnit.MINUTES) ) {
                    executorService.shutdownNow();
                }
            }
            catch( InterruptedException e ) {
                e.printStackTrace();
            }
        }
    }

    private Map<String,Object> toMap(JSONObject j) throws JSONException {
        HashMap<String,Object> map = new HashMap<String, Object>();

        for( String key : JSONObject.getNames(j) ) {
            map.put(key, toValue(j.get(key)));
        }
        return map;
    }

    private Object toValue(Object ob) throws JSONException {
        if( ob instanceof JSONObject ) {
            return toMap((JSONObject)ob);
        }
        else if( ob instanceof JSONArray ) {
            ArrayList<Object> converted = new ArrayList<Object>();
            JSONArray items = (JSONArray)ob;

            for( int i=0; i<items.length(); i++ ) {
                converted.add(toValue(items.get(i)));
            }
            return converted;
        }
        return ob;
    }
}
