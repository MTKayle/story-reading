package org.example.storyreading.storyservice.dto;

import java.io.Serializable;
import java.util.List;

public class NewChapterEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long storyId;
    private String storyTitle;
    private Long chapterId;
    private String chapterTitle;
    private Long authorId;
    private List<Long> followerIds;

    public NewChapterEvent() {}

    public NewChapterEvent(Long storyId, String storyTitle, Long chapterId, String chapterTitle, Long authorId, List<Long> followerIds) {
        this.storyId = storyId;
        this.storyTitle = storyTitle;
        this.chapterId = chapterId;
        this.chapterTitle = chapterTitle;
        this.authorId = authorId;
        this.followerIds = followerIds;
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

    public String getChapterTitle() {
        return chapterTitle;
    }

    public void setChapterTitle(String chapterTitle) {
        this.chapterTitle = chapterTitle;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public List<Long> getFollowerIds() {
        return followerIds;
    }

    public void setFollowerIds(List<Long> followerIds) {
        this.followerIds = followerIds;
    }
}

