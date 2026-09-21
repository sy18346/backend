package com.example.backend.service;
import com.example.backend.dto.CommentRequest;
import com.example.backend.dto.CommentResponse;
import com.example.backend.entity.Comment;
import com.example.backend.entity.CommentLike;
import com.example.backend.entity.Post;
import com.example.backend.entity.User;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.CommentLikeRepository;
import com.example.backend.repository.CommentRepository;
import com.example.backend.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final CommentLikeRepository commentLikeRepository;

    public CommentResponse createComment(Long postId, CommentRequest request) {

        User currentUser = getCurrentUserFromContext();

        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다. (ID: " + postId + ")"));

        Comment comment = Comment.builder()
                .text(request.getText())
                .post(post)
                .user(currentUser)
                .build();

        if (request.getParentId() != null) {
            Comment parentComment = commentRepository.findByIdAndDeletedAtIsNull(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("부모 댓글을 찾을 수 없습니다. (ID: " + request.getParentId() + ")"));

            if (!parentComment.getPost().getId().equals(postId)) {
                throw new UnauthorizedException("부모 댓글이 해당 게시글에 속하지 않습니다.");
            }

            comment.setParent(parentComment);
            Long replyCount = commentRepository.countByParentIdAndDeletedAtIsNull(parentComment.getId());
            comment.setSortOrder(replyCount != null ? replyCount.intValue() : 0);
        } else {
            Long topLevelCount = commentRepository.countByPostIdAndParentIsNullAndDeletedAtIsNull(postId);
            comment.setSortOrder(topLevelCount != null ? topLevelCount.intValue() : 0);
        }

        comment = commentRepository.save(comment);

        CommentResponse response = mapToResponse(comment, currentUser, false);
        return response;
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByPost(Long postId, Pageable pageable) {

        User currentUser = getCurrentUserFromContext();

        if (!postRepository.existsByIdAndDeletedAtIsNull(postId)) {
            throw new ResourceNotFoundException("게시글을 찾을 수 없습니다. (ID: " + postId + ")");
        }

        Page<Comment> comments = commentRepository.findByPostIdAndParentIsNull(postId, pageable);

        return comments.map(comment -> mapToResponse(comment, currentUser, true));
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getUserComments(Long userId, Pageable pageable) {
        User currentUser = getCurrentUserFromContextOptional().orElse(null);
        
        Page<Comment> comments = commentRepository.findByUserUserIdAndDeletedAtIsNull(userId, pageable);
        
        return comments.map(comment -> mapToResponse(comment, currentUser, false));
    }

    public CommentResponse updateComment(Long postId, Long commentId, CommentRequest request) {

        User currentUser = getCurrentUserFromContext();

        Comment comment = getCommentInPost(postId, commentId);

        if (!comment.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new UnauthorizedException("댓글을 수정할 권한이 없습니다.");
        }

        comment.updateText(request.getText());

        return mapToResponse(comment, currentUser, true);
    }

    public void deleteComment(Long postId, Long commentId) {

        User currentUser = getCurrentUserFromContext();

        Comment comment = getCommentInPost(postId, commentId);

        if (!comment.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new UnauthorizedException("댓글을 삭제할 권한이 없습니다.");
        }

        comment.markAsDeleted();

    }

    public void toggleLikeComment(Long postId, Long commentId) {
        User currentUser = getCurrentUserFromContext();

        Comment comment = getCommentInPost(postId, commentId);

        Optional<CommentLike> like = commentLikeRepository.findByUserAndComment(currentUser, comment);

        if (like.isPresent()) {
            commentLikeRepository.delete(like.get());
        } else {
            CommentLike newLike = CommentLike.builder()
                    .user(currentUser)
                    .comment(comment)
                    .build();
            commentLikeRepository.save(newLike);
        }
    }

    private CommentResponse mapToResponse(Comment comment, User currentUser, boolean includeReplies) {
        CommentResponse response = CommentResponse.fromEntity(comment);

        boolean isDeleted = comment.getDeletedAt() != null;
        if (!isDeleted) {
            boolean isLiked = commentLikeRepository.existsByUserAndComment(currentUser, comment);
            Long likeCount = commentLikeRepository.countByCommentId(comment.getId());
            response.setLiked(isLiked);
            response.setLikeCount(likeCount != null ? likeCount.intValue() : 0);
        } else {
            response.setLiked(false);
            response.setLikeCount(0);
        }

        if (includeReplies) {
            List<Comment> childComments = commentRepository.findByParentIdOrderBySortOrderAsc(comment.getId());
            if (!childComments.isEmpty()) {
                List<CommentResponse> replyResponses = childComments.stream()
                        .map(child -> mapToResponse(child, currentUser, true))
                        .collect(Collectors.toList());
                response.setReplies(replyResponses);
            }
        }

        return response;
    }

    private Comment getCommentInPost(Long postId, Long commentId) {
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "댓글을 찾을 수 없습니다. (ID: " + commentId + ")"
                        )
                );

        if (!comment.getPost().getId().equals(postId)) {
            throw new ResourceNotFoundException(
                    "해당 게시글의 댓글을 찾을 수 없습니다."
            );
        }

        return comment;
    }

    private User getCurrentUserFromContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("로그인이 필요하거나, 유효한 토큰이 아닙니다.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User) {
            return (User) principal;
        } else {
            throw new ResourceNotFoundException("유저 인증 정보가 올바르지 않습니다. (Principal: " + principal.toString() + ")");
        }
    }

    private Optional<User> getCurrentUserFromContextOptional() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || authentication.getPrincipal() == null || "anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.empty();
            }

            Object principal = authentication.getPrincipal();

            if (principal instanceof User) {
                return Optional.of((User) principal);
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}

