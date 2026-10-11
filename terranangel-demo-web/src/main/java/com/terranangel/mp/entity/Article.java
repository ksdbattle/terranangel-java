package com.terranangel.mp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 文章实体（MyBatis-Plus 风格）。
 * <p>
 * 【为什么用 Article 而不是 User】
 * 为了和 JPA 的 User/Order（PostgreSQL）完全隔离——业务无交集、表名不同、库不同，
 * 两个 ORM 各管各的，绝不会冲突。
 * <p>
 * 【MyBatis-Plus 注解】
 *   @TableName  —— 映射表名（相当于 JPA 的 @Table）
 *   @TableId    —— 主键，IdType.AUTO 表示数据库自增
 * <p>
 * 【对比 JPA】
 *   JPA:          @Entity + @Table + @Id + @GeneratedValue
 *   MyBatis-Plus: @TableName + @TableId(type = IdType.AUTO)
 * <p>
 * 【面试点】MyBatis-Plus 不需要你写 SQL，继承 BaseMapper 就有全套 CRUD，
 * 本质是"MyBatis + 增强"，比原生 MyBatis 省样板代码。
 */
@TableName("t_article")
public class Article {

    /** 主键（数据库自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 作者 */
    private String author;

    /** 浏览次数 */
    private Integer viewCount;

    /** 创建时间 */
    private LocalDateTime createdAt;

    public Article() {
    }

    public Article(String title, String content, String author) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.viewCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    // ==================== getter/setter ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
