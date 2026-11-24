@echo off
setlocal
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.13.10002-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MAVEN_CMD=%~dp0..\.tools\maven\apache-maven-3.9.9\bin\mvn.cmd"
if exist "%MAVEN_CMD%" (
  call "%MAVEN_CMD%" -f "%~dp0pom.xml" %*
  exit /b %ERRORLEVEL%
)
java -classpath "%~dp0.mvn\wrapper\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain %*
