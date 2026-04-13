package com.origin.launcher.fragments;

import android.content.DialogInterface;
import org.jetbrains.annotations.NotNull;
import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.widget.EditText;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import androidx.core.content.FileProvider;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Build;
import android.os.Environment;
import android.net.Uri;
import android.provider.Settings;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import android.os.Looper;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.origin.launcher.manager.ThemeManager;
import com.origin.launcher.utils.ThemeUtils;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.origin.launcher.activity.MainActivity;
import com.origin.launcher.Launcher.MinecraftLauncher;
import com.origin.launcher.discord.DiscordRPCHelper;
import com.origin.launcher.R;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.origin.launcher.versions.VersionManager;
import java.util.concurrent.ExecutorService;
import android.view.MotionEvent;
import android.content.Context;
import com.origin.launcher.utils.FeatureSettings;
import com.origin.launcher.manager.ResourcepackHandler;
import com.origin.launcher.versions.GameVersion;
import android.app.Activity;
import androidx.core.content.ContextCompat;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import coelho.msftauth.api.oauth20.OAuth20Token;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.origin.launcher.account.MsftAccountStore;
import com.origin.launcher.account.MsftAuthManager;
import com.origin.launcher.activity.AccountsActivity;
import com.origin.launcher.activity.MsftLoginActivity;
import com.origin.launcher.dialogs.LoadingDialog;
import com.origin.launcher.utils.AccountTextUtils;
import com.origin.launcher.utils.DialogUtils;

public class HomeFragment extends BaseThemedFragment {

    private static final String TAG = "HomeFragment";
    private TextView listener;
    private Button mbl2_button;
    private Button versions_button;
    private com.google.android.material.button.MaterialButton shareLogsButton;
    private MinecraftLauncher minecraftLauncher;
    private VersionManager versionManager;

