def config_dict(filename):
    """
    Convert content of config-file into dictionary.
    """
def readconfig(cfgdict):
    """
    Read config-files, change configuration-dict accordingly.

        If there is a turtle.cfg file in the current working directory,
        read it from there. If this contains an importconfig-value,
        say 'myway', construct filename turtle_mayway.cfg else use
        turtle.cfg and read it from the import-directory, where
        turtle.py is located.
        Update configuration dictionary first according to config-file,
        in the import directory, then according to config-file in the
        current working directory.
        If no config-file is found, the default configuration is used.
    
    """
def Vec2D(tuple):
    """
    A 2 dimensional vector class, used as a helper class
        for implementing turtle graphics.
        May be useful for turtle graphics programs also.
        Derived from tuple, so a vector is a tuple!

        Provides (for a, b vectors, k number):
           a+b vector addition
           a-b vector subtraction
           a*b inner product
           k*a and a*k multiplication with scalar
           |a| absolute value of a
           a.rotate(angle) rotation
    
    """
    def __new__(cls, x, y):
        """
        rotate self counterclockwise by angle
        
        """
    def __getnewargs__(self):
        """
        (%.2f,%.2f)
        """
def __methodDict(cls, _dict):
    """
    helper function for Scrolled Canvas
    """
def __methods(cls):
    """
    helper function for Scrolled Canvas
    """
def __forwardmethods(fromClass, toClass, toPart, exclude = ()):
    """
     MANY CHANGES ###

    """
def ScrolledCanvas(TK.Frame):
    """
    Modeled after the scrolled canvas class from Grayons's Tkinter book.

        Used as the default canvas, which pops up automatically when
        using turtle graphics functions or the Turtle class.
    
    """
2021-03-02 20:53:44,454 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, master, width=500, height=350,
                                          canvwidth=600, canvheight=500):
        """
        white
        """
    def reset(self, canvwidth=None, canvheight=None, bg = None):
        """
        Adjust canvas and scrollbars according to given canvas size.
        """
    def adjustScrolls(self):
        """
         Adjust scrollbars according to window- and canvas-size.
        
        """
    def onResize(self, event):
        """
        self-explanatory
        """
    def bbox(self, *args):
        """
         'forward' method, which canvas itself has inherited...
        
        """
    def cget(self, *args, **kwargs):
        """
         'forward' method, which canvas itself has inherited...
        
        """
    def config(self, *args, **kwargs):
        """
         'forward' method, which canvas itself has inherited...
        
        """
    def bind(self, *args, **kwargs):
        """
         'forward' method, which canvas itself has inherited...
        
        """
    def unbind(self, *args, **kwargs):
        """
         'forward' method, which canvas itself has inherited...
        
        """
    def focus_force(self):
        """
         'forward' method, which canvas itself has inherited...
        
        """
def _Root(TK.Tk):
    """
    Root class for Screen based on Tkinter.
    """
    def __init__(self):
        """
        both
        """
    def _getcanvas(self):
        """
        %dx%d%+d%+d
        """
    def ondestroy(self, destroy):
        """
        WM_DELETE_WINDOW
        """
    def win_width(self):
        """
        Provide the basic graphics functionality.
               Interface between Tkinter and turtle.py.

               To port turtle.py to some different graphics toolkit
               a corresponding TurtleScreenBase class has to be implemented.
    
        """
    def _blankimage():
        """
        return a blank image object
        
        """
    def _image(filename):
        """
        return an image object containing the
                imagedata from a gif-file named filename.
        
        """
    def __init__(self, cv):
        """
         expected: ordinary TK.Canvas
        """
    def _createpoly(self):
        """
        Create an invisible polygon item on canvas self.cv)
        
        """
2021-03-02 20:53:44,462 : INFO : tokenize_signature : --> do i ever get here?
    def _drawpoly(self, polyitem, coordlist, fill=None,
                  outline=None, width=None, top=False):
        """
        Configure polygonitem polyitem according to provided
                arguments:
                coordlist is sequence of coordinates
                fill is filling color
                outline is outline color
                top is a boolean value, which specifies if polyitem
                will be put on top of the canvas' displaylist so it
                will not be covered by other items.
        
        """
    def _createline(self):
        """
        Create an invisible line item on canvas self.cv)
        
        """
2021-03-02 20:53:44,463 : INFO : tokenize_signature : --> do i ever get here?
    def _drawline(self, lineitem, coordlist=None,
                  fill=None, width=None, top=False):
        """
        Configure lineitem according to provided arguments:
                coordlist is sequence of coordinates
                fill is drawing color
                width is width of drawn line.
                top is a boolean value, which specifies if polyitem
                will be put on top of the canvas' displaylist so it
                will not be covered by other items.
        
        """
    def _delete(self, item):
        """
        Delete graphics item from canvas.
                If item is"all" delete all graphics items.
        
        """
    def _update(self):
        """
        Redraw graphics items on canvas
        
        """
    def _delay(self, delay):
        """
        Delay subsequent canvas actions for delay ms.
        """
    def _iscolorstring(self, color):
        """
        Check if the string color is a legal Tkinter color string.
        
        """
    def _bgcolor(self, color=None):
        """
        Set canvas' backgroundcolor if color is not None,
                else return backgroundcolor.
        """
    def _write(self, pos, txt, align, font, pencolor):
        """
        Write txt at pos in canvas with specified font
                and color.
                Return text item and x-coord of right bottom corner
                of text's bounding box.
        """
    def _onclick(self, item, fun, num=1, add=None):
        """
        Bind fun to mouse-click event on turtle.
                fun must be a function with two arguments, the coordinates
                of the clicked point on the canvas.
                num, the number of the mouse-button defaults to 1
        
        """
            def eventfun(event):
                """
                <Button-%s>
                """
    def _onrelease(self, item, fun, num=1, add=None):
        """
        Bind fun to mouse-button-release event on turtle.
                fun must be a function with two arguments, the coordinates
                of the point on the canvas where mouse button is released.
                num, the number of the mouse-button defaults to 1

                If a turtle is clicked, first _onclick-event will be performed,
                then _onscreensclick-event.
        
        """
            def eventfun(event):
                """
                <Button%s-ButtonRelease>
                """
    def _ondrag(self, item, fun, num=1, add=None):
        """
        Bind fun to mouse-move-event (with pressed mouse button) on turtle.
                fun must be a function with two arguments, the coordinates of the
                actual mouse position on the canvas.
                num, the number of the mouse-button defaults to 1

                Every sequence of mouse-move-events on a turtle is preceded by a
                mouse-click event on that turtle.
        
        """
            def eventfun(event):
                """
                <Button%s-Motion>
                """
    def _onscreenclick(self, fun, num=1, add=None):
        """
        Bind fun to mouse-click event on canvas.
                fun must be a function with two arguments, the coordinates
                of the clicked point on the canvas.
                num, the number of the mouse-button defaults to 1

                If a turtle is clicked, first _onclick-event will be performed,
                then _onscreensclick-event.
        
        """
            def eventfun(event):
                """
                <Button-%s>
                """
    def _onkeyrelease(self, fun, key):
        """
        Bind fun to key-release event of key.
                Canvas must have focus. See method listen
        
        """
            def eventfun(event):
                """
                <KeyRelease-%s>
                """
    def _onkeypress(self, fun, key=None):
        """
        If key is given, bind fun to key-press event of key.
                Otherwise bind fun to any key-press.
                Canvas must have focus. See method listen.
        
        """
            def eventfun(event):
                """
                <KeyPress>
                """
    def _listen(self):
        """
        Set focus on canvas (in order to collect key-events)
        
        """
    def _ontimer(self, fun, t):
        """
        Install a timer, which calls fun after t milliseconds.
        
        """
    def _createimage(self, image):
        """
        Create and return image item on canvas.
        
        """
    def _drawimage(self, item, pos, image):
        """
        Configure image item as to draw image object
                at position (x,y) on canvas)
        
        """
    def _setbgpic(self, item, image):
        """
        Configure image item as to draw image object
                at center of canvas. Set item to the first item
                in the displaylist, so it will be drawn below
                any other item .
        """
    def _type(self, item):
        """
        Return 'line' or 'polygon' or 'image' depending on
                type of item.
        
        """
    def _pointlist(self, item):
        """
        returns list of coordinate-pairs of points of item
                Example (for insiders):
                >>> from turtle import *
                >>> getscreen()._pointlist(getturtle().turtle._item)
                [(0.0, 9.9999999999999982), (0.0, -9.9999999999999982),
                (9.9999999999999982, 0.0)]
                >>> 
        """
    def _setscrollregion(self, srx1, sry1, srx2, sry2):
        """
        Resize the canvas the turtles are drawing on. Does
                not alter the drawing window.
        
        """
    def _window_size(self):
        """
         Return the width and height of the turtle window.
        
        """
    def mainloop(self):
        """
        Starts event loop - calling Tkinter's mainloop function.

                No argument.

                Must be last statement in a turtle graphics program.
                Must NOT be used if a script is run from within IDLE in -n mode
                (No subprocess) - for interactive use of turtle graphics.

                Example (for a TurtleScreen instance named screen):
                >>> screen.mainloop()

        
        """
    def textinput(self, title, prompt):
        """
        Pop up a dialog window for input of a string.

                Arguments: title is the title of the dialog window,
                prompt is a text mostly describing what information to input.

                Return the string input
                If the dialog is canceled, return None.

                Example (for a TurtleScreen instance named screen):
                >>> screen.textinput("NIM", "Name of first player:")

        
        """
    def numinput(self, title, prompt, default=None, minval=None, maxval=None):
        """
        Pop up a dialog window for input of a number.

                Arguments: title is the title of the dialog window,
                prompt is a text mostly describing what numerical information to input.
                default: default value
                minval: minimum value for input
                maxval: maximum value for input

                The number input must be in the range minval .. maxval if these are
                given. If not, a hint is issued and the dialog remains open for
                correction. Return the number input.
                If the dialog is canceled,  return None.

                Example (for a TurtleScreen instance named screen):
                >>> screen.numinput("Poker", "Your stakes:", 1000, minval=10, maxval=10000)

        
        """
