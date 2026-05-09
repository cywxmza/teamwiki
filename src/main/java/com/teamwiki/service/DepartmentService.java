package com.teamwiki.service;

import com.teamwiki.dto.DepartmentDTO;
import com.teamwiki.entity.Department;
import com.teamwiki.exception.BusinessException;
import com.teamwiki.repository.DepartmentRepository;
import com.teamwiki.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public List<DepartmentDTO> getAll() {
        return departmentRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<DepartmentDTO> getEnabled() {
        return departmentRepository.findByEnabledTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public DepartmentDTO getById(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("部门不存在", HttpStatus.NOT_FOUND));
        return toDTO(dept);
    }

    @Transactional
    public DepartmentDTO create(DepartmentDTO dto) {
        if (departmentRepository.existsByName(dto.getName())) {
            throw new BusinessException("部门名称已存在", HttpStatus.BAD_REQUEST);
        }
        Department dept = Department.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .leader(dto.getLeader())
                .enabled(true)
                .build();
        dept = departmentRepository.save(dept);
        return toDTO(dept);
    }

    @Transactional
    public DepartmentDTO update(Long id, DepartmentDTO dto) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("部门不存在", HttpStatus.NOT_FOUND));
        if (dto.getName() != null) dept.setName(dto.getName());
        if (dto.getDescription() != null) dept.setDescription(dto.getDescription());
        if (dto.getLeader() != null) dept.setLeader(dto.getLeader());
        if (dto.getEnabled() != null) dept.setEnabled(dto.getEnabled());
        dept = departmentRepository.save(dept);
        return toDTO(dept);
    }

    @Transactional
    public void delete(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("部门不存在", HttpStatus.NOT_FOUND));
        departmentRepository.delete(dept);
    }

    private DepartmentDTO toDTO(Department dept) {
        long count = userRepository.countByDepartment(dept.getName());
        return DepartmentDTO.builder()
                .id(dept.getId())
                .name(dept.getName())
                .description(dept.getDescription())
                .leader(dept.getLeader())
                .enabled(dept.getEnabled())
                .memberCount(count)
                .createdAt(dept.getCreatedAt())
                .build();
    }
}
