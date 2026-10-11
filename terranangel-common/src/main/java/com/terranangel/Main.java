package com.terranangel;

import java.util.UUID;

//TIP 要<b>运行</b>代码，请按 <shortcut actionId="Run"/> 或
// 点击装订区域中的 <icon src="AllIcons.Actions.Execute"/> 图标。
public class Main {
    static void main() {
        //TIP 当文本光标位于高亮显示的文本处时按 <shortcut actionId="ShowIntentionActions"/>
        // 查看 IntelliJ IDEA 建议如何修正。
        IO.println(String.format("Hello and welcome!"));

        UUID id = UUID.randomUUID();

        System.out.println(id);
        System.out.println(id.version());
        System.out.println(id.variant());
    }
}
