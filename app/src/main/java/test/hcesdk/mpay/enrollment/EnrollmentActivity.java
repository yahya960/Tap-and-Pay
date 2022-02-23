/*
 * ----------------------------------------------------------------------------
 *
 *     Copyright (c)  2015  -  GEMALTO DEVELOPEMENT - R&D
 *
 * -----------------------------------------------------------------------------
 * GEMALTO MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. GEMALTO SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING,
 * MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * THIS SOFTWARE IS NOT DESIGNED OR INTENDED FOR USE OR RESALE AS ON-LINE
 * CONTROL EQUIPMENT IN HAZARDOUS ENVIRONMENTS REQUIRING FAIL-SAFE
 * PERFORMANCE, SUCH AS IN THE OPERATION OF NUCLEAR FACILITIES, AIRCRAFT
 * NAVIGATION OR COMMUNICATION SYSTEMS, AIR TRAFFIC CONTROL, DIRECT LIFE
 * SUPPORT MACHINES, OR WEAPONS SYSTEMS, IN WHICH THE FAILURE OF THE
 * SOFTWARE COULD LEAD DIRECTLY TO DEATH, PERSONAL INJURY, OR SEVERE
 * PHYSICAL OR ENVIRONMENTAL DAMAGE ("HIGH RISK ACTIVITIES"). GEMALTO
 * SPECIFICALLY DISCLAIMS ANY EXPRESS OR IMPLIED WARRANTY OF FITNESS FOR
 * HIGH RISK ACTIVITIES.
 *
 * -----------------------------------------------------------------------------
 */

package test.hcesdk.mpay.enrollment;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;


//import com.gemalto.mfs.mwsdk.dcm.DCMSecureStorageManager;
//import com.gemalto.mfs.mwsdk.dcm.exception.DCMSecureStorageException;
import com.gemalto.mfs.mwsdk.provisioning.ProvisioningServiceManager;
import com.gemalto.mfs.mwsdk.provisioning.listener.EnrollingServiceListener;
import com.gemalto.mfs.mwsdk.provisioning.model.EnrollmentStatus;
import com.gemalto.mfs.mwsdk.provisioning.model.KnownCpsErrorCodes;
import com.gemalto.mfs.mwsdk.provisioning.model.ProvisioningServiceCodeType;
import com.gemalto.mfs.mwsdk.provisioning.model.ProvisioningServiceError;
import com.gemalto.mfs.mwsdk.provisioning.model.ProvisioningServicePinType;
import com.gemalto.mfs.mwsdk.provisioning.sdkconfig.EnrollingBusinessService;
import com.gemalto.mfs.mwsdk.utils.chcodeverifier.CHCodeVerifier;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.iid.FirebaseInstanceId;


import test.hcesdk.mpay.R;
import test.hcesdk.mpay.util.Constants;
import test.hcesdk.mpay.util.DialogHelper;
import test.hcesdk.mpay.util.SecureKeyBoardHelper;
import test.hcesdk.mpay.util.Tags;
import test.hcesdk.mpay.util.TypeNotification;
import test.hcesdk.mpay.util.Util;


/**
 * GCM Registration/Enrollment
 */
public class EnrollmentActivity extends AppCompatActivity implements  OnClickListener { // NOPMD

	/**
	 * Tag for logging purpose
	 */
	private final static String TAG = EnrollmentActivity.class.getName();

	// UI elements

	/**
	 * Enrollment title
	 */
	private static TextView enrollmentTxt;

	/**
	 * Used to input senderid/userid/activation code/pin
	 */
	private static EditText inputEditText;
	private static Button startEnrollBtn;// NOPMD
	private static ProgressBar loadingBar;// NOPMD

	/**
	 * The text view that will show the guide for user to input
	 * projectID/username/activationcode/pin
	 */
	private static TextView inputHintTxt;

	/**
	 * Toggle button used to decide wether to use pin or not for enrollment
	 */
	private static ToggleButton usePinTBtn;

	/**
	 * Title to indicate use pin or not
	 */
	private static TextView usePinTxtView;

	// Service and listeners
//	private static GcmRegistrationListener gcmListener;// NOPMD
	private static EnrollingBusinessService enrollingBS;// NOPMD
//	private static GcmRegistrationBusinessService gcmRegistrationBS;// NOPMD

