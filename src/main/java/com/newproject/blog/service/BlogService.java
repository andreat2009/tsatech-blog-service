package com.newproject.blog.service;

import com.newproject.blog.domain.BlogComment;
import com.newproject.blog.domain.BlogPost;
import com.newproject.blog.dto.BlogCommentRequest;
import com.newproject.blog.dto.BlogCommentResponse;
import com.newproject.blog.dto.BlogPostRequest;
import com.newproject.blog.dto.BlogPostResponse;
import com.newproject.blog.events.EventPublisher;
import com.newproject.blog.exception.NotFoundException;
import com.newproject.blog.repository.BlogCommentRepository;
import com.newproject.blog.repository.BlogPostRepository;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BlogService {
    private final BlogPostRepository postRepository;
    private final BlogCommentRepository commentRepository;
    private final EventPublisher eventPublisher;

    public BlogService(
        BlogPostRepository postRepository,
        BlogCommentRepository commentRepository,
        EventPublisher eventPublisher
    ) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<BlogPostResponse> listPosts(Boolean active) {
        List<BlogPost> posts = active == null
            ? postRepository.findAllByOrderByPublishedAtDescCreatedAtDesc()
            : postRepository.findByActiveOrderByPublishedAtDescCreatedAtDesc(active);
        return posts.stream().map(this::toPostResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BlogPostResponse getPost(Long id) {
        return toPostResponse(postRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Blog post not found")));
    }

    @Transactional(readOnly = true)
    public BlogPostResponse getPostBySlug(String slug) {
        return toPostResponse(postRepository.findBySlugIgnoreCase(slug)
            .orElseThrow(() -> new NotFoundException("Blog post not found")));
    }

    @Transactional
    public BlogPostResponse createPost(BlogPostRequest request) {
        BlogPost post = new BlogPost();
        applyPost(post, request, true);
        OffsetDateTime now = OffsetDateTime.now();
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        if (post.getPublishedAt() == null) {
            post.setPublishedAt(now);
        }

        BlogPost saved = postRepository.save(post);
        BlogPostResponse response = toPostResponse(saved);
        eventPublisher.publish("BLOG_POST_CREATED", "blog_post", saved.getId().toString(), response);
        return response;
    }

    @Transactional
    public BlogPostResponse updatePost(Long id, BlogPostRequest request) {
        BlogPost post = postRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Blog post not found"));

        applyPost(post, request, false);
        post.setUpdatedAt(OffsetDateTime.now());
        BlogPost saved = postRepository.save(post);
        BlogPostResponse response = toPostResponse(saved);
        eventPublisher.publish("BLOG_POST_UPDATED", "blog_post", saved.getId().toString(), response);
        return response;
    }

    @Transactional
    public void deletePost(Long id) {
        BlogPost post = postRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Blog post not found"));
        postRepository.delete(post);
        eventPublisher.publish("BLOG_POST_DELETED", "blog_post", id.toString(), null);
    }

    @Transactional(readOnly = true)
    public List<BlogCommentResponse> listCommentsByPost(Long postId, Boolean approved) {
        List<BlogComment> comments = approved == null
            ? commentRepository.findByPostIdOrderByCreatedAtDesc(postId)
            : commentRepository.findByPostIdAndApprovedOrderByCreatedAtDesc(postId, approved);
        return comments.stream().map(this::toCommentResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BlogCommentResponse> listComments(Boolean approved) {
        List<BlogComment> comments = approved == null
            ? commentRepository.findAllByOrderByCreatedAtDesc()
            : commentRepository.findByApprovedOrderByCreatedAtDesc(approved);
        return comments.stream().map(this::toCommentResponse).collect(Collectors.toList());
    }

    @Transactional
    public BlogCommentResponse createComment(Long postId, BlogCommentRequest request) {
        postRepository.findById(postId)
            .orElseThrow(() -> new NotFoundException("Blog post not found"));

        BlogComment comment = new BlogComment();
        comment.setPostId(postId);
        comment.setAuthorName(request.getAuthorName());
        comment.setAuthorEmail(request.getAuthorEmail());
        comment.setComment(request.getComment());
        comment.setApproved(Boolean.FALSE);
        comment.setCreatedAt(OffsetDateTime.now());

        BlogComment saved = commentRepository.save(comment);
        BlogCommentResponse response = toCommentResponse(saved);
        eventPublisher.publish("BLOG_COMMENT_CREATED", "blog_comment", saved.getId().toString(), response);
        return response;
    }

    @Transactional
    public BlogCommentResponse setCommentApproval(Long commentId, boolean approved) {
        BlogComment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new NotFoundException("Blog comment not found"));
        comment.setApproved(approved);

        BlogComment saved = commentRepository.save(comment);
        BlogCommentResponse response = toCommentResponse(saved);
        eventPublisher.publish("BLOG_COMMENT_APPROVAL_UPDATED", "blog_comment", saved.getId().toString(), response);
        return response;
    }

    private void applyPost(BlogPost post, BlogPostRequest request, boolean createMode) {
        post.setTitle(request.getTitle());
        post.setSlug(uniqueSlug(request.getSlug(), request.getTitle(), createMode ? null : post.getId()));
        post.setExcerpt(request.getExcerpt());
        post.setContent(request.getContent());
        post.setAuthor(request.getAuthor());
        post.setPublishedAt(request.getPublishedAt());
        post.setActive(request.getActive() != null ? request.getActive() : Boolean.TRUE);
    }

    private String uniqueSlug(String requestedSlug, String title, Long currentId) {
        String base = normalizeSlug(requestedSlug != null && !requestedSlug.isBlank() ? requestedSlug : title);
        String candidate = base;
        int i = 2;
        while (postRepository.existsBySlugIgnoreCase(candidate)) {
            if (currentId != null) {
                BlogPost existing = postRepository.findBySlugIgnoreCase(candidate).orElse(null);
                if (existing != null && currentId.equals(existing.getId())) {
                    return candidate;
                }
            }
            candidate = base + "-" + i;
            i++;
        }
        return candidate;
    }

    private String normalizeSlug(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "post" : normalized;
    }

    private BlogPostResponse toPostResponse(BlogPost post) {
        BlogPostResponse response = new BlogPostResponse();
        response.setId(post.getId());
        response.setTitle(post.getTitle());
        response.setSlug(post.getSlug());
        response.setExcerpt(post.getExcerpt());
        response.setContent(post.getContent());
        response.setAuthor(post.getAuthor());
        response.setPublishedAt(post.getPublishedAt());
        response.setActive(post.getActive());
        response.setCreatedAt(post.getCreatedAt());
        response.setUpdatedAt(post.getUpdatedAt());
        return response;
    }

    private BlogCommentResponse toCommentResponse(BlogComment comment) {
        BlogCommentResponse response = new BlogCommentResponse();
        response.setId(comment.getId());
        response.setPostId(comment.getPostId());
        response.setAuthorName(comment.getAuthorName());
        response.setAuthorEmail(comment.getAuthorEmail());
        response.setComment(comment.getComment());
        response.setApproved(comment.getApproved());
        response.setCreatedAt(comment.getCreatedAt());
        return response;
    }
}
