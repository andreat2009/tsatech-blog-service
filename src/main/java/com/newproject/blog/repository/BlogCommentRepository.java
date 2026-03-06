package com.newproject.blog.repository;

import com.newproject.blog.domain.BlogComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogCommentRepository extends JpaRepository<BlogComment, Long> {
    List<BlogComment> findByPostIdOrderByCreatedAtDesc(Long postId);
    List<BlogComment> findByPostIdAndApprovedOrderByCreatedAtDesc(Long postId, Boolean approved);
    List<BlogComment> findAllByOrderByCreatedAtDesc();
    List<BlogComment> findByApprovedOrderByCreatedAtDesc(Boolean approved);
}
