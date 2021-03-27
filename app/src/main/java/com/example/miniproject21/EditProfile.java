package com.example.miniproject21;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EditProfile extends AppCompatActivity  {

    int spice;

    AutoCompleteTextView autoAllergen;
    AutoCompleteTextView autoVeg;

    String[] veg={"Any", "Jain", "Vegetarian", "Non-Vegetarian"};
    ArrayList<String> allergens;
    ArrayList<String> mArrayList;

    ListView allergenListView;
    EditProfileAdapter mAdapter;
    ArrayAdapter<String> adapterAllergens;
    ArrayAdapter<String> adapterVeg;

    int preference = 0;

    SeekBar s;

    public void dismissAllergen(View view) {
        ImageView mImageView = (ImageView) view;

        int index = Integer.parseInt(mImageView.getTag().toString());

        mArrayList.remove(index);
        mAdapter.notifyDataSetChanged();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();

        s = (SeekBar) findViewById(R.id.seekBar);
        s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                spice = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        mArrayList = new ArrayList<>();
        mAdapter = new EditProfileAdapter(this, mArrayList);
        allergenListView = findViewById(R.id.listViewAllergens);
        allergenListView.setAdapter(mAdapter);


        autoVeg = findViewById(R.id.autoVeg);
        adapterVeg = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, veg);
        autoVeg.setAdapter(adapterVeg);
        autoVeg.setThreshold(1);
        autoVeg.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3){
                autoVeg.setText(arg0.getItemAtPosition(arg2).toString());

                switch (arg0.getItemAtPosition(arg2).toString()) {
                    case "Jain":
                        preference = 0;
                        break;
                    case "Vegetarian":
                        preference = 1;
                        break;
                    case "Any":
                    case "Non-Vegetarian":
                        preference = 2;
                        break;
                }
            }
        });


        allergens = new ArrayList<>();
        autoAllergen = findViewById(R.id.autoAllergens);
        adapterAllergens = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, allergens);
        autoAllergen.setAdapter(adapterAllergens);
        autoAllergen.setThreshold(1);
        autoAllergen.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (mArrayList.contains(adapterView.getItemAtPosition(i).toString())) {
                    autoAllergen.setText("");
                    Toast.makeText(EditProfile.this, "Item already present in the list", Toast.LENGTH_SHORT).show();

                }
                else {
                    autoAllergen.setText("");
                    mArrayList.add(adapterView.getItemAtPosition(i).toString());
                    mAdapter.notifyDataSetChanged();

                }
            }
        });

        DocumentReference allergenDocumentRef = db.collection("predictableItems").document("#Allergens");
        allergenDocumentRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot mDocSnap = task.getResult();
                    if (mDocSnap.exists()) {
                        ArrayList<String> arrayList = (ArrayList<String>) mDocSnap.get("items");
                        allergens.clear();
                        allergens.addAll(arrayList);

                        adapterAllergens.notifyDataSetChanged();

                    }
                }
            }
        });


        // FETCH USER DATA IF EXISTS
        assert mUser != null;
        final DocumentReference mDocumentReference = db.collection("Users").document(mUser.getUid());
        mDocumentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot mDocSnap = task.getResult();
                    if (mDocSnap.exists()) {
                        if (mDocSnap.get("spice") != null && mDocSnap.get("veg") != null && mDocSnap.get("allergens") != null) {
                            spice = Integer.parseInt(mDocSnap.get("spice").toString());
                            s.setProgress(spice);

                            mArrayList.addAll((ArrayList<String>) mDocSnap.get("allergens"));
                            mAdapter.notifyDataSetChanged();

                            autoVeg.setText(mDocSnap.get("veg").toString());

                            preference = Integer.parseInt(mDocSnap.get("preference").toString());
                        }
                    }
                }
            }
        });

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void saveChanges(View view) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> mMap = new HashMap<>();
        mMap.put("preference", preference);
        mMap.put("spice", spice);
        mMap.put("veg", autoVeg.getText().toString());
        mMap.put("allergens", mArrayList);

        FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();

        assert mUser != null;
        db.collection("Users").document(mUser.getUid()).set(mMap, SetOptions.merge()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Intent intent = new Intent(EditProfile.this, HomePage.class);
                    startActivity(intent);
                    finish();
                } else {
                    assert task.getException() != null;
                    Toast.makeText(EditProfile.this, task.getException().toString(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        
    }
}
