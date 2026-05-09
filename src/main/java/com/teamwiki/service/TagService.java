package com.teamwiki.service;

import com.teamwiki.dto.TagDTO;
import com.teamwiki.entity.Tag;
import com.teamwiki.exception.BusinessException;
import com.teamwiki.repository.DocumentRepository;
import com.teamwiki.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final DocumentRepository documentRepository;

    public List<TagDTO> getAllTags() {
        return tagRepository.findAll().stream()
                .map(this::toTagDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public TagDTO createTag(TagDTO dto) {
        if (tagRepository.existsByName(dto.getName())) {
            throw new BusinessException("标签名已存在", HttpStatus.BAD_REQUEST);
        }

        Tag tag = Tag.builder()
                .name(dto.getName())
                .color(dto.getColor())
                .build();

        tag = tagRepository.save(tag);
        return toTagDTO(tag);
    }

    @Transactional
    public TagDTO updateTag(Long id, TagDTO dto) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new BusinessException("标签不存在", HttpStatus.NOT_FOUND));

        tag.setName(dto.getName());
        if (dto.getColor() != null) tag.setColor(dto.getColor());

        tag = tagRepository.save(tag);
        return toTagDTO(tag);
    }

    @Transactional
    public void deleteTag(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new BusinessException("标签不存在", HttpStatus.NOT_FOUND));
        tagRepository.delete(tag);
    }

    private TagDTO toTagDTO(Tag tag) {
        Long docCount = documentRepository.findByTagIdAndIsDeletedFalse(tag.getId(),
                org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();

        return TagDTO.builder()
                .id(tag.getId())
                .name(tag.getName())
                .color(tag.getColor())
                .documentCount(docCount.intValue())
                .build();
    }
}