def Terminator (Exception):
    """
    Will be raised in TurtleScreen.update, if _RUNNING becomes False.

        This stops execution of a turtle graphics script.
        Main purpose: use in the Demo-Viewer turtle.Demo.py.
    
    """
def TurtleGraphicsError(Exception):
    """
    Some TurtleGraphics Error
    
    """
def Shape(object):
    """
    Data structure modeling shapes.

        attribute _type is one of "polygon", "image", "compound"
        attribute _data is - depending on _type a poygon-tuple,
        an image or a list constructed using the addcomponent method.
    
    """
    def __init__(self, type_, data=None):
        """
        polygon
        """
    def addcomponent(self, poly, fill, outline=None):
        """
        Add component to a shape of type compound.

                Arguments: poly is a polygon, i. e. a tuple of number pairs.
                fill is the fillcolor of the component,
                outline is the outline color of the component.

                call (for a Shapeobject namend s):
                --   s.addcomponent(((0,0), (10,10), (-10,10)), "red", "blue")

                Example:
                >>> poly = ((0,0),(10,-5),(0,10),(-10,-5))
                >>> s = Shape("compound")
                >>> s.addcomponent(poly, "red", "blue")
                >>> # .. add more components and then use register_shape()
        
        """
def Tbuffer(object):
    """
    Ring buffer used as undobuffer for RawTurtle objects.
    """
    def __init__(self, bufsize=10):
        """
 
        """
def TurtleScreen(TurtleScreenBase):
    """
    Provides screen oriented methods like setbg etc.

        Only relies upon the methods of TurtleScreenBase and NOT
        upon components of the underlying graphics toolkit -
        which is Tkinter in this case.
    
    """
