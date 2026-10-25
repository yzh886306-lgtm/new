package com.library.mapper;

import com.library.dto.BookQuery;
import com.library.entity.Book;
import com.library.vo.BookVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BookMapper {
    /** 分页两条 SQL：先 count 再查当前页，条件必须完全一致 */
    long count(BookQuery query);

    List<BookVO> selectPage(BookQuery query);

    Book selectById(Long id);

    Book selectByIsbn(String isbn);

    /** 统计某分类下有几本书：删除分类前检查，防止留下"孤儿"图书 */
    long countByCategoryId(Long categoryId);

    int insert(Book book);

    int update(Book book);

    int deleteById(Long id);

    /**
     * 借书扣库存：UPDATE ... SET available_stock = available_stock - 1 WHERE id = ? AND available_stock > 0
     * 返回 0 表示库存已被别人借光（并发安全的关键，见 XML 注释）
     */
    int decreaseStock(@Param("id") Long id);

    /** 还书加库存 */
    int increaseStock(@Param("id") Long id);
}