    private com.microsoft.xbox.idp.toolkit.CircleImageView accountAvatar;
    private View accountAvatarContainer;
    private ProgressBar avatarProgress;
    private Button signInButton;
    private String lastAvatarXuid;
    private final OkHttpClient avatarClient = new OkHttpClient();
    private ExecutorService accountExecutor = Executors.newSingleThreadExecutor();
    private LoadingDialog accountLoadingDialog;
    private ActivityResultLauncher<Intent> accountLoginLauncher;

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void openStoragePermissionSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                startActivity(intent);
            } else {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                startActivity(intent);
            }
        } catch (Exception e) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            startActivity(intent);
        }
    }

    private void showLaunchStorageWarningDialog() {
        if (getContext() == null) return;

        LinearLayout dialogLayout = new LinearLayout(requireContext());
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        dialogLayout.setPadding(padding, padding, padding, padding);

        TextView titleText = new TextView(requireContext());
        titleText.setText("Storage Permission Required");
        titleText.setTextSize(18);
        titleText.setTypeface(null, Typeface.BOLD);
        titleText.setTextColor(ThemeManager.getInstance().getColor("onSurface"));
        dialogLayout.addView(titleText);

        TextView messageText = new TextView(requireContext());
        messageText.setText("Xelo Client requires storage access to launch Minecraft and manage game files properly. Without this permission, the game cannot be launched.\n\nPlease grant storage permission to continue.");
        messageText.setTextSize(14);
        messageText.setTextColor(ThemeManager.getInstance().getColor("onSurfaceVariant"));
        LinearLayout.LayoutParams msgParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        msgParams.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
        messageText.setLayoutParams(msgParams);
        dialogLayout.addView(messageText);

        LinearLayout buttonRow = new LinearLayout(requireContext());
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.topMargin = (int) (24 * getResources().getDisplayMetrics().density);
        buttonRow.setLayoutParams(rowParams);

        MaterialButton cancelButton = new MaterialButton(requireContext());
        cancelButton.setText("Exit");
        cancelButton.setAllCaps(false);
        cancelButton.setStateListAnimator(null);
        GradientDrawable cancelBg = new GradientDrawable();
        cancelBg.setShape(GradientDrawable.RECTANGLE);
        cancelBg.setColor(Color.parseColor("#F44336"));
        cancelBg.setCornerRadius(12 * getResources().getDisplayMetrics().density);
        cancelButton.setBackground(cancelBg);
        cancelButton.setBackgroundTintList(null);
        cancelButton.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cancelParams.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
        cancelButton.setLayoutParams(cancelParams);

        MaterialButton okButton = new MaterialButton(requireContext());
        okButton.setText("Grant Permission");
        okButton.setAllCaps(false);
        okButton.setStateListAnimator(null);
        ThemeUtils.applyThemeToButton(okButton, requireContext());
        LinearLayout.LayoutParams okParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        okButton.setLayoutParams(okParams);

        buttonRow.addView(cancelButton);
        buttonRow.addView(okButton);
        dialogLayout.addView(buttonRow);

        GradientDrawable dialogBg = new GradientDrawable();
        dialogBg.setShape(GradientDrawable.RECTANGLE);
        dialogBg.setColor(ThemeManager.getInstance().getColor("surface"));
        dialogBg.setCornerRadius(16 * getResources().getDisplayMetrics().density);
        dialogBg.setStroke(
                (int) (1 * getResources().getDisplayMetrics().density),
                ThemeManager.getInstance().getColor("outline")
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogLayout);
        builder.setCancelable(false);

        AlertDialog launchDialog = builder.create();
        if (launchDialog.getWindow() != null) {
            launchDialog.getWindow().setBackgroundDrawable(dialogBg);
        }

        cancelButton.setOnClickListener(v -> {
            launchDialog.dismiss();
            if (mbl2_button != null) mbl2_button.setEnabled(true);
            requireActivity().finish();
        });

        okButton.setOnClickListener(v -> {
            launchDialog.dismiss();
            if (mbl2_button != null) mbl2_button.setEnabled(true);
            openStoragePermissionSettings();
        });

        launchDialog.show();
    }

    private void launchGame() {
        if (mbl2_button == null) return;

        mbl2_button.setEnabled(false);

        if (!hasStoragePermission()) {
            showLaunchStorageWarningDialog();
            return;
        }

        if (FeatureSettings.getInstance().isLauncherManagedMcLoginEnabled()) {
            MsftAccountStore.MsftAccount active = getActiveAccount();
            boolean loggedIn = active != null
                    && active.minecraftUsername != null
                    && !active.minecraftUsername.isEmpty();
            if (!loggedIn) {
                mbl2_button.setEnabled(true);
                new AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.dialog_title_login_required))
                        .setMessage(getString(R.string.dialog_message_login_required))
                        .setPositiveButton(getString(R.string.go_to_accounts), (d, w) ->
                                startActivity(new Intent(requireContext(), AccountsActivity.class)))
                        .setNegativeButton(getString(R.string.disable_launcher_login_and_continue), null)
                        .show();
                return;
            }
        }

        GameVersion version = versionManager != null ? versionManager.getSelectedVersion() : null;

        if (version == null) {
            mbl2_button.setEnabled(true);
            showErrorDialog("No Version", "Please select a Minecraft version first.");
            return;
        }

        if (!version.isInstalled && !FeatureSettings.getInstance().isVersionIsolationEnabled()) {
            mbl2_button.setEnabled(true);
            showVersionIsolationDialog();
            return;
        }

        new Thread(() -> {
            try {
                minecraftLauncher.launch(requireActivity().getIntent(), version);
                requireActivity().runOnUiThread(() -> {
                    mbl2_button.setEnabled(true);
                    if (listener != null) listener.setText("Minecraft launched successfully");
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    mbl2_button.setEnabled(true);
                    showErrorDialog("Launch Failed", e.getMessage());
                });
            }
        }).start();
    }

    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show();
    }

    private void showVersionIsolationDialog() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Version Isolation Required")
            .setMessage("Enable version isolation to launch uninstalled versions?")
            .setPositiveButton("Enable", (dialog, which) -> {
                FeatureSettings.getInstance().setVersionIsolationEnabled(true);
                launchGame();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void setupManagersAndHandlers() {
        versionManager = VersionManager.get(requireContext());
        versionManager.loadAllVersions();
        minecraftLauncher = new MinecraftLauncher(requireContext());
    }

    private void checkResourcepack() {
        if (getActivity() == null) return;
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        new ResourcepackHandler((Activity) getActivity(), minecraftLauncher, executorService)
            .checkIntentForResourcepack();
    }

    private void registerAccountLoginLauncher() {
        accountLoginLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    String code = result.getData().getStringExtra("ms_auth_code");
                    String codeVerifier = result.getData().getStringExtra("ms_code_verifier");
                    if (code != null && codeVerifier != null) {
                        accountLoadingDialog = DialogUtils.ensure(requireActivity(), accountLoadingDialog);
                        DialogUtils.showWithMessage(accountLoadingDialog, getString(R.string.ms_login_exchanging));

                        accountExecutor.execute(() -> {
                            OkHttpClient client = new OkHttpClient();
                            try {
                                OAuth20Token token = MsftAuthManager.exchangeCodeForToken(
                                    client,
                                    MsftAuthManager.DEFAULT_CLIENT_ID,
                                    code, codeVerifier,
                                    MsftAuthManager.DEFAULT_SCOPE + " offline_access"
                                );

                                requireActivity().runOnUiThread(() ->
                                    DialogUtils.showWithMessage(accountLoadingDialog,
                                        getString(R.string.ms_login_auth_xbox_device)));

                                MsftAuthManager.XboxAuthResult xbox =
                                    MsftAuthManager.performXboxAuth(client, token, requireContext());

                                requireActivity().runOnUiThread(() ->
                                    DialogUtils.showWithMessage(accountLoadingDialog,
                                        getString(R.string.ms_login_fetch_minecraft_identity)));

                                android.util.Pair<String, String> nameAndXuid =
                                    MsftAuthManager.fetchMinecraftIdentity(client, xbox.xstsToken());
                                String minecraftUsername = nameAndXuid != null ? nameAndXuid.first : null;
                                String xuid = nameAndXuid != null ? nameAndXuid.second : null;
                                MsftAuthManager.saveAccount(requireContext(), token,
                                    xbox.gamertag(), minecraftUsername, xuid, xbox.avatarUrl());

                                requireActivity().runOnUiThread(() -> {
                                    DialogUtils.dismissQuietly(accountLoadingDialog);
                                    Toast.makeText(requireContext(),
                                        getString(R.string.ms_login_success,
                                            minecraftUsername != null ? minecraftUsername
                                                : getString(R.string.not_signed_in)),
                                        Toast.LENGTH_SHORT).show();
                                    refreshAccountHeaderUI();
                                });
                            } catch (Exception e) {
                                requireActivity().runOnUiThread(() -> {
                                    DialogUtils.dismissQuietly(accountLoadingDialog);
                                    Toast.makeText(requireContext(),
                                        getString(R.string.ms_login_failed_detail, e.getMessage()),
                                        Toast.LENGTH_LONG).show();
                                    refreshAccountHeaderUI();
                                });
                            }
                        });
                        return;
                    }
                }
                refreshAccountHeaderUI();
            }
        );
    }

    private void initAccountHeader() {
        View view = getView();
        if (view == null) return;

        signInButton = view.findViewById(R.id.signInButton);
        accountAvatar = view.findViewById(R.id.accountAvatar);
        accountAvatarContainer = view.findViewById(R.id.accountAvatarContainer);
        avatarProgress = view.findViewById(R.id.avatarProgress);

        if (signInButton != null) {
            signInButton.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), MsftLoginActivity.class);
                accountLoginLauncher.launch(intent);
            });
        }
        if (accountAvatarContainer != null) {
            accountAvatarContainer.setOnClickListener(this::showAccountSwitchPopup);
        }

        refreshAccountHeaderUI();
    }

    private MsftAccountStore.MsftAccount getActiveAccount() {
        java.util.List<MsftAccountStore.MsftAccount> list = MsftAccountStore.list(requireContext());
        for (MsftAccountStore.MsftAccount a : list) if (a.active) return a;
        return null;
    }

    private void refreshAccountHeaderUI() {
        MsftAccountStore.MsftAccount active = getActiveAccount();
        if (active == null) {
            if (signInButton != null) signInButton.setVisibility(View.VISIBLE);
            if (accountAvatarContainer != null) accountAvatarContainer.setVisibility(View.GONE);
            if (accountAvatar != null) accountAvatar.setImageDrawable(null);
            lastAvatarXuid = null;
            if (avatarProgress != null) avatarProgress.setVisibility(View.GONE);
        } else {
            if (signInButton != null) signInButton.setVisibility(View.GONE);
            if (accountAvatarContainer != null) accountAvatarContainer.setVisibility(View.VISIBLE);
            loadXboxAvatar(active);
        }
    }

    private void loadXboxAvatar(MsftAccountStore.MsftAccount active) {
        if (accountAvatar == null) return;
        String url = AccountTextUtils.sanitizeUrl(active != null ? active.xboxAvatarUrl : null);
        if (url == null) {
            if (avatarProgress != null) avatarProgress.setVisibility(View.GONE);
            accountAvatar.setImageDrawable(null);
            lastAvatarXuid = null;
            return;
        }
        accountAvatar.setImageDrawable(null);
        if (avatarProgress != null) avatarProgress.setVisibility(View.VISIBLE);
        accountExecutor.execute(() -> {
            try {
                try (Response imgResp = avatarClient.newCall(
                        new Request.Builder().url(url).build()).execute()) {
                    Bitmap bmp = (imgResp.isSuccessful() && imgResp.body() != null)
                        ? android.graphics.BitmapFactory.decodeStream(imgResp.body().byteStream())
                        : null;
                    requireActivity().runOnUiThread(() -> {
                        if (bmp != null) accountAvatar.setImageBitmap(bmp);
                        if (avatarProgress != null) avatarProgress.setVisibility(View.GONE);
                    });
                }
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    if (avatarProgress != null) avatarProgress.setVisibility(View.GONE);
                });
            }
        });
    }

    private void showAccountSwitchPopup(View anchor) {
        if (getContext() == null) return;
        java.util.List<MsftAccountStore.MsftAccount> list = MsftAccountStore.list(requireContext());

        View content = LayoutInflater.from(requireContext())
            .inflate(R.layout.popup_account_switch, null);
        RecyclerView recyclerAccounts = content.findViewById(R.id.recycler_accounts);
        TextView manageAction = content.findViewById(R.id.manage_action);
        com.microsoft.xbox.idp.toolkit.CircleImageView headerAvatar =
            content.findViewById(R.id.header_avatar);
        View headerContainer = content.findViewById(R.id.header_container);
        TextView headerName = content.findViewById(R.id.header_name);

        TypedValue outValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(
            android.R.attr.selectableItemBackground, outValue, true);
        int selectableRes = outValue.resourceId;
        int paddingH = (int) (16 * getResources().getDisplayMetrics().density);
        int paddingV = (int) (12 * getResources().getDisplayMetrics().density);
        int paddingR = (int) (12 * getResources().getDisplayMetrics().density);

        MsftAccountStore.MsftAccount active = getActiveAccount();
        headerName.setText(AccountTextUtils.displayNameOrNotSigned(requireContext(), active));
        if (accountAvatar != null && accountAvatar.getDrawable() != null) {
            headerAvatar.setImageDrawable(accountAvatar.getDrawable());
        } else if (active != null) {
            final String url = AccountTextUtils.sanitizeUrl(active.xboxAvatarUrl);
            if (url != null) {
                accountExecutor.execute(() -> {
                    try {
                        OkHttpClient client = new OkHttpClient();
                        Response imgResp = client.newCall(
                            new Request.Builder().url(url).build()).execute();
                        final Bitmap bmp = (imgResp.isSuccessful() && imgResp.body() != null)
                            ? android.graphics.BitmapFactory.decodeStream(imgResp.body().byteStream())
                            : null;
                        requireActivity().runOnUiThread(() -> {
                            if (bmp != null) headerAvatar.setImageBitmap(bmp);
                        });
                    } catch (Exception ignored) {}
                });
            }
        }

        final PopupWindow popup = new PopupWindow(content,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        if (android.os.Build.VERSION.SDK_INT >= 21) popup.setElevation(8f);

        final android.view.ViewGroup root = requireActivity().findViewById(android.R.id.content);
        final View scrim = new View(requireContext());
        scrim.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.scrim));
        scrim.setClickable(true);
        scrim.setOnClickListener(v -> popup.dismiss());
        root.addView(scrim, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        scrim.setAlpha(0f);
        scrim.animate().alpha(1f).setDuration(120).start();

        final java.util.List<MsftAccountStore.MsftAccount> displayList = new java.util.ArrayList<>();
        for (MsftAccountStore.MsftAccount a : list) {
            if (active == null || !android.text.TextUtils.equals(a.id, active.id))
                displayList.add(a);
        }

        recyclerAccounts.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerAccounts.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                TextView row = new TextView(parent.getContext());
                row.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                row.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.on_surface));
                row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                row.setPadding(paddingH, paddingV, paddingR, paddingV);
                row.setBackgroundResource(selectableRes);
                return new RecyclerView.ViewHolder(row) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                MsftAccountStore.MsftAccount account = displayList.get(position);
                ((TextView) holder.itemView).setText(AccountTextUtils.titleOrUnknown(account));
                holder.itemView.setOnClickListener(v -> {
                    popup.dismiss();
                    MsftAccountStore.setActive(requireContext(), account.id);
                    boolean withinSevenDays = AccountTextUtils.isRecentlyUpdated(account, 7);
                    if (withinSevenDays) {
                        String statusName = AccountTextUtils.displayNameOrNotSigned(
                            requireContext(), account);
                        Toast.makeText(requireContext(),
                            getString(R.string.ms_login_success, statusName),
                            Toast.LENGTH_SHORT).show();
                        refreshAccountHeaderUI();
                        return;
                    }
                    accountLoadingDialog = DialogUtils.ensure(requireActivity(), accountLoadingDialog);
                    DialogUtils.showWithMessage(accountLoadingDialog,
                        getString(R.string.ms_login_auth_xbox_device));
                    accountExecutor.execute(() -> {
                        OkHttpClient client = new OkHttpClient();
                        try {
                            MsftAuthManager.XboxAuthResult xbox =
                                MsftAuthManager.refreshAndAuth(client, account, requireContext());
                            android.util.Pair<String, String> nameAndXuid =
                                MsftAuthManager.fetchMinecraftIdentity(client, xbox.xstsToken());
                            String minecraftUsername = nameAndXuid != null ? nameAndXuid.first : null;
                            String xuid = nameAndXuid != null ? nameAndXuid.second : null;
                            MsftAccountStore.addOrUpdate(requireContext(), account.msUserId,
                                account.refreshToken, xbox.gamertag(), minecraftUsername,
                                xuid, xbox.avatarUrl());
                            MsftAccountStore.setActive(requireContext(), account.id);
                            requireActivity().runOnUiThread(() -> {
                                DialogUtils.dismissQuietly(accountLoadingDialog);
                                String statusName = minecraftUsername != null ? minecraftUsername
                                    : getString(R.string.not_signed_in);
                                Toast.makeText(requireContext(),
                                    getString(R.string.ms_login_success, statusName),
                                    Toast.LENGTH_SHORT).show();
                                refreshAccountHeaderUI();
                            });
                        } catch (Exception e) {
                            requireActivity().runOnUiThread(() -> {
                                DialogUtils.dismissQuietly(accountLoadingDialog);
                                Toast.makeText(requireContext(),
                                    getString(R.string.ms_login_failed_detail, e.getMessage()),
                                    Toast.LENGTH_LONG).show();
                                refreshAccountHeaderUI();
                            });
                        }
                    });
                });
            }

            @Override
            public int getItemCount() { return displayList.size(); }
        });

        float density = getResources().getDisplayMetrics().density;
        if (displayList.size() > 2) {
            recyclerAccounts.getLayoutParams().height = (int) ((48 * 2 + 16) * density);
        } else {
            recyclerAccounts.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }

        manageAction.setOnClickListener(v -> {
            popup.dismiss();
            startActivity(new Intent(requireContext(), AccountsActivity.class));
        });

        popup.setOnDismissListener(() -> {
            if (root != null && scrim != null) {
                scrim.animate().alpha(0f).setDuration(120).withEndAction(() -> {
                    try { root.removeView(scrim); } catch (Exception ignored) {}
                }).start();
            }
        });

        popup.showAsDropDown(anchor, 0, 0, Gravity.END);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        listener = view.findViewById(R.id.listener);
        mbl2_button = view.findViewById(R.id.mbl2_load);
        versions_button = view.findViewById(R.id.versions_button);
        shareLogsButton = view.findViewById(R.id.share_logs_button);
        Handler handler = new Handler(Looper.getMainLooper());

        // Apply initial theme
        applyInitialTheme(view);

        mbl2_button.setOnClickListener(v -> launchGame());

        // Long press to clear APK selection
        mbl2_button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                clearSelectedApk();
                return true;
            }
        });

        versions_button.setOnClickListener(v -> {
            try {
                requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_fade_in_right,
                        R.anim.slide_out_right,
                        R.anim.slide_in_left,
                        R.anim.slide_out_left
                    )
                    .replace(R.id.fragment_container, new VersionsFragment())
                    .addToBackStack(null)
                    .commit();

                Log.d(TAG, "Opening themes fragment");
            } catch (Exception e) {
                Log.e(TAG, "Error opening themes", e);
                Toast.makeText(getContext(), "Unable to open themes", Toast.LENGTH_SHORT).show();
            }
        });

        // Set initial log text
        listener.setText("Ready to launch Minecraft");

        // Show current selection status
        updateSelectionStatus();

        // Set up share button
        shareLogsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareLogs();
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupManagersAndHandlers();
        checkResourcepack();
        registerAccountLoginLauncher();
        initAccountHeader();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listener = null;
        mbl2_button = null;
        versions_button = null;
        shareLogsButton = null;
        signInButton = null;
        accountAvatar = null;
        accountAvatarContainer = null;
        avatarProgress = null;
    }

    /**
     * Apply initial theme to all views
     */
    private void applyInitialTheme(View view) {
        try {
            ThemeManager themeManager = ThemeManager.getInstance();
            if (themeManager != null && themeManager.isThemeLoaded()) {
                // Apply theme to main button
                if (mbl2_button instanceof MaterialButton) {
                    ThemeUtils.applyThemeToButton((MaterialButton) mbl2_button, requireContext());
                }

                // Apply theme to share button (remove background, make it text button)
                if (shareLogsButton != null) {
                    ThemeUtils.applyThemeToButton(shareLogsButton, requireContext());
                    // Remove background and make it transparent
                    shareLogsButton.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                    shareLogsButton.setStrokeWidth(0);
                }

                // Ensure versions nav button is transparent like share button
                if (versions_button instanceof MaterialButton) {
                    MaterialButton vb = (MaterialButton) versions_button;
                    ThemeUtils.applyThemeToButton(vb, requireContext());
                    vb.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                    vb.setStrokeWidth(0);
                    try {
                        vb.setIconTint(ColorStateList.valueOf(themeManager.getColor("onSurfaceVariant")));
                    } catch (Exception ignored) {}
                }

                // Apply theme to log text area
                if (listener != null) {
                    listener.setTextColor(themeManager.getColor("onSurfaceVariant"));
                    // Set background color for the log text area
                    View logCard = view.findViewById(R.id.logCard);
                    if (logCard instanceof MaterialCardView) {
                        MaterialCardView card = (MaterialCardView) logCard;
                        card.setCardBackgroundColor(themeManager.getColor("surfaceVariant"));
                        card.setStrokeColor(themeManager.getColor("outline"));
                    }
                }
            }
        } catch (Exception e) {
            // Handle error gracefully
        }
    }

    @Override
    protected void onApplyTheme() {
        super.onApplyTheme();

        View view = getView();
        if (view != null) {
            // Refresh all theme elements
            applyInitialTheme(view);
        }
    }

    private String getPackageNameFromSettings() {
        VersionManager vm = VersionManager.get(requireContext());
        GameVersion version = vm.getSelectedVersion();
        return version != null ? version.packageName : "com.mojang.minecraftpe";
    }

    private String getSelectedApkPath() {
        SharedPreferences prefs = requireContext().getSharedPreferences("selected_apk", 0);
        return prefs.getString("apk_path", null);
    }

    private void updateSelectionStatus() {
        String selectedApkPath = getSelectedApkPath();
        if (selectedApkPath != null && new File(selectedApkPath).exists()) {
            String fileName = new File(selectedApkPath).getName();
            listener.setText("Ready to launch Minecraft\nSelected APK: " + fileName);
        } else {
            listener.setText("Ready to launch Minecraft");
        }
    }

    private void clearSelectedApk() {
        SharedPreferences prefs = requireContext().getSharedPreferences("selected_apk", 0);
        prefs.edit().remove("apk_path").apply();
        updateSelectionStatus();
        Toast.makeText(requireContext(), "Cleared APK selection", Toast.LENGTH_SHORT).show();
    }

    private void shareLogs() {
        try {
            // Get the current log text
            String logText = listener.getText().toString();

            // Create a temporary file
            File logFile = new File(requireContext().getCacheDir(), "latestlog.txt");
            FileWriter writer = new FileWriter(logFile);
            writer.write(logText);
            writer.close();

            // Create the sharing intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");

            // Get the file URI using FileProvider
            android.net.Uri fileUri = FileProvider.getUriForFile(
                requireContext(),
                "com.origin.launcher.fileprovider",
                logFile
            );

            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Xelo Client Logs");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Xelo Client Latest Logs");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Start the sharing activity
            startActivity(Intent.createChooser(shareIntent, "Share Logs"));

        } catch (Exception e) {
            // Show error message
            android.widget.Toast.makeText(requireContext(), "Failed to share logs: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("SetTextI18n")

    private Object getPathList(@NotNull ClassLoader classLoader) throws Exception {
        Field pathListField = Objects.requireNonNull(classLoader.getClass().getSuperclass()).getDeclaredField("pathList");
        pathListField.setAccessible(true);
        return pathListField.get(classLoader);
    }

    private boolean processNativeLibraries(ApplicationInfo mcInfo, @NotNull Object pathList, @NotNull Handler handler, TextView listener) throws Exception {
        FileInputStream inStream = new FileInputStream(getApkWithLibs(mcInfo));
        BufferedInputStream bufInStream = new BufferedInputStream(inStream);
        ZipInputStream inZipStream = new ZipInputStream(bufInStream);
        if (!checkLibCompatibility(inZipStream)) {
            handler.post(() -> alertAndExit("Wrong minecraft architecture", "The minecraft you have installed does not support the same main architecture (" + Build.SUPPORTED_ABIS[0] + ") your device uses, Xelo client cant work with it"));
            return false;
        }
        Method addNativePath = pathList.getClass().getDeclaredMethod("addNativePath", Collection.class);
        ArrayList<String> libDirList = new ArrayList<>();
        File libdir = new File(mcInfo.nativeLibraryDir);
        if (libdir.list() == null || libdir.list().length == 0
         || (mcInfo.flags & ApplicationInfo.FLAG_EXTRACT_NATIVE_LIBS) != ApplicationInfo.FLAG_EXTRACT_NATIVE_LIBS) {
            loadUnextractedLibs(mcInfo);
            libDirList.add(requireActivity().getCodeCacheDir().getAbsolutePath() + "/");
        } else {
            libDirList.add(mcInfo.nativeLibraryDir);
        }
        addNativePath.invoke(pathList, libDirList);
        handler.post(() -> listener.append("\n-> " + mcInfo.nativeLibraryDir + " added to native library directory path"));
        return true;
    }

    private static Boolean checkLibCompatibility(ZipInputStream zip) throws Exception {
        ZipEntry ze = null;
        String requiredLibDir = "lib/" + Build.SUPPORTED_ABIS[0] + "/";
        while ((ze = zip.getNextEntry()) != null) {
            if (ze.getName().startsWith(requiredLibDir)) {
                return true;
            }
        }
        zip.close();
        return false;
    }

    private void alertAndExit(String issue, String description) {
        AlertDialog alertDialog = new AlertDialog.Builder(requireActivity()).create();
        alertDialog.setTitle(issue);
        alertDialog.setMessage(description);
        alertDialog.setCancelable(false);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Exit",
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                requireActivity().finish();
            }
        });
        alertDialog.show();
    }

    private void loadUnextractedLibs(ApplicationInfo appInfo) throws Exception {
        FileInputStream inStream = new FileInputStream(getApkWithLibs(appInfo));
        BufferedInputStream bufInStream = new BufferedInputStream(inStream);
        ZipInputStream inZipStream = new ZipInputStream(bufInStream);
        String zipPath = "lib/" + Build.SUPPORTED_ABIS[0] + "/";
        String outPath = requireActivity().getCodeCacheDir().getAbsolutePath() + "/";
        File dir = new File(outPath);
        dir.mkdir();
        extractDir(appInfo, inZipStream, zipPath, outPath);
    }

    public String getApkWithLibs(ApplicationInfo pkg) throws PackageManager.NameNotFoundException {
        String[] sn = pkg.splitSourceDirs;
        if (sn != null && sn.length > 0) {
            String cur_abi = Build.SUPPORTED_ABIS[0].replace('-', '_');
            for (String n : sn) {
                if (n.contains(cur_abi)) {
                    return n;
                }
            }
        }
        return pkg.sourceDir;
    }

    private static void extractDir(ApplicationInfo mcInfo, ZipInputStream zip, String zip_folder, String out_folder) throws Exception {
        ZipEntry ze = null;
        while ((ze = zip.getNextEntry()) != null) {
            if (ze.getName().startsWith(zip_folder) && !ze.getName().contains("c++_shared")) {
                String strippedName = ze.getName().substring(zip_folder.length());
                String path = out_folder + "/" + strippedName;
                OutputStream out = new FileOutputStream(path);
                BufferedOutputStream outBuf = new BufferedOutputStream(out);
                byte[] buffer = new byte[9000];
                int len;
                while ((len = zip.read(buffer)) != -1) {
                    outBuf.write(buffer, 0, len);
                }
                outBuf.close();
            }
        }
        zip.close();
    }

    private static void copyFile(InputStream from, @NotNull File to) throws IOException {
        File parentDir = to.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("Failed to create directories");
        }
        if (!to.exists() && !to.createNewFile()) {
            throw new IOException("Failed to create new file");
        }
        try (BufferedInputStream input = new BufferedInputStream(from);
             BufferedOutputStream output = new BufferedOutputStream(Files.newOutputStream(to.toPath()))) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        DiscordRPCHelper.getInstance().updateMenuPresence("Playing");
        refreshAccountHeaderUI();
    }

    @Override
    public void onPause() {
        super.onPause();
        DiscordRPCHelper.getInstance().updateIdlePresence();
    }
}
