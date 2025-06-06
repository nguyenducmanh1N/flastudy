package com.example.myapplication.model;

import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;


public class AIQuestion implements Parcelable {
    private String word;
    private String type;
    private String question;
    private List<String> options;
    private String correctAnswer;
    private String explanation_en;
    private String explanation_vi;


    public AIQuestion(String word, String type, String question,
                      List<String> options, String correctAnswer, String explanation) {
        this.word = word;
        this.type = type;
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.explanation_en= explanation_en;
        this.explanation_vi = explanation_vi;
    }

    protected AIQuestion(Parcel in) {
        word = in.readString();
        type = in.readString();
        question = in.readString();
        options = in.createStringArrayList();
        correctAnswer = in.readString();
        explanation_en = in.readString();
        explanation_vi = in.readString();
    }

    public static final Creator<AIQuestion> CREATOR = new Creator<AIQuestion>() {
        @Override
        public AIQuestion createFromParcel(Parcel in) {
            return new AIQuestion(in);
        }

        @Override
        public AIQuestion[] newArray(int size) {
            return new AIQuestion[size];
        }
    };

    public String getWord() {
        return word;
    }

    public String getType() {
        return type;
    }

    public String getQuestion() {
        return question;
    }

    public List<String> getOptions() {
        return options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }
    public String getExplanationEn() {
        return explanation_en;
    }

    public String getExplanationVi() {
        return explanation_vi;
    }



    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(word);
        parcel.writeString(type);
        parcel.writeString(question);
        parcel.writeStringList(options);
        parcel.writeString(correctAnswer);
        parcel.writeString(explanation_en);
        parcel.writeString(explanation_vi);

    }
}
