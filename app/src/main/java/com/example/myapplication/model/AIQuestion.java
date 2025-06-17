package com.example.myapplication.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public AIQuestion() {

    }

    public void setWord(String word) {
        this.word = word;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public void setExplanation_en(String explanation_en) {
        this.explanation_en = explanation_en;
    }

    public void setExplanation_vi(String explanation_vi) {
        this.explanation_vi = explanation_vi;
    }

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

    public String getExplanation_en() {
        return explanation_en;
    }

    public String getExplanation_vi() {
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

    public static AIQuestion fromMap(Map<String, Object> map) {
        AIQuestion q = new AIQuestion();
        q.setWord((String) map.get("word"));
        q.setType((String) map.get("type"));
        q.setQuestion((String) map.get("question"));
        q.setCorrectAnswer((String) map.get("correctAnswer"));

        Object optionsObj = map.get("options");
        if (optionsObj instanceof List) {
            List<?> rawList = (List<?>) optionsObj;
            List<String> options = new ArrayList<>();
            for (Object o : rawList) {
                options.add(String.valueOf(o));
            }
            q.setOptions(options);
        } else {
            q.setOptions(new ArrayList<>()); // tránh null
        }

        // Đảm bảo đúng tên trường như lưu trên Firestore
        q.setExplanation_en((String) map.get("explanation_en"));
        q.setExplanation_vi((String) map.get("explanation_vi"));
        return q;
    }
}