	/**
	 * Hard coded value for gcm token. Internal use only
	 */
	// private static String gcmTokenValue =
	// "APA91bHE4oOLshqedkg2aU0apeDWFAQ9kVUDBkJEeYI0GIhAjJcgZX0lFdCVFH7pk91B1Xuk398-g6DwSm5W9nUyfZg6rMpVEU1tFCzVWw3RF-amK3mOVjB6M-O0xKryCvJhB7oBmA1g";

	private static String tokenValue = "";
	private TypeNotification typeNotification;

	/**
	 * Default userid value
	 */
	private static String userID = Constants.TAG_USER_ID;

	/**
	 * Activation code input using normal keypad
	 */
	private static String actCodeWithKeypad;

	/**
	 * Default pin type is none
	 */
	private static ProvisioningServicePinType pinType = ProvisioningServicePinType.NONE;

	/**
	 * UI status, since one xml file is used for gcm/enter userid/activation
	 * code/input pin
	 *
	 */
	enum OperationStatus {
		ENTER_PROJECT_SENDER_ID, ENTER_USERID, ENTER_ACTIVATION_CODE, ENTER_MOBILE_PIN
	}

	/**
	 * Initial operation status.
	 */
	private static OperationStatus opStaus = OperationStatus.ENTER_PROJECT_SENDER_ID;

	/**
	 * Load EnrolLment UI
	 */


	private String GBC_MAC3_DIV = new String(Base64.encode(Util.hexStringToByteArray("d00b063876877e9d68e100a50b479104"), Base64.DEFAULT)); // added
	private String ZFS_WBC_AES_DIV = new String(Base64.encode(Util.hexStringToByteArray("d00b063876877e9d68e100a50b479104"),Base64.DEFAULT));

	@Override
	protected void onCreate(final Bundle savedState) {
		super.onCreate(savedState);

		// Loading UI
		setContentView(R.layout.activity_enrollment);
		enrollmentTxt = (TextView) findViewById(R.id.enrollText);
		inputEditText = (EditText) findViewById(R.id.projectIdEditText);

		loadingBar = (ProgressBar) findViewById(R.id.progressBar);
		usePinTBtn = (ToggleButton) findViewById(R.id.useMobilePinTBtn);
		usePinTxtView = (TextView) findViewById(R.id.useMobilePinTxt);
		inputHintTxt = (TextView) findViewById(R.id.projectIdTxt);

		usePinTBtn.setOnClickListener(this);

		inputEditText.setHint(Constants.CPS_SENDER_ID);
		inputEditText.setOnClickListener(this);
		startEnrollBtn = (Button) findViewById(R.id.startEnrolBtn);

		// set listeners
		startEnrollBtn.setOnClickListener(this);

		// get services
		//EnrollingBusinessService enrollingService = ProvisioningServiceManager.getEnrollingBusinessService();
		enrollingBS = ProvisioningServiceManager.getEnrollingBusinessService();


}

