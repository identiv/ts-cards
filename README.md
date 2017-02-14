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
- identiv-envelope: *rename this* pacs data formatting

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

### Contact Us

This is a new OSS project and we're still finding the best way to interact with
developers. Please reach out to us if you want to contribute or if we can help you get started etc. There are several options:

* here on github &mdash; open a pull request, raise issues, etc
* gitter chat &mdash; log into gitter with your github account and checkout the
  identiv rooms
* email &mdash; email us at oss@identiv.com


