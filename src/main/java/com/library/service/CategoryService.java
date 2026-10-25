package com.library.service;

import com.library.common.BusinessException;
import com.library.dto.CategoryDTO;
import com.library.entity.Category;
import com.library.mapper.BookMapper;
import com.library.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service 层放"业务规则"：Controller 只管收参数、回结果，Mapper 只管执行 SQL，
 * "名字不能重复""有书的分类不能删"这类判断都在这里。
 */
@Service
// Lombok 为所有 final 字段生成构造方法，Spring 通过构造方法注入依赖（比字段上写 @Autowired 更推荐）
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final BookMapper bookMapper;

    public List<Category> list() {
        return categoryMapper.selectAll();
    }

    public Long create(CategoryDTO dto) {
        // 先查一次给出友好提示；并发时两个请求可能都查不到，最终由唯一索引兜底（见 GlobalExceptionHandler）
        if (categoryMapper.selectByName(dto.getName()) != null) {
            throw new BusinessException("分类名已存在");
        }
        Category category = new Category();
        category.setName(dto.getName());
        categoryMapper.insert(category);
        return category.getId();
    }

    public void update(Long id, CategoryDTO dto) {
        getOrThrow(id);
        Category sameName = categoryMapper.selectByName(dto.getName());
        // 同名的是"别的分类"才算冲突，改成自己原来的名字是允许的
        if (sameName != null && !sameName.getId().equals(id)) {
            throw new BusinessException("分类名已存在");
        }
        Category category = new Category();
        category.setId(id);
        category.setName(dto.getName());
        categoryMapper.update(category);
    }

    public void delete(Long id) {
        getOrThrow(id);
        // 没有物理外键，"分类下还有书就不许删"只能在这里校验
        if (bookMapper.countByCategoryId(id) > 0) {
            throw new BusinessException("该分类下还有图书，不能删除");
        }
        categoryMapper.deleteById(id);
    }

    public Category getOrThrow(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        return category;
    }
}