2021-03-02 20:53:44,477 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, cv, mode=_CFG["mode"],
                 colormode=_CFG["colormode"], delay=_CFG["delay"]):
        """
        arrow
        """
    def clear(self):
        """
        Delete all drawings and all turtles from the TurtleScreen.

                No argument.

                Reset empty TurtleScreen to its initial state: white background,
                no backgroundimage, no eventbindings and tracing on.

                Example (for a TurtleScreen instance named screen):
                >>> screen.clear()

                Note: this method is not available as function.
        
        """
    def mode(self, mode=None):
        """
        Set turtle-mode ('standard', 'logo' or 'world') and perform reset.

                Optional argument:
                mode -- one of the strings 'standard', 'logo' or 'world'

                Mode 'standard' is compatible with turtle.py.
                Mode 'logo' is compatible with most Logo-Turtle-Graphics.
                Mode 'world' uses userdefined 'worldcoordinates'. *Attention*: in
                this mode angles appear distorted if x/y unit-ratio doesn't equal 1.
                If mode is not given, return the current mode.

                     Mode      Initial turtle heading     positive angles
                 ------------|-------------------------|-------------------
                  'standard'    to the right (east)       counterclockwise
                    'logo'        upward    (north)         clockwise

                Examples:
                >>> mode('logo')   # resets turtle heading to north
                >>> mode()
                'logo'
        
        """
    def setworldcoordinates(self, llx, lly, urx, ury):
        """
        Set up a user defined coordinate-system.

                Arguments:
                llx -- a number, x-coordinate of lower left corner of canvas
                lly -- a number, y-coordinate of lower left corner of canvas
                urx -- a number, x-coordinate of upper right corner of canvas
                ury -- a number, y-coordinate of upper right corner of canvas

                Set up user coodinat-system and switch to mode 'world' if necessary.
                This performs a screen.reset. If mode 'world' is already active,
                all drawings are redrawn according to the new coordinates.

                But ATTENTION: in user-defined coordinatesystems angles may appear
                distorted. (see Screen.mode())

                Example (for a TurtleScreen instance named screen):
                >>> screen.setworldcoordinates(-10,-0.5,50,1.5)
                >>> for _ in range(36):
                ...     left(10)
                ...     forward(0.5)
        
        """
    def register_shape(self, name, shape=None):
        """
        Adds a turtle shape to TurtleScreen's shapelist.

                Arguments:
                (1) name is the name of a gif-file and shape is None.
                    Installs the corresponding image shape.
                    !! Image-shapes DO NOT rotate when turning the turtle,
                    !! so they do not display the heading of the turtle!
                (2) name is an arbitrary string and shape is a tuple
                    of pairs of coordinates. Installs the corresponding
                    polygon shape
                (3) name is an arbitrary string and shape is a
                    (compound) Shape object. Installs the corresponding
                    compound shape.
                To use a shape, you have to issue the command shape(shapename).

                call: register_shape("turtle.gif")
                --or: register_shape("tri", ((0,0), (10,10), (-10,10)))

                Example (for a TurtleScreen instance named screen):
                >>> screen.register_shape("triangle", ((5,-3),(0,5),(-5,-3)))

        
        """
    def _colorstr(self, color):
        """
        Return color string corresponding to args.

                Argument may be a string or a tuple of three
                numbers corresponding to actual colormode,
                i.e. in the range 0<=n<=colormode.

                If the argument doesn't represent a color,
                an error is raised.
        
        """
    def _color(self, cstr):
        """

        """
    def colormode(self, cmode=None):
        """
        Return the colormode or set it to 1.0 or 255.

                Optional argument:
                cmode -- one of the values 1.0 or 255

                r, g, b values of colortriples have to be in range 0..cmode.

                Example (for a TurtleScreen instance named screen):
                >>> screen.colormode()
                1.0
                >>> screen.colormode(255)
                >>> pencolor(240,160,80)
        
        """
    def reset(self):
        """
        Reset all Turtles on the Screen to their initial state.

                No argument.

                Example (for a TurtleScreen instance named screen):
                >>> screen.reset()
        
        """
    def turtles(self):
        """
        Return the list of turtles on the screen.

                Example (for a TurtleScreen instance named screen):
                >>> screen.turtles()
                [<turtle.Turtle object at 0x00E11FB0>]
        
        """
    def bgcolor(self, *args):
        """
        Set or return backgroundcolor of the TurtleScreen.

                Arguments (if given): a color string or three numbers
                in the range 0..colormode or a 3-tuple of such numbers.

                Example (for a TurtleScreen instance named screen):
                >>> screen.bgcolor("orange")
                >>> screen.bgcolor()
                'orange'
                >>> screen.bgcolor(0.5,0,0.5)
                >>> screen.bgcolor()
                '#800080'
        
        """
    def tracer(self, n=None, delay=None):
        """
        Turns turtle animation on/off and set delay for update drawings.

                Optional arguments:
                n -- nonnegative  integer
                delay -- nonnegative  integer

                If n is given, only each n-th regular screen update is really performed.
                (Can be used to accelerate the drawing of complex graphics.)
                Second arguments sets delay value (see RawTurtle.delay())

                Example (for a TurtleScreen instance named screen):
                >>> screen.tracer(8, 25)
                >>> dist = 2
                >>> for i in range(200):
                ...     fd(dist)
                ...     rt(90)
                ...     dist += 2
        
        """
    def delay(self, delay=None):
        """
         Return or set the drawing delay in milliseconds.

                Optional argument:
                delay -- positive integer

                Example (for a TurtleScreen instance named screen):
                >>> screen.delay(15)
                >>> screen.delay()
                15
        
        """
    def _incrementudc(self):
        """
        Increment update counter.
        """
    def update(self):
        """
        Perform a TurtleScreen update.
        
        """
    def window_width(self):
        """
         Return the width of the turtle window.

                Example (for a TurtleScreen instance named screen):
                >>> screen.window_width()
                640
        
        """
    def window_height(self):
        """
         Return the height of the turtle window.

                Example (for a TurtleScreen instance named screen):
                >>> screen.window_height()
                480
        
        """
    def getcanvas(self):
        """
        Return the Canvas of this TurtleScreen.

                No argument.

                Example (for a Screen instance named screen):
                >>> cv = screen.getcanvas()
                >>> cv
                <turtle.ScrolledCanvas instance at 0x010742D8>
        
        """
    def getshapes(self):
        """
        Return a list of names of all currently available turtle shapes.

                No argument.

                Example (for a TurtleScreen instance named screen):
                >>> screen.getshapes()
                ['arrow', 'blank', 'circle', ... , 'turtle']
        
        """
    def onclick(self, fun, btn=1, add=None):
        """
        Bind fun to mouse-click event on canvas.

                Arguments:
                fun -- a function with two arguments, the coordinates of the
                       clicked point on the canvas.
                btn -- the number of the mouse-button, defaults to 1

                Example (for a TurtleScreen instance named screen)

                >>> screen.onclick(goto)
                >>> # Subsequently clicking into the TurtleScreen will
                >>> # make the turtle move to the clicked point.
                >>> screen.onclick(None)
        
        """
    def onkey(self, fun, key):
        """
        Bind fun to key-release event of key.

                Arguments:
                fun -- a function with no arguments
                key -- a string: key (e.g. "a") or key-symbol (e.g. "space")

                In order to be able to register key-events, TurtleScreen
                must have focus. (See method listen.)

                Example (for a TurtleScreen instance named screen):

                >>> def f():
                ...     fd(50)
                ...     lt(60)
                ...
                >>> screen.onkey(f, "Up")
                >>> screen.listen()

                Subsequently the turtle can be moved by repeatedly pressing
                the up-arrow key, consequently drawing a hexagon

        
        """
    def onkeypress(self, fun, key=None):
        """
        Bind fun to key-press event of key if key is given,
                or to any key-press-event if no key is given.

                Arguments:
                fun -- a function with no arguments
                key -- a string: key (e.g. "a") or key-symbol (e.g. "space")

                In order to be able to register key-events, TurtleScreen
                must have focus. (See method listen.)

                Example (for a TurtleScreen instance named screen
                and a Turtle instance named turtle):

                >>> def f():
                ...     fd(50)
                ...     lt(60)
                ...
                >>> screen.onkeypress(f, "Up")
                >>> screen.listen()

                Subsequently the turtle can be moved by repeatedly pressing
                the up-arrow key, or by keeping pressed the up-arrow key.
                consequently drawing a hexagon.
        
        """
    def listen(self, xdummy=None, ydummy=None):
        """
        Set focus on TurtleScreen (in order to collect key-events)

                No arguments.
                Dummy arguments are provided in order
                to be able to pass listen to the onclick method.

                Example (for a TurtleScreen instance named screen):
                >>> screen.listen()
        
        """
    def ontimer(self, fun, t=0):
        """
        Install a timer, which calls fun after t milliseconds.

                Arguments:
                fun -- a function with no arguments.
                t -- a number >= 0

                Example (for a TurtleScreen instance named screen):

                >>> running = True
                >>> def f():
                ...     if running:
                ...             fd(50)
                ...             lt(60)
                ...             screen.ontimer(f, 250)
                ...
                >>> f()   # makes the turtle marching around
                >>> running = False
        
        """
    def bgpic(self, picname=None):
        """
        Set background image or return name of current backgroundimage.

                Optional argument:
                picname -- a string, name of a gif-file or "nopic".

                If picname is a filename, set the corresponding image as background.
                If picname is "nopic", delete backgroundimage, if present.
                If picname is None, return the filename of the current backgroundimage.

                Example (for a TurtleScreen instance named screen):
                >>> screen.bgpic()
                'nopic'
                >>> screen.bgpic("landscape.gif")
                >>> screen.bgpic()
                'landscape.gif'
        
        """
    def screensize(self, canvwidth=None, canvheight=None, bg=None):
        """
        Resize the canvas the turtles are drawing on.

                Optional arguments:
                canvwidth -- positive integer, new width of canvas in pixels
                canvheight --  positive integer, new height of canvas in pixels
                bg -- colorstring or color-tuple, new backgroundcolor
                If no arguments are given, return current (canvaswidth, canvasheight)

                Do not alter the drawing window. To observe hidden parts of
                the canvas use the scrollbars. (Can make visible those parts
                of a drawing, which were outside the canvas before!)

                Example (for a Turtle instance named turtle):
                >>> turtle.screensize(2000,1500)
                >>> # e.g. to search for an erroneously escaped turtle ;-)
        
        """
