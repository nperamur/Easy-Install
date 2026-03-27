package neelesh.easy_install;

import neelesh.easy_install.gui.screen.MarkdownScreenInterface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.rendertype.RenderType;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.resources.Identifier;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;


public class MarkdownRenderer {
    private final ArrayList<ProjectImage> projectImages;
    private int count;
    private ArrayList<Button> linkButtons = new ArrayList<Button>();
    private ArrayList<String> linkUrls = new ArrayList<String>();
    private final ArrayList<Integer> linkIndexes = new ArrayList<Integer>();
    private final ArrayList<Integer> linkLengths = new ArrayList<Integer>();
    private final ArrayList<Integer> originalY = new ArrayList<Integer>();
    private Thread thread;
    private String body;
    private int startX;
    private int startY;
    private Screen screen;
    private int maxY;
    private int endX;
    
    public MarkdownRenderer(String body, int startX, int startY, int endX, Screen screen) {
        this.body = extractMarkdownFromHtml(body).getString();
        this.startX = startX;
        this.startY = startY;
        this.screen = screen;
        this.endX = endX;
        count = -1;
        this.projectImages = new ArrayList<>();
        if (thread != null) {
            thread.interrupt();
        }
        thread = new Thread(() ->{
            String str = "";
            boolean isImage = false;
            boolean linkInImage = false;
            boolean puttingImageUrl = false;
            boolean puttingImageWidth = false;
            String imageWidth = "";
            for (int i = 0; i < this.body.length(); i++) {
                if (i < this.body.length() - 2 && (this.body.charAt(i) == '-') && this.body.charAt(i + 1) == '\n' && (i == 0 || this.body.charAt(i - 1) != '-')) {
                    this.body = this.body.substring(0, i + 1) + " " + this.body.substring(i + 3);
                }
                if (this.body.startsWith("[![", i)) {
                    linkInImage = true;
                }
                if (this.body.startsWith("![", i)) {
                    isImage = true;
                } else if (this.body.startsWith("width=", i) && isImage) {
                    puttingImageWidth = true;
                    i += 5;
                } else if (puttingImageWidth) {
                    puttingImageWidth = Character.isDigit(this.body.charAt(i));
                    if (puttingImageWidth) {
                        imageWidth += Integer.parseInt(this.body.substring(i, i + 1));
                    }
                }
                if (this.body.charAt(i) == ')' && isImage) {
                    puttingImageUrl = false;
                    isImage = false;
                    URL url;
                    if (linkInImage && i < this.body.length() - 2 && this.body.charAt(i + 1) == '\n') {
                        this.body = this.body.substring(0, i + 1) + " " + this.body.substring(i + 2);
                    }
                    try {
                        if (!str.isEmpty() && str.substring(1).contains(" ")) {
                            str = str.substring(0, str.indexOf(" ", 1));
                        }

                        url = new URL(str); //UrlEscapers.urlFragmentEscaper().escape(str)
                        Identifier id = Identifier.fromNamespaceAndPath(EasyInstall.MOD_ID, "project_image_" + i);
                        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
                        Minecraft.getInstance().execute(() -> {
                            NativeImage image = new NativeImage(1, 1, false);
                            DynamicTexture texture = new DynamicTexture(() -> "", image);
                            textureManager.register(id, texture);
                        });
                        NativeImage image = ImageLoader.loadImage(url, id, Minecraft.getInstance());
                        if (image != null) {
                            ProjectImage projectImage = new ProjectImage(image, id, i);
                            if (!imageWidth.isEmpty()) {
                                projectImage.setWidth(Integer.parseInt(imageWidth));
                            }
                            if (linkInImage) {
                                projectImage.setLink(this.body.substring(this.body.indexOf('(', i + 1) + 1, this.body.indexOf(')', i + 1)));
                            }
                            projectImages.add(projectImage);
                        }
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    linkInImage = false;
                    imageWidth = "";
                    str = "";
                }
                if (puttingImageUrl) {
                    str += this.body.charAt(i);
                }
                if (this.body.charAt(i) == '(' && isImage) {
                    puttingImageUrl = true;
                }
            }
            Minecraft.getInstance().execute(() -> {
                count = 0;
            });
        });
        thread.start();
    }


    public void render(GuiGraphicsExtractor context, int scrollAmount) {
        int j = 0;
        for (Button linkButton : linkButtons) {
            linkButton.setY(scrollAmount + originalY.get(j));
            j++;
        }
        StringBuilder s = new StringBuilder();
        float scale = 1.0f;
        int y = startY;
        int x = startX;
        int imageIndex = 0;
        int imageHeight = 0;
        for (int i = 0; i < body.length(); i++) {
            if (projectImages.size() > imageIndex && projectImages.get(imageIndex).getPosition() == i) {
                ProjectImage image = projectImages.get(imageIndex);
                int imageWidth;
                if (!(image.getWidth() == -1) && (image.getWidth() * (endX - 150)/1000) < endX - x - 10) {
                    imageWidth = (int) (image.getWidth() * (endX - 150)/(1000 * scale));
                    context.blit(RenderPipelines.GUI_TEXTURED, image.getId(), (int)(x/scale), (int)((y+scrollAmount)/scale), 0, 0, imageWidth, image.getImage().getHeight() * imageWidth/image.getImage().getWidth(), imageWidth, image.getImage().getHeight() * imageWidth/image.getImage().getWidth());
                    imageHeight = Math.max(imageHeight, image.getImage().getHeight() * imageWidth/image.getImage().getWidth() + 10);
                    if (image.isClickable() && count == 0) {
                        createClickableImageButtons(x, y, imageWidth, image.getImage().getHeight() * imageWidth/image.getImage().getWidth(), image.getLink());
                    }
                    x+=imageWidth + 5;
                } else if (image.getImage().getWidth()/2 < (endX - 150)) {
                    if (image.getImage().getWidth()/2 >= (endX - x - 10)) {
                        x = 140;
                        y += imageHeight;
                        imageHeight = 0;
                    }
                    context.blit(RenderPipelines.GUI_TEXTURED, image.getId(), (int)(x/scale), (int)((y+scrollAmount)/scale), 0, 0, (int)(image.getImage().getWidth()/(2 * scale)), (int)(image.getImage().getHeight()/(2 * scale)), (int)(image.getImage().getWidth()/(2 * scale)), (int) (image.getImage().getHeight()/(2 * scale)));
                    imageWidth = image.getImage().getWidth()/2;
                    imageHeight = Math.max(imageHeight, image.getImage().getHeight()/2 + 10);
                    if (image.isClickable() && count == 0) {
                        createClickableImageButtons(x, y, imageWidth, image.getImage().getHeight()/2, image.getLink());
                    }
                    x+=imageWidth + 5;
                } else {
                    if (imageHeight > 0) {
                        x = 140;
                        y += imageHeight;
                        imageHeight = 0;
                    }
                    imageWidth = (endX - x - 10);
                    if (image.isClickable() && count == 0) {
                        createClickableImageButtons(x, y, imageWidth, image.getImage().getHeight() * imageWidth/image.getImage().getWidth() + 10, image.getLink());
                    }
                    context.blit(RenderPipelines.GUI_TEXTURED, image.getId(), (int)(x/scale), (int)((y+scrollAmount)/scale), 0, 0, (endX - x - 10), image.getImage().getHeight() * (int)((endX-x-10)/scale)/image.getImage().getWidth(), (int)((endX-x-10)/scale), image.getImage().getHeight() * (int)((endX-x-10)/scale)/image.getImage().getWidth());
                    y+=image.getImage().getHeight() * imageWidth/image.getImage().getWidth() + 10;
                }
                imageIndex++;
            }
            if (body.charAt(i) == '#' && s.toString().replaceAll("\\s+", "").isEmpty() || i == body.length() - 1) {
                if (i == body.length() - 1) {
                    s.append(body.charAt(i));
                }
                MutableComponent text = extractTextFromHtml(convertMarkdownToHtml(s.toString()));
                if (count == 0) {
                    putLinkButtons(text, x, y, (int) ((endX-x-10) / scale), scale);
                }
                context.textWithWordWrap(screen.getFont(), text, (int) (x/scale), (int) (y/scale + scrollAmount / scale), (int) ((endX-x-10) / scale), CommonColors.WHITE, false);
                int wrappedSize = screen.getFont().wordWrapHeight(text, (int) ((endX-x-10) / scale));
                if (scale == 1) {
                    scale = 1.4f;
                    context.pose().scale(scale, scale);
                }
                s = new StringBuilder();
                x = startX;
                y += imageHeight;
                imageHeight = 0;
                y += (int) (wrappedSize * scale);
            } else if ((body.charAt(i) == '\n')) {
                MutableComponent text = extractTextFromHtml(convertMarkdownToHtml(s.toString()));
                if (!text.getString().replaceAll("\\s+", "").isEmpty()) {
                    y += imageHeight;
                    if (imageHeight > 0) {
                        x = startX;
                    }
                    imageHeight = 0;
                }
                s = new StringBuilder(text.getString());
                if (count == 0) {
                    putLinkButtons(text, x, y, (int) ((endX-x-10) / scale), scale);
                }
                context.textWithWordWrap(screen.getFont(), text, (int) (x/(scale)), (int) (y/scale + scrollAmount / scale), (int) ((endX-x-10) / scale), CommonColors.WHITE, false);
                if (scale > 1) {
                    context.pose().scale(1/scale, 1/scale);
                }
                int wrappedSize = screen.getFont().wordWrapHeight(text, (int) ((endX-x-10) / scale));
                if (!s.toString().replaceAll("\\s+", "").isEmpty()) {
                    y += (int) (wrappedSize * scale);
                    if (scale > 1) {
                        y -= 10;
                    }
                }
                scale = 1f;
                s = new StringBuilder();
            } else if (((body.charAt(i) == '-' && body.charAt(i+1) != '-') || (body.charAt(i) == '*' && (i == body.length()-1 || body.charAt(i+1) != '*'))) && (s.toString().replace(" ", "").replace("\n", "").isEmpty())) {
                x = startX + 20;
            } if (body.startsWith("---", i) || body.startsWith("\n\n", i)) {
                x = startX;
                y += imageHeight;
                imageHeight = 0;
            }

            if (body.charAt(i) != '#' || !s.toString().replaceAll("\\s+", "").isEmpty()) {
                s.append(body.charAt(i));
            }
        }
        context.pose().scale(1/scale, 1/scale);
        maxY = y + imageHeight;
        if (count != -1) {
            count++;
        }
    }

    private void putLinkButtons(MutableComponent text, int x, int y, int width, double scale) {
        boolean isLink = false;
        int j = 0;
        int l = 0;
        int k = 0;
        int numLines = 0;
        int siblingIndex = 0;
        int m = 1;
        for (FormattedText visitableText : screen.getFont().getSplitter().splitLines(text, width, Style.EMPTY)) {
            String textString = visitableText.getString();
            String s = "";
            MutableComponent t = Component.literal("");
            int lastIndex = 0;
            for (int i = 0; i < textString.length(); i++) {
                if (linkIndexes.contains(k)) {
                    isLink = true;
                } else if (isLink && (linkLengths.get(l) == j || i == textString.length() - 1)) {
                    String link = linkUrls.get(l);
                    Button buttonWidget = Button.builder(Component.empty(), button -> {
                        ConfirmLinkScreen.confirmLinkNow(screen, link, false);
                    }).build();
                    MutableComponent prevText = Component.literal("");
                    MutableComponent currentText = Component.literal("");
                    if (text.getSiblings().get(siblingIndex).getStyle().isBold()) {
                        prevText.append(t);
                        prevText.append(Component.literal(textString.substring(lastIndex, i - s.length())).setStyle(Style.EMPTY.withBold(true)));
                    } else {
                        prevText.append(t);
                        prevText.append(Component.literal(textString.substring(lastIndex, i - s.length())));
                    }
                    if (linkLengths.get(l) != j) {
                        currentText.append(Component.literal(String.valueOf(textString.charAt(i))));
                    }
                    if (text.getSiblings().get(siblingIndex).getStyle().isBold()) {
                        currentText.append(Component.literal(s).setStyle(Style.EMPTY.withBold(true)));
                    } else {
                        currentText.append(Component.literal(s));
                    }

                    buttonWidget.setPosition((int) (x + screen.getFont().width(prevText) * scale), y + 9 * numLines);
                    originalY.add(y + 9 * numLines);
                    ((MarkdownScreenInterface) screen).addSelectableChild(buttonWidget);
                    buttonWidget.setSize((int) (screen.getFont().width(currentText) * scale), (int) (9 * scale));
                    linkButtons.add(buttonWidget);
                    if (linkLengths.get(l) == j) {
                        isLink = false;
                        j = 0;
                        l++;
                    }
                    s = "";
                }
                if (siblingIndex < text.getSiblings().size() && m >= text.getSiblings().get(siblingIndex).getString().length() && s.isEmpty()) {
                    m = 1;
                    if (text.getSiblings().get(siblingIndex).getStyle().isBold()) {
                        t.append(Component.literal(textString.substring(lastIndex, i)).setStyle(Style.EMPTY.withBold(true)));
                    } else {
                        t.append(Component.literal(textString.substring(lastIndex, i)));
                    }
                    lastIndex = i;
                    if (siblingIndex + 1 < text.getSiblings().size()) {
                        siblingIndex++;
                    }
                }
                if (isLink) {
                    s += textString.charAt(i);
                    j++;
                }

                k++;
                m++;
            }
            numLines++;
            k++;
            m++;
        }
    }

    private static String convertMarkdownToHtml(String markdown) {
        // Create the Markdown parser and renderer
        markdown = markdown.replaceAll("^\\s+", "");
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        // Parse the Markdown and render it to HTML

        Node document = parser.parse(markdown);
        return renderer.render(document);
    }

    private MutableComponent extractMarkdownFromHtml(String htmlContent) {
        htmlContent = htmlContent.replace("<li>", "-").replace("</p>", "\n");
        Document document = Jsoup.parse(htmlContent);
        Elements images = document.select("img");
        MutableComponent finalText;
        for (Element img : images) {
            String imgUrl = img.attr("src").replaceAll("\\s+", "");
            int width;
            String markdownImage;
            try {
                if (img.attr("width").contains("%")){
                    width = (int) (Double.parseDouble(img.attr("width").replace("%", "")) * 10);
                } else {
                    width = Integer.parseInt(img.attr("width").replace("px", ""));
                }
                markdownImage = String.format("![width=%d](%s)", width, imgUrl);
            } catch (NumberFormatException e) {
                markdownImage = String.format("![](%s)", imgUrl);
            }
            img.html(markdownImage);
        }
        Elements headers = document.select("h1, h2, h3, h4, h5, h6");
        for (Element header: headers) {
            int level = Integer.parseInt(header.tagName().substring(1));
            String markdownHeader = "#".repeat(level) + " " + header.text();
            header.html(markdownHeader);
        }


        Elements links = document.select("a");
        for (Element link : links) {
            String str = String.format("[%s](%s)", link.text().replaceAll("\\s+", ""), link.attr("href").replaceAll("\\s+", ""));
            link.html(str);
        }

        Elements formatting = document.select("i, em, b, strong, a");
        String s = document.wholeText();
        for (Element e : formatting) {
            String text = e.text();
            String wholeText = e.wholeText();
            String t = "";
            if (!e.tagName().equals("a") && s.indexOf(wholeText) - 1 > 0 && (Character.getType(s.charAt(s.indexOf(wholeText) - 1)) != Character.SPACE_SEPARATOR)) {
                t = " ";
            }
            if ((e.tagName().equals("b") || e.tagName().equals("strong")) && e.select("img").isEmpty() && !text.replaceAll("\\s+", "").isEmpty()) {
                t += String.format("__%s__", text);
            } else if ((e.tagName().equals("i") || e.tagName().equals("em")) && e.select("img").isEmpty() && !text.replaceAll("\\s+", "").isEmpty()) {
                t += String.format("_%s_", text);
            } else {
                t = text;
            }
            if (!e.tagName().equals("a") && s.indexOf(wholeText) + wholeText.length() < s.length() && (Character.getType(s.charAt(s.indexOf(wholeText) + wholeText.length())) != Character.SPACE_SEPARATOR) && (!Pattern.compile("\\p{P}").matcher(String.valueOf(s.charAt(s.indexOf(wholeText) + wholeText.length()))).matches())) {
                t += " ";
            }
            e.html(t);
        }
        String wholeText = document.wholeText();
        int i = 0;
        while (i < wholeText.length()) {
            if (wholeText.substring(i).startsWith("](\n") && i + 3 < wholeText.length()) {
                wholeText = wholeText.substring(0, i + 2) + wholeText.substring(i + 3);
            }
            if ((i == 0 || (wholeText.charAt(i - 1) != '(' && wholeText.charAt(i - 1) != '[' && wholeText.charAt(i - 1) != '"' && wholeText.charAt(i - 1) != '=')) && ((wholeText.startsWith("https://", i) || wholeText.startsWith("http://", i)))) {
                String url;
                try {
                    if (wholeText.indexOf(" ", i) == -1 || wholeText.indexOf("\n", i) == -1) {
                        url = wholeText.substring(i, Math.max(wholeText.indexOf(" ", i), wholeText.indexOf("\n", i)));
                    } else {
                        url = wholeText.substring(i, Math.min(wholeText.indexOf(" ", i), wholeText.indexOf("\n", i)));
                    }
                } catch (IndexOutOfBoundsException e) {
                    url = wholeText.substring(i);
                }
                wholeText = wholeText.substring(0, wholeText.indexOf(url, i)) + String.format("[%s](%s)", url, url) + wholeText.substring(wholeText.indexOf(url, i) + url.length());
            }
            i++;
        }
        finalText = Component.literal(wholeText);
        return finalText;
    }

    private MutableComponent extractTextFromHtml(String htmlContent) {
        htmlContent = htmlContent.replace("<li>", "-").replace("</p>", "\n");
        Document document = Jsoup.parse(htmlContent);

        MutableComponent finalText = Component.literal("");

        linkUrls.clear();
        linkLengths.clear();
        linkIndexes.clear();
        Elements formatting = document.select("i, em, b, strong, a");
        int lastIndex = 0;
        for (Element e : formatting) {
            try {
                String textBeforeLink = document.wholeText().substring(lastIndex, document.wholeText().indexOf(e.wholeText(), lastIndex));
                finalText.append(textBeforeLink);
                MutableComponent text = Component.literal(e.wholeText().replaceAll("\\s*->\\s*", " → ").replaceAll("\\s*<-\\s*", " ← ").replace("\\", ""));
                if (!e.select("a").isEmpty()) {
                    MutableComponent newText = Component.literal("");
                    int lastIndex2 = 0;

                    for (Element link : e.select("a")) {
                        newText.append(Component.literal(text.getString().substring(lastIndex2, text.getString().indexOf(link.text()))));
                        newText.append(Component.literal(text.getString().substring(text.getString().indexOf(link.text()), text.getString().indexOf(link.text()) + link.text().length())).setStyle(text.getStyle()).withColor(0x257DE6));
                        if (!link.text().isEmpty()) {
                            linkUrls.add(link.attr("href"));
                            linkIndexes.add(finalText.getString().length() + text.getString().indexOf(link.text()));
                            linkLengths.add(link.text().length());
                        }
                        lastIndex2 += newText.getString().length();
                    }
                    newText.append(text.getString().substring(lastIndex2));
                    text = newText;
                }
                if (!e.select("b, strong").isEmpty()) {
                    text = text.setStyle(text.getStyle().withBold(true));
                }
                if (!e.select("i, em").isEmpty()) {
                    text = text.setStyle(text.getStyle().withItalic(true));
                }

                finalText.append(text);
                lastIndex = document.wholeText().indexOf(e.wholeText(), lastIndex) + e.wholeText().length();

            } catch (Exception ignored) {

            }
        }
        document.outputSettings().prettyPrint(true);
        finalText.append(document.wholeText().substring(lastIndex).replaceAll("\\s*->\\s*", " → ").replaceAll("\\s*<-\\s*", " ← ").replace("\\", ""));
        return finalText;
    }


    private void createClickableImageButtons(int x, int y, int width, int height, String link) {
        Button button = Button.builder(Component.nullToEmpty(""), button1 -> {
            ConfirmLinkScreen.confirmLinkNow(screen, link, false);
        }).build();
        button.setSize(width, height);
        ((MarkdownScreenInterface) screen).addSelectableChild(button);
        button.setPosition(x, y);
        linkButtons.add(button);
        originalY.add(y);
    }

    public void setLinksActive(boolean active) {
        for (Button linkButton : linkButtons) {
            linkButton.active = active;

        }
    }

    public void refreshLinkPositions() {
        for (Button link : linkButtons) {
            ((MarkdownScreenInterface) (screen)).removeChild(link);
        }
        if (count >= 0) {
            count = 0;
            linkUrls.clear();
            linkLengths.clear();
            linkIndexes.clear();
            linkButtons.clear();
            originalY.clear();
        }

    }

    public int getMaxY() {
        return this.maxY;
    }

    public void setEndX(int x) {
        this.endX = x;
    }

}
