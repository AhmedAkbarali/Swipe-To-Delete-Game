package com.example.swipe_to_delete_game;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;


/*
    This app tests three different swipe Threshold settings for swipe-to-delete.
    We do this using objects in a list.
    Setup parameters determines the number of trials.
 */
public class STDTask extends AppCompatActivity {

    final static String MYDEBUG = "MYDEBUG"; // for Log.i messages

    final String WORKING_DIRECTORY = "/STDData/";
    final String SD2_HEADER = "App,Participant,Session,Block,Group, Total Number of Trials, Trial Number, " +
            "Time_(s), Completion Rate(%), ErrorRate(%)\n";

    final String APP = "StD";

    String task;
    CoordinatorLayout coordinatorLayout;
    Button nextButton;
    BufferedWriter  sd2;
    File f1, f2, f3;
    String sd2Leader; // sd2Leader to identify conditions for data written to sd2 files.
    long startTime, endTime;
    int numberOfTrials;
    int count, currTrial;
    float errorRate;
    float swipeThreshold;
    String participantCode;
    String sessionCode;
    String groupCode;
    private ArrayList<String> items = new ArrayList<>();
    private RecyclerAdapter adapter;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        initViews();
        enableSwipeToDeleteAndUndo();

        //Populate list
        for(int i=0; i < 50; i++){
            items.add(String.valueOf(i+1));
        }

        //Start timer
        startTime = System.nanoTime();

        count = 0;

