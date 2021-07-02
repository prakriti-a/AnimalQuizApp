package com.prakriti.animalquizapp;

import android.animation.Animator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MainFragment extends Fragment implements View.OnClickListener {

    private static final int NUM_OF_QUESTIONS_IN_QUIZ = 10;

    private List<String> fullAnimalNamesList, animalNamesList; // full list for animal type in quiz, names list for random guess options for 10 questions
    private Set<String> animalTypesInQuizSet; // no duplicate values
    private String correctAnswer;
    private int numOfGuesses, numOfCorrectAnswers, numOfGuessRows; // no of guess rows will be as per user settings
    private SecureRandom secureRandomNumber; // to get random animal in quiz
    private Handler handler; // for small delay between questions
    private Animation wrongAnswerAnimation;

    private LinearLayout quizLinearLayout; // for background color of main page
    private TextView txtQuestionNumber, txtAnswer; // for question number changes & answer
    private ImageView imageAnimal;
    private LinearLayout[] rowsOfGuesses; // for each row of linear layout holding guess buttons in fragment

    public MainFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment, 'view' is the reference to this fragment
        // viewgroup container is content_main.xml for fragment_main.xml
        View view = inflater.inflate(R.layout.fragment_main, container, false); // container is container of the fragment in content_main.xml

        fullAnimalNamesList = new ArrayList<>();
        animalNamesList = new ArrayList<>();
        secureRandomNumber = new SecureRandom();
        handler = new Handler();

        wrongAnswerAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.wrong_answer_animations); // pass anim xml file & activity's context
            // context by getActivity() if Fragment class is extended
        wrongAnswerAnimation.setRepeatCount(1); // exec only once

        quizLinearLayout = view.findViewById(R.id.linearLayoutMainFragment);
        txtQuestionNumber = view.findViewById(R.id.txtQuestionNumber);
        txtAnswer = view.findViewById(R.id.txtAnswer);
        imageAnimal = view.findViewById(R.id.imageAnimal);

        rowsOfGuesses = new LinearLayout[3];
        rowsOfGuesses[0] = view.findViewById(R.id.fragmentLLFirstRow);
        rowsOfGuesses[1] = view.findViewById(R.id.fragmentLLSecondRow);
        rowsOfGuesses[2] = view.findViewById(R.id.fragmentLLThirdRow);

        for(LinearLayout row : rowsOfGuesses) {
            for(int column = 0; column <row.getChildCount(); column++) { // iterate over children in view i.e the linear layout
                Button guessButton = (Button) row.getChildAt(column); // get each child, getChildAt returns a View by specifying index
                guessButton.setOnClickListener(this); // set listener for each button
                guessButton.setTextSize(24); // set text size
            }
        }
        txtQuestionNumber.setText(getString(R.string.question_number, 1, NUM_OF_QUESTIONS_IN_QUIZ));
            // formatted string, then arguments array is passed // 1 -> %1$d, total num of questions -> %2$d
        return view;
    }

    @Override
    public void onClick(View v) { // v -> clicked view i.e. button
        Button btnSelectedGuess = (Button) v;
        String guessSelected = btnSelectedGuess.getText().toString();
        String answer = getExactAnimalName(correctAnswer);
        ++numOfGuesses; // guesses made by user, used for calculating score

        if(guessSelected.equals(answer)) { // guessed correctly
            ++numOfCorrectAnswers; // for checking end of quiz
            txtAnswer.setText(R.string.correct_answer);
            disableAllGuessButtons(); // disable all buttons once correct answer is selected

            if(numOfCorrectAnswers == NUM_OF_QUESTIONS_IN_QUIZ) { // end of quiz is reached, 10 questions have been answered
                try {
                    DialogFragment quizResults = new DialogFragment() {
                        @NonNull
                        @Override
                        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle(R.string.result_title);
                            builder.setMessage(getString(R.string.result_message, numOfGuesses, (1000 / (double) numOfGuesses)));
                            // for formatted strings use getString()
                            builder.setPositiveButton(R.string.reset_quiz_button, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) { // listener for button
                                    resetAnimalQuiz();
                                }
                            });
                            return builder.create();
                        }
                    };
                    quizResults.setCancelable(false);
                    quizResults.show(getActivity().getSupportFragmentManager(), "QuizResults"); // call show() to display dialog
                    // getFragmentManager() is deprecated
                }
                catch (Exception e) {
                    Log.e("AnimalQuiz", "Error:", e);
                    e.printStackTrace();
                }
            }
            else { // quiz is not finished yet
                handler.postDelayed(new Runnable() { // background
                    @Override
                    public void run() {
                        animateQuizQuestion(true); // change question
                    }}, 500); // 0.5 sec delay
                }
        }
        else { // if answer is wrong, question does not change
            imageAnimal.startAnimation(wrongAnswerAnimation);
            txtAnswer.setText(R.string.wrong_answer);
            btnSelectedGuess.setEnabled(false); // disable button that was clicked already
        }
    }

    private String getExactAnimalName(String correctAnswer) {
        // get just the name from full path
        return correctAnswer.substring(correctAnswer.indexOf('-') + 1).replace('_', ' ');
    }

    private void disableAllGuessButtons() { // disable each button in each linear layout iteratively
        for(int row = 0; row < numOfGuessRows; row++) { // index based on user selection
            LinearLayout llRow = rowsOfGuesses[row];
//        for(LinearLayout llRow : rowsOfGuesses) {
            // here we dont use enhanced for loop, as not all 6 buttons may be visible
            // so check number of guesses variable, as selected by user. so only the visible rows are disabled
            // if settings are changed midway, newly visible buttons will be enabled
            for (int buttonIndex = 0; buttonIndex < llRow.getChildCount(); buttonIndex++) {
                llRow.getChildAt(buttonIndex).setEnabled(false);
            }
        }
    }

    public void resetAnimalQuiz() {
        AssetManager assets = getActivity().getAssets();
        fullAnimalNamesList.clear();
        try {
            for (String animalType : animalTypesInQuizSet) {
                String[] animalImagePaths = assets.list(animalType); // here, we're passing the folder names (animal types) to assets
                    // list returns a String[]
                // image paths set will hold the full names with ".png"
                for (String imagePath : animalImagePaths) {
                    fullAnimalNamesList.add(imagePath.replace(".png", ""));
                    // names list will hold the names without ".png"
                }
            }
        }
        catch (IOException e) {
            Log.e("AnimalQuiz", "ERROR", e);
            e.printStackTrace();
        }
        numOfGuesses = 0;
        numOfCorrectAnswers = 0;
        animalNamesList.clear();

        int counter = 1;
        int numOfAvailableAnimals = fullAnimalNamesList.size();
        while (counter <= NUM_OF_QUESTIONS_IN_QUIZ) { // 10 times
            int randomIndex = secureRandomNumber.nextInt(numOfAvailableAnimals); // gets random int b/w 0..x available animals
            String animalName = fullAnimalNamesList.get(randomIndex); // pass random num as index to available animals list, get name
            if(!animalNamesList.contains(animalName)) { // add name to name list if not already in it
                animalNamesList.add(animalName); // to make sure question is not duplicated / repeated
                ++counter;
            }
        }
        showNextQuizQuestion();
    }

    private void animateQuizQuestion(boolean animateOut) {
        if(numOfCorrectAnswers == 0) { // if answer is wrong - put wrong answer anim, else animate to next question
            // or if it is first question
            return;
        }
        int xTopLeft = 0, yTopLeft = 0; // top left corner of screen
        int xBottomRight = quizLinearLayout.getLeft() + quizLinearLayout.getRight();
        int yBottomRight = quizLinearLayout.getTop() + quizLinearLayout.getBottom();

        // max value for radius -> for circular animation
        int radius = Math.max(quizLinearLayout.getWidth(), quizLinearLayout.getHeight()); // max b/w width & height of main Linear Layout
        Animator animator;

        if(animateOut) { // if passed true
            animator = ViewAnimationUtils.createCircularReveal(quizLinearLayout, xBottomRight, yBottomRight, radius, 0);
            // pass the view, centerX, centerY, start radius, end radius
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {}
                @Override
                public void onAnimationEnd(Animator animation) {
                    showNextQuizQuestion();
                }
                @Override
                public void onAnimationCancel(Animator animation) {}
                @Override
                public void onAnimationRepeat(Animator animation) {}
            });
        }
        else { // passed false param
            animator = ViewAnimationUtils.createCircularReveal(quizLinearLayout, xTopLeft, yTopLeft, 0, radius);
        }
        animator.setDuration(500); // ms
        animator.start();
    }

    private void showNextQuizQuestion() { // show new question, buttons & image
        String nextAnimalName = animalNamesList.remove(0); // remove first one in list of unique names from secure random
        correctAnswer = nextAnimalName; // holds path without ".png"
        txtAnswer.setText("");
        txtQuestionNumber.setText(getString(R.string.question_number, numOfCorrectAnswers + 1, NUM_OF_QUESTIONS_IN_QUIZ));
            // correct answers + 1 -> current question num

        String animalType = nextAnimalName.substring(0, nextAnimalName.indexOf('-'));
            // extract animal type of the current question from animal name, excl '-'
        // used to pass name to asset manager & access the image for correct answer
        AssetManager assetManager = getActivity().getAssets();
        try (InputStream stream = assetManager.open(animalType + "/" + nextAnimalName + ".png")) // passing path
        { // stream open file at path -> assets/animal_type/full_name_path.png
            Drawable animalImage = Drawable.createFromStream(stream, nextAnimalName); // input stream & string source name
            imageAnimal.setImageDrawable(animalImage);
            animateQuizQuestion(false); // creates animation w/o changing question -> called after changing question
        }
        catch (IOException e) {
            Log.e("AnimalQuiz", "ERROR", e);
            e.printStackTrace();
        }
        Collections.shuffle(fullAnimalNamesList); // unordered & unpredictable

        int correctAnimalNameIndex = fullAnimalNamesList.indexOf(correctAnswer); // index of answer for current question
        String correctAnswerName = fullAnimalNamesList.remove(correctAnimalNameIndex);
        fullAnimalNamesList.add(correctAnswerName); // answer for current que is added to the end of array
            // this is done so that incorrect options can be specified as guesses - list has been shuffled

        for(int row = 0; row < numOfGuessRows; row++) { // num of guess rows selected
            for (int column = 0; column < rowsOfGuesses[row].getChildCount(); column++) {
                // iterate over all visible buttons
                Button btnGuess = (Button) rowsOfGuesses[row].getChildAt(column);
                btnGuess.setEnabled(true);
                String animalName = fullAnimalNamesList.get((row * 2) + column); // pattern - after list has been shuffled
                btnGuess.setText(getExactAnimalName(animalName)); // set all visible buttons as random names from list
            }
        }
        // set a random button as correct answer
        int row = secureRandomNumber.nextInt(numOfGuessRows); // random row index from visible rows
        int column = secureRandomNumber.nextInt(2); // random from 0 and 1 column (not incl 2)
        LinearLayout randomRow = rowsOfGuesses[row]; // get row
        String correctAnimalName = getExactAnimalName(correctAnswer);
        ((Button) randomRow.getChildAt(column)).setText(correctAnimalName);
    }

    // =============================== SHARED PREFERENCES LOGIC ============================================

    public void changeQuizGuessRows(SharedPreferences sharedPreferences) {
        final String NUM_OF_GUESS_OPTIONS = sharedPreferences.getString(MainActivity.NUM_OF_GUESSES_KEY, null); // def val already specified
            // try above code with getInt() ??
        numOfGuessRows = Integer.parseInt(NUM_OF_GUESS_OPTIONS) / 2;
        for(LinearLayout row : rowsOfGuesses) { // make all GONE
            row.setVisibility(View.GONE);
        }
        for(int row = 0; row < numOfGuessRows; row++) { // make only the selected rows VISIBLE
            rowsOfGuesses[row].setVisibility(View.VISIBLE);
        }
    }

    public void changeAnimalTypesInQuiz(SharedPreferences sharedPreferences) {
        // apply option selected by user, put in set, both are selected by default
        animalTypesInQuizSet = sharedPreferences.getStringSet(MainActivity.ANIMAL_TYPES_KEY, null); // set of String values
    }

    public void changeQuizBackgroundColor(SharedPreferences sharedPreferences) {
        String backgroundColor = sharedPreferences.getString(MainActivity.BG_COLOR_KEY, null); // change default to white
        switch (backgroundColor) {
            case "White":
                commonMethodForBackgroundColor(R.color.white);
                break;
            case "Black":
                // for black - change theme
                quizLinearLayout.setBackgroundColor(getResources().getColor(R.color.black, null));
                for(LinearLayout row : rowsOfGuesses) {
                    for (int column = 0; column < row.getChildCount(); column++) {
                        Button button = (Button) row.getChildAt(column);
                        button.setBackgroundColor(getResources().getColor(R.color.quiz_grey, null));
                        button.setTextColor(getResources().getColor(R.color.black, null));
                    }
                }
                txtAnswer.setTextColor(getResources().getColor(R.color.white, null));
                txtQuestionNumber.setTextColor(getResources().getColor(R.color.white, null));
                break;
            case "Green":
                commonMethodForBackgroundColor(R.color.quiz_green);
                break;
            case "Red":
                commonMethodForBackgroundColor(R.color.quiz_red);
                break;
            case "Blue":
                commonMethodForBackgroundColor(R.color.quiz_blue);
                break;
            case "Yellow":
                commonMethodForBackgroundColor(R.color.quiz_yellow);
                break;
        }
    }

    public void changeQuizFont(SharedPreferences sharedPreferences) {
        String fontSelected = sharedPreferences.getString(MainActivity.FONT_KEY, null);
        switch (fontSelected) {
            case "Hold On.ttf":
                commonMethodForFont(MainActivity.holdOn);
                break;
            case "Qabil Free Trial.ttf":
                commonMethodForFont(MainActivity.qabilFreeTrial);
                break;
            case "Murberry.ttf":
                commonMethodForFont(MainActivity.murberry);
                break;
        }
    }

    private void commonMethodForBackgroundColor(@ColorRes int backgroundColorID) {
        // same for all other colors
        quizLinearLayout.setBackgroundColor(getResources().getColor(backgroundColorID, null));
        for (LinearLayout row : rowsOfGuesses) {
            for (int column = 0; column < row.getChildCount(); column++) {
                Button button = (Button) row.getChildAt(column);
                button.setBackgroundColor(getResources().getColor(R.color.quiz_dark_grey, null));
                button.setTextColor(getResources().getColor(R.color.white, null));
            }
        }
        txtAnswer.setTextColor(getResources().getColor(R.color.black, null));
        txtQuestionNumber.setTextColor(getResources().getColor(R.color.black, null));
    }

    private void commonMethodForFont(Typeface fontName) {
        for(LinearLayout row : rowsOfGuesses) { // set for all buttons
            for (int column = 0; column < row.getChildCount(); column++) {
                ((Button) row.getChildAt(column)).setTypeface(fontName);
            }
        }
        txtQuestionNumber.setTypeface(fontName); // check how these look-------
        txtAnswer.setTypeface(fontName);
    }


//    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//    }
}