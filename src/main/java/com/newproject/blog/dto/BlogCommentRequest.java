package com.newproject.blog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class BlogCommentRequest {
    @NotBlank
    @Size(max = 255)
    private String authorName;
    @Email
    @NotBlank
    @Size(max = 255)
    private String authorEmail;
    // SECURITY: cap di lunghezza anti DoS/DB-bloat (defense-in-depth; il render e' gia' escaped).
    @NotBlank
    @Size(max = 5000)
    private String comment;

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorEmail() { return authorEmail; }
    public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