def TNavigator(object):
    """
    Navigation part of the RawTurtle.
        Implements methods for turtle movement.
    
    """
    def __init__(self, mode=DEFAULT_MODE):
        """
        reset turtle to its initial values

                Will be overwritten by parent class
        
        """
    def _setmode(self, mode=None):
        """
        Set turtle-mode to 'standard', 'world' or 'logo'.
        
        """
    def _setDegreesPerAU(self, fullcircle):
        """
        Helper function for degrees() and radians()
        """
    def degrees(self, fullcircle=360.0):
        """
         Set angle measurement units to degrees.

                Optional argument:
                fullcircle -  a number

                Set angle measurement units, i. e. set number
                of 'degrees' for a full circle. Default value is
                360 degrees.

                Example (for a Turtle instance named turtle):
                >>> turtle.left(90)
                >>> turtle.heading()
                90

                Change angle measurement unit to grad (also known as gon,
                grade, or gradian and equals 1/100-th of the right angle.)
                >>> turtle.degrees(400.0)
                >>> turtle.heading()
                100

        
        """
    def radians(self):
        """
         Set the angle measurement units to radians.

                No arguments.

                Example (for a Turtle instance named turtle):
                >>> turtle.heading()
                90
                >>> turtle.radians()
                >>> turtle.heading()
                1.5707963267948966
        
        """
    def _go(self, distance):
        """
        move turtle forward by specified distance
        """
    def _rotate(self, angle):
        """
        Turn turtle counterclockwise by specified angle if angle > 0.
        """
    def _goto(self, end):
        """
        move turtle to position end.
        """
    def forward(self, distance):
        """
        Move the turtle forward by the specified distance.

                Aliases: forward | fd

                Argument:
                distance -- a number (integer or float)

                Move the turtle forward by the specified distance, in the direction
                the turtle is headed.

                Example (for a Turtle instance named turtle):
                >>> turtle.position()
                (0.00, 0.00)
                >>> turtle.forward(25)
                >>> turtle.position()
                (25.00,0.00)
                >>> turtle.forward(-75)
                >>> turtle.position()
                (-50.00,0.00)
        
        """
    def back(self, distance):
        """
        Move the turtle backward by distance.

                Aliases: back | backward | bk

                Argument:
                distance -- a number

                Move the turtle backward by distance ,opposite to the direction the
                turtle is headed. Do not change the turtle's heading.

                Example (for a Turtle instance named turtle):
                >>> turtle.position()
                (0.00, 0.00)
                >>> turtle.backward(30)
                >>> turtle.position()
                (-30.00, 0.00)
        
        """
    def right(self, angle):
        """
        Turn turtle right by angle units.

                Aliases: right | rt

                Argument:
                angle -- a number (integer or float)

                Turn turtle right by angle units. (Units are by default degrees,
                but can be set via the degrees() and radians() functions.)
                Angle orientation depends on mode. (See this.)

                Example (for a Turtle instance named turtle):
                >>> turtle.heading()
                22.0
                >>> turtle.right(45)
                >>> turtle.heading()
                337.0
        
        """
    def left(self, angle):
        """
        Turn turtle left by angle units.

                Aliases: left | lt

                Argument:
                angle -- a number (integer or float)

                Turn turtle left by angle units. (Units are by default degrees,
                but can be set via the degrees() and radians() functions.)
                Angle orientation depends on mode. (See this.)

                Example (for a Turtle instance named turtle):
                >>> turtle.heading()
                22.0
                >>> turtle.left(45)
                >>> turtle.heading()
                67.0
        
        """
    def pos(self):
        """
        Return the turtle's current location (x,y), as a Vec2D-vector.

                Aliases: pos | position

                No arguments.

                Example (for a Turtle instance named turtle):
                >>> turtle.pos()
                (0.00, 240.00)
        
        """
    def xcor(self):
        """
         Return the turtle's x coordinate.

                No arguments.

                Example (for a Turtle instance named turtle):
                >>> reset()
                >>> turtle.left(60)
                >>> turtle.forward(100)
                >>> print turtle.xcor()
                50.0
        
        """
    def ycor(self):
        """
         Return the turtle's y coordinate
                ---
                No arguments.

                Example (for a Turtle instance named turtle):
                >>> reset()
                >>> turtle.left(60)
                >>> turtle.forward(100)
                >>> print turtle.ycor()
                86.6025403784
        
        """
    def goto(self, x, y=None):
        """
        Move turtle to an absolute position.

                Aliases: setpos | setposition | goto:

                Arguments:
                x -- a number      or     a pair/vector of numbers
                y -- a number             None

                call: goto(x, y)         # two coordinates
                --or: goto((x, y))       # a pair (tuple) of coordinates
                --or: goto(vec)          # e.g. as returned by pos()

                Move turtle to an absolute position. If the pen is down,
                a line will be drawn. The turtle's orientation does not change.

                Example (for a Turtle instance named turtle):
                >>> tp = turtle.pos()
                >>> tp
                (0.00, 0.00)
                >>> turtle.setpos(60,30)
                >>> turtle.pos()
                (60.00,30.00)
                >>> turtle.setpos((20,80))
                >>> turtle.pos()
                (20.00,80.00)
                >>> turtle.setpos(tp)
                >>> turtle.pos()
                (0.00,0.00)
        
        """
    def home(self):
        """
        Move turtle to the origin - coordinates (0,0).

                No arguments.

                Move turtle to the origin - coordinates (0,0) and set its
                heading to its start-orientation (which depends on mode).

                Example (for a Turtle instance named turtle):
                >>> turtle.home()
        
        """
    def setx(self, x):
        """
        Set the turtle's first coordinate to x

                Argument:
                x -- a number (integer or float)

                Set the turtle's first coordinate to x, leave second coordinate
                unchanged.

                Example (for a Turtle instance named turtle):
                >>> turtle.position()
                (0.00, 240.00)
                >>> turtle.setx(10)
                >>> turtle.position()
                (10.00, 240.00)
        
        """
    def sety(self, y):
        """
        Set the turtle's second coordinate to y

                Argument:
                y -- a number (integer or float)

                Set the turtle's first coordinate to x, second coordinate remains
                unchanged.

                Example (for a Turtle instance named turtle):
                >>> turtle.position()
                (0.00, 40.00)
                >>> turtle.sety(-10)
                >>> turtle.position()
                (0.00, -10.00)
        
        """
    def distance(self, x, y=None):
        """
        Return the distance from the turtle to (x,y) in turtle step units.

                Arguments:
                x -- a number   or  a pair/vector of numbers   or   a turtle instance
                y -- a number       None                            None

                call: distance(x, y)         # two coordinates
                --or: distance((x, y))       # a pair (tuple) of coordinates
                --or: distance(vec)          # e.g. as returned by pos()
                --or: distance(mypen)        # where mypen is another turtle

                Example (for a Turtle instance named turtle):
                >>> turtle.pos()
                (0.00, 0.00)
                >>> turtle.distance(30,40)
                50.0
                >>> pen = Turtle()
                >>> pen.forward(77)
                >>> turtle.distance(pen)
                77.0
        
        """
    def towards(self, x, y=None):
        """
        Return the angle of the line from the turtle's position to (x, y).

                Arguments:
                x -- a number   or  a pair/vector of numbers   or   a turtle instance
                y -- a number       None                            None

                call: distance(x, y)         # two coordinates
                --or: distance((x, y))       # a pair (tuple) of coordinates
                --or: distance(vec)          # e.g. as returned by pos()
                --or: distance(mypen)        # where mypen is another turtle

                Return the angle, between the line from turtle-position to position
                specified by x, y and the turtle's start orientation. (Depends on
                modes - "standard" or "logo")

                Example (for a Turtle instance named turtle):
                >>> turtle.pos()
                (10.00, 10.00)
                >>> turtle.towards(0,0)
                225.0
        
        """
    def heading(self):
        """
         Return the turtle's current heading.

                No arguments.

                Example (for a Turtle instance named turtle):
                >>> turtle.left(67)
                >>> turtle.heading()
                67.0
        
        """
    def setheading(self, to_angle):
        """
        Set the orientation of the turtle to to_angle.

                Aliases:  setheading | seth

                Argument:
                to_angle -- a number (integer or float)

                Set the orientation of the turtle to to_angle.
                Here are some common directions in degrees:

                 standard - mode:          logo-mode:
                -------------------|--------------------
                   0 - east                0 - north
                  90 - north              90 - east
                 180 - west              180 - south
                 270 - south             270 - west

                Example (for a Turtle instance named turtle):
                >>> turtle.setheading(90)
                >>> turtle.heading()
                90
        
        """
    def circle(self, radius, extent = None, steps = None):
        """
         Draw a circle with given radius.

                Arguments:
                radius -- a number
                extent (optional) -- a number
                steps (optional) -- an integer

                Draw a circle with given radius. The center is radius units left
                of the turtle; extent - an angle - determines which part of the
                circle is drawn. If extent is not given, draw the entire circle.
                If extent is not a full circle, one endpoint of the arc is the
                current pen position. Draw the arc in counterclockwise direction
                if radius is positive, otherwise in clockwise direction. Finally
                the direction of the turtle is changed by the amount of extent.

                As the circle is approximated by an inscribed regular polygon,
                steps determines the number of steps to use. If not given,
                it will be calculated automatically. Maybe used to draw regular
                polygons.

                call: circle(radius)                  # full circle
                --or: circle(radius, extent)          # arc
                --or: circle(radius, extent, steps)
                --or: circle(radius, steps=6)         # 6-sided polygon

                Example (for a Turtle instance named turtle):
                >>> turtle.circle(50)
                >>> turtle.circle(120, 180)  # semicircle
        
        """
    def speed(self, s=0):
        """
        dummy method - to be overwritten by child class
        """
    def _tracer(self, a=None, b=None):
        """
        dummy method - to be overwritten by child class
        """
    def _delay(self, n=None):
        """
        dummy method - to be overwritten by child class
        """
