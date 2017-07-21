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

## Inserting the Plugins
### JaCoCo Plugin and Corresponding Configurations
Some plugins may include Inclusions and Exclusions
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
### Dependency Check Plugin OWASP
```xml
<!-- SONARQUBE - Maven Plugin; FOR DEPENDENCY CHECK; Renewed version -->
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <version>3.3.0.603</version>
</plugin>
```

## Properties Configuration
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
  
## Commands  
### Code Coverage Report and Dependency Check
This command will produce the reports for code coverage and dependency check.
```
mvn clean verify
```
  
### Sonarqube Deployment  
This command will deploy the reports/results to local Sonarqube server.
```
mvn sonar:sonar
```

