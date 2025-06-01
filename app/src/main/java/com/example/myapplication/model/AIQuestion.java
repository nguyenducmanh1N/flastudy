package com.example.myapplication.model;

import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;


public class AIQuestion implements Parcelable {
    private String word;                // Từ vựng gốc (không bắt buộc nhưng hữu ích để debug)
    private String type;                // "meaning" hoặc "tense"
    private String question;            // Nội dung câu hỏi (đã có chỗ điền ... hoặc tương tự)
    private List<String> options;       // Danh sách 4 đáp án
    private String correctAnswer;       // Đáp án đúng
    private String explanation;         // Giải thích

    // Constructor
    public AIQuestion(String word, String type, String question,
                      List<String> options, String correctAnswer, String explanation) {
        this.word = word;
        this.type = type;
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
    }

    protected AIQuestion(Parcel in) {
        word = in.readString();
        type = in.readString();
        question = in.readString();
        options = in.createStringArrayList();
        correctAnswer = in.readString();
        explanation = in.readString();
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

    public String getExplanation() {
        return explanation;
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
        parcel.writeString(explanation);
    }
}
