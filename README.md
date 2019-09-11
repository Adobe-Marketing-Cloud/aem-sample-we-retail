# 🚫 ARCHIVED

We.Retail is no longer supported, the [WKND Guide](https://github.com/adobe/aem-guides-wknd) is the replacement reference site for Adobe Experience Manager.

## Modules

The main parts of the template are:

* core: Java bundle containing all core functionality like OSGi services, listeners or schedulers, as well as component-related Java code such as servlets or request filters.
* ui.apps: contains the /apps (and /etc) parts of the project, ie JS&CSS clientlibs, components, templates, runmode specific configs as well as Hobbes-tests
* ui.content: contains sample content using the components from the ui.apps
* it.tests: Java bundle containing JUnit tests that are executed server-side. This bundle is not to be deployed onto production.
* it.launcher: contains glue code that deploys the ui.tests bundle (and dependent bundles) to the server and triggers the remote JUnit execution
* all: additional module to build a single package embedding ui.apps and ui.content

## How to Build

To build all the modules run in the project root directory the following command with Maven 3:

    mvn clean install

If you have a running AEM instance you can build and package the whole project and deploy into AEM with  

    mvn clean install -PautoInstallPackage
    
Or to deploy it to a publish instance, run

    mvn clean install -PautoInstallPackagePublish
    
Or to deploy only the bundle to the author, run

    mvn clean install -PautoInstallBundle

To build a single package

    mvn clean install -PbuildSinglePackage

To install single package on an AEM instance

    mvn clean install -PbuildSinglePackage -PautoInstallSinglePackage
    

### UberJar

This project relies on the unobfuscated AEM 6.2 cq-quickstart. This is not publicly available from http://repo.adobe.com and must be 
manually 
downloaded from https://daycare.day.com/home/products/uberjar.html. After downloading the file (_cq-quickstart-6.2.0-apis.jar_), you must install it into your local Maven repository with this command:

    mvn install:install-file -Dfile=cq-quickstart-6.2.0-apis.jar -DgroupId=com.day.cq -DartifactId=cq-quickstart -Dversion=6.2.0 -Dclassifier=apis -Dpackaging=jar

## Testing

There are three levels of testing contained in the project:

* unit test in core: this show-cases classic unit testing of the code contained in the bundle. To test, execute:

    mvn clean test

* server-side integration tests: this allows to run unit-like tests in the AEM-environment, ie on the AEM server. To test, execute:

    mvn clean integration-test -PintegrationTests

* client-side Hobbes.js tests: JavaScript-based browser-side tests that verify browser-side behavior. To test:

    in the navigation, go the 'Operations' section and open the 'Testing' console; the left panel will allow you to run your tests.

## Maven Settings

The project comes with the auto-public repository configured. To setup the repository in your Maven settings, refer to:

    http://helpx.adobe.com/experience-manager/kb/SetUpTheAdobeMavenRepository.html

## Release History and System Requirements

The We.Retail reference site was first introduced in AEM 6.2. The following table gives an overview of the We.Retail releases and their system requirements:

We.Retail | Date        | AEM      | We.Retail Commons | Core Components | Core Components Extension
----------|-------------|----------|-------------------|-----------------|--------------------------
4.0.0     | 26/feb/2019 | 6.5.0.0  | 4.0.0             | 2.3.0           | 1.0.10
3.0.0     | 06/mar/2018 | 6.4.0.0  | 3.0.0             | 2.0.4           | 1.0.0
2.0.4     | 04/aug/2017 | 6.3.0.0  | 2.0.0             | 1.0.6           | N/A
1.0.0     | 05/jul/2016 | 6.2.0.0  | 1.0.0             | N/A             | N/A

For a full list of minimum system requirements for historical versions of the Core Components, see [Historical System Requirements](https://github.com/adobe/aem-core-wcm-components/blob/master/VERSIONS.md).
