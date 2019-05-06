package com.sowon.faceblindness_android;

import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        OptionActivity checklistActivity = new OptionActivity();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, checklistActivity).commit();
    }

    public void onClick_option(View view){
        OptionActivity optionActivity = new OptionActivity();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, optionActivity).commit();
    }
    public void onClick_category(View view){
        CategoryActivity categoryActivity = new CategoryActivity();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, categoryActivity).commit();
    }
    public void onClick_time(View view){
        TimeActivity timeActivity = new TimeActivity();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, timeActivity).commit();
    }
    public void onClick_timeline(View view){
        TimelineActivity timelineActivity = new TimelineActivity();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, timelineActivity).commit();
    }
    public void onClick_search(View view){
        SearchActivity searchActivity = new SearchActivity();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, searchActivity).commit();
    }
}