def TPen(object):
    """
    Drawing part of the RawTurtle.
        Implements drawing properties.
    
    """
    def __init__(self, resizemode=_CFG["resizemode"]):
        """
         or "user" or "noresize
        """
2021-03-02 20:53:44,495 : INFO : tokenize_signature : --> do i ever get here?
    def _reset(self, pencolor=_CFG["pencolor"],
                     fillcolor=_CFG["fillcolor"]):
        """
        Set resizemode to one of the values: "auto", "user", "noresize".

                (Optional) Argument:
                rmode -- one of the strings "auto", "user", "noresize"

                Different resizemodes have the following effects:
                  - "auto" adapts the appearance of the turtle
                           corresponding to the value of pensize.
                  - "user" adapts the appearance of the turtle according to the
                           values of stretchfactor and outlinewidth (outline),
                           which are set by shapesize()
                  - "noresize" no adaption of the turtle's appearance takes place.
                If no argument is given, return current resizemode.
                resizemode("user") is called by a call of shapesize with arguments.


                Examples (for a Turtle instance named turtle):
                >>> turtle.resizemode("noresize")
                >>> turtle.resizemode()
                'noresize'
        
        """
    def pensize(self, width=None):
        """
        Set or return the line thickness.

                Aliases:  pensize | width

                Argument:
                width -- positive number

                Set the line thickness to width or return it. If resizemode is set
                to "auto" and turtleshape is a polygon, that polygon is drawn with
                the same line thickness. If no argument is given, current pensize
                is returned.

                Example (for a Turtle instance named turtle):
                >>> turtle.pensize()
                1
                >>> turtle.pensize(10)   # from here on lines of width 10 are drawn
        
        """
    def penup(self):
        """
        Pull the pen up -- no drawing when moving.

                Aliases: penup | pu | up

                No argument

                Example (for a Turtle instance named turtle):
                >>> turtle.penup()
        
        """
    def pendown(self):
        """
        Pull the pen down -- drawing when moving.

                Aliases: pendown | pd | down

                No argument.

                Example (for a Turtle instance named turtle):
                >>> turtle.pendown()
        
        """
    def isdown(self):
        """
        Return True if pen is down, False if it's up.

                No argument.

                Example (for a Turtle instance named turtle):
                >>> turtle.penup()
                >>> turtle.isdown()
                False
                >>> turtle.pendown()
                >>> turtle.isdown()
                True
        
        """
    def speed(self, speed=None):
        """
         Return or set the turtle's speed.

                Optional argument:
                speed -- an integer in the range 0..10 or a speedstring (see below)

                Set the turtle's speed to an integer value in the range 0 .. 10.
                If no argument is given: return current speed.

                If input is a number greater than 10 or smaller than 0.5,
                speed is set to 0.
                Speedstrings  are mapped to speedvalues in the following way:
                    'fastest' :  0
                    'fast'    :  10
                    'normal'  :  6
                    'slow'    :  3
                    'slowest' :  1
                speeds from 1 to 10 enforce increasingly faster animation of
                line drawing and turtle turning.

                Attention:
                speed = 0 : *no* animation takes place. forward/back makes turtle jump
                and likewise left/right make the turtle turn instantly.

                Example (for a Turtle instance named turtle):
                >>> turtle.speed(3)
        
        """
    def color(self, *args):
        """
        Return or set the pencolor and fillcolor.

                Arguments:
                Several input formats are allowed.
                They use 0, 1, 2, or 3 arguments as follows:

                color()
                    Return the current pencolor and the current fillcolor
                    as a pair of color specification strings as are returned
                    by pencolor and fillcolor.
                color(colorstring), color((r,g,b)), color(r,g,b)
                    inputs as in pencolor, set both, fillcolor and pencolor,
                    to the given value.
                color(colorstring1, colorstring2),
                color((r1,g1,b1), (r2,g2,b2))
                    equivalent to pencolor(colorstring1) and fillcolor(colorstring2)
                    and analogously, if the other input format is used.

                If turtleshape is a polygon, outline and interior of that polygon
                is drawn with the newly set colors.
                For more info see: pencolor, fillcolor

                Example (for a Turtle instance named turtle):
                >>> turtle.color('red', 'green')
                >>> turtle.color()
                ('red', 'green')
                >>> colormode(255)
                >>> color((40, 80, 120), (160, 200, 240))
                >>> color()
                ('#285078', '#a0c8f0')
        
        """
    def pencolor(self, *args):
        """
         Return or set the pencolor.

                Arguments:
                Four input formats are allowed:
                  - pencolor()
                    Return the current pencolor as color specification string,
                    possibly in hex-number format (see example).
                    May be used as input to another color/pencolor/fillcolor call.
                  - pencolor(colorstring)
                    s is a Tk color specification string, such as "red" or "yellow"
                  - pencolor((r, g, b))
                    *a tuple* of r, g, and b, which represent, an RGB color,
                    and each of r, g, and b are in the range 0..colormode,
                    where colormode is either 1.0 or 255
                  - pencolor(r, g, b)
                    r, g, and b represent an RGB color, and each of r, g, and b
                    are in the range 0..colormode

                If turtleshape is a polygon, the outline of that polygon is drawn
                with the newly set pencolor.

                Example (for a Turtle instance named turtle):
                >>> turtle.pencolor('brown')
                >>> tup = (0.2, 0.8, 0.55)
                >>> turtle.pencolor(tup)
                >>> turtle.pencolor()
                '#33cc8c'
        
        """
    def fillcolor(self, *args):
        """
         Return or set the fillcolor.

                Arguments:
                Four input formats are allowed:
                  - fillcolor()
                    Return the current fillcolor as color specification string,
                    possibly in hex-number format (see example).
                    May be used as input to another color/pencolor/fillcolor call.
                  - fillcolor(colorstring)
                    s is a Tk color specification string, such as "red" or "yellow"
                  - fillcolor((r, g, b))
                    *a tuple* of r, g, and b, which represent, an RGB color,
                    and each of r, g, and b are in the range 0..colormode,
                    where colormode is either 1.0 or 255
                  - fillcolor(r, g, b)
                    r, g, and b represent an RGB color, and each of r, g, and b
                    are in the range 0..colormode

                If turtleshape is a polygon, the interior of that polygon is drawn
                with the newly set fillcolor.

                Example (for a Turtle instance named turtle):
                >>> turtle.fillcolor('violet')
                >>> col = turtle.pencolor()
                >>> turtle.fillcolor(col)
                >>> turtle.fillcolor(0, .5, 0)
        
        """
    def showturtle(self):
        """
        Makes the turtle visible.

                Aliases: showturtle | st

                No argument.

                Example (for a Turtle instance named turtle):
                >>> turtle.hideturtle()
                >>> turtle.showturtle()
        
        """
    def hideturtle(self):
        """
        Makes the turtle invisible.

                Aliases: hideturtle | ht

                No argument.

                It's a good idea to do this while you're in the
                middle of a complicated drawing, because hiding
                the turtle speeds up the drawing observably.

                Example (for a Turtle instance named turtle):
                >>> turtle.hideturtle()
        
        """
    def isvisible(self):
        """
        Return True if the Turtle is shown, False if it's hidden.

                No argument.

                Example (for a Turtle instance named turtle):
                >>> turtle.hideturtle()
                >>> print turtle.isvisible():
                False
        
        """
    def pen(self, pen=None, **pendict):
        """
        Return or set the pen's attributes.

                Arguments:
                    pen -- a dictionary with some or all of the below listed keys.
                    **pendict -- one or more keyword-arguments with the below
                                 listed keys as keywords.

                Return or set the pen's attributes in a 'pen-dictionary'
                with the following key/value pairs:
                   "shown"      :   True/False
                   "pendown"    :   True/False
                   "pencolor"   :   color-string or color-tuple
                   "fillcolor"  :   color-string or color-tuple
                   "pensize"    :   positive number
                   "speed"      :   number in range 0..10
                   "resizemode" :   "auto" or "user" or "noresize"
                   "stretchfactor": (positive number, positive number)
                   "shearfactor":   number
                   "outline"    :   positive number
                   "tilt"       :   number

                This dictionary can be used as argument for a subsequent
                pen()-call to restore the former pen-state. Moreover one
                or more of these attributes can be provided as keyword-arguments.
                This can be used to set several pen attributes in one statement.


                Examples (for a Turtle instance named turtle):
                >>> turtle.pen(fillcolor="black", pencolor="red", pensize=10)
                >>> turtle.pen()
                {'pensize': 10, 'shown': True, 'resizemode': 'auto', 'outline': 1,
                'pencolor': 'red', 'pendown': True, 'fillcolor': 'black',
                'stretchfactor': (1,1), 'speed': 3, 'shearfactor': 0.0}
                >>> penstate=turtle.pen()
                >>> turtle.color("yellow","")
                >>> turtle.penup()
                >>> turtle.pen()
                {'pensize': 10, 'shown': True, 'resizemode': 'auto', 'outline': 1,
                'pencolor': 'yellow', 'pendown': False, 'fillcolor': '',
                'stretchfactor': (1,1), 'speed': 3, 'shearfactor': 0.0}
                >>> p.pen(penstate, fillcolor="green")
                >>> p.pen()
                {'pensize': 10, 'shown': True, 'resizemode': 'auto', 'outline': 1,
                'pencolor': 'red', 'pendown': True, 'fillcolor': 'green',
                'stretchfactor': (1,1), 'speed': 3, 'shearfactor': 0.0}
        
        """
    def _newLine(self, usePos = True):
        """
        dummy method - to be overwritten by child class
        """
    def _update(self, count=True, forced=False):
        """
        dummy method - to be overwritten by child class
        """
    def _color(self, args):
        """
        dummy method - to be overwritten by child class
        """
    def _colorstr(self, args):
        """
        dummy method - to be overwritten by child class
        """
