package com.newproject.blog.domain;

import jakarta.persistence.*;

@Entity
@Table(
    name = "blog_post_translation",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_blog_post_translation", columnNames = {"post_id", "language_code"})
    }
)
public class BlogPostTranslation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private BlogPost post;

    @Column(name = "language_code", length = 5, nullable = false)
    private String languageCode;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 500)
    private String excerpt;

    @Column(columnDefinition = "text", nullable = false)
    private String content;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BlogPost getPost() {
        return post;
    }

    public void setPost(BlogPost post) {
        this.post = post;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
