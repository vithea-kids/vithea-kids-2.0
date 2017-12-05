package com.l2f.vitheakids;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.l2f.vitheakids.Storage.ImageStorage;
import com.l2f.vitheakids.model.Answer;
import com.l2f.vitheakids.model.Child;
import com.l2f.vitheakids.model.Exercise;
import com.l2f.vitheakids.model.ExerciseLogInfo;
import com.l2f.vitheakids.model.PersonalMessage;
import com.l2f.vitheakids.model.Resource;
import com.l2f.vitheakids.model.SequenceExercises;
import com.l2f.vitheakids.model.SequenceLogInfo;
import com.l2f.vitheakids.rest.FetchChildInfo;
import com.l2f.vitheakids.task.LoadImageTask;
import com.l2f.vitheakids.task.ReadTask;
import com.l2f.vitheakids.task.StopRead;
import com.l2f.vitheakids.util.ExerciseMenuListWithoutImageAdapter;
import com.l2f.vitheakids.util.CanvasUtil;
import com.l2f.vitheakids.util.Prompting;
import com.unity3d.player.*;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Updated by Soraia Meneses Alarcão on 21/07/2017
 */

public class VitheaKidsActivity extends AppCompatActivity {

    private String TAG = VitheaKidsActivity.class.getSimpleName();

// **** DECLARATIONS *******************************************************************************

    // Unity stuff
    private UnityPlayer mUnityPlayer;

    // Layout stuff
    private LayoutInflater linflater;
    private ListView lv;
    private BaseAdapter mAdapter;

    // Bundle info
    public String userName;
    public String password;
    private String greetingMsg;
    private String exListMsg;
    public Child child = null;
    private ExerciseLogInfo currentExerciseLog;
    private SequenceLogInfo currentSequenceLog;

    // Exercises
    private long currentSequenceId;
    private int currentExercisePosition;
    private int attempts = 0;
    private boolean inExercise;
    private boolean inHomeScreen = true;
    private boolean inSequenceScreen;
    public boolean promptingActive;
    public boolean reinforcementActive;
    private List<String> currentAnswers;
    private Button rightAnswerButton;
    private String lastInstruction;
    private List<Exercise> exercises;
    private Date timestampBeginExercise;
    private Date timestampEndExercise;

    private DateFormat dateFormat;

    private View characterContainer;
    private ActionBar actionBar;
    private ImageView rightAnswer;

    private ImageStorage imageStorage;

    private String seqName;

    Logger logger = Log4jHelper.getLogger( "VitheaKidsActivity" );
    // *************************************************************************************************

    public void initActivePrompting(Boolean state){
        promptingActive = state;
    }
    public void initActiveReinforcement(Boolean state){
        reinforcementActive = state;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        imageStorage = (ImageStorage) getApplication();


        getWindow().setFormat(PixelFormat.RGBX_8888); // <--- This makes xperia play happy
        setContentView(R.layout.main);

        initUnityCharacter();   // conf unity player
        initViews();            // conf views and layouts


        new FetchChildInfo(VitheaKidsActivity.this).execute();


        dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    }

    @Override
    protected void onStop(){

        super.onStop();
        finish();
    }

    private String logExercise(Child child, boolean isCorrect){
        String logString = "exerciseID: " + exercises.get(currentExercisePosition).getId() + "; ";
        logString += "timestampBegin: " + dateFormat.format(timestampBeginExercise) + "; ";
        logString += "timestampEnd: " + dateFormat.format(timestampEndExercise) + "; ";

        logString += "numberOfWrongAttempts: " + attempts + "; ";

        if (isCorrect) {
            logString += "correct: true; skipped: false; ";
        }
        else {
            logString += "correct: false; skipped: true; ";
        }

        logString += "promptingStrategy: " + child.getPrompting().getPromptingStrategy() + "; ";
        logString += "reinforcementStrategy: " + child.getReinforcement().getReinforcementStrategy() + "; ";
        logString += "\n";
        return logString;
    }

    private String logSequence() {
        return "";
    }

