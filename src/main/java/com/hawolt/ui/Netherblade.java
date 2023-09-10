package com.hawolt.ui;

import com.hawolt.logger.Logger;
import com.hawolt.util.RunLevel;
import io.javalin.http.Handler;
import me.friwi.jcefmaven.CefInitializationException;
import me.friwi.jcefmaven.UnsupportedPlatformException;
import org.cef.CefApp;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created: 31/07/2022 00:11
 * Author: Twitter @hawolt
 **/

public class Netherblade {

    private static boolean toggle;
    private static Rectangle previous;
    public static final Handler MINIMIZE = context -> {
        Netherblade.frame.setState(JFrame.ICONIFIED);
    };
    public static final Handler MAXIMIZE = context -> {
        Netherblade.toggle = !Netherblade.toggle;
        if (Netherblade.toggle) {
            Netherblade.previous = Netherblade.frame.getBounds();
            DisplayMode mode = Netherblade.frame.getGraphicsConfiguration().getDevice().getDisplayMode();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(Netherblade.frame.getGraphicsConfiguration());
            Netherblade.frame.setMaximizedBounds(new Rectangle(
                    mode.getWidth() - insets.right - insets.left,
                    mode.getHeight() - insets.top - insets.bottom
            ));
            Netherblade.frame.setExtendedState(Netherblade.frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        } else {
            Netherblade.frame.setBounds(Netherblade.previous);
        }
    };
    public static JFrame frame;

    public static void create() throws IOException {
        JFrame frame = new JFrame();
        String icon = "netherblade.png";
        frame.setIconImage(ImageIO.read(RunLevel.get(icon)));
        frame.setTitle("Netherblade");
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (CefApp.getState() != CefApp.CefAppState.NONE) CefApp.getInstance().dispose();
                frame.dispose();
                System.exit(0);
            }
        });
        Container container = frame.getContentPane();
        container.setPreferredSize(new Dimension(350, 100));
        container.setLayout(new BorderLayout());
        VisualProgressHandler handler = new VisualProgressHandler();
        container.add(handler, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        Path path = Paths.get(System.getProperty("java.io.tmpdir")).resolve("jcef-bundle");
        try {
            Chromium chromium = new Chromium("http://127.0.0.1:35199/home.html", path, handler);
            frame.dispose();
            frame.setUndecorated(true);
            container.removeAll();
            container.setPreferredSize(new Dimension(1000, 620));
            container.add(chromium.getBrowserUI(), BorderLayout.CENTER);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        } catch (UnsupportedPlatformException | CefInitializationException | IOException | InterruptedException e) {
            Logger.error(e);
        }
        Netherblade.frame = frame;
    }

    private static final int[] CURSOR_MAPPING = new int[]{
            Cursor.NW_RESIZE_CURSOR, Cursor.NW_RESIZE_CURSOR, Cursor.N_RESIZE_CURSOR,
            Cursor.NE_RESIZE_CURSOR, Cursor.NE_RESIZE_CURSOR,
            Cursor.NW_RESIZE_CURSOR, 0, 0, 0, Cursor.NE_RESIZE_CURSOR,
            Cursor.W_RESIZE_CURSOR, 0, 0, 0, Cursor.E_RESIZE_CURSOR,
            Cursor.SW_RESIZE_CURSOR, 0, 0, 0, Cursor.SE_RESIZE_CURSOR,
            Cursor.SW_RESIZE_CURSOR, Cursor.SW_RESIZE_CURSOR, Cursor.S_RESIZE_CURSOR,
            Cursor.SE_RESIZE_CURSOR, Cursor.SE_RESIZE_CURSOR
    };

    private static final int BORDER_DRAG_THICKNESS = 5;
    private static final int CORNER_DRAG_WIDTH = 16;
    private static boolean isHolding, move, resize;
    private static Rectangle bounds;
    private static Point drag = new Point(0, 0);

    public static void redirect(String message) {
        JSONObject object = new JSONObject(message);
        int opCode = object.getInt("type");
        if (opCode >= 1 || opCode <= 3) {
            boolean navbar = object.getBoolean("navbar");
            int mouseX = object.getInt("x");
            int mouseY = object.getInt("y");
            int cursorIndex = getCursor(getCorner(mouseX, mouseY));
            int current = frame.getCursor().getType();
            if (current != 0 || cursorIndex == 5) frame.setCursor(Cursor.getPredefinedCursor(cursorIndex == 5 ? 5 : 0));
            if (opCode == 2) {
                Netherblade.isHolding = true;
                Netherblade.move = navbar;
                Netherblade.bounds = frame.getBounds();
                Netherblade.resize = cursorIndex == 5;
                Netherblade.drag.setLocation(mouseX, mouseY);
            } else if (opCode == 3) {
                Netherblade.isHolding = false;
                Netherblade.resize = false;
                Netherblade.move = false;
            }
            if (!isHolding) return;
            Point screen = frame.getLocationOnScreen();
            int moveX = object.getInt("moveX");
            int moveY = object.getInt("moveY");
            if (Netherblade.move) {
                if (opCode == 1) frame.setLocation(screen.x + moveX, screen.y + moveY);
            } else if (Netherblade.resize) {
                if (Netherblade.drag.y < 30) {
                    Netherblade.bounds.x += mouseX - drag.x;
                    Netherblade.bounds.y += mouseY - drag.y;
                } else if (drag.y > 30) {
                    Netherblade.bounds.width += mouseX - frame.getWidth() + 1;
                    Netherblade.bounds.height += mouseY - frame.getHeight() + 1;
                    drag.setLocation(mouseX, mouseY);
                }
                frame.setBounds(Netherblade.bounds);
            }
        }

    }

    private static int getCorner(int x, int y) {
        Insets insets = frame.getInsets();
        int dx = getPosition(x - insets.left, frame.getWidth() - insets.left - insets.right);
        int dy = getPosition(y - insets.top, frame.getHeight() - insets.top - insets.bottom);
        return dx != -1 && dy != -1 ? dy * 5 + dx : -1;
    }

    private static int getPosition(int spot, int width) {
        if (spot < BORDER_DRAG_THICKNESS) {
            return 0;
        } else if (spot < CORNER_DRAG_WIDTH) {
            return 1;
        } else if (spot >= (width - BORDER_DRAG_THICKNESS)) {
            return 4;
        } else if (spot >= (width - CORNER_DRAG_WIDTH)) {
            return 3;
        }
        return 2;
    }

    private static int getCursor(int corner) {
        return corner != -1 ? CURSOR_MAPPING[corner] : 0;
    }
}