        //Unpack bundle
        Bundle b = getIntent().getExtras();
        participantCode = b.getString("participantCode");
        sessionCode = b.getString("sessionCode");
        groupCode = b.getString("groupCode");
        currTrial = b.getInt("currTrial");
        numberOfTrials = b.getInt("numberOfTrials");
        swipeThreshold = b.getFloat("swipeThreshold");
        task = b.getString("task");
        String title;
        if(currTrial % 2 == 0){
            title = ("Task " + task + ": Delete Even number items on list.");
        }else {
            title = ("Task " + task + ": Delete Odd number items.");
        }
        getSupportActionBar().setTitle(title);
        initFile();
    }

    private void initViews() {
        nextButton = findViewById(R.id.next);
        coordinatorLayout = findViewById(R.id.coordinatorLayout);
        recyclerView = findViewById(R.id.recyclerView);

        adapter = new RecyclerAdapter(items);
        recyclerView.setAdapter(adapter);
    }

    private void initFile(){
        // ===================
        // File initialization
        // ===================

        // make a working directory (if necessary) to store data files
        File dataDirectory = new File(Environment.getExternalStorageDirectory() + WORKING_DIRECTORY);
        if (!dataDirectory.exists() && !dataDirectory.mkdirs())
        {
            Log.e(MYDEBUG, "ERROR --> FAILED TO CREATE DIRECTORY: " + WORKING_DIRECTORY);
            super.onDestroy(); // cleanup
            this.finish(); // terminate
        }
        Log.i(MYDEBUG, "Working directory=" + dataDirectory);

        /*
         * The following do-loop creates data files for output and a string sd2Leader to write to the sd2
         * output files.  Both the filenames and the sd2Leader are constructed by combining the setup parameters
         * so that the filenames and sd2Leader are unique and also reveal the conditions used for the block of input.
         *
         * The block code begins "B01" and is incremented on each loop iteration until an available
         * filename is found.  The goal, of course, is to ensure data files are not inadvertently overwritten.
         */
        int blockNumber = 0;
        do
        {
            ++blockNumber;
            String blockCode = String.format(Locale.CANADA, "B%02d", blockNumber);
            String baseFilename = String.format("%s-%s-%s-%s-%s-%s-%s-%s", APP, task, participantCode,
                    sessionCode, blockCode, groupCode, String.valueOf(numberOfTrials), String.valueOf(currTrial));
            f1 = new File(dataDirectory, baseFilename + ".sd1");
            f2 = new File(dataDirectory, baseFilename + ".sd2");
            f3 = new File(dataDirectory, baseFilename + ".sd3");

            // also make a comma-delimited leader that will begin each data line written to the sd2 file
            sd2Leader = String.format("%s,%s,%s,%s,%s,%s,%s", APP, participantCode, sessionCode,
                    blockCode, groupCode, String.valueOf(numberOfTrials), String.valueOf(currTrial));
        } while (f1.exists() || f2.exists());

        try
        {
            sd2 = new BufferedWriter(new FileWriter(f2));
            // output header in sd2 file
            sd2.write(SD2_HEADER, 0, SD2_HEADER.length());
            sd2.flush();
        } catch (IOException e)
        {
            Log.e(MYDEBUG, "ERROR OPENING DATA FILES! e=" + e.toString());
            super.onDestroy();
            this.finish();
        } // end file initialization
    }

    private void enableSwipeToDeleteAndUndo() {
        SwipeToDeleteCallback swipeToDeleteCallback = new SwipeToDeleteCallback(this, 0.7f) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {


                final int position = viewHolder.getAdapterPosition();
                final String item = adapter.getData().get(position);

                adapter.removeItem(position);

                /*
                Snackbar snackbar = Snackbar
                        .make(coordinatorLayout, "Item was removed from the list.", Snackbar.LENGTH_LONG);
                snackbar.setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        adapter.restoreItem(item, position);
                        recyclerView.scrollToPosition(position);
                    }
                });

                snackbar.setActionTextColor(Color.YELLOW);
                snackbar.show();
                */

            }
        };

        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeToDeleteCallback);
        itemTouchhelper.attachToRecyclerView(recyclerView);
    }

    //handle button click
    public void nextClick(View v){
        if(v==nextButton){
            endTime = System.nanoTime() - startTime;

            //even or odd check
            int evenOrOdd = currTrial % 2;

            float completionRate;
            //Calculate completion rate.
            for(int i = 0; i < items.size(); i++){
                if(Integer.valueOf(items.get(i)) % 2 == evenOrOdd){
                    count++;
                }
            }
            completionRate =  1 - (count / 25f);
            //Launch next task, based on trial numbers. Write necessary info to disk
            count = 0;
            for(int i = 0; i < items.size(); i++){
                if(Integer.valueOf(items.get(i)) % 2 != evenOrOdd){
                    count++;
                }
            }
            errorRate = 1 - (count/25f);
            Bundle b = new Bundle();
            Intent i;
            //Determine which task is being loaded and change
            // currTrial, task string and swipeThreshold based on this
            if(numberOfTrials == currTrial){
                if (swipeThreshold == 0.7f){
                    b.putFloat("swipeThreshold", 0.5f);
                    b.putString("task", "Two");
                }else if (swipeThreshold == 0.5f){
                    b.putFloat("swipeThreshold", 0.2f);
                    b.putString("task", "Three");
                }
                b.putInt("currTrial", 1);
            }else{
                b.putInt("currTrial", currTrial + 1);
                b.putFloat("swipeThreshold", swipeThreshold);
                b.putString("task", task);
            }

            //If tasks are incomplete laucnch new task intent
            // else Go back to setup page once tasks are complete.
            if(numberOfTrials == currTrial && swipeThreshold == 0.2f){
                i = new Intent(getApplicationContext(), STDSetup.class);
            }else {
                i = new Intent(getApplicationContext(), STDTask.class);
            }

            //Format string to be written to file
            StringBuilder sd2Data = new StringBuilder(100);
            sd2Data.append(String.format("%s,", sd2Leader));

            // output time in seconds
            float d = endTime / 1000000000.0f;
            sd2Data.append(String.format(Locale.CANADA, "%.2f,", d));

            // output completion rate
            sd2Data.append(String.format(Locale.CANADA, "%.2f,", completionRate));

            // output error rate
            sd2Data.append(String.format(Locale.CANADA, "%.2f,", errorRate));


            // write to data files
            try
            {
                sd2.write(sd2Data.toString(), 0, sd2Data.length());
                sd2.flush();
            } catch (IOException e)
            {
                Log.e("MYDEBUG", "ERROR WRITING TO DATA FILE!\n" + e);
                super.onDestroy();
                this.finish();
            }

            b.putString("participantCode", participantCode);
            b.putString("sessionCode", sessionCode);
            b.putString("groupCode", groupCode);
            b.putInt("numberOfTrials", numberOfTrials);
            i.putExtras(b);
            startActivity(i);
            finish();
        }
    }
}
