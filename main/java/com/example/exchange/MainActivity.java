package com.example.exchange;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Spinner fromCurrencySpinner, toCurrencySpinner;
    private EditText amountEditText;
    private Button convertButton;
    private TextView resultTextView;
    private ProgressBar progressBar;

    private RequestQueue requestQueue;
    private List<String> currencyCodes = new ArrayList<>();
    private JSONObject exchangeRates;

    // API Key - Use your own API key from https://freecurrencyapi.com/
    private static final String API_KEY = "YOUR_API_KEY";
    private static final String API_URL = "https://api.freecurrencyapi.com/v1/latest?apikey=" + API_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Volley request queue
        requestQueue = Volley.newRequestQueue(this);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Currency Exchange");

        // Initialize UI elements
        fromCurrencySpinner = findViewById(R.id.fromCurrencySpinner);
        toCurrencySpinner = findViewById(R.id.toCurrencySpinner);
        amountEditText = findViewById(R.id.amountEditText);
        convertButton = findViewById(R.id.convertButton);
        resultTextView = findViewById(R.id.resultTextView);
        progressBar = findViewById(R.id.progressBar);

        // Fetch exchange rates when app starts
        fetchExchangeRates();

        // Set up convert button click listener
        convertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                convertCurrency();
            }
        });
    }

    private void fetchExchangeRates() {
        progressBar.setVisibility(View.VISIBLE);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                API_URL,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // Parse the response
                            exchangeRates = response.getJSONObject("data");

                            // Add USD (base currency) and all available currencies to the list
                            currencyCodes.add("USD"); // Base currency

                            Iterator<String> keys = exchangeRates.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                if (!currencyCodes.contains(key)) {
                                    currencyCodes.add(key);
                                }
                            }

                            // Set up currency spinners with the fetched data
                            setupCurrencySpinners();
                            progressBar.setVisibility(View.GONE);

                        } catch (JSONException e) {
                            handleError("Error parsing API response");
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        handleError("Network error: " + error.getMessage());
                        error.printStackTrace();
                    }
                }
        );

        requestQueue.add(request);
    }

    private void setupCurrencySpinners() {
        // Create adapter for the spinners
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                currencyCodes
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Set the adapters to the spinners
        fromCurrencySpinner.setAdapter(adapter);
        toCurrencySpinner.setAdapter(adapter);

        // Set default selections (USD -> EUR)
        fromCurrencySpinner.setSelection(currencyCodes.indexOf("USD"));
        if (currencyCodes.contains("EUR")) {
            toCurrencySpinner.setSelection(currencyCodes.indexOf("EUR"));
        }
    }

    private void convertCurrency() {
        // Get user input
        String fromCurrency = fromCurrencySpinner.getSelectedItem().toString();
        String toCurrency = toCurrencySpinner.getSelectedItem().toString();

        // Validate amount input
        String amountStr = amountEditText.getText().toString();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if we have exchange rates data
        if (exchangeRates == null) {
            Toast.makeText(this, "Exchange rates not available yet", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double result;

            // Calculate conversion based on rates
            if (fromCurrency.equals("USD")) {
                // Direct conversion from USD to target currency
                double exchangeRate = exchangeRates.getDouble(toCurrency);
                result = amount * exchangeRate;
            } else if (toCurrency.equals("USD")) {
                // Direct conversion from source currency to USD
                double exchangeRate = exchangeRates.getDouble(fromCurrency);
                result = amount / exchangeRate;
            } else {
                // Cross-conversion: first to USD, then to target currency
                double fromRate = exchangeRates.getDouble(fromCurrency);
                double toRate = exchangeRates.getDouble(toCurrency);

                double amountInUSD = amount / fromRate;
                result = amountInUSD * toRate;
            }

            // Format and display the result
            DecimalFormat df = new DecimalFormat("#.##");
            String formattedResult = df.format(result);

            resultTextView.setText(amount + " " + fromCurrency + " = " + formattedResult + " " + toCurrency);

        } catch (JSONException e) {
            Toast.makeText(this, "Error calculating conversion", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void handleError(String message) {
        progressBar.setVisibility(View.GONE);
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
        resultTextView.setText("Error: Unable to fetch exchange rates");
    }
}