package com.origin.launcher;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.graphics.Color;
import android.widget.TextView;
import java.util.ArrayList;
import android.util.SparseBooleanArray;
import androidx.appcompat.app.AppCompatActivity;
import com.origin.launcher.animation.DynamicAnim;

public class AutoFixActivity extends AppCompatActivity {
    private static final String KEY_SELECTED_VERSIONS = "selected_versions";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_autofix);
        setTitle("Shader Options");
        
        DynamicAnim.applyPressScaleRecursively(findViewById(android.R.id.content));
        
        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            saveVersions();
            finish();
        });
        backButton.setImageResource(R.drawable.ic_arrow_down);
        backButton.setRotation(0);
        
        ListView versionList = findViewById(R.id.version_list);
        String[] versions = {"v1.18.30", "v1.19.60", "v1.20.80", "v1.21.20", "v1.21.110+"};
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, versions);
        versionList.setAdapter(adapter);
        versionList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        
        for (int i = 0; i < adapter.getCount(); i++) {
            TextView textView = (TextView) adapter.getView(i, null, versionList);
            textView.setTextColor(Color.WHITE);
        }
        
        if (savedInstanceState != null) {
            ArrayList<String> savedVersions = savedInstanceState.getStringArrayList(KEY_SELECTED_VERSIONS);
            if (savedVersions != null) {
                for (int i = 0; i < versions.length; i++) {
                    if (savedVersions.contains(versions[i])) {
                        versionList.setItemChecked(i, true);
                    }
                }
            }
        }
        
        Button selectAllButton = findViewById(R.id.btn_select_all);
        if (selectAllButton != null) {
            selectAllButton.setOnClickListener(v -> {
                for (int i = 0; i < versionList.getCount(); i++) {
                    versionList.setItemChecked(i, true);
                }
            });
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveVersions(outState);
    }
    
    @Override
    public void onBackPressed() {
        saveVersions();
        super.onBackPressed();
    }
    
    private void saveVersions() {
        ListView versionList = findViewById(R.id.version_list);
        SparseBooleanArray checked = versionList.getCheckedItemPositions();
        String[] versions = {"v1.18.30", "v1.19.60", "v1.20.80", "v1.21.20", "v1.21.110+"};
        ArrayList<String> selected = new ArrayList<>();
        for (int i = 0; i < checked.size(); i++) {
            if (checked.valueAt(i)) {
                selected.add(versions[checked.keyAt(i)]);
            }
        }
        FeatureSettings.getInstance().setAutofixVersionsSafe(selected.toArray(new String[0]));
    }
    
    private void saveVersions(Bundle outState) {
        ListView versionList = findViewById(R.id.version_list);
        SparseBooleanArray checked = versionList.getCheckedItemPositions();
        String[] versions = {"v1.18.30", "v1.19.60", "v1.20.80", "v1.21.20", "v1.21.110+"};
        ArrayList<String> selected = new ArrayList<>();
        for (int i = 0; i < checked.size(); i++) {
            if (checked.valueAt(i)) {
                selected.add(versions[checked.keyAt(i)]);
            }
        }
        outState.putStringArrayList(KEY_SELECTED_VERSIONS, selected);
    }
}