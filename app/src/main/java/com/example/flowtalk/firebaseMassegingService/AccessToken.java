package com.example.flowtalk.firebaseMassegingService;

import android.util.Log;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.collect.Lists;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class AccessToken {

    private static final String TAG = "AccessToken";
    private static final String FIREBASE_MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

    public String getAccessToken() {
        try {
            // JSON string for service account credentials (Use securely)
            String jsonKey ="{\n" +
                    "  \"type\": \"service_account\",\n" +
                    "  \"project_id\": \"foodorder-9bd2b\",\n" +
                    "  \"private_key_id\": \"378f5bacdcd0eb777e8548729ca307e5559d419e\",\n" +
                    "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCooi5LqT5qMmWO\\nRkiQ/eNdO+vS42inmtut2gXZb2prn49o72BsuchGWLqLruDzIiTlG1SKGTiYPYT1\\n8WF+zFVEKf44BMlSJ1/SVJvGoQWj4aZIkFbTzOA7azYDjkytuQW6QGmcbPshKWgu\\n7Uyc6Wrdak8LCphmRrDTU2e/JYMid5i3y+SnmCzyD+xmw8Oa1o9kGIn3HLAtfxnD\\nTUuQY6q/RKqwWciD1w/klDzwKNcafKm285tCTbj2f0ZWKP0XkSwWeOaqlaxmj3s0\\nFvv7ouHV+uYA6Jh8xCDd7c65VENqrPySfqMcPnNth0kwHvO9Bi00f6RpK2veNlRS\\nD6JVU4q3AgMBAAECggEAOaPd0SzTUbdSMTRi33R4v8iYjef/QLLU/fK05yKHLKxI\\nd6TCv8cs5qazdMVczJ5SICGcXuvKBXeZt/s+yPmM95Sm+XzhV3xv61PSPy2Q4Phj\\nPxBfLVwtFQJLXyHHoeXJSGV18+roV+hwXQHtmwDJ707EH9EKUIM/M6yhnJt03FWY\\nXNMoOKzk8SwF43PfIVVR2wrgx1cCdmSKZiCcWQHKiWicvxO+8xOSlON+i1zwHbHZ\\ne/FdyMc4xpYtTYN6E3jXiimKAKirGyD8e2gPMR9G1rF+tq7rImkEJU1IrYe1iQuP\\nFtyh3dli3ws5oe5LtE8bQRBJb46tnuJE1oBX/6KlgQKBgQDgkMy1rYrUdD56jjVV\\ncOOMhNhoBg2Lm6wbIf7AH+1FkwVcAJvf/xm0n6KJecK4t+pThXY7RDLj+KPozn2L\\nGMyAjmN+3LdujjYKcTNa4cIUKoMGCzVl6hMhrDpnTlnVgvApIjPQTXxdhX6pdfjw\\nZFaGj0GEbSdga/PSw//3pJH8QQKBgQDAPRTQ+LDc3Kj3QG3DI/13OnvYo7pWZDEh\\nRbjjh/rXGM50GBX4/QaF2z7BaAc/tqX05a19f4n5mQ/bDy+pRfdL1TvKW4XQXdnW\\n1Q25M2cvuRBX6BrUTJcGpEAyqwcxlznQJ2sw2uc9y/z1pyfnIpOQ1rs+vTehAqeU\\nOyMZpU8o9wKBgQCrpyNDMB8xnJVxeqhb4gmdjKIaiZ3q/BiHOOoQk5b7mpD7LV/4\\nzXcpsOg963ujVLniE983GD9KMPGhb81Y+KDBF5YID9CKyluWicb2jiCa54/1m7W6\\nbD0pfQu7kZFrekX3OzSd4czRc8xI3mtl3VyiSHiNMB5sPrhGgBl1gy8vwQKBgHxi\\nCSn8/1KoY5s3UM3GZ6XRbXiK13I2eaNxnWN7BE5QeqSbQPxNmv7ZEQ3K5YXQBCkk\\nwtBEyV16L8wpU0lPP+cNHRadiUN8tgpO6AZAwC8cqvp174YV2FIvzGn8RzdLBdks\\n6CpAOQa/CflYInVm5dv9VEIP5AKF2TAL43Ygg8uZAoGALdnovgiJgU3p+5S5a5W2\\n5w2FnfZjFj5Y0pvmd8y7197jkk52XNaUfmUuL/Kb66WfIWwV41FE3LThlaje6vZa\\nznNcOz7J0bwAj2zQ37RjswSeGjJ2szcLjqF+1FOCr0TAXEUfDsR7SKTwfFVjOFks\\nbZxe5Qj/4nuja/3Lo3UgwLs=\\n-----END PRIVATE KEY-----\\n\",\n" +
                    "  \"client_email\": \"firebase-adminsdk-wvrhe@foodorder-9bd2b.iam.gserviceaccount.com\",\n" +
                    "  \"client_id\": \"115686052801034314336\",\n" +
                    "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
                    "  \"token_uri\": \"https://oauth2.googleapis.com/token\",\n" +
                    "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n" +
                    "  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-wvrhe%40foodorder-9bd2b.iam.gserviceaccount.com\",\n" +
                    "  \"universe_domain\": \"googleapis.com\"\n" +
                    "}\n";


            // Convert JSON string to InputStream
            InputStream stream = new ByteArrayInputStream(jsonKey.getBytes(StandardCharsets.UTF_8));

            // Load credentials from InputStream
            GoogleCredentials googleCredentials = GoogleCredentials.fromStream(stream)
                    .createScoped(Lists.newArrayList(FIREBASE_MESSAGING_SCOPE));

            // Refresh to obtain a new token
            googleCredentials.refresh();

            // Return the token value
            return googleCredentials.getAccessToken().getTokenValue();
        } catch (Exception e) {
            Log.e(TAG, "Error obtaining access token: ", e);
            return null;
        }
    }
}
