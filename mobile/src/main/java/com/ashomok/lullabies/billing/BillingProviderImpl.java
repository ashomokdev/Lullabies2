package com.ashomok.lullabies.billing;

import android.app.Activity;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;
import com.annimon.stream.Stream;
import com.ashomok.lullabies.R;
import com.ashomok.lullabies.billing.model.SkuRowData;
import com.ashomok.lullabies.utils.LogHelper;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by iuliia on 2/14/18.
 */

public class BillingProviderImpl implements BillingProvider {

    private BillingManager mBillingManager;
    private boolean mAdsFreeForever;
    public static final String ADS_FREE_FOREVER_SKU_ID = "ads_free_forever";
    public static final String TAG = LogHelper.makeLogTag(BillingProviderImpl.class);

    private List<SkuRowData> skuRowDataList = new ArrayList<>();

    @Nullable
    BillingProviderCallback callback;

    @NonNull
    private Activity activity;

    @Inject
    public BillingProviderImpl(@NonNull Activity activity) {
        this.activity = activity;
    }

    public void init() {
        // Create and initialize BillingManager which talks to BillingLibrary
        mBillingManager = new BillingManager(activity, new UpdateListener());
    }

    public void setCallback(@Nullable BillingProviderCallback callback) {
        this.callback = callback;
    }

    public List<SkuRowData> getSkuRowDataList() {
        return skuRowDataList;
    }

    @Override
    public List<SkuRowData> getSkuRowDataListForInAppPurchases() {
        return Stream.of(skuRowDataList)
                .filter(i -> i.getSkuType().equals(BillingClient.SkuType.INAPP))
                .toList();
    }

    private void updatePurchaseData() {
        List<String> inAppSkus = new ArrayList<>();
        inAppSkus.add(ADS_FREE_FOREVER_SKU_ID);
        processSkuRows(skuRowDataList, inAppSkus, BillingClient.SkuType.INAPP, null);
    }

    private void processSkuRows(List<SkuRowData> inList, List<String> skusList,
                                final @BillingClient.SkuType String billingType,
                                final Runnable executeWhenFinished) {
        getBillingManager().querySkuDetailsAsync(billingType, skusList,
                (responseCode, skuDetailsList) -> {

                    if (responseCode != BillingClient.BillingResponse.OK) {
                        Log.e(TAG, "Unsuccessful query for type: " + billingType
                                + ". Error code: " + responseCode);
                        onBillingError();
                    } else if (skuDetailsList == null || skuDetailsList.size() == 0) {
                        Log.e(TAG, "skuDetailsList is empty");
                        onBillingError();
                    } else {
                        // If we successfully got SKUs - fill all rows
                        for (SkuDetails details : skuDetailsList) {
                            Log.i(TAG, "Adding sku: " + details);
                            inList.add(new SkuRowData(details, billingType));
                        }
                        if (inList.size() == 0) {
                            onBillingError();
                        }
                        if (callback != null) {
                            callback.onSkuRowDataUpdated();
                        }
                    }

                    if (executeWhenFinished != null) {
                        executeWhenFinished.run();
                    }
                });
    }

    public void destroy() {
        if (mBillingManager != null) {
            mBillingManager.destroy();
        }
    }

    private void onBillingError() {
        int billingResponseCode = getBillingManager()
                .getBillingClientResponseCode();

        switch (billingResponseCode) {
            case BillingClient.BillingResponse.OK:
                // If manager was connected successfully, then show no SKUs error
                showError(R.string.error_no_skus);
                break;
            case BillingClient.BillingResponse.BILLING_UNAVAILABLE:
                showError(R.string.error_billing_unavailable);
                break;
            default:
                showError(R.string.unknown_error);
        }
    }

    private void showError(@StringRes int stringResId) {
        if (callback != null) {
            callback.showError(stringResId);
        }
    }

    private void showInfo(String message) {
        if (callback != null) {
            callback.showInfo(message);
        }
    }

    @Override
    public BillingManager getBillingManager() {
        return mBillingManager;
    }

    @Override
    public boolean isAdsFreeForever() {
        return mAdsFreeForever;
    }

    /**
     * Handler to billing updates
     */
    public class UpdateListener implements BillingManager.BillingUpdatesListener {
        @Override
        public void onBillingClientSetupFinished() {
            updatePurchaseData();
        }

        @Override
        public void onConsumeFinished(String token, @BillingClient.BillingResponse int result) {
            //consume nothing
        }

        @Override
        public void onPurchasesUpdated(List<Purchase> purchaseList) {
            mAdsFreeForever = false;

            for (Purchase purchase : purchaseList) {
                switch (purchase.getSku()) {
                    case ADS_FREE_FOREVER_SKU_ID:
                        mAdsFreeForever = true;
                        break;
                }
            }
            if (callback != null) {
                callback.onPurchasesUpdated();
            }
        }
    }
}
