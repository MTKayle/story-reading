package org.example.storyreading.storyservice.dto;

import java.io.Serializable;

public class NewChapterEvent implements Serializable {
    private Long storyId;
    private String storyTitle;
    private Long chapterId;
    private Integer chapterNumber;
    private String chapterTitle;

    public NewChapterEvent() {
    }

    public NewChapterEvent(Long storyId, String storyTitle, Long chapterId, Integer chapterNumber, String chapterTitle) {
        this.storyId = storyId;
        this.storyTitle = storyTitle;
        this.chapterId = chapterId;
        this.chapterNumber = chapterNumber;
        this.chapterTitle = chapterTitle;
    }

    public Long getStoryId() {
        return storyId;
    }

    public void setStoryId(Long storyId) {
        this.storyId = storyId;
    }

    public String getStoryTitle() {
        return storyTitle;
    }

    public void setStoryTitle(String storyTitle) {
        this.storyTitle = storyTitle;
    }

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public Integer getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(Integer chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public String getChapterTitle() {
        return chapterTitle;
    }

    public void setChapterTitle(String chapterTitle) {
        this.chapterTitle = chapterTitle;
    }
}

