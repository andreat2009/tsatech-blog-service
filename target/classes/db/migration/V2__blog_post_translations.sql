CREATE TABLE blog_post_translation (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    language_code VARCHAR(5) NOT NULL,
    title VARCHAR(255) NOT NULL,
    excerpt VARCHAR(500),
    content TEXT NOT NULL,
    CONSTRAINT fk_blog_post_translation_post
        FOREIGN KEY (post_id) REFERENCES blog_post(id) ON DELETE CASCADE,
    CONSTRAINT uk_blog_post_translation UNIQUE (post_id, language_code)
);

INSERT INTO blog_post_translation (post_id, language_code, title, excerpt, content)
SELECT p.id, l.language_code, p.title, p.excerpt, p.content
FROM blog_post p
CROSS JOIN (
    VALUES ('it'), ('en'), ('fr'), ('de'), ('es')
) AS l(language_code)
ON CONFLICT (post_id, language_code) DO NOTHING;