def _TurtleImage(object):
    """
    Helper class: Datatype to store Turtle attributes
    
    """
    def __init__(self, screen, shapeIndex):
        """
        polygon
        """
def RawTurtle(TPen, TNavigator):
    """
    Animation part of the RawTurtle.
        Puts RawTurtle upon a TurtleScreen and provides tools for
        its animation.
    
    """
2021-03-02 20:53:44,504 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:44,504 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:44,505 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, canvas=None,
                 shape=_CFG["shape"],
                 undobuffersize=_CFG["undobuffersize"],
                 visible=_CFG["visible"]):
        """
        bad canvas argument %s
        """
    def reset(self):
        """
        Delete the turtle's drawings and restore its default values.

                No argument.

                Delete the turtle's drawings from the screen, re-center the turtle
                and set variables to the default values.

                Example (for a Turtle instance named turtle):
                >>> turtle.position()
                (0.00,-22.00)
                >>> turtle.heading()
                100.0
                >>> turtle.reset()
                >>> turtle.position()
                (0.00,0.00)
                >>> turtle.heading()
                0.0
        
        """
    def setundobuffer(self, size):
        """
        Set or disable undobuffer.

                Argument:
                size -- an integer or None

                If size is an integer an empty undobuffer of given size is installed.
                Size gives the maximum number of turtle-actions that can be undone
                by the undo() function.
                If size is None, no undobuffer is present.

                Example (for a Turtle instance named turtle):
                >>> turtle.setundobuffer(42)
        
        """
    def undobufferentries(self):
        """
        Return count of entries in the undobuffer.

                No argument.

                Example (for a Turtle instance named turtle):
                >>> while undobufferentries():
                ...     undo()
        
        """
    def _clear(self):
        """
        Delete all of pen's drawings
        """
    def clear(self):
        """
        Delete the turtle's drawings from the screen. Do not move turtle.

                No arguments.

                Delete the turtle's drawings from the screen. Do not move turtle.
                State and position of the turtle as well as drawings of other
                turtles are not affected.

                Examples (for a Turtle instance named turtle):
                >>> turtle.clear()
        
        """
    def _update_data(self):
        """
        Perform a Turtle-data update.
        
        """
    def _tracer(self, flag=None, delay=None):
        """
        Turns turtle animation on/off and set delay for update drawings.

                Optional arguments:
                n -- nonnegative  integer
                delay -- nonnegative  integer

                If n is given, only each n-th regular screen update is really performed.
                (Can be used to accelerate the drawing of complex graphics.)
                Second arguments sets delay value (see RawTurtle.delay())

                Example (for a Turtle instance named turtle):
                >>> turtle.tracer(8, 25)
                >>> dist = 2
                >>> for i in range(200):
                ...     turtle.fd(dist)
                ...     turtle.rt(90)
                ...     dist += 2
        
        """
    def _color(self, args):
        """
        Convert colortriples to hexstrings.
        
        """
    def clone(self):
        """
        Create and return a clone of the turtle.

                No argument.

                Create and return a clone of the turtle with same position, heading
                and turtle properties.

                Example (for a Turtle instance named mick):
                mick = Turtle()
                joe = mick.clone()
        
        """
    def shape(self, name=None):
        """
        Set turtle shape to shape with given name / return current shapename.

                Optional argument:
                name -- a string, which is a valid shapename

                Set turtle shape to shape with given name or, if name is not given,
                return name of current shape.
                Shape with name must exist in the TurtleScreen's shape dictionary.
                Initially there are the following polygon shapes:
                'arrow', 'turtle', 'circle', 'square', 'triangle', 'classic'.
                To learn about how to deal with shapes see Screen-method register_shape.

                Example (for a Turtle instance named turtle):
                >>> turtle.shape()
                'arrow'
                >>> turtle.shape("turtle")
                >>> turtle.shape()
                'turtle'
        
        """
    def shapesize(self, stretch_wid=None, stretch_len=None, outline=None):
        """
        Set/return turtle's stretchfactors/outline. Set resizemode to "user".

                Optional arguments:
                   stretch_wid : positive number
                   stretch_len : positive number
                   outline  : positive number

                Return or set the pen's attributes x/y-stretchfactors and/or outline.
                Set resizemode to "user".
                If and only if resizemode is set to "user", the turtle will be displayed
                stretched according to its stretchfactors:
                stretch_wid is stretchfactor perpendicular to orientation
                stretch_len is stretchfactor in direction of turtles orientation.
                outline determines the width of the shapes's outline.

                Examples (for a Turtle instance named turtle):
                >>> turtle.resizemode("user")
                >>> turtle.shapesize(5, 5, 12)
                >>> turtle.shapesize(outline=8)
        
        """
    def shearfactor(self, shear=None):
        """
        Set or return the current shearfactor.

                Optional argument: shear -- number, tangent of the shear angle

                Shear the turtleshape according to the given shearfactor shear,
                which is the tangent of the shear angle. DO NOT change the
                turtle's heading (direction of movement).
                If shear is not given: return the current shearfactor, i. e. the
                tangent of the shear angle, by which lines parallel to the
                heading of the turtle are sheared.

                Examples (for a Turtle instance named turtle):
                >>> turtle.shape("circle")
                >>> turtle.shapesize(5,2)
                >>> turtle.shearfactor(0.5)
                >>> turtle.shearfactor()
                >>> 0.5
        
        """
    def settiltangle(self, angle):
        """
        Rotate the turtleshape to point in the specified direction

                Argument: angle -- number

                Rotate the turtleshape to point in the direction specified by angle,
                regardless of its current tilt-angle. DO NOT change the turtle's
                heading (direction of movement).


                Examples (for a Turtle instance named turtle):
                >>> turtle.shape("circle")
                >>> turtle.shapesize(5,2)
                >>> turtle.settiltangle(45)
                >>> stamp()
                >>> turtle.fd(50)
                >>> turtle.settiltangle(-45)
                >>> stamp()
                >>> turtle.fd(50)
        
        """
    def tiltangle(self, angle=None):
        """
        Set or return the current tilt-angle.

                Optional argument: angle -- number

                Rotate the turtleshape to point in the direction specified by angle,
                regardless of its current tilt-angle. DO NOT change the turtle's
                heading (direction of movement).
                If angle is not given: return the current tilt-angle, i. e. the angle
                between the orientation of the turtleshape and the heading of the
                turtle (its direction of movement).

                Deprecated since Python 3.1

                Examples (for a Turtle instance named turtle):
                >>> turtle.shape("circle")
                >>> turtle.shapesize(5,2)
                >>> turtle.tilt(45)
                >>> turtle.tiltangle()
        
        """
    def tilt(self, angle):
        """
        Rotate the turtleshape by angle.

                Argument:
                angle - a number

                Rotate the turtleshape by angle from its current tilt-angle,
                but do NOT change the turtle's heading (direction of movement).

                Examples (for a Turtle instance named turtle):
                >>> turtle.shape("circle")
                >>> turtle.shapesize(5,2)
                >>> turtle.tilt(30)
                >>> turtle.fd(50)
                >>> turtle.tilt(30)
                >>> turtle.fd(50)
        
        """
    def shapetransform(self, t11=None, t12=None, t21=None, t22=None):
        """
        Set or return the current transformation matrix of the turtle shape.

                Optional arguments: t11, t12, t21, t22 -- numbers.

                If none of the matrix elements are given, return the transformation
                matrix.
                Otherwise set the given elements and transform the turtleshape
                according to the matrix consisting of first row t11, t12 and
                second row t21, 22.
                Modify stretchfactor, shearfactor and tiltangle according to the
                given matrix.

                Examples (for a Turtle instance named turtle):
                >>> turtle.shape("square")
                >>> turtle.shapesize(4,2)
                >>> turtle.shearfactor(-0.5)
                >>> turtle.shapetransform()
                (4.0, -1.0, -0.0, 2.0)
        
        """
    def _polytrafo(self, poly):
        """
        Computes transformed polygon shapes from a shape
                according to current position and heading.
        
        """
    def get_shapepoly(self):
        """
        Return the current shape polygon as tuple of coordinate pairs.

                No argument.

                Examples (for a Turtle instance named turtle):
                >>> turtle.shape("square")
                >>> turtle.shapetransform(4, -1, 0, 2)
                >>> turtle.get_shapepoly()
                ((50, -20), (30, 20), (-50, 20), (-30, -20))

        
        """
    def _getshapepoly(self, polygon, compound=False):
        """
        Calculate transformed shape polygon according to resizemode
                and shapetransform.
        
        """
    def _drawturtle(self):
        """
        Manages the correct rendering of the turtle with respect to
                its shape, resizemode, stretch and tilt etc.
        """
    def stamp(self):
        """
        Stamp a copy of the turtleshape onto the canvas and return its id.

                No argument.

                Stamp a copy of the turtle shape onto the canvas at the current
                turtle position. Return a stamp_id for that stamp, which can be
                used to delete it by calling clearstamp(stamp_id).

                Example (for a Turtle instance named turtle):
                >>> turtle.color("blue")
                >>> turtle.stamp()
                13
                >>> turtle.fd(50)
        
        """
    def _clearstamp(self, stampid):
        """
        does the work for clearstamp() and clearstamps()
        
        """
    def clearstamp(self, stampid):
        """
        Delete stamp with given stampid

                Argument:
                stampid - an integer, must be return value of previous stamp() call.

                Example (for a Turtle instance named turtle):
                >>> turtle.color("blue")
                >>> astamp = turtle.stamp()
                >>> turtle.fd(50)
                >>> turtle.clearstamp(astamp)
        
        """
    def clearstamps(self, n=None):
        """
        Delete all or first/last n of turtle's stamps.

                Optional argument:
                n -- an integer

                If n is None, delete all of pen's stamps,
                else if n > 0 delete first n stamps
                else if n < 0 delete last n stamps.

                Example (for a Turtle instance named turtle):
                >>> for i in range(8):
                ...     turtle.stamp(); turtle.fd(30)
                ...
                >>> turtle.clearstamps(2)
                >>> turtle.clearstamps(-2)
                >>> turtle.clearstamps()
        
        """
    def _goto(self, end):
        """
        Move the pen to the point end, thereby drawing a line
                if pen is down. All other methods for turtle movement depend
                on this one.
        
        """
    def _undogoto(self, entry):
        """
        Reverse a _goto. Used for undo()
        
        """
    def _rotate(self, angle):
        """
        Turns pen clockwise by angle.
        
        """
    def _newLine(self, usePos=True):
        """
        Closes current line item and starts a new one.
                   Remark: if current line became too long, animation
                   performance (via _drawline) slowed down considerably.
        
        """
    def filling(self):
        """
        Return fillstate (True if filling, False else).

                No argument.

                Example (for a Turtle instance named turtle):
                >>> turtle.begin_fill()
                >>> if turtle.filling():
                ...     turtle.pensize(5)
                ... else:
                ...     turtle.pensize(3)
        
        """
    def begin_fill(self):
        """
        Called just before drawing a shape to be filled.

                No argument.

                Example (for a Turtle instance named turtle):
                >>> turtle.color("black", "red")
                >>> turtle.begin_fill()
                >>> turtle.circle(60)
                >>> turtle.end_fill()
        
        """
    def end_fill(self):
        """
        Fill the shape drawn after the call begin_fill().

                No argument.

                Example (for a Turtle instance named turtle):
                >>> turtle.color("black", "red")
                >>> turtle.begin_fill()
                >>> turtle.circle(60)
                >>> turtle.end_fill()
        
        """
    def dot(self, size=None, *color):
        """
        Draw a dot with diameter size, using color.

                Optional arguments:
                size -- an integer >= 1 (if given)
                color -- a colorstring or a numeric color tuple

                Draw a circular dot with diameter size, using color.
                If size is not given, the maximum of pensize+4 and 2*pensize is used.

                Example (for a Turtle instance named turtle):
                >>> turtle.dot()
                >>> turtle.fd(50); turtle.dot(20, "blue"); turtle.fd(50)
        
        """
    def _write(self, txt, align, font):
        """
        Performs the writing for write()
        
        """
    def write(self, arg, move=False, align="left", font=("Arial", 8, "normal")):
        """
        Write text at the current turtle position.

                Arguments:
                arg -- info, which is to be written to the TurtleScreen
                move (optional) -- True/False
                align (optional) -- one of the strings "left", "center" or right"
                font (optional) -- a triple (fontname, fontsize, fonttype)

                Write text - the string representation of arg - at the current
                turtle position according to align ("left", "center" or right")
                and with the given font.
                If move is True, the pen is moved to the bottom-right corner
                of the text. By default, move is False.

                Example (for a Turtle instance named turtle):
                >>> turtle.write('Home = ', True, align="center")
                >>> turtle.write((0,0), True)
        
        """
    def begin_poly(self):
        """
        Start recording the vertices of a polygon.

                No argument.

                Start recording the vertices of a polygon. Current turtle position
                is first point of polygon.

                Example (for a Turtle instance named turtle):
                >>> turtle.begin_poly()
        
        """
    def end_poly(self):
        """
        Stop recording the vertices of a polygon.

                No argument.

                Stop recording the vertices of a polygon. Current turtle position is
                last point of polygon. This will be connected with the first point.

                Example (for a Turtle instance named turtle):
                >>> turtle.end_poly()
        
        """
    def get_poly(self):
        """
        Return the lastly recorded polygon.

                No argument.

                Example (for a Turtle instance named turtle):
                >>> p = turtle.get_poly()
                >>> turtle.register_shape("myFavouriteShape", p)
        
        """
    def getscreen(self):
        """
        Return the TurtleScreen object, the turtle is drawing  on.

                No argument.

                Return the TurtleScreen object, the turtle is drawing  on.
                So TurtleScreen-methods can be called for that object.

                Example (for a Turtle instance named turtle):
                >>> ts = turtle.getscreen()
                >>> ts
                <turtle.TurtleScreen object at 0x0106B770>
                >>> ts.bgcolor("pink")
        
        """
    def getturtle(self):
        """
        Return the Turtleobject itself.

                No argument.

                Only reasonable use: as a function to return the 'anonymous turtle':

                Example:
                >>> pet = getturtle()
                >>> pet.fd(50)
                >>> pet
                <turtle.Turtle object at 0x0187D810>
                >>> turtles()
                [<turtle.Turtle object at 0x0187D810>]
        
        """
    def _delay(self, delay=None):
        """
        Set delay value which determines speed of turtle animation.
        
        """
    def onclick(self, fun, btn=1, add=None):
        """
        Bind fun to mouse-click event on this turtle on canvas.

                Arguments:
                fun --  a function with two arguments, to which will be assigned
                        the coordinates of the clicked point on the canvas.
                btn --  number of the mouse-button defaults to 1 (left mouse button).
                add --  True or False. If True, new binding will be added, otherwise
                        it will replace a former binding.

                Example for the anonymous turtle, i. e. the procedural way:

                >>> def turn(x, y):
                ...     left(360)
                ...
                >>> onclick(turn)  # Now clicking into the turtle will turn it.
                >>> onclick(None)  # event-binding will be removed
        
        """
    def onrelease(self, fun, btn=1, add=None):
        """
        Bind fun to mouse-button-release event on this turtle on canvas.

                Arguments:
                fun -- a function with two arguments, to which will be assigned
                        the coordinates of the clicked point on the canvas.
                btn --  number of the mouse-button defaults to 1 (left mouse button).

                Example (for a MyTurtle instance named joe):
                >>> class MyTurtle(Turtle):
                ...     def glow(self,x,y):
                ...             self.fillcolor("red")
                ...     def unglow(self,x,y):
                ...             self.fillcolor("")
                ...
                >>> joe = MyTurtle()
                >>> joe.onclick(joe.glow)
                >>> joe.onrelease(joe.unglow)

                Clicking on joe turns fillcolor red, unclicking turns it to
                transparent.
        
        """
    def ondrag(self, fun, btn=1, add=None):
        """
        Bind fun to mouse-move event on this turtle on canvas.

                Arguments:
                fun -- a function with two arguments, to which will be assigned
                       the coordinates of the clicked point on the canvas.
                btn -- number of the mouse-button defaults to 1 (left mouse button).

                Every sequence of mouse-move-events on a turtle is preceded by a
                mouse-click event on that turtle.

                Example (for a Turtle instance named turtle):
                >>> turtle.ondrag(turtle.goto)

                Subsequently clicking and dragging a Turtle will move it
                across the screen thereby producing handdrawings (if pen is
                down).
        
        """
    def _undo(self, action, data):
        """
        Does the main part of the work for undo()
        
        """
    def undo(self):
        """
        undo (repeatedly) the last turtle action.

                No argument.

                undo (repeatedly) the last turtle action.
                Number of available undo actions is determined by the size of
                the undobuffer.

                Example (for a Turtle instance named turtle):
                >>> for i in range(4):
                ...     turtle.fd(50); turtle.lt(80)
                ...
                >>> for i in range(8):
                ...     turtle.undo()
                ...
        
        """
