package com.newproject.blog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class BlogCommentRequest {
    @NotBlank
    private String authorName;
    @Email
    @NotBlank
    private String authorEmail;
    @NotBlank
    private String comment;

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorEmail() { return authorEmail; }
    public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
