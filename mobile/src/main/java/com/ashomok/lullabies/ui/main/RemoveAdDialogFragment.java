package com.ashomok.lullabies.ui.main;

import android.app.DialogFragment;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.ashomok.lullabies.R;

public class RemoveAdDialogFragment extends DialogFragment {

    private Button cancelButton;
    private Button buyButton;
    private CardView cardView;
    private TextView price;

    public RemoveAdDialogFragment() {
        // Empty constructor is required for DialogFragment
        // Make sure not to add arguments to the constructor
        // Use `newInstance` instead as shown below
    }

    public static RemoveAdDialogFragment newInstance(String price) {

        RemoveAdDialogFragment frag = new RemoveAdDialogFragment();
        Bundle args = new Bundle();
        args.putString("price", price);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return inflater.inflate(R.layout.remove_ads_dialog_layout, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cancelButton = view.findViewById(R.id.cancel_button);
        buyButton = view.findViewById(R.id.buy_button);
        price = view.findViewById(R.id.price);
        cardView = view.findViewById(R.id.card_view);

        buyButton.setOnClickListener(view1 -> onBuyClicked());
        cardView.setOnClickListener(view2 -> onBuyClicked());
        cancelButton.setOnClickListener(view12 -> onCancelClicked());

        String priceText = getArguments().getString("price");
        price.setText(priceText);
    }

    private void onCancelClicked() {
        dismiss();
    }

    private void onBuyClicked() {
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
           activity.mPresenter.onRemoveAdsClicked();
        }
        dismiss();
    }
}