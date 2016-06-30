package com.rmnoon.workflowy.app.dialog;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.rmnoon.workflowy.app.R;
import com.rmnoon.workflowy.app.WFModel;
import com.rmnoon.workflowy.app.WorkflowyListWidget;
import com.rmnoon.workflowy.client.BadLoginException;

import java.io.IOException;

import static com.rmnoon.workflowy.app.AppWidgetUtils.fireEvent;

/**
 * Created by rmnoon on 6/13/2016.
 */
public class LoginDialogActivity extends Activity {

    private LoginTask activeLoginAttempt;
    private CheckBox showPasswordBox;
    private EditText usernameText, passwordText;

    private TextView statusText;
    private int defaultStatusTextColor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_login);

        activeLoginAttempt = null;

        usernameText = (EditText) findViewById(R.id.username_text);
        passwordText = (EditText) findViewById(R.id.password_text);

        TextWatcher validator = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                validateButton();
            }
        };
        usernameText.addTextChangedListener(validator);
        passwordText.addTextChangedListener(validator);

        showPasswordBox = (CheckBox) findViewById(R.id.show_password_checkbox);
        showPasswordBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setShowPassword(showPasswordBox.isChecked());
            }
        });

        statusText = (TextView) findViewById(R.id.login_status_text);
        defaultStatusTextColor = statusText.getCurrentTextColor();

        findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginDialogActivity.this.finish();
            }
        });

        findViewById(R.id.log_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activeLoginAttempt != null) return;
                activeLoginAttempt = new LoginTask();
                activeLoginAttempt.execute((Void) null);
            }
        });

        setStatus(null, false);
        setShowPassword(false);
        validateButton();
    }

    private void setShowPassword(boolean toShow) {
        passwordText.setInputType(InputType.TYPE_CLASS_TEXT | (toShow ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_TEXT_VARIATION_PASSWORD));
    }

    private void setStatus(String status, boolean inProgress) {
        findViewById(R.id.status_layout).setVisibility(status != null && !status.isEmpty() || inProgress ? View.VISIBLE : View.GONE);
        findViewById(R.id.progress_bar).setVisibility(inProgress ? View.VISIBLE : View.GONE);

        statusText.setText(status == null ? "" : status);
        statusText.setTextColor(inProgress ? defaultStatusTextColor : getResources().getColor(R.color.colorAccent));

        findViewById(R.id.log_in_button).setEnabled(!inProgress);
        findViewById(R.id.username_text).setEnabled(!inProgress);
        findViewById(R.id.password_text).setEnabled(!inProgress);
        showPasswordBox.setEnabled(!inProgress);
    }

    private void validateButton() {
        findViewById(R.id.log_in_button).setEnabled(!usernameText.getText().toString().isEmpty() && !passwordText.getText().toString().isEmpty());
    }

    private class LoginTask extends AsyncTask<Void, Void, String> {
        private String username, password;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            username = usernameText.getText().toString();
            password = passwordText.getText().toString();

            setStatus(getString(R.string.loading), true);
        }

        @Override
        protected String doInBackground(Void... params) {
            WFModel model = WFModel.getInstance(getApplicationContext());
            model.clearSession();
            model.setConfiguredCredentials(username, password);
            try {
                model.refresh();
            } catch (IOException e) {
                e.printStackTrace();
                model.setConfiguredCredentials(null, null);
                return getString(R.string.bad_network);
            } catch (BadLoginException e) {
                model.setConfiguredCredentials(null, null);
                return getString(R.string.bad_login);
            }
            return null; // good login
        }

        @Override
        protected void onPostExecute(String reason) {
            super.onPostExecute(reason);

            setStatus(reason, false);
            activeLoginAttempt = null;

            if (reason == null) { // if successful, close activity
                fireEvent(LoginDialogActivity.this, AppWidgetManager.INVALID_APPWIDGET_ID, WorkflowyListWidget.REDRAW_EVENT, WorkflowyListWidget.class, null, null);
                LoginDialogActivity.this.finish();
            }
        }
    }

}
