package com.terranangel.mp;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.terranangel.mp.entity.Article;
import com.terranangel.mp.service.ArticleService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文章接口（MyBatis-Plus + MySQL 演示）。
 * <p>
 * 【前提】terranangel.datasource.mysql.enabled=true 且 MySQL 已启动，表 t_article 已建。
 * <p>
 * 【测试入口】
 *   POST   /mp/article/init                          初始化演示数据
 *   POST   /mp/article?title=..&content=..&author=..  新增
 *   GET    /mp/article/{id}                          按 id 查
 *   GET    /mp/article/all                           查全部
 *   GET    /mp/article/search?author=张三             条件查询
 *   GET    /mp/article/page?current=1&size=2         分页查询
 *   PUT    /mp/article/{id}?title=..&content=..       更新
 *   DELETE /mp/article/{id}                           删除
 */
@RestController
@RequestMapping("/mp/article")
@ConditionalOnProperty(prefix = "terranangel.datasource.mysql", name = "enabled", havingValue = "true")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    /** 初始化演示数据 */
    @PostMapping("/init")
    public String init() {
        return articleService.initDemoData();
    }

    /** 新增文章 */
    @PostMapping
    public Article create(@RequestParam String title,
                          @RequestParam String content,
                          @RequestParam String author) {
        return articleService.create(new Article(title, content, author));
    }

    /** 按 id 查 */
    @GetMapping("/{id}")
    public Article getById(@PathVariable Long id) {
        return articleService.getById(id);
    }

    /** 查全部 */
    @GetMapping("/all")
    public List<Article> listAll() {
        return articleService.listAll();
    }

    /** 条件查询（按作者/标题） */
    @GetMapping("/search")
    public List<Article> search(@RequestParam(required = false) String author,
                                @RequestParam(required = false) String title) {
        return articleService.search(author, title);
    }

    /** 分页查询 */
    @GetMapping("/page")
    public IPage<Article> page(@RequestParam(defaultValue = "1") int current,
                               @RequestParam(defaultValue = "10") int size) {
        return articleService.page(current, size);
    }

    /** 更新 */
    @PutMapping("/{id}")
    public Article update(@PathVariable Long id,
                          @RequestParam String title,
                          @RequestParam String content) {
        return articleService.update(id, title, content);
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        articleService.delete(id);
        return "删除成功: " + id;
    }
}
