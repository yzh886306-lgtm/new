package com.library.mapper;

import com.library.dto.ReaderQuery;
import com.library.entity.Reader;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReaderMapper {
    long count(ReaderQuery query);

    List<Reader> selectPage(ReaderQuery query);

    Reader selectById(Long id);

    Reader selectByCardNo(String cardNo);

    int insert(Reader reader);

    int update(Reader reader);

    /** 多个参数时必须用 @Param 起名，XML 里才能用 #{id}、#{status} 引用 */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int deleteById(Long id);
}
