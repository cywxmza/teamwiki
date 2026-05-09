package com.teamwiki.service;

import com.teamwiki.dto.DocumentDTO;
import com.teamwiki.dto.PageResponse;
import com.teamwiki.entity.BrowseHistory;
import com.teamwiki.entity.User;
import com.teamwiki.exception.BusinessException;
import com.teamwiki.repository.BrowseHistoryRepository;
import com.teamwiki.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BrowseHistoryService {

    private final BrowseHistoryRepository browseHistoryRepository;
    private final UserRepository userRepository;

    public PageResponse<DocumentDTO> getBrowseHistory(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", org.springframework.http.HttpStatus.NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<BrowseHistory> historyPage = browseHistoryRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        List<DocumentDTO> docs = historyPage.getContent().stream()
                .map(h -> DocumentDTO.builder()
                        .id(h.getDocument().getId())
                        .title(h.getDocument().getTitle())
                        .summary(h.getDocument().getSummary())
                        .createdAt(h.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return PageResponse.<DocumentDTO>builder()
                .content(docs)
                .totalElements(historyPage.getTotalElements())
                .totalPages(historyPage.getTotalPages())
                .currentPage(page)
                .pageSize(size)
                .build();
    }

    @Transactional
    public void clearBrowseHistory(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", org.springframework.http.HttpStatus.NOT_FOUND));
        browseHistoryRepository.deleteByUserId(user.getId());
    }
}
