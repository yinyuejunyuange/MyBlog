package org.oyyj.blogservice.util;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;



/**
 * MD格式文字处理工具类
 */

public class MDUtil {

    private static final String EM_START = "\u0001";
    private static final String EM_END   = "\u0002";

    private static final Parser PARSER = Parser.builder().build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().build();

    /**
     * Markdown → 纯文本，但保留 <em> 高亮
     */
    public static String mdToTextKeepHighlight(String md) {
        if (md == null) return "";

        // 1. 临时替换 <em>
        String temp = md
                .replace("<em>", EM_START)
                .replace("</em>", EM_END);

        // 2. MD -> HTML
        Node document = PARSER.parse(temp);
        String html = RENDERER.render(document);

        // 3. HTML -> 纯文本（去标签）
        String text = html.replaceAll("<[^>]+>", "");

        // 4. 还原 <em>
        return text
                .replace(EM_START, "<em>")
                .replace(EM_END, "</em>");
    }
}
