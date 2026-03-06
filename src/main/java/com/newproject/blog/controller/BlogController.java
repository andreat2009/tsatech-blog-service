package com.newproject.blog.controller;

import com.newproject.blog.dto.BlogCommentRequest;
import com.newproject.blog.dto.BlogCommentResponse;
import com.newproject.blog.dto.BlogPostRequest;
import com.newproject.blog.dto.BlogPostResponse;
import com.newproject.blog.service.BlogService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cms/blog")
public class BlogController {
    private final BlogService service;

    public BlogController(BlogService service) {
        this.service = service;
    }

    @GetMapping("/posts")
    public List<BlogPostResponse> listPosts(@RequestParam(required = false) Boolean active) {
        return service.listPosts(active);
    }

    @GetMapping("/posts/{id}")
    public BlogPostResponse getPost(@PathVariable Long id) {
        return service.getPost(id);
    }

    @GetMapping("/posts/slug/{slug}")
    public BlogPostResponse getPostBySlug(@PathVariable String slug) {
        return service.getPostBySlug(slug);
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public BlogPostResponse createPost(@Valid @RequestBody BlogPostRequest request) {
        return service.createPost(request);
    }

    @PutMapping("/posts/{id}")
    public BlogPostResponse updatePost(@PathVariable Long id, @Valid @RequestBody BlogPostRequest request) {
        return service.updatePost(id, request);
    }

    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@PathVariable Long id) {
        service.deletePost(id);
    }

    @GetMapping("/posts/{postId}/comments")
    public List<BlogCommentResponse> listCommentsByPost(
        @PathVariable Long postId,
        @RequestParam(required = false) Boolean approved
    ) {
        return service.listCommentsByPost(postId, approved);
    }

    @GetMapping("/comments")
    public List<BlogCommentResponse> listComments(@RequestParam(required = false) Boolean approved) {
        return service.listComments(approved);
    }

    @PostMapping("/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public BlogCommentResponse createComment(
        @PathVariable Long postId,
        @Valid @RequestBody BlogCommentRequest request
    ) {
        return service.createComment(postId, request);
    }

    @PatchMapping("/comments/{id}/approval")
    public BlogCommentResponse setApproval(@PathVariable Long id, @RequestParam boolean approved) {
        return service.setCommentApproval(id, approved);
    }
}
