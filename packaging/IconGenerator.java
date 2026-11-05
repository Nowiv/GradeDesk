import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Erzeugt das Platzhalter-Programmsymbol in Rot und Weiss für alle drei Plattformen.
 *
 * <p>Aufruf aus dem Projektverzeichnis:</p>
 * <pre>java packaging/IconGenerator.java</pre>
 *
 * <p>Das Motiv greift das Säulendiagramm auf: drei weisse Säulen auf rotem Grund,
 * dazu die Genügend-Linie. Es bleibt bis hinunter auf 16 Pixel lesbar.</p>
 *
 * <p>Diese Datei gehört zum Build-Werkzeug, nicht zur Anwendung. Sie liegt
 * ausserhalb von {@code src/} und wird nicht mitkompiliert.</p>
 */
public final class IconGenerator {

    private static final Color RED = new Color(0xD5, 0x2B, 0x1E);
    private static final Color RED_DEEP = new Color(0x8F, 0x17, 0x10);
    private static final Color WHITE = Color.WHITE;

    public static void main(String[] args) throws IOException {
        Path root = Path.of(".");
        Path resources = root.resolve("src/main/resources/ch/notenrechner/icon");
        Path packaging = root.resolve("packaging");

        Files.createDirectories(resources);
        Files.createDirectories(packaging.resolve("windows"));
        Files.createDirectories(packaging.resolve("macos"));
        Files.createDirectories(packaging.resolve("linux"));

        BufferedImage icon256 = render(256);
        ImageIO.write(icon256, "png", resources.resolve("notenrechner-256.png").toFile());
        ImageIO.write(icon256, "png", packaging.resolve("linux/Notenrechner.png").toFile());

        writeIco(packaging.resolve("windows/Notenrechner.ico"), List.of(16, 24, 32, 48, 64, 128, 256));
        writeIcns(packaging.resolve("macos/Notenrechner.icns"));

        System.out.println("Programmsymbole erzeugt.");
    }

    /** Zeichnet das Symbol in der gewünschten Kantenlänge. */
    private static BufferedImage render(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        double s = size;
        double radius = s * 0.22;

        g.setColor(RED);
        g.fill(new RoundRectangle2D.Double(0, 0, s, s, radius, radius));

        // Dezenter Rand, damit das Symbol auf hellem Grund nicht ausfranst.
        g.setColor(new Color(RED_DEEP.getRed(), RED_DEEP.getGreen(), RED_DEEP.getBlue(), 120));
        g.setStroke(new BasicStroke((float) Math.max(1, s * 0.008)));
        double inset = s * 0.012;
        g.draw(new RoundRectangle2D.Double(inset, inset, s - 2 * inset, s - 2 * inset,
                radius, radius));

        // Drei Säulen in steigender Höhe.
        double barWidth = s * 0.136;
        double gap = s * 0.072;
        double totalWidth = 3 * barWidth + 2 * gap;
        double left = (s - totalWidth) / 2.0;
        double baseline = s * 0.775;
        double[] heights = {s * 0.22, s * 0.355, s * 0.49};
        double barRadius = barWidth * 0.42;

        g.setColor(WHITE);
        for (int i = 0; i < 3; i++) {
            double x = left + i * (barWidth + gap);
            double height = heights[i];
            g.fill(new RoundRectangle2D.Double(x, baseline - height, barWidth, height,
                    barRadius, barRadius));
        }

        // Die Genügend-Linie, wie im Diagramm.
        double lineHeight = Math.max(1.5, s * 0.026);
        g.fill(new RoundRectangle2D.Double(left - gap, baseline, totalWidth + 2 * gap, lineHeight,
                lineHeight, lineHeight));

        g.dispose();
        return image;
    }

    private static byte[] png(int size) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(render(size), "png", out);
        return out.toByteArray();
    }

    /**
     * Schreibt eine ICO-Datei, die die einzelnen Grössen als eingebettete PNGs enthält.
     * Dieses Format versteht Windows seit Vista.
     */
    private static void writeIco(Path target, List<Integer> sizes) throws IOException {
        List<byte[]> images = new ArrayList<>();
        for (int size : sizes) {
            images.add(png(size));
        }

        int headerSize = 6 + 16 * images.size();
        try (OutputStream out = Files.newOutputStream(target)) {
            ByteBuffer header = ByteBuffer.allocate(headerSize).order(ByteOrder.LITTLE_ENDIAN);
            header.putShort((short) 0);                      // reserviert
            header.putShort((short) 1);                      // Typ: Symbol
            header.putShort((short) images.size());

            int offset = headerSize;
            for (int i = 0; i < images.size(); i++) {
                int size = sizes.get(i);
                header.put((byte) (size >= 256 ? 0 : size)); // 0 bedeutet 256
                header.put((byte) (size >= 256 ? 0 : size));
                header.put((byte) 0);                        // Farbpalette: keine
                header.put((byte) 0);                        // reserviert
                header.putShort((short) 1);                  // Ebenen
                header.putShort((short) 32);                 // Bit je Pixel
                header.putInt(images.get(i).length);
                header.putInt(offset);
                offset += images.get(i).length;
            }
            out.write(header.array());
            for (byte[] image : images) {
                out.write(image);
            }
        }
    }

    /** Schreibt eine ICNS-Datei mit PNG-Einträgen für macOS. */
    private static void writeIcns(Path target) throws IOException {
        record Entry(String type, byte[] data) {
        }
        List<Entry> entries = List.of(
                new Entry("ic07", png(128)),
                new Entry("ic08", png(256)),
                new Entry("ic09", png(512)),
                new Entry("ic11", png(32)),
                new Entry("ic12", png(64)),
                new Entry("ic13", png(256)));

        int total = 8;
        for (Entry entry : entries) {
            total += 8 + entry.data().length;
        }

        try (OutputStream out = Files.newOutputStream(target)) {
            ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
            header.put("icns".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            header.putInt(total);
            out.write(header.array());

            for (Entry entry : entries) {
                ByteBuffer chunk = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
                chunk.put(entry.type().getBytes(java.nio.charset.StandardCharsets.US_ASCII));
                chunk.putInt(8 + entry.data().length);
                out.write(chunk.array());
                out.write(entry.data());
            }
        }
    }

    private IconGenerator() {
    }
}
