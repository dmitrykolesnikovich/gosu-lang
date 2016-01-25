/*
 *
 * 2002 CaseCenter by Centrica Software
 *
 */
package editor.splitpane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.HashSet;
import java.util.Set;


/**
 * An alternative split pane with an easier api
 */
public class SplitPane extends JComponent implements SwingConstants
{
  public int SPLITTER_WIDTH = 7;

  protected int _iOrientation;
  protected int _iPosition;
  protected JPanel _pane1;
  protected JPanel _pane2;
  protected JComponent _comp1;
  protected JComponent _comp2;
  protected Splitter _splitter;
  protected boolean _hasGripBumps;
  public static final int MIN_POSITION = 0;
  public static final int MAX_POSITION = 100;

  private Set<ActionListener> _listeners = new HashSet<>();


  public SplitPane( int iOrientation, JComponent comp1, JComponent comp2 )
  {
    this(iOrientation, comp1, comp2, true);
  }

  public SplitPane( int iOrientation, JComponent comp1, JComponent comp2, boolean hasGripBumps )
  {
    _iOrientation = iOrientation;

    _pane1 = new JPanel();
    _pane2 = new JPanel();

    _splitter = new Splitter();
    _pane1.setLayout( new BorderLayout() );
    _pane2.setLayout( new BorderLayout() );

    SplitterHandler splitterHandler = new SplitterHandler();
    _splitter.addMouseListener( splitterHandler );
    _splitter.addMouseMotionListener( splitterHandler );

    addMainComponents( comp1, comp2 );
    _hasGripBumps = hasGripBumps;
  }

  protected void addMainComponents( JComponent comp1, JComponent comp2 )
  {
    add( _pane1 );
    add( _pane2 );
    add( _splitter );

    setLayout( new SplitPaneLayout() );

    setTop( comp1 );
    setBottom( comp2 );
  }

  public void setTop( JComponent c )
  {
    _pane1.add( _comp1 = c, BorderLayout.CENTER );
  }

  public JComponent getTop()
  {
    return _comp1;
  }

  public void clearTop() {
    _pane1.removeAll();
  }

  public void setBottom( JComponent c )
  {
    _pane2.add( _comp2 = c, BorderLayout.CENTER );
  }

  public JComponent getBottom()
  {
    return _comp2;
  }

  public void clearBottom() {
    _pane2.removeAll();
  }

  protected int getSplitterWidth()
  {
    return SPLITTER_WIDTH;
  }

  public int getOrientation()
  {
    return _iOrientation;
  }

  public void setOrientation( int iOrientation )
  {
    _iOrientation = iOrientation;

    invalidate();

    if( getParent() != null )
    {
      getParent().validate();
    }
  }

  public boolean hasGripBumps()
  {
    return _hasGripBumps;
  }

  public void setGripBumps(boolean gripBumps)
  {
    _hasGripBumps = gripBumps;
  }

  public int getPosition()
  {
    return _iPosition;
  }

  public void setPosition( int iPos )
  {
    iPos = iPos < MIN_POSITION ? MIN_POSITION : iPos;
    iPos = iPos > MAX_POSITION ? MAX_POSITION : iPos;

    _iPosition = iPos;
    invalidate();

    if( getParent() != null )
    {
      getParent().validate();
    }

    for (ActionListener listener : _listeners) {
      listener.actionPerformed(new ActionEvent(this, 0, Integer.toString( iPos )));
    }
  }

  public void addActionListener(ActionListener l) {
    _listeners.add(l);
  }

  class Splitter extends JPanel
  {
    Splitter()
    {
      setBackground( SystemColor.control );
      int iOrientation = getOrientation();

      setCursor( iOrientation == HORIZONTAL ? Cursor.getPredefinedCursor( Cursor.E_RESIZE_CURSOR )
                 : Cursor.getPredefinedCursor( Cursor.N_RESIZE_CURSOR ) );
    }
  }

  class SplitterHandler extends MouseAdapter implements MouseMotionListener
  {
    protected Point _ptDrag;
    protected Point _ptDragSave;
    protected boolean _bDragging;
    protected Rectangle _rcLast;
    protected MouseEvent _mouseEvent;


    public void mousePressed( MouseEvent e )
    {
      _mouseEvent = e;

      beginDrag( e.getPoint() );
      e.consume();
    }

    public void mouseReleased( MouseEvent e )
    {
      _mouseEvent = e;

      endDrag( e.getPoint() );
      e.consume();
    }

    public void mouseDragged( MouseEvent e )
    {
      _mouseEvent = e;

      drag( e.getPoint() );
      e.consume();
    }