def Screen():
    """
    Return the singleton screen object.
        If none exists at the moment, create a new one and return it,
        else return the existing one.
    """
def _Screen(TurtleScreen):
    """
    title
    """
    def __init__(self):
        """
         XXX there is no need for this code to be conditional,
         as there will be only a single _Screen instance, anyway
         XXX actually, the turtle demo is injecting root window,
         so perhaps the conditional creation of a root should be
         preserved (perhaps by passing it as an optional parameter)

        """
2021-03-02 20:53:44,533 : INFO : tokenize_signature : --> do i ever get here?
    def setup(self, width=_CFG["width"], height=_CFG["height"],
              startx=_CFG["leftright"], starty=_CFG["topbottom"]):
        """
         Set the size and position of the main window.

                Arguments:
                width: as integer a size in pixels, as float a fraction of the screen.
                  Default is 50% of screen.
                height: as integer the height in pixels, as float a fraction of the
                  screen. Default is 75% of screen.
                startx: if positive, starting position in pixels from the left
                  edge of the screen, if negative from the right edge
                  Default, startx=None is to center window horizontally.
                starty: if positive, starting position in pixels from the top
                  edge of the screen, if negative from the bottom edge
                  Default, starty=None is to center window vertically.

                Examples (for a Screen instance named screen):
                >>> screen.setup (width=200, height=200, startx=0, starty=0)

                sets window to 200x200 pixels, in upper left of screen

                >>> screen.setup(width=.75, height=0.5, startx=None, starty=None)

                sets window to 75% of screen by 50% of screen and centers
        
        """
    def title(self, titlestring):
        """
        Set title of turtle-window

                Argument:
                titlestring -- a string, to appear in the titlebar of the
                               turtle graphics window.

                This is a method of Screen-class. Not available for TurtleScreen-
                objects.

                Example (for a Screen instance named screen):
                >>> screen.title("Welcome to the turtle-zoo!")
        
        """
    def _destroy(self):
        """
        Shut the turtlegraphics window.

                Example (for a TurtleScreen instance named screen):
                >>> screen.bye()
        
        """
    def exitonclick(self):
        """
        Go into mainloop until the mouse is clicked.

                No arguments.

                Bind bye() method to mouseclick on TurtleScreen.
                If "using_IDLE" - value in configuration dictionary is False
                (default value), enter mainloop.
                If IDLE with -n switch (no subprocess) is used, this value should be
                set to True in turtle.cfg. In this case IDLE's mainloop
                is active also for the client script.

                This is a method of the Screen-class and not available for
                TurtleScreen instances.

                Example (for a Screen instance named screen):
                >>> screen.exitonclick()

        
        """
        def exitGracefully(x, y):
            """
            Screen.bye() with two dummy-parameters
            """