	public void enrollment(final String userid, final String token, final String userLanguage, // NOPMD
						   final ProvisioningServicePinType pinType) {
		Log.d(TAG, "[Start to call Enrol]");
		Log.d(TAG, "GCM token: " + token);
		Log.d(TAG, "userLanguage: " + userLanguage);
		Log.d(TAG, "pinType: " + pinType);

		enrollmentTxt.setText("Start enrollment");

		final EnrollingServiceListener enrolListener = new EnrollingServiceListener() {

			@Override
			public void onError(final ProvisioningServiceError error) { // NOPMD
				loadingBar.setVisibility(View.INVISIBLE);

				if (error.getCpsErrorCode() == KnownCpsErrorCodes.MOBILE_WALLET_UPDATE_REQUIRED) {
					DialogHelper.createUpdateApplicationDialog(EnrollmentActivity.this).show();
				} else {
					Toast.makeText(
							getApplicationContext(),
							"[onEnrollmentError]: \nCPSErrorCode: " + error.getCpsErrorCode() + "\nHttpStatusCode: "
									+ error.getHttpStatusCode() + "\nErrorMessage: " + error.getErrorMessage()
									+ "\nSdkErrorCode:" + error.getSdkErrorCode(), Toast.LENGTH_LONG).show();

					Log.d(TAG, "[onEnrollmentError]: \nCPSErrorCode: " + error.getCpsErrorCode() + "\nHttpStatusCode: "
							+ error.getHttpStatusCode() + "\nErrorMessage: " + error.getErrorMessage() + "\nSdkErrorCode:"
							+ error.getSdkErrorCode());

					opStaus = OperationStatus.ENTER_USERID;

					enrollmentTxt.setText("Start enrollment");
					showCPSErrorMessageDialog();
					displayEnterUserIDScreen();
				}
			}

			@Override
			public void onComplete() { // NOPMD

				// remove the progress bar
				loadingBar.setVisibility(View.INVISIBLE);
				usePinTBtn.setVisibility(View.INVISIBLE);
				usePinTxtView.setVisibility(View.INVISIBLE);

				enrollmentTxt.setText("Enrollment Finished.\nInstalling the first card");
				Toast.makeText(getApplicationContext(), "[Enrollment Success]", Toast.LENGTH_SHORT).show();

				Log.d(TAG, "[Enrollment onComplete]");

				opStaus = OperationStatus.ENTER_PROJECT_SENDER_ID;

				finish();
			}

			@Override
			public void onCodeRequired(final CHCodeVerifier verifier) { // NOPMD

				Log.d(TAG, "[Enrollment onCodeRequired]");

				inputCodeForEnrolment(verifier);
			}

			@Override
			public void onStarted() { // NOPMD
				Log.d(TAG, "[Enrollment onStarted]");

				loadingBar.setVisibility(View.VISIBLE);
				loadingBar.setIndeterminate(true);
			}
		};



		final EnrollmentStatus status = enrollingBS.isEnrolled();
		Log.d(TAG, "EnrollmentStatus: " + status);
		if (status == EnrollmentStatus.ENROLLMENT_NEEDED) {

			if (TextUtils.isEmpty(userid)
					|| TextUtils.isEmpty(userLanguage)
					|| TextUtils.isEmpty(token)
					|| enrolListener == null
					|| pinType == null) {
				String errorMsg = String.format("Enrollment ERROR: Some params is NULL userid=%s, token=%s," +
						" userLanguage=%s. Check WIFI connection to make sure GCM message is received", userid, token, userLanguage);
				Log.e(TAG, errorMsg + " enrolListener=" + enrolListener + ", enrolListener=" + pinType);
				Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
			} else {
				enrollingBS.enroll(userid, token, userLanguage, enrolListener, pinType);
			}
		} else if (status == EnrollmentStatus.ENROLLMENT_IN_PROGRESS) {
			Log.d(TAG, "userLanguage: " + userLanguage);

			Log.d(TAG, "enrollingServiceListener: " + enrolListener);
			enrollingBS.continueEnrollment(userLanguage, enrolListener, pinType);
		}

	}

