# Sypnosis  

This serves as a reference to the Continuous Integration build for projects using AEM 6.3.

## Pre-requisites
This project runs on [Maven v3.5.0](https://maven.apache.org). Ensure that all necessary plugins and their corresponding properties and configurations are set.

For Coverage Reports and Unit Testing, consider the following:  
- JaCoCo v0.7.9 Plugin  
- Surefire v2.15 Plugin  
- Failsafe v2.15 Plugin  

For Sonarqube v6.4 deployment, consider the following:  
- Sonar Maven v3.3.0.603 Plugin  

For Dependency Check, consider the following:  
- Dependency Check Owasp  

The modules version format `<version>${version}</version>` should  comply with [semantic versioning](https://semver.org).  

Developers are required to only code in SNAPSHOT version format.  

## Required build plugins and their configurations
### Test Coverage
Java Code Coverage (Jacoco) plugin configuration.  
```xml
<!-- Java Code Coverage plugin -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.7.9</version>
    <executions>
        <execution>
            <id>pre-unit-test</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
            <configuration>
                <destFile>${project.build.directory}/coverage-reports/jacoco-ut.exec</destFile>
                <propertyName>surefireArgLine</propertyName>
            </configuration>
        </execution>

        <execution>
            <id>post-unit-test</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
            <configuration>
                <includes>
                    <include>**/core/model/**</include>
                </includes>
                <excludes>
                    <exclude>**/core/model/*Article*</exclude>
                    <exclude>**/core/model/*CountriesFormOptionsDataSource*</exclude>
                    <exclude>**/core/model/*NavCartModel*</exclude>
                    <exclude>**/core/model/*ProductModel*</exclude>
                    <exclude>**/core/model/*ShoppingCartModel*</exclude>
                    <exclude>**/core/model/*WishlistModel*</exclude>
                    <exclude>**/core/model/*VendorOrderModel*</exclude>
                </excludes>
                <dataFile>${project.build.directory}/coverage-reports/jacoco-ut.exec</dataFile>
                <outputDirectory>${project.reporting.outputDirectory}/jacoco-ut</outputDirectory>
            </configuration>
        </execution>

        <execution>
            <id>pre-integration-test</id>
            <phase>pre-integration-test</phase>
            <goals>
                <goal>prepare-agent-integration</goal>
            </goals>
            <configuration>
                <destFile>${project.build.directory}/coverage-reports/jacoco-it.exec</destFile>
                <propertyName>failsafeArgLine</propertyName>
            </configuration>
        </execution>

        <execution>
            <id>post-integration-test</id>
            <phase>post-integration-test</phase>
            <goals>
                <goal>report-integration</goal>
            </goals>
            <configuration>

                <dataFile>${project.build.directory}/coverage-reports/jacoco-it.exec</dataFile>

                <outputDirectory>${project.reporting.outputDirectory}/jacoco-it</outputDirectory>
            </configuration>
        </execution>

        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <dataFile>${project.build.directory}/coverage-reports/jacoco-ut.exec</dataFile>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <includes>
                            <include>we.retail.core.model.*</include>
                        </includes>
                        <excludes>
                            <exclude>we.retail.core.model.handler.*</exclude>
                            <exclude>we.retail.core.model.Article*</exclude>
                            <exclude>we.retail.core.model.CountriesFormOptionsDataSource*</exclude>
                            <exclude>we.retail.core.model.NavCartModel</exclude>
                            <exclude>we.retail.core.model.WishlistModel</exclude>
                            <exclude>we.retail.core.model.ProductModel</exclude>
                            <exclude>we.retail.core.model.ShoppingCartModel*</exclude>
                            <exclude>we.retail.core.model.VendorOrderModel</exclude>
                        </excludes>
                        <limits>
                            <limit>
                                <counter>INSTRUCTION</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```
### Surefire Plugin
A plugin use for running Unit test.  
```xml
<!-- Maven Surefire Plugin -->
<plugin>
   <groupId>org.apache.maven.plugins</groupId>
   <artifactId>maven-surefire-plugin</artifactId>
   <version>2.15</version>
      <configuration>
         <argLine>${surefireArgLine}</argLine>
         <skipTests>${skip.unit.tests}</skipTests>
         <excludes>
            <exclude>**/IT*.java</exclude>
         </excludes>
      </configuration>
</plugin>
```
### Failsafe Plugin
If developers use the Surefire Plugin for running tests, then when you have a test failure, the build will stop at the integration-test phase and your integration test environment will not have been torn down correctly. 

The Failsafe Plugin is used during the integration-test and verify phases of the build lifecycle to execute the integration tests of an application. The Failsafe Plugin will not fail the build during the integration-test phase, thus enabling the post-integration-test phase to execute.  
```xml
<!-- Maven Failsafe Plugin -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <version>2.15</version>
    <executions>
        <execution>
            <id>integration-tests</id>
            <goals>
                <goal>integration-test</goal>
                <goal>verify</goal>
            </goals>
            <configuration>
                <argLine>${failsafeArgLine}</argLine>
                <skipTests>${skip.integration.tests}</skipTests>
            </configuration>
        </execution>
    </executions>
</plugin>
```
### Using a specific SonarQube version
```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <version>3.3.0.603</version>
</plugin>
```

## How to add build properties
```xml
<properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <aem.host>localhost</aem.host>
    <aem.port>4502</aem.port>
    <aem.publish.host>localhost</aem.publish.host>
    <aem.publish.port>4503</aem.publish.port>
    <aem.contextPath />
    <sling.user>admin</sling.user>
    <sling.password>admin</sling.password>
    <vault.user>admin</vault.user>
    <vault.password>admin</vault.password>
    <slf4j.version>1.5.11</slf4j.version>

    <!-- Dependency Report Path for Sonarqube -->
    <sonar.dependencyCheck.reportPath>${project.build.directory}/dependency-check-report.xml</sonar.dependencyCheck.reportPath>

    <!-- SonarQube inclusions -->
    <sonar.inclusions>
    **/core/model/**
    </sonar.inclusions>

    <!-- SonarQube exclusions -->
    <sonar.exclusions>
    **/core/model/*Article*,
    **/core/model/*CountriesFormOptionsDataSource*,
    **/core/model/*NavCartModel*,
    **/core/model/*ProductModel*,
    **/core/model/*ShoppingCartModel*,
    **/core/model/*WishlistModel*,
    **/core/model/*VendorOrderModel*,
    </sonar.exclusions>

    <sonar.jacoco.reportPaths>target/coverage-reports/jacoco-ut.exec</sonar.jacoco.reportPaths>
</properties>
```  
  
## How-to Commands  
For maven lifecycle references: https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html. 

### Build, Test, Jacoco, Dependency check and local Deploy
For Developers, this is how to build, test and deploy built java packages in their local maven repository (~/.m2).  
```bash 
mvn clean install
```
### Build, Test, Jacoco, Dependency check without Deploy
The ideal command for build servers. Do not deploy built packages to save time and space.  
```bash
mvn clean verify
```
### SonarQube Scan
This command will run and publish the static code analysis result along with the unit tests, jacoco, and dependenchy check results to SonarQube server `sonar.host`.  
```bash
mvn sonar:sonar
```
### Deploying Snapshots to Nexus
Only applicable if version is in SNAPSHOT format. E.g: `0.1.1-SNAPSHOT` or `0.1-SNAPSHOT`.  
```bash
mvn deploy -DaltDeploymentRepository="nexus-snapshot::default::http://localhost:8081/nexus/content/repositories/snapshots/"
```
Deployed packages should then be visible in http://localhost:8081/nexus/content/repositories/snapshots/.  

### Deploying Releases to Nexus
Only applicable if version is in RELEASE format. E.g: `0.1.1` or `0.1.3`. If the version to be deployed already exists or has already been deployed, maven will fail.  
```bash
mvn deploy -DaltDeploymentRepository="nexus-snapshot::default::http://localhost:8081/nexus/content/repositories/snapshots/"
```
Deployed packages should then be visible in http://localhost:8081/nexus/content/repositories/releases/.  
