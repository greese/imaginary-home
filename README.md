imaginary-home
==============

Imaginary Home is a Java API for home automation. It currently supports the managed of the Philips Hue lightbulbs, but
I intend to add support for other household components as I progress in my own efforts of automating my house. The idea
behind this library is to have something that can support a heterogeneous house made up of equipment from a variety of
vendors. As you will see with the initial Hue lightbulbs, the concept has been abstracted so it can support other
kinds of light bulbs without you needing to rewrite your tools.

I have not set myself up to publish to Maven central and I haven't created a nice and tidy download yet. To use this
library, you will currently need to fetch the source code and build it yourself. The command to build it is:

> mvn -Dmaven.test.skip=true clean package

This will build a JAR in the target/ directory and all of the supporting libraries will appear in target/lib.

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
you need to make sure all of the JARs in target/lib/ as well as the imaginary-home-2013.01-SNAPSHOT.jar from target/
are in your CLASSPATH. You can then get the full help by typing:

> java com.imaginary.home.lighting.Main

In particular, if you want to create an access key for your Hue hub so you can play with this library, push the button on
your Hue hub and issue the command:

> java com.imaginary.com.lighting.Main 192.168.1.1 pair "My Test App"

Feel free to substitute any string for "My Test App". Also, naturally, use the IP address for your hub instead of
192.168.1.1. This command will create an access key and pair it with your hub. You can then do things like:

> java com.imaginary.home.lighting.Main 192.168.1.1 fadeon huePairedUserName all 1m 100

This command will fade all of your lights on to full brightness (100%) over the course of 1 minute.