	private void showCPSErrorMessageDialog(){
		new AlertDialog.Builder(this)
				.setTitle(R.string.cps_enroll_error)
				.setMessage(R.string.msg_retry_activation_code)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// no actions
					}
				})
				// A null listener allows the button to dismiss the dialog and take no further action.
				.setIcon(android.R.drawable.ic_dialog_alert)
				.show();
	}

	private boolean checkGooglePlayService(Context context) {
		int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
		if (resultCode == ConnectionResult.SUCCESS) {
			return true;
		}
		return false;
	}

	private boolean checkHuaweiApiService(Context context) {
	/*	if (HuaweiApiAvailability.getInstance().isHuaweiMobileServicesAvailable(context) == ConnectionResult.SUCCESS) {
			return true;
		}
		return false;*/
	return false;
	}

	@Override
	public void onClick(final View view) { // NOPMD
		Log.i(TAG, "operation status: " + opStaus);

		switch (view.getId()) {
			case R.id.startEnrolBtn:
				if (opStaus == OperationStatus.ENTER_PROJECT_SENDER_ID) {
					// GCM registration flow
					// GCM registration is before enter user id
					enrollmentTxt.setText("Start enrollment");

					final String projectSenderID = configureProjectID();
					Log.d(TAG, "Project sender id: " + projectSenderID);
//					gcmRegistrationBS.registerIfNecessary(projectSenderID, gcmListener);
					if (checkGooglePlayService(getApplicationContext())) {
						typeNotification = TypeNotification.FIREBASE;
					} else if (checkHuaweiApiService(getApplicationContext())) {
						typeNotification = TypeNotification.HUAWEI;
					}
					//new NotificationRegistration().execute(projectSenderID); // comment for this project but if need to get HUAWEI pushnoti, it will use back for future
					getFirebaseTokenandDisplayUserScreen();

					loadingBar.setVisibility(View.VISIBLE);
					loadingBar.setIndeterminate(true);

				} else if (opStaus == OperationStatus.ENTER_USERID) {
					// enter user id flow
					// do the enrollment
					userID = inputEditText.getText().toString();
					Log.d(TAG, "user id: " + userID);

					displayEnterActivationCodeScreen();

					opStaus = OperationStatus.ENTER_ACTIVATION_CODE;

				} else if (opStaus == OperationStatus.ENTER_ACTIVATION_CODE) {

					Log.i(TAG, "Button is clicked, and opStatus is ENTER_ACTIVATION_CODE");

					//Before enroll time , when user enter activation code stage-> download the tables
				/*TableDownloadHelper tableDownloadHelper = new TableDownloadHelper();
					tableDownloadHelper.downloadTablestoFileDest(EnrollmentActivity.this);

					try {
						DCMSecureStorageManager.setGreyBoxDiversifier(GBC_MAC3_DIV);
						Log.e("greybox","success");
					} catch (DCMSecureStorageException e) {
						e.printStackTrace();
					}
					try {
						DCMSecureStorageManager.setWhiteBoxDiversifier(ZFS_WBC_AES_DIV);
						Log.e("wbox","success");
					} catch (DCMSecureStorageException e) {
						e.printStackTrace();
					}*/
					//////
					if (Constants.USE_SECURE_KEYPAD) {
						enrollment(userID, tokenValue, "UserLanguage", pinType);
					} else {

						// get the activation code input
						actCodeWithKeypad = inputEditText.getText().toString().trim();
						Log.d(TAG, "Input with normal keypad: " + actCodeWithKeypad);

						// check if input is empty
						if (TextUtils.isEmpty(actCodeWithKeypad)) {
							Toast.makeText(getApplicationContext(), "Input cannot be empty", Toast.LENGTH_LONG).show();
						} else {

							if (usePinTBtn.isChecked()) {

								// set the edittext to be empty to allow user to
								// input mobile pin
								inputEditText.setText("");
								inputHintTxt.setText("Please enter the Mobile PIN:");

								enrollmentTxt.setText("Input your mobile pin");

								opStaus = OperationStatus.ENTER_MOBILE_PIN;
							} else {
								enrollment(userID, tokenValue, Tags.USERLANGUAGE, pinType);
							}

						}

					}

				} else if (opStaus == OperationStatus.ENTER_MOBILE_PIN) {

					if (Constants.USE_SECURE_KEYPAD) {
						// ignore..

					} else {
						Log.i(TAG, "Start to enrol now with normal keypad");
						enrollment(userID, tokenValue, Tags.USERLANGUAGE, pinType);
					}

				}
				break;

			case R.id.projectIdEditText:

				if (opStaus == OperationStatus.ENTER_ACTIVATION_CODE) {
					Log.i(TAG, "EditText is clicked, and opStatus is ENTER_ACTIVATION_CODE");

					// if use secure keypad, after enter userid the secure keypad is
					// triggered
					if (Constants.USE_SECURE_KEYPAD) {
						enrollment(userID, tokenValue, Tags.USERLANGUAGE, pinType);
					} else {
						inputEditText.setInputType(InputType.TYPE_NUMBER_VARIATION_PASSWORD);
					}

				} else {
					Log.i(TAG, "EditText is clicked, and opStatus is: " + opStaus);
				}

				break;

			case R.id.useMobilePinTBtn:
				if (usePinTBtn.isChecked()) {
					Log.d(TAG, "Use mobile pin");

					pinType = ProvisioningServicePinType.MOBILE_PIN;
				} else {
					Log.d(TAG, "Do not use mobile pin");
					pinType = ProvisioningServicePinType.NONE;
				}

				break;
			default:
				break;
		}

	}

	private void displayEnterUserIDScreen() {
		Log.i(TAG, "[displayEnterUserIDScreen]");

		usePinTBtn.setVisibility(View.INVISIBLE);
		usePinTxtView.setVisibility(View.INVISIBLE);
		inputHintTxt.setText("Please enter the UserID:");
		inputEditText.setText(Constants.USERID);

		startEnrollBtn.setText("OK");

	}

	private void displayEnterActivationCodeScreen() {
		Log.i(TAG, "[displayEnterActivationCodeScreen]");

		usePinTBtn.setVisibility(View.VISIBLE);
		usePinTxtView.setVisibility(View.VISIBLE);
		inputHintTxt.setText("Please enter the activation code:");
		inputEditText.setText("");
		inputEditText.setHint("");

		startEnrollBtn.setText("OK");

	}



	private void inputCodeForEnrolment(final CHCodeVerifier chCodeVerifier) {

		runOnUiThread(new Runnable() { // NOPMD This is not a webapp
			public void run() { // NOPMD

				if (enrollingBS.getCodeType() == ProvisioningServiceCodeType.ACTIVATION_CODE) {

					inputHintTxt.setText("Please enter the activation code:");
					enrollmentTxt.setText("Input your Activation Code");

					// use securekeypad
					if (Constants.USE_SECURE_KEYPAD) {
						SecureKeyBoardHelper.inputCodeWithSecureKeypad(chCodeVerifier, "Please enter Activation Code",getSupportFragmentManager());
					} else {

						chCodeVerifier.inputCode(actCodeWithKeypad);
					}

				} else if (enrollingBS.getCodeType() == ProvisioningServiceCodeType.MOBILE_PIN
						&& usePinTBtn.isChecked()) {
					Log.d(TAG, "Enter mobile pin!");
					opStaus = OperationStatus.ENTER_MOBILE_PIN;
					// use securekeypad
					if (Constants.USE_SECURE_KEYPAD) {

						inputHintTxt.setText("Please enter the Mobile PIN:");
						// enter mobile pin
						enrollmentTxt.setText("Input your mobile pin");

						SecureKeyBoardHelper.inputCodeWithSecureKeypad(chCodeVerifier, "Please enter Mobile PIN",getSupportFragmentManager());
					} else {

						inputCodeWithNormalKeypad(chCodeVerifier);
					}
				}

			}
		});

	}

	private void inputCodeWithNormalKeypad(final CHCodeVerifier chCodeVerifier) {

		// use normal keypad
		final String codeInput = inputEditText.getText().toString().trim();
		Log.d(TAG, "Input with normal keypad: " + codeInput);

		if (TextUtils.isEmpty(codeInput)) {
			Toast.makeText(getApplicationContext(), "Input cannot be empty", Toast.LENGTH_LONG).show();
		} else {
			chCodeVerifier.inputCode(codeInput);
		}

	}

	private String configureProjectID() {

		String projectID = inputEditText.getText().toString();
		if (projectID == null || projectID.length() == 0) {
			Log.d(TAG, "Use default project ID");
			projectID = Constants.CPS_SENDER_ID;
		} else {
			Log.d(TAG, "Use custom project ID");

		}

		return projectID;
	}

	public void getFirebaseTokenandDisplayUserScreen()
	{
		String tokenId = null;
		if(typeNotification == TypeNotification.FIREBASE)
		{
			tokenId = FirebaseInstanceId.getInstance().getToken();
			Log.d(TAG, "tokenId:" + tokenId);
			if (tokenId == null) {
				throw new RuntimeException("Firebase token is null ");
			}
			else {
				displayUserScreen(tokenId);
			}
		}

	}

	private class NotificationRegistration extends AsyncTask<String, Integer, String> {

		@Override
		protected String doInBackground(String... params) {
			String tokenId = null;
			//try {
				if (typeNotification == TypeNotification.FIREBASE) {
					/*String appID = "1:" + params[0] + Constants.MOBILESDK_APP_ID_LAST_PART;
					Log.d(TAG, "Application ID : " + appID);
					Log.d(TAG, "Sender ID : " + params[0]);
					FirebaseApp.initializeApp(EnrollmentActivity.this, new FirebaseOptions.Builder().
							setApplicationId(appID).
							setGcmSenderId(params[0]).
							build());*/

					//tokenId = FirebaseInstanceId.getInstance().getToken(params[0], "FCM");
					tokenId = FirebaseInstanceId.getInstance().getToken();
					Log.d(TAG, "tokenId:" + tokenId);
				} else {
					/*tokenId = CPSHmsPushKitMessagingService.PREFIX_HMS_TOKEN +
							HmsInstanceId.getInstance(getApplicationContext()).getToken(
									AGConnectServicesConfig.fromContext(getApplicationContext()).
											getString("client/app_id"), HmsMessaging.DEFAULT_TOKEN_SCOPE);*/
					Log.d(TAG, "tokenId:" + tokenId);
				}
			//}
			/*catch (IOException | ApiException e) {
				Log.d(TAG, e.getMessage());
			}*/
			return tokenId;
		}

		@Override
		protected void onPostExecute(String tokenId) {
			displayUserScreen(tokenId);
		}
	}

	private void displayUserScreen(String tokenId) {
		Log.d(TAG, "ONCOMPLETE -- Notification Token:" + tokenId);
		loadingBar.setVisibility(View.INVISIBLE);
		opStaus = OperationStatus.ENTER_USERID;
		tokenValue = tokenId;
		displayEnterUserIDScreen();
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
		opStaus = OperationStatus.ENTER_PROJECT_SENDER_ID;
	}
}
