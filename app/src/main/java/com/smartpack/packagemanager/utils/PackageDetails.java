/*
 * Copyright (C) 2021-2022 sunilpaulmathew <sunil.kde@gmail.com>
 *
 * This file is part of Package Manager, a simple, yet powerful application
 * to manage other application installed on an android device.
 *
 */

package com.smartpack.packagemanager.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.LinearLayout;

import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;
import com.smartpack.packagemanager.BuildConfig;
import com.smartpack.packagemanager.R;
import com.smartpack.packagemanager.activities.ADBUninstallActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import in.sunilpaulmathew.sCommon.Utils.sAPKUtils;
import in.sunilpaulmathew.sCommon.Utils.sExecutor;
import in.sunilpaulmathew.sCommon.Utils.sPackageUtils;
import in.sunilpaulmathew.sCommon.Utils.sUtils;

/*
 * Created by sunilpaulmathew <sunil.kde@gmail.com> on February 16, 2020
 */
public class PackageDetails {

    public static void exportApp(LinearLayout linearLayout, MaterialTextView textView, Activity activity) {
        if (Build.VERSION.SDK_INT < 29 && sUtils.isPermissionDenied(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, activity)) {
            sUtils.requestPermission(new String[] {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, activity);
            sUtils.snackBar(activity.findViewById(android.R.id.content), activity.getString(R.string.permission_denied_write_storage)).show();
        } else if (new File(sPackageUtils.getSourceDir(Common.getApplicationID(), activity)).getName().equals("base.apk") && SplitAPKInstaller.splitApks(sPackageUtils.getParentDir(Common.getApplicationID(), activity)).size() > 1) {
            exportingBundleTask(linearLayout, textView, sPackageUtils.getParentDir(Common.getApplicationID(), activity), PackageData.getFileName(Common.getApplicationID(), activity),
                    Common.getApplicationIcon(), activity);
        } else {
            exportingTask(linearLayout, textView, Common.getSourceDir(), PackageData.getFileName(Common.getApplicationID(), activity), Common.getApplicationIcon(), activity);
        }
    }

    public static void exportingTask(LinearLayout linearLayout, MaterialTextView textView, String apk,
                                     String name,Drawable icon, Activity activity) {
        new sExecutor() {

            @SuppressLint("StringFormatInvalid")
            @Override
            public void onPreExecute() {

                showProgress(linearLayout, textView, activity.getString(R.string.exporting, name) + "...");
                PackageData.makePackageFolder(activity);
            }

            @Override
            public void doInBackground() {
                sUtils.sleep(1);
                sUtils.copy(new File(apk), new File(PackageData.getPackageDir(activity), name + ".apk"));
            }

            @SuppressLint("StringFormatInvalid")
            @Override
            public void onPostExecute() {
                hideProgress(linearLayout, textView);
                new MaterialAlertDialogBuilder(activity)
                        .setIcon(icon)
                        .setTitle(name)
                        .setMessage(activity.getString(R.string.export_apk_summary, PackageData.getPackageDir(activity)))
                        .setNegativeButton(activity.getString(R.string.cancel), (dialog, id) -> {
                        })
                        .setPositiveButton(activity.getString(R.string.share), (dialog, id) -> {
                            Uri uriFile = FileProvider.getUriForFile(activity,
                                    BuildConfig.APPLICATION_ID + ".provider", new File(PackageData.getPackageDir(activity), name + ".apk"));
                            Intent shareScript = new Intent(Intent.ACTION_SEND);
                            shareScript.setType("application/java-archive");
                            shareScript.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.shared_by, name));
                            shareScript.putExtra(Intent.EXTRA_TEXT, activity.getString(R.string.share_message, BuildConfig.VERSION_NAME));
                            shareScript.putExtra(Intent.EXTRA_STREAM, uriFile);
                            shareScript.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            activity.startActivity(Intent.createChooser(shareScript, activity.getString(R.string.share_with)));
                        }).show();
            }
        }.execute();
    }

    public static void exportingBundleTask(LinearLayout linearLayout, MaterialTextView textView, String apk, String name, Drawable icon, Activity activity) {
        new sExecutor() {

            @SuppressLint("StringFormatInvalid")
            @Override
            public void onPreExecute() {
                showProgress(linearLayout, textView, activity.getString(R.string.exporting_bundle, name) + "...");
                PackageData.makePackageFolder(activity);
            }

            @Override
            public void doInBackground() {
                sUtils.sleep(1);
                List<File> mFiles = new ArrayList<>();
                for (final String splitApps : SplitAPKInstaller.splitApks(apk)) {
                    mFiles.add(new File(apk + "/" + splitApps));
                }
                Utils.zip(PackageData.getPackageDir(activity) + "/" + name + ".apkm", mFiles);
            }

            @SuppressLint("StringFormatInvalid")
            @Override
            public void onPostExecute() {
                hideProgress(linearLayout, textView);
                new MaterialAlertDialogBuilder(activity)
                        .setIcon(icon)
                        .setTitle(name)
                        .setMessage(activity.getString(R.string.export_bundle_summary, PackageData.getPackageDir(activity) + "/" + name + ".apkm"))
                        .setNegativeButton(activity.getString(R.string.cancel), (dialog, id) -> {
                        })
                        .setPositiveButton(activity.getString(R.string.share), (dialog, id) -> {
                            Uri uriFile = FileProvider.getUriForFile(activity,
                                    BuildConfig.APPLICATION_ID + ".provider", new File(PackageData.getPackageDir(activity) + "/" + name + ".apkm"));
                            Intent shareScript = new Intent(Intent.ACTION_SEND);
                            shareScript.setType("application/zip");
                            shareScript.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.shared_by, name));
                            shareScript.putExtra(Intent.EXTRA_TEXT, activity.getString(R.string.share_message, BuildConfig.VERSION_NAME));
                            shareScript.putExtra(Intent.EXTRA_STREAM, uriFile);
                            shareScript.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            activity.startActivity(Intent.createChooser(shareScript, activity.getString(R.string.share_with)));
                        }).show();
            }
        }.execute();
    }

    public static void disableApp(LinearLayout progressLayout, LinearLayout openApp, MaterialTextView progressMessage,
                                  MaterialTextView statusMessage, Activity activity) {
        new sExecutor() {

            @SuppressLint("StringFormatInvalid")
            @Override
            public void onPreExecute() {
                showProgress(progressLayout, progressMessage, sPackageUtils.isEnabled(Common.getApplicationID(), activity) ?
                        activity.getString(R.string.disabling, Common.getApplicationName()) + "..." :
                        activity.getString(R.string.enabling, Common.getApplicationName()) + "...");
            }

            @Override
            public void doInBackground() {
                sUtils.sleep(1);
                if (sPackageUtils.isEnabled(Common.getApplicationID(), activity)) {
                    Utils.runCommand("pm disable " + Common.getApplicationID());
                } else {
                    Utils.runCommand("pm enable " + Common.getApplicationID());
                }
            }

            @Override
            public void onPostExecute() {
                hideProgress(progressLayout, progressMessage);
                statusMessage.setText(sPackageUtils.isEnabled(Common.getApplicationID(), activity) ? R.string.disable : R.string.enable);
                openApp.setVisibility(sPackageUtils.isEnabled(Common.getApplicationID(), activity) ? View.VISIBLE : View.GONE);
                Common.reloadPage(true);
            }
        }.execute();
    }

    @SuppressLint("StringFormatInvalid")
    public static void uninstallSystemApp(LinearLayout linearLayout, MaterialTextView textView, Activity activity) {
        if (Utils.rootAccess()) {
            new MaterialAlertDialogBuilder(activity)
                    .setIcon(Common.getApplicationIcon())
                    .setTitle(activity.getString(R.string.uninstall_title, Common.getApplicationName()))
                    .setMessage(activity.getString(R.string.uninstall_warning))
                    .setCancelable(false)
                    .setNegativeButton(activity.getString(R.string.cancel), (dialog, id) -> {
                    })
                    .setPositiveButton(activity.getString(R.string.yes), (dialog, id) ->
                            new sExecutor() {

                                @Override
                                public void onPreExecute() {
                                    showProgress(linearLayout, textView, activity.getString(R.string.uninstall_summary, Common.getApplicationName()));
                                }

                                @Override
                                public void doInBackground() {
                                    sUtils.sleep(1);
                                    Utils.runCommand("pm uninstall --user 0 " + Common.getApplicationID());
                                }

                                @Override
                                public void onPostExecute() {
                                    PackageData.setRawData(activity);
                                    hideProgress(linearLayout, textView);
                                    activity.finish();
                                    Common.reloadPage(true);
                                }
                            }.execute())
                    .show();
        } else {
            Intent details = new Intent(activity, ADBUninstallActivity.class);
            activity.startActivity(details);
        }
    }

    public static List<String> getPermissions(String packageName, Context context) {
        List<String> perms = new ArrayList<>();
        try {
            if (getPermissionsGranted(packageName, context).size() > 1) {
                perms.addAll(getPermissionsGranted(packageName, context));
            }
            if (getPermissionsDenied(packageName, context).size() > 1) {
                perms.addAll(getPermissionsDenied(packageName, context));
            }
        } catch (NullPointerException ignored) {}
        return perms;
    }

    public static List<String> getPermissionsGranted(String packageName, Context context) {
        List<String> perms = new ArrayList<>();
        try {
            perms.add("Granted");
            for (int i = 0; i < Objects.requireNonNull(PackageData.getPackageInfo(packageName, context)).requestedPermissions.length; i++) {
                if ((Objects.requireNonNull(PackageData.getPackageInfo(packageName, context)).requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                    perms.add(Objects.requireNonNull(PackageData.getPackageInfo(packageName, context)).requestedPermissions[i]);
                }
            }
        } catch (NullPointerException ignored) {
        }
        return perms;
    }

    public static List<String> getPermissionsDenied(String packageName, Context context) {
        List<String> perms = new ArrayList<>();
        try {
            perms.add("Denied");
            for (int i = 0; i < Objects.requireNonNull(PackageData.getPackageInfo(packageName, context)).requestedPermissions.length; i++) {
                if ((Objects.requireNonNull(PackageData.getPackageInfo(packageName, context)).requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) == 0) {
                    perms.add(Objects.requireNonNull(PackageData.getPackageInfo(packageName, context)).requestedPermissions[i]);
                }
            }
        } catch (NullPointerException ignored) {
        }
        return perms;
    }

    public static List<ActivityInfo> getActivities(String packageName, Context context) {
        List<ActivityInfo> activities = new ArrayList<>();
        try {
            try {
                ActivityInfo[] list = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_ACTIVITIES).activities;
                activities.addAll(Arrays.asList(list));
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        } catch (NullPointerException ignored) {}
        return activities;
    }

    public static JSONObject getPackageDetails(String packageName, Context context) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("Name", PackageData.getAppName(packageName, context));
            obj.put("Package Name", packageName);
            obj.put("Version", sAPKUtils.getVersionName(sPackageUtils.getSourceDir(packageName, context), context));
            obj.put("Google Play", "https://play.google.com/store/apps/details?id=" + packageName);
            if (new File(sPackageUtils.getSourceDir(packageName, context)).getName().equals("base.apk") && SplitAPKInstaller
                    .splitApks(sPackageUtils.getParentDir(packageName, context)).size() > 1) {
                obj.put("App Bundle", true);
                obj.put("Bundle Size", PackageData.getBundleSize(sPackageUtils.getParentDir(packageName, context)));
                JSONArray apks = new JSONArray();
                for (String apk : SplitAPKInstaller
                        .splitApks(sPackageUtils.getParentDir(packageName, context))) {
                    apks.put(apk);
                }
                obj.put("Split APKs", apks);

            } else {
                obj.put("App Bundle", false);
                obj.put("APK Size", sAPKUtils.getAPKSize(sPackageUtils.getSourceDir(packageName ,context)));
            }
            obj.put("Installed", sPackageUtils.getInstalledDate(packageName, context));
            obj.put("Last updated", sPackageUtils.getUpdatedDate(packageName, context));
            JSONObject permissions = new JSONObject();
            JSONArray granted = new JSONArray();
            for (String grantedPermissions : PackageDetails.getPermissionsGranted(packageName, context)) {
                if (!grantedPermissions.equals("Granted")) {
                    granted.put(grantedPermissions);
                }
            }
            permissions.put("Granted", granted);
            JSONArray denied = new JSONArray();
            for (String deniedPermissions : PackageDetails.getPermissionsDenied(packageName, context)) {
                if (!deniedPermissions.equals("Denied")) {
                    denied.put(deniedPermissions);
                }
            }
            permissions.put("Denied", denied);
            obj.put("Permissions", permissions);
            return obj;
        } catch (JSONException ignored) {
        }
        return null;
    }

    private static void showProgress(LinearLayout linearLayout, MaterialTextView textView, String message) {
        textView.setText(message);
        textView.setVisibility(View.VISIBLE);
        linearLayout.setVisibility(View.VISIBLE);
    }

    private static void hideProgress(LinearLayout linearLayout, MaterialTextView textView) {
        textView.setVisibility(View.GONE);
        linearLayout.setVisibility(View.GONE);
    }

}