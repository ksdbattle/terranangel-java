package com.terranangel.context;

/**
 * 当前登录用户上下文（Scoped Value）。
 * <p>
 * 【用法】
 * <pre>
 *   CurrentUser user = UserContext.get();       // 未绑定返回 null
 *   CurrentUser user = UserContext.require();   // 未绑定抛异常
 *   String name = UserContext.username();       // 便捷取用户名
 * </pre>
 * <p>
 * 【谁负责绑定】Web 层由 JWT 过滤器在请求进入时绑定，请求结束自动失效。
 * 业务代码只读，不需要关心来源。
 * <p>
 * 【为什么用 ScopedValue 而不是 ThreadLocal】
 *   ThreadLocal 必须"用完手动 clear"，一旦某条代码路径忘了写（尤其是写在
 *   doFilter 之后而不是 finally 里），池线程被复用时就会把上一个用户的身份
 *   泄漏给下一个请求——这是最严重的越权漏洞，而且偶发、难复现。
 *   ScopedValue 把生命周期交给作用域结构：{@link #call} 一结束就自动失效，
 *   从根上杜绝"忘记清理"，也不会泄漏到下一个请求。
 * <p>
 * 【注意】作用域内的代码（含异步派生的子任务之外）才能读到；跨线程池时
 * 上下文不会自动传播，这一点和 ThreadLocal 一致。
 */
public final class UserContext {

    /** 作用域值：只有绑定了才可读，作用域结束自动失效 */
    private static final ScopedValue<CurrentUser> USER = ScopedValue.newInstance();

    private UserContext() {
    }

    /**
     * 在指定用户的作用域内执行操作。
     * <p>
     * 作用域结束后用户自动失效，无需（也无法）手动清理。
     *
     * @param user 当前登录用户
     * @param op   作用域内要执行的操作
     * @return 操作返回值
     */
    public static <T, X extends Throwable> T call(CurrentUser user, ScopedValue.CallableOp<T, X> op) throws X {
        return ScopedValue.where(USER, user).call(op);
    }

    /** 在指定用户的作用域内执行一段无返回值、不抛受检异常的代码 */
    public static void run(CurrentUser user, Runnable op) {
        ScopedValue.where(USER, user).run(op);
    }

    /** 获取当前用户，未绑定返回 null */
    public static CurrentUser get() {
        // 注意：不能用 orElse(null)——ScopedValue.orElse 的入参不允许为 null，
        // 传 null 会直接抛 NullPointerException，必须先判断是否已绑定。
        return USER.isBound() ? USER.get() : null;
    }

    /** 获取当前用户，未绑定直接抛异常 */
    public static CurrentUser require() {
        CurrentUser user = get();
        if (user == null) {
            throw new IllegalStateException("当前没有登录用户");
        }
        return user;
    }

    /** 便捷获取用户名，未绑定返回 null */
    public static String username() {
        CurrentUser user = get();
        return user == null ? null : user.username();
    }

    /** 便捷获取用户 ID，未绑定返回 null */
    public static Long userId() {
        CurrentUser user = get();
        return user == null ? null : user.userId();
    }
}
