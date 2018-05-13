package com.ashomok.lullabies.ui.main;

import android.support.annotation.StringRes;

import com.ashomok.lullabies.billing.model.SkuRowData;
import com.ashomok.lullabies.di_dagger.BasePresenter;

/**
 * Created by iuliia on 1/29/18.
 */

/**
 * This specifies the contract between the view and the presenter.
 */
public interface MainContract {
    interface View {

        void showError(@StringRes int errorMessageRes);

        void showInfo (@StringRes int infoMessageRes);

        void updateView(boolean isAdsActive);

        void initRemoveAdsRow(SkuRowData item);

        void showInfo(String message);
    }

    interface Presenter extends BasePresenter<View> {
        void onRemoveAdsClicked(SkuRowData item);
    }
}
