# We.Retail
[![Build Status](https://travis-ci.org/Adobe-Marketing-Cloud/aem-sample-we-retail.png?branch=master)](https://travis-ci.org/Adobe-Marketing-Cloud/aem-sample-we-retail)

This is an AEM 6.3 reference implementation for the retail industry.

## Modules

The main parts of the template are:

* core: Java bundle containing all core functionality like OSGi services, listeners or schedulers, as well as component-related Java code such as models, servlets or request filters.
* ui.apps: contains the /apps (and /etc) parts of the project, ie JS&CSS clientlibs, components, templates
* ui.content: contains sample content using the components from the ui.apps
* config: configuration packages, contains configurations needed for the implementation
* it.tests.ui-js: UI integrations tests based on [Hobbes](https://docs.adobe.com/docs/en/aem/6-2/develop/components/hobbes.html)
* parent: Parent POM with basic configurations for building the project
* all: additional module to build a single package embedding core bundle, ui.apps, ui.content and config package

## How to build

To build all the modules run in the project root directory the following command with Maven 3:

    mvn clean install

If you have a running AEM instance you can build and package the whole project and deploy into AEM with  

    mvn clean install -PautoInstallPackage
    
Or to deploy it to a publish instance, run

    mvn clean install -PautoInstallPackagePublish
    
Or to deploy only the bundle to the author, run

    mvn clean install -PautoInstallBundle

To install single package on an AEM instance

    mvn clean install -PautoInstallSinglePackage
    

### UberJar

This project relies on the unobfuscated AEM 6.3 cq-quickstart. This is publicly available on https://repo.adobe.com

For more details about the UberJar please head over to the
[How to Build AEM Projects using Apache Maven](https://docs.adobe.com/docs/en/aem/6-3/develop/dev-tools/ht-projects-maven.html)
documentation page.

## Testing

Testing is done using client-side Hobbes.js tests: JavaScript-based browser-side tests that verify browser-side behavior. To test:

    in the navigation, go the 'Operations' section and open the 'Testing' console; the left panel will allow you to run your tests.


## Maven settings

The project comes with the auto-public repository configured. To setup the repository in your Maven settings, refer to:

    http://helpx.adobe.com/experience-manager/kb/SetUpTheAdobeMavenRepository.html