    private void initViews() {

        characterContainer = findViewById(R.id.characterContainer);

        // Orientations
        if (getResources().getConfiguration().orientation  == Configuration.ORIENTATION_PORTRAIT) {
            int height = (int) (CanvasUtil.getHeight(getWindowManager().getDefaultDisplay()) * 0.45);
            characterContainer.getLayoutParams().height = height;
        } else {
            characterContainer.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
        }

        // Unity layout
        FrameLayout layout = (FrameLayout) findViewById(R.id.view1);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        View view =  mUnityPlayer.getView();
        layout.addView(view, 0, lp);

        linflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view1 = (View) linflater.inflate(R.layout.repeat_button, null);
        layout.addView(view1);

        layout.getChildAt(0).bringToFront();
        layout.getChildAt(0).invalidate();

        layout.getChildAt(1).bringToFront();
        layout.getChildAt(0).invalidate();

        Log.d("layout",layout.getChildAt(0).toString());
       /* linflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view1 = (View) linflater.inflate(R.layout.repeat_button, null);

        layout.addView(view1);*/


    }
    private void initUnityCharacter() {
        mUnityPlayer = new UnityPlayer(this);
        int glesMode = mUnityPlayer.getSettings().getInt("gles_mode", 1);
        boolean trueColor8888 = false;
        mUnityPlayer.init(glesMode, trueColor8888);
    }
    private void setupActionBar(String title) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setTitle(title);
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        actionBar.show();
    }
    private void hideActionBar() {
        actionBar.hide();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // To support deep linking, we need to make sure that the client can get access to
        // the last sent intent. The clients access this through a JNI api that allows them
        // to get the intent set on launch. To update that after launch we have to manually
        // replace the intent with the one caught here.
        setIntent(intent);
    }
    // Quit Unity
    @Override
    protected void onDestroy() {
        mUnityPlayer.quit();
        super.onDestroy();
    }
    // Pause Unity
    @Override
    protected void onPause() {
        super.onPause();
        mUnityPlayer.pause();
    }
    // Resume Unity
    @Override
    protected void onResume() {
        super.onResume();
        mUnityPlayer.resume();
    }
    // Low Memory Unity
   @Override
    public void onLowMemory() {
        super.onLowMemory();
       // mUnityPlayer.lowMemory();
    }
    // Trim Memory Unity
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level == TRIM_MEMORY_RUNNING_CRITICAL) {
            //mUnityPlayer.lowMemory();
        }
    }
    // This ensures the layout will be correct.
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mUnityPlayer.configurationChanged(newConfig);
    }
    // Notify Unity of the focus change.
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        mUnityPlayer.windowFocusChanged(hasFocus);
    }
    // For some reason the multiple key event type is not supported by the ndk.
    // Force event injection by overriding dispatchKeyEvent().
    @Override
   public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_MULTIPLE)
            return mUnityPlayer.injectEvent(event);
        return super.dispatchKeyEvent(event);
    }
    // Pass any events not handled by (unfocused) views straight to UnityPlayer
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return mUnityPlayer.injectEvent(event);
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) { return mUnityPlayer.injectEvent(event); }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mUnityPlayer.injectEvent(event);
    }
    /*API12*//*
    public boolean onGenericMotionEvent(MotionEvent event) { return mUnityPlayer.injectEvent(event); }
*/
// **** View setters - methods that change the view ************************************************

    public void setSequenceSelectionView() {
        Log.d("entreiEKLECTION", "setSequenceSelectionView called");

        setupActionBar(getString(R.string.sequences_of_exercise));

        Log.d(TAG, "setSequenceSelectionView called");

        inHomeScreen = false;
        inSequenceScreen = true;

        // Clear rightFrameLayout
        LinearLayout rightFrameLayout = (LinearLayout) findViewById(R.id.rightFrame);
        rightFrameLayout.removeAllViews();

        ViewGroup.LayoutParams layoutParams = rightFrameLayout.getLayoutParams();
        layoutParams.height = CanvasUtil.getHeight(getWindowManager().getDefaultDisplay());

        linflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Inflate view
        LinearLayout  view = (LinearLayout ) linflater.inflate(R.layout.exercise_menu_layout, null);
        rightFrameLayout.addView(view);
        Log.d("DEBUG", "list view by inflater" + view);

        // Setup adapter
        lv = (ListView) findViewById(R.id.list_by_type);            // exercise_menu_layout
        mAdapter = new ExerciseMenuListWithoutImageAdapter(this);   // util folder

        // Load data
        if(this.child.getSequencesList().size() == 0){
            TextView textView = new TextView(this.getApplicationContext());
            textView.setText(R.string.noClasses);
            textView.setTextColor(Color.BLACK);
            LayoutParams par = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
            par.setMargins(50,0,0,0);
            textView.setLayoutParams(par);
            view.addView(textView);
        }
        int i = 0;
        for (SequenceExercises sInfo : this.child.getSequencesList()) {
            // Shows only sequences with exercises
            if (sInfo.getSequenceExercises().size() > 0) {
                ((ExerciseMenuListWithoutImageAdapter) mAdapter).add(sInfo.getName());
            }
            i++;
        }

        lv.setAdapter(mAdapter);
        final VitheaKidsActivity  act = this;
        // Setup onClickListener
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Log.d(TAG, position + " " + id);
                SequenceExercises seq = child.getSequencesList().get(position);
                currentSequenceId = seq.getSequenceId();

                if(child.getSequenceExercisesPreferences().getSequenceExercisesOrder().equals("RANDOM")){
                    Log.d("shuffleEnable", "true");
                    Collections.shuffle(seq.getSequenceExercises());
                }

                exercises = seq.getSequenceExercises();
                currentExercisePosition = 0;
                timestampBeginExercise = new Date();
                inExercise = true;          // Start exercises
                inHomeScreen = false;       // Not in home screen
                inSequenceScreen = false;   // Not in sequence screen

                seqName = seq.getName();

                List<Resource> resources = child.getAllResourcesBySeqPosition(position);
                new LoadImageTask(resources,seq.getName(),act).execute();

                         // Draw view exercise
            }
        });

    }
    public void setExerciseView() {
        //stop character taklk
        new StopRead().execute();

        setupActionBar(getString(R.string.exercise));

        // trick because of the next button
        inExercise = true;

        // Clear rightFrameLayout
        LinearLayout rightFrameLayout = (LinearLayout) findViewById(R.id.rightFrame);
        rightFrameLayout.removeAllViews();

        ViewGroup.LayoutParams layoutParams = rightFrameLayout.getLayoutParams();
        layoutParams.height = CanvasUtil.getHeight(getWindowManager().getDefaultDisplay());
        linflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        ScrollView scroll_view = (ScrollView) linflater.inflate(R.layout.scroll, null);
        rightFrameLayout.addView(scroll_view);
        LinearLayout container = (LinearLayout) findViewById(R.id.scroll_container);

        // Set exercise info
        if (child != null) {
            Exercise exercise = exercises.get(currentExercisePosition);

            // TODO TYPE OF EXERCISES - in the future factory?
            if (exercise.getType().equals("TEXT")) {
                setSimpleMultipleChoiceExerciseView(exercise, container);   //text
            } else if (exercise.getType().equals("IMAGE")) {
                setImageMultipleChoiceExerciseView(exercise, container);    // image
            }

            // Exercises navigation
            View navigationView = linflater.inflate(R.layout.navigation_view, null);
            container.addView(navigationView);

            // Send to Animated Character
            readWithOrWithoutEmotion(child, exercise.getQuestion().getQuestionDescription());
            lastInstruction = exercise.getQuestion().getQuestionDescription();
/*
            ImageView repeat_view = (ImageView) findViewById(R.id.repeat_button_view);

            repeat_view.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                   // Toast.makeText(getApplicationContext(), lastInstruction, Toast.LENGTH_SHORT).show();
                    readWithOrWithoutEmotion(child, lastInstruction);
                }
            });*/
        }
    }
    /**
     * One image with four button options
     * @param exercise
     * @param container
     */
    public void setSimpleMultipleChoiceExerciseView(Exercise exercise, LinearLayout container)  {
        rightAnswerButton = null;

        // Layout
        View ex_view = (View) linflater.inflate(R.layout.exercise_long_options_layout, null);
        container.addView(ex_view);

        // Question

        TextView questionText = (TextView) findViewById(R.id.questionText);
        if (!child.getSequenceExercisesPreferences().getSequenceExerciseCapitalization().equals("DEFAULT")) {
            questionText.setText(exercise.getQuestion().getQuestionDescription().toUpperCase());
        } else {
            questionText.setText(exercise.getQuestion().getQuestionDescription());
        }

        // Stimulus
        Resource stimulus = exercise.getQuestion().getStimulus();


        if (stimulus!=(null)) {

                    byte[] image  = imageStorage.getImage(seqName,stimulus.getResourceId());
                    ImageView img = (ImageView) findViewById(R.id.stimulusImage);
                    setImageOfView(img,image);
        }
      else {
            ImageView img = (ImageView) findViewById(R.id.stimulusImage);
            img.setVisibility(View.GONE);
            LinearLayout layout = (LinearLayout) findViewById(R.id.layoutButtonText);
            //change top margin
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)layout.getLayoutParams();
            params.setMargins(0, 30, 0, 0);
            layout.setLayoutParams(params);        }

        // Exercise Options
        // Init Buttons given the number of options
        List<Answer> answers = exercise.getAnswers();
        List<Integer> idButtons = new ArrayList<>(Arrays.asList(R.id.button1, R.id.button2, R.id.button3, R.id.button4));
        List<Button> buttonList = new ArrayList<>();
        int numberAnswers = answers.size();

        if (!answers.isEmpty()) {
            Collections.shuffle(answers);

            if (numberAnswers > 4) numberAnswers = 4;

            for (int i = 0; i < numberAnswers; i++) {
                Button btn = (Button) findViewById(idButtons.get(i));
                Answer answer = answers.get(i);
                if (!child.getSequenceExercisesPreferences().getSequenceExerciseCapitalization().equals("DEFAULT")) {
                    btn.setText(answer.getAnswerDescription().toUpperCase());
                } else {
                    btn.setText(answer.getAnswerDescription());
                }
                btn.setVisibility(View.VISIBLE);

                if (answer.getAnswerId() == exercise.getRightAnswer().getAnswerId()) { //right answer
                    btn.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            rightAnswerHandler(v, child);
                        }
                    });
                    rightAnswerButton = btn;

                } else {
                    btn.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            distractorHandler(v);
                        }
                    });
                }
                buttonList.add(btn);
            }
            currentAnswers = new ArrayList<>();
            for (int i = 0; i < numberAnswers; i++) {
                currentAnswers.add(buttonList.get(i).getText().toString().toUpperCase());
            }
        }
            // Prompting
            // TODO REVIEW
            if (promptingActive && child.getPrompting() != null) {
                if (child.getPrompting().getPromptingStrategy().equals("ALWAYS")) {
                    if (child.getPrompting().getPromptingColor()) {
                        Prompting.setButtonColor(this.getApplicationContext(), rightAnswerButton);
                    }
                    if (child.getPrompting().getPromptingSize()) {
                        Prompting.setSizeText(this.getApplicationContext(),rightAnswerButton);
                    }
                    if (child.getPrompting().getPromptingScratch()) {
                        for (Button v : buttonList) {
                            if (v != rightAnswerButton) {
                                Prompting.scratchText(this.getApplicationContext(),v);
                            }
                        }
                    }
                    if (child.getPrompting().getPromptingRead()) {
                        Prompting.readAnswers(currentAnswers);
                    }
                }
            }

        }
    /***}
     * Multiple images
     * @param exercise
     * @param container
     */
    private void setImageMultipleChoiceExerciseView(Exercise exercise, LinearLayout container)  {
        View ex_view = (View) linflater.inflate(R.layout.exercise_multiple_images, null);
        container.addView(ex_view);

        // Question & stimulus
        TextView questionText = (TextView) findViewById(R.id.questionText);
        TextView stimulusText = (TextView) findViewById(R.id.stimulusText);
        String stimulusTextContent = exercise.getQuestion().getStimulusText();

        if (!child.getSequenceExercisesPreferences().getSequenceExerciseCapitalization().equals("DEFAULT")) {
            questionText.setText(exercise.getQuestion().getQuestionDescription().toUpperCase());
            stimulusText.setText(stimulusTextContent == null ? "" : stimulusTextContent.toUpperCase());
        } else {
            questionText.setText(exercise.getQuestion().getQuestionDescription());
            stimulusText.setText(stimulusTextContent == null ? "" : stimulusTextContent.toUpperCase());

        }

        // Stimulus


        // TODO REVIEW number answers
        List<Answer> answers = exercise.getAnswers();
        List<Integer> idImageViews = new ArrayList<>(Arrays.asList(R.id.first, R.id.second, R.id.third, R.id.four));
        List<ImageView> optionsList = new ArrayList<>();

        if (!answers.isEmpty()) {
            Collections.shuffle(answers);
            int numberAnswers = answers.size();

            // FIXME Verify if will be possible to have more than 4 answers
            if(numberAnswers > 4) numberAnswers = 4;



            for (int i = 0; i < numberAnswers; i++) {
                ImageView option = (ImageView) findViewById(idImageViews.get(i));
                Answer answer = answers.get(i);
                Resource answerImage = exercise.getAnswers().get(i).getStimulus();
                String path = answerImage != null ? answerImage.getResourcePath() : "";

                if (!path.isEmpty()) {
                    String domain = getString(R.string.resources_addr_kids);
                    path = domain + path;

                    byte[] bitmap = imageStorage.getImage(seqName, answerImage.getResourceId());
                    setImageOfView(option,bitmap);
                }

                option.setVisibility(View.VISIBLE);

                if (answer.getAnswerId() == exercise.getRightAnswer().getAnswerId()) {
                    option.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            rightAnswerHandler(v, child);


                        }
                    });
                    rightAnswer=option;



                } else {
                    option.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            distractorHandler(v);

                        }
                    });
                }
                optionsList.add(option);
            }
        }

        // TODO Prompting


        // TODO REVIEW
        if (promptingActive && child.getPrompting() != null) {
            if (child.getPrompting().getPromptingStrategy().equals("ALWAYS")) {
               if (child.getPrompting().getPromptingColor()) {
                   Prompting.setImageColor(this.getApplicationContext(), rightAnswer);
              }
               if (child.getPrompting().getPromptingSize()) {
                   Prompting.setImageSize(this.getApplicationContext(), rightAnswer);
               }

                if (child.getPrompting().getPromptingScratch()) {
                    for (ImageView v : optionsList) {
                        if (v != rightAnswer) {
                            Prompting.scratchImage(this.getApplicationContext(),v);
                        }
                    }
                }
            }
        }
    }

    public void setReinforcementView() {
        new StopRead().execute();//stop character talk and also  clean the text variable on unity

        Resource res;
        //// FIXME: 18/10/2017 
        try {
            res = child.getReinforcement().getReinforcementResource();
        } catch (NullPointerException e) {
            res = null;
        }

        if (reinforcementActive && res!=null) {
            hideActionBar(); // TODO maybe not hide?


            playMessage(child, "SEQUENCE_REINFORCEMENT");

            // Clear rightFrameLayout
            LinearLayout rightFrameLayout = (LinearLayout) findViewById(R.id.rightFrame);
            rightFrameLayout.removeAllViews();

            ViewGroup.LayoutParams layoutParams = rightFrameLayout.getLayoutParams();
            layoutParams.height = CanvasUtil.getHeight(getWindowManager().getDefaultDisplay());
            linflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            ScrollView scroll_view = (ScrollView) linflater.inflate(R.layout.scroll, null);
            rightFrameLayout.addView(scroll_view);


            View ref_view = (View) linflater.inflate(R.layout.reinforcement_view, null);
            LinearLayout container = (LinearLayout) findViewById(R.id.scroll_container);
            container.addView(ref_view);


            ImageView image = (ImageView) findViewById(R.id.reinforcement_image);
            byte[] bitmap = imageStorage.getImage(seqName, res.getResourceId());
            Log.d("reinforcement", Integer.toString(bitmap.length));
            //setImageOfView(image,bitmap);
            Glide.with(VitheaKidsActivity.this).load(bitmap).apply(RequestOptions.skipMemoryCacheOf(true)).apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE)).into(image);

            image.setVisibility(View.VISIBLE);
            // equivalent to next button
            ImageButton closeReinforcement = (ImageButton) findViewById(R.id.closeReinforcement);
            closeReinforcement.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    nextExerciseHandler();
                }
            });
        }
    }
    // last screen when show basic stats
    public void setFinalResultsView() {

        // Clear rightFrameLayout
        LinearLayout rightFrameLayout = (LinearLayout) findViewById(R.id.rightFrame);
        rightFrameLayout.removeAllViews();

        android.widget.RelativeLayout.LayoutParams para = (android.widget.RelativeLayout.LayoutParams) rightFrameLayout.getLayoutParams();
        para.height = CanvasUtil.getHeight(getWindowManager().getDefaultDisplay());

        linflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        RelativeLayout results_view = (RelativeLayout) linflater.inflate(R.layout.end_exercises, null);

        ScrollView scroll_view = (ScrollView) linflater.inflate(R.layout.scroll, null);
        rightFrameLayout.addView(scroll_view);

        LinearLayout container = (LinearLayout) findViewById(R.id.scroll_container);
        container.addView(results_view);

        TextView totalViewResult = (TextView) findViewById(R.id.totalViewResult);
        totalViewResult.setText(Integer.valueOf(currentSequenceLog.getTotalCount()).toString());

        TextView skippedViewResult = (TextView) findViewById(R.id.skippedViewResult);
        skippedViewResult.setText(Integer.valueOf(currentSequenceLog.getSkippedCount()).toString());

        TextView correctViewResult = (TextView) findViewById(R.id.correctViewResult);
        correctViewResult.setText(Integer.valueOf(currentSequenceLog.getCorrectCount()).toString());

        Float distractorAvg = Float.valueOf(currentSequenceLog.getDistractorHitsAvg());
        TextView distractorViewResult = (TextView) findViewById(R.id.distractorViewResult);
        distractorViewResult.setText(String.format("%.2f", distractorAvg).toString());

    }
    public void setNavigationView() {

        ImageButton next = (ImageButton) findViewById(R.id.nextExerciseButton);
        TextView nextText = (TextView) findViewById(R.id.nextExerciseTextView);
        ImageButton previous = (ImageButton) findViewById(R.id.previousExerciseButton);
        TextView previousText = (TextView) findViewById(R.id.previousExerciseTextView);
        ImageButton end = (ImageButton) findViewById(R.id.endExerciseButton);
        TextView endText = (TextView) findViewById(R.id.endExerciseTextView);

        Boolean isLastExercise = currentExercisePosition == exercises.size() - 1;
        Log.d("POSICAO DO EXERCICIO",Integer.toString(currentExercisePosition) );

        if (isLastExercise && next != null) {
            next.setVisibility(View.INVISIBLE);
            nextText.setVisibility(View.INVISIBLE);
            end.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    endHandler();
                }
            });
        } else {
            Log.d("PROXIMO",Integer.toString(currentExercisePosition) );
            next.setVisibility(View.VISIBLE);
            end.setVisibility(View.INVISIBLE);
            endText.setVisibility(View.INVISIBLE);
            next.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    nextExerciseHandler();
                }
            });
        }

        Boolean isFirstExercise = currentExercisePosition == 0;
        if (isFirstExercise && previous != null) {
            previous.setVisibility(View.INVISIBLE);
            previousText.setVisibility(View.INVISIBLE);
        } else {
            previous.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    previousExerciseHandler();
                }
            });
        }
    }

