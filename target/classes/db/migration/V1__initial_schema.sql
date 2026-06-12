CREATE TABLE blog_post (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    excerpt VARCHAR(500),
    content TEXT NOT NULL,
    author VARCHAR(255),
    published_at TIMESTAMPTZ,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE blog_comment (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    author_name VARCHAR(255) NOT NULL,
    author_email VARCHAR(255) NOT NULL,
    comment TEXT NOT NULL,
    approved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_blog_comment_post FOREIGN KEY (post_id) REFERENCES blog_post(id) ON DELETE CASCADE
);

CREATE INDEX idx_blog_post_active_published ON blog_post(active, published_at DESC);
CREATE INDEX idx_blog_comment_post_created ON blog_comment(post_id, created_at DESC);
CREATE INDEX idx_blog_comment_approved ON blog_comment(approved);
