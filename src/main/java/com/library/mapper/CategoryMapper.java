package com.library.mapper;

import com.library.entity.Category;

import java.util.List;

/** Mapper 接口只声明"要做什么"，SQL 写在 resources/mapper/CategoryMapper.xml，靠 namespace + 方法名对上 */
public interface CategoryMapper {
    List<Category> selectAll();

    Category selectById(Long id);

    Category selectByName(String name);

    /** 返回影响行数；插入后自增主键会回填到 category.id */
    int insert(Category category);

    int update(Category category);

    int deleteById(Long id);
}
