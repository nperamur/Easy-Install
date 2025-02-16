package neelesh.easy_install.gui.tab;

import com.google.common.net.UrlEscapers;
import neelesh.easy_install.ImageLoader;
import neelesh.easy_install.ProjectImage;
import neelesh.easy_install.gui.screen.ProjectScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.tab.GridScreenTab;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TabButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.http.client.utils.URIUtils;
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

import static neelesh.easy_install.gui.screen.ProjectScreen.VERTICAL_SEPARATOR_TEXTURE;

public class DescriptionTab extends GridScreenTab implements Drawable {
    private ProjectScreen projectScreen;
    private final ArrayList<ProjectImage> projectImages;
    private int count;
    private ArrayList<ButtonWidget> linkButtons = new ArrayList<ButtonWidget>();
    private ArrayList<String> linkUrls = new ArrayList<String>();
    private final ArrayList<Integer> linkIndexes = new ArrayList<Integer>();
    private final ArrayList<Integer> linkLengths = new ArrayList<Integer>();
    private final ArrayList<Integer> originalY = new ArrayList<Integer>();
    private Thread thread;

    public DescriptionTab(Text title, ProjectScreen projectScreen) {
        super(title);
        projectScreen.getProjectInfo().setBody(extractMarkdownFromHtml(projectScreen.getProjectInfo().getBody()).getString());
        count = -1;
        this.projectScreen = projectScreen;
        this.projectImages = new ArrayList<ProjectImage>();
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
            for (int i = 0; i < projectScreen.getProjectInfo().getBody().length(); i++) {
                if (i < projectScreen.getProjectInfo().getBody().length() - 2 && (projectScreen.getProjectInfo().getBody().charAt(i) == '-') && projectScreen.getProjectInfo().getBody().charAt(i + 1) == '\n' && (i == 0 || projectScreen.getProjectInfo().getBody().charAt(i - 1) != '-')) {
                    projectScreen.getProjectInfo().setBody(projectScreen.getProjectInfo().getBody().substring(0, i + 1) + " " + projectScreen.getProjectInfo().getBody().substring(i + 3));
                }
                if (projectScreen.getProjectInfo().getBody().startsWith("[![", i)) {
                    linkInImage = true;
                }
                if (projectScreen.getProjectInfo().getBody().startsWith("![", i)) {
                    isImage = true;
                } else if (projectScreen.getProjectInfo().getBody().startsWith("width=", i) && isImage) {
                    puttingImageWidth = true;
                    i += 5;
                } else if (puttingImageWidth) {
                    puttingImageWidth = Character.isDigit(projectScreen.getProjectInfo().getBody().charAt(i));
                    if (puttingImageWidth) {
                        imageWidth += Integer.parseInt(projectScreen.getProjectInfo().getBody().substring(i, i + 1));
                    }
                }
                if (projectScreen.getProjectInfo().getBody().charAt(i) == ')' && isImage) {
                    puttingImageUrl = false;
                    isImage = false;
                    URL url;
                    if (linkInImage && i < projectScreen.getProjectInfo().getBody().length() - 2 && projectScreen.getProjectInfo().getBody().charAt(i + 1) == '\n') {
                        projectScreen.getProjectInfo().setBody(projectScreen.getProjectInfo().getBody().substring(0, i + 1) + " " + projectScreen.getProjectInfo().getBody().substring(i + 2));
                    }
                    try {
                        url = new URL(str); //UrlEscapers.urlFragmentEscaper().escape(str)
                        Identifier id = Identifier.of("project_image:" + i);
                        NativeImage image = ImageLoader.loadImage(url, id, MinecraftClient.getInstance());
                        if (image != null) {
                            ProjectImage projectImage = new ProjectImage(image, id, i);
                            if (!imageWidth.isEmpty()) {
                                projectImage.setWidth(Integer.parseInt(imageWidth));
                            }
                            if (linkInImage) {
                                projectImage.setLink(projectScreen.getProjectInfo().getBody().substring(projectScreen.getProjectInfo().getBody().indexOf('(', i + 1) + 1, projectScreen.getProjectInfo().getBody().indexOf(')', i + 1)));
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
                    str += projectScreen.getProjectInfo().getBody().charAt(i);
                }
                if (projectScreen.getProjectInfo().getBody().charAt(i) == '(' && isImage) {
                    puttingImageUrl = true;
                }
            }
            MinecraftClient.getInstance().execute(() -> {
                count = 0;
            });
        });
        thread.start();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int j = 0;
        for (ButtonWidget linkButton : linkButtons) {
            linkButton.setY(projectScreen.getScrollAmount() + originalY.get(j));
            j++;
        }
        context.getMatrices().translate(0, 0, 100);
        String body = projectScreen.getProjectInfo().getBody();
        StringBuilder s = new StringBuilder();
        float scale = 1.0f;
        int y = 30;
        int x = 140;
        int imageIndex = 0;
        int imageHeight = 0;
        for (int i = 0; i < body.length(); i++) {
            if (projectImages.size() > imageIndex && projectImages.get(imageIndex).getPosition() == i) {
                ProjectImage image = projectImages.get(imageIndex);
                int imageWidth;
                if (!(image.getWidth() == -1) && (image.getWidth() * (projectScreen.width - 150)/1000) < projectScreen.width - x - 10) {
                    imageWidth = (int) (image.getWidth() * (projectScreen.width - 150)/(1000 * scale));
                    context.drawTexture(RenderLayer::getGuiTextured, image.getId(), (int)(x/scale), (int)((y+projectScreen.getScrollAmount())/scale), 0, 0, imageWidth, image.getImage().getHeight() * imageWidth/image.getImage().getWidth(), imageWidth, image.getImage().getHeight() * imageWidth/image.getImage().getWidth());
                    imageHeight = Math.max(imageHeight, image.getImage().getHeight() * imageWidth/image.getImage().getWidth() + 10);
                    if (image.isClickable() && count == 0) {
                        createClickableImageButtons(x, y, imageWidth, image.getImage().getHeight() * imageWidth/image.getImage().getWidth(), image.getLink());
                    }
                    x+=imageWidth + 5;
                } else if (image.getImage().getWidth()/2 < (projectScreen.width - 150)) {
                    if (image.getImage().getWidth()/2 >= (projectScreen.width - x - 10)) {
                        x = 140;
                        y += imageHeight;
                        imageHeight = 0;
                    }
                    context.drawTexture(RenderLayer::getGuiTextured, image.getId(), (int)(x/scale), (int)((y+projectScreen.getScrollAmount())/scale), 0, 0, (int)(image.getImage().getWidth()/(2 * scale)), (int)(image.getImage().getHeight()/(2 * scale)), (int)(image.getImage().getWidth()/(2 * scale)), (int) (image.getImage().getHeight()/(2 * scale)));
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
                    imageWidth = (projectScreen.width - x - 10);
                    if (image.isClickable() && count == 0) {
                        createClickableImageButtons(x, y, imageWidth, image.getImage().getHeight() * imageWidth/image.getImage().getWidth() + 10, image.getLink());
                    }
                    context.drawTexture(RenderLayer::getGuiTextured, image.getId(), (int)(x/scale), (int)((y+projectScreen.getScrollAmount())/scale), 0, 0, (projectScreen.width - x - 10), image.getImage().getHeight() * (int)((projectScreen.width-x-10)/scale)/image.getImage().getWidth(), (int)((projectScreen.width-x-10)/scale), image.getImage().getHeight() * (int)((projectScreen.width-x-10)/scale)/image.getImage().getWidth());
                    y+=image.getImage().getHeight() * imageWidth/image.getImage().getWidth() + 10;
                }
                imageIndex++;
            }
            if (body.charAt(i) == '#' && s.toString().trim().isEmpty() || i == body.length() - 1) {
                if (i == body.length() - 1) {
                    s.append(body.charAt(i));
                }
                MutableText text = extractTextFromHtml(convertMarkdownToHtml(s.toString()));
                if (count == 0) {
                    putLinkButtons(text, x, y, (int) ((projectScreen.width-x-10) / scale), scale);
                }
                context.drawTextWrapped(projectScreen.getTextRenderer(), text, (int) (x/scale), (int) (y/scale + projectScreen.getScrollAmount() / scale), (int) ((projectScreen.width-x-10) / scale), 0xFFFFFF);
                int wrappedSize = projectScreen.getTextRenderer().getWrappedLinesHeight(text, (int) ((projectScreen.width-x-10) / scale));
                if (scale == 1) {
                    scale = 1.4f;
                    context.getMatrices().scale(scale, scale, 1.0f);
                }
                s = new StringBuilder();
                x = 140;
                y += imageHeight;
                imageHeight = 0;
                y += (int) (wrappedSize * scale);
            } else if ((body.charAt(i) == '\n')) {
                MutableText text = extractTextFromHtml(convertMarkdownToHtml(s.toString()));
                if (!text.getString().trim().isEmpty()) {
                    y += imageHeight;
                    if (imageHeight > 0) {
                        x = 140;
                    }
                    imageHeight = 0;
                }
                s = new StringBuilder(text.getString());
                if (count == 0) {
                    putLinkButtons(text, x, y, (int) ((projectScreen.width-x-10) / scale), scale);
                }
                context.drawTextWrapped(projectScreen.getTextRenderer(), text, (int) (x/(scale)), (int) (y/scale + projectScreen.getScrollAmount() / scale), (int) ((projectScreen.width-x-10) / scale), 0xFFFFFF);
                if (scale > 1) {
                    context.getMatrices().scale(1/scale, 1/scale, 1.0f);
                }
                int wrappedSize = projectScreen.getTextRenderer().getWrappedLinesHeight(text, (int) ((projectScreen.width-x-10) / scale));
                if (!s.toString().trim().isEmpty()) {
                    y += (int) (wrappedSize * scale);
                    if (scale > 1) {
                        y -= 10;
                    }
                }
                scale = 1f;
                s = new StringBuilder();
            } else if (((body.charAt(i) == '-' && body.charAt(i+1) != '-') || (body.charAt(i) == '*' && (i == body.length()-1 || body.charAt(i+1) != '*'))) && (s.toString().replace(" ", "").replace("\n", "").isEmpty())) {
                x = 160;
            } if (body.startsWith("---", i) || body.startsWith("\n\n", i)) {
                x = 140;
                y += imageHeight;
                imageHeight = 0;
            }

            if (body.charAt(i) != '#' || !s.toString().trim().isEmpty()) {
                s.append(body.charAt(i));
            }
        }
        context.getMatrices().scale(1/scale, 1/scale, 1.0f);
        context.getMatrices().translate(0, 0, -100);
        projectScreen.renderDarkening(context, 131, projectScreen.getScrollAmount() + ((TabButtonWidget) projectScreen.getTabNavigationWidget().children().get(0)).getHeight()-10, projectScreen.width, y + imageHeight);
        context.drawTexture(
                RenderLayer::getGuiTextured, VERTICAL_SEPARATOR_TEXTURE, 131, projectScreen.getScrollAmount() + ((TabButtonWidget) projectScreen.getTabNavigationWidget().children().getFirst()).getHeight() - 12, 0.0F, 0.0F, 2, y + imageHeight, 2, 32
        );
       projectScreen.setMaxY(y + imageHeight);
        if (count != -1) {
            count++;
        }
    }
    public void putLinkButtons(MutableText text, int x, int y, int width, double scale) {
        boolean isLink = false;
        int j = 0;
        int l = 0;
        int k = 0;
        int numLines = 0;
        int siblingIndex = 0;
        int m = 1;
        for (StringVisitable visitableText : projectScreen.getTextRenderer().getTextHandler().wrapLines(text, width, Style.EMPTY)) {
            String textString = visitableText.getString();
            String s = "";
            MutableText t = Text.literal("");
            int lastIndex = 0;
            for (int i = 0; i < textString.length(); i++) {
                if (linkIndexes.contains(k)) {
                    isLink = true;
                } else if (isLink && (linkLengths.get(l) == j || i == textString.length() - 1)) {
                    String link = linkUrls.get(l);
                    ButtonWidget buttonWidget = ButtonWidget.builder(Text.empty(), button -> {
                        ConfirmLinkScreen.open(projectScreen, link, false);
                    }).build();
                    MutableText prevText = Text.literal("");
                    MutableText currentText = Text.literal("");
                    if (text.getSiblings().get(siblingIndex).getStyle().isBold()) {
                        prevText.append(t);
                        prevText.append(Text.literal(textString.substring(lastIndex, i - s.length())).setStyle(Style.EMPTY.withBold(true)));
                    } else {
                        prevText.append(t);
                        prevText.append(Text.literal(textString.substring(lastIndex, i - s.length())));
                    }
                    if (linkLengths.get(l) != j) {
                        currentText.append(Text.literal(String.valueOf(textString.charAt(i))));
                    }
                    if (text.getSiblings().get(siblingIndex).getStyle().isBold()) {
                        currentText.append(Text.literal(s).setStyle(Style.EMPTY.withBold(true)));
                    } else {
                        currentText.append(Text.literal(s));
                    }

                    buttonWidget.setPosition((int) (x + projectScreen.getTextRenderer().getWidth(prevText) * scale), y + 9 * numLines);
                    originalY.add(y + 9 * numLines);
                    projectScreen.addSelectableChild(buttonWidget);
                    buttonWidget.setDimensions((int) (projectScreen.getTextRenderer().getWidth(currentText) * scale), (int) (9 * scale));
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
                        t.append(Text.literal(textString.substring(lastIndex, i)).setStyle(Style.EMPTY.withBold(true)));
                    } else {
                        t.append(Text.literal(textString.substring(lastIndex, i)));
                    }
                    lastIndex = i;
                    siblingIndex++;
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

    private MutableText extractMarkdownFromHtml(String htmlContent) {
        htmlContent = htmlContent.replace("<li>", "-").replace("</p>", "\n");
        Document document = Jsoup.parse(htmlContent);
        Elements images = document.select("img");
        MutableText finalText;
        for (Element img : images) {
            String altText = img.attr("alt");
            String imgUrl = img.attr("src");
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
                markdownImage = String.format("![%s](%s)", altText, imgUrl);
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
            String str = String.format("[%s](%s)", link.text(), link.attr("href"));
            link.html(str);
        }

        Elements formatting = document.select("i, em, b, strong, a");
        for (Element e : formatting) {
            String text = e.text();
            String t = "";
            if (!e.tagName().equals("a") && document.wholeText().indexOf(text) - 1 > 0 && !(Character.getType(document.wholeText().charAt(document.wholeText().indexOf(text) - 1)) == Character.SPACE_SEPARATOR)) {
                t = " ";
            }
            if ((e.tagName().equals("b") || e.tagName().equals("strong")) && e.select("img").isEmpty() && !text.trim().isEmpty()) {
                t += String.format("__%s__", text);
            } else if ((e.tagName().equals("i") || e.tagName().equals("em")) && e.select("img").isEmpty() && !text.trim().isEmpty()) {
                t += String.format("_%s_", text);
            } else {
                t = text;
            }
            if (!e.tagName().equals("a") && document.wholeText().indexOf(text) + 1 < document.wholeText().length() - 1 && (Character.getType(document.wholeText().charAt(document.wholeText().indexOf(text) + 1)) != Character.SPACE_SEPARATOR)) {
                t += " ";
            }
            e.html(t);
        }
        String text = document.wholeText();
        String wholeText = document.wholeText();
        for (int i = 0; i < wholeText.length(); i++) {
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
                text = text.substring(0, text.indexOf(url, i)) + String.format("[%s](%s)", url, url) + text.substring(text.indexOf(url, i) + url.length());
            }
        }
        finalText = Text.literal(text);
        return finalText;
    }

    private MutableText extractTextFromHtml(String htmlContent) {
        htmlContent = htmlContent.replace("<li>", "-").replace("</p>", "\n");
        Document document = Jsoup.parse(htmlContent);

        MutableText finalText = Text.literal("");

        linkUrls.clear();
        linkLengths.clear();
        linkIndexes.clear();
        Elements formatting = document.select("i, em, b, strong, a");
        int lastIndex = 0;
        for (Element e : formatting) {
            try {
                String textBeforeLink = document.wholeText().substring(lastIndex, document.wholeText().indexOf(e.wholeText(), lastIndex));
                finalText.append(textBeforeLink);
                MutableText text = Text.literal(e.wholeText().replaceAll("\\s*->\\s*", " → ").replaceAll("\\s*<-\\s*", " ← ").replace("\\", ""));
                if (!e.select("a").isEmpty()) {
                    MutableText newText = Text.literal("");
                    int lastIndex2 = 0;

                    for (Element link : e.select("a")) {
                        newText.append(Text.literal(text.getString().substring(lastIndex2, text.getString().indexOf(link.text()))));
                        newText.append(Text.literal(text.getString().substring(text.getString().indexOf(link.text()), text.getString().indexOf(link.text()) + link.text().length())).setStyle(text.getStyle().withColor(0x257DE6)));
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


    public void createClickableImageButtons(int x, int y, int width, int height, String link) {
        ButtonWidget button = ButtonWidget.builder(Text.of(""), button1 -> {
            ConfirmLinkScreen.open(projectScreen, link, false);
        }).build();
        button.setDimensions(width, height);
        projectScreen.addSelectableChild(button);
        button.setPosition(x, y);
        linkButtons.add(button);
        originalY.add(y);
    }

    public void setLinksActive(boolean active) {
        for (ButtonWidget linkButton : linkButtons) {
            linkButton.active = active;

        }
    }

    public void refreshLinkPositions() {
        for (ButtonWidget link : linkButtons) {
            projectScreen.removeChild(link);
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

    public ArrayList<ButtonWidget> getLinkButtons() {
        return linkButtons;
    }
}
