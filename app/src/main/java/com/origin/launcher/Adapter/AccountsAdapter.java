package com.origin.launcher.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.origin.launcher.R;
import com.origin.launcher.auth.MsftAccountStore;
import com.origin.launcher.AccountTextUtils;

import java.util.ArrayList;
import java.util.List;

public class AccountsAdapter extends RecyclerView.Adapter<AccountsAdapter.AccountViewHolder> {

    public interface OnAccountActionListener {
        void onSetActive(MsftAccountStore.MsftAccount account);
        void onDelete(MsftAccountStore.MsftAccount account);
    }

    private final List<MsftAccountStore.MsftAccount> accounts = new ArrayList<>();
    private OnAccountActionListener listener;

    public void setOnAccountActionListener(OnAccountActionListener listener) {
        this.listener = listener;
    }

    public void updateAccounts(List<MsftAccountStore.MsftAccount> list) {
        accounts.clear();
        if (list != null) accounts.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_msft_account, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        MsftAccountStore.MsftAccount a = accounts.get(position);
        String titleText = AccountTextUtils.titleOrUnknown(a);
        holder.title.setText(titleText);

        String sub = AccountTextUtils.subtitle(a);
        holder.subtitle.setText(sub);

        holder.activeBadge.setVisibility(a.active ? View.VISIBLE : View.GONE);

        holder.btnUse.setOnClickListener(v -> {
            if (listener != null) listener.onSetActive(a);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(a);
        });
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    static class AccountViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView subtitle;
        TextView activeBadge;
        Button btnUse;
        Button btnDelete;

        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.account_title);
            subtitle = itemView.findViewById(R.id.account_subtitle);
            activeBadge = itemView.findViewById(R.id.active_badge);
            btnUse = itemView.findViewById(R.id.btn_use);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}