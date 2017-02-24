APDU Engine
===========

Introduction
------------

Apdu Engine simplifies sending a series of APDU messages to a smart card and
validating the responses. The initial implementation focusses on DESFire (which
is admittedly quite simple) but it could be adapted to other uses (e.g. Global
Platform).

There are four sub-projects here:

* apdu-engine: this is the core APDU handling code (contains DESFire code)
* apdu-engine-cli: a very simple CLI tool for interacting with DESFire cards
* apdu-engine-gui: a simple tool for exploring DESFire cards (this could be
  extended to support operations on other card types)
* identiv-oacf: Open Access Card Format (OACF) data formatting

### Getting Started

There are two main suggested ways to get started. The most appropriate for you
will depend on what you want to achieve.

1. The recommended route is to learn about your cards first up. If you have a
   DESFire EV1 card in hand and would like to interact with it then you can
   clone this repository, build it and look at the notes on getting started
   with apdu-engine-gui.

2. if you are more interested in looking at code - e.g. if you don't have a
   card yet - then you might want to read the design notes below and then check
   out the JavaDocs.

### Building

We use [Gradle](https://gradle.org). The repository contains a Gradle wrapper
which can download and use Gradle itself. You can alternately install Gradle
2.14.1 from https://gradle.org and adapt the commands below to use your local
install of Gradle.

#### Build Everything

```
$ ./gradlew build
```

This will build everything. You can run apdu-engine-cli and apdu-engine-ui
directly from the commandline:

```
java -jar apdu-engine-ui/build/libs/apdu-engine-ui-1.1.0-SNAPSHOT-capsule.jar
```
and:

```
java -jar apdu-engine-cli/build/libs/apdu-engine-cli-1.1.0-SNAPSHOT-capsule.jar
usage: java -jar apdu-engine-cli.jar [options]
 -a,--reset-aes               Reset PICC to default AES key
 -c,--create-app <arg>        Create TS app with diversified keys +
                              provided payload (does not change PICC
                              master key)
 -d,--reset-2k3des            Reset PICC to default 2K3DES key (takes
                              precedence over --reset-aes: if both are
                              specified then only --reset-2k3des takes
                              effect)
 -f,--format-card             Format card
 -h,--help                    Show usage information
 -k,--get-key-settings        Get PICC key settings
 -l,--change-key-settings     Require auth to create application
 -n,--dry-run                 Make no destructive changes, just log
 -p,--create-pacs-app <arg>   Create TS app with diversified keys and wrap
                              payload in TS credential format
 -r,--read-app                Read TS app file 0
 -v,--get-version             Get Version info from card (includes UID)
 -x,--key <arg>               Specify key for PICC authentication
 -z,--diversify-key           Diversify PICC key on UID
```

#### Dependencies

We have kept the dependencies fairly minimal but used third-party libraries as
appropriate. This is an overview of what we use:

**Slf4j**: We use SLF4J for logging as this allows the logging backend to be
decided by consumers of the library rather than mandated by the library itself.

**Guava**: This provides high quality utility classes which allow code to be
less verbose and more expressive

**Groovy**: We use Groovy (and Spock and JUnit) for unit tests.  Spock, in
particular, allows us to write short, expressive tests.

**Capsule**: we package the CLI and UI tools in capsules which contain all
their dependencies in a single JAR (except the JRE itself).

### Stability

This code base is still quite new. It works reliably and a very similar
revision of this code is in production at Identiv. This is a SNAPSHOT release
and changes should be anticipated. We do not know of any bugs but would welcome
bug reports or pull requests via GitHub.

### Contact Us

This is a new OSS project and we're still finding the best way to interact with
developers. Please reach out to us if you want to contribute or if we can help
you get started etc. There are several options:

* here on github &mdash; open a pull request, raise issues, etc
* gitter chat &mdash; log into gitter with your github account and checkout the
  identiv rooms
* email &mdash; email us at oss@identiv.com

APDU Engine GUI
---------------

This is a simple UI which supports (limited) interactions with DESFire EV1
cards. This will be extended as time goes on.

APDU Engine CLI
---------------

This is more directly a test tool (rather than exploration as is the case for
APDU Engine GUI). The CLI tool is a little more fully featured but is more
targetted towards things that we needed at Identiv during development of our TS
card oferring.

APDU Engine
-----------

This is the core code library which implements DESFire communication. All
communication is handled via APDU wrapping. This is compatible with Java's
`javax.smartcardio` package.

The apdu-engine-gui and apdu-engine-cli as well as the unit tests can serve as
example code alongside this documentation.

The main class to interact with is called ApduSession. The basic usage pattern is:

1. create ApduSession object - this will hold session state and coordinate
   assembling actual binary data to send to the card
2. create and configure ApduCommand objects
3. broker communication with the card

Here is some simple sample code which authenticates to a card (and establishes
a session key) using a default (i.e. all zeros) AES key.

        ApduSession session = new ApduSession(
            new SelectPicc(),
            new DesfireAuth(new byte[16], DesfireKeyType.AES_128);
        StatusResponse status = session.transmit(
            SmartcardIoTransmitter.create());

That is quite a short snippet which invokes quite a lot of code! Let's break
that down a little. First we create ApduSession and pass it a varargs list of
ApduCommand objects, representing commands to send to the card. These can be as
simple as a single command and expected response, or, in the case of
authentication, they can represent several exchanges back and forth.

In this example we have both:

1. selecting the PICC is optional on EV1 cards but is demonstrative here
2. the DesfireAuth command is obviously critical - apdu-engine currentl
   implements 2-key 3DES and AES - this command will perform mutual
   authentication between the terminal and card (in this case the PICC). The
   result is a mutually agreed session key.
3. session.transmit exchanges data with the card

It is worth noting here that we have extracted an interface here for exchanging
data with a card. We have a simple (and limited) implementation for using
javax.smartcardio to exchange with the card. Getting started this is probably
the simplest and most straight-forward approach available but the best way
forward will depend on the needs of your project. At Identiv we also marshal
data from a server via JSON to a client which actually communicates with the
card - in this case we do not use the transmitter interface at all (though we
hope to refactor to make this possible in the future). The other usage of
ApduTransmitter is by MockTransmitter which is in essence a very limitted
emulation of a DESFire EV1 card - it allows us to test our command
implementations by emulating a card.

### ApduCommand implementations

We have implemented many (but by no means all) DESFire EV1 commands. As above
these are implemented as commands wrapped in APDUs (this allows for simple
interoperability).


|        DESFire Command       |    Class    | Comments                                                                                        |
|:----------------------------:|:-----------:|-------------------------------------------------------------------------------------------------|
| AuthenticateISO              | DesfireAuth | Currently supports authentication and establishing a session key. Supports: AES & 2-key 3DES    |
| AuthenticateAA               | DesfireAuth | ...                                                                                             |
| ChangeKey                    |             |                                                                                                 |
| ChangeKeySettings            |             |                                                                                                 |
| CreateApplication            |             |                                                                                                 |
| CreateStdFile                |             |                                                                                                 |
| CreateApplication            |             |                                                                                                 |
| DeleteApplication            |             |                                                                                                 |
| GetCardUID                   |             |                                                                                                 |
| FormatCard                   |             |                                                                                                 |
| GetApplicationIds            |             |                                                                                                 |
| GetDfNames                   |             |                                                                                                 |
| GetFileIds                   |             |                                                                                                 |
| GetFileSettings              |             |                                                                                                 |
| GetKeySettings               |             |                                                                                                 |
| GetKeyVersion                |             |                                                                                                 |
| GetVersion                   |             |                                                                                                 |
| ReadData                     |             |                                                                                                 |
| SelectApplication            |             |                                                                                                 |
| WriteData                    |             |                                                                                                 |

#### DesfireAuth

&#x26a0; &#xfe0f; **Note:** 3-key 3DES authentication is not supported.

This supports generating the correct commands for both 2-key 3DES and AES
authentication. While 3-key 3DES has not been tested it would not be difficult
to extend support.

Contributors
------------

| Contributor       | Contact                                                   | Details                                 |
|-------------------|-----------------------------------------------------------|-----------------------------------------|
| Mark Butcher      | mbutcher@identiv.com https://github.com/macbutch          | overall design + initial implementation |
| Hoai Phuong Lu    | hoai-phuong@it-developers.com https://github.com/phuonglu | apdu-engine implementation              |
| Thu "Daniel' Tran | thu@it-developers.com https://github.com/danielthu        | apdu-engine-gui                         |
