/*
 * Copyright (c) 2016. David de Andrés and Juan Carlos Ruiz, DISCA - UPV, Development of apps for mobile devices.
 */

package labs.dadm.l0503_accesssoapwebservice;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.ref.WeakReference;

/*
 * Converts a given temperature from Celsius degrees into Fahrenheit degrees (or viceversa)
 * using the SOAP web service located at: http://www.w3schools.com/xml/tempconvert.asmx
 * */
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
    }

    /*
     * Handles the event to convert the temperature
     * */
    public void convertTemperature(View v) {

        if (isConnected()) {
            // Create the AsyncTask in charge of accessing the web service in background
            TemperatureAsyncTask task = new TemperatureAsyncTask(this);

            switch (v.getId()) {

                // Convert Celsius degrees into Fahrenheit degrees
                case R.id.bC2F:

                    // Check that something has been entered as temperature
                    if (!etCelsius.getText().toString().isEmpty()) {
                        task.execute(CELSIUS_TO_FAHRENHEIT, etCelsius.getText().toString());
                    }
                    // Notify the user that it is required to enter a temperature
                    else {
                        Toast.makeText(SoapTemperatureActivity.this, R.string.not_data, Toast.LENGTH_SHORT).show();
                    }
                    break;

                // Convert Fahrenheit degrees into Celsius degrees
                case R.id.bF2C:

                    // Check that something has been entered as temperature
                    if (!etFahrenheit.getText().toString().isEmpty()) {
                        task.execute(FAHRENHEIT_TO_CELSIUS, etFahrenheit.getText().toString());
                    }
                    // Notify the user that it is required to enter a temperature
                    else {
                        Toast.makeText(SoapTemperatureActivity.this, R.string.not_data, Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
        // Notify the user that the device has not got Internet connection
        else {
            Toast.makeText(SoapTemperatureActivity.this, R.string.not_connected, Toast.LENGTH_SHORT).show();
        }
    }

    /*
     * Determines whether the device has got Internet connection.
     * */
    public boolean isConnected() {
        // Get a reference to the ConnectivityManager
        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        // Get information about the default active data network
        NetworkInfo info = manager.getActiveNetworkInfo();
        // There will be connectivity when there is a default connected network
        return ((info != null) && (info.isConnected()));
    }

    /*
     * Accesses a SOAP web service to convert the given temperature to other units.
     * Input parameters are: an int identifying the operation to perform,
     * and a String representing the temperature in the original units.
     * The output parameter is a String representing the temperature in the target units.
     * */
    private static class TemperatureAsyncTask extends AsyncTask<Object, Void, SoapPrimitive> {

        WeakReference<SoapTemperatureActivity> activity;

        // Constants related to the web service
        private final static String NAME_SPACE = "https://www.w3schools.com/xml/";
        private final static String LOCATION = "https://www.w3schools.com/xml/tempconvert.asmx";
        private final static String ACTION_TO_FAHRENHEIT = "CelsiusToFahrenheit";
        private final static String ACTION_TO_CELSIUS = "FahrenheitToCelsius";
        private final static String CELSIUS = "Celsius";
        private final static String FAHRENHEIT = "Fahrenheit";

        // Operation to perform
        int op;

        TemperatureAsyncTask(SoapTemperatureActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        /*
         * Updates the UI before starting the background task
         * */
        @Override
        protected void onPreExecute() {
            // Displays an indeterminate ProgressBar to show that an operation is in progress
            this.activity.get().pbTemperature.setVisibility(View.VISIBLE);
            // Disables the buttons so the user cannot launch multiple requests
            this.activity.get().bC2F.setEnabled(false);
            this.activity.get().bF2C.setEnabled(false);
        }

        /*
         * Accesses the web service on background convert the given temperature to other units.
         * */
        @Override
        protected SoapPrimitive doInBackground(Object... params) {

            // Property Name and Action for the SOAP web service
            String propertyName;
            String action;

            // Get the operation to perform
            op = (Integer) params[0];
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
            request.addProperty(propertyName, params[1]);

            // Create the envelope that will transport the SOAP object (SOAP v1.2)
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER12);
            envelope.dotNet = true;
            // Include the SOAP object within the envelope
            envelope.setOutputSoapObject(request);

            SoapPrimitive response = null;
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

            return response;
        }

        /*
         * Updates the UI according to the received response
         * */
        @Override
        protected void onPostExecute(SoapPrimitive result) {

            // Check whether the web services returned a response
            if (result != null) {
                // Update the temperature according to the requested operation
                if (op == CELSIUS_TO_FAHRENHEIT) {
                    this.activity.get().etFahrenheit.setText(result.toString());
                } else {
                    this.activity.get().etCelsius.setText(result.toString());
                }
            }
            // Notify the user that the request could not be completed
            else {
                Toast.makeText(this.activity.get(), R.string.not_completed, Toast.LENGTH_SHORT).show();
            }

            // Hides the indeterminate ProgressBar as the operation has finished
            this.activity.get().pbTemperature.setVisibility(View.INVISIBLE);
            // Enables the buttons so the user cannot launch another request
            this.activity.get().bC2F.setEnabled(true);
            this.activity.get().bF2C.setEnabled(true);
        }


    }
}
