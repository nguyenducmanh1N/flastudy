package com.example.myapplication.model;

import android.os.Parcel;
import android.os.Parcelable;
public class Vocabulary implements Parcelable {
    private String id;
    private String word;
    private String meaning;
    private String example;
    private String audio;
    private String phonetic;

//    public String getTense() {
//        return tense;
//    }
//
//    public void setTense(String tense) {
//        this.tense = tense;
//    }
//
//    private String tense;

    public Vocabulary() {}

    public Vocabulary(String word, String meaning, String example, String audio, String phonetic) {
        this.word = word;
        this.meaning = meaning;
        this.example = example;
        this.audio = audio;
        this.phonetic = phonetic;
    }

    protected Vocabulary(Parcel in) {
        id = in.readString();
        word = in.readString();
        meaning = in.readString();
        example = in.readString();
        audio = in.readString();
        phonetic = in.readString();
    }

    public static final Creator<Vocabulary> CREATOR = new Creator<Vocabulary>() {
        @Override
        public Vocabulary createFromParcel(Parcel in) {
            return new Vocabulary(in);
        }

        @Override
        public Vocabulary[] newArray(int size) {
            return new Vocabulary[size];
        }
    };

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }

    public String getMeaning() { return meaning; }
    public void setMeaning(String meaning) { this.meaning = meaning; }

    public String getExample() { return example; }
    public void setExample(String example) { this.example = example; }

    public String getAudio() { return audio; }
    public void setAudio(String audio) { this.audio = audio; }

    public String getPhonetic() { return phonetic; }
    public void setPhonetic(String phonetic) { this.phonetic = phonetic; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(word);
        dest.writeString(meaning);
        dest.writeString(example);
        dest.writeString(audio);
        dest.writeString(phonetic);
    }
}
