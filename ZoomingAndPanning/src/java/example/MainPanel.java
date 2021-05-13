// -*- mode:java; encoding:utf-8 -*-
// vim:set fileencoding=utf-8:
// @homepage@

package example;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import javax.swing.*;

public final class MainPanel extends JPanel {
  private transient ZoomAndPanHandler zoomAndPanHandler;
  private final ImageIcon icon;

  private MainPanel() {
    super(new BorderLayout());
    icon = new ImageIcon(getClass().getResource("CRW_3857_JFR.jpg"));
    setPreferredSize(new Dimension(320, 240));
  }

  @Override public void updateUI() {
    removeMouseListener(zoomAndPanHandler);
    removeMouseMotionListener(zoomAndPanHandler);
    removeMouseWheelListener(zoomAndPanHandler);
    super.updateUI();
    zoomAndPanHandler = new ZoomAndPanHandler();
    addMouseListener(zoomAndPanHandler);
    addMouseMotionListener(zoomAndPanHandler);
    addMouseWheelListener(zoomAndPanHandler);
  }

  @Override protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setTransform(zoomAndPanHandler.getCoordAndZoomTransform());
    icon.paintIcon(this, g2, 0, 0);
    g2.dispose();
  }

  public static void main(String[] args) {
    EventQueue.invokeLater(MainPanel::createAndShowGui);
  }

  private static void createAndShowGui() {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
      ex.printStackTrace();
      Toolkit.getDefaultToolkit().beep();
    }
    JFrame frame = new JFrame("@title@");
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.getContentPane().add(new MainPanel());
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }
}

class ZoomAndPanHandler extends MouseAdapter {
  private static final double ZOOM_FACTOR = 1.2;
  private static final int MIN_ZOOM = -10;
  private static final int MAX_ZOOM = 10;
  private static final int EXTENT = 1;
  private final BoundedRangeModel zoomRange = new DefaultBoundedRangeModel(0, EXTENT, MIN_ZOOM, MAX_ZOOM + EXTENT);
  private final AffineTransform coordAndZoomTransform = new AffineTransform();
  private final Point2D dragStartPoint = new Point();

  @Override public void mousePressed(MouseEvent e) {
    dragStartPoint.setLocation(e.getPoint());
  }

  @Override public void mouseDragged(MouseEvent e) {
    Point2D dragEndPoint = e.getPoint();
    Point2D dragStart = transformPoint(dragStartPoint);
    Point2D dragEnd = transformPoint(dragEndPoint);
    coordAndZoomTransform.translate(dragEnd.getX() - dragStart.getX(), dragEnd.getY() - dragStart.getY());
    dragStartPoint.setLocation(dragEndPoint);
    e.getComponent().repaint();
  }

  @Override public void mouseWheelMoved(MouseWheelEvent e) {
    int dir = e.getWheelRotation();
    int z = zoomRange.getValue();
    zoomRange.setValue(z + EXTENT * (dir > 0 ? -1 : 1));
    if (z == zoomRange.getValue()) {
      return;
    }
    Component c = e.getComponent();
    Rectangle r = c.getBounds();
    Point2D p = new Point2D.Double(r.getCenterX(), r.getCenterY());
    Point2D p1 = transformPoint(p);
    double scale = dir > 0 ? 1 / ZOOM_FACTOR : ZOOM_FACTOR;
    coordAndZoomTransform.scale(scale, scale);
    Point2D p2 = transformPoint(p);
    coordAndZoomTransform.translate(p2.getX() - p1.getX(), p2.getY() - p1.getY());
    c.repaint();
  }

  // https://community.oracle.com/thread/1263955
  // How to implement Zoom & Pan in Java using Graphics2D
  private Point2D transformPoint(Point2D p1) {
    AffineTransform inverse = coordAndZoomTransform;
    boolean hasInverse = coordAndZoomTransform.getDeterminant() != 0d;
    if (hasInverse) {
      try {
        inverse = coordAndZoomTransform.createInverse();
      } catch (NoninvertibleTransformException ex) {
        // should never happen
        assert false;
      }
    }
    Point2D p2 = new Point();
    inverse.transform(p1, p2);
    return p2;
  }

  public AffineTransform getCoordAndZoomTransform() {
    return coordAndZoomTransform;
  }
}
