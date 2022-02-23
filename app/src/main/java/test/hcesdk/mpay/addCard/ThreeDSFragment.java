package test.hcesdk.mpay.addCard;


import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.PendingCardActivation;
import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.ThreeDSecure;

import test.hcesdk.mpay.R;
import test.hcesdk.mpay.util.AppLogger;
import test.hcesdk.mpay.util.Web3dsJavascriptInterface;

public class ThreeDSFragment extends Fragment {

    private static final String TAG = "ThreeDSFragment";
    WebView web3DSWeb;

    PendingCardActivation pendingCardActivation;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_3ds, container, false);
        web3DSWeb = view.findViewById(R.id.webview);

        web3DSWeb.getSettings().setJavaScriptEnabled(true);
        web3DSWeb.addJavascriptInterface(new Web3dsJavascriptInterface() {
            @android.webkit.JavascriptInterface
            public void onActivationFinished(final int resultCode) {
                AppLogger.d(TAG, "WLMPA - onActivationFinished: " + resultCode);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // layout visibility & content management
                        web3DSWeb.setVisibility(View.GONE);
                        pendingCardActivation.activate(resultCode, (AddCardActivity) getActivity());
                    }
                });
            }
        }, "Web3DSInterface");
        // log document loading & errors
        web3DSWeb.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                AppLogger.d(TAG, "3DS page: \"" + url + "\" started...");

                toggleProgress(true);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                AppLogger.d(TAG, "3DS page: \"" + url + "\" finished...");

                toggleProgress(false);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                String errorDetail = "Receive Error: ";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    errorDetail += "[" + error.getErrorCode() + "] " + error.getDescription();
                } else {
                    errorDetail += error.toString();
                }
                AppLogger.d(TAG, "3DS \"" + errorDetail + "\" !");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    errorDetail += "\n" + request.getUrl();
                    AppLogger.d(TAG, "3DS Error URL: \"" + request.getUrl() + "\n");

                    if (request.getUrl().toString().toLowerCase().contains("/favicon.ico")) {
                        // ignore error
                        AppLogger.d(TAG, "3DS Error URL is ignored!");

                        return;
                    }
                }
                // display error is specific fragment
                Toast.makeText(getActivity(), errorDetail, Toast.LENGTH_SHORT).show();
                getActivity().onBackPressed();
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                String errorDetail = "HTTP Error: ";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    errorDetail += "[" + errorResponse.getStatusCode() + "] " + errorResponse.getReasonPhrase();
                } else {
                    errorDetail += errorResponse.toString();
                }
                AppLogger.d(TAG, "3DS \"" + errorDetail + "\" !");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    errorDetail += "\n" + request.getUrl();
                    AppLogger.d(TAG, "3DS HTTP Error URL: \"" + request.getUrl() + "\n");

                    return;
                }

                // display error is specific fragment
                Toast.makeText(getActivity(), errorDetail, Toast.LENGTH_SHORT).show();
                getActivity().onBackPressed();
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                // workaround for favicon error
                if (url.toLowerCase().contains("/favicon.ico")) {
                    AppLogger.d(TAG, "3DS Interceptor: Ignore favicon.ico request");

                    try {
                        return new WebResourceResponse("image/png", null, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }

            @Override
            @SuppressLint("NewApi")
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // workaround for favicon error
                if (!request.isForMainFrame() && request.getUrl().getPath().endsWith("/favicon.ico")) {
                    AppLogger.d(TAG, "3DS Interceptor: Ignore favicon.ico request");

                    try {
                        return new WebResourceResponse("image/png", null, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                String errorDetail = error.getUrl() + "SSL Error: ";
                switch (error.getPrimaryError()) {
                    case SslError.SSL_EXPIRED: // 1
                        errorDetail += "The certificate has expired";
                        break;

                    case SslError.SSL_IDMISMATCH: // 2
                        errorDetail += "Hostname mismatch";
                        break;

                    case SslError.SSL_UNTRUSTED: // 3
                        errorDetail += "The certificate authority is not trusted";
                        SslCertificate certificate = error.getCertificate();
                        SslCertificate.DName detail = certificate.getIssuedBy();
                        errorDetail += "\nCName=" + detail.getCName() + " DName=" + detail.getDName() + "OName=" + detail.getOName() + "UName=" + detail.getUName();
                        break;

                    case SslError.SSL_DATE_INVALID: // 4
                        errorDetail += "The date of the certificate is invalid";
                        break;

                    case SslError.SSL_INVALID: // 5
                        errorDetail += "A generic error occurred";
                        break;

                    default:
                        errorDetail += "Other error";
                }
                AppLogger.d(TAG, "3DS \"" + errorDetail + "\" !");
                AppLogger.d(TAG, "3DS SSL Proceed !");
                handler.proceed();
            }

        });


        // retrieve launch arguments
        ThreeDSecure threeDSecure = pendingCardActivation.getThreeDSecure();
        if (threeDSecure != null) {
            String acsMethod = threeDSecure.getAcsMethod();
            String acsUrl = threeDSecure.getAcsUrl();
            String acsQuery = threeDSecure.getAcsQuery();
            AppLogger.d(TAG, "Launch arguments: " + acsMethod + " " + acsUrl + " " + acsQuery);

            // checks ACS method
            if ("GET".equalsIgnoreCase(acsMethod)) {
                String acsUri = acsUrl + "?" + acsQuery;
                web3DSWeb.loadUrl(acsUri);
            } else if ("POST".equalsIgnoreCase(acsMethod)) {
                web3DSWeb.postUrl(acsUrl, acsQuery.getBytes());
            } else {
                AppLogger.e(TAG, "ACS access method is incorrect!");
                getActivity().onBackPressed();
            }
        } else {
            AppLogger.e(TAG, "Launch arguments missing!");
            getActivity().onBackPressed();
        }
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        toggleProgress(false);
    }


    private void toggleProgress(boolean show) {
        if (getActivity() != null) {
            ((AddCardActivity) getActivity()).toggleProgress(show);
        }
    }


}