    public void mouseMoved( MouseEvent e )
    {
      e.consume();
    }

    protected void beginDrag( Point pt )
    {
      _bDragging = true;
      _ptDrag = pt;
    }

    protected void endDrag( Point pt )
    {
      try
      {
        if( _rcLast != null )
        {
          if( getOrientation() == HORIZONTAL )
          {
            setPosition( (int)(((float)(_rcLast.x + _rcLast.width / 2) / (float)SplitPane.this.getWidth()) * MAX_POSITION) );
          }
          else
          {
            setPosition( (int)(((float)(_rcLast.y + _rcLast.height / 2) / (float)SplitPane.this.getHeight()) * MAX_POSITION) );
          }

          dragBorderXOR( null );
          _rcLast = null;
          _ptDrag = null;
          _ptDragSave = null;
        }
      }
      finally
      {
        _bDragging = false;
      }
    }

    protected void drag( Point pt )
    {
      if( _ptDrag == null )
      {
        return;
      }

      Rectangle rcBounds = _splitter.getBounds();

      int iXDiff = 0;
      int iYDiff = 0;
      if( getOrientation() == HORIZONTAL )
      {
        iXDiff = pt.x - _ptDrag.x;
      }
      else
      {
        iYDiff = pt.y - _ptDrag.y;
      }

      if( (iXDiff == 0) && (iYDiff == 0) )
      {
        return;
      }

      rcBounds.x += iXDiff;
      rcBounds.y += iYDiff;

      if( _ptDragSave != null && _ptDragSave.equals( pt ) )
      {
        return;
      }

      _ptDragSave = pt;

      Rectangle rcDrag = new Rectangle( rcBounds.x, rcBounds.y, rcBounds.width, rcBounds.height );
      dragBorderXOR( rcDrag );
    }

    protected synchronized void dragBorderXOR( Rectangle rcBounds )
    {
      Container parent = SplitPane.this;

      Graphics g = parent.getGraphics();
      if( g == null )
      {
        return;
      }

      g.setXORMode( new Color( UIManager.getColor( "desktop" ).getRGB() ) );  // This color is replaced with...
      g.setColor( SystemColor.window );     // ...this color when under the pen.

      if( _rcLast != null )
      {
        g.fillRect( _rcLast.x + 1, _rcLast.y + 1, _rcLast.width - 2, _rcLast.height - 2 );
      }
      _rcLast = rcBounds;
      if( _rcLast != null )
      {
        g.fillRect( _rcLast.x + 1, _rcLast.y + 1, _rcLast.width - 2, _rcLast.height - 2 );
      }

      g.dispose();
      g = null;
    }
  }


  class SplitPaneLayout implements LayoutManager
  {
    public SplitPaneLayout()
    {
    }

    public void addLayoutComponent( String name, Component comp )
    {
    }

    public void removeLayoutComponent( Component comp )
    {
    }

    public Dimension preferredLayoutSize( Container parent )
    {
      return parent.getSize();
    }

    public Dimension minimumLayoutSize( Container parent )
    {
      return new Dimension( 0, 0 );
    }

    public void layoutContainer( Container parentContainer )
    {
      int iOrientation = getOrientation();

      Insets insets = getInsets();

      if( iOrientation == HORIZONTAL )
      {
        int iPos = (int)((float)getWidth() * ((float)getPosition() / MAX_POSITION));
        _splitter.setBounds( insets.left + (iPos - getSplitterWidth() / 2), insets.top, getSplitterWidth(), getHeight() - insets.top - insets.bottom );

        _pane1.setBounds( insets.left, insets.top, _splitter.getX() - insets.left, getHeight() - insets.top - insets.bottom );
        _pane2.setBounds( _splitter.getX() + _splitter.getWidth(), insets.top, getWidth() - insets.right - (_splitter.getX() + _splitter.getWidth()), getHeight() - insets.top - insets.bottom );
      }
      else
      {
        int iPos = (int)((float)getHeight() * ((float)getPosition() / MAX_POSITION));
        _splitter.setBounds( insets.left, insets.top + (iPos - getSplitterWidth() / 2), getWidth() - insets.left - insets.right, getSplitterWidth() );

        _pane1.setBounds( insets.left, insets.top, getWidth() - insets.left - insets.right, _splitter.getY() - insets.top );
        _pane2.setBounds( insets.left, _splitter.getY() + _splitter.getHeight(), getWidth() - insets.left - insets.right, getHeight() - insets.bottom - (_splitter.getY() + _splitter.getHeight()) );
      }
    }
  }
}
