package mutsa.hackathon.service;

import mutsa.hackathon.domain.WeeklyRewardImageSource;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Component
public class FallbackWeeklyPosterGenerator implements WeeklyImageGenerator {

    private static final int SIZE = 1024;

    @Override
    public GeneratedWeeklyImage generate(
            WeeklyRewardGenerationContext context,
            WeeklyVisualPlan visualPlan
    ) {
        List<Color> palette = context.days().stream()
                .map(WeeklyRewardGenerationContext.DayRecord::colorHex)
                .map(Color::decode)
                .toList();

        BufferedImage image = new BufferedImage(
                SIZE,
                SIZE,
                BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );
            graphics.setColor(new Color(247, 244, 238));
            graphics.fillRect(0, 0, SIZE, SIZE);

            int variant = Math.floorMod(context.weekStartDate().hashCode(), 3);
            if (variant == 0) {
                drawGraphicBlocks(graphics, palette);
            } else if (variant == 1) {
                drawLandscapeRhythm(graphics, palette);
            } else {
                drawPainterlyOrbit(graphics, palette);
            }

            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.setColor(new Color(30, 32, 38, 180));
            graphics.setStroke(new BasicStroke(10f));
            graphics.draw(new RoundRectangle2D.Double(
                    32,
                    32,
                    SIZE - 64,
                    SIZE - 64,
                    44,
                    44
            ));
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", output)) {
                throw new IllegalStateException("PNG 이미지 인코더를 찾을 수 없습니다.");
            }
            return new GeneratedWeeklyImage(
                    output.toByteArray(),
                    "image/png",
                    "png",
                    WeeklyRewardImageSource.FALLBACK
            );
        } catch (IOException exception) {
            throw new IllegalStateException("대체 주간 이미지를 만들 수 없습니다.", exception);
        }
    }

    private void drawGraphicBlocks(Graphics2D g, List<Color> colors) {
        int count = colors.size();
        int blockWidth = 800 / count;
        for (int index = 0; index < count; index++) {
            Color color = colors.get(index);
            g.setColor(color);
            int x = 112 + index * blockWidth;
            int height = 300 + Math.floorMod(color.getRGB(), 380);
            g.fill(new RoundRectangle2D.Double(
                    x,
                    780 - height,
                    Math.max(72, blockWidth - 24),
                    height,
                    32,
                    32
            ));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.34f));
            g.fill(new Ellipse2D.Double(x - 30, 100 + index * 54, 230, 230));
            g.setComposite(AlphaComposite.SrcOver);
        }
    }

    private void drawLandscapeRhythm(Graphics2D g, List<Color> colors) {
        g.setColor(new Color(235, 232, 225));
        g.fillRect(0, 0, SIZE, 470);
        int bandHeight = 560 / colors.size();
        for (int index = 0; index < colors.size(); index++) {
            Color color = colors.get(index);
            Path2D path = new Path2D.Double();
            int y = 430 + index * bandHeight;
            path.moveTo(0, y + 90);
            path.curveTo(230, y - 80, 430, y + 150, 650, y + 10);
            path.curveTo(800, y - 70, 940, y + 80, SIZE, y + 15);
            path.lineTo(SIZE, SIZE);
            path.lineTo(0, SIZE);
            path.closePath();
            g.setColor(color);
            g.fill(path);
        }
        g.setColor(colors.get(0));
        g.fill(new Ellipse2D.Double(120, 110, 210, 210));
    }

    private void drawPainterlyOrbit(Graphics2D g, List<Color> colors) {
        g.setStroke(new BasicStroke(55f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int center = SIZE / 2;
        for (int index = 0; index < colors.size(); index++) {
            Color color = colors.get(index);
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 210));
            int inset = 95 + index * 45;
            g.draw(new Arc2D.Double(
                    inset,
                    inset,
                    SIZE - inset * 2,
                    SIZE - inset * 2,
                    28 + index * 31,
                    185 + index * 9,
                    Arc2D.OPEN
            ));
        }
        g.setColor(colors.get(colors.size() - 1));
        g.fill(new RoundRectangle2D.Double(
                center - 130,
                center - 170,
                260,
                340,
                96,
                96
        ));
    }
}
