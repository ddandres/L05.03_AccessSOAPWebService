/*
 * Copyright (c) 2016. David de Andrés and Juan Carlos Ruiz, DISCA - UPV, Development of apps for mobile devices.
 */

package labs.dadm.l0503_accesssoapwebservice;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

// Converts a given temperature from Celsius degrees into Fahrenheit degrees (or viceversa)
// using the SOAP web service located at: http://www.w3schools.com/xml/tempconvert.asmx
public class SoapTemperatureActivity extends AppCompatActivity {

    // Identify the operation to perform
    private static final int CELSIUS_TO_FAHRENHEIT = 0;
    private static final int FAHRENHEIT_TO_CELSIUS = 1;

    // Hols reference to View objects
    EditText etCelsius;
    EditText etFahrenheit;
    ProgressBar pbTemperature;
    ImageButton bC2F;
    ImageButton bF2C;

    // Hanlder
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_soap_temperature);

        // Keep references to View objects30
        etCelsius = findViewById(R.id.etCelsius);
        etFahrenheit = findViewById(R.id.etFahrenheit);
        pbTemperature = findViewById(R.id.pbTemperature);
        bC2F = findViewById(R.id.bC2F);
        bF2C = findViewById(R.id.bF2C);

        final View.OnClickListener listener = v -> convertTemperature(v.getId());
        findViewById(R.id.bC2F).setOnClickListener(listener);
        findViewById(R.id.bF2C).setOnClickListener(listener);

        handler = new Handler();
    }

    // Handles the event to convert the temperature
    private void convertTemperature(int clickedButton) {

        if (isConnected()) {
            // Create the AsyncTask in charge of accessing the web service in background
            TemperatureThread thread = new TemperatureThread();

            // Displays an indeterminate ProgressBar to show that an operation is in progress
            pbTemperature.setVisibility(View.VISIBLE);
            // Disables the buttons so the user cannot launch multiple requests
            bC2F.setEnabled(false);
            bF2C.setEnabled(false);

            if (clickedButton == R.id.bC2F) {
                // Convert Celsius degrees into Fahrenheit degrees

                // Check that something has been entered as temperature
                if (!etCelsius.getText().toString().isEmpty()) {
                    thread.configureOperation(CELSIUS_TO_FAHRENHEIT, etCelsius.getText().toString());
                    thread.start();
                }
                // Notify the user that it is required to enter a temperature
                else {
                    Toast.makeText(SoapTemperatureActivity.this, R.string.not_data, Toast.LENGTH_SHORT).show();
                }
            } else if (clickedButton == R.id.bF2C) {
                // Convert Fahrenheit degrees into Celsius degrees

                // Check that something has been entered as temperature
                if (!etFahrenheit.getText().toString().isEmpty()) {
                    thread.configureOperation(FAHRENHEIT_TO_CELSIUS, etFahrenheit.getText().toString());
                    thread.start();
                }
                // Notify the user that it is required to enter a temperature
                else {
                    Toast.makeText(SoapTemperatureActivity.this, R.string.not_data, Toast.LENGTH_SHORT).show();
                }
            }
        }
        // Notify the user that the device has not got Internet connection
        else {
            Toast.makeText(SoapTemperatureActivity.this, R.string.not_connected, Toast.LENGTH_SHORT).show();
        }
    }

    // Determines whether the device has got Internet connection.
    public boolean isConnected() {
        boolean result = false;

        // Get a reference to the ConnectivityManager
        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        // Get information about the default active data network
        if (Build.VERSION.SDK_INT > 22) {
            final Network activeNetwork = manager.getActiveNetwork();
            if (activeNetwork != null) {
                final NetworkCapabilities networkCapabilities = manager.getNetworkCapabilities(activeNetwork);
                result = networkCapabilities != null && (
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
            }
        } else {
            NetworkInfo info = manager.getActiveNetworkInfo();
            result = ((info != null) && (info.isConnected()));
        }

        return result;
    }

    public void resultReceived(int op, String result) {
        // Check whether the web services returned a response
        if (result != null) {
            // Update the temperature according to the requested operation
            if (op == CELSIUS_TO_FAHRENHEIT) {
                etFahrenheit.setText(result);
            } else {
                etCelsius.setText(result);
            }
        }
        // Notify the user that the request could not be completed
        else {
            Toast.makeText(this, R.string.not_completed, Toast.LENGTH_SHORT).show();
        }

        // Hides the indeterminate ProgressBar as the operation has finished
        pbTemperature.setVisibility(View.INVISIBLE);
        // Enables the buttons so the user cannot launch another request
        bC2F.setEnabled(true);
        bF2C.setEnabled(true);
    }

    // Accesses a SOAP web service to convert the given temperature to other units.
    // Input parameters are: an int identifying the operation to perform,
    // and a String representing the temperature in the original units.
    // The output parameter is a String representing the temperature in the target units.
    private class TemperatureThread extends Thread {

        // Constants related to the web service
        private final static String NAME_SPACE = "https://www.w3schools.com/xml/";
        private final static String LOCATION = "https://www.w3schools.com/xml/tempconvert.asmx";
        private final static String ACTION_TO_FAHRENHEIT = "CelsiusToFahrenheit";
        private final static String ACTION_TO_CELSIUS = "FahrenheitToCelsius";
        private final static String CELSIUS = "Celsius";
        private final static String FAHRENHEIT = "Fahrenheit";

        // Operation to perform
        int op;
        // Value to convert
        String value;
        // Result
        SoapPrimitive response = null;

        public void setOp(int op) {
            this.op = op;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public void configureOperation(int op, String value) {
            setOp(op);
            setValue(value);
        }

        @Override
        public void run() {
            // Property Name and Action for the SOAP web service
            String propertyName;
            String action;

            // Obtain the Property Name and Action for the required operation
            if (op == CELSIUS_TO_FAHRENHEIT) {
                propertyName = CELSIUS;
                action = ACTION_TO_FAHRENHEIT;
            } else {
                propertyName = FAHRENHEIT;
                action = ACTION_TO_CELSIUS;
            }

            // Create the required SOAP object that will hold all the data to be exchanged
            SoapObject request = new SoapObject(NAME_SPACE, action);
            // Add the temperature to convert to the SOAP object (key, value)
            request.addProperty(propertyName, value);

            // Create the envelope that will transport the SOAP object (SOAP v1.2)
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER12);
            envelope.dotNet = true;
            // Include the SOAP object within the envelope
            envelope.setOutputSoapObject(request);

            // Use HTTP as the transport for SOAP calls
            HttpTransportSE transport = new HttpTransportSE(LOCATION);
            try {
                // Access the web service sending the generated envelope
                transport.call(NAME_SPACE + action, envelope);
                // Get the response from the web service (primitive type, just a String)
                response = (SoapPrimitive) envelope.getResponse();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            handler.post(() -> resultReceived(op, response != null ? response.toString() : null));
        }
    }
}
