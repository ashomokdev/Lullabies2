package com.ashomok.lullabies.ui.main;

import android.content.Context;
import android.support.annotation.Nullable;

import com.ashomok.lullabies.R;
import com.ashomok.lullabies.billing.BillingProviderCallback;
import com.ashomok.lullabies.billing.BillingProviderImpl;
import com.ashomok.lullabies.billing.model.SkuRowData;
import com.ashomok.lullabies.utils.LogHelper;
import com.ashomok.lullabies.utils.NetworkHelper;

import java.util.List;

import javax.inject.Inject;

import static com.ashomok.lullabies.billing.BillingProviderImpl.ADS_FREE_FOREVER_SKU_ID;

/**
 * Created by iuliia on 1/29/18.
 */

public class MainPresenter implements MainContract.Presenter {

    public static final String TAG = LogHelper.makeLogTag(MainPresenter.class);
    @Nullable
    private MainContract.View view;

    @Inject
    BillingProviderImpl billingProvider;

    @Inject
    Context context;

    private BillingProviderCallback billingProviderCallback = new BillingProviderCallback() {
        @Override
        public void onPurchasesUpdated() {
            if (view != null) {
                boolean isAdsActive = !billingProvider.isAdsFreeForever();
                view.updateView(isAdsActive);
            }
        }

        @Override
        public void showError(int stringResId) {
            if (view != null) {
                view.showError(stringResId);
            }
        }

        @Override
        public void showInfo(String message) {
            if (view != null) {
                view.showInfo(message);
            }
        }

        @Override
        public void onSkuRowDataUpdated() {
            updateSkuRows(billingProvider.getSkuRowDataListForInAppPurchases());
        }
    };


    @Inject
    MainPresenter() {
    }

    /**
     * update sku rows
     *
     * @param skuRowData
     */
    private void updateSkuRows(List<SkuRowData> skuRowData) {
        if (view != null) {
            if (skuRowData.size() == 2) {
                for (SkuRowData item : skuRowData) {
                    switch (item.getSku()) {
                        case ADS_FREE_FOREVER_SKU_ID:
                            view.initRemoveAdsRow(item);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }


    @Override
    public void takeView(MainContract.View activity) {
        view = activity;
        init();
    }

    private void init() {
        billingProvider.setCallback(billingProviderCallback);
        billingProvider.init();

        if (view != null) {
            checkConnection();
        }
    }

    @Override
    public void dropView() {
        view = null;
        billingProvider.destroy();
    }


    private boolean isOnline() {
        return NetworkHelper.isOnline(context);
    }

    private void checkConnection() {
        if (view != null) {
            if (!isOnline()) {
                view.showError(R.string.no_internet_connection);
            }
        }
    }

    @Override
    public void onRemoveAdsClicked(SkuRowData data) {
        if (data != null) {
            billingProvider.getBillingManager().initiatePurchaseFlow(data.getSku(),
                    data.getSkuType());

        }
    }
}