//**** Region Handlers - what happens when you do some actions ************************************

    protected void rightAnswerHandler(View v, Child child) {
        timestampEndExercise = new Date();
        logger.info(logExercise(child, true));
        inExercise = false;
        playMessage(child, "EXERCISE_REINFORCEMENT"); // TODO Only reinforcement?
        if(reinforcementActive) playReinforcement(child);
        else nextExerciseHandler();
        attempts = 0;
    }
    private void playReinforcement(Child child) { // TODO child.getReinforcement() - efficiency
        switch (child.getReinforcement().getReinforcementStrategy()) {
            case "ALWAYS":
                setReinforcementView();
                break;
            case "AT_FIRST":
                if(attempts == 0) {
                    setReinforcementView();
                }
                else nextExerciseHandler();
                break;
            case "OFF": // TODO OFF Reinforcement
            default:
                nextExerciseHandler();
                break;
        }
    }
    public void playMessage(Child child, String exercise_reinforcement) {
        //Exercise reinforcement
        List<PersonalMessage> personalMessages = child.getPersonalMessageList();
        String sentence = "";
        for (PersonalMessage message : personalMessages) {
            if (message.getMessageType().equals(exercise_reinforcement)) {
                sentence = message.getMessage();
                Log.d("greetings", sentence);
                break;
            }
        }

        // if generic, must check if it should say it worth joy or sadness
        if (!sentence.isEmpty()) {
            readWithOrWithoutEmotion(child,sentence);
            lastInstruction = sentence;
        }
    }
    protected void distractorHandler(View v) {
        attempts++;

        // TODO REVIEW AND REFACTOR PROMPTING
        // Prompting
        if (promptingActive && child.getPrompting() != null ) {
            Exercise exercise = exercises.get(currentExercisePosition);
            Boolean isTextExercise = exercise.getType().equals("TEXT");
            Boolean isImageExercise = exercise.getType().equals("IMAGE");

            if (child.getPrompting().getPromptingStrategy().equals("IF_NEEDED") || child.getPrompting().getPromptingStrategy().equals("ALWAYS")) {
                if (child.getPrompting().getPromptingHide()) {
                    v.setVisibility(View.INVISIBLE);
                    if(isImageExercise){
                        FrameLayout fr = (FrameLayout) v.getParent();
                        ImageView im  = (ImageView) fr.getChildAt(1);
                        im.setVisibility(View.INVISIBLE);
                    }
                    if(isTextExercise){
                        currentAnswers.remove(((Button) v).getText().toString().toUpperCase());}
                }
                else {
                    if (child.getPrompting().getPromptingScratch() && isTextExercise) {
                        Prompting.scratchText(this.getApplicationContext(), (Button) v);
                    }

                    if (child.getPrompting().getPromptingScratch() && isImageExercise){
                        Prompting.scratchImage(this.getApplicationContext(), (ImageView) v);
                    }
                }
                if (child.getPrompting().getPromptingColor() && isTextExercise) {
                    Prompting.setButtonColor(this.getApplicationContext(), (Button) rightAnswerButton);
                }

                if (child.getPrompting().getPromptingColor() && isImageExercise) {
                   Prompting.setImageColor(this.getApplicationContext(),rightAnswer);
                }
                if(child.getPrompting().getPromptingSize() && isTextExercise) {
                    Prompting.setSizeText(this.getApplicationContext(), rightAnswerButton);
                }

                if(child.getPrompting().getPromptingSize() && isImageExercise) {
                    Prompting.setImageSize(this.getApplicationContext(), rightAnswer);
                }


                if(child.getPrompting().getPromptingRead() && isTextExercise){
                        Prompting.readAnswers(currentAnswers);

                }
                else {
                    readWithOrWithoutEmotion(child, "Tenta outra vez.");
                }
            }
        }
    }
    private void previousExerciseHandler() {
        this.currentExercisePosition--;
        inExercise = true;
        setExerciseView();
        setNavigationView();
    }
    protected void nextExerciseHandler() {
        this.currentExercisePosition++;
        Boolean hasFinished = currentExercisePosition >= exercises.size();
        if (!hasFinished) {
            timestampBeginExercise = new Date();
            inExercise = true;
            setExerciseView();
            setNavigationView();
        } else {
            endHandler();
        }
    }
    protected void endHandler() {
        // TODO Send logger and show results
        //new SendLogs(this, currentSequenceLog).execute();
        //setFinalResultsView();

        //Sequence reinforcement
        if(reinforcementActive) playMessage(child, "SEQUENCE_REINFORCEMENT");

        setSequenceSelectionView();
    }
    protected void homeHandler() {
        setSequenceSelectionView();


    }

    protected  void backMenuHandler(){
        CustomDialogClass cdd=new CustomDialogClass(this);
        cdd.show();

    }

    // logout
    // TODO Verify why sometimes new child inherit stuff from the last child
    private void exitHandler(){
        this.child = null;
        SharedPreferences settings = this.getSharedPreferences(getString(R.string.APP_PREFERENCES), MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.clear();
        prefEditor.commit();
        //Thread.sleep(10000);
        Intent i = new Intent(VitheaKidsActivity.this, LoginActivity.class);
            // Add new Flag to start new Activity
        startActivity(i);
    }

    private void reinforcementHandler(MenuItem item){
        reinforcementActive = !(reinforcementActive);
        if(reinforcementActive){
            item.setTitle("Ligar Reforço");
        }
        else item.setTitle("Desligar Reforço");

        if(!inSequenceScreen){
           // setExerciseView();
           // setNavigationView();
        }
    }
    private void promptingHandler(MenuItem item){
        promptingActive = !(promptingActive);
        if(promptingActive){
            item.setTitle("Ligar Ajuda");
        }
        else item.setTitle("Desligar Ajuda");

        Log.e("home", "" + inHomeScreen);
        Log.e("sequence", "" + inSequenceScreen);

        if(!inSequenceScreen){
           // setExerciseView();
           // setNavigationView();
        }
    }

// **** Menu ***************************************************************************************

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        MenuItem itemPrompting = menu.findItem(R.id.prompting_option);
        MenuItem itemReinforcement = menu.findItem(R.id.reinforcement_option);

        if(promptingActive) itemPrompting.setTitle("Desligar Ajuda");
        else itemPrompting.setTitle("Ligar Ajuda");
        if(reinforcementActive) itemReinforcement.setTitle("Desligar Reforço");
        else itemReinforcement.setTitle("Ligar Reforço");

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (!inHomeScreen) {
            if (!inSequenceScreen && id == R.id.end_option) {
                endHandler();
                //return true;
            } else if (id == R.id.exit) {
                homeHandler();
            }
        }
        if (id == R.id.exit) {
            exitHandler();
        }
        if(id == R.id.back_menu_option){
            backMenuHandler();
        }
        else if(id == R.id.prompting_option){
            promptingHandler(item);
        }
        else if(id == R.id.reinforcement_option){
            reinforcementHandler(item);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        MenuItem item = menu.findItem(R.id.end_option);
        MenuItem itemBack = menu.findItem(R.id.back_menu_option);

        if (!inSequenceScreen) {
            item.setEnabled(true);
            itemBack.setEnabled(true);
        } else {
            // disabled
            item.setEnabled(false);
            itemBack.setEnabled(false);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Generates a new drawable with a new color
     * @param color
     * @return StateListDrawable
     */

    public  StateListDrawable makeSelector(int color) {

        StateListDrawable stateListDrawable = new StateListDrawable();
        Resources res = this.getApplicationContext().getResources();
        Drawable buttonSelected = res.getDrawable(R.drawable.buttonselected);
        GradientDrawable  buttonNormal = (GradientDrawable) res.getDrawable(R.drawable.bottonnormal);
        buttonNormal.setColor(color);
        stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, buttonSelected );
        stateListDrawable.addState(new int[]{}, buttonNormal);

        return stateListDrawable;
    }

    /**
     * readWithOrWithoutEmotion
     * @param sentence
     * Verify if the text has to be pronounced with emotion
     * Launch a task to read the text
     */
    public void readWithOrWithoutEmotion(Child child, String sentence){

        if (child.getEmotions()) {
            new ReadTask().execute(sentence, "joy");
        } else {
            new ReadTask().execute(sentence);
        }

    }

    public void setImageOfView(ImageView  imageView , byte[] bytes){
        Glide.get(this.getApplicationContext()).clearMemory();
        Glide.with(this.getApplicationContext()).load(bytes).apply(RequestOptions.skipMemoryCacheOf(true)).apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE)).into(imageView);
    }

}

