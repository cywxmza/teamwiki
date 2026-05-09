package com.teamwiki.service;

import com.teamwiki.dto.*;
import com.teamwiki.entity.*;
import com.teamwiki.exception.BusinessException;
import com.teamwiki.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final DocumentPermissionRepository permissionRepository;
    private final FavoriteRepository favoriteRepository;
    private final BrowseHistoryRepository browseHistoryRepository;
    private final DocumentVersionRepository versionRepository;
    private final CommentRepository commentRepository;
    private final ActivityRepository activityRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final AuditLogService auditLogService;

    public List<UserDTO> getCollaborators(Long documentId) {
        return permissionRepository.findByDocumentId(documentId).stream()
                .map(p -> toUserDTO(p.getUser()))
                .distinct()
                .collect(Collectors.toList());
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "documents", allEntries = true),
        @CacheEvict(value = "hotDocs", allEntries = true),
        @CacheEvict(value = "dashboard", allEntries = true)
    })
    public DocumentDTO createDocument(DocumentRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));

        if (request.getKnowledgeBaseId() == null) {
            throw new BusinessException("文档必须属于一个知识库", HttpStatus.BAD_REQUEST);
        }

        KnowledgeBase kb = knowledgeBaseRepository.findById(request.getKnowledgeBaseId())
                .orElseThrow(() -> new BusinessException("知识库不存在", HttpStatus.NOT_FOUND));

        Document doc = Document.builder()
                .title(request.getTitle())
                .content(request.getContent() != null ? request.getContent() : "")
                .summary(request.getSummary())
                .visibility(request.getVisibility() != null ? Document.DocVisibility.valueOf(request.getVisibility()) : Document.DocVisibility.PRIVATE)
                .allowComment(request.getAllowComment() != null ? request.getAllowComment() : true)
                .knowledgeBase(kb)
                .createdBy(user)
                .lastEditedBy(user)
                .build();

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BusinessException("分类不存在", HttpStatus.NOT_FOUND));
            doc.setCategory(category);
        }

        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            List<Tag> tags = tagRepository.findAllById(request.getTagIds());
            doc.setTags(tags);
        }

        doc = documentRepository.save(doc);

        DocumentPermission permission = DocumentPermission.builder()
                .document(doc)
                .user(user)
                .permissionLevel(DocumentPermission.PermissionLevel.MANAGE)
                .build();
        permissionRepository.save(permission);

        Activity activity = Activity.builder()
                .user(user)
                .document(doc)
                .type(Activity.ActivityType.CREATE)
                .description(user.getNickname() + " 创建了文档 " + doc.getTitle())
                .build();
        activityRepository.save(activity);

        auditLogService.logDocumentAccess(user, doc, "CREATE");

        return toDocumentDTO(doc, user.getId());
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "documents", allEntries = true),
        @CacheEvict(value = "hotDocs", allEntries = true),
        @CacheEvict(value = "dashboard", allEntries = true)
    })
    public DocumentDTO updateDocument(Long id, DocumentRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));

        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("文档不存在", HttpStatus.NOT_FOUND));

        if (doc.getIsDeleted()) {
            throw new BusinessException("文档已删除", HttpStatus.BAD_REQUEST);
        }

        doc.setTitle(request.getTitle());
        if (request.getContent() != null) doc.setContent(request.getContent());
        if (request.getSummary() != null) doc.setSummary(request.getSummary());
        if (request.getVisibility() != null) doc.setVisibility(Document.DocVisibility.valueOf(request.getVisibility()));
        if (request.getAllowComment() != null) doc.setAllowComment(request.getAllowComment());
        if (request.getKnowledgeBaseId() != null) {
            KnowledgeBase kb = knowledgeBaseRepository.findById(request.getKnowledgeBaseId())
                    .orElseThrow(() -> new BusinessException("知识库不存在", HttpStatus.NOT_FOUND));
            doc.setKnowledgeBase(kb);
        }
        doc.setLastEditedBy(user);

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BusinessException("分类不存在", HttpStatus.NOT_FOUND));
            doc.setCategory(category);
        } else {
            doc.setCategory(null);
        }

        if (request.getTagIds() != null) {
            List<Tag> tags = tagRepository.findAllById(request.getTagIds());
            doc.setTags(tags);
        }

        doc = documentRepository.save(doc);

        Activity activity = Activity.builder()
                .user(user)
                .document(doc)
                .type(Activity.ActivityType.EDIT)
                .description(user.getNickname() + " 编辑了文档 " + doc.getTitle())
                .build();
        activityRepository.save(activity);

        return toDocumentDTO(doc, user.getId());
    }

    @Transactional
    public void autoSaveDraft(Long id, String content, String username) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("文档不存在", HttpStatus.NOT_FOUND));
        doc.setContent(content);
        documentRepository.save(doc);
    }

    @Transactional
    public DocumentVersionDTO publishDocument(Long id, String changeDescription, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));

        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("文档不存在", HttpStatus.NOT_FOUND));

        DocumentVersion lastVersion = versionRepository.findTopByDocumentIdOrderByVersionNumberDesc(id)
                .orElse(null);

        int newVersionNumber = (lastVersion != null) ? lastVersion.getVersionNumber() + 1 : 1;

        DocumentVersion version = DocumentVersion.builder()
                .document(doc)
                .versionNumber(newVersionNumber)
                .contentSnapshot(doc.getContent())
                .changeDescription(changeDescription)
                .createdBy(user)
                .build();

        version = versionRepository.save(version);
        doc.setPublishedAt(LocalDateTime.now());
        documentRepository.save(doc);

        return DocumentVersionDTO.builder()
                .id(version.getId())
                .versionNumber(version.getVersionNumber())
                .changeDescription(version.getChangeDescription())
                .createdBy(toUserDTO(user))
                .createdAt(version.getCreatedAt())
                .build();
    }

    public DocumentDTO getDocument(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));

        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("文档不存在", HttpStatus.NOT_FOUND));

        if (doc.getIsDeleted()) {
            throw new BusinessException("文档已删除", HttpStatus.NOT_FOUND);
        }

        doc.setViewCount(doc.getViewCount() + 1);
        doc = documentRepository.save(doc);

        BrowseHistory history = BrowseHistory.builder()
                .user(user)
                .document(doc)
                .build();
        browseHistoryRepository.save(history);

        return toDocumentDTO(doc, user.getId());
    }

    @Cacheable(value = "documents", key = "{#page, #size, #keyword, #categoryId, #tagId, #kbId, #sort}")
    public PageResponse<DocumentDTO> getDocuments(int page, int size, String keyword, Long categoryId, Long tagId, Long kbId, String sort, String username) {
        User currentUser = userRepository.findByUsername(username).orElse(null);
        Sort sortOrder = getSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<Document> docPage;

        if (keyword != null && !keyword.isEmpty()) {
            docPage = documentRepository.searchByKeyword(keyword, pageable);
        } else if (kbId != null) {
            docPage = documentRepository.findByKnowledgeBaseIdAndIsDeletedFalse(kbId, pageable);
        } else if (categoryId != null) {
            docPage = documentRepository.findByCategoryIdAndIsDeletedFalse(categoryId, pageable);
        } else if (tagId != null) {
            docPage = documentRepository.findByTagIdAndIsDeletedFalse(tagId, pageable);
        } else {
            docPage = documentRepository.findByIsDeletedFalse(pageable);
        }

        boolean isAdmin = currentUser != null && currentUser.getRole() == User.UserRole.ADMIN;
        Long userId = currentUser != null ? currentUser.getId() : null;

        List<DocumentDTO> docs = docPage.getContent().stream()
                .filter(doc -> isVisibleToUser(doc, userId, isAdmin))
                .map(doc -> toDocumentDTO(doc, userId))
                .collect(Collectors.toList());

        return PageResponse.<DocumentDTO>builder()
                .content(docs)
                .totalElements(docPage.getTotalElements())
                .totalPages(docPage.getTotalPages())
                .currentPage(page)
                .pageSize(size)
                .build();
    }

    private boolean isVisibleToUser(Document doc, Long userId, boolean isAdmin) {
        if (isAdmin) return true;
        if (doc.getVisibility() == Document.DocVisibility.PUBLIC) return true;
        if (doc.getVisibility() == Document.DocVisibility.TEAM) return true;
        if (userId != null && doc.getCreatedBy() != null && doc.getCreatedBy().getId().equals(userId)) return true;
        if (userId != null && permissionRepository.findByDocumentIdAndUserId(doc.getId(), userId).isPresent()) return true;
        return false;
    }

    public PageResponse<DocumentDTO> getMyDocuments(String username, int page, int size, String tab) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Document> docPage;

        if ("participated".equals(tab)) {
            docPage = documentRepository.findParticipatedDocuments(user.getId(), pageable);
        } else {
            docPage = documentRepository.findByCreatedByIdAndIsDeletedFalse(user.getId(), pageable);
        }

        List<DocumentDTO> docs = docPage.getContent().stream()
                .map(doc -> toDocumentDTO(doc, user.getId()))
                .collect(Collectors.toList());

        return PageResponse.<DocumentDTO>builder()
                .content(docs)
                .totalElements(docPage.getTotalElements())
                .totalPages(docPage.getTotalPages())
                .currentPage(page)
                .pageSize(size)
                .build();
    }

    public List<DocumentDTO> getRecentDocuments(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));
        List<Document> docs = documentRepository.findRecentByUser(user.getId(), PageRequest.of(0, 10));
        return docs.stream().map(doc -> toDocumentDTO(doc, user.getId())).collect(Collectors.toList());
    }

    @Cacheable(value = "hotDocs", key = "'top5'")
    public List<DocumentDTO> getHotDocuments() {
        List<Document> docs = documentRepository.findTopByViewCount(PageRequest.of(0, 5));
        return docs.stream().map(doc -> toDocumentDTO(doc, null)).collect(Collectors.toList());
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "documents", allEntries = true),
        @CacheEvict(value = "hotDocs", allEntries = true),
        @CacheEvict(value = "dashboard", allEntries = true)
    })
    public void deleteDocument(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));

        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("文档不存在", HttpStatus.NOT_FOUND));

        doc.setIsDeleted(true);
        doc.setDeletedAt(LocalDateTime.now());
        documentRepository.save(doc);

        Activity activity = Activity.builder()
                .user(user)
                .document(doc)
                .type(Activity.ActivityType.DELETE)
                .description(user.getNickname() + " 删除了文档 " + doc.getTitle())
                .build();
        activityRepository.save(activity);
        auditLogService.logDocumentAccess(user, doc, "DELETE");
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "documents", allEntries = true),
        @CacheEvict(value = "dashboard", allEntries = true)
    })
    public void restoreDocument(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));

        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("文档不存在", HttpStatus.NOT_FOUND));

        doc.setIsDeleted(false);
        doc.setDeletedAt(null);
        documentRepository.save(doc);

        Activity activity = Activity.builder()
                .user(user)
                .document(doc)
                .type(Activity.ActivityType.RESTORE)
                .description(user.getNickname() + " 恢复了文档 " + doc.getTitle())
                .build();
        activityRepository.save(activity);
    }

    @Transactional
    public void permanentDeleteDocument(Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("文档不存在", HttpStatus.NOT_FOUND));
        commentRepository.clearParentReferencesByDocumentId(id);
        commentRepository.deleteByDocumentId(id);
        favoriteRepository.deleteByDocumentId(id);
        permissionRepository.deleteByDocumentId(id);
        browseHistoryRepository.deleteByDocumentId(id);
        versionRepository.deleteByDocumentId(id);
        activityRepository.deleteByDocumentId(id);
        documentRepository.delete(doc);
    }

    public PageResponse<DocumentDTO> getDeletedDocuments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "deletedAt"));
        Page<Document> docPage = documentRepository.findByIsDeletedTrue(pageable);

        List<DocumentDTO> docs = docPage.getContent().stream()
                .map(doc -> toDocumentDTO(doc, null))
                .collect(Collectors.toList());

        return PageResponse.<DocumentDTO>builder()
                .content(docs)
                .totalElements(docPage.getTotalElements())
                .totalPages(docPage.getTotalPages())
                .currentPage(page)
                .pageSize(size)
                .build();
    }

    @Transactional
    public void toggleFavorite(Long documentId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));

        if (favoriteRepository.existsByUserIdAndDocumentId(user.getId(), documentId)) {
            favoriteRepository.deleteByUserIdAndDocumentId(user.getId(), documentId);
        } else {
            Document doc = documentRepository.findById(documentId)
                    .orElseThrow(() -> new BusinessException("文档不存在", HttpStatus.NOT_FOUND));
            Favorite favorite = Favorite.builder().user(user).document(doc).build();
            favoriteRepository.save(favorite);
        }
    }

    public List<DocumentDTO> getFavorites(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));

        return favoriteRepository.findByUserId(user.getId()).stream()
                .map(fav -> toDocumentDTO(fav.getDocument(), user.getId()))
                .collect(Collectors.toList());
    }

    public PageResponse<DocumentVersionDTO> getDocumentVersions(Long documentId, int page, int size) {
        List<DocumentVersion> versions = versionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId);

        return PageResponse.<DocumentVersionDTO>builder()
                .content(versions.stream().map(v -> DocumentVersionDTO.builder()
                        .id(v.getId())
                        .versionNumber(v.getVersionNumber())
                        .changeDescription(v.getChangeDescription())
                        .createdBy(toUserDTO(v.getCreatedBy()))
                        .createdAt(v.getCreatedAt())
                        .build()).collect(Collectors.toList()))
                .totalElements((long) versions.size())
                .totalPages(1)
                .currentPage(0)
                .pageSize(size)
                .build();
    }

    public DocumentVersionDTO getVersionDetail(Long documentId, Long versionId) {
        DocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException("版本不存在", HttpStatus.NOT_FOUND));
        return DocumentVersionDTO.builder()
                .id(version.getId())
                .versionNumber(version.getVersionNumber())
                .changeDescription(version.getChangeDescription())
                .createdBy(toUserDTO(version.getCreatedBy()))
                .createdAt(version.getCreatedAt())
                .build();
    }

    @Transactional
    public void rollbackToVersion(Long documentId, Long versionId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));

        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("文档不存在", HttpStatus.NOT_FOUND));

        DocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException("版本不存在", HttpStatus.NOT_FOUND));

        doc.setContent(version.getContentSnapshot());
        doc.setLastEditedBy(user);
        documentRepository.save(doc);

        Activity activity = Activity.builder()
                .user(user)
                .document(doc)
                .type(Activity.ActivityType.EDIT)
                .description(user.getNickname() + " 回滚了文档 " + doc.getTitle() + " 到版本 " + version.getVersionNumber())
                .build();
        activityRepository.save(activity);
    }

    private Sort getSort(String sort) {
        if (sort == null) return Sort.by(Sort.Direction.DESC, "updatedAt");
        return switch (sort) {
            case "title" -> Sort.by(Sort.Direction.ASC, "title");
            case "viewCount" -> Sort.by(Sort.Direction.DESC, "viewCount");
            default -> Sort.by(Sort.Direction.DESC, "updatedAt");
        };
    }

    @Transactional
    public String enableShare(Long id, String username) {
        userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("文档不存在", HttpStatus.NOT_FOUND));

        if (doc.getShareToken() == null || doc.getShareToken().isEmpty()) {
            doc.setShareToken(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        }
        doc.setShareEnabled(true);
        documentRepository.save(doc);
        return doc.getShareToken();
    }

    @Transactional
    public void disableShare(Long id, String username) {
        userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("文档不存在", HttpStatus.NOT_FOUND));
        doc.setShareEnabled(false);
        documentRepository.save(doc);
    }

    public DocumentDTO getSharedDocument(String shareToken) {
        Document doc = documentRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new BusinessException("文档不存在或分享已关闭", HttpStatus.NOT_FOUND));

        if (!Boolean.TRUE.equals(doc.getShareEnabled())) {
            throw new BusinessException("分享已关闭", HttpStatus.FORBIDDEN);
        }

        if (doc.getIsDeleted()) {
            throw new BusinessException("文档已删除", HttpStatus.NOT_FOUND);
        }

        doc.setViewCount(doc.getViewCount() + 1);
        documentRepository.save(doc);

        return toDocumentDTO(doc, null);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "documents", allEntries = true),
        @CacheEvict(value = "hotDocs", allEntries = true),
        @CacheEvict(value = "dashboard", allEntries = true)
    })
    public int batchDelete(List<Long> ids, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));
        int count = 0;
        for (Long id : ids) {
            documentRepository.findById(id).ifPresent(doc -> {
                doc.setIsDeleted(true);
                doc.setDeletedAt(LocalDateTime.now());
                documentRepository.save(doc);
            });
            count++;
        }
        auditLogService.log(user, "BATCH_DELETE", "DOCUMENT", null,
                "批量删除 " + count + " 个文档", ids.toString());
        return count;
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "documents", allEntries = true),
        @CacheEvict(value = "dashboard", allEntries = true)
    })
    public int batchRestore(List<Long> ids, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));
        int count = 0;
        for (Long id : ids) {
            documentRepository.findById(id).ifPresent(doc -> {
                if (doc.getIsDeleted()) {
                    doc.setIsDeleted(false);
                    doc.setDeletedAt(null);
                    documentRepository.save(doc);
                }
            });
            count++;
        }
        return count;
    }

    @Transactional
    public void batchTagDocuments(List<Long> docIds, Long tagId, String username) {
        userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new BusinessException("标签不存在", HttpStatus.NOT_FOUND));
        for (Long docId : docIds) {
            documentRepository.findById(docId).ifPresent(doc -> {
                if (!doc.getTags().contains(tag)) {
                    doc.getTags().add(tag);
                    documentRepository.save(doc);
                }
            });
        }
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("totalDocuments", documentRepository.count());
        stats.put("totalDeleted", documentRepository.findByIsDeletedTrue(PageRequest.of(0, 1)).getTotalElements());
        stats.put("totalKnowledgeBases", knowledgeBaseRepository.count());
        stats.put("totalUsers", userRepository.count());
        List<Document> top5 = documentRepository.findTopByViewCount(PageRequest.of(0, 5));
        stats.put("topDocs", top5.stream().map(d -> toDocumentDTO(d, null)).collect(Collectors.toList()));
        return stats;
    }

    private DocumentDTO toDocumentDTO(Document doc, Long currentUserId) {
        Boolean isFavorited = currentUserId != null && favoriteRepository.existsByUserIdAndDocumentId(currentUserId, doc.getId());
        Long commentCount = commentRepository.countByDocumentId(doc.getId());

        List<UserDTO> collaborators = permissionRepository.findByDocumentId(doc.getId()).stream()
                .map(p -> toUserDTO(p.getUser()))
                .collect(Collectors.toList());

        return DocumentDTO.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .content(doc.getContent())
                .summary(doc.getSummary())
                .visibility(doc.getVisibility().name())
                .allowComment(doc.getAllowComment())
                .viewCount(doc.getViewCount())
                .commentCount(commentCount)
                .category(doc.getCategory() != null ? CategoryDTO.builder()
                        .id(doc.getCategory().getId())
                        .name(doc.getCategory().getName())
                        .build() : null)
                .knowledgeBase(doc.getKnowledgeBase() != null ? KnowledgeBaseDTO.builder()
                        .id(doc.getKnowledgeBase().getId())
                        .name(doc.getKnowledgeBase().getName())
                        .visibility(doc.getKnowledgeBase().getVisibility().name())
                        .build() : null)
                .createdBy(doc.getCreatedBy() != null ? toUserDTO(doc.getCreatedBy()) : null)
                .lastEditedBy(doc.getLastEditedBy() != null ? toUserDTO(doc.getLastEditedBy()) : null)
                .tags(doc.getTags() != null ? doc.getTags().stream().map(t -> TagDTO.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .color(t.getColor())
                        .build()).collect(Collectors.toList()) : List.of())
                .collaborators(collaborators)
                .isFavorited(isFavorited)
                .shareToken(doc.getShareToken())
                .shareEnabled(doc.getShareEnabled())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .publishedAt(doc.getPublishedAt())
                .build();
    }

    private UserDTO toUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .department(user.getDepartment())
                .role(user.getRole().name())
                .enabled(user.getEnabled())
                .build();
    }
}
