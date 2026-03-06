package com.newproject.blog.repository;

import com.newproject.blog.domain.BlogPost;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {
    List<BlogPost> findAllByOrderByPublishedAtDescCreatedAtDesc();
    List<BlogPost> findByActiveOrderByPublishedAtDescCreatedAtDesc(Boolean active);
    Optional<BlogPost> findBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCase(String slug);
}
