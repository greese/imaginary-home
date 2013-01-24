imaginary-home
==============

Imaginary Home is a suite of Open Source APIs for home automation. It includes both a reference implementation for
a RESTful home automation API for cloud and home, but also a Java abstraction API for talking to devices and the
REST API. Imaginary Home opens up the entire spectrum of home automation—not just the devices, but also remote interaction
with them through cloud services.

A typical home automation system, for example, consists of the devices, a cloud service, and a controller relay that
enables communication between the devices in the home and the cloud. Some systems open up the device API. Few open up
the controller or cloud service elements. With Philips Hue, for example, the only way currently to remotely control
your lights is through their iPhone app or web portal. To use tools you develop or other ecosystem tools, you must be
inside your house. Even if they open up their cloud portal API, that portal controls only your Hue light bulbs, not the
other devices in your home.

The Imaginary Home API four components:

* An API-driven cloud service serving as a reference implementation for two REST APIs:
** An externally-facing REST API for third-party tools to remotely manage your home
** A controller relay API for enabling controller relays in the home to communicate with one or more cloud services
* An API-driven controller relay service that also acts as a reference implementation for the relay side of the
cloud service/relay API
* A Java abstraction API for communicating with home automation devices
* Reference implementations for various devices (such as Philips Hue)

Because we don't have a 1.0 release yet, no official release is in Maven Central. However, snapshots are regularly
published. The current Maven Central snapshot build is:
* groupId -> com.imaginary
* project ID -> imaginary-home
* version -> 2013.01-SNAPSHOT

If you want to quickly see what this library can do, run the unit tests. First, create a ~/.m2/settings.xml file that
looks minimally like this:

```xml
<?xml version="1.0" encoding="UTF-8"?>

<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
 <profiles>
    <profile>
      <id>hue</id>
      <properties>
        <ip>192.168.1.1</ip>
        <accessKey>huePairedUserName</accessKey>
      </properties>
    </profile>
 </profiles>
</settings>
```

Of course, you should put in the IP address of your hub and an actual paired user name that your Hue device will
recognize. If you have yet to pair your device, you can use the command line tool to pair it. More on that later.

With that profile created in your settings.xml file, issue the command:

> mvn -Phue clean test

A test will run for the next 5 to 10 minutes that will run through all of the features of this library against your
Hue lightbulbs. To see the test source code, check out the HueTest.java class.

This library comes with a command-line utility that provides command-line access to all of its features. To run it,
clone the imaginary-home source from GitHub, build the project, and make sure all of the JARs in target/lib/ as well as the
imaginary-home-2013.01-SNAPSHOT.jar from target/ are in your CLASSPATH. You can then get the full help by typing:

> java com.imaginary.home.lighting.Main

In particular, if you want to create an access key for your Hue hub so you can play with this library, push the button on
your Hue hub and issue the command:

> java com.imaginary.com.lighting.Main 192.168.1.1 pair "My Test App"

Feel free to substitute any string for "My Test App". Also, naturally, use the IP address for your hub instead of
192.168.1.1. This command will create an access key and pair it with your hub. You can then do things like:

> java com.imaginary.home.lighting.Main 192.168.1.1 fadeon huePairedUserName all 1m 100

This command will fade all of your lights on to full brightness (100%) over the course of 1 minute.
