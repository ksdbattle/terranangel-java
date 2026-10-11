package com.terranangel.mp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.terranangel.mp.entity.Article;
import com.terranangel.mp.mapper.ArticleMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 文章服务（MyBatis-Plus 演示）。
 * <p>
 * 【开关】terranangel.datasource.mysql.enabled=true 才装配，否则应用照常启动。
 * <p>
 * 【事务】必须显式指定 mysqlTransactionManager，
 * 因为容器里有两个事务管理器（PG 的 + MySQL 的），不指定会报错或用错。
 */
@Service
@ConditionalOnProperty(prefix = "terranangel.datasource.mysql", name = "enabled", havingValue = "true")
public class ArticleService {

    private final ArticleMapper articleMapper;

    public ArticleService(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    /**
     * 新增文章（BaseMapper.insert，不用写 SQL）。
     */
    @Transactional(transactionManager = "mysqlTransactionManager")
    public Article create(Article article) {
        articleMapper.insert(article);
        return article;
    }

    /**
     * 按 id 查（BaseMapper.selectById）。
     */
    public Article getById(Long id) {
        return articleMapper.selectById(id);
    }

    /**
     * 查全部（BaseMapper.selectList）。
     */
    public List<Article> listAll() {
        return articleMapper.selectList(null);
    }

    /**
     * 条件查询（LambdaQueryWrapper，类型安全的条件构造器）。
     * 演示：按作者查 + 标题模糊匹配 + 按 id 倒序。
     */
    public List<Article> search(String author, String titleKeyword) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(author != null && !author.isBlank(), Article::getAuthor, author)
               .like(titleKeyword != null && !titleKeyword.isBlank(), Article::getTitle, titleKeyword)
               .orderByDesc(Article::getId);
        return articleMapper.selectList(wrapper);
    }

    /**
     * 分页查询（需要 PaginationInnerInterceptor 插件，已在 MybatisPlusConfig 注册）。
     */
    public IPage<Article> page(int current, int size) {
        return articleMapper.selectPage(new Page<>(current, size), null);
    }

    /**
     * 更新（BaseMapper.updateById，只更新非 null 字段）。
     */
    @Transactional(transactionManager = "mysqlTransactionManager")
    public Article update(Long id, String title, String content) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new RuntimeException("文章不存在: " + id);
        }
        article.setTitle(title);
        article.setContent(content);
        articleMapper.updateById(article);
        return article;
    }

    /**
     * 删除（BaseMapper.deleteById）。
     */
    @Transactional(transactionManager = "mysqlTransactionManager")
    public void delete(Long id) {
        articleMapper.deleteById(id);
    }

    /**
     * 初始化演示数据（注意：表由用户手动建，见 README 或 SQL 脚本）。
     */
    public String initDemoData() {
        List<Article> articles = List.of(
                new Article("MyBatis-Plus 入门", "MyBatis-Plus 是 MyBatis 的增强工具...", "张三"),
                new Article("JPA 与 Hibernate", "JPA 是规范，Hibernate 是实现...", "李四"),
                new Article("Spring Boot 4 新特性", "Spring Boot 4 基于 Spring Framework 7...", "张三")
        );
        articles.forEach(articleMapper::insert);
        return "已初始化 " + articles.size() + " 篇文章";
    }
}
