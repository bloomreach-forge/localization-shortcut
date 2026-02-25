#  Localization Shortcut plugin

Localization Shortcut plugin adds a shortcut to the CMS dashboard.
It allows the user to change the locale and/or timezone settings within the CMS context.
This can be particularly useful when users log into the CMS through an SSO-service (therefore bypassing the login options).

## Install

Install the plugin version property in the ```<properties>``` section of the main pom.xml.

Set the [latest version](release-notes.md) that has been released for your brX(M) project version. 

```xml
<bloomreach.forge.localizationshortcut.version>4.0.2</bloomreach.forge.localizationshortcut.version>
```

Install the following dependencies in the ```<dependencies>``` section of the ```cms-dependencies``` module's pom.xml:

```xml
    <dependency>
      <groupId>org.bloomreach.forge.localization-shortcut</groupId>
      <artifactId>bloomreach-localization-shortcut-repository</artifactId>
      <version>${bloomreach.forge.localizationshortcut.version}</version>
    </dependency>
```
```xml
    <dependency>
      <groupId>org.bloomreach.forge.localization-shortcut</groupId>
      <artifactId>bloomreach-localization-shortcut-cms</artifactId>
      <version>${bloomreach.forge.localizationshortcut.version}</version>
    </dependency>
```

## Setup

* Use the CMS Console (/cms/console) to set up this plugin:

* A dashboard shortcut node (localizationShortcutPlugin) will be installed under /hippo:configuration/hippo:frontend/cms/cms-dashshortcuts

* The locales available in the dropdown are the same as the ones that have been defined for your project. They can be altered here: [Localization modules](https://documentation.bloomreach.com/14/library/concepts/editor-interface/cms-ui-localization-modules.html) 

* To change labels for a locale, find the frontend:pluginconfig child node that corresponds with your locale and alter its properties

* If no frontend:pluginconfig node is present for your locale, copy an existing one and name it according to your locale. Then alter its properties.

* A "show.timezones" property has been added to switch between showing or hiding the dropdown for changing the timezone in the plugin dialog.

* The timezones available in the dropdown are defined as values of multi-valued String property "selectable.timezones". Valid timezone values can be found in the [IANA tz database](https://en.wikipedia.org/wiki/List_of_tz_database_time_zones).

* When no "selectable.timezones" property is present, ALL timezones in the [IANA tz database](https://en.wikipedia.org/wiki/List_of_tz_database_time_zones) will be listed by default.


