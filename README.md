# MCA Automation

Spring Boot service for MCA automation APIs.

## Requirements

- Java 17 or newer
- Maven 3.9 or newer
- Internet access from the server to `www.mca.gov.in`
- `GEMINI_API_KEY` for the custom captcha extractor

Tesseract is no longer the active captcha path, but the dependency still exists for the dormant OCR flow. If you re-enable the old OCR path later, install Tesseract and set the OCR variables shown below.

## Run Locally

From the project folder:

```bash
cd /path/to/Ekmicro-API-automation
mvn -q -DskipTests package
GEMINI_API_KEY="YOUR_GEMINI_API_KEY" mvn spring-boot:run
```

If local testing is going through Charles Proxy or your JDK truststore cannot validate the MCA certificate chain, you may see `PKIX path building failed`. For local debugging only, run with:

```bash
GEMINI_API_KEY="YOUR_GEMINI_API_KEY" \
GEMINI_MODEL="gemini-2.5-flash" \
MCA_TLS_TRUST_ALL="true" \
mvn spring-boot:run
```

The service starts on:

```text
http://localhost:8080
```

Sample request:

```bash
curl --location 'http://localhost:8080/getcompanymasterdata' \
  --header 'Content-Type: application/json' \
  --header 'X-API-KEY: test-key' \
  --data '{
    "CompanyNameOrCIN": "L74909DL2008PLC180850",
    "userName": "YOUR_MCA_EMAIL",
    "password": "YOUR_MCA_PASSWORD",
    "deviceId": "YOUR_DEVICE_ID"
  }'
```

If login reaches the OTP stage, the response is the MCA cookie string to pass into `/verifyotpp`.

## Build A Jar

```bash
cd /path/to/Ekmicro-API-automation
mvn -q -DskipTests package
java -jar target/mca-automation-0.0.1-SNAPSHOT.jar
```

With environment variables:

```bash
GEMINI_API_KEY="YOUR_GEMINI_API_KEY" \
GEMINI_MODEL="gemini-2.5-flash" \
MCA_TLS_TRUST_ALL="false" \
SERVER_TOMCAT_THREADS_MAX="100" \
SERVER_TOMCAT_THREADS_MIN_SPARE="10" \
SERVER_TOMCAT_ACCEPT_COUNT="100" \
java -jar target/mca-automation-0.0.1-SNAPSHOT.jar
```

## Run On EC2 Linux

These commands target Amazon Linux 2023. Run them on the EC2 instance after SSH login.

Install Java, Maven, and Git:

```bash
sudo dnf update -y
sudo dnf install -y java-17-amazon-corretto-devel maven git
java -version
mvn -version
```

Copy or clone the project onto EC2, then enter the project folder:

```bash
cd /home/ec2-user/Ekmicro-API-automation
```

Set runtime environment variables:

```bash
export GEMINI_API_KEY="YOUR_GEMINI_API_KEY"
export GEMINI_MODEL="gemini-2.5-flash"
export MCA_TLS_TRUST_ALL="false"
export SERVER_TOMCAT_THREADS_MAX="100"
export SERVER_TOMCAT_THREADS_MIN_SPARE="10"
export SERVER_TOMCAT_ACCEPT_COUNT="100"
```

Build and run in the foreground:

```bash
mvn -q -DskipTests package
java -jar target/mca-automation-0.0.1-SNAPSHOT.jar
```

Run in the background with logs:

```bash
mkdir -p logs
nohup java -jar target/mca-automation-0.0.1-SNAPSHOT.jar > logs/mca-automation.log 2>&1 &
tail -f logs/mca-automation.log
```

Health check from the EC2 instance:

```bash
curl -i http://localhost:8080/
```

Test the API from the EC2 instance:

```bash
curl --location 'http://localhost:8080/getcompanymasterdata' \
  --header 'Content-Type: application/json' \
  --header 'X-API-KEY: test-key' \
  --data '{
    "CompanyNameOrCIN": "L74909DL2008PLC180850",
    "userName": "YOUR_MCA_EMAIL",
    "password": "YOUR_MCA_PASSWORD",
    "deviceId": "YOUR_DEVICE_ID"
  }'
```

To call it from your laptop, open inbound TCP port `8080` in the EC2 security group, then use:

```bash
curl --location 'http://EC2_PUBLIC_IP:8080/getcompanymasterdata' \
  --header 'Content-Type: application/json' \
  --header 'X-API-KEY: test-key' \
  --data '{
    "CompanyNameOrCIN": "L74909DL2008PLC180850",
    "userName": "YOUR_MCA_EMAIL",
    "password": "YOUR_MCA_PASSWORD",
    "deviceId": "YOUR_DEVICE_ID"
  }'
```

## OCR Dependencies

Maven downloads the Java OCR dependency (`tess4j`), but native Tesseract still must be installed on the machine.

macOS:

```bash
brew install tesseract
```

Ubuntu/Debian Linux:

```bash
sudo apt update
sudo apt install -y tesseract-ocr libtesseract-dev libleptonica-dev
```

Optional environment variables:

```bash
export OCR_TESSDATA_PATH="/usr/share/tessdata"
export OCR_LANGUAGE="eng"
export OCR_NATIVE_LIBRARY_PATH="/usr/lib64"
```

OCR is wired into the Maven Spring Boot run config. Linux uses the default Tesseract paths, and macOS/Homebrew paths are selected automatically by Maven profile. Start the server with:

```bash
mvn spring-boot:run
```

Captcha image audit saving is off by default. Enable it only while collecting test images:

```bash
-Dmca.captcha.audit.enabled=true
```

The custom captcha fallback still reads `GEMINI_API_KEY` from the environment.

Override these only if Tesseract is installed outside the default library/search paths:

```bash
mvn spring-boot:run -Docr.tessdataPath="/path/to/tessdata" -Docr.nativeLibraryPath="/path/to/native/libs"
```

## Notes

- The app uses the custom Gemini captcha extractor by default.
- `MCA_TLS_TRUST_ALL=true` disables HTTPS certificate validation for MCA calls and should only be used for local proxy/JDK truststore debugging. Keep it `false` or unset on EC2/production.
- The default database is in-memory H2.
- Same-user login attempts are serialized to avoid MCA captcha, OTP, and session-cookie collisions.
- Different users can still be served concurrently by Spring Boot/Tomcat.
