Philips Hue Controller - Documentation
This project provides a Web-based interface and a Java Backend Server for local control of Philips Hue lamps via Bridge v1/v2.

System Requirements
Java JDK 11+

Philips Hue Bridge connected to the same local network.

API Token (already configured in the system as TOKEN).

API Endpoints Architecture
The server listens at http://localhost:8080. All data is returned in JSON format.

1. Get Lamps List
Returns a complete list of all connected lamps and their current states.

Endpoint: /getLamps

Method: GET

Response: A JSON object containing IDs, names, and technical specifications for each lamp.

2. Power Control (Toggle)
Turns a specific lamp on or off.

Endpoint: /toggle

Method: GET

Query Parameters:

id (int): The unique ID of the lamp (e.g., 3).

on (boolean): true to turn on, false to turn off.

Example: /toggle?id=3&on=true

3. Set Color (HEX)
Changes the lamp color using a hexadecimal code. The server automatically converts the HEX value into HSL coordinates compatible with the Bridge.

Endpoint: /setHex

Method: GET

Query Parameters:

id (int): The ID of the lamp.

color (string): HEX color code without the # symbol (e.g., FF0000 for red).

Example: /setHex?id=3&color=00FF00

Web Interface Features
The interface is built with Tailwind CSS and includes:

Scan Lamps: A quick-access button to query the Bridge and view the current network status in JSON format.

ID Selector: Numeric input to specify which lamp to operate on.

Power Controls: Two distinct buttons for immediate power-on or power-off.

Color Picker: A native color selector that allows users to visually choose the desired tint.

Response Console: A scrollable section showing real-time success or error messages returned by the Bridge.

Technical Notes
CORS (Cross-Origin Resource Sharing)
The Java server is configured to accept requests from any origin via the following header:
Access-Control-Allow-Origin: *
This is essential to allow the local HTML page to communicate with the Java process on port 8080.

Color Conversion
The system utilizes the java.awt.Color library to map the RGB color space to the Philips Hue HSB/HSL system:

Hue: Mapped on a scale of 0-65535.

Saturation: Mapped on a scale of 0-254.

Brightness: Mapped on a scale of 0-254.

How to Start
Run the Main.java class to start the HTTP server.

Open the index.html file in a modern web browser.

Ensure the Bridge is reachable at the IP address 172.16.17.200.
