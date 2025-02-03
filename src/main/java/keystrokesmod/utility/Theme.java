package keystrokesmod.utility;

import keystrokesmod.module.impl.client.Settings;

import java.awt.*;

public enum Theme {

    Rainbow(null, null), // 0
    Cherry(new Color(255, 200, 200), new Color(243, 58, 106)), // 1
    Cotton_candy(new Color(99, 249, 255), new Color(255, 104, 204)), // 2
    Flare(new Color(231, 39, 24), new Color(245, 173, 49)), // 3
    Flower(new Color(215, 166, 231), new Color(211, 90, 232)), // 4
    Gold(new Color(255, 215, 0), new Color(240, 159, 0)), // 5
    Grayscale(new Color(240, 240, 240), new Color(110, 110, 110)), // 6
    Royal(new Color(125, 204, 241), new Color(30, 71, 170)), // 7
    Sky(new Color(160, 230, 225), new Color(15, 190, 220)), // 8
    Vine(new Color(17, 192, 45), new Color(201, 234, 198)), // 9

    // new color shi
    Astolfo(new Color(255, 74, 255), new Color(74, 255, 255), new Color(255, 255, 255)), // 10
    Ocean_Breeze(new Color(79, 179, 209), new Color(44, 141, 153)), // 12// 13
    Twilight(new Color(106, 63, 125), new Color(241, 167, 193)), // 14
    Minty_Fresh(new Color(62, 207, 119), new Color(86, 214, 214)), // 15
    Galaxy(new Color(61, 13, 117), new Color(0, 161, 214), new Color(0, 0, 0)), // 17
    Aurora(new Color(161, 161, 255), new Color(0, 153, 255), new Color(75, 0, 130)), // 19
    Tropical_Punch(new Color(255, 58, 74), new Color(253, 203, 88), new Color(87, 193, 229)), // 20
    Purple(new Color(157, 0, 255), new Color(191, 0, 255)), // 21
    Ocean(new Color(55, 59, 68), new Color(66, 134, 244), new Color(255, 255, 255)); // 22

    private final Color firstGradient;
    private final Color secondGradient;
    private final Color thirdGradient;

    public static Color[] descriptor = new Color[]{new Color(95, 235, 255), new Color(68, 102, 250)};
    public static Color[] hiddenBind = new Color[]{new Color(245, 33, 33), new Color(229, 21, 98)};

    Theme(Color firstGradient, Color secondGradient) {
        this.firstGradient = firstGradient;
        this.secondGradient = secondGradient;
        this.thirdGradient = null;
    }

    Theme(Color firstGradient, Color secondGradient, Color thirdGradient) {
        this.firstGradient = firstGradient;
        this.secondGradient = secondGradient;
        this.thirdGradient = thirdGradient;
    }

    public static int getGradient(int index, double delay) {
        if (index > 0) {
            if (values()[index].thirdGradient != null) {
                return convert(values()[index].firstGradient, values()[index].secondGradient, values()[index].thirdGradient,
                        (Math.sin(System.currentTimeMillis() / 1.0E8 * Settings.timeMultiplier.getInput() * 400000.0 + delay * Settings.offset.getInput()) + 1.0) * 0.5).getRGB();
            }
            return convert(values()[index].firstGradient, values()[index].secondGradient,
                    (Math.sin(System.currentTimeMillis() / 1.0E8 * Settings.timeMultiplier.getInput() * 400000.0 + delay * Settings.offset.getInput()) + 1.0) * 0.5).getRGB();
        } else if (index == 0) {
            return Utils.getChroma(2, (long) delay);
        }
        return -1;
    }

    public static Color convert(Color color1, Color color2, Color color3, double n) {
        double n2 = 1.0 - n;
        Color color12 = convert(color1, color2, n);
        return convert(color12, color3, n2);
    }

    public static Color convert(Color color1, Color color2, double n) {
        double n2 = 1.0 - n;
        return new Color(
                (int) (color1.getRed() * n + color2.getRed() * n2),
                (int) (color1.getGreen() * n + color2.getGreen() * n2),
                (int) (color1.getBlue() * n + color2.getBlue() * n2)
        );
    }

    public static int[] getGradients(int index) {
        Theme[] values = values();

        if (values != null && index >= 0 && index < values.length && values[index] != null) {
            Color firstGradient = values[index].firstGradient;
            Color secondGradient = values[index].secondGradient;
            Color thirdGradient = values[index].thirdGradient;

            if (firstGradient != null && secondGradient != null) {
                if (thirdGradient != null) {
                    return new int[]{
                            firstGradient.getRGB(),
                            secondGradient.getRGB(),
                            thirdGradient.getRGB()
                    };
                }
                return new int[]{firstGradient.getRGB(), secondGradient.getRGB()};
            } else {
                return new int[]{Utils.getChroma(2, (long) 0), Utils.getChroma(2, (long) 0)};
            }
        }

        return new int[]{0, 0};
    }

    public static String[] themes = new String[]{
            "Rainbow", "Cherry", "Cotton candy", "Flare", "Flower", "Gold", "Grayscale",
            "Royal", "Sky", "Vine", "Astolfo",
            "Ocean Breeze", "Twilight", "Minty Fresh",
            "Galaxy", "Aurora", "Tropical Punch", "Purple", "Ocean"
    };
}
