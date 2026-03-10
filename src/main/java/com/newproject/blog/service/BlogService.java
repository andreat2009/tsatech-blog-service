package com.newproject.blog.service;

import com.newproject.blog.domain.BlogComment;
import com.newproject.blog.domain.BlogPost;
import com.newproject.blog.domain.BlogPostTranslation;
import com.newproject.blog.dto.BlogCommentRequest;
import com.newproject.blog.dto.BlogCommentResponse;
import com.newproject.blog.dto.BlogPostRequest;
import com.newproject.blog.dto.BlogPostResponse;
import com.newproject.blog.dto.LocalizedContent;
import com.newproject.blog.events.EventPublisher;
import com.newproject.blog.exception.BadRequestException;
import com.newproject.blog.exception.NotFoundException;
import com.newproject.blog.repository.BlogCommentRepository;
import com.newproject.blog.repository.BlogPostRepository;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    public List<BlogPostResponse> listPosts(Boolean active, String language) {
        List<BlogPost> posts = active == null
            ? postRepository.findAllByOrderByPublishedAtDescCreatedAtDesc()
            : postRepository.findByActiveOrderByPublishedAtDescCreatedAtDesc(active);

        String requestedLanguage = LanguageSupport.normalizeLanguage(language);
        final String resolvedLanguage = requestedLanguage != null ? requestedLanguage : LanguageSupport.DEFAULT_LANGUAGE;

        posts.sort(Comparator
            .comparing(BlogPost::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(post -> resolveLocalizedContent(post.getTranslations(), resolvedLanguage, post.getTitle(), post.getExcerpt(), post.getContent()).getTitle(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(BlogPost::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        return posts.stream().map(post -> toPostResponse(post, resolvedLanguage)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BlogPostResponse getPost(Long id, String language) {
        BlogPost post = postRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Blog post not found"));
        return toPostResponse(post, language);
    }

    @Transactional(readOnly = true)
    public BlogPostResponse getPostBySlug(String slug, String language) {
        BlogPost post = postRepository.findBySlugIgnoreCase(slug)
            .orElseThrow(() -> new NotFoundException("Blog post not found"));
        return toPostResponse(post, language);
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
        BlogPostResponse response = toPostResponse(saved, LanguageSupport.DEFAULT_LANGUAGE);
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
        BlogPostResponse response = toPostResponse(saved, LanguageSupport.DEFAULT_LANGUAGE);
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
        Map<String, LocalizedContent> normalizedTranslations = normalizeTranslations(
            request.getTranslations(),
            request.getTitle(),
            request.getExcerpt(),
            request.getContent(),
            post.getTitle(),
            post.getExcerpt(),
            post.getContent()
        );

        LocalizedContent defaultContent = normalizedTranslations.get(LanguageSupport.DEFAULT_LANGUAGE);
        post.setTitle(defaultContent.getTitle());
        post.setExcerpt(defaultContent.getExcerpt());
        post.setContent(defaultContent.getContent());
        post.setSlug(uniqueSlug(request.getSlug(), defaultContent.getTitle(), createMode ? null : post.getId()));
        post.setAuthor(request.getAuthor());
        post.setPublishedAt(request.getPublishedAt());
        post.setActive(request.getActive() != null ? request.getActive() : Boolean.TRUE);

        syncTranslations(post, normalizedTranslations);
    }

    private void syncTranslations(BlogPost post, Map<String, LocalizedContent> localizedContents) {
        Map<String, BlogPostTranslation> existingByLanguage = post.getTranslations().stream()
            .collect(Collectors.toMap(
                translation -> translation.getLanguageCode().toLowerCase(Locale.ROOT),
                translation -> translation,
                (first, ignored) -> first
            ));

        for (String language : LanguageSupport.SUPPORTED_LANGUAGES) {
            LocalizedContent localizedContent = localizedContents.get(language);
            BlogPostTranslation translation = existingByLanguage.get(language);
            if (translation == null) {
                translation = new BlogPostTranslation();
                translation.setPost(post);
                translation.setLanguageCode(language);
                post.getTranslations().add(translation);
                existingByLanguage.put(language, translation);
            }
            translation.setTitle(localizedContent.getTitle());
            translation.setExcerpt(localizedContent.getExcerpt());
            translation.setContent(localizedContent.getContent());
        }

        post.getTranslations().removeIf(translation ->
            !LanguageSupport.SUPPORTED_LANGUAGES.contains(translation.getLanguageCode().toLowerCase(Locale.ROOT)));
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

    private BlogPostResponse toPostResponse(BlogPost post, String language) {
        LocalizedContent localized = resolveLocalizedContent(post.getTranslations(), language, post.getTitle(), post.getExcerpt(), post.getContent());

        BlogPostResponse response = new BlogPostResponse();
        response.setId(post.getId());
        response.setTitle(localized.getTitle());
        response.setSlug(post.getSlug());
        response.setExcerpt(localized.getExcerpt());
        response.setContent(localized.getContent());
        response.setAuthor(post.getAuthor());
        response.setPublishedAt(post.getPublishedAt());
        response.setActive(post.getActive());
        response.setCreatedAt(post.getCreatedAt());
        response.setUpdatedAt(post.getUpdatedAt());
        response.setTranslations(toLocalizedContentMap(post.getTranslations(), post.getTitle(), post.getExcerpt(), post.getContent()));
        return response;
    }

    private Map<String, LocalizedContent> normalizeTranslations(
        Map<String, LocalizedContent> requested,
        String fallbackTitle,
        String fallbackExcerpt,
        String fallbackContent,
        String existingTitle,
        String existingExcerpt,
        String existingContent
    ) {
        Map<String, LocalizedContent> normalized = new LinkedHashMap<>();

        String defaultTitle = firstNonBlank(
            extractValue(requested, LanguageSupport.DEFAULT_LANGUAGE, Field.TITLE),
            fallbackTitle,
            existingTitle
        );
        String defaultExcerpt = firstNonBlank(
            extractValue(requested, LanguageSupport.DEFAULT_LANGUAGE, Field.EXCERPT),
            fallbackExcerpt,
            existingExcerpt
        );
        String defaultContent = firstNonBlank(
            extractValue(requested, LanguageSupport.DEFAULT_LANGUAGE, Field.CONTENT),
            fallbackContent,
            existingContent
        );

        if (defaultTitle == null || defaultTitle.isBlank()) {
            throw new BadRequestException("Blog post title is required");
        }
        if (defaultContent == null || defaultContent.isBlank()) {
            throw new BadRequestException("Blog post content is required");
        }

        for (String language : LanguageSupport.SUPPORTED_LANGUAGES) {
            LocalizedContent content = new LocalizedContent();
            content.setTitle(firstNonBlank(
                extractValue(requested, language, Field.TITLE),
                language.equals(LanguageSupport.DEFAULT_LANGUAGE) ? fallbackTitle : null,
                defaultTitle
            ));
            content.setExcerpt(firstNonBlank(
                extractValue(requested, language, Field.EXCERPT),
                language.equals(LanguageSupport.DEFAULT_LANGUAGE) ? fallbackExcerpt : null,
                defaultExcerpt
            ));
            content.setContent(firstNonBlank(
                extractValue(requested, language, Field.CONTENT),
                language.equals(LanguageSupport.DEFAULT_LANGUAGE) ? fallbackContent : null,
                defaultContent
            ));
            normalized.put(language, content);
        }

        return normalized;
    }

    private Map<String, LocalizedContent> toLocalizedContentMap(
        List<BlogPostTranslation> translations,
        String fallbackTitle,
        String fallbackExcerpt,
        String fallbackContent
    ) {
        Map<String, LocalizedContent> map = new LinkedHashMap<>();
        Map<String, BlogPostTranslation> byLanguage = translations.stream()
            .collect(Collectors.toMap(
                translation -> translation.getLanguageCode().toLowerCase(Locale.ROOT),
                translation -> translation,
                (first, ignored) -> first
            ));

        for (String language : LanguageSupport.SUPPORTED_LANGUAGES) {
            BlogPostTranslation translation = byLanguage.get(language);
            LocalizedContent content = new LocalizedContent();
            content.setTitle(firstNonBlank(
                translation != null ? translation.getTitle() : null,
                language.equals(LanguageSupport.DEFAULT_LANGUAGE) ? fallbackTitle : null,
                fallbackTitle
            ));
            content.setExcerpt(firstNonBlank(
                translation != null ? translation.getExcerpt() : null,
                language.equals(LanguageSupport.DEFAULT_LANGUAGE) ? fallbackExcerpt : null,
                fallbackExcerpt
            ));
            content.setContent(firstNonBlank(
                translation != null ? translation.getContent() : null,
                language.equals(LanguageSupport.DEFAULT_LANGUAGE) ? fallbackContent : null,
                fallbackContent
            ));
            map.put(language, content);
        }

        return map;
    }

    private LocalizedContent resolveLocalizedContent(
        List<BlogPostTranslation> translations,
        String language,
        String fallbackTitle,
        String fallbackExcerpt,
        String fallbackContent
    ) {
        String resolvedLanguage = LanguageSupport.normalizeLanguage(language);
        if (resolvedLanguage == null) {
            resolvedLanguage = LanguageSupport.DEFAULT_LANGUAGE;
        }

        Map<String, LocalizedContent> map = toLocalizedContentMap(translations, fallbackTitle, fallbackExcerpt, fallbackContent);
        LocalizedContent localized = map.get(resolvedLanguage);
        if (localized == null) {
            localized = map.get(LanguageSupport.DEFAULT_LANGUAGE);
        }
        if (localized == null) {
            localized = new LocalizedContent();
            localized.setTitle(fallbackTitle);
            localized.setExcerpt(fallbackExcerpt);
            localized.setContent(fallbackContent);
        }
        return localized;
    }

    private String extractValue(Map<String, LocalizedContent> requested, String language, Field field) {
        if (requested == null) {
            return null;
        }

        LocalizedContent content = requested.get(language);
        if (content == null) {
            return null;
        }

        return switch (field) {
            case TITLE -> trimToNull(content.getTitle());
            case EXCERPT -> trimToNull(content.getExcerpt());
            case CONTENT -> trimToNull(content.getContent());
        };
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

    private enum Field {
        TITLE,
        EXCERPT,
        CONTENT
    }
}