def Turtle(RawTurtle):
    """
    RawTurtle auto-creating (scrolled) canvas.

        When a Turtle object is created or a function derived from some
        Turtle method is called a TurtleScreen object is automatically created.
    
    """
2021-03-02 20:53:44,537 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:44,538 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:44,539 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self,
                 shape=_CFG["shape"],
                 undobuffersize=_CFG["undobuffersize"],
                 visible=_CFG["visible"]):
        """
        turtle_docstringdict
        """
def read_docstrings(lang):
    """
    Read in docstrings from lang-specific docstring dictionary.

        Transfer docstrings, translated to lang, from a dictionary-file
        to the methods of classes Screen and Turtle and - in revised form -
        to the corresponding functions.
    
    """
def getmethparlist(ob):
    """
    Get strings describing the arguments for the given object

        Returns a pair of strings representing function parameter lists
        including parenthesis.  The first string is suitable for use in
        function definition and the second is suitable for use in function
        call.  The "self" parameter is not included.
    
    """
def _turtle_docrevise(docstr):
    """
    To reduce docstrings from RawTurtle class for functions
    
    """
def _screen_docrevise(docstr):
    """
    To reduce docstrings from TurtleScreen class for functions
    
    """
def _make_global_funcs(functions, cls, obj, init, docrevise):
    """

    """
    def switchpen():
        """
        Demo of old turtle.py - module
        """
    def demo2():
        """
        Demo of some new features.
        """
        def baba(xdummy, ydummy):
            """
              Click me!
            """
