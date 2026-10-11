package com.terranangel.mp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.terranangel.mp.entity.Article;

/**
 * 文章 Mapper（MyBatis-Plus）。
 * <p>
 * 【核心优势】只要继承 BaseMapper&lt;Article&gt;，就自动拥有全套 CRUD 方法：
 *   insert / deleteById / updateById / selectById / selectList / selectPage ...
 * 一行 SQL 都不用写。
 * <p>
 * 【对比 JPA】和 JpaRepository 是同一个思路：
 *   JPA:           extends JpaRepository<Article, Long>
 *   MyBatis-Plus:  extends BaseMapper<Article>
 * <p>
 * 【复杂查询】需要写 SQL 时，可以用 @Select 注解或 XML（本项目用注解方式演示）。
 */
public interface ArticleMapper extends BaseMapper<Article> {
}
