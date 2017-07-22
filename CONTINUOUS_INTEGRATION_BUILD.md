# Sypnosis  
This serves as a reference to the Continuous Integration build for projects using AEM 6.3.
### Maven Plugins Required
1. This project runs on [Maven v3.5.0](https://maven.apache.org). Ensure that all necessary plugins and their corresponding properties and configurations are set.

    - maven-surefire-plugin
    - maven-failsafe-plugin
    - jacoco-maven-plugin
    - sonar-maven-plugin
    - dependency-check-maven

2. The modules version format `<version>${version}</version>` should comply with [semantic versioning](https://semver.org) standards.  

3. To enable automated versioning, developers are required to always develop on SNAPSHOT versions such as `<version>1.0-SNAPSHOT</version>`  

4. It is recommended that everyone who is involved in building the project app understands the [maven lifecycle](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html).     
---
#### Jacoco Plugin sample
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.7.9</version>
    <executions>

        <!-- Configure the jacoco output file for unit test -->
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

        <!-- Generate jacoco unit test report with exclusions -->
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

        <!-- Configure the jacoco output file for integration test -->
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

        <!-- Generate jacoco integration test report --> 
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

        <!-- 
        Enforce jacoco rule for unit test output with exclusions.
        The rule is that the COVEREDRATIO of all packages combined (BUNDLE)
        should be 80% and counter is INSTRUCTION
        --> 
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
#### Maven Surefire Plugin sample 
```xml
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
#### Failsafe Plugin sample
If developers use the Surefire Plugin for running tests, then when you have a test failure, the build will stop at the integration-test phase and your integration test environment will not have been torn down correctly. 

The Failsafe Plugin is used during the integration-test and verify phases of the build lifecycle to execute the integration tests of an application. The Failsafe Plugin will not fail the build during the integration-test phase, thus enabling the post-integration-test phase to execute.  
```xml
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
**Using a specific SonarQube version**
```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <version>3.3.0.603</version>
</plugin>
```

#### Adding build properties to configure plugins behavior
```xml
<properties>

   ....

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
    <!-- Sonar use dependency check xml report path -->
    <sonar.jacoco.reportPaths>target/coverage-reports/jacoco-ut.exec</sonar.jacoco.reportPaths>

    <!-- Configurable properties for dependency-check -->
    <dependencyCheck.skip>true</dependencyCheck.skip>
    <dependencyCheck.failBuild>true</dependencyCheck.failBuild>

  ....

</properties>
```  
#### Using OWASP Top 10 Dependency Check
```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>2.0.1</version>
    <configuration>
        <format>ALL</format>
        <skip>${dependencyCheck.skip}</skip>
        <failBuildOnAnyVulnerability>${dependencyCheck.failBuild}</failBuildOnAnyVulnerability>
        <suppressionFiles>
            <suppressionFile>${basedir}/dependency-check-suppressions.xml</suppressionFile>
        </suppressionFiles>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>aggregate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```
----
## How-to Build Commands  

These set of commands are ideally executed in the Contintuous Integration server.  

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
### Security test with OWASP Dependency check
```bash
mvn dependency-check:aggregate
```
### SonarQube Scan
This command will run and publish the static code analysis result along with the unit tests, jacoco, and dependenchy check results to SonarQube server `sonar.host.url`.  
```bash
mvn sonar:sonar -Dsonar.host.url=http://sonar.example.com -Dsonar.login=aGlhZHNkYTEyM2wxMmszO2wxMjszazE7bDIzazFsMjszMTJzZAo
```
SonarQube preview mode should be executed for feature branches and forks.  
```bash
mvn sonar:sonar -Dsonar.host.url=http://sonar.example.com -Dsonar.login=aGlhZHNkYTEyM2wxMmszO2wxMjszazE7bDIzazFsMjszMTJzZAo -Dsonar.analysis.mode=preview -Dsonar.report.export.path=report.json
```
A json report file will be generated as `target/sonar/report.json`.  
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
