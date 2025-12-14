package neelesh.easy_install;

import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class MarkdownTest {
    MarkdownRenderer markdownRenderer;


    @BeforeEach
    public void setup() {
        markdownRenderer = new MarkdownRenderer("", 0, 0, 0, new TitleScreen());
    }

    @Test
    public void testMarkdownToHtml() {
        // Test plain text with no markdown
        try {
            // Test Simple Paragraph
            String markdown1 = "hi";
            String expectedHtml1 = "<p>hi</p>";
            assertEquals(expectedHtml1, testMarkdownToHtmlMethod(markdown1).replace("\n", ""));
            // Test Heading Conversion
            String markdown2 = "# Heading 1";
            String expectedHtml2 = "<h1>Heading 1</h1>";
            assertEquals(expectedHtml2, testMarkdownToHtmlMethod(markdown2).replace("\n", ""));

            // Test Bold Text
            String markdown3 = "This is **bold** text.";
            String expectedHtml3 = "<p>This is <strong>bold</strong> text.</p>";
            assertEquals(expectedHtml3, testMarkdownToHtmlMethod(markdown3).replace("\n", ""));

            // Test Italic Text
            String markdown4 = "This is *italic* text.";
            String expectedHtml4 = "<p>This is <em>italic</em> text.</p>";
            assertEquals(expectedHtml4, testMarkdownToHtmlMethod(markdown4).replace("\n", ""));

            // Test Bullet List
            String markdown5 = "* Item 1\n* Item 2\n* Item 3";
            String expectedHtml5 = "<ul><li>Item 1</li><li>Item 2</li><li>Item 3</li></ul>";
            assertEquals(expectedHtml5, testMarkdownToHtmlMethod(markdown5).replace("\n", ""));

            // Test Ordered List
            String markdown6 = "1. Item 1\n2. Item 2\n3. Item 3\n";
            String expectedHtml6 = "<ol><li>Item 1</li><li>Item 2</li><li>Item 3</li></ol>";
            assertEquals(expectedHtml6, testMarkdownToHtmlMethod(markdown6).replace("\n", ""));

            // Test Code Block
            String markdown7 = "```java\nSystem.out.println(Hello World);\n```";
            String expectedHtml7 = "<pre><code class=\"language-java\">System.out.println(Hello World);</code></pre>";
            assertEquals(expectedHtml7, testMarkdownToHtmlMethod(markdown7).replace("\n", ""));

            // Test Image
            String markdown8 = "![alt text](image_url)";
            String expectedHtml8 = "<p><img src=\"image_url\" alt=\"alt text\" /></p>";
            assertEquals(expectedHtml8, testMarkdownToHtmlMethod(markdown8).replace("\n", ""));

            // Test Link
            String markdown9 = "[Google](https://www.google.com)";
            String expectedHtml9 = "<p><a href=\"https://www.google.com\">Google</a></p>";
            assertEquals(expectedHtml9, testMarkdownToHtmlMethod(markdown9).replace("\n", ""));

            // Test Multiple Elements
            String markdown10 = "# Heading 1\nThis is **bold** text\n* Item 1\n* Item 2";
            String expectedHtml10 = "<h1>Heading 1</h1><p>This is <strong>bold</strong> text</p><ul><li>Item 1</li><li>Item 2</li></ul>";
            assertEquals(expectedHtml10, testMarkdownToHtmlMethod(markdown10).replace("\n", ""));
        } catch(Exception e) {
            fail("Caused error");
        }
    }

    @Test
    public void testMarkdownFromHtml() {
        try {
            String input = "<p><h1>My Title</h1></p>This is a paragraph with <i>italic</i> text.";
            String expectedOutput = "# My Title\nThis is a paragraph with _italic_ text.";

            assertEquals(expectedOutput, testMarkdownFromHtmlMethod(input));
            // Test case 2: Conversion with bold and italic text
            String input2 = "<p>This is <b>bold</b> and <i>italic</i> text.</p>";
            String expectedOutput2 = "This is __bold__ and _italic_ text.\n";
            assertEquals(expectedOutput2, testMarkdownFromHtmlMethod(input2));

            // Test case 3: Handling of multiple paragraphs
            String input3 = "<p>This is the first paragraph.</p><p>This is the second paragraph.</p>";
            String expectedOutput3 = "This is the first paragraph.\nThis is the second paragraph.\n";
            assertEquals(expectedOutput3, testMarkdownFromHtmlMethod(input3));

            // Test case 4: Handling of line breaks in paragraphs
            String input4 = "<p>This is a line.<br>This is a new line.</p>";
            String expectedOutput4 = "This is a line.\nThis is a new line.\n";
            assertEquals(expectedOutput4, testMarkdownFromHtmlMethod(input4));


            // Test case 5: Handling of links
            String input5 = "<p>This is a <a href='https://example.com'>link</a>.</p>";
            String expectedOutput5 = "This is a [link](https://example.com).\n";
            assertEquals(expectedOutput5, testMarkdownFromHtmlMethod(input5));

            // Test case 6: Handling of images
            String input6 = "<p><img src='image.jpg' alt='example image'/></p>";
            String expectedOutput6 = "![](image.jpg)\n";
            assertEquals(expectedOutput6, testMarkdownFromHtmlMethod(input6));

            // Test case 7: Handling of strong and emphasized text
            String input7 = "<p><strong>Strong text</strong> and <em>emphasized text</em>.</p>";
            String expectedOutput7 = "__Strong text__ and _emphasized text_.\n";
            assertEquals(expectedOutput7, testMarkdownFromHtmlMethod(input7));

        } catch(Exception e) {
            fail("Caused error");
        }
    }

    @Test
    public void testExtractTextFromHtml() {
        try {
            String input1 = "This is a <a href='https://example.com'>link</a>.";
            MutableComponent expectedOutput1 = Component.literal("This is a ").append(Component.literal("link").setStyle(Style.EMPTY.withColor(0x257DE6))).append(".");
            assertEquals(expectedOutput1, flattenText(testExtractTextFromHtmlMethod(input1)));

            String input2 = "This is <b>bold</b> and <i>italic</i> text.";
            MutableComponent expectedOutput2 = Component.literal("This is ").append(Component.literal("bold").setStyle(Style.EMPTY.withBold(true))).append(" and ").append(Component.literal("italic").setStyle(Style.EMPTY.withItalic(true))).append(" text.");
            assertEquals(expectedOutput2, flattenText(testExtractTextFromHtmlMethod(input2)));

            String input3 = "This is <strong>strong</strong> and <em>emphasized</em> text.";
            MutableComponent expectedOutput3 = Component.literal("This is ").append(Component.literal("strong").setStyle(Style.EMPTY.withBold(true))).append(" and ").append(Component.literal("emphasized").setStyle(Style.EMPTY.withItalic(true))).append(" text.");
            assertEquals(expectedOutput3, flattenText(testExtractTextFromHtmlMethod(input3)));
        } catch(Exception e) {
            fail("Caused error");
        }
    }



    private String testMarkdownFromHtmlMethod(String input) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method method = MarkdownRenderer.class.getDeclaredMethod("extractMarkdownFromHtml", String.class);
        method.setAccessible(true);
        return ((MutableComponent) method.invoke(markdownRenderer, input)).getString();
    }

    private MutableComponent testExtractTextFromHtmlMethod(String input) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method method = MarkdownRenderer.class.getDeclaredMethod("extractTextFromHtml", String.class);
        method.setAccessible(true);
        return ((MutableComponent) method.invoke(markdownRenderer, input));
    }

    private String testMarkdownToHtmlMethod(String input) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method method = MarkdownRenderer.class.getDeclaredMethod("convertMarkdownToHtml", String.class);
        method.setAccessible(true);
        return (String) method.invoke(markdownRenderer, input);
    }


    private MutableComponent flattenText(MutableComponent text) {
        MutableComponent finalText = null;
        for (Component sibling : text.getSiblings()) {
            if (!sibling.equals(Component.empty()) && finalText != null) {
                finalText.append(flattenText((MutableComponent) sibling));
            } else if (finalText == null && !sibling.equals(Component.empty())) {
                finalText = Component.literal(sibling.getString()).setStyle(sibling.getStyle());
            }
        }
        if (finalText == null) {
            finalText = text;
        }
        return finalText;
    }

}
