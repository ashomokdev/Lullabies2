package com.ashomok.lullabies;

import android.content.Context;

import java.util.LinkedHashMap;

/**
 * Created by iuliia on 12/12/16.
 */


public class Settings {

    public static boolean isTestMode = false; //todo undo in prod

    public static boolean isAdsActive = true; //will be set in MainActivity

    /* BASE_64_ENCODED_PUBLIC_KEY should be YOUR APPLICATION'S PUBLIC KEY
     * (that you got from the Google Play developer console). This is not your
     * developer public key, it's the *app-specific* public key.
     *
     * Instead of just storing the entire literal string here embedded in the
     * program,  construct the key at runtime from pieces or
     * use bit manipulation (for example, XOR with some other string) to hide
     * the actual key.  The key itself is not secret information, but we don't
     * want to make it easy for an attacker to replace the public key with one
     * of their own and then fake messages from the server.
     */
    public static final String BASE_64_ENCODED_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlrmor76CFdZYovQsXFmMI6FFAQeVgp0m3sEqi0wlqR4W9imkc/JmHF1qgyxgHVMPj2EqNQR5BTZ7/pMJaEi99px6/+2xBQ0axWKAq9dQyFWEDnRiDuzIhDVq0vWJvs99/MgJ4qipYAo/mgsbe1JdQTfwfX34rZoU7VyI+OnL1klZfhpDhBC5wMQI54cUjsRF0tjnIxzy8Hr+uSbSU+JiumarDZ0iU/kZtSbr3uvP7ub+daZ1HIT6erJPUx2vJ3AFf1RDvkcgB/rQNTL7XXSiiL+ES3oX/YKs5+iVp/ULwrTwNN6VobPwt6lnxuzM11aKKZy1avFJsAbP6KbiGrIQFwIDAQAB";
}
